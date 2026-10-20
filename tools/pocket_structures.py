"""Adapt all six supplied pocket-pet structures to native jigsaw/spawner/chest formats."""
from menagerie_content import *
from pocket_content import PETS
import nouveau_nbt

def main():
    adaptations=[]
    for p in (PETS/'structure').glob('*.nbt'):
        def replace(s):
            if s=='inventorypets:oak_leaves':return 'minecraft:oak_leaves'
            if s.startswith('inventorypets:') and s.endswith('_spawn'):return 'minecraft:spawner'
            return remap(s,'pocket','inventorypets')
        _,_,root=nouveau_nbt.read(nouveau_nbt.adapt(p.read_bytes(),replace))
        palette=root['palette'][1][1]
        spawners={i for i,v in enumerate(palette) if v.get('id')==(8,'minecraft:spawner')}
        chests={i for i,v in enumerate(palette) if v.get('id')==(8,'minecraft:chest')}
        mob='minecraft:blaze' if p.stem=='nether_dungeon' else 'minecraft:drowned' if p.stem=='sea_cave' else 'minecraft:skeleton'
        for block in root['blocks'][1][1]:
            if block['state'][1] in spawners:block['nbt']=(10,{'id':(8,'minecraft:mob_spawner'),'Delay':(2,80),'MinSpawnDelay':(2,400),'MaxSpawnDelay':(2,800),'SpawnCount':(2,1),'MaxNearbyEntities':(2,3),'RequiredPlayerRange':(2,12),'SpawnRange':(2,3),'SpawnData':(10,{'entity':(10,{'id':(8,mob)})})})
            if block['state'][1] in chests:block['nbt']=(10,{'id':(8,'minecraft:chest'),'LootTable':(8,'jugcraft:chests/pocket_discovery')})
        out=DATA/f'structure/pocket/{p.name}';out.parent.mkdir(parents=True,exist_ok=True);out.write_bytes(nouveau_nbt.write((10,'',root)))
        id='pocket_'+p.stem
        write(DATA/f'worldgen/template_pool/{id}.json',{'name':'jugcraft:'+id,'fallback':'minecraft:empty','elements':[{'weight':1,'element':{'element_type':'minecraft:single_pool_element','location':'jugcraft:pocket/'+p.stem,'projection':'rigid','processors':'minecraft:empty'}}]})
        biome='#minecraft:is_nether' if p.stem=='nether_dungeon' else '#minecraft:is_ocean' if p.stem=='sea_cave' else '#minecraft:is_forest' if p.stem=='tree_top' else '#minecraft:is_overworld'
        height=180 if p.stem=='sky_dungeon' else 250 if p.stem=='space_dungeon' else 24 if p.stem=='underground_dungeon' else 64 if p.stem=='nether_dungeon' else 0
        definition={'type':'minecraft:jigsaw','biomes':biome,'step':'surface_structures','spawn_overrides':{},'terrain_adaptation':'none','start_pool':'jugcraft:'+id,'size':1,'start_height':{'absolute':height},'max_distance_from_center':80,'use_expansion_hack':False}
        if p.stem in {'tree_top','sea_cave'}:definition['project_start_to_heightmap']='OCEAN_FLOOR_WG' if p.stem=='sea_cave' else 'WORLD_SURFACE_WG'
        write(DATA/f'worldgen/structure/{id}.json',definition)
        write(DATA/f'worldgen/structure_set/{id}.json',{'structures':[{'structure':'jugcraft:'+id,'weight':1}],'placement':{'type':'minecraft:random_spread','spacing':64,'separation':24,'salt':181100+len(adaptations)}})
        adaptations.append({'source':p.relative_to(SOURCE).as_posix(),'runtime':out.relative_to(RES).as_posix(),'changes':['26.3 block-state palette','Jugcraft decorative block IDs','bounded vanilla mob spawners','native pet loot chest','sparse vanilla jigsaw generation']})
    write(DATA/'loot_table/chests/pocket_discovery.json',{'type':'minecraft:chest','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:pocket_pet_'+name} for name in ['chicken','cow','sheep','furnace','crafting_table','anvil','squid','ocelot','pig','snow_golem']]},{'rolls':3,'entries':[{'type':'minecraft:item','name':i} for i in ['minecraft:wheat','minecraft:gold_nugget','minecraft:coal']]}]})
    for name in ['cloud_block','sand_block','stone_block']:
        write(ASSET/f'blockstates/pocket_{name}.json',remap(read(PETS/f'blockstates/{name}.json'),'pocket','inventorypets',True))
        write(ASSET/f'items/pocket_{name}.json',{'model':{'type':'minecraft:model','model':'jugcraft:menagerie/pocket/block/'+name}})
        write(DATA/f'loot_table/blocks/pocket_{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:pocket_'+name}]}]})
    for p in (MOUNTS/'models').rglob('*.json'):write(ASSET/'models/menagerie/mount'/p.relative_to(MOUNTS/'models'),remap(read(p),'mount','mythicmounts',True))
    for p in (MOUNTS/'blockstates').glob('*.json'):
        write(ASSET/f'blockstates/mount_{p.name}',remap(read(p),'mount','mythicmounts',True))
        write(ASSET/f'items/mount_{p.name}',{'model':{'type':'minecraft:model','model':'jugcraft:menagerie/mount/item/'+p.stem}})
        write(DATA/f'loot_table/blocks/mount_{p.name}',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:mount_'+p.stem}]}]})
    write(ROOT/'art/owner-library/catalog/edited-2026-10-18/pocket-structure-adaptation.json',adaptations)
    print('Integrated six supplied pocket structures, with native spawners and pet rewards.')

if __name__=='__main__':main()
