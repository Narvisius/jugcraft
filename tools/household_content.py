"""Native household resource adapter. Originals remain untouched; no source programs are run."""
import collections
import copy
import hashlib
import itertools
import pathlib
import shutil
from functools import lru_cache
from menagerie_content import ROOT, RES, ASSET, DATA, read, write
from menagerie_data import loot
from native_shapes import model_boxes, multipart_shapes

SOURCE = ROOT/'art/owner-library/originals/Edited-2026-10-18-Part2/furniture'
PREFIX = 'household_'
ALIASES = {'sweet_berry_jam': 'sweet_berry_jam', 'plate': 'plate'}
COPIES = []

def ident(name):
    return 'jugcraft:'+ALIASES.get(name, PREFIX+name)

def adapt(value, resource=False):
    if isinstance(value, str):
        if value.startswith('#refurbished_furniture:'):
            return '#jugcraft:household/'+value.split(':',1)[1]
        if value.startswith('refurbished_furniture:'):
            path=value.split(':',1)[1]
            return 'jugcraft:household/'+path if resource else ident(path)
        return value
    if isinstance(value, list):return [adapt(v,resource) for v in value]
    if isinstance(value, dict):return {adapt(k,resource):adapt(v,resource) for k,v in value.items()}
    return value

def copied(src,dst):
    dst.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(src,dst)
    COPIES.append({'source':src.relative_to(SOURCE).as_posix(),'runtime':dst.relative_to(RES).as_posix(),'sha256':hashlib.sha256(src.read_bytes()).hexdigest()})

@lru_cache(None)
def geometry(ref):
    if not ref.startswith('refurbished_furniture:'):return []
    p=SOURCE/'models'/(ref.split(':',1)[1]+'.json')
    if not p.exists():return []
    data=read(p)
    return data.get('elements',geometry(data.get('parent','')))

def boxes(model):
    return model_boxes(model,geometry)


def kind(name):
    if name.endswith('cutting_board'):return 'cutting'
    if name.endswith(('stove','grill')):return 'oven'
    if name.endswith('toaster'):return 'toaster'
    if name.endswith('microwave'):return 'microwave'
    if name=='frying_pan':return 'pan'
    if name.endswith('freezer'):return 'freezer'
    if name=='workbench':return 'workbench'
    if name.endswith(('storage_cabinet','drawer','crate','storage_jar','mail_box','cooler','fridge')) or name in ('post_box','recycle_bin'):return 'storage'
    if name.endswith(('chair','sofa','stool','toilet','bath')):return 'seat'
    if name.endswith(('sink','basin')):return 'sink'
    if name.endswith(('lamp','ceiling_light','lightswitch','ceiling_fan','range_hood')):return 'switch'
    if name.endswith('trampoline'):return 'trampoline'
    if name in ('television','computer'):return 'display'
    if name=='doorbell':return 'bell'
    if name.endswith('electricity_generator'):return 'generator'
    return 'decor'

def main():
    catalog={'blocks':{},'items':{},'sounds':[]}
    lang=read(ASSET/'lang/en_us.json');names=read(SOURCE/'lang/en_us.json')
    for root in ['textures','sounds']:
        for p in (SOURCE/root).rglob('*'):
            if p.is_file():copied(p,ASSET/root/'household'/p.relative_to(SOURCE/root))
    for p in (SOURCE/'models').rglob('*.json'):
        model=adapt(read(p),True)
        # Legacy overlays rely on a removed custom render type; ordinary translucent atlas sprites suffice.
        model.pop('render_type',None)
        model.pop('loader',None);model.pop('fabric:type',None)
        if 'components' in model:model['elements']=model.pop('components')
        write(ASSET/'models/household'/p.relative_to(SOURCE/'models'),model)
    for p in (SOURCE/'blockstates').glob('*.json'):
        n=p.stem
        if n in ALIASES:continue
        d=read(p);props=collections.defaultdict(set);shapes={}
        if kind(n)=='generator':d={'variants':{k.replace('powered=','lit='):v for k,v in d['variants'].items()}}
        for key,model in d.get('variants',{}).items():
            for pair in key.split(','):
                if '=' in pair:
                    k,v=pair.split('=');props[k].add(v)
            shapes[key]=boxes(model)
        def condition(v):
            for k,x in v.items():
                if isinstance(x,str):props[k].update(x.split('|'))
                elif isinstance(x,list):
                    for child in x:condition(child)
        for part in d.get('multipart',[]):condition(part.get('when',{}))
        for values in props.values():
            if values and values <= {'false','true'}:values.update(['false','true'])
        behavior=kind(n)
        if behavior in ('oven','toaster','microwave','pan','freezer'):props['lit'].update(['false','true'])
        if behavior=='sink' or n.endswith('bath'):props['water_level'].update(['0','1','2','3'])
        catalog['blocks'][PREFIX+n]={'source':n,'kind':behavior,'properties':{k:sorted(v) for k,v in props.items()},'shapes':shapes,'parts':multipart_shapes(d,geometry),'state':d}
        runtime=adapt(d,True)
        if behavior=='sink' or n.endswith('bath'):
            parts=runtime.get('multipart',[])
            if 'variants' in runtime:parts=[{'when':dict(pair.split('=') for pair in key.split(',') if '=' in pair),'apply':model} for key,model in runtime['variants'].items()]
            for level in (1,2,3):
                path=f'household/block/water_{level}'
                height=5+level*2
                write(ASSET/f'models/{path}.json',{'textures':{'water':'minecraft:block/water_still'},'elements':[{'from':[2,height,2],'to':[14,height+.01,14],'faces':{'up':{'texture':'#water','uv':[0,0,16,16],'tintindex':0}}}]})
                parts.append({'when':{'water_level':str(level)},'apply':{'model':'jugcraft:'+path}})
            runtime={'multipart':parts}
        write(ASSET/f'blockstates/{PREFIX+n}.json',runtime)
        pool={'rolls':1,'entries':[{'type':'minecraft:item','name':ident(n)}]}
        if n=='door_mat':pool['entries'][0]['functions']=[{'function':'minecraft:copy_components','source':'block_entity','include':['minecraft:custom_name']}]
        if 'type' in props and 'head' in props['type']:pool['condition']={'type':'minecraft:match_block','blocks':ident(n),'state':{'type':'bottom'}}
        write(DATA/f'loot_table/blocks/{PREFIX+n}.json',{'type':'minecraft:block','pools':[pool]})
        lang['block.jugcraft.'+PREFIX+n]=names.get('block.refurbished_furniture.'+n,n.replace('_',' ').title())
    blade=copy.deepcopy(read(SOURCE/'models/block/ceiling_fan_blade.json')['components'][0]);blade.pop('rotation',None)
    for name in catalog['blocks']:
        if not name.endswith('ceiling_fan'):continue
        source=name.removeprefix(PREFIX)
        write(ASSET/f'models/household/fan_blade_{source}.json',{'textures':{'texture':'jugcraft:household/block/'+source},'elements':[blade]})
        write(ASSET/f'items/household/fan_blade_{source}.json',{'model':{'type':'minecraft:model','model':f'jugcraft:household/fan_blade_{source}'}})
    for p in (SOURCE/'items').glob('*.json'):

        n=p.stem
        if n in ALIASES:continue
        write(ASSET/f'items/{PREFIX+n}.json',adapt(read(p),True))
        if PREFIX+n not in catalog['blocks']:
            catalog['items'][PREFIX+n]={'source':n,'food':n not in ('knife','spatula','package','television_remote','wrench')}
            lang['item.jugcraft.'+PREFIX+n]=names.get('item.refurbished_furniture.'+n,n.replace('_',' ').title())
    for p in (SOURCE/'tags').rglob('*.json'):
        rel=p.relative_to(SOURCE/'tags');write(DATA/'tags'/rel.parts[0]/'household'/pathlib.Path(*rel.parts[1:]),adapt(read(p)))
    counts=collections.Counter()
    for p in (SOURCE/'recipe').rglob('*.json'):
        d=adapt(read(p));typ=d['type'].split(':',1)[1];out='household/'+p.relative_to(SOURCE/'recipe').as_posix()
        # All construction costs are retained; the workbench uses an inventory-backed recipe catalogue.
        if typ=='household_workbench_constructing':d={'type':'jugcraft:household_construction','ingredients':d['materials'],'result':d['result']}
        elif typ=='household_cutting_board_slicing':d={'type':'jugcraft:cutting','ingredient':d['ingredient'],'tool':'#jugcraft:knives','results':[d['result']]}
        elif typ=='household_cutting_board_combining':d={'type':'minecraft:crafting_shapeless','ingredients':d['ingredients'],'result':d['result']}
        elif typ in ('household_oven_baking','household_freezer_solidifying','household_frying_pan_cooking','household_toaster_heating','household_microwave_heating'):
            appliance={'household_oven_baking':'oven','household_freezer_solidifying':'freezer','household_frying_pan_cooking':'pan','household_toaster_heating':'toaster','household_microwave_heating':'microwave'}[typ]
            d={'type':'jugcraft:household_'+appliance,'ingredient':d['ingredient'],'result':d['result'],'cookingtime':d.get('time',200),'experience':.1,'category':d.get('category','food')}
        elif typ=='household_crafting_special_door_mat_copy':d={'type':'jugcraft:household_door_mat_copy'}
        d['fabric:load_conditions']=[{'condition':'jugcraft:feature_enabled','feature':'agriculture'}]
        counts[d['type']]+=1;write(DATA/'recipe'/out,d)
    # Workbench bootstrap must not require itself.
    write(DATA/'recipe/household/workbench_bootstrap.json',{'type':'minecraft:crafting_shaped','pattern':['II','PP'],'key':{'I':'minecraft:iron_ingot','P':'#minecraft:planks'},'result':{'id':ident('workbench')}})
    knife=DATA/'tags/item/knives.json';v=read(knife);v['values']=sorted(set(v['values']+[ident('knife')]));write(knife,v)
    sounds=read(ASSET/'sounds.json')
    for name,event in read(SOURCE/'sounds.json').items():
        target='household.'+name;event=copy.deepcopy(event)
        for i,s in enumerate(event.get('sounds',[])):
            if isinstance(s,str):event['sounds'][i]='jugcraft:household/'+s.split(':')[-1]
            else:s['name']='jugcraft:household/'+s['name'].split(':')[-1]
        sounds[target]=event;catalog['sounds'].append(target)
    write(ASSET/'sounds.json',sounds);write(ASSET/'lang/en_us.json',lang)
    atlas=read(RES/'assets/minecraft/atlases/blocks.json');entry={'type':'minecraft:directory','source':'household','prefix':'household/'}
    if entry not in atlas['sources']:atlas['sources'].append(entry)
    write(RES/'assets/minecraft/atlases/blocks.json',atlas)
    write(RES/'jugcraft/menagerie/household.json',catalog)
    write(ROOT/'art/owner-library/catalog/edited-2026-10-18-part2/household-runtime.json',{'copies':COPIES,'blocks':len(catalog['blocks']),'items':len(catalog['items']),'recipes':dict(counts),'aliases':ALIASES,'originalsUntouched':True,'validation':'Deferred until remaining batch is implemented, per owner.'})
    print('Prepared household resources:',len(catalog['blocks']),'blocks;',len(catalog['items']),'items.')

if __name__=='__main__':
    import pathlib
    main()
