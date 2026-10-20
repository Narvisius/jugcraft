"""Native landscape adaptation; serialized generation uses the shared World Designer registry.

Preserve originals and notices. Do not run supplied class files, scripts or shaders.
"""
import copy
import hashlib
import re
import shutil
from pathlib import Path
from menagerie_content import ROOT,RES,ASSET,DATA,read,write
from menagerie_data import loot
from ecology_content import classify,decor_definition
import nouveau_nbt
import owner_template_data

SOURCE=ROOT/'art/owner-library/originals/Edited-2026-10-18-Part2/jims biomes'
GROUP='landscape';NAMESPACES={'biomesoplenty','terralith'};COPIES=[];ADAPTATIONS=[]
FLUIDS={'blood':0xFF8F2333,'liquid_null':0xFF341F4B}
FLOWERS=set('barley blue_hydrangea burning_blossom bush cattail clover dead_grass desert_grass dune_grass endbloom enderphyte eyebulb flower_bud glowflower glowshroom goldenrod icy_iris lavender marigold orange_cosmos origin_dandelion origin_rose pink_daffodil pink_hibiscus reed sea_oats sprout tiny_cactus toadstool tundra_shrub violet watergrass waterlily white_lavender wilted_lily'.split())


def target(path):
    parent,_,leaf=path.rpartition('/');return (parent+'/' if parent else '')+GROUP+'_'+leaf


def adapt(v,resource=False):
    if isinstance(v,str):
        if v.startswith('#'):return '#'+adapt(v[1:],resource)
        if ':' in v and v.split(':')[0] in NAMESPACES:
            path=v.split(':',1)[1];return 'jugcraft:'+('menagerie/'+GROUP+'/'+path if resource else target(path))
        return v
    if isinstance(v,list):return [adapt(x,resource) for x in v]
    if isinstance(v,dict):return {adapt(k,resource):adapt(x,resource) for k,x in v.items()}
    return v


def copied(p,d):
    d.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,d);COPIES.append({'source':p.relative_to(SOURCE).as_posix(),'runtime':d.relative_to(RES).as_posix(),'sha256':hashlib.sha256(p.read_bytes()).hexdigest()})


def state(block):return {'id':block if ':' in block else 'jugcraft:landscape_'+block}


DETAILS={
 'anomaly':('crystal','anomaly','minecraft:air',2,5), 'barnacles':('patch','barnacles','minecraft:air',3,1),
 'big_dripleaf':('patch','minecraft:big_dripleaf','minecraft:air',4,1), 'big_pumpkin':('rock','minecraft:pumpkin','minecraft:air',2,3),
 'black_sand_splatter':('surface','black_sand','minecraft:air',4,1), 'bone_spine':('spine','minecraft:bone_block','minecraft:air',1,8),
 'bramble':('patch','bramble','minecraft:air',4,1), 'corner_cobwebs':('patch','minecraft:cobweb','minecraft:air',3,1),
 'crag_moss':('surface','minecraft:moss_block','minecraft:air',3,1), 'crag_splatter':('surface','minecraft:gravel','minecraft:air',3,1),
 'dead_coral_patch':('patch','minecraft:dead_tube_coral','minecraft:air',3,1), 'dripstone_splatter':('surface','minecraft:dripstone_block','minecraft:air',4,1),
 'endscraper':('pillar','minecraft:purpur_block','minecraft:air',3,24), 'eroded_pillar':('pillar','minecraft:terracotta','minecraft:air',3,12),
 'extra_glow_lichen':('patch','minecraft:glow_lichen','minecraft:air',3,1), 'flesh_tendon':('pillar','flesh_tendons','minecraft:air',1,5),
 'giant_glowshroom':('mushroom','minecraft:mushroom_stem','glowshroom_block',5,10), 'hanging_flesh_tendon':('vine','flesh_tendons_strand','minecraft:air',1,7),
 'high_grass':('patch','high_grass','minecraft:air',3,1), 'hot_spring_vents':('vent','minecraft:stone','minecraft:magma_block',1,2),
 'huge_clover':('clover','flower_stem','huge_clover_petal',3,4), 'huge_flower':('flower','flower_stem','pink_flower_petal_block',3,6),
 'huge_glowshroom':('mushroom','minecraft:mushroom_stem','glowshroom_block',3,6), 'huge_lily_pad':('pad','huge_lily_pad','minecraft:air',2,1),
 'huge_toadstool':('mushroom','minecraft:mushroom_stem','toadstool_block',3,5), 'inferno_splatter':('surface','minecraft:magma_block','minecraft:air',3,1),
 'jagged_sandstone':('pillar','minecraft:sandstone','minecraft:air',2,8), 'large_fumarole':('vent','orpiment','orpiment_fumarole',2,6),
 'large_rose_quartz':('crystal','rose_quartz_block','minecraft:air',3,12), 'lumaloop':('patch','lumaloop','minecraft:air',3,1),
 'medium_glowshroom':('mushroom','minecraft:mushroom_stem','glowshroom_block',2,4), 'monolith':('pillar','minecraft:obsidian','minecraft:air',2,15),
 'mossy_black_sand_splatter':('surface','mossy_black_sand','minecraft:air',4,1), 'moss_splatter':('surface','minecraft:moss_block','minecraft:air',4,1),
 'mud_splatter':('surface','minecraft:mud','minecraft:air',4,1), 'mycelium_splatter':('surface','minecraft:mycelium','minecraft:air',4,1),
 'nether_vines':('vine','minecraft:weeping_vines','minecraft:air',1,7), 'obsidian_splatter':('surface','minecraft:obsidian','minecraft:air',3,1),
 'origin_gravel_cliffs':('surface','minecraft:gravel','minecraft:air',5,1), 'pumpkin_patch':('patch','minecraft:pumpkin','minecraft:air',5,1),
 'rainforest_cliffs_vines':('vine','minecraft:vine','minecraft:air',1,12), 'rooted_stump':('pillar','minecraft:oak_log','minecraft:air',1,2),
 'scattered_rocks':('rock','minecraft:cobblestone','minecraft:air',1,2), 'scrub':('patch','tundra_shrub','minecraft:air',4,1),
 'short_bamboo':('pillar','minecraft:bamboo','minecraft:air',1,3), 'small_brown_mushroom':('patch','minecraft:brown_mushroom','minecraft:air',3,1),
 'small_crystal':('crystal','rose_quartz_block','minecraft:air',1,4), 'small_dripleaf':('patch','minecraft:small_dripleaf','minecraft:air',3,1),
 'small_fumarole':('vent','orpiment','orpiment_fumarole',1,3), 'small_glowshroom':('patch','glowshroom','minecraft:air',3,1),
 'small_red_mushroom':('patch','minecraft:red_mushroom','minecraft:air',3,1), 'small_toadstool':('patch','toadstool','minecraft:air',3,1),
 'sparse_dune_grass':('patch','dune_grass','minecraft:air',2,1), 'stringy_cobweb':('vine','stringy_cobweb','minecraft:air',1,5),
 'termite_mound':('pillar','minecraft:packed_mud','minecraft:air',2,5), 'thin_bamboo':('column','minecraft:bamboo','minecraft:air',1,6),
 'tidepool':('lake','minecraft:water','minecraft:stone',3,2), 'webbing':('patch','webbing','minecraft:air',3,1), 'wispjelly':('patch','wispjelly','minecraft:air',3,1)
}


def generation(d):
    if isinstance(d,list):return [generation(x) for x in d]
    if not isinstance(d,dict):return d
    typ=d.get('type','')
    if typ.startswith('biomesoplenty:'):
        name=typ.split(':')[1]
        if name in ('binary','wisp_bubble','end_spore'):return {'type':'jugcraft:landscape_'+name}
        if name=='dead_coral_block':return {'type':'minecraft:simple_block','to_place':adapt(d['to_place'])}
        if name.endswith('_tree'):
            style=name[:-5];height=d.get('min_height',4);maximum=d.get('max_height',8)
            if style=='bush':height=1;maximum=2
            return {'type':'jugcraft:landscape_tree','style':style,'trunk':adapt(d.get('trunk_provider',state('minecraft:oak_log'))),'leaves':adapt(d.get('foliage_provider',state('minecraft:oak_leaves'))),'alternate':adapt(d.get('alt_foliage_provider',state('minecraft:air'))),'hanging':adapt(d.get('hanging_provider',state('minecraft:air'))),'min_height':max(1,min(40,height)),'max_height':max(1,min(40,maximum)),'width':max(1,min(4,d.get('trunk_width',1))),'radius':max(1,min(6,d.get('max_leaves_radius',3)))}
        if name=='lake':return {'type':'jugcraft:landscape_detail','shape':'lake','primary':adapt(d['fluid']),'secondary':adapt(d['barrier']),'radius':5,'height':3}
        if name.startswith('fallen_'):
            wood=name.removeprefix('fallen_').removesuffix('_log');block='minecraft:oak_log' if wood=='log' else 'minecraft:birch_log' if wood=='birch' else wood+'_log'
            spec=('log',block,'minecraft:air',1,5)
        else:spec=DETAILS[name]
        shape,a,b,r,h=spec;return {'type':'jugcraft:landscape_detail','shape':shape,'primary':state(a),'secondary':state(b),'radius':r,'height':h}
    out={k:generation(v) for k,v in d.items()}
    if typ and ':' not in typ:out['type']='minecraft:'+typ
    return adapt(out)


def sound_references(v):
    if isinstance(v,list):return [sound_references(x) for x in v]
    if not isinstance(v,dict):return v
    out={k:sound_references(x) for k,x in v.items()}
    for k in ('sound','sound_event','sound_id','loop'):
        value=out.get(k)
        if isinstance(value,str) and value.startswith('jugcraft:landscape_'):out[k]='jugcraft:landscape.'+value.removeprefix('jugcraft:landscape_')
    return out


def main():
    names=read(SOURCE/'lang/en_us.json');lang=read(ASSET/'lang/en_us.json');sounds=read(ASSET/'sounds.json')
    catalog={'blocks':{},'items':{},'fluids':FLUIDS,'species':[],'sounds':[],'biomes':[]}
    for folder in ('textures','sounds'):
        for p in (SOURCE/folder).rglob('*'):
            if p.is_file():copied(p,ASSET/folder/'menagerie/landscape'/p.relative_to(SOURCE/folder))
    for p in (SOURCE/'models').rglob('*.json'):
        d=adapt(read(p),True);d.pop('render_type',None);d.pop('loader',None);write(ASSET/'models/menagerie/landscape'/p.relative_to(SOURCE/'models'),d)
    for p in (SOURCE/'particles').glob('*.json'):write(ASSET/f'particles/landscape_{p.name}',adapt(read(p),True))
    for p in (SOURCE/'blockstates').glob('*.json'):
        n=p.stem;k=classify(n);d=read(p)
        if n in FLOWERS or n in ('purple_wildflowers','white_petals'):k='flower'
        if n in ('tall_lavender','tall_white_lavender'):k='double_plant'
        if n in ('black_sand','orange_sand','white_sand'):k='sand'
        if n.endswith('_campfire'):k='campfire'
        if n in FLUIDS:k='fluid'
        if n.endswith('_vertical_slab'):k='decor'
        if k=='building' and any(key for key in d.get('variants',{}) if key) or 'multipart' in d:k='decor' if k=='building' else k
        if k=='fruit':k='decor'
        definition=decor_definition(SOURCE,'biomesoplenty',n,k,d)
        light=12 if any(x in n for x in ('glow','lumaloop','burning','fumarole','wispjelly')) else 0
        wood=k in ('log','leaves','sapling','fence','fence_gate','door','trapdoor','button','pressure_plate','sign','wall_sign','hanging_sign','wall_hanging_sign','shelf') or n.endswith('_planks')
        catalog['blocks']['landscape_'+n]={'source':n,'kind':k,'wood':wood,'light':light,'definition':definition}
        write(ASSET/f'blockstates/landscape_{n}.json',adapt(d,True));lang['block.jugcraft.landscape_'+n]=names.get('block.biomesoplenty.'+n,n.replace('_',' ').title())
    for p in (SOURCE/'items').glob('*.json'):
        n=p.stem;catalog['items']['landscape_'+n]={'source':n};write(ASSET/f'items/landscape_{n}.json',adapt(read(p),True));lang['item.jugcraft.landscape_'+n]=names.get('item.biomesoplenty.'+n,n.replace('_',' ').title())
    # The common fluid engine uses the same authored sprites under its shared sprite convention.
    for n in FLUIDS:
        for suffix in ('still','flow'):
            source=SOURCE/f'textures/block/{n}_{suffix}.png'
            if not source.exists():source=SOURCE/f'textures/block/{n}.png'
            copied(source,ASSET/f'textures/block/landscape_{n}_{suffix}.png')
            if source.with_suffix('.png.mcmeta').exists():copied(source.with_suffix('.png.mcmeta'),ASSET/f'textures/block/landscape_{n}_{suffix}.png.mcmeta')
    for folder in ('recipe','loot_table','tags','jukebox_song','damage_type'):
        for p in (SOURCE/folder).rglob('*.json'):
            rel=p.relative_to(SOURCE/folder);value=sound_references(adapt(read(p)));write(DATA/folder/rel.parent/('landscape_'+p.name),value)
    for folder in ('feature','placed_feature','placement','block_state_provider','material_rule','carver','biome','structure','structure_set','processor_list','template_pool','noise','density_function'):
        for p in (SOURCE/'worldgen'/folder).rglob('*.json'):
            rel=p.relative_to(SOURCE/'worldgen'/folder);original=read(p);value=sound_references(generation(original))
            write(DATA/'worldgen'/folder/rel.parent/('landscape_'+p.name),value)
            if folder=='feature' and original.get('type','').startswith('biomesoplenty:'):ADAPTATIONS.append({'source':rel.as_posix(),'native':value})
            if folder=='biome':catalog['biomes'].append({'id':'jugcraft:'+target(rel.with_suffix('').as_posix()),'source':rel.with_suffix('').as_posix(),'temperature':original.get('temperature',.5),'downfall':original.get('downfall',.5)})
    from beasts_content import recipe,modern_loot
    for old,new in [('recipes','recipe'),('loot_tables','loot_table')]:
        for p in (SOURCE/old).rglob('*.json'):
            rel=p.relative_to(SOURCE/old);d=adapt(read(p));d=recipe(d) if new=='recipe' else modern_loot(d)
            write(DATA/new/rel.parent/('landscape_'+p.name),sound_references(d))
    import biomes
    dimension = biomes.owner_dimension_file()
    write(RES/'jugcraft/menagerie/landscape-dimensions.json',dimension)
    # Explicit sapling routes do not depend on guessing which natural placement selector grows a wood.
    for id,b in catalog['blocks'].items():
        if b['kind']!='sapling':continue
        wood=b['source'].removesuffix('_sapling');log=wood+'_log';leaf=wood+'_leaves'
        if 'landscape_'+log not in catalog['blocks']:log='minecraft:oak_log'
        if 'landscape_'+leaf not in catalog['blocks']:leaf='pink_maple_leaves' if wood=='maple' else 'minecraft:oak_leaves'
        write(DATA/f'worldgen/feature/landscape_sapling_{wood}.json',{'type':'jugcraft:landscape_tree','style':'palm' if wood=='palm' else 'redwood' if wood=='redwood' else 'taiga' if wood in ('fir','pine') else 'basic','trunk':state(log),'leaves':state(leaf),'min_height':5,'max_height':10 if wood!='redwood' else 24,'radius':3,'width':1})
    for p in list((SOURCE/'structure').rglob('*.nbt'))+list((SOURCE/'structures').rglob('*.nbt')):
        rel=p.relative_to(SOURCE/('structure' if 'structure' in p.parts else 'structures'));out=DATA/'structure'/rel.parent/('landscape_'+p.name);out.parent.mkdir(parents=True,exist_ok=True);out.write_bytes(owner_template_data.adapt(p.read_bytes(),adapt));ADAPTATIONS.append({'source':p.relative_to(SOURCE).as_posix(),'runtime':out.relative_to(RES).as_posix(),'changes':'Native namespaces and 26.3 structure palette schema.'})
    for key,event in read(SOURCE/'sounds.json').items():
        event=adapt(event,True);sounds['landscape.'+key]=event;catalog['sounds'].append('landscape.'+key)
    write(ASSET/'sounds.json',sounds);write(ASSET/'lang/en_us.json',lang);write(RES/'jugcraft/menagerie/landscape.json',catalog)
    atlas=read(RES/'assets/minecraft/atlases/blocks.json');entry={'type':'minecraft:directory','source':'menagerie/landscape','prefix':'menagerie/landscape/'}
    if entry not in atlas['sources']:atlas['sources'].append(entry)
    write(RES/'assets/minecraft/atlases/blocks.json',atlas)
    write(ROOT/'art/owner-library/catalog/edited-2026-10-18-part2/landscape-runtime.json',{'copies':COPIES,'adaptations':ADAPTATIONS,'blocks':len(catalog['blocks']),'items':len(catalog['items']),'biomes':len(catalog['biomes']),'validation':'Deferred until the complete batch is implemented. Native shape adapters require visual review.'})
    print('Prepared landscape resources:',len(catalog['blocks']),'blocks;',len(catalog['biomes']),'biomes.')


if __name__=='__main__':main()
