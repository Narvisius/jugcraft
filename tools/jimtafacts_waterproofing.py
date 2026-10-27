"""Source waterproofing sprite and one-for-one item-pipe conversion recipes."""
import json, hashlib, shutil
from pathlib import Path
from jimtafacts_transport import KINDS
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
def dump(path,value):
 path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')
def generate(shared=True):
 src=ROOT/'art/owner-library/originals/Oct23/Jimtafacts/buildcraft reborn by jimbo/assets/buildcrafttransport/textures/items/pipewaterproof.png'
 dst=RES/'assets/jugcraft/textures/item/jimtafacts/pipe_waterproofing.png'
 dst.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(src,dst)
 ident='jimta_pipe_waterproofing'
 dump(RES/f'assets/jugcraft/models/item/{ident}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'jugcraft:item/jimtafacts/pipe_waterproofing'}})
 dump(RES/f'assets/jugcraft/items/{ident}.json',{'model':{'type':'minecraft:model','model':'jugcraft:item/'+ident}})
 def recipe(name,ingredients,result,count=1):
  dump(RES/f'data/jugcraft/recipe/jimtafacts_transport/{name}.json',{'type':'minecraft:crafting_shapeless','category':'redstone','ingredients':ingredients,'result':{'id':'jugcraft:'+result,'count':count}})
 recipe('pipe_waterproofing',['jugcraft:rubber','minecraft:green_dye'],ident,4)
 conversions=[]
 for kind in KINDS:
  if not kind.endswith('_fluid'):continue
  dry=kind[:-6]+'_item'
  if dry not in KINDS:continue
  recipe('waterproof_'+kind,['jugcraft:jimta_pipe_'+dry,'jugcraft:'+ident],'jimta_pipe_'+kind)
  # Removing the lining returns only the pipe; never refund consumed sealant.
  recipe('strip_'+kind,['jugcraft:jimta_pipe_'+kind],'jimta_pipe_'+dry)
  conversions.append({'from':'jimta_pipe_'+dry,'to':'jimta_pipe_'+kind})
 if shared:
  p=RES/'assets/jugcraft/lang/en_us.json';lang=json.loads(p.read_text());lang['item.jugcraft.'+ident]='Pipe Waterproofing';dump(p,lang)
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/waterproofing-runtime-copies.json',{'source':src.relative_to(ROOT).as_posix(),'runtime':dst.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(src.read_bytes()).hexdigest(),'changes':'Original pixels unchanged. Native rubber and green dye make four sealant; optional one-to-one source-style pipe conversions. Existing direct recipes remain. Stripping does not refund sealant.','conversions':conversions})
if __name__=='__main__':generate()
