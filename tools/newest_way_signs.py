"""Original direction-board artwork and paid wood-specific crafting."""
from newest_building import ROOT,SOURCE,RES,read,write,map_value
A=SOURCE/'SUPPS/assets/supplementaries';O=RES/'assets/jugcraft'
WOODS=['oak','spruce','birch','jungle','acacia','dark_oak','mangrove','cherry','bamboo','crimson','warped']
def main():
 data=read(RES/'jugcraft/newest_supps.json');lang=read(O/'lang/en_us.json')
 data['blocks']['way_sign']={'source':'way_sign','collection':'newest_supps','kind':'supps','function':'way_sign','capacity':2,'wood':True,'properties':{'waterlogged':['false','true']},'shapes':{'':[[0,2,0,16,15,16]]}}
 data['blocks']['way_sign_wall']={**data['blocks']['way_sign'],'source':'way_sign_wall','properties':{'waterlogged':['false','true'],'facing':['north','south','east','west']}}
 write(O/'blockstates/supps_way_sign_wall.json',{'variants':{'':{'model':'jugcraft:newest/empty'}}})
 write(O/'blockstates/supps_way_sign.json',{'variants':{'':{'model':'jugcraft:newest/empty'}}})
 for wood in WOODS:
  name='way_sign_'+wood
  write(O/'items'/('supps_'+name+'.json'),{'model':{'type':'minecraft:model','model':'jugcraft:newest/supps/item/'+name}})
  write(O/'items'/('supps_'+name+'_placed.json'),{'model':{'type':'minecraft:model','model':'jugcraft:newest/supps/special_models/block/way_signs/'+name}})
  write(RES/'data/jugcraft/recipe'/('supps_'+name+'.json'),{'type':'minecraft:crafting_shapeless','ingredients':['minecraft:'+wood+'_sign'],'result':{'id':'jugcraft:supps_'+name,'count':2}})
  lang['item.jugcraft.supps_'+name]=wood.replace('_',' ').title()+' Direction Sign'
 lang['block.jugcraft.supps_way_sign']='Direction Signpost';lang['block.jugcraft.supps_way_sign_wall']='Wall Direction Signs'
 write(O/'lang/en_us.json',lang);write(RES/'jugcraft/newest_supps.json',data)
 catalog=ROOT/'art/owner-library/catalog/edited-2026-10-29/supplementaries-local.json'
 if catalog.exists():
  value=read(catalog);value['implementedRegistrationScope']=sorted(data['blocks']);value['remainingBlockFamilies']=[n for n in value['remainingBlockFamilies'] if n not in data['blocks']];write(catalog,value)
 from newest_road_signs import main as road_signs
 road_signs()
if __name__=='__main__':main()
