from jimtafacts_crafting import distinct
"""Source stationary BuildCraft geometry with bounded native civil-work devices."""
from pathlib import Path
import json,shutil,hashlib
ROOT=Path(__file__).resolve().parents[1];RES=ROOT/'src/main/resources';SOURCE=ROOT/'art/owner-library/originals/Oct23/Jimtafacts/buildcraft reborn by jimbo/assets'
DEVICES={'builder':('buildcraftbuilders','builder/main'),'filler':('buildcraftbuilders','filler/main'),'replacer':('buildcraftbuilders','replacer'),'quarry':('buildcraftbuilders','quarry'),'mining_well':('buildcraftfactory','mining_well'),'flood_gate':('buildcraftfactory','flood_gate')}
def items():return blocks()|{'jimta_volume_marker'}
def blocks():return {'jimta_civil_'+id for id in DEVICES}
def dump(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
def generate(shared=True):
 manifest=[];lang={};copied={}
 def texture(ref):
  if ref.startswith('#'):return ref
  if ref in copied:return copied[ref]
  ns,path=ref.split(':',1);src=SOURCE/ns/f'textures/{path}.png';relative=f'block/jimtafacts/civil/{ns}/{path}';dest=RES/f'assets/jugcraft/textures/{relative}.png';dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(src,dest)
  manifest.append({'source':src.relative_to(ROOT).as_posix(),'runtime':dest.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(src.read_bytes()).hexdigest()});copied[ref]='jugcraft:'+relative
  sidecar=src.with_suffix('.png.mcmeta')
  if sidecar.exists():shutil.copyfile(sidecar,dest.with_suffix('.png.mcmeta'))
  return copied[ref]
 def model(ns,path):
  value=json.loads((SOURCE/ns/f'models/block/{path}.json').read_text(encoding='utf-8'));value['parent']='minecraft:block/cube' if 'elements' not in value else 'minecraft:block/block';value['textures']={key:texture(ref) for key,ref in value.get('textures',{}).items()};return value
 texture('buildcraftbuilders:blocks/frame/default')
 texture('buildcraftbuilders:blocks/quarry/drill')
 texture('buildcraftbuilders:blocks/frame/default')
 texture('buildcraftbuilders:blocks/quarry/drill')
 for key,(ns,path) in DEVICES.items():
  id='jimta_civil_'+key;value=model(ns,path);dump(RES/f'assets/jugcraft/models/block/{id}.json',value)
  variants={f'facing={facing},lit={str(lit).lower()}':{'model':'jugcraft:block/'+id,'y':rotation} for facing,rotation in [('north',0),('east',90),('south',180),('west',270)] for lit in (True,False)}
  dump(RES/f'assets/jugcraft/blockstates/{id}.json',{'variants':variants});dump(RES/f'assets/jugcraft/items/{id}.json',{'model':{'type':'minecraft:model','model':'jugcraft:block/'+id}})
  dump(RES/f'data/jugcraft/loot_table/blocks/{id}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:'+id}],'condition':{'type':'minecraft:survives_explosion'}}]})
  lang['block.jugcraft.'+id]=key.replace('_',' ').title()
  dump(RES/f'data/jugcraft/recipe/jimtafacts_civil/{key}.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':distinct('jimtafacts_civil/'+key,['jugcraft:steel_plate']*3+['jugcraft:iron_gear']*2+['jugcraft:advanced_circuit','jugcraft:electric_motor','jugcraft:machine_casing','minecraft:diamond' if key=='quarry' else 'minecraft:glass']),'result':{'id':'jugcraft:'+id,'count':1}})
 ref=texture('buildcraftcore:items/marker_volume');dump(RES/'assets/jugcraft/models/item/jimta_volume_marker.json',{'parent':'minecraft:item/generated','textures':{'layer0':ref}});dump(RES/'assets/jugcraft/items/jimta_volume_marker.json',{'model':{'type':'minecraft:model','model':'jugcraft:item/jimta_volume_marker'}});lang['item.jugcraft.jimta_volume_marker']='Volume Marker'
 dump(RES/'data/jugcraft/recipe/jimtafacts_civil/volume_marker.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':['minecraft:redstone_torch','minecraft:lapis_lazuli','jugcraft:basic_circuit'],'result':{'id':'jugcraft:jimta_volume_marker','count':1}})
 dump(RES/'data/jugcraft/tags/block/jimtafacts_civil.json',{'replace':False,'values':['jugcraft:'+id for id in sorted(blocks())]})
 if shared:
  p=RES/'assets/jugcraft/lang/en_us.json';v=json.loads(p.read_text(encoding='utf-8'));v.update(lang);dump(p,v)
  for tag in ('mineable/pickaxe','needs_iron_tool'):
   p=RES/f'data/minecraft/tags/block/{tag}.json';v=json.loads(p.read_text(encoding='utf-8'));ref='#jugcraft:jimtafacts_civil'
   if ref not in v['values']:v['values'].append(ref)
   dump(p,v)
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/civil-runtime-copies.json',manifest)
 return items()
if __name__=='__main__':
 import sys
 print(f'Generated {len(generate("--no-shared" not in sys.argv))} civil workshop items.')
