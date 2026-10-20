"""Native woodland creature resources from the second owner batch (cnb).

Models, animation channels and art are supplied files. Runtime animation aliases
and armor bone names adapt them to Jugcraft's existing GeckoLib implementation.
"""
import json, shutil, hashlib
from pathlib import Path
from model_writer import separate_coplanar
from menagerie_content import ROOT, RES, ASSET, DATA, read, write

SOURCE=ROOT/'art/owner-library/originals/Edited-2026-10-18-Part2/cnb'
PREFIX='grove_'
COPIES=[]
SPECS={
    'little_grebe':('amphibious','river','passive',.5,.6,10,'wheat_seeds',False),
    'lizard':('land','desert','passive',.5,.3,8,'spider_eye',False),
    'cindershell':('land','nether','neutral',1.6,1.7,40,'coal',False),
    'sporeling':('land','forest','neutral',.5,.9,16,'brown_mushroom',False),
    'yeti':('land','snowy','neutral',1.7,2.6,60,'apple',False),
    'lilytad':('amphibious','swamp','passive',.7,.7,12,'seagrass',False),
    'minipad':('amphibious','swamp','passive',.5,.45,10,'seagrass',False),
    'end_whale':('air','end','neutral',3,2.4,80,'chorus_fruit',True),
    'cactem':('land','desert','neutral',.8,1.7,26,'cactus',False),
}
VARIANTS={
    'little_grebe':['little_grebe'], 'cindershell':['cindershell'], 'yeti':['yeti'], 'end_whale':['end_whale'],
    'lizard':['lizard_desert','lizard_desert_2','lizard_jungle','lizard_jungle_2','lizard_mushroom'],
    'sporeling':['sporeling_red_overworld','sporeling_brown_overworld','sporeling_red_nether','sporeling_brown_nether','sporeling_crimson_fungus','sporeling_warped_fungus','sporeling_goomy','sporeling_shrimpsnail','sporeling_bit0','sporeling_listacalista','sporeling_yungwilder'],
    'lilytad':['lilytad_pink','lilytad_light_pink','lilytad_yellow','lilytad_sheared'],
    'minipad':['minipad_pink','minipad_light_pink','minipad_yellow','minipad_sheared'],
    'cactem':['warrior_cactem','elder_cactem'],
}
ACTIONS={
    'little_grebe':{'walk':'little_grebe.walk','swim':'little_grebe.swim','baby_walk':'little_grebe_chick.walk'},
    'lizard':{'walk':'lizard_walk','sit':'lizard_dance'},
    'cindershell':{'idle':'cindershell_idle','walk':'cindershell_walk','baby_walk':'baby_cindershell_walk','eat':'cindershell_eat'},
    'sporeling':{'idle':'sporeling_idle','walk':'sporeling_walk','sit':'sporeling_sitting','attack':'sporeling_bite'},
    'yeti':{'walk':'yeti_adult_walk','baby_walk':'yeti_baby_walk','attack':'yeti_attack'},
    'lilytad':{'walk':'lilytad.walk','swim':'lilytad.walk'},
    'minipad':{'idle':'minipad_float','walk':'minipad_walk','swim':'minipad_swim'},
    'end_whale':{'idle':'whale_fly','walk':'whale_fly','fly':'whale_fly'},
    'cactem':{'idle':'cactem_idle','walk':'cactem_run','baby_walk':'cactem_baby_run','attack':'cactem_throw'},
}

def copy(p,d):
    d.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,d)
    COPIES.append({'source':p.relative_to(SOURCE).as_posix(),'runtime':d.relative_to(RES).as_posix(),'sha256':hashlib.sha256(p.read_bytes()).hexdigest()})

def resource(value):
    if isinstance(value,str):
        if value.startswith(('block/','item/')):return 'minecraft:'+value
        return value.replace('cnb:','jugcraft:menagerie/grove/')
    if isinstance(value,list):return [resource(x) for x in value]
    if isinstance(value,dict):return {k:resource(v) for k,v in value.items() if k!='overrides'}
    return value

def model_for(name,variant,baby=False):
    leaf=variant if name in ('cactem','sporeling') else name
    if name=='sporeling' and not (SOURCE/f'geo/entity/{name}/{leaf}.geo.json').exists():leaf='sporeling_nether' if 'fungus' in variant else 'sporeling_red_overworld'
    if name=='lizard' and 'mushroom' in variant:leaf='mushroom_lizard'
    if baby and name in ('cactem','cindershell','yeti'):leaf='baby_'+name
    if baby and name=='little_grebe':leaf='little_grebe_chick'
    return 'grove/'+name+'/'+leaf

def recipe(name,ingredients,count=1):
    write(DATA/f'recipe/grove_{name}.json',{'type':'minecraft:crafting_shapeless','ingredients':ingredients,'result':{'id':'jugcraft:grove_'+name,'count':count},'fabric:load_conditions':[{'condition':'jugcraft:feature_enabled','feature':'agriculture'}]})

def main():
    lang=read(ASSET/'lang/en_us.json');names=read(SOURCE/'lang/en_us.json');sounds=read(ASSET/'sounds.json');catalog={'species':[],'sounds':[],'items':{},'blocks':{}}
    for kind in ['textures','sounds']:
        for p in (SOURCE/kind).rglob('*'):
            if p.is_file():copy(p,ASSET/kind/'menagerie/grove'/p.relative_to(SOURCE/kind))
    for glow in (SOURCE/'textures').rglob('*_glow.png'):
        copy(glow,ASSET/'textures/menagerie/grove'/glow.relative_to(SOURCE/'textures').with_name(glow.stem.removesuffix('_glow')+'_glowmask.png'))
    for p in (SOURCE/'geo/entity').rglob('*.json'):copy(p,ASSET/'geckolib/models/entity/menagerie/grove'/p.relative_to(SOURCE/'geo/entity'))
    for name,(movement,habitat,mood,width,height,health,feed,mount) in SPECS.items():
        variants=[]
        for variant in VARIANTS[name]:
            baby='baby_'+name if name in ('cindershell','yeti','cactem') else 'little_grebe_chick' if name=='little_grebe' else variant
            variants.append({'name':variant,'texture':f'menagerie/grove/entity/{name}/{variant}','babyTexture':f'menagerie/grove/entity/{name}/{baby}','weight':0 if variant.endswith('sheared') else 1,'model':model_for(name,variant),'babyModel':model_for(name,variant,True)})
        animation=read(SOURCE/f'animations/{name}.json')
        for action,original in ACTIONS[name].items():animation['animations'][action]=animation['animations'][original]
        write(ASSET/f'geckolib/animations/entity/menagerie/grove_{name}.animation.json',animation)
        catalog['species'].append({'source':name,'id':'grove_'+name,'name':('Woodland Lizard' if name=='lizard' else names['entity.cnb.'+name]),'shape':name,'movement':movement,'habitat':habitat,'mood':mood,'width':width,'height':height,'health':health,'feed':'minecraft:'+feed,'mount':mount,'model':'grove_'+name,'variants':variants,'clips':list(animation['animations']),'armorTextures':{}})
        # Fallback model is needed before client data arrives.
        copy(SOURCE/('geo/entity/'+model_for(name,VARIANTS[name][0]).removeprefix('grove/')+'.geo.json'),ASSET/f'geckolib/models/entity/menagerie/grove_{name}.geo.json')
        lang['entity.jugcraft.grove_'+name]=names['entity.cnb.'+name]
        key='grove_'+name+'_spawn_egg';catalog['items'][key]={'name':('Woodland Lizard' if name=='lizard' else names['entity.cnb.'+name])+' Spawn Egg','kind':'spawn','species':'grove_'+name}
        write(ASSET/f'items/{key}.json',{'model':{'type':'minecraft:model','model':'minecraft:item/cow_spawn_egg','tints':[{'type':'minecraft:constant','value':0x425F45},{'type':'minecraft:constant','value':0xCFB783}]}})
    for key,value in read(SOURCE/'sounds.json').items():
        key='grove_'+key;value=resource(value);value.pop('subtitle',None);sounds[key]=value;catalog['sounds'].append(key)
    for p in (SOURCE/'models').rglob('*.json'):
        model=resource(read(p))
        if p.stem=='cactem_spear':model.pop('parent',None)
        if model.get('elements'):separate_coplanar(model['elements'])
        write(ASSET/'models/menagerie/grove'/p.relative_to(SOURCE/'models'),model)
    for p in (SOURCE/'blockstates').glob('*.json'):
        key='grove_'+p.stem;catalog['blocks'][key]={'source':p.stem,'name':names.get('block.cnb.'+p.stem,p.stem.replace('_',' ').title())}
        write(ASSET/f'blockstates/{key}.json',resource(read(p)))
        write(ASSET/f'items/{key}.json',{'model':{'type':'minecraft:model','model':'jugcraft:menagerie/grove/block/'+('lizard_eggs_1' if p.stem=='lizard_egg_block' else p.stem)}})
        result='grove_lizard_egg' if p.stem=='lizard_egg_block' else key
        write(DATA/f'loot_table/blocks/{key}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:'+result}]}]})
    for key,value in names.items():
        if not key.startswith('item.cnb.'):continue
        source=key.split('.')[-1];id='grove_'+source
        if id in catalog['items'] or id in catalog['blocks'] or source in ['cinder_sword_'+str(i) for i in range(1,5)]:continue
        catalog['items'][id]={'name':value,'source':source,'kind':'item'}
        write(ASSET/f'items/{id}.json',{'model':{'type':'minecraft:model','model':'jugcraft:menagerie/grove/item/'+source}})
    for id,v in catalog['items'].items():lang['item.jugcraft.'+id]=v['name']
    for id,v in catalog['blocks'].items():lang['block.jugcraft.'+id]=v['name']
    for n in range(1,4):lang[f'item.jugcraft.grove_heal_spell_book_{n}']='Mending Tome '+['I','II','III'][n-1]
    for variant in VARIANTS['lizard']:lang['item.jugcraft.grove_lizard_item_'+variant.removeprefix('lizard_')]=variant.replace('_',' ').title()+' Companion'
    lang['item.jugcraft.grove_entity_net']='Woodland Capture Net'
    heat_model={'type':'minecraft:model','model':'jugcraft:menagerie/grove/item/cinder_sword'}
    for heat in range(1,5):heat_model={'type':'minecraft:condition','property':'minecraft:component','predicate':'minecraft:custom_data','value':'{CinderHeat:'+str(heat)+'}','on_true':{'type':'minecraft:model','model':'jugcraft:menagerie/grove/item/cinder_sword_'+str(heat)},'on_false':heat_model}
    write(ASSET/'items/grove_cinder_sword.json',{'model':heat_model})
    for color in ['pink','light_pink','yellow']:
        lang['item.jugcraft.grove_'+color+'_waterlily']=color.replace('_',' ').title()+' Waterlily Sprig'
    # Actual shared acquisitions; creature products enter through drops/shearing.
    recipe('apple_slice',['minecraft:apple'],4)
    recipe('entity_net',['minecraft:stick','minecraft:string','minecraft:string','minecraft:iron_nugget'])
    recipe('cinder_furnace',['minecraft:furnace']+['jugcraft:grove_cindershell_shell_shard']*4)
    recipe('cinder_sword',['minecraft:stick']+['jugcraft:grove_cindershell_shell_shard']*4)
    recipe('cactem_spear',['minecraft:stick','minecraft:stick','minecraft:cactus','jugcraft:grove_yeti_antler'])
    recipe('flower_crown',['minecraft:string','jugcraft:grove_pink_minipad_flower','jugcraft:grove_yellow_minipad_flower'])
    recipe('glowing_flower_crown',['jugcraft:grove_flower_crown','jugcraft:grove_pink_minipad_flower_glow'])
    recipe('sporeling_backpack',['minecraft:chest','minecraft:brown_mushroom','minecraft:string','jugcraft:grove_yeti_hide'])
    for tier in range(1,4):recipe('heal_spell_book_'+str(tier),['minecraft:book' if tier==1 else 'jugcraft:grove_heal_spell_book_'+str(tier-1),'minecraft:amethyst_shard','jugcraft:grove_'+['pink','light_pink','yellow'][tier-1]+'_minipad_flower_glow'])
    for color in ['pink','light_pink','yellow']:
        recipe(color+'_waterlily',['jugcraft:grove_'+color+'_minipad_flower','minecraft:lily_pad'])
        recipe(color+'_waterlily_block',['jugcraft:grove_'+color+'_waterlily'])
        recipe('potted_'+color+'_waterlily',['jugcraft:grove_'+color+'_waterlily','minecraft:flower_pot'])
        recipe(color+'_minipad_flower_glow',['jugcraft:grove_'+color+'_minipad_flower','minecraft:glowstone_dust'])
    for species,drops in {'yeti':['yeti_hide','yeti_antler'],'cindershell':['cindershell_shell_shard'],'cactem':['yeti_antler']}.items():
        if species=='cactem':continue
        write(DATA/f'loot_table/entities/grove_{species}.json',{'type':'minecraft:entity','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:grove_'+drop}]} for drop in drops]})
    for armor,part in [('flower_crown','armorHead'),('sporeling_backpack','armorBody')]:
        model=read(SOURCE/f'geo/armor/{armor}.geo.json');model['minecraft:geometry'][0]['bones'][0]['name']=part
        write(ASSET/f'geckolib/models/armor/menagerie/{armor}.geo.json',model)
        animation=read(SOURCE/f'animations/{armor}.json')
        if not animation.get('animations'):animation['animations']={'idle':{'loop':True,'bones':{}}}
        write(ASSET/f'geckolib/animations/armor/menagerie/{armor}.animation.json',animation)
    from owner_resource_completion import wearable_metadata
    for wearable in ['flower_crown', 'glowing_flower_crown', 'sporeling_backpack']:wearable_metadata(wearable)
    atlas_path=RES/'assets/minecraft/atlases/blocks.json';atlas=read(atlas_path)
    for folder in ['block','blocks','item','items','entity/lilytad']:
        directory='menagerie/grove/'+folder;entry={'type':'minecraft:directory','source':directory,'prefix':directory+'/'}
        if entry not in atlas['sources']:atlas['sources'].append(entry)
    write(atlas_path,atlas);write(ASSET/'lang/en_us.json',lang);write(ASSET/'sounds.json',sounds);write(RES/'jugcraft/menagerie/grove.json',catalog)
    write(ROOT/'art/owner-library/catalog/edited-2026-10-18-part2/grove-runtime.json',{'copies':COPIES,'adaptations':['Jugcraft resource IDs','native state-driven animation aliases','armor bone names','native gameplay and acquisition recipes; static armor idle definitions; waterlily atlas sprites; spear parent repair; current spawn-egg model'],'originalsUntouched':True})
    print('Prepared',len(catalog['species']),'woodland species,',len(catalog['items']),'items,',len(catalog['blocks']),'blocks.')

if __name__=='__main__':main()
