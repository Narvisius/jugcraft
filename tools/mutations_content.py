"""Native mutant encounter resources. New articulated geometry; supplied art/audio/data preserved."""
import copy
import shutil
import hashlib
from menagerie_content import ROOT,RES,ASSET,DATA,read,write,animal_geometry
from menagerie_data import loot
from wildlife_geometry import apply_skin

SOURCE=ROOT/'art/owner-library/originals/Edited-2026-10-18-Part2/mutations'
COPIES=[]
SPECS={'mutant_zombie':('humanoid',1.6,3.2,180,'hostile'), 'mutant_skeleton':('skeleton',1.1,3.1,160,'hostile'), 'mutant_creeper':('creeper',1.8,2.4,160,'hostile'), 'mutant_enderman':('enderman',1.2,3.8,200,'hostile'), 'mutant_snow_golem':('snowman',1.4,2.4,100,'neutral'), 'spider_pig':('spider',1.1,.9,40,'neutral'), 'creeper_minion':('creeper',.45,.65,20,'neutral')}

def adapt(v,resource=False):
    if isinstance(v,str):
        if v.startswith('#'):return '#'+adapt(v[1:],resource)
        if v.startswith('mutantmonsters:'):
            path=v.split(':',1)[1];return 'jugcraft:menagerie/mutant/'+path if resource else 'jugcraft:'+'/'.join(path.split('/')[:-1]+['mutant_'+path.split('/')[-1]])
        return v
    if isinstance(v,list):return [adapt(x,resource) for x in v]
    if isinstance(v,dict):return {adapt(k,resource):adapt(x,resource) for k,x in v.items()}
    return v

def copied(p,d):
    d.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,d);COPIES.append({'source':p.relative_to(SOURCE).as_posix(),'runtime':d.relative_to(RES).as_posix(),'sha256':hashlib.sha256(p.read_bytes()).hexdigest()})

def rig(s):
    w=s['width']*16;h=s['height']*16;kind=s['shape'];bones=[{'name':'root','pivot':[0,0,0]}]
    def part(name,o,size,parent='root',pivot=None,role='fur'):
        bones.append({'name':name,'parent':parent,'pivot':pivot or [o[0]+size[0]/2,o[1]+size[1],o[2]+size[2]/2],'cubes':[{'origin':o,'size':size,'_skin_role':role}]})
    if kind in ('humanoid','skeleton','enderman'):
        thin=kind in ('skeleton','enderman');leg=h*.43;torso=h*.37;armw=w*(.12 if thin else .28)
        part('body',[-w*.32,leg,-w*.16],[w*.64,torso,w*.32]);part('head',[-w*.25,leg+torso,-w*.25],[w*.5,h*.2,w*.5],parent='body')
        for side in [-1,1]:
            part('leg_'+str(side),[side*w*.17-w*.08,0,-w*.09],[w*.16,leg,w*.18],pivot=[side*w*.17,leg,0])
            part('arm_'+str(side),[side*w*.48-armw/2,leg*.4,-armw/2],[armw,torso+leg*.6,armw],parent='body',pivot=[side*w*.48,leg+torso,0])
            if kind=='enderman':part('lower_arm_'+str(side),[side*w*.55-armw/2,leg*.12,-armw/2],[armw,torso,armw],parent='body',pivot=[side*w*.55,leg+torso*.3,0])
        if kind=='skeleton':
            bones[1]['cubes']=[]
            for y in range(4):bones[1]['cubes'].append({'origin':[-w*.32,leg+y*torso/4,-w*.16],'size':[w*.64,torso*.12,w*.32]})
            bones[1]['cubes'].append({'origin':[-w*.07,leg,-w*.07],'size':[w*.14,torso,w*.14]})
    elif kind=='snowman':
        part('body',[-w/2,0,-w/2],[w,h*.4,w]);part('chest',[-w*.36,h*.4,-w*.36],[w*.72,h*.35,w*.72]);part('head',[-w*.25,h*.75,-w*.25],[w*.5,h*.25,w*.5])
        for side in [-1,1]:part('arm_'+str(side),[side*w*.55-w*.1,h*.32,-w*.1],[w*.2,h*.4,w*.2],pivot=[side*w*.45,h*.7,0])
    elif kind=='creeper':
        part('body',[-w*.25,h*.15,-w*.25],[w*.5,h*.6,w*.5]);part('head',[-w*.38,h*.7,-w*.38],[w*.76,h*.3,w*.76])
        for i in range(4):part('leg_'+str(i),[(i%2-.5)*w*.65-w*.15,0,(i//2-.5)*w*.65-w*.15],[w*.3,h*.25,w*.3])
    else:
        part('body',[-w*.3,h*.3,-w*.35],[w*.6,h*.7,w*.7]);part('head',[-w*.22,h*.35,-w*.6],[w*.44,h*.5,w*.35])
        for side in [-1,1]:
            for i in range(4):part(f'leg_{side}_{i}',[side*w*.3-(w*.3 if side<0 else 0),0,(i-1.5)*w*.2],[w*.3,h*.35,w*.08])
    head=next(b for b in bones if b['name']=='head');o=head['cubes'][0]['origin'];size=head['cubes'][0]['size']
    for sign in [-1,1]:head['cubes'].append({'origin':[sign*size[0]*.2-size[0]*.08,o[1]+size[1]*.55,o[2]-.05],'size':[size[0]*.16,size[1]*.16,.1],'_skin_role':'dark'})
    model={'format_version':'1.12.0','minecraft:geometry':[{'description':{'identifier':'geometry.'+s['id'],'texture_width':64,'texture_height':64,'visible_bounds_width':6,'visible_bounds_height':7,'visible_bounds_offset':[0,h/32,0]},'bones':bones}]}
    move={b['name']:{'rotation':[f'math.sin(query.anim_time * 260) * {20 if i%2 else -20}',0,0]} for i,b in enumerate(bones) if b['name'].startswith(('leg','arm'))}
    attack={b['name']:{'rotation':{'0':[0,0,0],'.25':[-95,0,0],'.5':[25,0,0],'.8':[0,0,0]}} for b in bones if b['name'].startswith('arm')}
    burst={'root':{'scale':{'0':[1,1,1],'.4':[1.15,1.15,1.15],'.8':[1,1,1]}}}
    animation={'format_version':'1.8.0','animations':{
        'idle':{'loop':True,'bones':{}},'walk':{'loop':True,'bones':move},
        'sit':{'loop':True,'bones':{'root':{'position':[0,-h*.15,0]}}},
        'attack':{'loop':False,'animation_length':.8,'bones':attack or burst}}}

    from owner_creature_refinements import refine
    refine(model,s)
    write(ASSET/f"geckolib/animations/entity/menagerie/{s['id']}.animation.json",animation)
    for v in s['variants']:
        path='mutant/'+s['source']+'/'+v['name'];write(ASSET/f'geckolib/models/entity/menagerie/{path}.geo.json',apply_skin(copy.deepcopy(model),v['texture']));v['model']=path;v['babyModel']=path

def main():
    catalog={'species':[],'items':{},'blocks':{},'sounds':[]};lang=read(ASSET/'lang/en_us.json');names=read(SOURCE/'lang/en_us.json')
    for folder in ['textures','sounds']:
        for p in (SOURCE/folder).rglob('*'):
            if p.is_file():copied(p,ASSET/folder/'menagerie/mutant'/p.relative_to(SOURCE/folder))
    for p in (SOURCE/'models').rglob('*.json'):write(ASSET/'models/menagerie/mutant'/p.relative_to(SOURCE/'models'),adapt(read(p),True))
    for n,(shape,w,h,hp,mood) in SPECS.items():
        folder=SOURCE/'textures/entity'/n
        skins=list(folder.glob('*.png')) if folder.exists() else [SOURCE/f'textures/entity/{n if n!="creeper_minion" else "mutant_creeper"}.png']
        skins=[p for p in skins if not any(x in p.stem for x in ['eyes','glow','soul','arm'])]
        variants=[{'name':p.stem,'texture':'menagerie/mutant/'+p.relative_to(SOURCE/'textures').with_suffix('').as_posix(),'babyTexture':'menagerie/mutant/'+p.relative_to(SOURCE/'textures').with_suffix('').as_posix(),'weight':1} for p in skins]
        s={'source':n,'id':'mutant_'+n,'name':names.get('entity.mutantmonsters.'+n,n.replace('_',' ').title()),'shape':shape,'movement':'land','habitat':'summoned','mood':mood,'width':w,'height':h,'health':hp,'feed':'minecraft:'+('gunpowder' if n=='creeper_minion' else 'snowball' if n=='mutant_snow_golem' else 'carrot'),'mount':n=='spider_pig','model':'mutant_'+n,'variants':variants,'clips':['idle','walk','sit','attack'],'armorTextures':{}}
        rig(s);catalog['species'].append(s);lang['entity.jugcraft.'+s['id']]=s['name']
    for p in (SOURCE/'items').glob('*.json'):
        n=p.stem;id='mutant_'+n;catalog['items'][id]={'source':n,'name':names.get('item.mutantmonsters.'+n,n.replace('_',' ').title())}
        model=adapt(read(p),True)
        if n.endswith('_spawn_egg'):model={'model':{'type':'minecraft:model','model':'minecraft:item/zombie_spawn_egg'}}
        elif n=='mutant_skeleton_skull':model['model']['model']['kind']='skeleton'
        elif n=='endersoul_hand':
            hand=read(SOURCE/'models/item/endersoul_hand_in_hand.json')
            hand['textures']={'all':'jugcraft:menagerie/mutant/item/endersoul_hand_model','particle':'jugcraft:menagerie/mutant/item/endersoul_hand'}
            hand['elements']=[]
            parts=[([4,3,6],[12,11,10]),([1,5,6],[4,10,9])]+[([4+i*2,11,6],[5.5+i*2,16-(1 if i in [0,3] else 0),9]) for i in range(4)]
            for origin,end in parts:hand['elements'].append({'from':origin,'to':end,'faces':{face:{'texture':'#all','uv':[0,0,4,4]} for face in ['north','east','south','west','up','down']}})
            write(ASSET/'models/menagerie/mutant/item/endersoul_hand_in_hand.json',hand)
            model['model']['fallback']={'type':'minecraft:model','model':'jugcraft:menagerie/mutant/item/endersoul_hand_in_hand'}
        write(ASSET/f'items/{id}.json',model);lang['item.jugcraft.'+id]=catalog['items'][id]['name']
    for n in ['mutant_skeleton_skull','mutant_skeleton_wall_skull']:
        catalog['blocks']['mutant_'+n]={'source':n}
        write(ASSET/f'blockstates/mutant_{n}.json',{'variants':{'':{'model':'minecraft:block/skull'}}})
        write(DATA/f'loot_table/blocks/mutant_{n}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:mutant_mutant_skeleton_skull'}]}]})
    write(DATA/'tags/item/mutant_bones.json',{'values':['jugcraft:mutant_mutant_skeleton_rib','jugcraft:mutant_mutant_skeleton_limb']})
    for p in (SOURCE/'recipe').rglob('*.json'):
        write(DATA/'recipe/mutant'/p.relative_to(SOURCE/'recipe'),adapt(read(p)))
    for p in (SOURCE/'equipment').glob('*.json'):
        data=read(p)
        for layers in data['layers'].values():
            for layer in layers:layer['texture']='jugcraft:mutant_skeleton'
        write(ASSET/'equipment/mutant_skeleton.json',data)
    for p in (SOURCE/'textures/entity/equipment').rglob('*.png'):copied(p,ASSET/'textures/entity/equipment'/p.relative_to(SOURCE/'textures/entity/equipment'))
    # The encounter reward loop has concrete drops, independent of legacy custom loot modifiers.
    drops={'mutant_zombie':{'hulk_hammer':1},'mutant_creeper':{'creeper_shard':1},'mutant_enderman':{'endersoul_hand':1},'mutant_skeleton':{'mutant_skeleton_rib':6,'mutant_skeleton_limb':6,'mutant_skeleton_pelvis':1,'mutant_skeleton_shoulder_pad':2,'mutant_skeleton_skull':1},'mutant_snow_golem':{},'spider_pig':{},'creeper_minion':{}}
    for name,parts in drops.items():write(DATA/f'loot_table/entities/mutant_{name}.json',{'type':'minecraft:entity','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:mutant_'+part,'modifier':[{'type':'minecraft:set_count','count':count}]}]} for part,count in parts.items()]})
    write(DATA/'recipe/mutant/chemical_x.json',{'type':'minecraft:crafting_shaped','pattern':['GEG','DBD','GEG'],'key':{'G':'minecraft:gunpowder','E':'minecraft:ender_pearl','D':'minecraft:diamond','B':'minecraft:glass_bottle'},'result':{'id':'jugcraft:mutant_chemical_x'}})
    sounds=read(ASSET/'sounds.json')
    for name,event in read(SOURCE/'sounds.json').items():
        target='mutant.'+name;event=copy.deepcopy(event)
        for i,s in enumerate(event.get('sounds',[])):
            if isinstance(s,str):event['sounds'][i]='jugcraft:menagerie/mutant/'+s.split(':')[-1]
            else:s['name']='jugcraft:menagerie/mutant/'+s['name'].split(':')[-1]
        sounds[target]=event;catalog['sounds'].append(target)
    write(ASSET/'sounds.json',sounds);write(ASSET/'lang/en_us.json',lang);write(RES/'jugcraft/menagerie/mutant.json',catalog)
    atlas=read(RES/'assets/minecraft/atlases/blocks.json');entry={'type':'minecraft:directory','source':'menagerie/mutant','prefix':'menagerie/mutant/'}
    if entry not in atlas['sources']:atlas['sources'].append(entry)
    write(RES/'assets/minecraft/atlases/blocks.json',atlas)
    write(ROOT/'art/owner-library/catalog/edited-2026-10-18-part2/mutant-runtime.json',{'copies':COPIES,'species':list(SPECS),'items':list(catalog['items']),'geometry':'New native Jugcraft rigs because no creature geometry or gameplay source was supplied.','validation':'Deferred until remaining implementation is complete.'})
    print('Prepared seven mutant encounter/companion species and',len(catalog['items']),'items.')

if __name__=='__main__':main()
