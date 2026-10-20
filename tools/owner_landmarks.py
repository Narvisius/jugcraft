"""Native jigsaw entries for supplied ruins and reconstructed procedural cave landmarks.

Original templates are retained. The old custom structure implementations were
not supplied as source; the bounded landmark shapes below are newly authored.
"""
import math
from pathlib import Path
from menagerie_content import ROOT,RES,DATA,read,write
import nouveau_nbt
import owner_template_data
from ecology_content import adapt as ecology_adapt

BASE=ROOT/'art/owner-library/originals/Edited-2026-10-18-Part2'


def template(path,blocks):
    minimum=[min(p[a] for p in blocks) for a in range(3)];maximum=[max(p[a] for p in blocks) for a in range(3)]
    palette=[];indices={};entries=[]
    for pos,name in blocks.items():
        if ':' not in name:name='jugcraft:cavern_'+name
        if name not in indices:indices[name]=len(palette);palette.append({'id':(8,name)})
        entries.append({'pos':(9,(3,[pos[a]-minimum[a] for a in range(3)])),'state':(3,indices[name])})
    root={'DataVersion':(3,5023),'size':(9,(3,[maximum[a]-minimum[a]+1 for a in range(3)])),'palette':(9,(10,palette)),'blocks':(9,(10,entries)),'entities':(9,(10,[]))}
    out=DATA/'structure'/(path+'.nbt');out.parent.mkdir(parents=True,exist_ok=True);out.write_bytes(nouveau_nbt.write((10,'',root)))


def register(name,locations,biomes,height=-36,surface=False,salt=182001):
    write(DATA/f'worldgen/template_pool/{name}.json',{'name':'jugcraft:'+name,'fallback':'minecraft:empty','elements':[{'weight':1,'element':{'element_type':'minecraft:single_pool_element','location':'jugcraft:'+location,'projection':'rigid','processors':'minecraft:empty'}} for location in locations]})
    d={'type':'minecraft:jigsaw','biomes':biomes,'step':'surface_structures' if surface else 'underground_structures','spawn_overrides':{},'terrain_adaptation':'beard_thin' if surface else 'none','start_pool':'jugcraft:'+name,'size':0,'start_height':{'absolute':height},'max_distance_from_center':80,'use_expansion_hack':False}
    if surface:d['project_start_to_heightmap']='WORLD_SURFACE_WG'
    write(DATA/f'worldgen/structure/{name}.json',d)
    write(DATA/f'worldgen/structure_set/{name}.json',{'structures':[{'structure':'jugcraft:'+name,'weight':1}],'placement':{'type':'minecraft:random_spread','spacing':32,'separation':16,'salt':salt}})


def shape(name):
    blocks={}
    def put(x,y,z,b):blocks[(x,y,z)]=b
    if name=='donut_arch':
        for x in range(-12,13):
            for y in range(22):
                r=math.sqrt(x*x+(y-10)**2)
                if 6<=r<=10:
                    for z in range(-2,3):put(x,y,z,'block_of_frosting' if y>16 else 'block_of_chocolate')
    elif name=='forlorn_bridge':
        for x in range(-14,15):
            for z in range(-2,3):put(x,5,z,'thornwood_planks')
            if x%2==0:
                for z in (-2,2):put(x,6,z,'thornwood_fence')
        for x in (-13,13):
            for z in (-2,2):
                for y in range(-3,9):put(x,y,z,'thornwood_log')
    elif name=='volcano':
        for y in range(20):
            radius=16-y*.6
            for x in range(-16,17):
                for z in range(-16,17):
                    r=math.hypot(x,z)
                    if radius-2<=r<=radius:put(x,y,z,'primal_magma' if y>16 else 'flood_basalt')
                    elif y==16 and r<4:put(x,y,z,'minecraft:lava')
    elif name=='forlorn_canyon':
        for x in range(-14,15):
            for z in range(-14,15):
                for y in range(18):
                    if abs(x)>6+int(math.sin(z*.4)*2) or y==0:put(x,y,z,'guanostone' if y>1 else 'guano_block')
    else:
        wall,floor,liquid={'acid_pit':('radrock','radrock','acid'),'dino_bowl':('limestone','minecraft:grass_block',None),'ferrocave':('galena','galena',None),'cake_cave':('block_of_chocolate','block_of_frosting',None),'ocean_trench':('abyssmarine','abyssmarine','minecraft:water')}[name]
        for x in range(-14,15):
            for z in range(-14,15):
                r=math.hypot(x,z)
                if r>14:continue
                put(x,0,z,wall);put(x,1,z,floor)
                for y in range(2,18):
                    if r>12:put(x,y,z,wall)
                    elif name in ('ferrocave','cake_cave') and y==17:put(x,y,z,floor if name=='cake_cave' else wall)
                    elif liquid and y<(5 if name=='acid_pit' else 15):put(x,y,z,liquid)
    return blocks


def gingerbread_town():
    """Place every supplied house around the supplied centre, with walkable connecting paths."""
    blocks={};palette=[];index={};entities=[]
    def palette_id(state):
        key=repr(state)
        if key not in index:index[key]=len(palette);palette.append(state)
        return index[key]
    floor=palette_id({'id':(8,'jugcraft:cavern_wafer_cookie_block')})
    path=palette_id({'id':(8,'jugcraft:cavern_block_of_frosting')})
    for x in range(55):
        for z in range(55):
            if math.hypot(x-27,z-27)<27:blocks[(x,0,z)]={'pos':(9,(3,[x,0,z])),'state':(3,floor)}
    names=['gingerbread_town_center']+['gingerbread_house_'+str(i) for i in range(10)]
    for i,name in enumerate(names):
        root=nouveau_nbt.read((DATA/'structure'/('cavern_'+name+'.nbt')).read_bytes())[2];size=root['size'][1][1]
        cx=27 if i==0 else round(27+20*math.cos((i-1)*math.tau/10));cz=27 if i==0 else round(27+20*math.sin((i-1)*math.tau/10));offset=(cx-size[0]//2,1,cz-size[2]//2)
        lookup=[palette_id(state) for state in root['palette'][1][1]]
        for entry in root['blocks'][1][1]:
            at=tuple(entry['pos'][1][1][a]+offset[a] for a in range(3));entry=dict(entry);entry['pos']=(9,(3,list(at)));entry['state']=(3,lookup[entry['state'][1]]);blocks[at]=entry
        for n in range(1,21):
            x=round(27+(cx-27)*n/20);z=round(27+(cz-27)*n/20)
            for dx in (-1,0,1):blocks[(x+dx,0,z)]={'pos':(9,(3,[x+dx,0,z])),'state':(3,path)}
    root={'DataVersion':(3,5023),'size':(9,(3,[55,24,55])),'palette':(9,(10,palette)),'blocks':(9,(10,list(blocks.values()))),'entities':(9,(10,entities))}
    out=DATA/'structure/cavern/landmarks/gingerbread_town.nbt';out.parent.mkdir(parents=True,exist_ok=True);out.write_bytes(nouveau_nbt.write((10,'',root)))


def main():
    for p in (BASE/'jimbologics/structure').rglob('*.nbt'):
        rel=p.relative_to(BASE/'jimbologics/structure');target=rel.parent/('ecology_'+p.name);out=DATA/'structure'/target;out.parent.mkdir(parents=True,exist_ok=True);out.write_bytes(owner_template_data.adapt(p.read_bytes(),ecology_adapt))
    ruins=['desert_ruin/ecology_'+p.stem for p in (BASE/'jimbologics/structure/desert_ruin').glob('*.nbt')]
    register('ecology_desert_ruins',ruins,'minecraft:desert',0,True,182001)
    provided={'underground_cabin':('magnetic_caves','underground_cabin_*'),'abyssal_ruins':('abyssal_chasm','abyssal_ruins_*'),'licowitch_tower':('candy_cavity','licowitch_tower'),'soda_bottle':('candy_cavity','soda_bottle'),'gingerbread_town':('candy_cavity','gingerbread_town_center')}
    for i,(name,(biome,pattern)) in enumerate(provided.items()):
        locations=['cavern_'+p.stem for p in (BASE/'jims caves/structures').glob(pattern+'.nbt')]
        if name=='gingerbread_town':gingerbread_town();locations=['cavern/landmarks/gingerbread_town']
        register('cavern_'+name,locations,'jugcraft:cavern_'+biome,salt=182010+i)
    created={}
    for i,(name,biome) in enumerate({'ferrocave':'magnetic_caves','dino_bowl':'primordial_caves','volcano':'primordial_caves','acid_pit':'toxic_caves','ocean_trench':'abyssal_chasm','forlorn_canyon':'forlorn_hollows','forlorn_bridge':'forlorn_hollows','cake_cave':'candy_cavity','donut_arch':'candy_cavity'}.items()):
        blocks=shape(name);path='cavern/landmarks/'+name;template(path,blocks);register('cavern_'+name,[path],'jugcraft:cavern_'+biome,salt=182030+i);created[name]=len(blocks)
    write(ROOT/'art/owner-library/catalog/edited-2026-10-18-part2/landmark-runtime.json',{'provided_ecology_templates':len(ruins),'provided_cave_landmarks':list(provided),'newly_authored_cave_shapes':created,'validation':'Not tested; owner requested all implementation first.'})


if __name__=='__main__':main()
