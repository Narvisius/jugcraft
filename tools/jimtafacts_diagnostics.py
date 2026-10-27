"""Original power-test block and debugger icon on native JE diagnostics."""
import json,hashlib,shutil
from jimtafacts_factory import ROOT,RES,SOURCE,dump

def generate(shared=True):
 manifest=[]
 for ident,source,block in [('jimta_power_tester','buildcraftcore/textures/blocks/power_tester.png',True),('jimta_factory_debugger','buildcraftlib/textures/items/debugger.png',False)]:
  src=SOURCE/source;target=RES/f'assets/jugcraft/textures/item/jimtafacts/{ident}.png';target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(src,target)
  model='jugcraft:'+('block/' if block else 'item/')+ident
  dump(RES/f'assets/jugcraft/models/{"block" if block else "item"}/{ident}.json',{'parent':'minecraft:block/cube_all' if block else 'minecraft:item/generated','textures':{'all' if block else 'layer0':'jugcraft:item/jimtafacts/'+ident}})
  dump(RES/f'assets/jugcraft/items/{ident}.json',{'model':{'type':'minecraft:model','model':model}})
  if block:
   dump(RES/f'assets/jugcraft/blockstates/{ident}.json',{'variants':{'':{'model':model}}})
   dump(RES/f'data/jugcraft/loot_table/blocks/{ident}.json',{'type':'minecraft:block','pools':[{'rolls':1,'condition':{'type':'minecraft:survives_explosion'},'entries':[{'type':'minecraft:item','name':'jugcraft:'+ident}]}]})
  manifest.append({'source':src.relative_to(ROOT).as_posix(),'runtime':target.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(src.read_bytes()).hexdigest()})
 for ident,parts in [('jimta_power_tester',['jugcraft:iron_plate','jugcraft:basic_circuit','minecraft:redstone_lamp']),('jimta_factory_debugger',['jugcraft:copper_wire','jugcraft:basic_circuit','minecraft:glass'])]:
  dump(RES/f'data/jugcraft/recipe/jimtafacts_factory/{ident}.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':parts,'result':{'id':'jugcraft:'+ident}})
 if shared:
  p=RES/'assets/jugcraft/lang/en_us.json';data=json.loads(p.read_text());data.update({'block.jugcraft.jimta_power_tester':'JE Load Tester','item.jugcraft.jimta_factory_debugger':'Workshop Diagnostic Probe'});dump(p,data)
  p=RES/'data/minecraft/tags/block/mineable/pickaxe.json';data=json.loads(p.read_text());data['values']=list(dict.fromkeys(data['values']+['jugcraft:jimta_power_tester']));dump(p,data)
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/diagnostic-runtime-copies.json',manifest)
 return {'jimta_power_tester','jimta_factory_debugger'}
if __name__=='__main__':generate()
