"""Runtime adapters for the supplied legendary-creature collection."""
import hashlib, shutil
from pathlib import Path
from menagerie_content import ROOT, RES, ASSET, DATA, read, write, remap
from menagerie_data import loot

SOURCE=ROOT/'art/owner-library/originals/Edited-2026-10-18-Part2/legendarycreatures'
SPECS={
    'desert_mojo':('land','desert','neutral',.9,1.5,28,'cactus'),
    'forest_mojo':('land','forest','neutral',.9,1.5,28,'oak_sapling'),
    'hound':('land','forest','hostile',1.1,1.1,32,'beef'),
    'scarecrow':('land','plains','hostile',.8,2.1,34,'wheat'),
    'scorpion':('land','desert','neutral',1,.6,22,'spider_eye'),
    'wisp':('air','forest','neutral',.5,.7,14,'glowstone_dust'),
    'nether_wisp':('air','nether','neutral',.5,.7,18,'glowstone_dust'),
    'ender_wisp':('air','end','neutral',.5,.7,20,'chorus_fruit'),
    'corpse_eater':('land','cave','hostile',.9,1.8,36,'rotten_flesh'),
    'peacock_spider':('land','jungle','neutral',.8,.6,18,'spider_eye'),
    'bullfrog':('amphibious','swamp','neutral',.7,.65,16,'slime_ball'),
    'butterfly':('air','meadow','passive',.4,.4,6,'dandelion'),
    'dragonfly':('air','swamp','passive',.5,.4,6,'sugar'),
    'shroomling':('land','forest','passive',.5,.8,12,'brown_mushroom'),
    'ladybug':('land','meadow','passive',.35,.3,6,'sugar'),
    'scarab':('land','desert','neutral',.45,.35,8,'cactus'),
    'mantis':('land','jungle','neutral',.5,.9,12,'spider_eye'),
    'hermit_crab':('amphibious','beach','passive',.55,.4,12,'kelp'),
    'jinx':('land','jungle','neutral',.6,1.2,22,'melon_slice'),
    'frogman':('amphibious','swamp','hostile',.7,1.8,28,'slime_ball'),
}
VARIANTS={'scorpion':['scorpion','scorpion_level2'],'peacock_spider':['peacock_spider','peacock_spider_level2','peacock_spider_level3'],'bullfrog':['bullfrog','bullfrog_level2','bullfrog_level3'],'frogman':['frogman','frogman_level2','frogman_level3'],**{name:[name,name+'2',name+'3'] for name in ['butterfly','dragonfly','ladybug','scarab','mantis','hermit_crab']}}
ITEMS=['hound_tooth','fertile_slimeball','straw_hat','blowgun']
COPIES=[]

def copy(p,d):
    d.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,d);COPIES.append({'source':p.relative_to(SOURCE).as_posix(),'runtime':d.relative_to(RES).as_posix(),'sha256':hashlib.sha256(p.read_bytes()).hexdigest()})

def resource(v):
    if isinstance(v,str):return ('minecraft:'+v) if v.startswith(('block/','item/')) else v.replace('legendarycreatures:','jugcraft:menagerie/fable/')
    if isinstance(v,list):return [resource(x) for x in v]
    if isinstance(v,dict):return {k:resource(x) for k,x in v.items() if k!='overrides'}
    return v

def main():
    lang=read(ASSET/'lang/en_us.json');names=read(SOURCE/'lang/en_us.json');sounds=read(ASSET/'sounds.json');catalog={'species':[],'sounds':[],'items':{},'blocks':{'fable_hound_trap':{'source':'hound_trap','name':'Hound Trap'},'fable_doom_fire':{'source':'doom_fire','name':'Doom Fire'}}}
    for kind in ['textures','sounds']:
        for p in (SOURCE/kind).rglob('*'):
            if p.is_file():copy(p,ASSET/kind/'menagerie/fable'/p.relative_to(SOURCE/kind))
    for p in (SOURCE/'geo').glob('*.json'):copy(p,ASSET/'geckolib/models/entity/menagerie/fable'/p.name)
    for name in ['root','convulsion']:
        copy(SOURCE/f'textures/mob_effect/{name}.png',ASSET/f'textures/mob_effect/fable_{name}.png')
    copy(SOURCE/'animations/wisp_purse.animation.json',ASSET/'geckolib/animations/entity/menagerie/fable/wisp_purse.animation.json')
    for name,(movement,habitat,mood,width,height,health,feed) in SPECS.items():
        base='wisp' if name.endswith('wisp') else name
        textures=VARIANTS.get(name,['mojo' if name.endswith('mojo') else name]);variants=[]
        for tex in textures:
            baby=('scorpion_level2_babies' if tex.endswith('level2') else 'scorpion_babies') if name=='scorpion' else tex
            variants.append({'name':tex,'texture':'menagerie/fable/entity/'+tex,'babyTexture':'menagerie/fable/entity/'+baby,'weight':1,'model':'fable/'+base,'babyModel':'fable/'+('scorpion_baby' if name=='scorpion' else base)})
        animation=read(SOURCE/f'animations/{base}.animation.json')
        if name=='scorpion':animation['animations'].update({'baby_'+k:v for k,v in read(SOURCE/'animations/scorpion_baby.animation.json')['animations'].items()})
        for action in ['fly','swim']:
            if movement in ('air','amphibious') and 'walk' in animation['animations']:animation['animations'][action]=animation['animations']['walk']
        attack={'hound':'bite','scorpion':'tail','jinx':'shoot'}.get(name)
        if attack:animation['animations']['attack']=animation['animations'][attack]
        write(ASSET/f'geckolib/animations/entity/menagerie/fable_{name}.animation.json',animation)
        copy(SOURCE/f'geo/{base}.geo.json',ASSET/f'geckolib/models/entity/menagerie/fable_{name}.geo.json')
        display='Fable '+names['entity.legendarycreatures.'+name]
        catalog['species'].append({'source':name,'id':'fable_'+name,'name':display,'shape':name,'movement':movement,'habitat':habitat,'mood':mood,'width':width,'height':height,'health':health,'feed':'minecraft:'+feed,'mount':False,'model':'fable_'+name,'variants':variants,'clips':list(animation['animations']),'armorTextures':{}})
        lang['entity.jugcraft.fable_'+name]=display
        id='fable_'+name+'_spawn_egg';catalog['items'][id]={'kind':'spawn','species':'fable_'+name,'name':display+' Spawn Egg'}
        write(ASSET/f'items/{id}.json',{'model':{'type':'minecraft:model','model':'minecraft:item/cow_spawn_egg','tints':[{'type':'minecraft:constant','value':0x513E63},{'type':'minecraft:constant','value':0xB8AD74}]}})
    catalog['items']['fable_scorpion_baby_spawn_egg']={'kind':'spawn','species':'fable_scorpion','baby':True,'name':'Fable Baby Scorpion Spawn Egg'}
    write(ASSET/'items/fable_scorpion_baby_spawn_egg.json',read(ASSET/'items/fable_scorpion_spawn_egg.json'))
    for p in (SOURCE/'models').rglob('*.json'):write(ASSET/'models/menagerie/fable'/p.relative_to(SOURCE/'models'),resource(read(p)))
    for name in ITEMS:
        id='fable_'+name;catalog['items'][id]={'kind':'item','source':name,'name':names['item.legendarycreatures.'+name]}
        write(ASSET/f'items/{id}.json',{'model':{'type':'minecraft:model','model':'jugcraft:menagerie/fable/item/'+name}})
    for key,v in catalog['blocks'].items():
        write(ASSET/f'blockstates/{key}.json',resource(read(SOURCE/f'blockstates/{v["source"]}.json')))
        if key!='fable_doom_fire':write(ASSET/f'items/{key}.json',{'model':{'type':'minecraft:model','model':'jugcraft:menagerie/fable/block/hound_trap_open'}})
        else:
            write(ASSET/'models/item/fable_doom_fire.json',{'parent':'minecraft:item/generated','textures':{'layer0':'jugcraft:menagerie/fable/block/doom_fire_0'}})
            write(ASSET/'items/fable_doom_fire.json',{'model':{'type':'minecraft:model','model':'jugcraft:item/fable_doom_fire'}})
        lang['block.jugcraft.'+key]=v['name']
    for key,value in read(SOURCE/'sounds.json').items():
        if key=='hound_base_attack_hit':value={'sounds':['legendarycreatures:hound/root']}
        key='fable_'+key;value=resource(value);value.pop('subtitle',None);sounds[key]=value;catalog['sounds'].append(key)
    for p in (SOURCE/'loot_tables').rglob('*.json'):
        rel=p.relative_to(SOURCE/'loot_tables');v=loot(remap(read(p),'fable','legendarycreatures'));write(DATA/'loot_table'/rel.parent/('fable_'+p.name),v)
    write(DATA/'loot_table/blocks/fable_doom_fire.json',{'type':'minecraft:block','pools':[]})
    for p in (SOURCE/'recipes').glob('*.json'):
        v=remap(read(p),'fable','legendarycreatures');v['key']={k:x.get('item','#'+x.get('tag','')) for k,x in v['key'].items()};v['result']['id']=v['result'].pop('item');v['fabric:load_conditions']=[{'condition':'jugcraft:feature_enabled','feature':'agriculture'}];write(DATA/f'recipe/fable_{p.name}',v)
    for id,v in catalog['items'].items():lang['item.jugcraft.'+id]=v['name']
    lang['item.jugcraft.fable_straw_hat']='Fable Straw Hat'
    lang['effect.jugcraft.fable_root']='Rooted'
    lang['effect.jugcraft.fable_convulsion']='Convulsion'
    lang['item.jugcraft.fable_doom_fire']='Doom Ember'
    lang['encyclopedia.jugcraft.page.woodland']='Woodland & Fable Creatures'
    write(DATA/'recipe/fable_doom_fire.json',{'type':'minecraft:crafting_shapeless','ingredients':['minecraft:blaze_powder','minecraft:coal','jugcraft:fable_hound_tooth'],'result':{'id':'jugcraft:fable_doom_fire','count':2},'fabric:load_conditions':[{'condition':'jugcraft:feature_enabled','feature':'agriculture'}]})
    copy(SOURCE/'textures/models/armor/straw_hat.png',ASSET/'textures/entity/equipment/humanoid/menagerie/fable_straw_hat.png')
    write(ASSET/'equipment/menagerie/fable_straw_hat.json',{'layers':{'humanoid':[{'texture':'jugcraft:menagerie/fable_straw_hat'}]}})
    atlas_path=RES/'assets/minecraft/atlases/blocks.json';atlas=read(atlas_path)
    for folder in ['block','item']:
        directory='menagerie/fable/'+folder;entry={'type':'minecraft:directory','source':directory,'prefix':directory+'/'}
        if entry not in atlas['sources']:atlas['sources'].append(entry)
    write(atlas_path,atlas);write(ASSET/'lang/en_us.json',lang);write(ASSET/'sounds.json',sounds);write(RES/'jugcraft/menagerie/fable.json',catalog)
    write(ROOT/'art/owner-library/catalog/edited-2026-10-18-part2/fable-runtime.json',{'copies':COPIES,'adaptations':['Jugcraft registry/resources','26.3 loot and recipes','native animation aliases; missing hound hit audio uses supplied root sound; current spawn-egg model'],'originalsUntouched':True})
    print('Prepared',len(catalog['species']),'fable creatures and supplied loot, equipment and traps.')

if __name__=='__main__':main()
