"""Copy original paintbrush/border pixels and adapt their models to native item components."""
import hashlib,json,shutil
from jimtafacts_factory import ROOT,RES,SOURCE,dump
COLORS=['white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black']

def generate(shared=True):
 manifest=[];entries=[]
 def copy(src,dst):
  dst.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(src,dst)
  manifest.append({'source':src.relative_to(ROOT).as_posix(),'runtime':dst.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(src.read_bytes()).hexdigest()})
 for i,name in enumerate(['clean']+COLORS):
  original='silver' if name=='light_gray' else name
  model=SOURCE/f'buildcraftcore/models/item/paintbrush/{original}.json'
  data=json.loads(model.read_text());data['parent']='minecraft:item/handheld'
  for key,texture in data['textures'].items():
   ns,path=texture.split(':');src=SOURCE/f'{ns}/textures/{path}.png'
   dst=RES/f'assets/jugcraft/textures/item/jimtafacts/paintbrush/{name}.png';copy(src,dst)
   data['textures'][key]=f'jugcraft:item/jimtafacts/paintbrush/{name}'
  target=RES/f'assets/jugcraft/models/item/jimta_paintbrush_{name}.json';dump(target,data)
  manifest.append({'source':model.relative_to(ROOT).as_posix(),'runtime':target.relative_to(ROOT).as_posix(),'adaptation':'Namespace/parent migration; original UV/pixels unchanged.'})
  entries.append({'threshold':i,'model':{'type':'minecraft:model','model':f'jugcraft:item/jimta_paintbrush_{name}'}})
 dump(RES/'assets/jugcraft/items/jimta_paintbrush.json',{'model':{'type':'minecraft:range_dispatch','property':'minecraft:custom_model_data','index':0,'entries':entries,'fallback':entries[0]['model']}})
 copy(SOURCE/'buildcrafttransport/textures/pipes/colour_border_outer.png',RES/'assets/jugcraft/textures/block/jimtafacts/pipe_color_border.png')
 # Transparent source border sits just outside the native 6px core (scaled for 4px fluid/JE cores).
 dump(RES/'assets/jugcraft/models/item/jimta_pipe_color_marker.json',{'textures':{'border':'jugcraft:block/jimtafacts/pipe_color_border','particle':'jugcraft:block/jimtafacts/pipe_color_border'},'elements':[{'from':[4.98]*3,'to':[11.02]*3,'faces':{face:{'uv':[0,0,16,16],'texture':'#border','tintindex':0} for face in ['north','south','east','west','up','down']}}]})
 dump(RES/'assets/jugcraft/items/jimta_pipe_color_marker.json',{'model':{'type':'minecraft:model','model':'jugcraft:item/jimta_pipe_color_marker','tints':[{'type':'minecraft:custom_model_data','index':0,'default':16777215}]}})
 dump(RES/'data/jugcraft/recipe/jimtafacts_factory/paintbrush.json',{'type':'minecraft:crafting_shapeless','category':'equipment','ingredients':['#minecraft:wool','minecraft:iron_nugget','minecraft:stick'],'result':{'id':'jugcraft:jimta_paintbrush'}})
 if shared:
  p=RES/'assets/jugcraft/lang/en_us.json';lang=json.loads(p.read_text());lang.update({'item.jugcraft.jimta_paintbrush':'Factory Paintbrush','item.jugcraft.jimta_pipe_color_marker':'Pipe Color Marker (render helper)'});dump(p,lang)
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/paintbrush-runtime-copies.json',{'files':manifest,'adaptation':'Original 17 sprites and transparent border unchanged; native 64-use dye loads and decorative-block/JE-cable repainting. Factory pipe colors govern loaded item/fluid/JE edges; unpainted junctions bridge colors. Border mesh follows native pipe cores; source traveling-item simulation is not introduced.'})

if __name__=='__main__':generate()
