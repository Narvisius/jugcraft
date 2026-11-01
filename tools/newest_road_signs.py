"""Native road-sign structure data, original anchor NBT and destination tags."""
from newest_building import SOURCE,RES,read,write,map_value
from newest_wilds_monastery import source_json,modernize
import nouveau_nbt

def main():
 d=SOURCE/'SUPPS/data/supplementaries'; out=RES/'data/jugcraft'
 def mapped(v):return map_value(v,'supplementaries','supps')
 src=d/'structure/road_sign.nbt';k,n,r=nouveau_nbt.read(nouveau_nbt.adapt(src.read_bytes(),mapped));r=modernize((k,r))[1]
 dest=out/'structure/supps_road_sign.nbt';dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(nouveau_nbt.write((k,n,r)))
 write(out/'worldgen/template_pool/supps_road_sign/start_pool.json',mapped(source_json(d/'worldgen/template_pool/road_sign/start_pool.json')))
 write(out/'worldgen/template_pool/supps_road_sign/feature_pool.json',{'fallback':'minecraft:empty','elements':[{'weight':1,'element':{'feature':{'feature':{'type':'jugcraft:supps_road_sign_post'},'placement':[]},'projection':'rigid','element_type':'minecraft:feature_pool_element'}}]})
 write(out/'worldgen/structure/supps_road_sign.json',{'type':'jugcraft:supps_road_sign','biomes':'#jugcraft:supps_has_road_signs','terrain_adaptation':'none','step':'surface_structures','spawn_overrides':{}})
 write(out/'worldgen/structure_set/supps_road_signs.json',mapped(read(d/'worldgen/structure_set/road_signs.json')))
 write(out/'tags/worldgen/biome/supps_has_road_signs.json',{'values':['minecraft:plains','minecraft:sunflower_plains','minecraft:forest','minecraft:birch_forest','minecraft:taiga','minecraft:savanna','minecraft:desert','minecraft:meadow']})
 write(out/'tags/worldgen/structure/supps_road_sign_destinations.json',mapped(read(d/'tags/worldgen/structure/road_sign_destinations.json')))
if __name__=='__main__':main()
