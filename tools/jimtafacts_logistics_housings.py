"""Original industrial sorter/buffer faces on shared native item logistics."""
from pathlib import Path
import hashlib,json,shutil
from PIL import Image
ROOT=Path(__file__).resolve().parents[1];RES=ROOT/'src/main/resources'
SOURCE=ROOT/'art/owner-library/originals/Oct23/Jimtafacts/industrialcraft reborn by jimbo/assets/ic2/textures/blocks/machine'
DEVICES={'jimta_sorting_machine':'blockSortingMachine','jimta_item_buffer':'blockItemBuffer'}
def blocks():return set(DEVICES)
def dump(path,value):
 path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')
def generate(shared=True):
 copies=[];lang={}
 for ident,name in DEVICES.items():
  source=SOURCE/(name+'.png');target=RES/f'assets/jugcraft/textures/block/jimtafacts/logistics/{ident}.png';target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source,target)
  copies.append({'source':source.relative_to(ROOT).as_posix(),'runtime':target.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'changes':'Original atlas pixels; inactive six-face tiles selected by model UVs. No invented lit state.'})
  with Image.open(source) as image:w,h=image.size
  tiles=w//h
  faces={face:{'texture':'#all','uv':[16*index/tiles,0,16*(index+1)/tiles,16],'cullface':face} for face,index in zip(('down','up','north','south','west','east'),(0,1,3,5,4,2))}
  dump(RES/f'assets/jugcraft/models/block/{ident}.json',{'parent':'minecraft:block/block','textures':{'all':'jugcraft:block/jimtafacts/logistics/'+ident,'particle':'#all'},'elements':[{'from':[0,0,0],'to':[16,16,16],'faces':faces}]})
  variants={}
  for side,rot in {'north':{},'south':{'y':180},'east':{'y':90},'west':{'y':270},'up':{'x':270},'down':{'x':90}}.items():variants['facing='+side]={'model':'jugcraft:block/'+ident,**rot}
  dump(RES/f'assets/jugcraft/blockstates/{ident}.json',{'variants':variants})
  dump(RES/f'assets/jugcraft/items/{ident}.json',{'model':{'type':'minecraft:model','model':'jugcraft:block/'+ident}})
  dump(RES/f'data/jugcraft/loot_table/blocks/{ident}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:'+ident}],'condition':{'type':'minecraft:survives_explosion'}}]})
  lang['block.jugcraft.'+ident]='Industrial '+ident.removeprefix('jimta_').replace('_',' ').title()
  original='jimta_item_filter' if ident=='jimta_sorting_machine' else 'jimta_chute'
  for reverse in (False,True):
   before,after=(ident,original) if reverse else (original,ident)
   dump(RES/f'data/jugcraft/recipe/jimtafacts_factory/{ident}_{"restore" if reverse else "housing"}.json',{'type':'minecraft:crafting_shapeless','category':'redstone','ingredients':['jugcraft:'+before],'result':{'id':'jugcraft:'+after}})
 if shared:
  path=RES/'assets/jugcraft/lang/en_us.json';v=json.loads(path.read_text());v.update(lang);dump(path,v)
  path=RES/'data/minecraft/tags/block/mineable/pickaxe.json';v=json.loads(path.read_text());v['values']=sorted(set(v['values'])|{'jugcraft:'+ident for ident in DEVICES});dump(path,v)
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/logistics-housing-runtime-copies.json',{'copies':copies,'adaptation':'Sorter uses existing nine-sample native sorter and one directional destination; no legacy six-face independent sorting GUI. Buffer uses shared owned nine-slot chute, configurable outputs/intake, redstone pause and finite JE boost. Both retain native menus/claim and cargo rules.'})
if __name__=='__main__':generate()
