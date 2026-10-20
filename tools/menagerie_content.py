"""Native Jugcraft wildlife/mount resources from the owner's preserved October 18 inputs.

Only runtime copies are adapted. Missing animal geometry is newly constructed, not
represented as the supplied mod's model/source. Run this after editing the roster.
"""
from pathlib import Path
import json, re, shutil
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'art/owner-library/originals/Edited-2026-10-18'
NATURE = SOURCE / 'jimbos_heavily modified naturalist animals'
MOUNTS = SOURCE / 'mythicmounts'
RES = ROOT / 'src/main/resources'
ASSET = RES / 'assets/jugcraft'
DATA = RES / 'data/jugcraft'
PROVENANCE = []

def read(p):
    text=p.read_text(encoding='utf-8-sig')
    text=re.sub(r'("(?:\\.|[^"\\])*")|//[^\n]*|/\*[\s\S]*?\*/',lambda m:m.group(1) or '',text)
    text=re.sub(r'("(?:\\.|[^"\\])*")|,\s*(?=[}\]])',lambda m:m.group(1) or '',text)
    return json.loads(text.strip().removesuffix(","))

def write(p,v):
    p.parent.mkdir(parents=True,exist_ok=True)
    p.write_text(json.dumps(v,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')

def copy(src,dst):
    dst.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(src,dst)
    PROVENANCE.append({'source':src.relative_to(SOURCE).as_posix(),'runtime':dst.relative_to(RES).as_posix(),'change':'byte-identical'})

def remap(v,group='wildlife',namespace='naturalist',resource=False):
    if isinstance(v,str):
        if v.startswith('#'):return '#'+remap(v[1:],group,namespace,resource)
        if v.startswith(namespace+':'):
            path=v.split(':',1)[1]
            if resource:return 'jugcraft:menagerie/'+group+'/'+path
            parent,separator,leaf=path.rpartition('/')
            return 'jugcraft:'+(parent+'/' if separator else '')+group+'_'+leaf
        return v
    if isinstance(v,list):return [remap(x,group,namespace,resource) for x in v]
    if isinstance(v,dict):return {remap(k,group,namespace,resource):remap(x,group,namespace,resource) for k,x in v.items()}
    return v

# species, silhouette, locomotion, habitat, disposition, width, height, health, feed
ROSTER = '''
alligator reptile amphibious swamp predator 1.5 .7 30 cod
anglerfish fish water ocean passive .5 .5 6 kelp
ant insect land forest passive .25 .18 4 sugar
bass fish water river passive .55 .4 6 wheat_seeds
bear bear land forest neutral 1.4 1.5 30 honey_bottle
bird bird air forest passive .35 .45 6 wheat_seeds
black_bear bear land forest neutral 1.3 1.4 28 sweet_berries
blobfish fish water deep_ocean passive .65 .5 8 kelp
boar boar land forest neutral .9 .9 16 carrot
butterfly butterfly air meadow passive .4 .35 4 dandelion
capybara rodent amphibious swamp passive .8 .8 16 melon_slice
caterpillar worm land forest passive .3 .2 4 oak_leaves
catfish fish water river passive .9 .5 10 cod
clam shell water beach passive .5 .3 8 kelp
crab crab amphibious beach neutral .55 .3 8 kelp
deer deer land forest passive .8 1.5 18 wheat
desert_scorpion scorpion land desert neutral .5 .35 10 spider_eye
dragonfly butterfly air swamp passive .4 .3 4 sugar
duck bird amphibious river passive .55 .65 10 wheat_seeds
elephant elephant land savanna neutral 2.6 3.1 70 melon_slice
firefly insect air swamp passive .25 .2 4 sugar
giant_isopod insect water deep_ocean passive .8 .4 14 kelp
giraffe giraffe land savanna passive 1.4 3.5 32 acacia_leaves
great_white_shark shark water ocean predator 1.6 .9 40 cod
hedgehog rodent land forest neutral .45 .4 8 sweet_berries
hippo hippo amphibious river neutral 1.7 1.5 40 melon_slice
jellyfish jellyfish water ocean neutral .8 1 10 kelp
jungle_scorpion scorpion land jungle neutral .9 .5 18 spider_eye
komodo_dragon reptile land jungle predator 1.1 .6 24 chicken
lion cat land savanna predator 1.2 1.2 30 beef
lizard reptile land desert passive .4 .25 6 spider_eye
mammoth elephant land snowy neutral 2.8 3.3 80 wheat
mole rodent land forest passive .45 .35 8 carrot
ostrich ostrich land savanna neutral .9 1.8 22 wheat_seeds
piranha fish water jungle predator .4 .4 8 beef
rat rodent land plains passive .4 .3 6 wheat
ray ray water ocean neutral 1.2 .4 16 cod
rhino rhino land savanna neutral 1.7 1.8 45 wheat
snail shell land forest passive .35 .35 6 red_mushroom
snake snake land desert neutral .6 .25 10 rabbit
starfish star water beach passive .4 .18 6 kelp
tiger cat land jungle predator 1.2 1.2 32 beef
tortoise shell land savanna passive .8 .55 18 cactus
turkey bird land forest passive .65 .8 12 wheat_seeds
vulture bird air badlands neutral .85 .9 16 rotten_flesh
whale whale water deep_ocean passive 3.5 2.2 100 cod
zebra horse land savanna passive 1.1 1.6 24 wheat
'''
MOUNT_SPECS = [
 ('acenciamount','acencia','acencia_mount','land','forest',1.3,1.7),
 ('batmount','netherbat','bat_mount','air','nether',1.4,1.2),
 ('colelytramount','colelytra','colelytra_mount','air','forest',1.4,1.1),
 ('courierbirdmount','courierbird','courierbird_mount','air','meadow',1.2,1.8),
 ('dragonmount','dragon','dragon_mount','air','mountain',1.8,2),
 ('firebirdmount','firebird','firebird_mount','air','nether',1.3,1.8),
 ('griffonmount','griffon','griffon_mount','air','mountain',1.5,1.8),
 ('groundlizardmount','geckotoalizard','groundlizard_mount','land','desert',1.3,.9),
 ('mothmount','moth','moth_mount','air','forest',1.2,1.2),
 ('nightmaremount','nightmare','nightmare_mount','land','nether',1.2,1.7),
 ('nudibranchmount','nudibranch','nudibranch_mount','water','ocean',1.4,.8),
 ('ridinglizardmount','ridinglizard','ridinglizard_mount','land','jungle',1.2,1.5),
 ('seaturtlemount','archelon','archelon_mount','water','ocean',1.8,.8),
 ('wolfmount','direwolf','direwolf_mount','land','snowy',1.2,1.4),
]

def wildlife_variants(species):
    folder=NATURE/'naturalist/naturalist'/f'{species}_variant'
    entries=[]
    for p in sorted(folder.glob('*.json')):
        v=read(p);tex=v['texture'].split(':')[1];baby=v.get('baby_texture',v['texture']).split(':')[1]
        if not (NATURE/'textures'/f'{tex}.png').exists():continue
        if not (NATURE/'textures'/f'{baby}.png').exists():baby=tex
        entries.append({'name':p.stem,'texture':f'menagerie/wildlife/{tex}','babyTexture':f'menagerie/wildlife/{baby}','weight':v.get('weight',1)})
    if not entries:
        candidates=[p for p in (NATURE/'textures/entity').rglob('*.png') if p.stem==species or p.parent.name==species and 'baby' not in p.stem and 'glow' not in p.stem]
        if species=='desert_scorpion':candidates=[NATURE/'textures/entity/scorpion/desert_scorpion.png']
        if species=='jungle_scorpion':candidates=list((NATURE/'textures/entity/scorpion').glob('*jungle*png'))
        for p in candidates:
            tex='menagerie/wildlife/'+p.relative_to(NATURE/'textures').with_suffix('').as_posix()
            entries.append({'name':p.stem,'texture':tex,'babyTexture':tex,'weight':1})
    if not entries:raise ValueError('No texture: '+species)
    return entries

def animal_geometry(e):
    """New articulated species silhouettes; UV rectangles stay within original atlases."""
    texture=RES/'assets/jugcraft/textures'/f"{e['variants'][0]['texture']}.png"
    tw,th=Image.open(texture).size
    # The original texture dimensions are retained; geometry is constructed independently.
    bones=[{'name':'root','pivot':[0,0,0]}]
    family=e['shape'];w=e['width']*16;h=e['height']*16
    def part(name,origin,size,parent='root',pivot=None,uv=(0,0),rotation=None):
        x,y,z=size; bone={'name':name,'parent':parent,'pivot':pivot or [origin[0]+x/2,origin[1]+y,origin[2]+z/2],
             'cubes':[{'origin':origin,'size':size,'uv':[min(uv[0],max(0,tw-2*(x+z))),min(uv[1],max(0,th-y-z))]}]}
        if rotation:bone['rotation']=rotation
        bones.append(bone)
    if family in {'fish','shark','whale','ray'}:
        length=w*1.8;part('body',[-w/2,h*.2,-length/2],[w,h*.7,length])
        part('tail',[-w*.3,h*.2,length/2],[w*.6,h*.6,w*.9],uv=(0,th/2))
        for sign in [-1,1]:part('fin_'+str(sign),[sign*w*.5-(w*.7 if sign<0 else 0),h*.2,-w*.2],[w*.7,max(.2,h*.08),w*.6],uv=(0,th/2))
        if family=='shark':part('dorsal',[-w*.07,h*.7,-w*.2],[w*.14,h*.5,w*.7],uv=(0,th/2))
        if e['source']=='anglerfish':part('lure',[-.3,h*.8,-w*.7],[.6,h*.5,.6],uv=(0,0))
    elif family in {'bird','ostrich','butterfly','insect'}:
        insect=family in {'butterfly','insect'};base=0 if insect else h*.25
        part('body',[-w*.35,base,-w*.5],[w*.7,h*.5,w]);part('head',[-w*.25,base+h*.4,-w*.7],[w*.5,h*.3,w*.5])
        for sign in [-1,1]:
            part('wing_'+str(sign),[sign*w*.3-(w*.8 if sign<0 else 0),base+h*.25,-w*.35],[w*.8,max(.2,h*.06),w*.8],pivot=[sign*w*.3,base+h*.25,0],uv=(0,th/2))
            if not insect:part('leg_'+str(sign),[sign*w*.2-w*.05,0,-w*.05],[w*.1,base,w*.15],uv=(tw/2,0))
        if not insect:part('beak',[-w*.13,base+h*.5,-w*.95],[w*.26,h*.12,w*.3],uv=(tw/2,th/2))
    elif family in {'snake','worm'}:
        for i in range(6):part('segment_'+str(i),[-w*.35,0,i*w*.4-w],[w*.7,h,w*.48],parent='root' if i==0 else 'segment_'+str(i-1))
    elif family=='jellyfish':
        part('body',[-w/2,h*.6,-w/2],[w,h*.4,w]);
        for i in range(4):part('tentacle_'+str(i),[(i%2-.5)*w*.6,h*.05,(i//2-.5)*w*.6],[w*.12,h*.6,w*.12],uv=(0,th/2))
    elif family=='star':
        part('body',[-w*.25,0,-w*.25],[w*.5,h,w*.5])
        for i in range(5):part('arm_'+str(i),[-w*.13,0,0],[w*.26,h*.7,w*.7],pivot=[0,0,0],rotation=[0,i*72,0])
    else:
        low=family in {'reptile','shell','crab','scorpion','rodent'};legs=h*(.15 if low else .36)
        length=w*(1.8 if family=='reptile' else 1.3)
        bodyh=h*.5;part('body',[-w*.5,legs,-length/2],[w,bodyh,length])
        neck=h*.45 if family=='giraffe' else 0
        if neck:part('neck',[-w*.2,legs+bodyh,-length*.5],[w*.4,neck,w*.4])
        headw=w*(.48 if family not in {'elephant','hippo'} else .7)
        part('head',[-headw/2,legs+bodyh*.35+neck,-length*.5-headw*.65],[headw,bodyh*.7,headw],uv=(0,th/2))
        for i in range(4):
            x=(-1 if i%2==0 else 1)*w*.33;z=(-1 if i//2==0 else 1)*length*.32
            part('leg_'+str(i),[x-w*.13,0,z-w*.13],[w*.26,max(.5,legs),w*.26],pivot=[x,legs,z],uv=(tw/2,0))
        if family in {'reptile','cat','horse','rodent','scorpion'}:part('tail',[-w*.1,legs,length*.4],[w*.2,bodyh*.3,w*(1 if family=='reptile' else .7)],uv=(tw/2,th/2))
        if family in {'elephant','rhino','deer','giraffe','boar'}:
            if family=='elephant':
                part('trunk',[-w*.13,h*.15,-length*.5-headw*.75],[w*.26,h*.6,w*.25],uv=(0,th/2))
                for sign in [-1,1]:part('ear_'+str(sign),[sign*headw*.5-(w*.3 if sign<0 else 0),h*.5,-length*.45],[w*.3,h*.35,w*.1],uv=(tw/2,th/2))
            else:
                for sign in [-1,1]:part('horn_'+str(sign),[sign*w*.16-w*.04,legs+bodyh+neck,-length*.5],[w*.08,h*.25,w*.1],uv=(0,0))
        if family in {'crab','scorpion'}:
            for sign in [-1,1]:part('claw_'+str(sign),[sign*w*.7-w*.12,h*.25,-w*.6],[w*.24,h*.35,w*.5],uv=(0,th/2))
    write(ASSET/f"geckolib/models/entity/menagerie/{e['id']}.geo.json",{'format_version':'1.12.0','minecraft:geometry':[{'description':{'identifier':'geometry.'+e['id'],'texture_width':tw,'texture_height':th,'visible_bounds_width':max(4,e['width']*3),'visible_bounds_height':max(4,e['height']*2),'visible_bounds_offset':[0,e['height']/2,0]},'bones':bones}]})
    walk={};fly={};swim={}
    for i,b in enumerate(bones):
        n=b['name']
        if n.startswith('leg'):walk[n]={'rotation':[f'math.sin(query.anim_time * 360) * {22 if i%2 else -22}',0,0]}
        if n.startswith('wing'):fly[n]={'rotation':[0,0,f'math.sin(query.anim_time * 720) * {35 if n.endswith("1") and not n.endswith("-1") else -35}']}
        if n.startswith(('tail','segment','tentacle','fin')):swim[n]={'rotation':[0,f'math.sin(query.anim_time * 240 + {i*25}) * 18',0]}
    animations={}
    for name,b in [('idle',{}),('walk',walk),('fly',fly),('swim',swim),('sit',{'root':{'position':[0,-h*.2,0]}})]:animations[name]={'loop':True,'animation_length':1,'bones':b}
    write(ASSET/f"geckolib/animations/entity/menagerie/{e['id']}.animation.json",{'format_version':'1.8.0','animations':animations})

def main():
    lang=read(ASSET/'lang/en_us.json');catalog={'species':[],'items':{},'blocks':{},'sounds':[]}
    # Preserve all texture and sound variants and animation sidecars in runtime copies.
    for group,source in [('wildlife',NATURE),('mount',MOUNTS)]:
        for kind in ['textures','sounds']:
            for p in (source/kind).rglob('*'):
                if p.is_file():copy(p,ASSET/kind/'menagerie'/group/p.relative_to(source/kind))
    names=read(NATURE/'lang/en_us.json')
    for row in ROSTER.strip().splitlines():
        name,shape,move,habitat,mood,w,h,hp,feed=row.split()
        e={'source':name,'id':'wildlife_'+name,'name':names['entity.naturalist.'+name],'shape':shape,'movement':move,'habitat':habitat,'mood':mood,'width':float(w),'height':float(h),'health':int(hp),'feed':'minecraft:'+feed,'mount':False,'model':'wildlife_'+name,'variants':wildlife_variants(name),'clips':['idle','walk','fly','swim','sit']}
        catalog['species'].append(e);animal_geometry(e)
    for model,name,texture,move,habitat,w,h in MOUNT_SPECS:
        variants=[]
        for p in sorted((MOUNTS/'textures/model/entity').glob(texture+'*.png')):
            if '_frame' in p.stem and not p.stem.endswith('_frame1'):continue
            tex='menagerie/mount/model/entity/'+p.stem
            variants.append({'name':p.stem,'texture':tex,'babyTexture':tex,'weight':1})
        if not variants:raise ValueError('Missing mount textures '+name)
        clips=list(read(MOUNTS/f'animations/{model}.animation.json')['animations'])
        e={'source':name,'id':'mount_'+name,'name':name.replace('_',' ').title(),'shape':model,'movement':move,'habitat':habitat,'mood':'neutral','width':w,'height':h,'health':40 if move!='air' else 48,'feed':'minecraft:golden_carrot','mount':True,'model':'mount_'+name,'variants':variants,'clips':clips}
        catalog['species'].append(e)
        copy(MOUNTS/f'geo/{model}.geo.json',ASSET/f'geckolib/models/entity/menagerie/mount_{name}.geo.json')
        copy(MOUNTS/f'animations/{model}.animation.json',ASSET/f'geckolib/animations/entity/menagerie/mount_{name}.animation.json')
    for e in catalog['species']:
        lang['entity.jugcraft.'+e['id']]=e['name']
        key=e['id']+'_spawn_egg';catalog['items'][key]={'name':e['name']+' Spawn Egg','kind':'spawn','species':e['id']}
        # Spawn eggs use the shared vanilla model with per-species tints.
        write(ASSET/f'items/{key}.json',{'model':{'type':'minecraft:model','model':'minecraft:item/cow_spawn_egg','tints':[{'type':'minecraft:constant','value':0x728265},{'type':'minecraft:constant','value':0xCAB792}]}})
    for k,n in names.items():
        if k.startswith('item.naturalist.') and not k.endswith('_spawn_egg'):
            source=k.split('.')[-1];catalog['items']['wildlife_'+source]={'name':n,'kind':'item','source':source}
        if k.startswith('block.naturalist.'):
            source=k.split('.')[-1];catalog['blocks']['wildlife_'+source]={'name':n,'source':source}
    # Source glow-goop is a block without a block translation.
    catalog['blocks']['wildlife_glow_goop']={'name':'Glow Goop','source':'glow_goop'}
    for p in (NATURE/'models').rglob('*.json'):write(ASSET/'models/menagerie/wildlife'/p.relative_to(NATURE/'models'),remap(read(p),resource=True))
    for p in (NATURE/'items').glob('*.json'):
        # Keep the project's uniform spawn-egg definition above.
        if p.stem.endswith('_spawn_egg'):continue
        write(ASSET/f'items/wildlife_{p.name}',remap(read(p),resource=True))
    for p in (NATURE/'blockstates').glob('*.json'):write(ASSET/f'blockstates/wildlife_{p.name}',remap(read(p),resource=True))
    sounds=read(ASSET/'sounds.json')
    for k,v in read(NATURE/'sounds.json').items():
        key='wildlife_'+k;v=remap(v,resource=True)
        v.pop('subtitle',None);sounds[key]=v;catalog['sounds'].append(key)
    for p in (MOUNTS/'sounds').glob('*.ogg'):
        key='mount_'+p.stem;sounds[key]={'sounds':['jugcraft:menagerie/mount/'+p.stem]};catalog['sounds'].append(key)
    write(ASSET/'sounds.json',sounds)
    for kind in ['recipe','loot_table','tags','jukebox_song']:
        for p in (NATURE/'naturalist'/kind).rglob('*.json'):
            value=remap(read(p));rel=p.relative_to(NATURE/'naturalist'/kind)
            if kind=='recipe' and value.get('type')=='jugcraft:wildlife_net':continue
            destkind={'recipes':'recipe','loot_tables':'loot_table'}.get(kind,kind)
            # Prefix the leaf, preserving vanilla registry subdirectories.
            if kind=='tags':rel=Path(*[{'items':'item','blocks':'block','entity_types':'entity_type'}.get(x,x) for x in rel.parts])
            write(DATA/destkind/rel.parent/('wildlife_'+rel.name),value)
    for key,v in catalog['blocks'].items():
        catalog['items'].pop(key,None);lang['block.jugcraft.'+key]=v['name']
        if not (ASSET/f'items/{key}.json').exists():write(ASSET/f'items/{key}.json',{'model':{'type':'minecraft:model','model':'jugcraft:menagerie/wildlife/block/'+v['source']}})
    for key,v in catalog['items'].items():lang['item.jugcraft.'+key]=v['name']
    write(ASSET/'lang/en_us.json',lang)
    write(RES/'jugcraft/menagerie/catalog.json',catalog)
    write(ROOT/'art/owner-library/catalog/edited-2026-10-18/runtime-files.json',PROVENANCE)
    print('Generated',len(catalog['species']),'species,',len(catalog['blocks']),'blocks,',len(catalog['items']),'items; originals untouched.')

if __name__=='__main__':main()
