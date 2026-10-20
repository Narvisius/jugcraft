"""Complete native owner resources; preserve source PNGs and source library files."""
import shutil
from menagerie_content import ROOT, RES, ASSET, DATA, read, write


def remap(value):
    if isinstance(value, str):
        if value.startswith('goblinsanddungeons:'):
            return 'jugcraft:menagerie/landscape/' + value.split(':', 1)[1]
        return {'farmersdelight:block/pie_bottom':'jugcraft:menagerie/ecology/block/pie_bottom',
                'farmersdelight:block/pie_side':'jugcraft:menagerie/ecology/block/pie_side',
                'rusticdelight:block/tray_bottom':'jugcraft:menagerie/ecology/block/tray_bottom'}.get(value,value)
    if isinstance(value,list):return [remap(v) for v in value]
    if isinstance(value,dict):return {k:remap(v) for k,v in value.items()}
    return value


def client_atlases():
    """Generated-item layers must share the 26.3 item atlas with the vanilla bucket."""
    atlas=RES/'assets/minecraft/atlases/particles.json'
    definition=read(atlas) if atlas.exists() else {'sources':[]}
    for folder in sorted((ASSET/'textures/menagerie').glob('*/particle')):
        source={'type':'minecraft:directory','source':f'menagerie/{folder.parent.name}/particle','prefix':f'menagerie/{folder.parent.name}/'}
        if source not in definition['sources']:definition['sources'].append(source)
    write(atlas,definition)
    copies=[]
    for p in (ASSET/'models/menagerie/cavern/item').glob('*_bucket.json'):
        d=read(p)
        for key,ref in list(d.get('textures',{}).items()):
            old='jugcraft:menagerie/cavern/item/bucket_content/'
            new='jugcraft:item/owner_cavern/bucket_content/'
            if not isinstance(ref,str) or not ref.startswith((old,new)):continue
            name=ref.rsplit('/',1)[-1]
            source=ASSET/f'textures/menagerie/cavern/item/bucket_content/{name}.png'
            target=ASSET/f'textures/item/owner_cavern/bucket_content/{name}.png'
            target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(source,target)
            sidecar=source.with_suffix('.png.mcmeta')
            if sidecar.exists():shutil.copy2(sidecar,target.with_suffix('.png.mcmeta'))
            d['textures'][key]=new+name;copies.append({'source':source.relative_to(ASSET).as_posix(),'runtime':target.relative_to(ASSET).as_posix()})
        write(p,d)
    for p in list((ASSET/'models/household').glob('fan_blade_*.json'))+list((ASSET/'models/household/block').glob('water_*.json')):
        d=read(p);textures=d.setdefault('textures',{})
        if 'particle' not in textures:textures['particle']='#'+next(iter(textures));write(p,d)
    write(ROOT/'art/owner-library/catalog/edited-2026-10-18-part2/item-atlas-adaptations.json',{'copies':copies,'reason':'Unchanged owner overlay PNGs copied into the native item atlas; all bucket layers now use one atlas.'})


def wearable_metadata(name):
    """26.3 requires a real nonempty layer even when GeckoLib supplies the worn geometry."""
    source=ASSET/('textures/menagerie/companion/armor/acorn.png' if name=='companion_acorn' else 'textures/menagerie/grove/armor/'+('flower_crown' if name=='glowing_flower_crown' else name)+'.png')
    target=ASSET/f'textures/entity/equipment/humanoid/menagerie/{name}.png'
    target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source,target)
    write(ASSET/f'equipment/menagerie/{name}.json',{'layers':{'humanoid':[{'texture':'jugcraft:menagerie/'+name}]}})


def runtime_loading():
    """Fix the concrete metadata/audio failures found by the real resource loader."""
    repairs=[]
    for name in ('companion_acorn','flower_crown','glowing_flower_crown','sporeling_backpack'):wearable_metadata(name)
    sounds=read(ASSET/'sounds.json')
    for key,event in sounds.items():
        entries=[]
        for entry in event.get('sounds',[]):
            name=entry if isinstance(entry,str) else entry['name'];replacement=name
            if name.startswith('jugcraft:menagerie/') and not (ASSET/'sounds'/(name.split(':',1)[1]+'.ogg')).exists() and (isinstance(entry,str) or entry.get('type','file')=='file'):
                rest=name.removeprefix('jugcraft:menagerie/');group,_,path=rest.partition('/')
                if group in ('ecology','mutant') and path.startswith(('mob/turtle/egg/','mob/llama/','mob/chicken/','mob/illusion_illager/')):replacement='minecraft:'+path
                elif group=='cavern' and 'luxtructosaurus_breathe_' in path:
                    replacement=name.replace('luxtructosaurus_breathe_','luxtructosaurus_breath_')
                    if not (ASSET/'sounds'/(replacement.split(':',1)[1]+'.ogg')).exists():replacement=None
                elif group=='cavern' and 'luxtructosaurus_snort_' in path:replacement=name.replace('luxtructosaurus_snort_','luxtructosaurus_idle_')
                elif group=='cavern' and path.startswith('block/purple_soda/purple_soda_swim_'):replacement=None
                if replacement!=name:repairs.append({'event':key,'old':name,'new':replacement,'reason':'Retain vanilla namespace, correct supplied filename, or use available native audio variants.'})
            if replacement is None:continue
            if isinstance(entry,str):entries.append(replacement)
            else:entries.append({**entry,'name':replacement})
        if event.get('sounds') and not entries:raise ValueError('Audio repair would silence event '+key)
        event['sounds']=entries
    write(ASSET/'sounds.json',sounds)
    # Explicit OR branches keep the supplied petal amounts distinct in the current blockstate parser.
    catalog_path=RES/'jugcraft/menagerie/landscape.json';catalog=read(catalog_path)
    for name in ('landscape_purple_wildflowers','landscape_white_petals'):
        block=catalog['blocks'][name];block['kind']='flower';block['definition']['properties']['flower_amount']=['1','2','3','4']
        p=ASSET/f'blockstates/{name}.json';d=read(p)
        for part in d['multipart']:
            condition=part.get('when',{});amount=condition.get('flower_amount','')
            if '|' in amount:part['when']={'OR':[{**condition,'flower_amount':n} for n in amount.split('|')]}
        write(p,d)
    write(catalog_path,catalog)
    native_pies()
    write(ROOT/'art/owner-library/catalog/edited-2026-10-18-part2/runtime-loading-repairs.json',{'audio':repairs,'wearables':'Valid nonempty equipment metadata with unchanged supplied PNGs; GeckoLib still renders the original geometry.','petals':'Explicit OR states preserve each amount.','originalsChanged':False})


def native_pies():
    """Four native bite stages replace the absent external pie template parents."""
    def piece(x0,z0,x1,z1,cut=(),omit=()):
        sides={'north':[x0,12,x1,16],'south':[x0,12,x1,16],'east':[z0,12,z1,16],'west':[z0,12,z1,16],
               'up':[x0,z0,x1,z1],'down':[x0,z0,x1,z1]}
        return {'from':[x0,0,z0],'to':[x1,4,z1],'faces':{
            face:{'uv':uv,'texture':'#'+('inner' if face in cut else 'top' if face=='up' else 'bottom' if face=='down' else 'side')}
            for face,uv in sides.items() if face not in omit}}
    stages=[[piece(1,1,15,15)],
            [piece(1,8,15,15,('north',)),piece(1,1,8,8,('east',),('south',))],
            [piece(1,8,15,15,('north',))],[piece(1,8,8,15,('north','east'))]]
    for index,elements in enumerate(stages):
        p=ASSET/('models/menagerie/ecology/block/maple_pie'+('' if index==0 else '_slice'+str(index))+'.json')
        d=read(p);d['parent']='minecraft:block/block';d['elements']=elements;d['textures']['particle']='#side';write(p,d)


def main():
    for folder in ('models/menagerie','blockstates'):
        for path in (ASSET/folder).rglob('*.json'):
            before=read(path);after=remap(before)
            if 'models/' in folder and after.get('elements'):
                textures=after.get('textures',{})
                for element in after['elements']:
                    for side,face in element.get('faces',{}).items():
                        if face.get('texture')=='#missing':
                            key=('3' if side in ('up','down') else '4') if path.stem=='fern_thatch' else '1' if path.stem=='curly_fern_top' else '0'
                            face['texture']='#'+key
                # Source resource IDs without namespaces are vanilla by definition.
            
            if before!=after:write(path,after)
    for name in ('pie_bottom','pie_side','tray_bottom'):
        src=ROOT/f'art/owner-library/originals/Blocks/farming and food textures/{name}.png'
        dest=ASSET/f'textures/menagerie/ecology/block/{name}.png'
        dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(src,dest)
    lang=read(ASSET/'lang/en_us.json')
    for group in ('household','ecology','mutant','beast','landscape','cavern'):
        path=RES/f'jugcraft/menagerie/{group}.json';catalog=read(path)
        for id,b in catalog['blocks'].items():
            name=b['source'];kind=b.get('kind','')
            lang.setdefault('block.jugcraft.'+id,name.replace('_',' ').title())
            # Catalogs explicitly distinguish inventory items from wall/plant/fluid states.
            if group=='landscape' and id not in catalog['items'] and kind not in ('wall_sign','wall_hanging_sign','potted','fluid'):
                catalog['items'][id]={'source':name,'food':False,'armor':''}
            if kind=='fluid':catalog['items'].pop(id,None)
            if group=='cavern' and (kind=='potted' or name in ('ambersol_light','archaic_vine_plant')):
                catalog['items'].pop(id,None)
            has_item=group in ('household','beast') or id in catalog['items']
            if not has_item:
                # These block-only states are never registered as inventory items.
                (ASSET/f'items/{id}.json').unlink(missing_ok=True)
            if has_item:
                definition=ASSET/f'items/{id}.json'
                prefix='household' if group=='household' else 'menagerie/'+group
                item_model=f'{prefix}/item/{name}'
                model_path=ASSET/f'models/{item_model}.json'
                if not model_path.exists():
                    state=read(ASSET/f'blockstates/{id}.json')
                    variants=list(state.get('variants',{}).values())
                    entry=variants[0] if variants else state['multipart'][0]['apply']
                    if isinstance(entry,list):entry=entry[0]
                    write(model_path,{'parent':entry['model']})
                if not definition.exists():write(definition,{'model':{'type':'minecraft:model','model':'jugcraft:'+item_model}})
            drop=id if has_item else id.replace('_wall_hanging_sign','_hanging_sign').replace('_wall_sign','_sign').replace('_wall_skull','_skull')
            loot_path=DATA/f'loot_table/blocks/{id}.json'
            if not loot_path.exists() or (group=='beast' and not read(loot_path).get('pools')):
                entries=[]
                if drop in catalog['items'] or group in ('household','beast'):entries=[{'type':'minecraft:item','name':'jugcraft:'+drop}]
                elif kind=='potted':entries=[{'type':'minecraft:item','name':'minecraft:flower_pot'},{'type':'minecraft:item','name':'jugcraft:'+group+'_'+name.removeprefix('potted_')}]
                write(loot_path,{'type':'minecraft:block','pools':[{'rolls':1,'entries':[e],'condition':{'type':'minecraft:survives_explosion'}} for e in entries]})
        if group=='cavern':
            write(ASSET/'models/menagerie/cavern/item/underground_cabin_explorer_map.json',{'parent':'jugcraft:menagerie/cavern/item/cave_map'})
        write(path,catalog)
    # Disambiguate genuinely distinct inventory entries in search results.
    from collections import defaultdict
    names=defaultdict(list)
    for key,value in lang.items():
        if key.startswith('item.jugcraft.') and key.count('.')==2:names[value].append(key)
    adjectives={'ecology':'Grove','landscape':'Wildland','beast':'Wild','cavern':'Cave','fauna':'Meadow'}
    for name,keys in names.items():
        if len(keys)<2:continue
        for key in keys:
            id=key.split('.')[-1];group,_,part=id.partition('_')
            if group in adjectives:lang[key]=adjectives[group]+' '+part.replace('_',' ').title()
    write(ASSET/'lang/en_us.json',lang)
    # Supplied armor overlay metadata, not an item registered by Jugcraft.
    (ASSET/'items/beast_insulated_with_fur.json').unlink(missing_ok=True)
    import diagonal_connections as dg
    dg.write_all(write,ASSET,RES/'assets/minecraft')
    write(RES/'data/jugcraft/tags/block/connects_diagonally.json',{'replace':False,'values':dg.blocks()})
    p=RES/'data/minecraft/tags/block/walls.json';d=read(p)
    d['values']=sorted(set(d['values'])|{'jugcraft:diagonal_'+n for ns,n in dg.all_walls()})
    write(p,d)
    from menagerie_data import loot
    for relative in ('blocks/household_door_mat','advancements/landscape_all_biomes'):
        path=DATA/f'loot_table/{relative}.json';write(path,loot(read(path)))
    client_atlases()
    runtime_loading()


if __name__=='__main__':main()
