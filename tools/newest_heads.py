"""Original mob-head textures and item transforms; charged-creeper loot is added natively."""
from newest_building import ROOT,SOURCE,RES,read,write
import shutil
A=SOURCE/'SUPPS/assets/supplementaries';O=RES/'assets/jugcraft'
def append_tag(namespace,kind,name,values):
 p=RES/'data'/namespace/'tags'/kind/(name+'.json');d=read(p) if p.exists() else {'replace':False,'values':[]}
 for v in values:
  if v not in d['values']:d['values'].append(v)
 write(p,d)
def main():
 data=read(RES/'jugcraft/newest_supps.json');lang=read(O/'lang/en_us.json')
 for family in ['enderman','spider']:
  for wall in [False,True]:
   name=family+('_wall' if wall else '')+'_head'
   props={'facing':['north','south','east','west']} if wall else {'rotation':[str(i) for i in range(16)]}
   shapes={'facing=north':[[4,4,8,12,12,16]],'facing=south':[[4,4,0,12,12,8]],'facing=east':[[0,4,4,8,12,12]],'facing=west':[[8,4,4,16,12,12]]} if wall else {'':[[4,0,4,12,8,12]]}
   data['blocks'][name]={'source':name,'collection':'newest_supps','kind':'supps','function':'mob_head','capacity':0,'wood':False,'properties':props,'shapes':shapes}
   write(O/'blockstates'/('supps_'+name+'.json'),{'variants':{'':{'model':'jugcraft:newest/empty'}}})
   write(RES/'data/jugcraft/loot_table/blocks'/('supps_'+name+'.json'),{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:supps_'+family+'_head'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
   lang['block.jugcraft.supps_'+name]=family.title()+' Head'
  name=family+'_head';write(O/'items'/('supps_'+name+'.json'),{'model':{'type':'minecraft:special','base':'jugcraft:newest/supps/item/'+name,'model':{'type':'jugcraft:supps_contents','name':name}}})
  for suffix in ['', '_eyes']:
   p=A/'textures/entity'/(name+suffix+'.png');target=O/'textures/newest/supps/entity'/p.name;target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,target)
 ids=['jugcraft:supps_'+f+'_head' for f in ['enderman','spider']]
 append_tag('minecraft','item','skulls',ids);append_tag('minecraft','item','noteblock_top_instruments',ids);append_tag('minecraft','item','gaze_disguise_equipment',[ids[0]])
 append_tag('c','block','skulls',ids+['jugcraft:supps_'+f+'_wall_head' for f in ['enderman','spider']])
 write(O/'lang/en_us.json',lang);write(RES/'jugcraft/newest_supps.json',data)
 catalog=ROOT/'art/owner-library/catalog/edited-2026-10-29/supplementaries-local.json'
 if catalog.exists():
  c=read(catalog);c['implementedRegistrationScope']=sorted(data['blocks']);c['remainingBlockFamilies']=[n for n in c['remainingBlockFamilies'] if n not in data['blocks']];write(catalog,c)
if __name__=='__main__':main()
