"""Adapt the larger owner creature collection into native Jugcraft resources.

Geometry is reconstructed because this subset has no model geometry source.
Original files stay intact. No supplied programs are executed.
"""
import copy
import hashlib
import re
import shutil
from menagerie_content import ROOT,RES,ASSET,DATA,read,write,animal_geometry
from menagerie_data import loot
from ecology_content import decor_definition
from wildlife_geometry import apply_skin,refine_shape,cube
from beast_profiles import profiles

SOURCE=ROOT/'art/owner-library/originals/Edited-2026-10-18-Part2/jims mobs'
COPIES=[]
ALIASES={'sea_bunny_slime_block':'companion_sea_bunny_slime_block','silk_cocoon':'companion_silk_cocoon'}
FOOD=set('banana maggot lobster_tail cooked_lobster_tail sopa_de_macaco moose_ribs cooked_moose_ribs blobfish fish_oil shrimp_fried_rice boiled_emu_egg kangaroo_meat cooked_kangaroo_meat kangaroo_burger gongylidia cosmic_cod rainbow_jelly raw_catfish cooked_catfish flying_fish mosquito_repellent_stew'.split())
ARMOR={'roadrunner_boots':'boots','crocodile_chestplate':'chestplate','centipede_leggings':'leggings','moose_headgear':'helmet','frontier_cap':'helmet','sombrero':'helmet','spiked_turtle_shell':'helmet','emu_leggings':'leggings','fedora':'helmet','tarantula_hawk_elytra':'chestplate','froststalker_helmet':'helmet','rocky_chestplate':'chestplate','flying_fish_boots':'boots','novelty_hat':'helmet','unsettling_kimono':'chestplate'}


def adapt(v,resource=False):
    if isinstance(v,str):
        if v.startswith('#'):return '#'+adapt(v[1:],resource)
        if v.startswith('alexsmobs:'):
            path=v.split(':',1)[1]
            return 'jugcraft:menagerie/beast/'+path if resource else 'jugcraft:'+('/'.join(path.split('/')[:-1])+('/' if '/' in path else ''))+ALIASES.get(path.split('/')[-1],'beast_'+path.split('/')[-1])
        return v
    if isinstance(v,list):return [adapt(x,resource) for x in v]
    if isinstance(v,dict):return {adapt(k,resource):adapt(x,resource) for k,x in v.items()}
    return v


def copied(p,d):
    d.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,d)
    COPIES.append({'source':p.relative_to(SOURCE).as_posix(),'runtime':d.relative_to(RES).as_posix(),'sha256':hashlib.sha256(p.read_bytes()).hexdigest()})


def skin_paths(name):
    alias={'centipede_head':'cave_centipede','bone_serpent':'bone_serpent_head','void_worm':'void_worm_head','catfish':'catfish_small','murmur':'murmur'}.get(name,name)
    paths=[]
    for p in (SOURCE/'textures/entity').rglob('*.png'):
        if not (p.stem==alias or p.stem.startswith(alias+'_')):continue
        if any(x in p.stem[len(alias):] for x in ('eye','glow','overlay','outer','hurt','sleep','angry','shedding','baby','spit','glint','bone','lead','claws','scars','slime','hood','transform','sheared','saddle','armor','tamed','helmet','necklace','fur_')):continue
        paths.append(p)
    if not paths:raise ValueError('Supply an explicit skin mapping for '+name)
    return sorted(paths)


def sculpt(s):
    original=s['shape'];shape=original;group=s['id'].split('_')[0]
    if shape in ('ape','humanoid','sentinel'):shape='bear'
    elif shape in ('octopus','tornado'):shape='jellyfish'
    elif shape in ('mushroom','kangaroo'):shape='rodent'
    elif shape=='cube':shape='shell'
    elif shape in ('pterosaur','bat'):shape='bird'
    elif shape=='ceratopsian':shape='rhino'
    elif shape=='sauropod':shape='giraffe'
    elif shape=='theropod':shape='reptile'
    elif shape in ('creeper','sphere'):shape='shell'
    base=copy.deepcopy(s);base['shape']=shape;animal_geometry(base)
    p=ASSET/f"geckolib/models/entity/menagerie/{s['id']}.geo.json";model=read(p);refine_shape(model,base)
    bones=model['minecraft:geometry'][0]['bones'];w=s['width']*16;h=s['height']*16
    def part(n,o,size,parent='root',pivot=None):bones.append({'name':n,'parent':parent,'pivot':pivot or [o[0]+size[0]/2,o[1]+size[1],o[2]+size[2]/2],'cubes':[cube(o,size)]})
    if original in ('ape','humanoid','sentinel'):
        bones[:]=[{'name':'root','pivot':[0,0,0]}]
        part('body',[-w*.35,h*.4,-w*.2],[w*.7,h*.4,w*.4]);part('head',[-w*.25,h*.8,-w*.25],[w*.5,h*.2,w*.5],'body')
        for side in [-1,1]:
            part('leg_'+str(side),[side*w*.2-w*.12,0,-w*.12],[w*.24,h*.42,w*.24])
            part('arm_'+str(side),[side*w*.48-w*.12,h*(.15 if original=='ape' else .35),-w*.12],[w*.24,h*(.65 if original=='ape' else .45),w*.24],'body')
            if original=='sentinel':
                for i in range(2):part(f'arm_{side}_{i}',[side*w*.6-w*.08,h*.2+i*h*.2,-w*.1],[w*.16,h*.3,w*.2],'body')
    elif original=='cube':bones[:]=[{'name':'root','pivot':[0,0,0]},{'name':'body','parent':'root','pivot':[0,h/2,0],'cubes':[cube([-w/2,0,-w/2],[w,h,w])]}]
    elif original=='octopus':
        bones[:]=[b for b in bones if not b['name'].startswith('tentacle')]
        for i in range(8):
            import math
            a=i*math.pi/4;x=math.sin(a)*w*.34;z=math.cos(a)*w*.34
            part('tentacle_'+str(i),[x-w*.07,0,z-w*.07],[w*.14,h*.7,w*.14],pivot=[x,h*.7,z])
    elif original=='tornado':
        bones[:]=[{'name':'root','pivot':[0,0,0]}]
        for i in range(5):part('swirl_'+str(i),[-w*(i+1)/10,h*i/5,-w*(i+1)/10],[w*(i+1)/5,h/5,w*(i+1)/5])
    elif original=='mushroom':
        part('cap',[-w*.65,h*.78,-w*.65],[w*1.3,h*.22,w*1.3]);part('stem',[-w*.12,h*.4,-w*.12],[w*.24,h*.5,w*.24])
    elif original=='kangaroo':
        for b in bones:
            if b['name']=='body':b['cubes']=[cube([-w*.3,h*.25,-w*.3],[w*.6,h*.55,w*.6])]
            if b['name']=='tail':b['cubes']=[cube([-w*.12,0,w*.2],[w*.24,h*.25,w*1.1])]
        part('pouch',[-w*.22,h*.35,-w*.37],[w*.44,h*.2,w*.1])
    elif original=='theropod':
        bones[:]=[{'name':'root','pivot':[0,0,0]}]
        part('body',[-w*.38,h*.36,-w*.35],[w*.76,h*.4,w*1.1])
        part('neck',[-w*.23,h*.64,-w*.5],[w*.46,h*.23,w*.5])
        part('head',[-w*.28,h*.76,-w*1.05],[w*.56,h*.22,w*.8])
        for side in [-1,1]:
            part('leg_'+str(side),[side*w*.35-w*.12,0,-w*.05],[w*.24,h*.45,w*.4])
            part('arm_'+str(side),[side*w*.38-w*.06,h*.4,-w*.55],[w*.12,h*.2,w*.3])
        for i in range(4):part('tail_'+str(i),[-w*(.2-i*.035),h*(.3-i*.055),w*(.6+i*.4)],[w*(.4-i*.07),h*(.22-i*.035),w*.5])
        if s['source'] in ('tremorzilla','luxtructosaurus'):
            for i in range(5):part('dorsal_'+str(i),[-w*.04,h*.7,-w*.3+i*w*.22],[w*.08,h*.25,w*.15])
    elif original=='creeper':
        bones[:]=[{'name':'root','pivot':[0,0,0]}]
        part('body',[-w*.3,h*.2,-w*.3],[w*.6,h*.55,w*.6]);part('head',[-w*.5,h*.65,-w*.5],[w,h*.35,w])
        for i in range(4):part('leg_'+str(i),[(i%2-.5)*w*.65-w*.17,0,(i//2-.5)*w*.65-w*.17],[w*.34,h*.25,w*.34])
    elif original=='sphere':
        bones[:]=[{'name':'root','pivot':[0,0,0]}];part('body',[-w*.4,h*.1,-w*.4],[w*.8,h*.8,w*.8])
        for side in [-1,1]:
            part('spike_x'+str(side),[side*w*.45-w*.1,h*.4,-w*.1],[w*.2,h*.2,w*.2]);part('spike_z'+str(side),[-w*.1,h*.4,side*w*.45-w*.1],[w*.2,h*.2,w*.2])
    if original=='ceratopsian':part('frill',[-w*.48,h*.4,-w*.65],[w*.96,h*.5,w*.15])
    if original in ('pterosaur','bat'):
        for b in bones:
            if b['name'].startswith('wing'):
                side=-1 if '-1' in b['name'] else 1;b['cubes']=[cube([side*w*.3-(w*1.1 if side<0 else 0),h*.55,-w*.4],[w*1.1,h*.06,w*.9])]
    if s['source'] in ('lobster','mantis_shrimp'):part('abdomen',[-w*.25,0,w*.3],[w*.5,h*.55,w*.85])
    if s['source']=='hammerhead_shark':part('hammer_head',[-w*.9,h*.3,-w*1.1],[w*1.8,h*.45,w*.4])
    if s['source'] in ('crocodile','caiman','komodo_dragon','froststalker'):
        part('snout',[-w*.22,h*.4,-w*1.3],[w*.44,h*.25,w*.7])
    if s['source']=='anteater':part('snout',[-w*.1,h*.4,-w*1.1],[w*.2,h*.2,w*.65])
    from owner_creature_refinements import refine,animate
    refine(model,s)
    for v in s['variants']:
        ref=group+'/'+s['source']+'/'+v['name'];v['model']=ref;v['babyModel']=ref
        write(ASSET/f'geckolib/models/entity/menagerie/{ref}.geo.json',apply_skin(copy.deepcopy(model),v['texture']))
    ap=ASSET/f"geckolib/animations/entity/menagerie/{s['id']}.animation.json";animation=read(ap)
    valid={b['name'] for b in bones}
    for clip in animation['animations'].values():clip['bones']={k:v for k,v in clip.get('bones',{}).items() if k in valid}
    animation['animations']['attack']={'animation_length':.7,'loop':False,'bones':{'root':{'rotation':{'0':[0,0,0],'.2':[-15,0,0],'.4':[20,0,0],'.7':[0,0,0]}}}}
    for i,b in enumerate(bones):
        n=b['name']
        if n.startswith(('arm','leg')):animation['animations']['walk']['bones'][n]={'rotation':[f'math.sin(query.anim_time * 360) * {22 if i%2 else -22}',0,0]}
        if n.startswith('swirl'):animation['animations']['idle']['bones'][n]={'rotation':[0,f'query.anim_time * {90+i*30}',0]}
        if n.startswith('tentacle'):animation['animations']['swim']['bones'][n]={'rotation':[f'math.sin(query.anim_time * 200 + {i*35}) * 18',0,0]}
    from wildlife_geometry import refine_animations
    refine_animations(animation,model)
    animate(animation,s);write(ap,animation)


def recipe(v):
    if isinstance(v,list):return [recipe(x) for x in v]
    if not isinstance(v,dict):return v
    if set(v)=={'item'}:return v['item']
    if set(v)=={'tag'}:return '#'+v['tag']
    d={k:recipe(x) for k,x in v.items()}
    if isinstance(d.get('result'),str):d['result']={'id':d['result']}
    elif isinstance(d.get('result'),dict) and 'item' in d['result']:d['result']['id']=d['result'].pop('item')
    return d


def modern_loot(v):
    if isinstance(v,list):return [modern_loot(x) for x in v]
    if not isinstance(v,dict):return v
    d={k:modern_loot(x) for k,x in v.items()}
    for key in ('type','condition','function'):
        if isinstance(d.get(key),str) and ':' not in d[key]:d[key]='minecraft:'+d[key]
    return loot(d)


def main():
    names=read(SOURCE/'lang/en_us.json');lang=read(ASSET/'lang/en_us.json');sounds=read(ASSET/'sounds.json')
    catalog={'species':[],'blocks':{},'items':{},'sounds':[],'capsid':[]}
    for folder in ('textures','sounds'):
        for p in (SOURCE/folder).rglob('*'):
            if p.is_file():copied(p,ASSET/folder/'menagerie/beast'/p.relative_to(SOURCE/folder))
    for p in (SOURCE/'models').rglob('*.json'):
        d=adapt(read(p),True);d.pop('overrides',None);d.pop('render_type',None)
        write(ASSET/'models/menagerie/beast'/p.relative_to(SOURCE/'models'),d)
    for s in profiles():
        s['name']=names['entity.alexsmobs.'+s['source']];s['variants']=[]
        for p in skin_paths(s['source']):
            texture='menagerie/beast/'+p.relative_to(SOURCE/'textures').with_suffix('').as_posix()
            s['variants'].append({'name':p.stem,'texture':texture,'babyTexture':texture,'weight':1})
        sculpt(s);catalog['species'].append(s);lang['entity.jugcraft.'+s['id']]=s['name']
        egg='beast_spawn_egg_'+('centipede' if s['source']=='centipede_head' else s['source'])
        catalog['items'][egg]={'source':egg[6:],'kind':'spawn','species':s['id']}
        write(ASSET/f'items/{egg}.json',{'model':{'type':'minecraft:model','model':'minecraft:item/zombie_spawn_egg' if s['mood']=='hostile' else 'minecraft:item/cow_spawn_egg'}})
    for p in (SOURCE/'blockstates').glob('*.json'):
        n=p.stem
        if 'block.alexsmobs.'+n not in names or n in ALIASES:continue
        definition=decor_definition(SOURCE,'alexsmobs',n,'beast',read(p));id='beast_'+n
        catalog['blocks'][id]={'source':n,'definition':definition};write(ASSET/f'blockstates/{id}.json',adapt(read(p),True))
        lang['block.jugcraft.'+id]=names['block.alexsmobs.'+n]
        write(ASSET/f'items/{id}.json',{'model':{'type':'minecraft:model','model':'jugcraft:menagerie/beast/item/'+n}})
    for key,title in names.items():
        if not key.startswith('item.alexsmobs.'):continue
        n=key.removeprefix('item.alexsmobs.');id='beast_'+n
        if '.' in n or n.endswith(('_inventory','_hand')) or n.startswith('dimensional_carver_shard_') or n in ('tab_icon','insulated_with_fur') or id in catalog['items']:continue
        catalog['items'][id]={'source':n,'kind':'item','food':n in FOOD,'armor':ARMOR.get(n,'')}
        write(ASSET/f'items/{id}.json',{'model':{'type':'minecraft:model','model':'jugcraft:menagerie/beast/item/'+n}})
        lang['item.jugcraft.'+id]=title
    for id,v in catalog['items'].items():
        if v['kind']=='spawn':lang['item.jugcraft.'+id]=next(s['name'] for s in catalog['species'] if s['id']==v['species'])+' Spawn Egg'
    for p in (SOURCE/'recipes').rglob('*.json'):
        d=read(p)
        if d['type'].startswith('alexsmobs:'):
            write(DATA/'recipe/beast'/p.relative_to(SOURCE/'recipes'),{'type':'jugcraft:beast_'+d['type'].split(':')[1]});continue
        if 'crittersandcompanions:' in str(d):continue
        write(DATA/'recipe/beast'/p.relative_to(SOURCE/'recipes'),recipe(adapt(d)))
    for p in (SOURCE/'capsid_recipes').glob('*.json'):catalog['capsid'].append(recipe(adapt(read(p))))
    for p in (SOURCE/'loot_tables').rglob('*.json'):
        if p.stem not in {s['source'] for s in catalog['species']} and 'beast_'+p.stem not in catalog['blocks']:continue
        d=modern_loot(adapt(read(p)));d.setdefault('type','minecraft:entity' if p.parent.name=='entities' else 'minecraft:block')
        write(DATA/'loot_table'/p.relative_to(SOURCE/'loot_tables').parent/('beast_'+p.name),d)
    for p in (SOURCE/'tags').rglob('*.json'):
        rel=p.relative_to(SOURCE/'tags');kind={'items':'item','blocks':'block','entity_types':'entity_type'}.get(rel.parts[0],rel.parts[0])
        if kind not in ('item','block','entity_type'):continue
        write(DATA/'tags'/kind/rel.parent.relative_to(rel.parts[0])/('beast_'+p.name),adapt(read(p)))
    for key,event in read(SOURCE/'sounds.json').items():
        if key.startswith('entity.'):continue # Companion sounds already registered by their own adapter.
        event=adapt(copy.deepcopy(event),True);event.pop('subtitle',None)
        for i,sound in enumerate(event.get('sounds',[])):
            if isinstance(sound,str) and ':' not in sound:event['sounds'][i]='jugcraft:menagerie/beast/'+sound
            elif isinstance(sound,dict) and ':' not in sound['name']:sound['name']='jugcraft:menagerie/beast/'+sound['name']
        sounds['beast.'+key]=event;catalog['sounds'].append('beast.'+key)
    write(ASSET/'sounds.json',sounds);write(ASSET/'lang/en_us.json',lang);write(RES/'jugcraft/menagerie/beast.json',catalog)
    atlas=read(RES/'assets/minecraft/atlases/blocks.json');entry={'type':'minecraft:directory','source':'menagerie/beast','prefix':'menagerie/beast/'}
    if entry not in atlas['sources']:atlas['sources'].append(entry)
    write(RES/'assets/minecraft/atlases/blocks.json',atlas)
    write(ROOT/'art/owner-library/catalog/edited-2026-10-18-part2/beast-runtime.json',{'copies':COPIES,'species':len(catalog['species']),'blocks':len(catalog['blocks']),'items':len(catalog['items']),'geometry':'Reconstructed native rigs; supplied skin images are preserved. Visual review pending.','validation':'Not run; owner requested implementation first.'})
    print('Prepared native beast resources:',len(catalog['species']),'species,',len(catalog['blocks']),'blocks,',len(catalog['items']),'items.')


if __name__=='__main__':main()
