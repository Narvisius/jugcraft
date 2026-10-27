"""Source library/planner artwork on shared native blueprints and saved civil zones."""
import json,hashlib,shutil
from jimtafacts_factory import ROOT,RES,SOURCE,dump
DEVICES={'jimta_blueprint_library':('buildcraftbuilders','library'),'jimta_zone_planner':('buildcraftrobotics','zone_planner')}
def blocks():return set(DEVICES)
def items():return blocks()
def generate(shared=True):
 manifest=[]
 for ident,(namespace,name) in DEVICES.items():
  source=SOURCE/f'{namespace}/models/block/{name}.json';data=json.loads(source.read_text(encoding='utf-8'));data['parent']='minecraft:block/cube'
  for key,value in list(data['textures'].items()):
   ns,path=value.split(':');src=SOURCE/f'{ns}/textures/{path}.png';dst=RES/f'assets/jugcraft/textures/block/jimtafacts/planning/{name}/{src.name}';dst.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(src,dst)
   data['textures'][key]='jugcraft:block/jimtafacts/planning/'+name+'/'+src.stem
   manifest.append({'source':src.relative_to(ROOT).as_posix(),'runtime':dst.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(src.read_bytes()).hexdigest()})
  ref='jugcraft:block/jimtafacts/planning/'+name
  dump(RES/f'assets/jugcraft/models/block/jimtafacts/planning/{name}.json',data)
  dump(RES/f'assets/jugcraft/blockstates/{ident}.json',{'variants':{'':{'model':ref}}})
  dump(RES/f'assets/jugcraft/items/{ident}.json',{'model':{'type':'minecraft:model','model':ref}})
  dump(RES/f'data/jugcraft/loot_table/blocks/{ident}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:'+ident}],'condition':{'type':'minecraft:survives_explosion'}}]})
  dump(RES/f'data/jugcraft/recipe/jimtafacts_factory/{ident}.json',{'type':'minecraft:crafting_shapeless','category':'redstone','ingredients':['jugcraft:machine_casing','jugcraft:basic_circuit','minecraft:book','minecraft:compass' if name=='zone_planner' else 'minecraft:paper'],'result':{'id':'jugcraft:'+ident}})
 if shared:
  p=RES/'assets/jugcraft/lang/en_us.json';v=json.loads(p.read_text(encoding='utf-8'));v.update({'block.jugcraft.'+name:name.removeprefix('jimta_').replace('_',' ').title() for name in DEVICES});dump(p,v)
  p=RES/'data/minecraft/tags/block/mineable/pickaxe.json';v=json.loads(p.read_text(encoding='utf-8'));v['values']=sorted(set(v['values'])|{'jugcraft:'+name for name in DEVICES});dump(p,v)
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/planning-runtime-copies.json',manifest)
 return len(DEVICES)
if __name__=='__main__':print('Generated',generate(),'planning desks')
