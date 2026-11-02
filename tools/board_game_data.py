"""Native carved board tables; use existing vanilla wood/stone materials, no raster edits."""
import json
from pathlib import Path
from decor_data import box
ROOT=Path(__file__).resolve().parents[1];RES=ROOT/'src/main/resources'
def generate(writer=None):
 def emit(rel,d):
  p=RES/rel
  if writer:writer(p,d)
  else:p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,indent=2)+'\n',encoding='utf-8')
 for name in ['checkers_table','chess_table']:
  tex={'wood':'minecraft:block/dark_oak_planks','rim':'minecraft:block/spruce_planks','light':'minecraft:block/birch_planks','dark':'minecraft:block/polished_deepslate','brass':'minecraft:block/yellow_terracotta','particle':'minecraft:block/dark_oak_planks'}
  elems=[box((0,11,0),(16,12.5,16),'#wood'),box((.4,12.5,.4),(15.6,13,15.6),'#rim')]
  for x in (1,12):
   for z in (1,12):elems.extend([box((x,0,z),(x+3,11,z+3),'#wood'),box((x-.2,0,z-.2),(x+3.2,1,z+3.2),'#rim'),box((x,8,z),(x+3,9,z+3),'#brass')])
  for x in range(8):
   for z in range(8):elems.append(box((2+x*1.5,13,2+z*1.5),(3.5+x*1.5,13.15,3.5+z*1.5),'#light' if (x+z)%2==(1 if name=='chess_table' else 0) else '#dark'))
  for x,z in [(1,1),(14,1),(1,14),(14,14)]:elems.append(box((x,13,z),(x+1,13.3,z+1),'#brass'))
  if name=='chess_table':
   elems += [box((7,8,-.1),(9,10,.1),'#brass'),box((6.4,9,-.1),(9.6,9.5,.1),'#brass')]
  else:elems += [box((6.5,8,-.1),(9.5,10,.1),'#brass')]
  emit('assets/jugcraft/models/block/'+name+'.json',{'textures':tex,'elements':elems,'display':{'gui':{'rotation':[30,225,0],'scale':[.75,.75,.75]},'ground':{'translation':[0,3,0],'scale':[.4,.4,.4]},'fixed':{'rotation':[0,180,0],'scale':[.5,.5,.5]}}})
  emit('assets/jugcraft/blockstates/'+name+'.json',{'variants':{'':{'model':'jugcraft:block/'+name}}})
  emit('assets/jugcraft/items/'+name+'.json',{'model':{'type':'minecraft:model','model':'jugcraft:block/'+name}})
  emit('data/jugcraft/loot_table/blocks/'+name+'.json',{'type':'minecraft:block','pools':[{'rolls':1,'condition':{'type':'minecraft:survives_explosion'},'entries':[{'type':'minecraft:item','name':'jugcraft:'+name}]}]})
  emit('data/jugcraft/recipe/'+name+'.json',{'type':'minecraft:crafting_shaped','pattern':['WBW','PPP','S S'],'key':{'W':'minecraft:quartz' if name=='chess_table' else 'minecraft:white_dye','B':'minecraft:ink_sac','P':'#minecraft:planks','S':'minecraft:stick'},'result':{'id':'jugcraft:'+name}})
 lang=json.loads((RES/'assets/jugcraft/lang/en_us.json').read_text(encoding='utf-8'))
 for n in ['checkers_table','chess_table']:lang['block.jugcraft.'+n]=n.replace('_',' ').title()
 emit('assets/jugcraft/lang/en_us.json',lang)
if __name__=='__main__':generate()
