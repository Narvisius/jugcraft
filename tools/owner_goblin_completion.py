"""Native goblin subcollection bundled with the owner's landscape files."""
from menagerie_content import ROOT,RES,ASSET,DATA,read,write
from beasts_content import sculpt
from owner_worldgen_completion import modern


def main():
    path=RES/'jugcraft/menagerie/landscape.json';c=read(path);lang=read(ASSET/'lang/en_us.json')
    for name,w,h,hp in [('gob',.6,1.1,16),('garch',.6,1.2,20),('gobber',.9,1.4,32),('goblo',.6,1.1,18),('goom',.6,1.1,18),('hobgob',.7,1.8,32),('goblin_king',1.1,2.3,140),('mimic',1,1,30),('ogre',1.5,2.8,80)]:
        id='landscape_'+name
        s=dict(source=name,id=id,name=name.replace('_',' ').title(),shape='cube' if name=='mimic' else 'humanoid',movement='land',habitat='summoned' if name=='goblin_king' else 'cave',width=w,height=h,health=hp,mood='hostile',feed='minecraft:rotten_flesh',mount=False,model=id,clips=['idle','walk','fly','swim','sit','attack'],armorTextures={},variants=[])
        for skin in [name]+(['ogre_shrek'] if name=='ogre' else []):
            texture='menagerie/landscape/entities/'+skin;s['variants'].append(dict(name=skin,texture=texture,babyTexture=texture,weight=1))
        sculpt(s);c['species']=[x for x in c['species'] if x['id']!=id]+[s];lang['entity.jugcraft.'+id]=s['name']
        egg=id+'_spawn_egg';c['items'][egg]={'source':name+'_spawn_egg','food':False,'armor':'','species':id}
        write(ASSET/f'items/{egg}.json',{'model':{'type':'minecraft:model','model':'minecraft:item/zombie_spawn_egg'}})
        lang['item.jugcraft.'+egg]=s['name']+' Spawn Egg'
        lootpath=DATA/f'loot_table/entities/{id}.json'
        if not lootpath.exists():write(lootpath,{'type':'minecraft:entity','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:landscape_goblin_eye'}],'condition':{'type':'minecraft:random_chance','chance':.35}}]})
    for name in ('goblin_crown','goblin_eye','goblin_soul_bullet','ogre_tusk','ring_of_experience','ring_of_glory','ring_of_health','ring_of_stealth','soul_ash','staff_amethyst'):
        id='landscape_'+name;c['items'][id]={'source':name,'food':False,'armor':''}
        write(ASSET/f'items/{id}.json',{'model':{'type':'minecraft:model','model':'jugcraft:menagerie/landscape/item/'+name}})
        lang['item.jugcraft.'+id]=name.replace('_',' ').title()
    for name,pattern,keys in [
        ('goblin_crown',['G G','GEG',' T '],{'G':'minecraft:gold_ingot','E':'jugcraft:landscape_goblin_eye','T':'jugcraft:landscape_ogre_tusk'}),
        ('staff_amethyst',[' A ',' T ',' T '],{'A':'minecraft:amethyst_shard','T':'jugcraft:landscape_ogre_tusk'}),
        ('goblin_soul_bullet',['AE'],{'A':'jugcraft:landscape_soul_ash','E':'jugcraft:landscape_goblin_eye'})]:
        write(DATA/f'recipe/landscape_{name}.json',{'type':'minecraft:crafting_shaped','pattern':pattern,'key':keys,'result':{'id':'jugcraft:landscape_'+name,'count':4 if name=='goblin_soul_bullet' else 1},'fabric:load_conditions':[{'condition':'jugcraft:feature_enabled','feature':'agriculture'}]})
    write(DATA/'recipe/landscape_soul_ash.json',{'type':'minecraft:smelting','ingredient':'minecraft:soul_soil','result':{'id':'jugcraft:landscape_soul_ash'},'experience':.1,'cookingtime':200,'fabric:load_conditions':[{'condition':'jugcraft:feature_enabled','feature':'agriculture'}]})
    import shutil
    dest=ASSET/'textures/entity/equipment/humanoid/landscape/goblin_crown.png';dest.parent.mkdir(parents=True,exist_ok=True)
    shutil.copyfile(ASSET/'textures/menagerie/landscape/models/armor/goblin_crown.png',dest)
    write(ASSET/'equipment/landscape/goblin_crown.json',{'layers':{'humanoid':[{'texture':'jugcraft:landscape/goblin_crown'}]}})
    write(DATA/'tags/item/landscape_crown_repair.json',{'replace':False,'values':['minecraft:gold_ingot']})
    sounds=read(ASSET/'sounds.json')
    for creature in c['species']:
        n=creature['source']
        for action,suffix in [('ambient','idle'),('hurt','hurt'),('death','death')]:
            matches=list((ASSET/f'sounds/menagerie/landscape/{n}').glob(f'*_{suffix}*.ogg'))
            if not matches:continue
            key=f'landscape.entity.{n}.{action}'
            sounds[key]={'sounds':['jugcraft:'+x.relative_to(ASSET/'sounds').with_suffix('').as_posix() for x in matches]}
            if key not in c['sounds']:c['sounds'].append(key)
    write(ASSET/'sounds.json',sounds)
    if not (DATA/'recipe/landscape_urn.json').exists():
        write(DATA/'recipe/landscape_urn.json',{'type':'minecraft:crafting_shaped','pattern':['B B','B B','BBB'],'key':{'B':'minecraft:brick'},'result':{'id':'jugcraft:landscape_urn'},'fabric:load_conditions':[{'condition':'jugcraft:feature_enabled','feature':'agriculture'}]})
    write(path,c);write(ASSET/'lang/en_us.json',lang)


if __name__=='__main__':main()
