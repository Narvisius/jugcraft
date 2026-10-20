"""Ecology and Familiar Fauna runtime copies, native wood/plant families and reconstructed fauna rigs."""
import copy
import hashlib
import re
import shutil
from pathlib import Path
from collections import defaultdict
from functools import lru_cache
from menagerie_content import ROOT,RES,ASSET,DATA,read,write,animal_geometry
from menagerie_data import loot
from native_shapes import model_boxes,multipart_shapes
from wildlife_geometry import refine_shape,apply_skin

BASE=ROOT/'art/owner-library/originals/Edited-2026-10-18-Part2'
SOURCES={'ecology':BASE/'jimbologics','fauna':BASE/'jug_fauna'}
NAMESPACES={'ecologics':'ecology','familiarfauna':'fauna'}
COPIES=[]
FOODS=set('coconut coconut_slice cooked_prickly_pear crab_claw crab_meat maple_cookie maple_pancake maple_pancakes maple_pie maple_pie_slice maple_syrup_bottle prickly_pear tropical_stew walnut turkey_leg_raw turkey_leg_cooked venison_raw venison_cooked'.split())

def adapt(v,resource=False):
    if isinstance(v,str):
        if v.startswith('#'):return '#'+adapt(v[1:],resource)
        if ':' in v and v.split(':')[0] in NAMESPACES:
            ns,path=v.split(':',1);g=NAMESPACES[ns]
            return 'jugcraft:'+('menagerie/'+g+'/'+path if resource else '/'.join(path.split('/')[:-1]+[g+'_'+path.split('/')[-1]]))
        return v
    if isinstance(v,list):return [adapt(x,resource) for x in v]
    if isinstance(v,dict):return {adapt(k,resource):adapt(x,resource) for k,x in v.items()}
    return v

def copy_asset(src,dst):
    dst.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(src,dst)
    COPIES.append({'source':src.relative_to(BASE).as_posix(),'runtime':dst.relative_to(RES).as_posix(),'sha256':hashlib.sha256(src.read_bytes()).hexdigest()})

def classify(n):
    if n.startswith('potted_'):return 'potted'
    for suffix,k in [('wall_hanging_sign','wall_hanging_sign'),('wall_sign','wall_sign'),('hanging_sign','hanging_sign'),('sign','sign'),('fence_gate','fence_gate'),('pressure_plate','pressure_plate'),('trapdoor','trapdoor'),('door','door'),('button','button'),('fence','fence'),('stairs','stairs'),('slab','slab'),('wall','wall'),('leaves','leaves'),('log','log'),('wood','log'),('sapling','sapling'),('seedling','sapling'),('cabinet','cabinet'),('shelf','shelf')]:
        if n.endswith('_'+suffix):return k
    return {'azalea_flower':'flower','coconut':'fruit','hanging_coconut':'hanging_fruit','spile':'spile','prickly_pear':'cactus_fruit','maple_sap_cauldron':'cauldron','maple_pie':'meal','maple_pancakes':'meal','moss_layer':'layer','pale_moss_layer':'layer','surface_moss':'moss','thin_ice':'ice','sandcastle':'decor','seashell':'decor','pot':'decor'}.get(n,'building')

SPECS={
 'ecology':{'camel':('horse','land','desert',1.7,2.4,32,'minecraft:cactus'),'coconut_crab':('crab','amphibious','beach',.8,.5,16,'coconut'),'penguin':('bird','amphibious','snowy',.6,.9,14,'minecraft:cod'),'squirrel':('rodent','land','forest',.4,.5,8,'walnut')},
 'fauna':{'butterfly':('butterfly','air','meadow',.5,.3,6,'minecraft:sugar'),'deer':('deer','land','forest',.9,1.5,18,'minecraft:wheat'),'dragonfly':('insect','air','swamp',.5,.3,6,'minecraft:sugar'),'pixie':('butterfly','air','meadow',.45,.55,10,'minecraft:amethyst_shard'),'snail':('shell','land','forest',.4,.4,8,'minecraft:carrot'),'turkey':('bird','land','forest',.6,.8,12,'minecraft:wheat_seeds')}
}

@lru_cache(None)
def elements(source,namespace,ref):
    if not ref.startswith(namespace+':'):return []
    p=source/'models'/(ref.split(':',1)[1]+'.json')
    if not p.exists():return []
    model=read(p);return model.get('elements',elements(source,namespace,model.get('parent','')))

def decor_definition(source,namespace,name,kind,state):
    props=defaultdict(set);shapes={}
    def cond(c):
        for k,v in c.items():
            if isinstance(v,list):
                for x in v:cond(x)
            else:props[k].update(str(v).lower().split('|'))
    for key,model in state.get('variants',{}).items():
        cond(dict(p.split('=') for p in key.split(',') if '=' in p))
        shapes[key]=model_boxes(model,lambda ref:elements(source,namespace,ref))
    for part in state.get('multipart',[]):cond(part.get('when',{}))
    for key,values in props.items():
        if values <= {'true','false'}:values.update(['true','false'])
    if name=='spile':props['fullness'].add('0')
    return {'source':name,'kind':'storage' if kind in ('cabinet','shelf') else kind,'properties':{k:sorted(v) for k,v in props.items()},'shapes':shapes,'parts':multipart_shapes(state,lambda ref:elements(source,namespace,ref)),'state':state}

def main():
    catalog={'blocks':{},'items':{},'species':[],'sounds':[]};lang=read(ASSET/'lang/en_us.json');sounds=read(ASSET/'sounds.json')
    for group,source in SOURCES.items():
        ns='ecologics' if group=='ecology' else 'familiarfauna'
        for folder in ['textures','sounds']:
            for p in (source/folder).rglob('*'):
                if p.is_file():copy_asset(p,ASSET/folder/'menagerie'/group/p.relative_to(source/folder))
        for p in (source/'models').rglob('*.json'):
            model=adapt(read(p),True);model.pop('overrides',None);write(ASSET/'models/menagerie'/group/p.relative_to(source/'models'),model)
        names=read(source/'lang/en_us.json') if (source/'lang/en_us.json').exists() else {}
        for p in (source/'blockstates').glob('*.json'):
            n=p.stem;id=group+'_'+n;k=classify(n)
            catalog['blocks'][id]={'source':n,'kind':k,'group':group,'definition':decor_definition(source,ns,n,k,read(p))}
            d=adapt(read(p),True)
            # Extended distance was a source implementation detail; vanilla leaves retain decay/waterlogging.
            if k=='leaves':
                d={'variants':{'':{'model':f'jugcraft:menagerie/{group}/block/{n}'}}}
            if n=='hanging_coconut':d['variants']['age=3']=d['variants']['age=2']
            write(ASSET/f'blockstates/{id}.json',d)
            lang['block.jugcraft.'+id]=names.get('block.'+ns+'.'+n,n.replace('_',' ').title())
        for p in (source/'items').glob('*.json'):
            n=p.stem;id=group+'_'+n
            catalog['items'][id]={'source':n,'group':group,'food':n in FOODS}
            write(ASSET/f'items/{id}.json',adapt(read(p),True));lang['item.jugcraft.'+id]=names.get('item.'+ns+'.'+n,n.replace('_',' ').title())
        if group=='fauna':
            for p in (source/'models/item').glob('*.json'):
                n=p.stem;id=group+'_'+n;catalog['items'][id]={'source':n,'group':group,'food':n in FOODS}
                write(ASSET/f'items/{id}.json',{'model':{'type':'minecraft:model','model':f'jugcraft:menagerie/fauna/item/{n}'}});lang['item.jugcraft.'+id]=n.replace('_',' ').title()
        for root in ['recipe','recipes']:
            for p in (source/root).rglob('*.json'):
                d=adapt(read(p));d.pop('conditions',None)
                if d.get('type')=='forge:ore_shaped':d['type']='minecraft:crafting_shaped'
                def modern(x):
                    if isinstance(x,list):return [modern(v) for v in x]
                    if not isinstance(x,dict):return x
                    if x.get('type')=='forge:ore_dict':return {'stickWood':'minecraft:stick','string':'minecraft:string','plankWood':'#minecraft:planks','blockGlass':'minecraft:glass','paneGlass':'minecraft:glass_pane'}[x['ore']]
                    if set(x)=={'item'}:return x['item']
                    if set(x)=={'tag'}:return '#'+x['tag']
                    return {k:modern(v) for k,v in x.items()}
                result=d.get('result')
                if isinstance(result,dict) and 'item' in result:result['id']=result.pop('item')
                if isinstance(result,str):d['result']={'id':result}
                d=modern(d)
                write(DATA/'recipe'/group/p.relative_to(source/root),d)
        for root in ['loot_table','loot_tables']:
            for p in (source/root).rglob('*.json'):
                rel=p.relative_to(source/root);write(DATA/'loot_table'/rel.parent/(group+'_'+rel.name),loot(adapt(read(p))))
        for p in (source/'tags').rglob('*.json'):
            rel=p.relative_to(source/'tags');folder={'blocks':'block','items':'item','entity_types':'entity_type'}.get(rel.parts[0],rel.parts[0]);write(DATA/'tags'/folder/Path(*rel.parts[1:-1])/(group+'_'+rel.name),adapt(read(p)))
        for name,event in read(source/'sounds.json').items():
            target=group+'.'+name;event=copy.deepcopy(event)
            for i,v in enumerate(event.get('sounds',[])):
                if isinstance(v,str):event['sounds'][i]='jugcraft:menagerie/'+group+'/'+v.split(':')[-1]
                else:v['name']='jugcraft:menagerie/'+group+'/'+v['name'].split(':')[-1]
            sounds[target]=event;catalog['sounds'].append(target)
        for name,(shape,movement,habitat,w,h,hp,feed) in SPECS[group].items():
            skins=[p for p in (source/'textures/entity').rglob('*.png') if (p.stem==name or p.stem.startswith(name+'_') or name in p.parts) and not any(x in p.stem for x in ['armor','glow','saddle'])]
            skins=[p for p in skins if 'fawn' not in p.stem and not p.stem.endswith('_baby')] or skins
            variants=[{'name':p.stem,'texture':'menagerie/'+group+'/'+p.relative_to(source/'textures').with_suffix('').as_posix(),'babyTexture':'menagerie/'+group+'/'+p.relative_to(source/'textures').with_suffix('').as_posix(),'weight':1,'model':group+'_'+name,'babyModel':group+'_'+name} for p in skins]
            for variant in variants:
                baby=source/'textures/entity'/f'{name}_baby.png'
                if name=='deer':baby=source/'textures/entity/deer/fawn.png'
                if baby.exists():variant['babyTexture']='menagerie/'+group+'/'+baby.relative_to(source/'textures').with_suffix('').as_posix()
            s={'source':name,'id':group+'_'+name,'name':name.replace('_',' ').title(),'shape':shape,'movement':movement,'habitat':habitat,'mood':'passive','width':w,'height':h,'health':hp,'feed':feed if ':' in feed else 'jugcraft:'+group+'_'+feed,'mount':name=='camel','model':group+'_'+name,'variants':variants,'clips':['idle','walk','fly','swim','sit'],'armorTextures':{}}
            animal_geometry(s);base=read(ASSET/f"geckolib/models/entity/menagerie/{s['id']}.geo.json");refine_shape(base,s)
            from owner_creature_refinements import refine
            refine(base,s)
            from wildlife_geometry import refine_animations
            ap=ASSET/f"geckolib/animations/entity/menagerie/{s['model']}.animation.json"
            animation=read(ap);refine_animations(animation,base);write(ap,animation)
            for variant in variants:
                path=group+'/'+name+'/'+variant['name'];write(ASSET/f'geckolib/models/entity/menagerie/{path}.geo.json',apply_skin(copy.deepcopy(base),variant['texture']));variant['model']=path;variant['babyModel']=path+'_baby';write(ASSET/f'geckolib/models/entity/menagerie/{path}_baby.geo.json',apply_skin(copy.deepcopy(base),variant['babyTexture']))
            catalog['species'].append(s);egg=group+'_'+name+'_spawn_egg';catalog['items'][egg]={'source':name+'_spawn_egg','group':group,'species':s['id'],'food':False}
            write(ASSET/f'items/{egg}.json',{'model':{'type':'minecraft:model','model':'minecraft:item/cow_spawn_egg'}});lang['item.jugcraft.'+egg]=s['name']+' Spawn Egg';lang['entity.jugcraft.'+s['id']]=s['name']
    for wood in ['coconut','maple','walnut','azalea','flowering_azalea']:
        write(DATA/f'worldgen/feature/ecology/{wood}.json',{'type':'jugcraft:ecology_tree','wood':wood})
        write(DATA/f'worldgen/placed_feature/ecology/{wood}.json',{'feature':'jugcraft:ecology/'+wood,'placement':[{'type':'minecraft:rarity_filter','chance':16},{'type':'minecraft:in_square'},{'type':'minecraft:heightmap','heightmap':'MOTION_BLOCKING'},{'type':'minecraft:biome'}]})
    song=read(SOURCES['ecology']/'jukebox_song/coconut.json');song['sound_event']='jugcraft:ecology.music_disc.coconut';write(DATA/'jukebox_song/ecology_coconut.json',song)
    write(DATA/'recipe/ecology/fauna_bug_habitat.json',{'type':'minecraft:crafting_shaped','pattern':['GGG','G G','PPP'],'key':{'G':'minecraft:glass','P':'#minecraft:planks'},'result':{'id':'jugcraft:fauna_bug_habitat'}})
    write(DATA/'recipe/ecology/maple_pancakes.json',{'type':'minecraft:crafting_shapeless','ingredients':['jugcraft:ecology_maple_pancake'],'result':{'id':'jugcraft:ecology_maple_pancakes'}})
    for group in SOURCES:
        entry={'type':'minecraft:directory','source':'menagerie/'+group,'prefix':'menagerie/'+group+'/'};atlas=read(RES/'assets/minecraft/atlases/blocks.json')
        if entry not in atlas['sources']:atlas['sources'].append(entry)
        write(RES/'assets/minecraft/atlases/blocks.json',atlas)
    write(ASSET/'lang/en_us.json',lang);write(ASSET/'sounds.json',sounds);write(RES/'jugcraft/menagerie/ecology.json',catalog)
    write(ROOT/'art/owner-library/catalog/edited-2026-10-18-part2/ecology-runtime.json',{'copies':COPIES,'blocks':len(catalog['blocks']),'items':len(catalog['items']),'species':len(catalog['species']),'geometry':'New Jugcraft articulated rigs; supplied texture bitmaps preserved unchanged.','validation':'Deferred until remaining implementation is complete.'})
    print('Prepared ecology',len(catalog['blocks']),'blocks',len(catalog['items']),'items',len(catalog['species']),'species')

if __name__=='__main__':main()
