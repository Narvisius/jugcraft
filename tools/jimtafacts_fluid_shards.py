"""Original fragile-container layers, conserved native single-fill storage."""
from pathlib import Path
import hashlib,json,shutil
ROOT=Path(__file__).resolve().parents[1];RES=ROOT/'src/main/resources'
SOURCE=ROOT/'art/owner-library/originals/Oct23/Jimtafacts/buildcraft reborn by jimbo/assets/buildcraftcore/textures/items'
def dump(path,value):
 path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')
def generate(shared=True):
 copies=[];ident='jimta_fragile_fluid_shard'
 for suffix in ('base','fluid'):
  source=SOURCE/f'fragile_fluid_shard_{suffix}.png';target=RES/f'assets/jugcraft/textures/item/jimtafacts/production/fragile_shard_{suffix}.png'
  target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source,target)
  copies.append({'source':source.relative_to(ROOT).as_posix(),'runtime':target.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'changes':'None; original layer pixels retained.'})
 for filled in (False,True):
  name=ident+('_filled' if filled else '')
  textures={'layer0':'jugcraft:item/jimtafacts/production/fragile_shard_base'}
  if filled:textures['layer1']='jugcraft:item/jimtafacts/production/fragile_shard_fluid'
  dump(RES/f'assets/jugcraft/models/item/{name}.json',{'parent':'minecraft:item/generated','textures':textures})
 dump(RES/f'assets/jugcraft/items/{ident}.json',{'model':{'type':'minecraft:range_dispatch','property':'minecraft:custom_model_data','index':0,'fallback':{'type':'minecraft:model','model':'jugcraft:item/'+ident},'entries':[{'threshold':1,'model':{'type':'minecraft:model','model':'jugcraft:item/'+ident+'_filled'}}]}})
 dump(RES/'data/jugcraft/recipe/jimtafacts_production/fragile_fluid_shard.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':['minecraft:glass_pane','minecraft:paper'],'result':{'id':'jugcraft:'+ident,'count':2}})
 if shared:
  path=RES/'assets/jugcraft/lang/en_us.json';v=json.loads(path.read_text());v['item.jugcraft.'+ident]='Fragile Fluid Shard';dump(path,v)
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/fluid-shard-runtime-copies.json',{'copies':copies,'adaptation':'Original base/fluid layers and 500 mB capacity. Native craftable empty shell accepts one fill, then is extraction-only and disappears on final drain. No automatic replacement of tank drops: native tanks already preserve their complete fluid. Fluid appearance is a neutral original overlay; tooltip identifies contents. Transactional tanks and fluid bottler conserve partial transfers and aborted operations.'})
if __name__=='__main__':generate()
