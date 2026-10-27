"""Original magnetic fence elevator housing; native bounded JE lift."""
import hashlib,json
from PIL import Image
from asset_png import save_rgba
from jimtafacts_construction import ROOT,RES,SOURCE,dump

def generate(shared=True):
 ident='jimta_magnetizer';src=SOURCE/'textures/blocks/machine/blockMagnetizer.png';im=Image.open(src).convert('RGBA');tile=im.height;manifest=[];textures={}
 for face,index in {'down':0,'up':1,'north':3,'south':5,'west':4,'east':2}.items():
  target=RES/f'assets/jugcraft/textures/jimtafacts/magnetizer/{face}.png';target.parent.mkdir(parents=True,exist_ok=True);save_rgba(im.crop((index*tile,0,(index+1)*tile,tile)),target)
  textures[face]=f'jugcraft:jimtafacts/magnetizer/{face}'
  manifest.append({'source':src.relative_to(ROOT).as_posix(),'runtime':target.relative_to(ROOT).as_posix(),'source_sha256':hashlib.sha256(src.read_bytes()).hexdigest(),'adaptation':f'Original atlas tile {index}'})
 textures['particle']=textures['north'];dump(RES/f'assets/jugcraft/models/block/{ident}.json',{'parent':'minecraft:block/cube','textures':textures})
 dump(RES/f'assets/jugcraft/blockstates/{ident}.json',{'variants':{f'facing={face}':{'model':f'jugcraft:block/{ident}','y':angle} for face,angle in [('north',0),('east',90),('south',180),('west',270)]}})
 dump(RES/f'assets/jugcraft/items/{ident}.json',{'model':{'type':'minecraft:model','model':f'jugcraft:block/{ident}'}})
 dump(RES/f'data/jugcraft/recipe/jimtafacts_industry/{ident}.json',{'type':'minecraft:crafting_shaped','category':'misc','pattern':['CFC','CMC','CRC'],'key':{'C':'jugcraft:copper_wire','F':'jugcraft:jimta_iron_fence','M':'jugcraft:machine_casing','R':'jugcraft:basic_circuit'},'result':{'id':'jugcraft:'+ident}})
 dump(RES/f'data/jugcraft/loot_table/blocks/{ident}.json',{'type':'minecraft:block','pools':[{'rolls':1,'condition':{'type':'minecraft:survives_explosion'},'entries':[{'type':'minecraft:item','name':'jugcraft:'+ident}]}]})
 if shared:
  p=RES/'assets/jugcraft/lang/en_us.json';data=json.loads(p.read_text());data['block.jugcraft.'+ident]='Magnetizer';dump(p,data)
  p=RES/'data/minecraft/tags/block/mineable/pickaxe.json';data=json.loads(p.read_text());data['values']=list(dict.fromkeys(data['values']+['jugcraft:'+ident]));dump(p,data)
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/magnetizer-runtime-copies.json',manifest)
 return {ident}
if __name__=='__main__':generate()
