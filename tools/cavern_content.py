"""Native cave collection resources, adapted templates and serialized generation profiles."""
import copy
from pathlib import Path
from menagerie_content import ROOT,RES,ASSET,DATA,read,write
from ecology_content import classify,decor_definition
from beasts_content import recipe,modern_loot,sculpt
from cavern_profiles import profiles
import landscape_content as landscape
import nouveau_nbt
import owner_template_data
import hashlib
import shutil

SOURCE=ROOT/'art/owner-library/originals/Edited-2026-10-18-Part2/jims caves'
COPIES=[]
FLUIDS={'acid':0xFF93B51D,'purple_soda':0xFF7C3A98}
CAVES={'magnetic_caves':('galena','galena',-64),'primordial_caves':('limestone','minecraft:grass_block',-64),'toxic_caves':('radrock','radrock',-64),'abyssal_chasm':('abyssmarine','abyssmarine',8),'forlorn_hollows':('guanostone','guano_block',-64),'candy_cavity':('block_of_chocolate','block_of_frosting',-64)}
FLOWERS=set('curly_fern cycad fiddlehead flytrap licoroot_sprout pewen_pines thornwood_branch tree_star underweed sweet_puff'.split())
FOOD=set('trilocaris_tail cooked_trilocaris_tail pine_nuts dinosaur_nugget serene_salad seething_stew primordial_soup radgill cooked_radgill spelunkie slam green_soylent lanternfish cooked_lanternfish tripodfish cooked_tripodfish cooked_mussel deep_sea_sushi_roll stinky_fish vesper_stew darkened_apple purple_soda_bottle hot_chocolate_bottle vanilla_ice_cream_scoop chocolate_ice_cream_scoop sweetberry_ice_cream_scoop sundae caramel caramel_apple gingerbread_crumbs alex_meal biome_treat jelly_bean dinosaur_chop cooked_dinosaur_chop'.split())
ARMOR={'primordial_helmet':'helmet','primordial_tunic':'chestplate','primordial_pants':'leggings','hazmat_mask':'helmet','diving_helmet':'helmet','hood_of_darkness':'helmet','cloak_of_darkness':'chestplate','rainbounce_boots':'boots'}
for material in ('hazmat','diving','gingerbread'):
    for part in ('helmet','chestplate','leggings','boots'):ARMOR.setdefault(material+'_'+part,part)


def target(path):
    a,_,b=path.rpartition('/');return (a+'/' if a else '')+'cavern_'+b


def adapt(v,resource=False):
    if isinstance(v,str):
        if v.startswith('#'):return '#'+adapt(v[1:],resource)
        if v.startswith('alexscaves:'):return 'jugcraft:'+('menagerie/cavern/'+v.split(':',1)[1] if resource else target(v.split(':',1)[1]))
        return v
    if isinstance(v,list):return [adapt(x,resource) for x in v]
    if isinstance(v,dict):return {adapt(k,resource):adapt(x,resource) for k,x in v.items()}
    return v


def copied(p,d):
    d.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,d);COPIES.append({'source':p.relative_to(SOURCE).as_posix(),'runtime':d.relative_to(RES).as_posix(),'sha256':hashlib.sha256(p.read_bytes()).hexdigest()})


def modern(v):
    if isinstance(v,list):return [modern(x) for x in v]
    if not isinstance(v,dict):return v
    d={k:modern(x) for k,x in v.items()}
    if 'Name' in d:d['id']=d.pop('Name')
    if 'Properties' in d:d['properties']=d.pop('Properties')
    if d.get('type')=='minecraft:simple_state_provider':return d['state']
    if 'type' in d and 'value' in d and isinstance(d['value'],dict):d={**d.pop('value'),**d}
    if 'type' in d and 'config' in d:d={**d.pop('config'),**d}
    return adapt(d)


def state(name):return {'id':name if ':' in name else 'jugcraft:cavern_'+name}


def native_feature(original):
    typ=original.get('type','');name=typ.split(':')[-1];d=original.get('config',{})
    if typ.startswith('minecraft:'):return modern(original)
    if 'structures' in d:return {'type':'jugcraft:owner_template','templates':adapt(d['structures']),'sink':min(8,d.get('sink_by',0))}
    if name in ('ancient_tree','giant_ancient_tree','pewen_tree','thornwood_tree','thornwood_tree_with_branches','licoroot_tree','licoroot_tree_with_sprouts'):
        wood='pewen' if 'pewen' in name or 'ancient' in name else 'thornwood' if 'thornwood' in name else 'licoroot'
        leaf='ancient_leaves' if 'ancient' in name else 'pewen_pines' if wood=='pewen' else 'thornwood_branch' if wood=='thornwood' else 'licoroot_vine'
        return {'type':'jugcraft:landscape_tree','style':'big' if 'giant' in name else 'taiga' if wood=='pewen' else 'basic','trunk':state(wood+'_log' if wood!='licoroot' else 'licoroot'),'leaves':state(leaf),'min_height':8 if 'giant' in name else 4,'max_height':24 if 'giant' in name else 10,'radius':4,'width':2 if 'giant' in name else 1}
    if name=='galena_hexagon':return {'type':'jugcraft:landscape_detail','shape':'pillar','primary':modern(d.get('hexagon_made_of',state('galena'))),'radius':2,'height':8}
    if name=='magnetic_node':return {'type':'jugcraft:landscape_detail','shape':'crystal','primary':modern(d.get('node_made_of',d.get('block',state('azure_neodymium_node')))),'radius':1,'height':3}
    materials={
      'abyssal_boulder':('rock','abyssmarine','minecraft:air',3,4), 'acid_vent':('vent','radrock','acid',1,3), 'ambersol':('crystal','ambersol','minecraft:air',2,4),
      'amber_monolith':('pillar','amber','minecraft:air',2,8), 'black_vent':('vent','geothermal_vent','minecraft:magma_block',1,4),
      'candy_cane':('pillar','candy_cane_pole','candy_cane',1,5), 'ceiling_frostmint':('vine','frostmint','minecraft:air',1,3),
      'ceiling_ice_cream_cone':('vine','vanilla_ice_cream','wafer_cookie_block',1,6), 'ice_cream_scoop':('rock','vanilla_ice_cream','minecraft:air',3,4),
      'cookie_shelf':('shelf','cookie_block','minecraft:air',3,1), 'cycad':('patch','cycad','minecraft:air',3,1),
      'abyssal_flora':('patch','dusk_anemone','minecraft:air',4,1), 'encrusted_peppermint':('patch','small_peppermint','minecraft:air',4,1),
      'fill_in_bubbles_with_water':('lake','minecraft:water','abyssmarine',3,3), 'floating_orb':('rock','galena','minecraft:air',3,4),
      'floating_gummy_ring':('ring','gummy_ring_red','minecraft:air',2,1), 'gobstopper_geode':('rock','rock_candy_pink','minecraft:air',3,5),
      'guano_pile':('rock','guano_block','minecraft:air',2,2), 'lollipop':('flower','candy_cane_pole','lollipop_bunch',2,5),
      'mussel':('patch','mussel','minecraft:air',3,1), 'nuclear_siren':('pillar','metal_scaffolding','nuclear_siren',1,4),
      'peering_coprolith':('patch','peering_coprolith','minecraft:air',3,1), 'peppermint_patch':('patch','small_peppermint','minecraft:air',4,1),
      'ping_pong_sponge':('patch','ping_pong_sponge','minecraft:air',3,1), 'covered_block_blob':('rock','limestone','minecraft:moss_block',3,4),
      'spilled_ice_cream_cone':('rock','chocolate_ice_cream','wafer_cookie_block',2,3), 'sprinkles_pile':('patch','sprinkles','minecraft:air',3,1),
      'subterranodon_roost':('pillar','limestone','minecraft:air',2,9), 'sulfur_stack':('crystal','sulfur','sulfur_cluster',2,5),
      'sundrop_patch':('patch','sundrop','minecraft:air',4,1), 'sweet_puff':('patch','sweet_puff','minecraft:air',4,1),
      'tesla_bulb':('vine','tesla_bulb','minecraft:air',1,2), 'thornwood_roots':('vine','thornwood_branch','minecraft:air',1,4),
      'tube_worm':('patch','tube_worm','minecraft:air',3,1), 'volcano_boulder':('rock','flood_basalt','primal_magma',3,4),
      'whalefall':('log','baleen_bone','minecraft:air',1,9)
    }
    shape,a,b,r,h=materials[name]
    primary=d.get('state',d.get('block_state',state(a)))
    return {'type':'jugcraft:landscape_detail','shape':shape,'primary':modern(primary),'secondary':state(b),'radius':r,'height':h}


def biome(name,d):
    spawns={}
    for category,entries in d.get('spawners',{}).items():
        spawns[category]=[{'type':adapt(v['type']),'weight':v['weight'],'count':v['minCount'] if v['minCount']==v['maxCount'] else {'type':'minecraft:uniform','min_inclusive':v['minCount'],'max_inclusive':v['maxCount']}} for v in entries]
    fx=d['effects'];attrs={'minecraft:gameplay/natural_mob_spawns':{'argument':{'spawns_by_category':spawns,'spawn_costs':{}},'modifier':'overlay'},'minecraft:visual/sky_color':'#%06x'%fx.get('sky_color',0),'minecraft:visual/fog_color':'#%06x'%fx.get('fog_color',0)}
    # Current sound IDs use the native cave sound registry, not the old namespace.
    if 'music' in fx:
        music=copy.deepcopy(fx['music']);music['sound']='jugcraft:cavern.'+music['sound'].split(':')[-1];music.pop('replace_current_music',None);attrs['minecraft:audio/background_music']={'default':music}
    if 'ambient_sound' in fx:attrs['minecraft:audio/ambient_sounds']={'loop':'jugcraft:cavern.'+fx['ambient_sound'].split(':')[-1]}
    features=adapt(d.get('features',[[] for _ in range(11)]));features[0].insert(0,'jugcraft:cavern_chamber_'+name)
    return {'attributes':attrs,'has_precipitation':False,'temperature':d.get('temperature',.5),'downfall':d.get('downfall',0),'effects':{'water_color':'#%06x'%fx.get('water_color',0x3f76e4)},'carvers':[],'features':features}


def main():
    names=read(SOURCE/'lang/en_us.json');lang=read(ASSET/'lang/en_us.json');sounds=read(ASSET/'sounds.json')
    catalog={'blocks':{},'items':{},'fluids':FLUIDS,'sounds':[],'species':[],'biomes':[]}
    for folder in ('textures','sounds'):
        for p in (SOURCE/folder).rglob('*'):
            if p.is_file():copied(p,ASSET/folder/'menagerie/cavern'/p.relative_to(SOURCE/folder))
    for p in (SOURCE/'models').rglob('*.json'):
        try:d=adapt(read(p),True)
        except ValueError as e:raise ValueError(str(p)) from e
        d.pop('overrides',None);d.pop('render_type',None);write(ASSET/'models/menagerie/cavern'/p.relative_to(SOURCE/'models'),d)
    for p in (SOURCE/'blockstates').glob('*.json'):
        n=p.stem;k=classify(n);d=read(p)
        if n in FLOWERS:k='flower'
        if n.endswith('_egg') or n in {'spelunkery_table','quarry','confection_oven','conversion_crucible','hologram_projector','nuclear_furnace_component','amber_monolith','gobthumper','azure_magnet','scarlet_magnet','magnetic_activator','magnetic_levitation_rail','nuclear_bomb','nuclear_siren','siren_light','tesla_bulb','drain','copper_valve','dinosaur_chop','cooked_dinosaur_chop','giant_sweetberry','mussel','beholder','primal_magma','fissure_primal_magma','geothermal_vent','flood_basalt'}:k='cavern'
        if n in {'gingerbarrel','metal_barrel'}:k='shelf'
        if n in FLUIDS:k='fluid'
        if n in ('sugar_glass','depth_glass'):k='glass'
        if k=='building' and (any(d.get('variants',{})) or 'multipart' in d):k='decor'
        definition=decor_definition(SOURCE,'alexscaves',n,k,d)
        if n=='copper_valve':definition['properties']['powered']=['false','true']
        light=12 if any(x in n for x in ('lamp','light','ambersol','tesla','torch','sundrop','primal_magma')) else 0
        wood=k in ('log','leaves','sapling','fence','fence_gate','door','trapdoor','button','pressure_plate','sign','wall_sign','hanging_sign','wall_hanging_sign') or n.endswith('_planks')
        catalog['blocks']['cavern_'+n]={'source':n,'kind':k,'wood':wood,'light':light,'definition':definition}
        write(ASSET/f'blockstates/cavern_{n}.json',adapt(d,True));lang['block.jugcraft.cavern_'+n]=names.get('block.alexscaves.'+n,n.replace('_',' ').title())
        if not n.endswith(('wall_sign','wall_hanging_sign','wall_torch')) and n not in FLUIDS:catalog['items']['cavern_'+n]={'source':n,'food':n in FOOD,'armor':''}
    for key,title in names.items():
        if not key.startswith('item.alexscaves.'):continue
        n=key.removeprefix('item.alexscaves.')
        if '.' in n or n.endswith(('_inventory','_loading','_filled')) or n.startswith(('advancement_','natures_compass_warning','dreadbow_pulling')):continue
        catalog['items']['cavern_'+n]={'source':n,'food':n in FOOD or n.startswith('sweetish_fish_') and not n.endswith('_bucket'),'armor':ARMOR.get(n,'')};lang['item.jugcraft.cavern_'+n]=title
    for s in profiles():
        s['variants']=[];name=s['source']
        for p in (SOURCE/'textures/entity').rglob('*.png'):
            if not(p.stem==name or p.stem.startswith(name+'_')):continue
            suffix=p.stem[len(name):]
            if any(x in suffix for x in ('eye','glow','overlay','fluid','active','scared','winch','quarry','gel','darkness','charged','beam','heart','head','body','segment','armor','saddle','innards')):continue
            texture='menagerie/cavern/'+p.relative_to(SOURCE/'textures').with_suffix('').as_posix();s['variants'].append({'name':p.stem,'texture':texture,'babyTexture':texture,'weight':1})
        if not s['variants']:raise ValueError('Missing explicit creature skin mapping for '+name)
        sculpt(s);catalog['species'].append(s);lang['entity.jugcraft.'+s['id']]=names.get('entity.alexscaves.'+name,s['name'])
        catalog['items']['cavern_spawn_egg_'+name]={'source':'spawn_egg_'+name,'species':s['id']}
    for id,entry in catalog['items'].items():
        n=entry['source'];model={'type':'minecraft:model','model':'jugcraft:menagerie/cavern/item/'+n}
        if n.startswith('spawn_egg_'):model['model']='minecraft:item/zombie_spawn_egg'
        write(ASSET/f'items/{id}.json',{'model':model})
    for n in FLUIDS:
        for suffix in ('still','flow'):
            p=SOURCE/f'textures/block/{n}_{"flowing" if suffix=="flow" else suffix}.png';copied(p,ASSET/f'textures/block/cavern_{n}_{suffix}.png')
            if p.with_suffix('.png.mcmeta').exists():copied(p.with_suffix('.png.mcmeta'),ASSET/f'textures/block/cavern_{n}_{suffix}.png.mcmeta')
    for p in (SOURCE/'recipes').rglob('*.json'):
        d=read(p)
        if d['type']=='alexscaves:cave_map':continue # Native cave locator uses the serialized cave-region selection.
        write(DATA/'recipe'/p.relative_to(SOURCE/'recipes').parent/('cavern_'+p.name),recipe(adapt(d)))
    for p in (SOURCE/'loot_tables').rglob('*.json'):write(DATA/'loot_table'/p.relative_to(SOURCE/'loot_tables').parent/('cavern_'+p.name),modern_loot(adapt(read(p))))
    for p in (SOURCE/'tags').rglob('*.json'):
        rel=p.relative_to(SOURCE/'tags');parts=[{'items':'item','blocks':'block','entity_types':'entity_type','fluids':'fluid'}.get(x,x) for x in rel.parts[:-1]]
        write(DATA/'tags'/Path(*parts)/('cavern_'+p.name),adapt(read(p)))
    for p in (SOURCE/'worldgen/configured_feature').glob('*.json'):write(DATA/f'worldgen/feature/cavern_{p.name}',native_feature(read(p)))
    for p in (SOURCE/'worldgen/placed_feature').glob('*.json'):write(DATA/f'worldgen/placed_feature/cavern_{p.name}',modern(read(p)))
    for name,(stone,floor,water) in CAVES.items():
        id='jugcraft:cavern_'+name
        write(DATA/f'worldgen/feature/cavern_chamber_{name}.json',{'type':'jugcraft:cavern_chamber','biome':id,'stone':state(stone)['id'],'floor':state(floor)['id'],'waterline':water})
        write(DATA/f'worldgen/placed_feature/cavern_chamber_{name}.json',{'feature':'jugcraft:cavern_chamber_'+name,'placement':[]})
        d=read(SOURCE/f'worldgen/biome/{name}.json');write(DATA/f'worldgen/biome/cavern_{name}.json',biome(name,d));catalog['biomes'].append({'id':id,'source':name})
    for wood in ('ancient','pewen','thornwood'):
        write(DATA/f'worldgen/feature/cavern_sapling_{wood}.json',native_feature({'type':'alexscaves:'+wood+'_tree','config':{}}))
    for p in (SOURCE/'structures').rglob('*.nbt'):
        out=DATA/'structure'/p.relative_to(SOURCE/'structures').parent/('cavern_'+p.name);out.parent.mkdir(parents=True,exist_ok=True);out.write_bytes(owner_template_data.adapt(p.read_bytes(),adapt))
    for key,event in read(SOURCE/'sounds.json').items():
        event=adapt(event,True);event.pop('subtitle',None)
        for i,sound in enumerate(event.get('sounds',[])):
            if isinstance(sound,str) and ':' not in sound:event['sounds'][i]='jugcraft:menagerie/cavern/'+sound
            elif isinstance(sound,dict) and ':' not in sound['name']:sound['name']='jugcraft:menagerie/cavern/'+sound['name']
        sounds['cavern.'+key]=event;catalog['sounds'].append('cavern.'+key)
    write(ASSET/'sounds.json',sounds);write(ASSET/'lang/en_us.json',lang);write(RES/'jugcraft/menagerie/cavern.json',catalog)
    atlas=read(RES/'assets/minecraft/atlases/blocks.json');entry={'type':'minecraft:directory','source':'menagerie/cavern','prefix':'menagerie/cavern/'}
    if entry not in atlas['sources']:atlas['sources'].append(entry)
    write(RES/'assets/minecraft/atlases/blocks.json',atlas)
    write(ROOT/'art/owner-library/catalog/edited-2026-10-18-part2/cavern-runtime.json',{'copies':COPIES,'species':len(catalog['species']),'blocks':len(catalog['blocks']),'items':len(catalog['items']),'biomes':list(CAVES),'validation':'Implementation in progress; testing deferred until complete. Reconstructed rigs require visual review.'})
    print('Prepared cave resources:',len(catalog['species']),'species,',len(catalog['blocks']),'blocks,',len(catalog['items']),'items.')


if __name__=='__main__':main()
