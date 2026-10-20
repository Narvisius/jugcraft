"""Register the newly supplied decaying-creature family with preserved owner skins."""
import json
import shutil
from pathlib import Path
from recover_creature_models import ROOT,INPUT,OUT,write,addpart
from menagerie_data import loot as native_loot
import re,math

SOURCE=Path('C:/Users/james/Downloads/Magic Mods/rotten creature thing')
RES=ROOT/'src/main/resources';ASSET=RES/'assets/jugcraft';DATA=RES/'data/jugcraft'
ROSTER={
 'burned':('Burned','nether',22,['burned/burned','burned/burned_crazy','burned/burned_obsidian']),
 'dead_beard':('Dead Beard','beach',70,['dead_beard']),
 'frostbitten':('Frostbitten','snowy',20,['frostbitten']),
 'glacial_hunter':('Glacial Hunter','snowy',20,['glacial_hunter']),
 'immortal':('Immortal','storm',25,['immortal/immortal']),
 'mummy':('Mummy','desert',26,['mummy/mummy','mummy/ancient_mummy']),
 'scarab':('Scarab','summoned',10,['scarab/scarab_death_beetle','scarab/scarab_embellished_beetle']),
 'swampy':('Swampy','swamp',16,['swampy']),
 'undead_miner':('Undead Miner','cave',24,['undead_miner/undead_miner_stone','undead_miner/undead_miner_iron','undead_miner/undead_miner_gold','undead_miner/undead_miner_diamond']),
 'zap':('Zap','summoned',22,['zap']),
 'zombie_lackey':('Zombie Lackey','summoned',20,['zombie_lackey']),
 'skeleton_lackey':('Skeleton Lackey','summoned',18,['skeleton_lackey'])}

def adapt(value):
    if isinstance(value,str):return value.replace('rottencreatures:','jugcraft:rotten_')
    if isinstance(value,list):return [adapt(x) for x in value]
    if isinstance(value,dict):return {k:adapt(v) for k,v in value.items()}
    return value

def main():
    atlaspath=RES/'assets/minecraft/atlases/blocks.json'
    atlas=json.loads(atlaspath.read_text())
    for folder in ('block','item'):
        entry={'type':'minecraft:directory','source':'menagerie/rotten/'+folder,'prefix':'menagerie/rotten/'+folder+'/'}
        if entry not in atlas['sources']:atlas['sources'].append(entry)
    write(atlaspath,atlas)
    langpath=ASSET/'lang/en_us.json';lang=json.loads(langpath.read_text())
    species=[];attributes={}
    for key,(name,habitat,health,skins) in ROSTER.items():
        ident='rotten_'+key
        species.append(dict(source=key,id=ident,name=name,shape='insect' if key=='scarab' else 'humanoid',movement='land',habitat=habitat,mood='hostile',width={'dead_beard':1,'glacial_hunter':.7}.get(key,.6),height={'mummy':2.25,'dead_beard':2.05,'immortal':2,'zap':1.8,'scarab':.5,'zombie_lackey':1.85,'skeleton_lackey':1.85}.get(key,1.95),health=health,feed='minecraft:rotten_flesh',mount=False,model=ident,clips=[],armorTextures={},variants=[dict(name=Path(s).name,texture='menagerie/rotten/entity/'+s,babyTexture='menagerie/rotten/entity/'+s,weight=1,model=ident,babyModel=ident) for s in skins]))
        original=next(p for p in (INPUT/'rotten').rglob('*.java') if p.stem.lower()==key.replace('_',''))
        attributes[ident]={k:float(v) for k,v in re.findall(r'method_26868\(class_5134\.(field_\d+), ([\d.]+)\)',original.read_text())}
        lang['entity.jugcraft.'+ident]=name;lang['item.jugcraft.'+ident+'_spawn_egg']=name+' Spawn Egg'
        egg={'scarab':'endermite','skeleton_lackey':'skeleton'}.get(key,'zombie')
        write(ASSET/'items'/(ident+'_spawn_egg.json'),{'model':{'type':'minecraft:model','model':'minecraft:item/'+egg+'_spawn_egg'}})
    for f in (SOURCE/'assets/rottencreatures/textures').rglob('*'):
        if not f.is_file():continue
        target=ASSET/'textures/menagerie/rotten'/f.relative_to(SOURCE/'assets/rottencreatures/textures');target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(f,target)
    for name in ['freeze','channelled']:
        source=SOURCE/'assets/rottencreatures/textures/mob_effect'/(name+'.png')
        if source.exists():
            target=ASSET/'textures/mob_effect'/('rotten_'+name+'.png');target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source,target)
    sounds={};soundpath=ASSET/'sounds.json';existing=json.loads(soundpath.read_text())
    for key,value in json.loads((SOURCE/'assets/rottencreatures/sounds.json').read_text()).items():
        # Sound paths retain their directory names; item-ID prefix adaptation is not valid here.
        val=json.loads(json.dumps(value))
        for i,sound in enumerate(val.get('sounds',[])):
            if isinstance(sound,str):val['sounds'][i]='jugcraft:rotten/'+sound.split(':')[-1]
            elif 'name' in sound:sound['name']='jugcraft:rotten/'+sound['name'].split(':')[-1]
        if 'subtitle' in val:val['subtitle']='subtitles.jugcraft.rotten.'+key;lang[val['subtitle']]=key.replace('.',' ').replace('_',' ').title()
        sounds['rotten.'+key]=val
    existing.update(sounds);write(soundpath,existing)
    for f in (SOURCE/'assets/rottencreatures/sounds').rglob('*.ogg'):
        target=ASSET/'sounds/rotten'/f.relative_to(SOURCE/'assets/rottencreatures/sounds');target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(f,target)
    items=['corrupted_wart','frozen_rotten_flesh','magma_rotten_flesh','spear','tnt_barrel','treasure_chest']
    for key in items:
        lang['item.jugcraft.rotten_'+key]=key.replace('_',' ').title()
        source=SOURCE/'assets/rottencreatures/models/item'/(key+'.json')
        if source.exists():
            raw=source.read_text().replace('rottencreatures:item/','jugcraft:item/rotten_').replace('rottencreatures:block/','jugcraft:block/rotten_')
            model=json.loads(raw)
            if 'textures' in model:model['textures']={k:v.replace('jugcraft:item/rotten_','jugcraft:menagerie/rotten/item/').replace('jugcraft:block/rotten_','jugcraft:menagerie/rotten/block/') for k,v in model['textures'].items()}
            if key=='treasure_chest':model['textures']['particle']='#0'
            write(ASSET/'models/item'/('rotten_'+key+'.json'),model)
        write(ASSET/'items'/('rotten_'+key+'.json'),{'model':{'type':'minecraft:model','model':'jugcraft:item/rotten_'+key}})
    for f in (SOURCE/'data/rottencreatures/recipe').glob('*.json'):
        data=adapt(json.loads(f.read_text()))
        if 'key' in data:data['key']={k:v.get('item','#'+v.get('tag','')) if isinstance(v,dict) else v for k,v in data['key'].items()}
        if isinstance(data.get('ingredient'),dict):data['ingredient']=data['ingredient'].get('item')
        data['fabric:load_conditions']=[{'condition':'jugcraft:feature_enabled','feature':'agriculture'}]
        write(DATA/'recipe'/('rotten_'+f.name),data)
    for key in ROSTER:
        name='undead_stone_miner' if key=='undead_miner' else key
        source=SOURCE/'data/rottencreatures/loot_table/entities'/(name+'.json')
        if source.exists():write(DATA/'loot_table/entities'/('rotten_'+key+'.json'),native_loot(adapt(json.loads(source.read_text()))))
    for key in ['tnt_barrel','treasure_chest']:
        source=SOURCE/'assets/rottencreatures/models'/('block/tnt_barrel.json' if key=='tnt_barrel' else 'item/treasure_chest.json')
        model=json.loads(source.read_text().replace('rottencreatures:block/','jugcraft:menagerie/rotten/block/'))
        if key=='treasure_chest':model['textures']['particle']='#0'
        write(ASSET/'models/block'/('rotten_'+key+'.json'),model)
        write(ASSET/'blockstates'/('rotten_'+key+'.json'),{'variants':{'':{'model':'jugcraft:block/rotten_'+key}}})
        write(DATA/'loot_table/blocks'/('rotten_'+key+'.json'),{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:rotten_'+key}],'condition':{'type':'minecraft:survives_explosion'}}]})
        lang['block.jugcraft.rotten_'+key]=key.replace('_',' ').title()
    lang['entity.jugcraft.rotten_hunter_wolf']='Hunter Wolf';lang['item.jugcraft.rotten_hunter_wolf_spawn_egg']='Hunter Wolf Spawn Egg'
    write(ASSET/'items/rotten_hunter_wolf_spawn_egg.json',{'model':{'type':'minecraft:model','model':'minecraft:item/wolf_spawn_egg'}})
    for name in ['freeze','channelled']:lang['effect.jugcraft.rotten_'+name]=name.title()
    for name in ['corrupted','freeze','long_freeze','strong_freeze']:
        for bottle in ['potion','splash_potion','lingering_potion','tipped_arrow']:lang['item.minecraft.'+bottle+'.effect.rotten_'+name]=('Arrow of ' if bottle=='tipped_arrow' else 'Potion of ')+name.replace('_',' ').title()
    for before,reagent,after in [('minecraft:water','jugcraft:rotten_corrupted_wart','corrupted'),('jugcraft:rotten_corrupted','jugcraft:rotten_frozen_rotten_flesh','freeze'),('jugcraft:rotten_freeze','minecraft:redstone','long_freeze'),('jugcraft:rotten_freeze','minecraft:glowstone_dust','strong_freeze')]:
        for bottle in ['potion','splash_potion','lingering_potion']:
            write(DATA/'recipe/brewing'/('rotten_'+after+'_'+bottle+'.json'),{'type':'minecraft:brewing','input':{'item':'minecraft:'+bottle,'potion_contents':{'potions':before}},'output':{'id':'minecraft:'+bottle,'components':{'minecraft:potion_contents':{'potion':'jugcraft:rotten_'+after}}},'reagent':{'item':reagent}})
    for name in ['corrupted','freeze','long_freeze','strong_freeze']:
        for before,reagent,after in [('potion','gunpowder','splash_potion'),('splash_potion','dragon_breath','lingering_potion')]:
            write(DATA/'recipe/brewing'/('rotten_'+name+'_'+reagent+'.json'),{'type':'minecraft:brewing','input':{'item':'minecraft:'+before,'potion_contents':{'potions':'jugcraft:rotten_'+name}},'output':{'id':'minecraft:'+after,'components':{'minecraft:potion_contents':{'potion':'jugcraft:rotten_'+name}}},'reagent':{'item':'minecraft:'+reagent}})
    # The supplied lackey renderers inherit vanilla zombie/skeleton geometry.
    for kind in ['zombie','skeleton']:
        parts=[];thin=kind=='skeleton'
        for name,pose,box,uv in [('head',[0,0,0,0,0,0],[-4,-8,-4,8,8,8],[0,0]),('body',[0,0,0,0,0,0],[-4,0,-2,8,12,4],[16,16]),('right_arm',[-5,2,0,0,0,0],[-1 if thin else -3,-2,-1 if thin else -2,2 if thin else 4,12,2 if thin else 4],[40,16]),('left_arm',[5,2,0,0,0,0],[-1,-2,-1 if thin else -2,2 if thin else 4,12,2 if thin else 4],[40,16]),('right_leg',[-2 if thin else -1.9,12,0,0,0,0],[-1 if thin else -2,0,-1 if thin else -2,2 if thin else 4,12,2 if thin else 4],[0,16]),('left_leg',[2 if thin else 1.9,12,0,0,0,0],[-1 if thin else -2,0,-1 if thin else -2,2 if thin else 4,12,2 if thin else 4],[0,16])]:addpart(parts,name,'$mesh',pose,box,uv,mirror=name.startswith('left'))
        if not thin:addpart(parts,'hat','$mesh',[0,0,0,0,0,0],[-4,-8,-4,8,8,8],[32,0],.5)
        write(OUT/('rotten_'+kind+'lackeymodel.json'),dict(textureSize=[64,32 if thin else 64],parts=parts,animationRoot='$mesh',headLook='head',actions={},motions=[],source='Rendering.java delegates to vanilla '+kind))
    for modelpath in OUT.glob('rotten_*model.json'):
        model=json.loads(modelpath.read_text());bones={p['name'] for p in model['parts']}
        if not {'left_leg','right_leg','left_arm','right_arm'}<=bones:continue
        expressions=[e for e in model.get('expressions',[]) if e.get('adapter')!='vanilla-humanoid-gait']
        model['expressions']=expressions
        for bone,phase in [('right_leg',0),('left_leg',math.pi),('right_arm',math.pi),('left_arm',0)]:
            expressions.append(dict(adapter='vanilla-humanoid-gait',bone=bone,target='xRot',op='+=',value=['*',['*',['cos',['+',['*','walk',.6662],phase]],1.4],'move']))
        write(modelpath,model)
    write(RES/'jugcraft/menagerie/rotten.json',dict(species=species,sounds=list(sounds),items=items))
    write(RES/'jugcraft/menagerie/rotten_attributes.json',attributes)
    write(langpath,lang)

if __name__=='__main__':main()
