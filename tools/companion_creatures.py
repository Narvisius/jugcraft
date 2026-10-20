"""Adapt the complete companion geometry subset inside the owner's jims mobs folder.

This collection also contains a much larger Alex-namespaced roster without
geometry. The manifest explicitly keeps that roster separate and pending.
"""
import copy as cp
import hashlib
import re
import shutil
from menagerie_content import ROOT, RES, ASSET, DATA, read, write

SOURCE=ROOT/'art/owner-library/originals/Edited-2026-10-18-Part2/jims mobs'
SPECS={
    'dragonfly':('air','swamp',1.3,1.0,28,'sugar',True),
    'dumbo_octopus':('water','ocean',.5,.6,10,'cod',False),
    'ferret':('amphibious','forest',.6,.45,14,'rabbit',False),
    'jumping_spider':('land','forest',.45,.4,10,'spider_eye',False),
    'koi_fish':('water','river',.5,.45,8,'wheat_seeds',False),
    'ladybug':('air','meadow',.4,.35,8,'sugar',False),
    'leaf_insect':('land','jungle',.4,.5,8,'oak_leaves',False),
    'otter':('amphibious','river',.7,.6,16,'cod',False),
    'red_panda':('land','jungle',.7,.85,18,'bamboo',False),
    'roly_poly':('land','forest',.8,.6,18,'brown_mushroom',False),
    'sea_bunny':('water','ocean',.4,.35,8,'kelp',False),
    'shima_enaga':('air','snowy',.35,.4,8,'wheat_seeds',False),
    'snail':('land','forest',.5,.5,8,'carrot',False),
    'stag_beetle':('land','forest',.55,.45,12,'apple',False),
    'stick_bug':('land','jungle',.35,.6,8,'oak_leaves',False),
    'weevil':('land','forest',.45,.5,10,'wheat',False),
}
ITEMS='acorn acorn_hat clam pearl silk silk_cocoon silk_lead grappling_hook dragonfly_wing iron_dragonfly_armor gold_dragonfly_armor diamond_dragonfly_armor netherite_dragonfly_armor pearl_necklace_1 pearl_necklace_2 pearl_necklace_3 snail_slime_bottle sea_bunny_slime_bottle koi_fish koi_fish_bucket dumbo_octopus_bucket sea_bunny_bucket'.split()
COPIES=[]

def copy(p,d):
    d.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,d)
    COPIES.append({'source':p.relative_to(SOURCE).as_posix(),'runtime':d.relative_to(RES).as_posix(),'sha256':hashlib.sha256(p.read_bytes()).hexdigest()})

def texture(name):
    p=SOURCE/'textures/entity'/f'{name}.png'
    copy(p,ASSET/'textures/menagerie/companion/entity'/p.name)
    if p.with_suffix('.png.mcmeta').exists():copy(p.with_suffix('.png.mcmeta'),ASSET/'textures/menagerie/companion/entity'/(p.name+'.mcmeta'))
    return 'menagerie/companion/entity/'+name

def resource(v):
    if isinstance(v,str):return v.replace('crittersandcompanions:','jugcraft:menagerie/companion/')
    if isinstance(v,list):return [resource(x) for x in v]
    if isinstance(v,dict):return {k:resource(x) for k,x in v.items() if k!='overrides'}
    return v

def recipe(name,ingredients,count=1):
    write(DATA/f'recipe/companion_{name}.json',{'type':'minecraft:crafting_shapeless','ingredients':ingredients,'result':{'id':'jugcraft:companion_'+name,'count':count},'fabric:load_conditions':[{'condition':'jugcraft:feature_enabled','feature':'agriculture'}]})

def main():
    lang=read(ASSET/'lang/en_us.json');catalog={'species':[],'sounds':[],'items':{},'blocks':{'companion_sea_bunny_slime_block':{'source':'sea_bunny_slime_block','name':'Sea Bunny Slime Block'}}}
    for name,(movement,habitat,w,h,hp,feed,mount) in SPECS.items():
        variants=[];base=SOURCE/f'geo/entity/{name}.geo.json';baby=SOURCE/f'geo/entity/baby_{name}.geo.json'
        copy(base,ASSET/f'geckolib/models/entity/menagerie/companion_{name}.geo.json')
        if baby.exists():copy(baby,ASSET/f'geckolib/models/entity/menagerie/companion/baby_{name}.geo.json')
        paths=sorted(p for p in (SOURCE/'textures/entity').glob(name+'*.png') if re.fullmatch(re.escape(name)+r'(?:_\d+|_gary)?',p.stem))
        for p in paths:
            babytex='baby_'+p.stem if (SOURCE/f'textures/entity/baby_{p.stem}.png').exists() else 'baby_'+name if (SOURCE/f'textures/entity/baby_{name}.png').exists() else p.stem
            variants.append({'name':p.stem,'texture':texture(p.stem),'babyTexture':texture(babytex),'weight':1,'model':'companion_'+name,'babyModel':'companion/baby_'+name if baby.exists() else 'companion_'+name})
        animation=read(SOURCE/f'animations/entity/{name}.animation.json')
        aliases={'dragonfly':{'idle':'dragonfly_fly','walk':'dragonfly_fly','fly':'dragonfly_fly','sit':'dragonfly_sit'},'dumbo_octopus':{'swim':'dumbo_octopus_swim','idle':'dumbo_octopus_idle'},'koi_fish':{'swim':'koi_fish_swim','idle':'koi_fish_swim'},'ferret':{'walk':'run'},'sea_bunny':{'walk':'sea_bunny_move','swim':'sea_bunny_move','idle':'sea_bunny'}}.get(name,{})
        for alias,original in aliases.items():animation['animations'][alias]=cp.deepcopy(animation['animations'][original])
        babyanim=SOURCE/f'animations/entity/baby_{name}.animation.json'
        if babyanim.exists():
            baby_clips=read(babyanim)['animations']
            # The supplied snail sit channel stores easing names as coordinates.
            if name=='snail':baby_clips['sit']=cp.deepcopy(baby_clips['idle'])
            animation['animations'].update({'baby_'+k:v for k,v in baby_clips.items()})
        write(ASSET/f'geckolib/animations/entity/menagerie/companion_{name}.animation.json',animation)
        armor={}
        if name=='dragonfly':
            for tier,part in [('iron','iron'),('golden','gold'),('diamond','diamond'),('netherite','netherite')]:armor[tier]=texture('dragonfly_armor_'+part)
        display=('Companion ' if name in ('dragonfly','snail','ladybug') else '')+name.replace('_',' ').title()
        catalog['species'].append({'source':name,'id':'companion_'+name,'name':display,'shape':name,'movement':movement,'habitat':habitat,'mood':'passive','width':w,'height':h,'health':hp,'feed':'minecraft:'+feed,'mount':mount,'model':'companion_'+name,'variants':variants,'clips':list(animation['animations']),'armorTextures':armor})
        key='companion_'+name+'_spawn_egg';catalog['items'][key]={'kind':'spawn','species':'companion_'+name,'name':display+' Spawn Egg'}
        write(ASSET/f'items/{key}.json',{'model':{'type':'minecraft:model','model':'minecraft:item/cow_spawn_egg','tints':[{'type':'minecraft:constant','value':0x69865A},{'type':'minecraft:constant','value':0xE1BF7A}]}})
        lang['entity.jugcraft.companion_'+name]=display
        products={'dragonfly':'dragonfly_wing','jumping_spider':'silk','weevil':'acorn','koi_fish':'koi_fish'}
        pools=[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:companion_'+products[name]}]}] if name in products else []
        write(DATA/f'loot_table/entities/companion_{name}.json',{'type':'minecraft:entity','pools':pools})
    for name in ITEMS:
        key='companion_'+name;catalog['items'][key]={'kind':'item','source':name,'name':'Companion '+name.replace('_',' ').title()}
        for p in (SOURCE/'models/item').glob(name+'*.json'):
            if p.stem!=name and not re.fullmatch(re.escape(name)+r'_\d+',p.stem):continue
            model=resource(read(p));write(ASSET/'models/menagerie/companion/item'/p.name,model)
            for ref in model.get('textures',{}).values():
                if isinstance(ref,str) and ref.startswith('jugcraft:menagerie/companion/'):
                    rel=ref.removeprefix('jugcraft:menagerie/companion/');copy(SOURCE/'textures'/(rel+'.png'),ASSET/'textures/menagerie/companion'/(rel+'.png'))
        write(ASSET/f'items/{key}.json',{'model':{'type':'minecraft:model','model':'jugcraft:menagerie/companion/item/'+name}})
    for name in ['sea_bunny_slime_block']:
        p=SOURCE/f'textures/block/{name}.png';copy(p,ASSET/f'textures/menagerie/companion/block/{name}.png')
        write(ASSET/f'models/menagerie/companion/block/{name}.json',{'parent':'minecraft:block/cube_all','textures':{'all':'jugcraft:menagerie/companion/block/'+name}})
        write(ASSET/f'blockstates/companion_{name}.json',{'variants':{'':{'model':'jugcraft:menagerie/companion/block/'+name}}})
        write(ASSET/f'items/companion_{name}.json',{'model':{'type':'minecraft:model','model':'jugcraft:menagerie/companion/block/'+name}})
        write(DATA/f'loot_table/blocks/companion_{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:companion_'+name}]}]})
        lang['block.jugcraft.companion_'+name]='Sea Bunny Slime Block'
    # The supplied cocoon is a block-shaped inventory model, not a missing icon.
    write(ASSET/'models/menagerie/companion/block/silk_cocoon.json',resource(read(SOURCE/'models/block/silk_cocoon.json')))
    copy(SOURCE/'textures/block/silk_cocoon.png',ASSET/'textures/menagerie/companion/block/silk_cocoon.png')
    # Render the supplied pack on the animated body's bone using the shared item layer.
    pack=read(SOURCE/'geo/entity/roly_poly_chest.geo.json')['minecraft:geometry'][0]
    elements=[]
    for bone in pack['bones']:
        for cube in bone.get('cubes',[]):
            origin=cube['origin'];size=cube['size'];faces={}
            for face,uv in cube['uv'].items():
                u,v=uv['uv'];w,h=uv['uv_size'];faces[face]={'uv':[u/4,v/4,(u+w)/4,(v+h)/4],'texture':'#all'}
            elements.append({'from':[origin[i]+8 for i in range(3)],'to':[origin[i]+size[i]+8 for i in range(3)],'faces':faces})
    texture('roly_poly_chest')
    write(ASSET/'models/menagerie/companion/item/roly_pack.json',{'textures':{'all':'jugcraft:menagerie/companion/entity/roly_poly_chest','particle':'jugcraft:menagerie/companion/entity/roly_poly_chest'},'elements':elements})
    write(ASSET/'items/companion_roly_pack.json',{'model':{'type':'minecraft:model','model':'jugcraft:menagerie/companion/item/roly_pack'}})
    armor=read(SOURCE/'geo/item/armor/acorn.geo.json')
    write(ASSET/'geckolib/models/armor/menagerie/companion_acorn.geo.json',armor)
    armor_tex=SOURCE/'textures/item/armor/acorn.png'
    if not armor_tex.exists():armor_tex=SOURCE/'textures/models/armor/acorn.png'
    copy(armor_tex,ASSET/'textures/menagerie/companion/armor/acorn.png')
    write(ASSET/'geckolib/animations/armor/menagerie/companion_acorn.animation.json',{'format_version':'1.8.0','animations':{'idle':{'loop':True,'bones':{}}}})
    recipe('acorn',['minecraft:oak_leaves','minecraft:oak_leaves'],2)
    recipe('clam',['minecraft:kelp','minecraft:sand','minecraft:cod'])
    recipe('acorn_hat',['jugcraft:companion_acorn']*5+['minecraft:leather'])
    recipe('silk_lead',['jugcraft:companion_silk']*3+['minecraft:slime_ball'])
    recipe('silk_cocoon',['jugcraft:companion_silk']*4)
    recipe('grappling_hook',['jugcraft:companion_silk_lead','minecraft:iron_ingot','minecraft:iron_ingot'])
    for tier,material in [('iron','iron_ingot'),('gold','gold_ingot'),('diamond','diamond'),('netherite','netherite_ingot')]:recipe(tier+'_dragonfly_armor',['minecraft:'+material]*3+['minecraft:leather'])
    recipe('pearl_necklace_1',['jugcraft:companion_pearl','jugcraft:companion_silk'] )
    for tier in [2,3]:recipe('pearl_necklace_'+str(tier),['jugcraft:companion_pearl_necklace_'+str(tier-1)]+['jugcraft:companion_pearl']*tier)
    recipe('sea_bunny_slime_block',['jugcraft:companion_sea_bunny_slime_bottle']*4)
    for key,v in catalog['items'].items():lang['item.jugcraft.'+key]=v['name']
    from owner_resource_completion import wearable_metadata
    wearable_metadata('companion_acorn')
    atlaspath=RES/'assets/minecraft/atlases/blocks.json';atlas=read(atlaspath)
    for folder in ['block','item']:
        path='menagerie/companion/'+folder;entry={'type':'minecraft:directory','source':path,'prefix':path+'/'}
        if entry not in atlas['sources']:atlas['sources'].append(entry)
    pack_source={'type':'minecraft:single','resource':'jugcraft:menagerie/companion/entity/roly_poly_chest'}
    if pack_source not in atlas['sources']:atlas['sources'].append(pack_source)
    write(atlaspath,atlas);write(ASSET/'lang/en_us.json',lang);write(RES/'jugcraft/menagerie/companion.json',catalog)
    write(ROOT/'art/owner-library/catalog/edited-2026-10-18-part2/companion-runtime.json',{'copies':COPIES,'scope':'16 complete companion models within the mixed jims mobs folder; other Alex-namespaced content remains pending','adaptations':['Jugcraft registry and resources','Native gameplay and acquisition','Animation aliases; invalid baby snail sit channel uses supplied idle; original armor hierarchy retained; static armor idle definition; current spawn-egg model'],'originalsUntouched':True})
    print('Prepared',len(catalog['species']),'companions and',len(catalog['items']),'items.')

if __name__=='__main__':main()
