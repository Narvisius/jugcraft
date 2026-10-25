"""Original encounter geometry/animation/art; native 26.3 schemas and explicit obtainability."""
import json
from menagerie_data import loot

ITEMS=('ice_spider_spawn_egg','keeper_spawn_egg','ice_spider_egg','cooked_ice_spider_egg','prepared_ice_spider_egg','ice_spider_pheromones','keeper_flamberge','legionnaire_flamberge','icy_fang','cinder_essence')
BLOCKS=('ice_spider_egg','cooked_ice_spider_egg')
ENTITIES=('ice_spider','citadel_keeper','spider_tomb','spider_ice_spike')
def items():return ['jimta_spellbooks_'+name for name in ITEMS]
def blocks():return ['jimta_spellbooks_'+name for name in BLOCKS]
def entities():return ['jimtafacts_'+name for name in ENTITIES]
def j(name):return 'jugcraft:jimta_spellbooks_'+name

def generate(copy,texture,dump,res,art,lang):
    source=art.parents[1]/'data/irons_spellbooks'
    for entity,geometry,animation,skins in (
        ('ice_spider','ice_spider','ice_spider.animation',('entity/ice_spider/ice_spider',)),
        ('citadel_keeper','citadel_keeper','casting_animations',('entity/keeper/keeper','entity/keeper/keeper_restored','entity/keeper/keeper_ghost'))):
        copy(art/f'geo/{geometry}.geo.json',res/f'assets/jugcraft/geckolib/models/entity/jimtafacts/{entity}.geo.json')
        copy(art/f'animations/{animation}.json',res/f'assets/jugcraft/geckolib/animations/entity/jimtafacts/{entity}.animation.json')
        for skin in skins:texture('irons_spellbooks:'+skin)
    for name in ENTITIES:lang['entity.jugcraft.jimtafacts_'+name]=name.replace('_',' ').title()
    for skin in ('entity/ice_spike','entity/ice_tomb/ice_tomb','entity/ice_tomb/ice_tomb_cull'):texture('irons_spellbooks:'+skin)
    models=set()
    def model(ref):
        if not ref.startswith('irons_spellbooks:'):return ref if ':' in ref else 'minecraft:'+ref
        path=ref.split(':',1)[1];native='jugcraft:jimtafacts/spellbooks/'+path
        if path in models:return native
        models.add(path);raw=json.loads((art/f'models/{path}.json').read_text(encoding='utf-8'));raw=raw['base'] if raw.get('loader')=='neoforge:separate_transforms' else raw
        raw=dict(raw)
        if 'parent' in raw:raw['parent']=model(raw['parent'])
        raw['textures']={key:texture(value) for key,value in raw.get('textures',{}).items()}
        raw.pop('loader',None);raw.pop('render_type',None);raw.pop('overrides',None)
        from jimtafacts_model_repairs import repair
        dump(res/f'assets/jugcraft/models/jimtafacts/spellbooks/{path}.json',repair('jimtafacts/spellbooks/'+path,raw));return native
    for name in ITEMS:
        raw=json.loads((art/f'models/item/{name}.json').read_text(encoding='utf-8'))
        if name.endswith('_spawn_egg'):
            # Source spawn-egg textures used tint indices; 26.3 tint definitions live on the item model node.
            colors=(8552850,16119275) if name.startswith('ice') else (3484973,0x766A76)
            definition={'type':'minecraft:model','model':'minecraft:item/spider_spawn_egg' if name.startswith('ice') else 'minecraft:item/zombie_spawn_egg','tints':[{'type':'minecraft:constant','value':c} for c in colors]}
        else:
            definition={'type':'minecraft:model','model':model('irons_spellbooks:item/'+name)}
            if raw.get('loader')=='neoforge:separate_transforms':
                cases=[]
                for view,value in raw['perspectives'].items():
                    value=dict(value)
                    if 'parent' in value:value['parent']=model(value['parent'])
                    value['textures']={key:texture(v) for key,v in value.get('textures',{}).items()}
                    path=f'jimtafacts/spellbooks/perspective/{name}_{view}';dump(res/f'assets/jugcraft/models/{path}.json',value)
                    cases.append({'when':view,'model':{'type':'minecraft:model','model':'jugcraft:'+path}})
                definition={'type':'minecraft:select','property':'minecraft:display_context','cases':cases,'fallback':definition}
        dump(res/f'assets/jugcraft/items/jimta_spellbooks_{name}.json',{'model':definition});lang['item.jugcraft.jimta_spellbooks_'+name]=name.replace('_',' ').title()
    def state_models(value):
        if isinstance(value,list):return [state_models(x) for x in value]
        if isinstance(value,dict):return {k:model(v) if k=='model' else state_models(v) for k,v in value.items()}
        return value
    for name in BLOCKS:
        dump(res/f'assets/jugcraft/blockstates/jimta_spellbooks_{name}.json',state_models(json.loads((art/f'blockstates/{name}.json').read_text(encoding='utf-8'))))
        lang['block.jugcraft.jimta_spellbooks_'+name]=name.replace('_',' ').title()
    def remap(value):
        if isinstance(value,str):return j(value.split(':',1)[1]) if value.startswith('irons_spellbooks:') else value
        if isinstance(value,list):return [remap(v) for v in value]
        if isinstance(value,dict):return {k:remap(v) for k,v in value.items()}
        return value
    for name in ('ice_spider','citadel_keeper'):
        data=loot(remap(json.loads((source/f'loot_table/entities/{name}.json').read_text(encoding='utf-8'))))
        dump(res/f'data/jugcraft/loot_table/entities/jimtafacts_{name}.json',data)
    dump(res/'data/jugcraft/loot_table/blocks/jimta_spellbooks_ice_spider_egg.json',loot(remap(json.loads((source/'loot_table/blocks/ice_spider_egg.json').read_text(encoding='utf-8')))))
    dump(res/'data/jugcraft/loot_table/blocks/jimta_spellbooks_cooked_ice_spider_egg.json',{'type':'minecraft:block','pools':[]})
    for name in ('prepared_ice_spider_egg','cooked_ice_spider_egg_from_smoking','cooked_ice_spider_egg_from_smelting','cooked_ice_spider_egg_from_campfire_cooking'):
        recipe=remap(json.loads((source/f'recipe/{name}.json').read_text(encoding='utf-8')))
        def flatten(v):
            if isinstance(v,list):return [flatten(x) for x in v]
            if isinstance(v,dict):
                if set(v)=={'item'}:return v['item']
                if set(v)=={'tag'}:return '#'+v['tag']
                return {k:flatten(x) for k,x in v.items()}
            return v
        dump(res/f'data/jugcraft/recipe/jimta_spellbooks_{name}.json',flatten(recipe))
    # These explicit native routes consume the original encounter drops without registering unused source metals.
    dump(res/'data/jugcraft/recipe/jimta_spellbooks_legionnaire_flamberge.json',{'type':'minecraft:smithing_transform','template':j('cinder_essence'),'base':j('keeper_flamberge'),'addition':'minecraft:netherite_ingot','result':{'id':j('legionnaire_flamberge')}})
    dump(res/'data/jugcraft/recipe/jimta_spellbooks_ice_spider_pheromones.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':[j('icy_fang'),'minecraft:fermented_spider_eye','minecraft:honey_bottle'],'result':{'id':j('ice_spider_pheromones'),'count':2}})
    # Simple block placement and its ground rule use current 26.3 schemas, not an obsolete random_patch wrapper.
    dump(res/'data/jugcraft/worldgen/feature/jimtafacts_ice_spider_nests.json',{'type':'minecraft:simple_block','to_place':{'id':j('ice_spider_egg'),'properties':{'frosted':'true'}}})
    dump(res/'data/jugcraft/worldgen/placed_feature/jimtafacts_ice_spider_nests.json',{'feature':'jugcraft:jimtafacts_ice_spider_nests','placement':[{'type':'minecraft:rarity_filter','chance':12},{'type':'minecraft:in_square'},{'type':'minecraft:heightmap','heightmap':'MOTION_BLOCKING_NO_LEAVES'},{'type':'minecraft:biome'},{'type':'minecraft:block_predicate_filter','predicate':{'type':'minecraft:matching_blocks','blocks':'minecraft:air'}}]})
    sound_path=res/'assets/jugcraft/sounds.json';sounds=json.loads(sound_path.read_text(encoding='utf-8'));original_sounds=json.loads((art/'sounds.json').read_text(encoding='utf-8'))
    for native,original in {
        'ice_spider_howl':'entity.ice_spider.howl','ice_spider_ambient':'entity.ice_spider.ambient','ice_spider_hurt':'entity.ice_spider.hurt','ice_spider_death':'entity.ice_spider.death','ice_spider_swing':'entity.ice_spider.swing','ice_spider_grapple_latch':'entity.ice_spider.grapple_latch','ice_spider_grapple_spit':'entity.ice_spider.grapple_spit',
        'keeper_idle':'entity.citadel_keeper.idle','keeper_hurt':'entity.citadel_keeper.hurt','keeper_death':'entity.citadel_keeper.death'}.items():
        names=[]
        for sound in original_sounds[original]['sounds']:
            relative=sound.split(':',1)[1];copy(art/f'sounds/{relative}.ogg',res/f'assets/jugcraft/sounds/jimtafacts/spellbooks/{relative}.ogg');names.append('jugcraft:jimtafacts/spellbooks/'+relative)
        sounds['jimtafacts.'+native]={'sounds':names,'subtitle':'subtitles.jugcraft.jimtafacts.'+native};lang['subtitles.jugcraft.jimtafacts.'+native]=native.replace('_',' ').capitalize()
    dump(sound_path,sounds)
