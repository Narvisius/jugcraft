"""Native 26.3 adapters for the supplied gameplay data, structures and registry assets."""
import copy
import json
from pathlib import Path
import nouveau_nbt


def write_all(write, assets, data, lang):
    from nouveau_content import DATA, ASSET, CATALOG, LANG, resource_id, asset_id, id_of, walk, ROOT
    def read(path): return json.loads(path.read_text(encoding='utf-8'))
    def registry(value):
        if value.startswith('ars_nouveau:'):value='ars_jimbaux:'+value.split(':',1)[1]
        if value == 'minecraft:patch_grass': return 'minecraft:grass'
        if value.startswith('#'): return '#' + resource_id(value[1:])
        if value.startswith('ars_jimbaux:entities/'):
            return 'jugcraft:entities/nouveau_' + value.split('/', 1)[1].replace('wilden_boss', 'wilden_chimera')
        if value.startswith('ars_jimbaux:blocks/'):
            return 'jugcraft:blocks/' + id_of(value.split('/', 1)[1])
        return resource_id(value)
    def convert(value):
        if isinstance(value, str): return registry(value)
        if isinstance(value, list): return [convert(v) for v in value]
        if not isinstance(value, dict): return value
        result = {key: convert(v) for key, v in value.items()}
        if result.get('type') == 'minecraft:simple_state_provider': return result['state']
        if 'config' in result and isinstance(result['config'], dict): result = {**result, **result.pop('config')}; result.pop('config', None)
        if 'Name' in result:
            result['id'] = result.pop('Name')
            if 'Properties' in result: result['properties'] = result.pop('Properties')
        if 'function' in result: result['type'] = result.pop('function')
        if isinstance(result.get('condition'), str): result['type'] = result.pop('condition')
        if 'functions' in result: result['modifier'] = result.pop('functions')
        if 'conditions' in result and isinstance(result['conditions'], list):
            values = result.pop('conditions')
            if values: result['condition'] = values[0] if len(values) == 1 else {'type':'minecraft:all_of','terms':values}
        return result
    for source_lang in sorted((ASSET/'lang').glob('*.json')):
        if source_lang.stem=='en_us':continue
        translated={}
        for key,value in read(source_lang).items():
            if key.startswith(('item.ars_jimbaux.','block.ars_jimbaux.')):
                prefix,name=key.split('.ars_jimbaux.',1);key=prefix+'.jugcraft.'+id_of(name)
            else:key=key.replace('ars_jimbaux.','jugcraft.nouveau_')
            translated[key]=value.replace('Ars Nouveau','Jug Nouveau')
        target=assets/'lang'/source_lang.name
        existing=read(target) if target.exists() else {};existing.update(translated);write(target,existing)
    for source_key, text in LANG.items():
        target = source_key.replace('ars_jimbaux.', 'jugcraft.nouveau_')
        if source_key.startswith(('item.ars_jimbaux.', 'block.ars_jimbaux.')):
            prefix, name = source_key.split('.ars_jimbaux.', 1); target = prefix + '.jugcraft.' + id_of(name)
        lang[target] = text.replace('Ars Nouveau', 'Jug Nouveau')
        if source_key.startswith('item.ars_jimbaux.ritual_'): lang[target] = 'Ritual of ' + lang[target]
        if source_key.startswith('item.ars_jimbaux.music_disc_') and not source_key.endswith('.desc'): lang[target] = 'Music Disc — ' + LANG.get(source_key+'.desc', source_key.rsplit('.',1)[-1].replace('music_disc_','').replace('_',' ').title())
    for folder in ('damage_type', 'banner_pattern', 'enchantment', 'advancement'):
        for path in sorted((DATA / folder).rglob('*.json')):
            value = walk(read(path), registry)
            if folder == 'enchantment':
                # Focus and reactive handlers replace the old loader's attribute/effect hooks.
                value.pop('effects', None)
                if isinstance(value.get('supported_items'), dict): value['supported_items'] = '#jugcraft:nouveau/reactive_compatible'
            if folder == 'banner_pattern': value['asset_id'] = asset_id(read(path)['asset_id'])
            if folder == 'advancement':
                original=read(path)
                recipe_key=lambda ref: 'jugcraft:nouveau/'+ref.split(':',1)[1] if ref.startswith('ars_jimbaux:') else ref
                if 'recipes' in original.get('rewards',{}): value['rewards']['recipes']=[recipe_key(ref) for ref in original['rewards']['recipes']]
                for name,criterion in original.get('criteria',{}).items():
                    if criterion.get('trigger')=='minecraft:recipe_unlocked':
                        value['criteria'][registry(name)]['conditions']={'recipes':recipe_key(criterion['conditions']['recipe'])}
                for criterion in value.get('criteria',{}).values():
                    if criterion.get('trigger','').startswith('jugcraft:nouveau_'): criterion.clear(); criterion['trigger']='minecraft:impossible'
            if folder == 'advancement' and 'display' in value:
                background = value['display'].get('background')
                if background: value['display']['background'] = asset_id(read(path)['display']['background']).replace(':textures/', ':').removesuffix('.png')
            value = walk(value, lambda s: s.replace('ars_jimbaux.', 'jugcraft.nouveau_'))
            write(data / folder / ('nouveau_' + path.relative_to(DATA / folder).as_posix()), value)
    # Source entity/chest tables retain their rolls, conditions, enchantments and equipment rewards.
    # Block drops are authored by the block-family adapter so inventories/fluids are never duplicated.
    for folder in ('loot_table', 'loot_tables'):
        for path in sorted((DATA / folder).rglob('*.json')):
            rel = path.relative_to(DATA / folder).as_posix()
            if rel.startswith('blocks/'): continue
            target = 'entities/nouveau_' + rel.split('/', 1)[1].replace('wilden_boss', 'wilden_chimera') if rel.startswith('entities/') else 'nouveau_' + rel
            write(data / 'loot_table' / target, convert(read(path)))
    for folder in ('template_pool', 'processor_list', 'structure', 'structure_set'):
        for path in sorted((DATA / 'worldgen' / folder).rglob('*.json')):
            value = convert(read(path))
            if folder == 'structure':
                value['type'] = 'minecraft:jigsaw'
                for override in value.get('spawn_overrides', {}).values():
                    for spawn in override.get('spawns', []):
                        low, high = spawn.pop('minCount', 1), spawn.pop('maxCount', 1)
                        spawn['count'] = low if low == high else {'type':'minecraft:uniform','min_inclusive':low,'max_inclusive':high}
            if folder == 'processor_list':
                # The runtime palette has native waterlogged properties; the old custom processor is redundant.
                value['processors'] = [p for p in value.get('processors', []) if not p.get('processor_type', '').endswith('waterlogging_fix')]
            write(data / 'worldgen' / folder / ('nouveau_' + path.relative_to(DATA / 'worldgen' / folder).as_posix()), value)
    for path in sorted((DATA / 'structure').glob('*.nbt')):
        target = data / 'structure' / ('nouveau_' + path.name)
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(nouveau_nbt.adapt(path.read_bytes(), registry))
    # Archwood feature aliases preserve the original worldgen references and supplied pod identities.
    for old, color in [('blazing','red'),('cascading','blue'),('flourishing','green'),('vexing','purple')]:
        for name in (old + '_tree', 'natural_' + old + '_tree'):
            write(data / 'worldgen/feature' / ('nouveau_' + name + '.json'), {'type':'jugcraft:nouveau_archwood','color':color,'height':9,'fruit':True})
        write(data / f'worldgen/feature/nouveau/{color}_archwood.json', {'type':'jugcraft:nouveau_archwood','color':color,'height':9,'fruit':True})
    for path in sorted((DATA / 'worldgen/configured_feature').glob('*.json')):
        if path.stem.endswith('_tree'): continue
        value = convert(read(path))
        if path.stem == 'lights':
            value = {'type':'minecraft:simple_block','to_place':{'id':'jugcraft:nouveau_light_block','properties':{'level':'10','waterlogged':'false'}}}
        if path.stem == 'patch_berry':
            value = {'type':'minecraft:simple_block','to_place':{'id':'jugcraft:nouveau_sourceberry_bush','properties':{'age':'3'}}}
        write(data / 'worldgen/feature' / ('nouveau_' + path.name), value)
    for path in sorted((DATA / 'worldgen/placed_feature').glob('*.json')):
        write(data / 'worldgen/placed_feature' / ('nouveau_' + path.name), convert(read(path)))
    for path in sorted((DATA / 'worldgen/biome').glob('*.json')):
        value = convert(read(path)); old = value.pop('effects', {}); attrs = {}
        if isinstance(value.get('carvers'), dict): value['carvers'] = [entry for group in value['carvers'].values() for entry in group]
        for old_key, key in [('sky_color','sky_color'),('fog_color','fog_color'),('water_fog_color','water_fog_color')]:
            if old_key in old: attrs['minecraft:visual/' + key] = f'#{old.pop(old_key):06x}'
        music = old.pop('music', None)
        if music:
            if music['sound'].startswith('jugcraft:'): music['sound'] = 'jugcraft:nouveau.' + music['sound'].split(':')[-1].removeprefix('nouveau_')
            attrs['minecraft:audio/background_music'] = {'default':music}
        old.pop('mood_sound', None)
        value['effects'] = {k:f'#{v:06x}' if isinstance(v, int) else v for k,v in old.items()}
        spawns = value.pop('spawners', {})
        for entries in spawns.values():
            for spawn in entries:
                low, high = spawn.pop('minCount', 1), spawn.pop('maxCount', 1)
                spawn['count'] = low if low == high else {'type':'minecraft:uniform','min_inclusive':low,'max_inclusive':high}
        attrs['minecraft:gameplay/natural_mob_spawns'] = {'modifier':'overlay','argument':{'spawn_costs':value.pop('spawn_costs',{}),'spawns_by_category':spawns}}
        value['attributes'] = attrs
        if path.stem == 'archwood_forest':
            # Use the shared current-version forest base; retain the supplied climate, colors, creatures and canopy.
            import biomes_data
            value['features'] = biomes_data.steps('nouveau_archwood_forest')
            value['features'][9].extend(['jugcraft:nouveau_placed_dense_archwoods','jugcraft:nouveau_placed_berry_bush','jugcraft:nouveau_placed_lights'])
        write(data / 'worldgen/biome' / ('nouveau_' + path.name), value)
    dimension = read(data / 'dimension_type/moonlit_ring.json')
    dimension.update(has_fixed_time=False, ambient_light=.8)
    dimension['attributes'] = {'minecraft:gameplay/bed_rule':{'can_sleep':'never','can_set_spawn':'never','destroy_on_use':False},'minecraft:gameplay/respawn_anchor_works':False,'minecraft:gameplay/can_start_raid':False,'minecraft:visual/sky_color':'#192c45','minecraft:visual/fog_color':'#192c45'}
    write(data / 'dimension_type/nouveau_planarium.json', dimension)
    write(data / 'dimension/nouveau_planarium.json', {'type':'jugcraft:nouveau_planarium','generator':{'type':'minecraft:flat','settings':{'biome':'jugcraft:nouveau_jar','features':False,'lakes':False,'layers':[],'structure_overrides':[]}}})
    # Source curios slots are mapped onto the installed shared Trinkets slots.
    slots = {'necklace':'chest/necklace','belt':'legs/belt','head':'head/hat','ring':'hand/ring','an_focus':'offhand/glove'}
    for source, slot in slots.items():
        source_tag = DATA / 'tags/item/curios' / (source + '.json')
        values = walk(read(source_tag).get('values', []), registry) if source_tag.exists() else []
        if not values:
            values = ['jugcraft:' + e['id'] for n,e in CATALOG['items'].items() if e['kind']=='trinket' and (n.startswith('amulet_') if source=='necklace' else n.startswith('belt_') if source=='belt' else n.endswith('crown') if source=='head' else 'ring' in n if source=='ring' else n.endswith('_focus'))]
        target = data.parent / 'trinkets/tags/item' / (slot + '.json')
        previous = read(target).get('values', []) if target.exists() else []
        write(target, {'replace':False,'values':list(dict.fromkeys(previous + values))})
    write(data / 'tags/item/nouveau/reactive_compatible.json', {'values':['#minecraft:enchantable/durability','#minecraft:enchantable/armor'] + ['jugcraft:'+v['id'] for v in CATALOG['items'].values() if v['kind'] in {'spellbook','instrument'}]})
    write(data / 'tags/item/nouveau_armor_repair.json', {'values':['jugcraft:nouveau_magebloom_fiber']})
    write(data / 'tags/block/nouveau/gauntlet_mineable.json', {'values':['#minecraft:mineable/axe','#minecraft:mineable/pickaxe','#minecraft:mineable/shovel','#minecraft:mineable/hoe']})
    write(data / 'tags/banner_pattern/nouveau_pattern_item/nouveau.json', {'values':['jugcraft:nouveau_'+p.stem for p in sorted((DATA/'banner_pattern').glob('*.json'))]})
    write(data.parent/'minecraft/tags/banner_pattern/no_item_required.json', {'replace':False,'values':[]})
    # Potions use native 26.3 brewing recipes, including every bottle form and both duration/power upgrades.
    for effect, reagent in {'shielding':'bastion_pod','recovery':'mendosteen_pod','freezing':'frostaya_pod','blasting':'bombegranate_pod','mana_regen':'sourceberry_bush'}.items():
        for form in ('potion','splash_potion','lingering_potion'):
            def brew(name, base, result, ingredient):
                write(data / f'recipe/nouveau/brewing/{form}_{name}.json', {'type':'minecraft:brewing','input':{'item':'minecraft:'+form,'potion_contents':{'potions':base}},'output':{'id':'minecraft:'+form,'components':{'minecraft:potion_contents':{'potion':result}}},'reagent':{'item':ingredient}})
            base='jugcraft:nouveau_'+effect
            brew(effect,'minecraft:awkward',base,'jugcraft:'+id_of(reagent))
            brew(effect+'_extended',base,base+'_long','minecraft:redstone')
            brew(effect+'_strong',base,base+'_strong','minecraft:glowstone_dust')
        for suffix in ('','_long','_strong'):
            potion='jugcraft:nouveau_'+effect+suffix
            for form,out,reagent_item in [('potion','splash_potion','gunpowder'),('splash_potion','lingering_potion','dragon_breath')]:
                write(data/f'recipe/nouveau/brewing/{effect}{suffix}_{out}.json',{'type':'minecraft:brewing','input':{'item':'minecraft:'+form,'potion_contents':{'potions':potion}},'output':{'id':'minecraft:'+out,'components':{'minecraft:potion_contents':{'potion':potion}}},'reagent':{'item':'minecraft:'+reagent_item}})
            for form in ('potion','splash_potion','lingering_potion','tipped_arrow'): lang[f'item.minecraft.{form}.effect.nouveau_{effect}{suffix}'] = ('Arrow of ' if form=='tipped_arrow' else ('Lingering ' if form=='lingering_potion' else 'Splash ' if form=='splash_potion' else '')+'Potion of ') + effect.replace('_',' ').title()
    write(assets.parents[1]/'jugcraft/nouveau/particles.json',[p.stem for p in sorted((ASSET/'particles').glob('*.json'))])
    # Translate the supplied NeoForge loot injection into a small Fabric-readable table specification.
    injections=[]
    for path in (DATA/'loot_modifiers').glob('*.json'):
        original=read(path);tables=[]
        def collect(v):
            if isinstance(v,dict):
                if 'loot_table_id' in v: tables.append(v['loot_table_id'])
                for child in v.values(): collect(child)
            elif isinstance(v,list):
                for child in v:collect(child)
        collect(original)
        injections.append({'tables':tables, **{rarity+'_'+field:original.get(rarity+'_'+field,default) for rarity,chance,rolls in [('common',.3,3),('uncommon',.2,2),('rare',.1,1)] for field,default in [('chance',chance),('rolls',rolls)]}})
    write(assets.parents[1]/'jugcraft/nouveau/loot_injections.json',injections)
    # Optional light integration uses the installed client's native definitions.
    write(assets/'dynamiclights/item/nouveau_jar_of_light.json',{'match':{'items':'jugcraft:nouveau_jar_of_light'},'luminance':15})
    write(assets/'dynamiclights/entity/nouveau_starbuncle.json',{'match':{'type':'jugcraft:starbuncle'},'luminance':10})
    import nouveau_guide
    nouveau_guide.write_all(write,assets,data,lang)
    finalize_resources(write,assets,data)
    import jug_nouveau
    write(ROOT/"docs/features/jug-nouveau-source-manifest.json",jug_nouveau.source_manifest())
    for suffix,label in [('familiar','Choose familiar'),('crown',"Use Alchemist's Crown"),('spell_style','Style held spell instrument'),('familiar_collection','Toggle familiar collection')]: lang['key.jugcraft.nouveau_'+suffix] = label


def finalize_resources(write,assets,data):
    """Late merges run after shared generators, preserving their entries and owned component drops."""
    from nouveau_content import ASSET, DATA, CATALOG, INTERNAL, resource_id, asset_id, id_of, walk
    read=lambda path: json.loads(path.read_text(encoding='utf-8'))
    for registry, name, values in [('item','storage_blocks/quartz',['minecraft:quartz_block']),('block','relocation_not_supported',['minecraft:bedrock','minecraft:end_portal_frame']),('entity_type','capturing_not_supported',['minecraft:ender_dragon','minecraft:wither','minecraft:player']),('entity_type','bosses',['minecraft:ender_dragon','minecraft:wither']),('worldgen/biome','is_mushroom',['minecraft:mushroom_fields'])]:
        path=data.parent/'c/tags'/registry/(name+'.json');existing=read(path) if path.exists() else {'replace':False,'values':[]}
        existing['values']=list(dict.fromkeys(resource_id(v) for v in existing.get('values',[])+values));write(path,existing)
    sounds=read(assets/'sounds.json') if (assets/'sounds.json').exists() else {}
    sounds.update({'nouveau.'+k:walk(v,asset_id) for k,v in read(ASSET/'sounds.json').items()})
    write(assets/'sounds.json',sounds)
    for namespace in ('minecraft','c','curios','forge','accessories'):
        for path in sorted((DATA.parent/namespace/'tags').rglob('*.json')):
            original=read(path)
            # These shared directories also describe other owner mods. Only append this port's membership.
            entries=[v for v in original.get('values',[]) if (v.get('id','') if isinstance(v,dict) else v).lstrip('#').startswith('ars_jimbaux:')]
            if not entries: continue
            target=data.parent/namespace/'tags'/path.relative_to(DATA.parent/namespace/'tags')
            existing=read(target) if target.exists() else {'replace':False,'values':[]}
            for value in walk(entries,resource_id):
                if value not in existing['values']: existing['values'].append(value)
            write(target,existing)
    for name,spec in CATALOG['blocks'].items():
        if spec['kind']=='existing':continue
        key='jugcraft:'+spec['id'];entry={'type':'minecraft:item','name':key}
        if name in INTERNAL or spec['kind'] in {'wall_sign','hanging_wall_sign'}:
            value={'type':'minecraft:block','pools':[]}
        elif spec['kind']=='potted':
            plant='jugcraft:'+id_of(name.removeprefix('potted_'))
            value={'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':item}]} for item in ('minecraft:flower_pot',plant)]}
        elif spec['kind']=='station':
            entry['modifier']=[{'type':'minecraft:copy_components','source':'block_entity','include':['minecraft:custom_name','minecraft:potion_contents','jugcraft:nouveau_jar_doses','jugcraft:ley_charge','jugcraft:nouveau_captured_entity']}]
            value={'type':'minecraft:block','pools':[{'rolls':1,'entries':[entry]}]}
        elif spec['kind']=='slab':
            entry['modifier']=[{'type':'minecraft:set_count','count':2,'condition':{'type':'minecraft:match_block','blocks':key,'state':{'type':'double'}}},{'type':'minecraft:explosion_decay'}]
            value={'type':'minecraft:block','pools':[{'rolls':1,'entries':[entry]}]}
        elif spec['kind']=='door':
            value={'type':'minecraft:block','pools':[{'rolls':1,'entries':[entry],'condition':{'type':'minecraft:match_block','blocks':key,'state':{'half':'lower'}}}]}
        elif name=='magebloom_crop':
            value={'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:'+id_of('magebloom_crop')}]},{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:'+id_of('magebloom'),'modifier':[{'type':'minecraft:set_count','count':{'type':'minecraft:uniform','min':1,'max':3}}]}],'condition':{'type':'minecraft:match_block','blocks':key,'state':{'age':'7'}}}]}
        else: continue
        write(data/'loot_table/blocks'/(spec['id']+'.json'),value)
