"""Translate supplied Ars layouts, inventories and generation data; no library code is executed."""
import hashlib
from pathlib import Path
import nouveau_nbt
from nouveau_content import STATIONS


def generate(source,res,read,write,registry):
    base=source/'data/ars_additions'
    stations=STATIONS|{'ender_source_jar','source_spawner','enchanting_wixie_cauldron','warp_nexus'}
    def state(value):
        v=dict(value);name=v.pop('Name',v.get('id','minecraft:air'));v['id']=registry(name)
        props=v.pop('Properties',v.get('properties',{})).copy();short=name.split(':')[-1]
        if short in stations:
            allowed={'facing','lit','fill'}|({'half','waterlogged','requires_source'} if short=='warp_nexus' else set())
            if 'powered' in props:props['lit']=props.pop('powered')
            props={k:v for k,v in props.items() if k in allowed}
        elif 'magelight_lantern' in short:
            props={k:v for k,v in props.items() if k in {'hanging','waterlogged','light_level','color'}}
        if props:v['properties']=props
        else:v.pop('properties',None)
        return v
    def convert(v):
        if isinstance(v,str):return registry(v)
        if isinstance(v,list):return [convert(x) for x in v]
        if not isinstance(v,dict):return v
        if 'Name' in v and isinstance(v['Name'],str) and ':' in v['Name']:return {k:convert(x) for k,x in state(v).items()}
        return {k:convert(x) for k,x in v.items()}
    def compound(value):return (10,value)
    def string(value):return (8,value)
    def unwrap(v):
        kind,value=v
        if kind==10:return {k:unwrap(x) for k,x in value.items()}
        if kind==9:return [unwrap((value[0],x)) for x in value[1]]
        return value
    def wrap(v):
        if isinstance(v,dict):return compound({k:wrap(x) for k,x in v.items()})
        if isinstance(v,list):
            values=[wrap(x) for x in v];return (9,(values[0][0] if values else 10,[x[1] for x in values]))
        if isinstance(v,str):return string(v)
        if isinstance(v,bool):return (1,int(v))
        return (6,v) if isinstance(v,float) else (3,v)
    def item(v):
        value=convert(v);value['count']=max(1,value.pop('Count',value.get('count',1)))
        old=value.pop('tag',{});components=value.setdefault('components',{})
        if 'Potion' in old:
            potion=old.pop('Potion').replace('_potion','')
            components['minecraft:potion_contents']={'potion':potion}
        if 'Damage' in old:components['minecraft:damage']=old.pop('Damage')
        if 'display' in old:
            display=old.pop('display')
            if 'Name' in display:components['minecraft:custom_name']=display['Name']
            if 'Lore' in display:components['minecraft:lore']=display['Lore']
        if 'Enchantments' in old:components['minecraft:enchantments']={e['id']:e['lvl'] for e in old.pop('Enchantments')}
        if old:components['minecraft:custom_data']=old
        if not components:value.pop('components',None)
        return value
    def preset(spell):
        parts=spell.get('recipe',[])
        if not parts:return None
        name='structure_spell_'+hashlib.sha256('|'.join(parts).encode()).hexdigest()[:12]
        write(res/f'data/jugcraft/nouveau_recipe/ars_additions/presets/{name}.json',{'type':'caster_tome','name':spell.get('name','Library inscription'),'spell':parts})
        return 'jugcraft:ars_additions/presets/'+name
    def entity_data(v):
        # Captured templates get fresh identities on load, never reused template UUIDs.
        retained={k:convert(x) for k,x in v.items() if k in {'id','Health','Variant','variant','Color','Sheared','Age','Size','CustomName','PersistenceRequired'}}
        return retained
    def block_data(v,block=''):
        original=v.get('id',block);short=original.split(':')[-1];out=convert(v)
        if short in stations:
            out={'id':'jugcraft:nouveau_workshop','nouveau_schema':1}
            if 'source' in v and short!='ender_source_jar':out['ley']=max(0,min(1000,int(v['source'])//100))
            cargo=[]
            for key in ('itemStack','armorStack'):
                if v.get(key,{}).get('id') not in (None,'minecraft:air'):cargo.append(dict(item(v[key]),Slot=0))
            if cargo:out['Items']=cargo
            if 'filterStack' in v:out['filter']=item(v['filterStack'])
            if 'neededCount' in v:out['threshold']=max(1,min(64,v['neededCount']))
            if 'inverted' in v:out['inverted']=bool(v['inverted'])
            if 'ritualID' in v:out['ritual']=v['ritualID'].split(':')[-1].removeprefix('ritual_')
            if 'entityTag' in v:out['captured']=entity_data(v['entityTag'])
            spell=v.get('spell') or v.get('spell_caster',{}).get('spells',{}).get('0')
            if spell:
                ref=preset(spell)
                if ref:out['ars_preset']=ref
        elif short in {'falseweave','ghostweave','mirrorweave','sky_block'}:
            out={'id':'jugcraft:nouveau_weave','facade':state(v.get('mimic_state',{'Name':'minecraft:stone'})),'showFacade':bool(v.get('showFacade',False))}
        elif short=='storage_lectern':out={'id':'jugcraft:bookwyrm_lectern'}
        elif original.startswith(('ars_nouveau:','ars_additions:')) and ('sconce' in short or 'lantern' in short):return None
        if 'Items' in out:out['Items']=[dict(item(x),Slot=x.get('Slot',0)) for x in out['Items']]
        return out
    def typed(value):
        # Preserve widths and arrays for untouched vanilla fields (UUIDs, pose and profiles).
        kind,data=value
        if kind==8:return (8,registry(data))
        if kind==9:return (9,(data[0],[typed((data[0],x))[1] for x in data[1]]))
        if kind!=10:return value
        out={k:typed(v) for k,v in data.items() if k not in {'ForgeCaps','ForgeData','CanUpdate'}}
        if 'id' in data and ('Count' in data or 'count' in data):return wrap(item(unwrap(value)))
        return (10,out)
    def structure_entity(value):
        out=typed(value)[1]
        out.pop('UUID',None);out.pop('Attributes',None)
        for name in ('Item','item'):
            if name in out:out['item']=out.pop(name)
        equipment={}
        for legacy,names in [('ArmorItems',('feet','legs','chest','head')),('HandItems',('mainhand','offhand'))]:
            if legacy in out:
                for name,stack in zip(names,out.pop(legacy)[1][1]):
                    if stack.get('id',(8,'minecraft:air'))[1]!='minecraft:air':equipment[name]=(10,stack)
        if equipment:out['equipment']=(10,equipment)
        return (10,out)
    for path in (base/'structure').rglob('*.nbt'):
        kind,name,root=nouveau_nbt.read(path.read_bytes())
        original_palette=[unwrap((10,x)) for x in root['palette'][1][1]]
        root['palette']=(9,(10,[wrap(state(v))[1] for v in original_palette]))
        for block in root.get('blocks',(9,(10,[])))[1][1]:
            if 'nbt' not in block:continue
            old=block['nbt'];original=old[1].get('id',(8,''))[1]
            if original.startswith(('ars_nouveau:','ars_additions:')):
                data=block_data(unwrap(old),original_palette[block['state'][1]].get('Name',''))
                if data is None:block.pop('nbt')
                else:block['nbt']=wrap(data)
            else:block['nbt']=typed(old)
        for entity in root.get('entities',(9,(10,[])))[1][1]:
            if 'nbt' in entity:entity['nbt']=structure_entity(entity['nbt'])
        root['DataVersion']=(3,5023)
        target=res/'data/jugcraft/structure'/('ars_additions_'+path.relative_to(base/'structure').as_posix())
        target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(nouveau_nbt.write((kind,name,root)))
    for path in (base/'worldgen').rglob('*.json'):
        data=read(path);kind=path.relative_to(base/'worldgen').parts[0]
        if kind=='structure':
            for group in data.get('spawn_overrides',{}).values():
                for spawn in group.get('spawns',[]):
                    if 'minCount' in spawn:spawn['count']={'type':'minecraft:uniform','min_inclusive':spawn.pop('minCount'),'max_inclusive':spawn.pop('maxCount')}
        if kind=='processor_list':
            for proc in data['processors']:
                for rule in proc.get('rules',[]):
                    modifier=rule.get('block_entity_modifier',{})
                    if modifier.get('type')=='minecraft:append_static':
                        original=rule.get('output_state',{}).get('Name','')
                        modifier['data']=block_data(modifier.get('data',{}),original) or {}
                        # Appending metadata must not reset inventory/source unless the source supplied it.
                        modifier['data'].pop('nouveau_schema',None);modifier['data'].pop('id',None)
        relative=path.relative_to(base/'worldgen');write(res/'data/jugcraft/worldgen'/relative.parts[0]/('ars_additions_'+Path(*relative.parts[1:]).as_posix()),convert(data))
    for path in (base/'tags').rglob('*.json'):
        relative=path.relative_to(base/'tags');parts=2 if relative.parts[0]=='worldgen' else 1
        target=res/'data/jugcraft/tags'/Path(*relative.parts[:parts])/('ars_additions_'+Path(*relative.parts[parts:]).as_posix())
        write(target,convert(read(path)))
    for path in (base/'loot_table/chests').glob('*.json'):
        data=read(path)
        def loot(v):
            if isinstance(v,list):return [loot(x) for x in v]
            if not isinstance(v,dict):return registry(v) if isinstance(v,str) else v
            if v.get('function')=='ars_additions:exploration_scroll':
                return {'function':'minecraft:set_components','components':{'jugcraft:ars_exploration_target':registry('#'+v.get('tag','ars_additions:on_explorer_warp_scroll'))}}
            return {k:loot(x) for k,x in v.items()}
        write(res/'data/jugcraft/loot_table'/('ars_additions_chests/'+path.name),loot(data))

    injections=[]
    for path in (base/'loot_modifiers').glob('*.json'):
        data=read(path);tables=[]
        def table_ids(v):
            if isinstance(v,dict):
                if 'loot_table_id' in v:tables.append(registry(v['loot_table_id']))
                for x in v.values():table_ids(x)
            elif isinstance(v,list):
                for x in v:table_ids(x)
        table_ids(data)
        injections.append({'tables':tables,**{k:v for k,v in data.items() if k.endswith(('_chance','_rolls'))}})
    write(res/'jugcraft/nouveau/ars_additions_loot_injections.json',injections)
