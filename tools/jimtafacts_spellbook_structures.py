"""Data-only migration of the complete supplied spellbook jigsaw layout collection.

No source programs run. Missing third-party statue assets are recorded and only
their decorative cells are omitted; building layouts and source originals stay.
"""
from pathlib import Path
import collections
import hashlib
import json
import re
from nouveau_nbt import read,write

PREFIX='jugcraft:jimtafacts/spellbooks/'
ENTITIES=set('pyromancer cryomancer archevoker cultist priest apothecarist necromancer cursed_armor_stand dead_king dead_king_corpse fire_boss ice_spider citadel_keeper catacombs_zombie magehunter_vindicator frozen_humanoid'.split())
PROCESSORS={'foundation':'jimtafacts_foundation','handle_lit_blocks':'jimtafacts_handle_lit','clear_vault_data':'jimtafacts_clear_vault','weather_copper':'jimtafacts_weather_copper'}
MISSING_STATUES={'irons_lib:player_statue','irons_lib:statue','irons_patreon_lib:statue'}
WORKSHOPS={'alchemist_cauldron','blood_cauldron','pedestal','bone_vault','cinderous_vault','wisewood_chiseled_bookshelf','cinderous_soul_rune'}
ATTR_PREFIXES=('minecraft:generic.','minecraft:player.','minecraft:zombie.')

def load(path):
    # Strip comments without corrupting strings; several original worldgen files use JSON comments.
    source=re.sub(r'"(?:\\.|[^"\\])*"|//[^\n\r]*|/\*[\s\S]*?\*/',lambda m:m[0] if m[0].startswith('"') else '',path.read_text(encoding='utf-8-sig'))
    return json.loads(source)

def registry_ref(value,kind='item'):
    if not isinstance(value,str):return value
    if value=='minecraft:chain':return 'minecraft:iron_chain'
    if value.startswith('#'):return '#'+registry_ref(value[1:],'tag')
    if not value.startswith('irons_spellbooks:'):return value
    name=value.split(':',1)[1]
    if kind in ('resource','tag','loot','pool'):return PREFIX+name
    if kind=='entity' or name in ENTITIES:return 'jugcraft:jimtafacts_'+name
    if kind=='processor':return 'jugcraft:'+PROCESSORS[name]
    return 'jugcraft:jimta_spellbooks_'+name

def node(kind,value):return (kind,value)
def string(value):return (8,value)
def compound(value):return (10,value)
def sequence(kind,values):return (9,(kind,values))
def text_component(value):
    if not isinstance(value,str):return value
    try:parsed=json.loads(value)
    except (ValueError,TypeError):return string(value)
    return as_tag(parsed)
def as_tag(value):
    if isinstance(value,bool):return (1,int(value))
    if isinstance(value,int):return (3,value)
    if isinstance(value,float):return (6,value)
    if isinstance(value,str):return string(value)
    if isinstance(value,dict):return compound({k:as_tag(v) for k,v in value.items()})
    if isinstance(value,list):
        entries=[as_tag(v) for v in value];kind=entries[0][0] if entries else 0
        if any(k!=kind for k,_ in entries):return sequence(10,[{'text':string(str(v))} for v in value])
        return sequence(kind,[v for _,v in entries])
    return string(str(value))

def item(data):
    """Legacy item stacks/components, including source scrolls, without foreign component IDs."""
    if not data or data.get('id',string('minecraft:air'))[1]=='minecraft:air':return {}
    result=dict(data);original=result['id'][1];result['id']=string(registry_ref(original))
    if 'Count' in result:result['count']=(3,result.pop('Count')[1])
    legacy=result.pop('tag',compound({}))[1];components=dict(result.get('components',compound({}))[1])
    for old,new in (('Damage','minecraft:damage'),('Unbreakable','minecraft:unbreakable')):
        if old in legacy and (old!='Unbreakable' or legacy[old][1]):components[new]=compound({}) if old=='Unbreakable' else legacy[old]
    potion=legacy.get('Potion') or components.get('minecraft:potion_contents',compound({}))[1].get('potion')
    if potion:
        if potion[1].startswith('irons_spellbooks:'):components['minecraft:potion_contents']=compound({'potion':string(registry_ref(potion[1]))})
        else:components['minecraft:potion_contents']=compound({'potion':potion})
    display=legacy.get('display',compound({}))[1]
    if 'Name' in display:components['minecraft:custom_name']=text_component(display['Name'][1])
    for key in ('minecraft:custom_name','minecraft:item_name'):
        if key in components and components[key][0]==8:components[key]=text_component(components[key][1])
    spells=components.pop('irons_spellbooks:spell_container',legacy.get('ISB_Spells',compound({})))[1]
    learned=[]
    for entry in spells.get('data',sequence(10,[]))[1][1]:
        name=entry.get('id',string(''))[1].split(':')[-1]
        if name:learned.append('angel_wings' if name=='angel_wing' else name)
    if original=='irons_spellbooks:scroll' and learned:result['id']=string('jugcraft:jimta_spellbooks_scroll_'+learned[0])
    elif learned:components['jugcraft:jimtafacts_spellbook_contents']=sequence(8,learned)
    # Legacy source attributes/Forge capabilities are replaced by the registered native item's behavior.
    components={k:v for k,v in components.items() if not k.startswith(('irons_spellbooks:','neoforge:','forge:','curios:'))}
    if components:result['components']=compound(components)
    else:result.pop('components',None)
    return result

def entity(data):
    out=dict(data);original=out.get('id',string(''))[1];out['id']=string(registry_ref(original,'entity'))
    # Template copies must not share persistent identity, memories, combat debt or author-world coordinates.
    for key in ('UUID','UUIDMost','UUIDLeast','Brain','HomePos','AngryAt','Leash','Attributes','attributes','ForgeCaps','forge:spawn_type','NeoForgeData','cardinal_components','ISB_Spells','spellSelection','spellData','heartStopAccumulatedDamage'):
        out.pop(key,None)
    equipment=dict(out.get('equipment',compound({}))[1]);chances=dict(out.get('drop_chances',compound({}))[1])
    for source,slots in (('ArmorItems',('feet','legs','chest','head')),('HandItems',('mainhand','offhand'))):
        for slot,stack in zip(slots,out.pop(source,sequence(10,[]))[1][1]):
            value=item(stack)
            if value:equipment[slot]=compound(value)
    for source,slots in (('ArmorDropChances',('feet','legs','chest','head')),('HandDropChances',('mainhand','offhand'))):
        for slot,chance in zip(slots,out.pop(source,sequence(5,[]))[1][1]):chances[slot]=(5,chance)
    if equipment:out['equipment']=compound(equipment)
    if chances:out['drop_chances']=compound(chances)
    if original.startswith('irons_spellbooks:'):
        out['PersistenceRequired']=(1,1)
        if original=='irons_spellbooks:citadel_keeper':
            if not equipment:out['equipment']=compound({'mainhand':compound({'id':string('jugcraft:jimta_spellbooks_keeper_flamberge'),'count':(3,1)})})
            if 'Restored' in out:out['restored']=out.pop('Restored')
    if 'CustomName' in out and out['CustomName'][0]==8:out['CustomName']=text_component(out['CustomName'][1])
    return out

def visit(kind,value,key=''):
    if kind==9:return (value[0],[visit(value[0],v,key) for v in value[1]])
    if kind==8:
        if key in ('LootTable','loot_table','items_to_drop_when_ominous','pool','target','name') or key=='data' and value.startswith('irons_spellbooks:'):return registry_ref(value,'resource')
        if key=='final_state' and value.startswith('irons_spellbooks:'):
            start,sep,tail=value.partition('[');return registry_ref(start)+('['+tail if sep else '')
        if key in ('id','Name'):return registry_ref(value)
        return value
    if kind!=10:return value
    data=dict(value)
    if 'id' in data and ('count' in data or 'Count' in data):data=item(data)
    elif 'id' in data and (key=='entity' or data['id'][1].split(':')[-1] in ENTITIES or 'Pos' in data or 'Rotation' in data):data=entity(data)
    result={name:(tag,visit(tag,v,name)) for name,(tag,v) in data.items()}
    if 'Name' in result and result['Name'][0]==8 and ':' in result['Name'][1]:
        result['id']=result.pop('Name')
        if 'Properties' in result:result['properties']=result.pop('Properties')
    return result

def adapt_template(raw,path,report):
    kind,name,root=read(raw);root=visit(kind,root)
    palettes=root.get('palettes')
    lists=[root['palette'][1][1]] if 'palette' in root else [v for v in palettes[1][1]] if palettes else []
    missing=set()
    for palette in lists:
        for index,state in enumerate(palette):
            ident=state.get('id',string(''))[1]
            if ident in MISSING_STATUES:
                missing.add(index);palette[index]={'id':string('minecraft:air')}
                report['omitted_statue_palettes'].append({'template':path,'palette_index':index,'missing_source':ident})
    for entry in root.get('blocks',sequence(10,[]))[1][1]:
        state=entry['state'][1]
        if state in missing:entry.pop('nbt',None);continue
        nbt=entry.get('nbt',compound({}))[1]
        if not nbt:continue
        block=lists[0][state]['id'][1] if lists else ''
        short=block.removeprefix('jugcraft:jimta_spellbooks_')
        if short in WORKSHOPS:
            nbt['id']=string('jugcraft:jimtafacts_spell_workshop')
            # Native nodes start unowned and unsolved. No author-world vault claim history survives.
            nbt.pop('server_data',None);nbt.pop('shared_data',None)
            if 'heldItem' in nbt:
                held=nbt.pop('heldItem')[1]
                if held:nbt['items']=sequence(10,[held]+[{} for _ in range(8)])
            if 'Items' in nbt:
                slots=[{} for _ in range(9)]
                for stack in nbt.pop('Items')[1][1]:
                    slot=stack.pop('Slot',(1,0))[1]
                    if 0<=slot<9:slots[slot]=stack
                nbt['items']=sequence(10,slots)
        if short=='pocket_dimension_portal_frame':
            entry.pop('nbt',None);continue
        if short=='portal_frame':
            # Frame orientation remains; links and author-world identities must never be imported.
            nbt.clear();nbt['id']=string('jugcraft:jimtafacts_portal_frame')
        if nbt.get('id')==string('minecraft:vault'):
            nbt.pop('server_data',None);nbt.pop('shared_data',None)
    # 26.3 palette/item/equipment schemas are authored here; leave no obsolete source data fixer task.
    root['DataVersion']=(3,5023)
    report['templates'].append({'path':path,'source_sha256':hashlib.sha256(raw).hexdigest(),'blocks':len(root.get('blocks',sequence(10,[]))[1][1]),'entities':len(root.get('entities',sequence(10,[]))[1][1])})
    return write((kind,name,root))

def worldgen(value,key=''):
    if isinstance(value,list):return [worldgen(v,key) for v in value]
    if isinstance(value,str):
        if value=='irons_spellbooks:individual_terrain_element':return 'jugcraft:jimtafacts_individual_terrain'
        if key=='processor_type' and value.startswith('irons_spellbooks:'):return registry_ref(value,'processor')
        if key in ('block','Name','id'):return registry_ref(value)
        if key=='type' and value.startswith('irons_spellbooks:'):return registry_ref(value,'entity')
        return registry_ref(value,'resource')
    if not isinstance(value,dict):return value
    out={k:worldgen(v,k) for k,v in value.items() if not k.startswith('_')}
    if 'Name' in out:
        out['id']=out.pop('Name')
        if 'Properties' in out:out['properties']={k:str(v).lower() for k,v in out.pop('Properties').items()}
    if 'minCount' in out:
        low,high=out.pop('minCount'),out.pop('maxCount');out['count']=low if low==high else {'type':'minecraft:uniform','min_inclusive':low,'max_inclusive':high}
    if 'processors' in out and isinstance(out['processors'],list):out['processors']=[p for p in out['processors'] if p.get('processor_type')!='irons_lib:randomize_player_statue']
    return out

def generate(copy,texture,dump,res,art,lang):
    source=art.parents[1]/'data/irons_spellbooks'
    report={'templates':[],'omitted_statue_palettes':[],'source':'Owner-supplied Oct23 Jimtafacts; original source notices retained.','validation':'Authored; combined runtime tests deferred.'}
    for path in sorted((source/'structure').rglob('*.nbt')):
        relative=path.relative_to(source/'structure').as_posix();out=res/'data/jugcraft/structure/jimtafacts/spellbooks'/relative
        out.parent.mkdir(parents=True,exist_ok=True);out.write_bytes(adapt_template(path.read_bytes(),relative,report))
    for folder in ('structure','structure_set','template_pool','processor_list'):
        for path in sorted((source/'worldgen'/folder).rglob('*.json')):
            if path.name.startswith('_'):continue
            data=worldgen(load(path));relative=path.relative_to(source/'worldgen'/folder)
            # Registry objects remain available to existing saves; new placements follow the feature switch.
            if folder=='structure_set':data['fabric:load_conditions']=[{'condition':'jugcraft:feature_enabled','feature':'concordance'}]
            dump(res/f'data/jugcraft/worldgen/{folder}/jimtafacts/spellbooks'/relative,data)
            if folder=='structure':dump(res/f'data/jugcraft/tags/worldgen/structure/jimtafacts/spellbooks/locate/{path.stem}.json',{'values':[PREFIX+path.stem]})
    for folder in ('worldgen/biome','block'):
        for path in sorted((source/'tags'/folder).rglob('*.json')):
            data=worldgen(load(path));relative=path.relative_to(source/'tags'/folder)
            if 'remove' in data:
                # Source NeoForge tag subtraction is unavailable here. Its only use excludes mountains/oceans from this snow tag.
                if relative.as_posix() not in ('has_structure/ice_spider_den.json','ice_spider_patrols.json'):raise ValueError('Unknown source subtractive tag '+str(relative))
                values=['minecraft:snowy_plains','minecraft:snowy_taiga','minecraft:ice_spikes']
                if relative.as_posix()=='ice_spider_patrols.json':values+=['minecraft:grove','minecraft:snowy_slopes','minecraft:frozen_peaks','minecraft:jagged_peaks']
                data={'replace':False,'values':values}
            dump(res/f'data/jugcraft/tags/{folder}/jimtafacts/spellbooks'/relative,data)
    dump(res.parents[2]/'docs/audits/jimtafacts-spellbook-structures.json',report)
