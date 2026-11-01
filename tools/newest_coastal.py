"""Original coastal life and potted flax resources; native mechanics are in Java."""
from newest_building import ROOT,SOURCE,RES,read,write,map_value
from newest_shapes import variants
S=SOURCE/'SUPPS';A=S/'assets/supplementaries';O=RES/'assets/jugcraft'
def main():
 data=read(RES/'jugcraft/newest_supps.json');lang=read(O/'lang/en_us.json')
 # The old random-rotation loader is not a native model loader. Retain its
 # original inline mesh; blockstate facing handles each attached surface.
 barnacles=read(A/'models/block/barnacles.json')
 write(O/'models/newest/supps/block/barnacles.json',map_value(barnacles.get('model',barnacles),'supplementaries','supps',True))
 for name in ['barnacles','potted_flax']:
  data['blocks'][name]={'source':name,'collection':'newest_supps','kind':name,'wood':False}
  write(O/'blockstates'/('supps_'+name+'.json'),map_value(read(A/'blockstates'/(name+'.json')),'supplementaries','supps',True))
  write(RES/'data/jugcraft/loot_table/blocks'/('supps_'+name+'.json'),map_value(read(S/'data/supplementaries/loot_table/blocks'/(name+'.json')),'supplementaries','supps'))
  lang['block.jugcraft.supps_'+name]='Barnacles' if name=='barnacles' else 'Potted Flax'
 write(O/'items/supps_barnacles.json',{'model':{'type':'minecraft:model','model':'jugcraft:newest/supps/item/barnacles'}})
 write(RES/'data/jugcraft/tags/block/supps_barnacles_blacklist.json',map_value(read(S/'data/supplementaries/tags/block/barnacles_blacklist.json'),'supplementaries','supps'))
 write(RES/'data/jugcraft/worldgen/feature/supps_barnacles_patch.json',{'type':'jugcraft:supps_barnacles_patch'})
 for name in ['ocean_barnacles','shore_barnacles']:
  write(RES/'data/jugcraft/worldgen/placed_feature'/('supps_'+name+'.json'),map_value(read(S/'data/supplementaries/worldgen/placed_feature'/(name+'.json')),'supplementaries','supps'))
 name='jar_boat';state=read(A/'blockstates/jar_boat.json');props={'facing':['north','south','east','west']}
 data['blocks'][name]={'source':name,'collection':'newest_supps','kind':'supps','function':'jar_boat','capacity':0,'wood':False,'properties':props,'shapes':variants(A,'supplementaries',state,props)}
 write(O/'blockstates/supps_jar_boat.json',map_value(state,'supplementaries','supps',True))
 write(O/'items/supps_jar_boat.json',{'model':{'type':'minecraft:special','base':'jugcraft:newest/supps/item/jar_boat','model':{'type':'jugcraft:supps_contents','name':'jar_boat'}}})
 write(O/'items/supps_jar_boat_ship.json',{'model':{'type':'minecraft:model','model':'jugcraft:newest/supps/special_models/block/jar_boat_ship'}})
 write(RES/'data/jugcraft/loot_table/blocks/supps_jar_boat.json',map_value(read(S/'data/supplementaries/loot_table/blocks/jar_boat.json'),'supplementaries','supps'))
 write(RES/'data/jugcraft/recipe/supps_boat_jar.json',{'type':'minecraft:crafting_shapeless','ingredients':['jugcraft:supps_jar','#minecraft:boats'],'result':{'id':'jugcraft:supps_jar_boat','count':1}})
 lang['block.jugcraft.supps_jar_boat']='Boat in a Jar'
 write(O/'lang/en_us.json',lang);write(RES/'jugcraft/newest_supps.json',data)
 catalog=ROOT/'art/owner-library/catalog/edited-2026-10-29/supplementaries-local.json'
 if catalog.exists():
  value=read(catalog);value['implementedRegistrationScope']=sorted(data['blocks']);value['remainingBlockFamilies']=[n for n in value['remainingBlockFamilies'] if n not in data['blocks']];write(catalog,value)
if __name__=='__main__':main()
