"""Explicit second-pass survival routes. Owner-authorized; OpenAI Codex (GPT-6).

Decorative crafting uses existing materials. Biological resources and rare
equipment retain collection/encounter sources, never a blanket recipe fallback.
"""
import json


def recipes(add, shaped, shapeless, construction, cook, cut):
    # Consume the shell-bearing block rather than dropping extra shells each
    # time the same self-dropping block is placed and broken again.
    add('ecology_seashell', {'type':'minecraft:stonecutting','ingredient':'jugcraft:wildlife_shellstone','result':{'id':'jugcraft:ecology_seashell','count':2}})
    # The orchard already grows real bananas; shared processing should accept
    # the imported food IDs without inventing a second crop or growing machine.
    # The one-banana crafting grid already makes a banana pup. Use the existing
    # cutting board for feed preparation instead of hiding that planting recipe.
    cut('beast_banana', 'banana', [('beast_banana',1)])
    shapeless('pocket_banana', ['beast_banana'], name='pocket_banana_food')
    shaped('atm', ['III','RER','ICI'], {'I':'minecraft:iron_ingot','R':'minecraft:redstone','E':'minecraft:emerald','C':'basic_circuit'})
    shaped('toilet_paper_streamer', ['PPP',' S '], {'P':'minecraft:paper','S':'minecraft:string'}, 3)
    shaped('beast_fedora', [' W ','WWW',' L '], {'W':'minecraft:brown_wool','L':'minecraft:leather'})
    shaped('beast_sombrero', [' H ','HHH',' S '], {'H':'minecraft:hay_block','S':'minecraft:string'})
    shaped('beast_pigshoes', ['I I','L L'], {'I':'minecraft:iron_ingot','L':'minecraft:leather'})
    shaped('beast_phantom_sail', ['MPM','MPM',' S '], {'M':'minecraft:phantom_membrane','P':'minecraft:stick','S':'minecraft:string'})
    shaped('beast_spectre_sail', ['PEP',' S '], {'P':'beast_phantom_sail','E':'ectoplasm','S':'minecraft:string'})
    shaped('beast_leafcutter_ant_chamber', ['DDD','DMD','DDD'], {'D':'minecraft:dirt','M':'minecraft:brown_mushroom'})
    shapeless('beast_leafcutter_anthill', ['beast_leafcutter_ant_chamber','beast_leafcutter_ant_pupa'])
    shapeless('beast_potted_flutter', ['minecraft:flower_pot','beast_acacia_blossom'])
    shapeless('beast_acacia_blossom', ['minecraft:acacia_leaves','minecraft:bone_meal'])
    for sand in ('sand','red_sand'):
        shaped('beast_'+sand+'_circle', ['SSS','S S','SSS'], {'S':'minecraft:'+sand})
    cut('beast_skunk_spray', 'beast_stink_bottle', [('beast_skunk_spray',1),('minecraft:glass_bottle',1)])
    shapeless('beast_poison_bottle', ['minecraft:glass_bottle','minecraft:spider_eye','beast_komodo_spit'])
    shapeless('beast_lava_bottle', ['minecraft:glass_bottle','minecraft:magma_cream','minecraft:blaze_powder'])
    add('beast_shrimp_fried_rice', {'type':'jugcraft:pot_cooking','ingredients':[{'ingredient':i} for i in ('jugcraft:rice','jugcraft:beast_cooked_lobster_tail','minecraft:egg','minecraft:bowl')], 'result':{'id':'jugcraft:beast_shrimp_fried_rice'}, 'time':200})
    shaped('cavern_dinosaur_train', ['C C','MMM'], {'C':'cavern_dinosaur_chop','M':'minecraft:minecart'})
    shapeless('cavern_jelly_bean', ['minecraft:sugar','cavern_gelatin_red'])
    shapeless('cavern_giant_sweetberry', ['minecraft:sweet_berries']*6)
    cut('cavern_pewen_branch', 'cavern_pewen_log', [('cavern_pewen_branch',4)])
    for part in ('block','pole'):
        cut('cavern_stripped_candy_cane_'+part, 'cavern_candy_cane_'+part, [('cavern_stripped_candy_cane_'+part,1)])
    shapeless('cavern_carmine_froglight', ['minecraft:pearlescent_froglight','minecraft:red_dye'])
    for size in ('small','medium','large'):
        add('cavern_sulfur_bud_'+size, {'type':'minecraft:stonecutting','ingredient':'jugcraft:cavern_sulfur','result':{'id':'jugcraft:cavern_sulfur_bud_'+size}})
    for moss in ('moss','pale_moss'):
        cut('ecology_'+moss+'_layer', 'minecraft:'+moss+'_carpet', [('ecology_'+moss+'_layer',1)])
    shapeless('landscape_ashed_soul_sand', ['minecraft:soul_sand','hearth_ash'])
    cook('landscape_dried_salt', 'salt', ('smelting',))
    colors=('white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black')
    for color in colors:
        if color == 'pink': continue # Supplied pink petals already have a route.
        shapeless('landscape_'+color+'_flower_petal_block', ['minecraft:pink_petals','minecraft:'+color+'_dye'])
    cut('landscape_rose_quartz_chunk', 'landscape_rose_quartz_block', [('landscape_rose_quartz_chunk',4)])
    for form in ('small_rose_quartz_bud','medium_rose_quartz_bud','large_rose_quartz_bud','rose_quartz_cluster'):
        add('landscape_'+form, {'type':'minecraft:stonecutting','ingredient':'jugcraft:landscape_rose_quartz_block','result':{'id':'jugcraft:landscape_'+form}})
    shapeless('nouveau_blank_glyph', ['nouveau_blank_parchment','nouveau_source_gem'])
    shaped('nouveau_rune', ['SAS','RGR'], {'S':'minecraft:stone','A':'nouveau_abjuration_essence','R':'minecraft:redstone','G':'nouveau_source_gem'})
    shapeless('nouveau_starby_gift', ['minecraft:paper','minecraft:string','nouveau_starbuncle_shards','minecraft:gold_ingot'])
    shaped('nouveau_alakarkinos_hat', ['TTT','TGT'], {'T':'minecraft:terracotta','G':'nouveau_alakarkinos_token'})
    for out, ins in {
        'pocket_candy_cane':['minecraft:sugar','minecraft:sugar','minecraft:red_dye'],
        'pocket_rock_candy':['hard_candy','minecraft:stick'],
        'pocket_egg_nog':['minecraft:milk_bucket','minecraft:egg','minecraft:sugar'],
        'pocket_holiday_cookie':['minecraft:cookie','minecraft:sugar','minecraft:green_dye'],
        'pocket_holiday_gift':['minecraft:paper','minecraft:string','pocket_holiday_cookie'],
        'pocket_siamese_gift':['minecraft:paper','minecraft:string','minecraft:cod'],
        'pocket_easter_egg':['minecraft:egg','minecraft:purple_dye','minecraft:gold_nugget'],
    }.items():shapeless(out,ins)
    shaped('pocket_item_petrifier',['OEO','RDR','OEO'],{'O':'minecraft:obsidian','E':'minecraft:ender_pearl','R':'minecraft:redstone','D':'minecraft:diamond'})
    # Solstice pieces are iron-stat seasonal cosmetics, available year-round.
    for part in ('helmet','chestplate','leggings','boots','sword'):
        shapeless('pocket_solstice_'+part,['minecraft:iron_'+part,'minecraft:gold_ingot','minecraft:green_dye','minecraft:red_dye'])
    shapeless('pocket_stone_block',['minecraft:stone','minecraft:gold_nugget'])
    cut('landscape_bramble_leaves','landscape_bramble',[('landscape_bramble_leaves',1)])
    cut('landscape_dead_branch','minecraft:dead_bush',[('landscape_dead_branch',1)])
    shapeless('landscape_bush',['minecraft:oak_leaves','minecraft:stick'])
    shapeless('landscape_spanish_moss',['shroud_moss'])
    shapeless('landscape_willow_vine',['landscape_willow_leaves','minecraft:vine'])
    for plant in ('high_grass','lumaloop','spanish_moss'):
        shapeless('landscape_'+plant+'_plant',['landscape_'+plant,'minecraft:bone_meal'])
    add('cavern_fissure_primal_magma',{'type':'minecraft:stonecutting','ingredient':'jugcraft:cavern_primal_magma','result':{'id':'jugcraft:cavern_fissure_primal_magma'}})


# At most one per pool unless noted. Tables belong to real native creatures or
# harvestable blocks, not unreferenced reward manifests. No boss reward is crafted.
SOURCES = {
    'entities/beast_leafcutter_ant': [('beast_leafcutter_ant_pupa',.25)],
    'entities/beast_cockroach': [('beast_cockroach_ootheca',.15)],
    'entities/beast_enderiophage': [('beast_ender_residue',.5)],
    'entities/beast_bunfungus': [('beast_mungal_spores',.5)],
    'entities/beast_soul_vulture': [('beast_soul_heart',.25)],
    'entities/beast_flutter': [('beast_music_disc_thime',.02)],
    'entities/beast_spectre': [('beast_ghostly_pickaxe',.02),('beast_halo',.02)],
    'entities/cavern_magnetron': [('cavern_heart_of_iron',1),('cavern_disc_fragment_fusion',.25)],
    'entities/cavern_licowitch': [('cavern_licowitch_radiant_essence',.5)],
    'entities/cavern_boundroid': [('cavern_bone_worms',.25)],
    'entities/cavern_radgill': [('cavern_stinky_fish',.5)],
    'entities/cavern_tremorzilla': [('cavern_volcanic_core',1)],
    'entities/grove_sporeling': [('grove_sporeling_overworld_egg',.1),('grove_sporeling_nether_egg',.1)],
    'chests/cavern_underground_cabin': [('cavern_underground_cabin_explorer_map',.25)],
    'chests/pocket_discovery': [(i,.1) for i in ('pocket_blue_screen','pocket_start_button','pocket_windows_31','pocket_windows_7','pocket_windows_8','pocket_windows_me','pocket_windows_mojave','pocket_windows_xp','pocket_xerox_parc_gui')],
    'entities/wildlife_boar': [('wildlife_music_disc_death_by_hogs',.02)],
    'entities/wildlife_deer': [('wildlife_music_disc_wild_ones',.01)],
    'blocks/ecology_coconut_leaves': [('ecology_music_disc_coconut',.001)],
    'chests/cavern_caveman_house': [('beast_ancient_dart',.5)] + [('cavern_cave_painting_'+n,.15) for n in ('friendship','hunt',*[f'mystery_{i}' for i in range(1,10)])],
    'entities/nouveau_wilden_hunter': [('nouveau_music_disc_firel_the_wild_hunt',.02)],
    'entities/nouveau_wilden_guardian': [('nouveau_music_disc_thistle_the_sound_of_glass',.02)],
    'entities/nouveau_wilden_chimera': [('nouveau_music_disc_aria_biblio',.1)],
}


def sources(data):
    manifest=[]
    shellstone=data/'loot_table/blocks/wildlife_shellstone.json'
    shells=json.loads(shellstone.read_text(encoding='utf-8'))
    shells['pools']=[p for p in shells['pools'] if p.get('name')!='jugcraft_source_ecology_seashell']
    shellstone.write_text(json.dumps(shells,indent=2)+'\n',encoding='utf-8')
    # Queens belong to occupied colonies, never an unconditional random pool.
    hill=data/'loot_table/blocks/wildlife_ant_hill.json'
    h=json.loads(hill.read_text(encoding='utf-8'))
    h['pools']=[p for p in h['pools'] if p.get('name')!='jugcraft_source_wildlife_queen_ant']
    hill.write_text(json.dumps(h,indent=2)+'\n',encoding='utf-8')
    species=set()
    for p in (data.parents[1]/'jugcraft/menagerie').glob('*.json'):
        catalog=json.loads(p.read_text(encoding='utf-8'))
        if isinstance(catalog,dict): species.update(s['id'] for s in catalog.get('species',[]))
    for table, rewards in SOURCES.items():
        p=data/('loot_table/'+table+'.json')
        if not p.exists() and table.removeprefix('entities/') not in species: raise ValueError('Loot source must already exist or be a registered species: '+table)
        d=json.loads(p.read_text(encoding='utf-8')) if p.exists() else {'type':'minecraft:entity','pools':[]}
        for item,chance in rewards:
            name='jugcraft_source_'+item
            d['pools']=[pool for pool in d.get('pools',[]) if pool.get('name')!=name]
            d['pools'].append({'name':name,'rolls':1,'condition':{'type':'minecraft:random_chance','chance':chance},'entries':[{'type':'minecraft:item','name':'jugcraft:'+item}]})
            manifest.append({'table':'jugcraft:'+table,'item':'jugcraft:'+item,'chance':chance})
        p.write_text(json.dumps(d,indent=2)+'\n',encoding='utf-8')
    return manifest


def soda_lake(data):
    """New-chunk-only source in Candy Cavity, using the existing bounded placer."""
    name='cavern_survival_soda_lake'
    documents={
        'worldgen/feature/'+name+'.json': {'type':'jugcraft:landscape_detail','shape':'lake','primary':{'id':'jugcraft:cavern_purple_soda'},'secondary':{'id':'jugcraft:cavern_block_of_chocolate'},'radius':4,'height':3},
        'worldgen/placed_feature/'+name+'.json': {'feature':'jugcraft:'+name,'placement':[
            {'type':'minecraft:rarity_filter','chance':4},{'type':'minecraft:in_square'},
            {'type':'minecraft:height_range','height':{'absolute':-20}},
            {'type':'minecraft:environment_scan','direction_of_search':'down','max_steps':32,'target_condition':{'type':'minecraft:solid'}},
            {'type':'minecraft:biome'}]}}
    for name,d in documents.items(): (data/name).write_text(json.dumps(d,indent=2)+'\n',encoding='utf-8')
    path=data/'worldgen/biome/cavern_candy_cavity.json'; d=json.loads(path.read_text(encoding='utf-8'))
    feature='jugcraft:cavern_survival_soda_lake'
    if feature not in d['features'][6]:d['features'][6].append(feature)
    path.write_text(json.dumps(d,indent=2)+'\n',encoding='utf-8')


def structure_chests(data):
    """Connect empty vanilla containers in runtime copies; retain authored stock.

    The originals and occupied/explicitly assigned containers are never changed.
    Native owner barrels use the shared randomizable cabinet inventory.
    """
    from nouveau_nbt import read, write
    targets=('cavern_caveman_house','cavern_underground_cabin','cavern_magnetic_ruins',
             'cavern_toxic_ruins','cavern_abyssal_ruins','cavern_forlorn_ruins',
             'cavern_gingerbread_town','cavern_licowitch_tower')
    links=[]
    catalog_path=data.parents[1]/'jugcraft/menagerie/cavern.json'
    catalog=json.loads(catalog_path.read_text(encoding='utf-8'))
    rusty=catalog['blocks']['cavern_rusty_barrel'];rusty['kind']='shelf';rusty['definition']['kind']='storage'
    catalog_path.write_text(json.dumps(catalog,indent=2)+'\n',encoding='utf-8')
    for p in sorted((data/'structure').rglob('*.nbt')):
        template=p.relative_to(data/'structure').as_posix()[:-4]
        table=next((t for t in targets if p.stem==t or p.stem.startswith(t+'_')),None)
        if template=='cavern/landmarks/gingerbread_town':table='cavern_gingerbread_town'
        if p.stem=='landscape_ruined_keep':table='landscape_keep_loot_table'
        elif p.stem in ('landscape_small_goblin_camp','landscape_medium_goblin_camp','landscape_large_goblin_camp'):table='landscape_camp_loot_table'
        if not table: continue
        key='jugcraft:chests/'+table
        assert (data/('loot_table/chests/'+table+'.json')).exists()
        root=read(p.read_bytes());nbt=root[2];palette=nbt.get('palette',(9,(10,[])))[1][1];changed=False
        for b in nbt.get('blocks',(9,(10,[])))[1][1]:
            state=palette[b['state'][1]];id=state.get('id',state.get('Name',(8,'')))[1]
            if id not in ('minecraft:chest','minecraft:barrel','jugcraft:cavern_metal_barrel','jugcraft:cavern_rusty_barrel','jugcraft:cavern_gingerbarrel'):continue
            fields=b.get('nbt',(10,{}))[1]
            existing=fields.get('LootTable',(8,''))[1]
            # Legacy namespace flattening left these two real treasure routes
            # pointing at nonexistent tables. Preserve their authored rewards.
            if existing in ('jugcraft:landscape_chests/camp_loot_table','jugcraft:landscape_chests/keep_loot_table'):
                fields['LootTable']=(8,key);existing=key;changed=True
            if existing:
                if existing==key: links.append({'template':'jugcraft:'+template,'table':key,'pos':b['pos'][1][1]})
                continue
            if fields.get('Items',(9,(10,[])))[1][1]:continue
            fields.pop('Items',None);fields['id']=(8,'jugcraft:kitchen_cabinet' if id.startswith('jugcraft:') else id);fields['LootTable']=(8,key)
            b['nbt']=(10,fields);changed=True
            links.append({'template':'jugcraft:'+template,'table':key,'pos':b['pos'][1][1]})
        if changed:p.write_bytes(write(root))
    return links
