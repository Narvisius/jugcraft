"""Runtime copies for supplied ceiling/wall and animated rope buntings."""
from copy import deepcopy
from newest_building import ROOT,SOURCE,RES,read,write,map_value
from newest_shapes import variants
from newest_twigs import recipe
S=SOURCE/'SUPPS';A=S/'assets/supplementaries';O=RES/'assets/jugcraft'
COLORS=['white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black']
def mapped(value,asset=False):return map_value(value,'supplementaries','supps',asset)
def main():
 data=read(RES/'jugcraft/newest_supps.json');lang=read(O/'lang/en_us.json');source_lang=read(A/'lang/en_us.json')
 for color in COLORS:
  for wall in [False,True]:
   name='bunting_'+('wall_' if wall else '')+color
   state=read(A/'blockstates'/(name+'.json'))
   props={'facing':['north','south','east','west']} if wall else {'axis':['x','z']}
   data['blocks'][name]={'source':name,'collection':'newest_supps','kind':'bunting','function':'decor','capacity':0,'wood':False,'properties':props,'shapes':variants(A,'supplementaries',state,props)}
   write(O/'blockstates'/('supps_'+name+'.json'),mapped(state,True))
   if not wall:write(O/'items'/('supps_'+name+'.json'),{'model':{'type':'minecraft:model','model':'jugcraft:newest/supps/item/'+name}})
   write(RES/'data/jugcraft/loot_table/blocks'/('supps_'+name+'.json'),{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:supps_bunting_'+color}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
   lang['block.jugcraft.supps_'+name]=source_lang.get('block.supplementaries.bunting_'+color,color.replace('_',' ').title()+' Bunting')
 rope=deepcopy(data['blocks']['rope']);rope.update({'source':'rope_buntings','kind':'supps','function':'rope_bunting','capacity':4})
 data['blocks']['rope_buntings']=rope
 write(O/'blockstates/supps_rope_buntings.json',read(O/'blockstates/supps_rope.json'))
 write(RES/'data/jugcraft/loot_table/blocks/supps_rope_buntings.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:supps_rope'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
 for path in (S/'data/supplementaries/recipe/buntings').glob('*.json'):
  value=mapped(read(path));value.pop('fabric:load_conditions',None);value.pop('neoforge:conditions',None)
  write(RES/'data/jugcraft/recipe/buntings'/('supps_'+path.name),recipe(value))
 for path in (S/'data/supplementaries/tags/item').glob('*bunting*.json'):
  write(RES/'data/jugcraft/tags/item'/('supps_'+path.name),mapped(read(path)))
 lang['block.jugcraft.supps_rope_buntings']='Rope with Buntings'
 write(O/'lang/en_us.json',lang);write(RES/'jugcraft/newest_supps.json',data)
 catalog=ROOT/'art/owner-library/catalog/edited-2026-10-29/supplementaries-local.json'
 if catalog.exists():
  value=read(catalog);names=set(data['blocks']);value['implementedRegistrationScope']=sorted(names)
  value['remainingBlockFamilies']=[n for n in value['remainingBlockFamilies'] if n not in names];write(catalog,value)
if __name__=='__main__':main()
