"""Original cannon art, paid recipes, ammunition piles and native control resources."""
from newest_building import ROOT,SOURCE,RES,read,write
import shutil
from menagerie_data import loot
A=SOURCE/'SUPPS/assets/supplementaries';O=RES/'assets/jugcraft'
def main():
 data=read(RES/'jugcraft/newest_supps.json');lang=read(O/'lang/en_us.json')
 data['blocks']['cannon']={'source':'cannon','collection':'newest_supps','kind':'supps','function':'cannon','capacity':2,'wood':False,'properties':{'waterlogged':['false','true'],'facing':['up','down','north','south','east','west']},'shapes':{'':[[0,0,0,16,2,16],[0,2,5,2,12,11],[14,2,5,16,12,11],[2,2,2,14,14,14]]}}
 data['blocks']['cannonball']={'source':'cannonball','collection':'newest_supps','kind':'cannonball','function':'cannonball','capacity':0,'wood':False,'properties':{}}
 write(O/'blockstates/supps_cannon.json',{'variants':{'':{'model':'jugcraft:newest/empty'}}})
 write(O/'items/supps_cannon.json',{'model':{'type':'minecraft:special','base':'jugcraft:newest/supps/item/cannon','model':{'type':'jugcraft:supps_contents','name':'cannon'}}})
 write(O/'items/supps_cannonball.json',{'model':{'type':'minecraft:model','model':'jugcraft:newest/supps/block/cannonball_1'}})
 write(O/'blockstates/supps_cannonball.json',{'variants':{'balls='+str(i):{'model':'jugcraft:newest/supps/block/cannonball_'+str(i)} for i in range(1,5)}})
 write(RES/'data/jugcraft/loot_table/blocks/supps_cannon.json',loot({'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:supps_cannon'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]}))
 write(RES/'data/jugcraft/loot_table/blocks/supps_cannonball.json',loot({'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:supps_cannonball','functions':[{'function':'minecraft:set_count','count':i,'conditions':[{'condition':'minecraft:block_state_property','block':'jugcraft:supps_cannonball','properties':{'balls':str(i)}}]} for i in range(1,5)]}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]}))
 write(RES/'data/jugcraft/recipe/supps_cannon.json',{'type':'minecraft:crafting_shaped','pattern':['III','IDI','PPP'],'key':{'I':'minecraft:iron_ingot','D':'minecraft:dispenser','P':'#minecraft:planks'},'result':{'id':'jugcraft:supps_cannon'}})
 write(RES/'data/jugcraft/recipe/supps_cannonball.json',{'type':'minecraft:crafting_shaped','pattern':[' I ','III',' I '],'key':{'I':'minecraft:iron_block'},'result':{'id':'jugcraft:supps_cannonball','count':8}})
 write(RES/'data/jugcraft/tags/item/supps_cannon_blacklist.json',{'replace':False,'values':[]})
 for n in ['cannon','cannon_gunpowder','cannon_lit']:
  dest=O/'textures/newest/supps/block'/(n+'.png');dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(A/'textures/block'/(n+'.png'),dest)
 sounds=read(O/'sounds.json');sounds['supps_cannon_fire']={'sounds':['jugcraft:newest/supps/block/cannon_fire_'+str(i) for i in range(4)]};write(O/'sounds.json',sounds)
 for i in range(4):
  dest=O/'sounds/newest/supps/block'/('cannon_fire_'+str(i)+'.ogg');dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(A/'sounds/block'/dest.name,dest)
 lang['block.jugcraft.supps_cannon']='Cannon';lang['block.jugcraft.supps_cannonball']='Cannonball';write(O/'lang/en_us.json',lang);write(RES/'jugcraft/newest_supps.json',data)
 catalog=ROOT/'art/owner-library/catalog/edited-2026-10-29/supplementaries-local.json'
 if catalog.exists():
  c=read(catalog);c['implementedRegistrationScope']=sorted(data['blocks']);c['remainingBlockFamilies']=[n for n in c['remainingBlockFamilies'] if n not in data['blocks']];write(catalog,c)
if __name__=='__main__':main()
