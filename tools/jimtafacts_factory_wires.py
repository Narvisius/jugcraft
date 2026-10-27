"""Original sixteen wire icons/skins on native removable pipe signal channels."""
import json,hashlib,shutil
from jimtafacts_factory import ROOT,RES,SOURCE,dump
COLORS=['white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black']
def items():return {'jimta_pipe_wire_'+c for c in COLORS}
def generate(shared=True):
 manifest=[];lang={}
 for i,color in enumerate(COLORS):
  legacy='silver' if color=='light_gray' else color;ident='jimta_pipe_wire_'+color
  for folder,dest in [('items/wire','item'),('wires','block')]:
   src=SOURCE/f'buildcrafttransport/textures/{folder}/{legacy}.png';target=RES/f'assets/jugcraft/textures/{dest}/jimtafacts/{ident}.png';target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(src,target)
   manifest.append({'source':src.relative_to(ROOT).as_posix(),'runtime':target.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(src.read_bytes()).hexdigest()})
  dump(RES/f'assets/jugcraft/models/item/{ident}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'jugcraft:item/jimtafacts/'+ident}})
  # Seat narrow conductors against the six-pixel conduit. Four along each side.
  edge=i//4;along=5.25+(i%4)*1.5
  x,y=(along,11.05) if edge==0 else (11.05,along) if edge==1 else (along,4.35) if edge==2 else (4.35,along)
  dump(RES/f'assets/jugcraft/models/item/{ident}_fitted.json',{'textures':{'wire':'jugcraft:block/jimtafacts/'+ident,'particle':'#wire'},'elements':[{'from':[x,y,0],'to':[x+.6,y+.6,8.3],'faces':{face:{'texture':'#wire','uv':[0,0,16,16]} for face in ['up','down','north','south','west','east']}}]})
  dump(RES/f'assets/jugcraft/items/{ident}.json',{'model':{'type':'minecraft:range_dispatch','property':'minecraft:custom_model_data','index':0,'entries':[{'threshold':1,'model':{'type':'minecraft:model','model':'jugcraft:item/'+ident+'_fitted'}}],'fallback':{'type':'minecraft:model','model':'jugcraft:item/'+ident}}})
  dump(RES/f'data/jugcraft/recipe/jimtafacts_transport/{ident}.json',{'type':'minecraft:crafting_shapeless','category':'redstone','ingredients':['jugcraft:copper_wire','minecraft:redstone','minecraft:'+color+'_dye'],'result':{'id':'jugcraft:'+ident,'count':4}})
  lang['item.jugcraft.'+ident]=color.replace('_',' ').title()+' Pipe Signal Wire'
 if shared:
  p=RES/'assets/jugcraft/lang/en_us.json';data=json.loads(p.read_text());data.update(lang);dump(p,data)
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/pipe-wires-runtime-copies.json',manifest)
if __name__=='__main__':generate()
