"""Native replacement layouts for missing owner templates, using Jugcraft's town builder.

These thirteen layouts are new implementations of the supplied structure concepts;
they are not the 153 absent template files. Supplied pocket-pet NBT is adapted separately.
"""
from menagerie_content import *
from town_voxels import Voxels,S
from town_buildings import house,style
from retro_game_shop import nbt_bytes,DATA_VERSION
import nouveau_nbt,random,math

CONCEPTS={'badlands_pyramid':'#minecraft:is_badlands','black_spiral':'#minecraft:is_nether','campsite':'#minecraft:is_forest',
 'dark_forest_settlement':'minecraft:dark_forest','desert_shrine':'minecraft:desert','end_shipwreck':'minecraft:end_highlands',
 'farmstead':'minecraft:plains','guide_post_cold':'#minecraft:is_taiga','guide_post_warm':'#minecraft:is_savanna',
 'mangrove_hut':'minecraft:mangrove_swamp','mausoleum':'minecraft:dark_forest','ruins':'#minecraft:is_jungle','tavern':'minecraft:plains'}

def template(v,loot):
    x0,y0,z0,x1,y1,z1=v.bounds();states=sorted(set(v.b.values()));palette=[]
    for s in states:
        name,_,props=s.partition('[');p={'id':name}
        if props:p['properties']=dict(x.split('=') for x in props.rstrip(']').split(','))
        palette.append(p)
    blocks=[]
    for (x,y,z),s in sorted(v.b.items()):
        record={'pos':[x-x0,y-y0,z-z0],'state':states.index(s)}
        if s.startswith('minecraft:chest'):record['nbt']={'id':'minecraft:chest','LootTable':'jugcraft:chests/'+loot}
        blocks.append(record)
    return {'DataVersion':DATA_VERSION,'size':[x1-x0+1,y1-y0+1,z1-z0+1],'palette':palette,'blocks':blocks,'entities':[]}

def build(name,index):
    rng=random.Random(181026+index);v=Voxels()
    if name in {'tavern','farmstead','mangrove_hut','dark_forest_settlement'}:
        st=style(rng,wood='mangrove' if name=='mangrove_hut' else 'spruce')
        v=house(rng,11,13,2,purpose='tavern' if name=='tavern' else 'home',st=st).v
        if name=='farmstead':
            for x in range(13,23):
                for z in range(2,12):v.set(x,0,z,S('water') if x==18 else S('farmland',moisture=7));v.set(x,1,z,S('air') if x==18 else S('wheat',age=7))
        if name=='dark_forest_settlement':v.paste(house(rng,9,9,1,purpose='shop').v,16,0,0,origin_size=(9,9))
        if name=='mangrove_hut':
            for x in [0,10]:
                for z in [0,12]:v.box(x,-4,z,x,-1,z,S('mangrove_log',axis='y'))
    elif name in {'badlands_pyramid','desert_shrine'}:
        stone='red_sandstone' if name.startswith('badlands') else 'smooth_sandstone'
        for y in range(7):v.box(y,y,y,16-y,y,16-y,S(stone))
        v.box(4,1,4,12,4,12,S('air'));v.box(7,1,0,9,3,8,S('air'));v.set(8,1,11,S('chest',facing='north'));v.set(8,5,8,S('sea_lantern'))
        for x,z in [(4,4),(4,12),(12,4),(12,12)]:v.box(x,1,z,x,5,z,S('chiseled_sandstone'))
    elif name=='black_spiral':
        for y in range(26):
            for x in range(11):
                for z in range(11):
                    d=math.hypot(x-5,z-5)
                    if 4<=d<=5.3:v.set(x,y,z,S('polished_blackstone_bricks'))
        # One block rises per perimeter step. Each successive step shares an edge,
        # with sixteen steps between laps providing generous player headroom.
        route=[(x,3) for x in range(3,7)]+[(7,z) for z in range(3,7)]+[(x,7) for x in range(7,3,-1)]+[(3,z) for z in range(7,3,-1)]
        v.box(1,0,1,9,0,9,S('polished_blackstone_bricks'))
        for y in range(25):
            x,z=route[y%len(route)];v.set(x,y+1,z,S('polished_blackstone_bricks'))
        v.box(3,25,3,7,25,7,S('polished_blackstone_bricks'))
        v.box(4,1,0,6,3,2,S('air'));v.set(5,26,5,S('chest',facing='north'))
    elif name=='end_shipwreck':
        for z in range(23):
            half=max(1,4-abs(z-11)//4)
            for x in range(6-half,7+half):v.set(x,0,z,S('purpur_block'));v.set(x,1,z,S('air'))
            for x in [6-half,6+half]:v.set(x,1,z,S('purpur_stairs',facing='east' if x<6 else 'west'))
        v.box(6,1,11,6,12,11,S('end_stone_bricks'));v.box(1,7,11,11,10,11,S('purple_wool'));v.set(6,1,18,S('chest',facing='north'))
    elif name in {'guide_post_cold','guide_post_warm','campsite'}:
        v.box(0,0,0,10,0,10,S('coarse_dirt'));v.set(5,1,5,S('campfire',lit='true',facing='north'))
        for x in [1,9]:v.box(x,1,1,x,4,1,S('spruce_log',axis='y'))
        v.box(0,5,0,10,5,3,S('spruce_slab',type='bottom'));v.set(3,1,2,S('barrel'));v.set(7,1,2,S('chest',facing='south'))
        if name=='campsite':
            for y in range(4):v.box(1+y,y+1,7,9-y,y+1,10,S('orange_wool'))
            v.box(4,1,7,6,2,9,S('air'))
    elif name=='mausoleum':
        v.box(0,0,0,12,0,14,S('deepslate_tiles'));v.walls(0,1,0,12,5,14,S('deepslate_bricks'));v.box(0,6,0,12,6,14,S('deepslate_tiles'))
        v.box(5,1,0,7,3,1,S('air'))
        for x in [3,9]:
            for z in [4,8,12]:v.box(x-1,1,z,x+1,1,z+1,S('stone_brick_slab',type='bottom'))
        v.set(6,1,12,S('chest',facing='north'));v.set(6,5,7,S('soul_lantern',hanging='true'))
    else:
        v.box(0,0,0,14,0,14,S('mossy_stone_bricks'))
        for x,z in [(1,1),(1,13),(13,1),(13,13)]:v.box(x,1,z,x,3+rng.randrange(4),z,S('mossy_cobblestone_wall'))
        v.walls(1,1,1,13,2,13,S('mossy_cobblestone'));v.box(6,1,1,8,2,1,S('air'));v.set(7,1,11,S('chest',facing='north'))
    return v

def main():
    for i,(name,biome) in enumerate(CONCEPTS.items()):
        id='menagerie_'+name;v=build(name,i);p=DATA/f'structure/menagerie/{name}.nbt';p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(nbt_bytes(template(v,id)))
        write(DATA/f'worldgen/template_pool/{id}.json',{'name':'jugcraft:'+id,'fallback':'minecraft:empty','elements':[{'weight':1,'element':{'element_type':'minecraft:single_pool_element','location':'jugcraft:menagerie/'+name,'processors':'minecraft:empty','projection':'rigid'}}]})
        write(DATA/f'worldgen/structure/{id}.json',{'type':'minecraft:jigsaw','biomes':biome if biome.startswith('#') else [biome],'step':'surface_structures','spawn_overrides':{},'terrain_adaptation':'beard_thin','start_pool':'jugcraft:'+id,'size':1,'start_height':{'absolute':0},'project_start_to_heightmap':'WORLD_SURFACE_WG','max_distance_from_center':80,'use_expansion_hack':False})
        if name=='black_spiral':
            path=DATA/f'worldgen/structure/{id}.json';value=read(path)
            value.pop('project_start_to_heightmap',None);value['start_height']={'absolute':40};value['terrain_adaptation']='none';write(path,value)
        write(DATA/f'worldgen/structure_set/{id}.json',{'structures':[{'structure':'jugcraft:'+id,'weight':1}],'placement':{'type':'minecraft:random_spread','spacing':48,'separation':20,'salt':181026+i}})
        write(DATA/f'loot_table/chests/{id}.json',{'type':'minecraft:chest','pools':[{'rolls':{'type':'minecraft:uniform','min':2,'max':4},'entries':[{'type':'minecraft:item','name':item,'weight':weight} for item,weight in [('minecraft:bread',5),('minecraft:gold_nugget',5),('jugcraft:wildlife_capture_net',2),('jugcraft:pocket_pet_chicken',1),('jugcraft:mount_summoning_staff',1)]]}]})
    write(ROOT/'art/owner-library/catalog/edited-2026-10-18/structure-adaptation.json',{'implementation':'New native layouts using the existing Jugcraft town/voxel builder. These do not reproduce unavailable source templates.','concepts':list(CONCEPTS),'missingOriginalTemplates':153,'originalsUntouched':True})
    print('Wrote',len(CONCEPTS),'native structure concepts and bounded vanilla jigsaw placement.')

if __name__=='__main__':main()
