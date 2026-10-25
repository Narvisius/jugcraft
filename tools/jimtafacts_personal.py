"""Original safe model sheet and personal workshop atlases/GUI panels with native barter recipes."""
from pathlib import Path
import hashlib,json,shutil
from PIL import Image
ROOT=Path(__file__).resolve().parents[1];RES=ROOT/'src/main/resources'
SOURCE=ROOT/'art/owner-library/originals/Oct23/Jimtafacts/industrialcraft reborn by jimbo/assets/ic2'
DEVICES={'personal_safe':'PersonalChest','barter_station':'PersonalTrader','energy_vendor':'PersonalTraderEnergy'}
GUIS={'personal_safe':'PersonalChest','barter_station_owner':'TradeOMatOpen','barter_station_customer':'TradeOMatClosed','energy_vendor_owner':'EnergyOMatOpen','energy_vendor_customer':'EnergyOMatClosed'}
def blocks():return {'jimta_'+n for n in DEVICES}
def items():return blocks()
def dump(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
def generate(shared=True):
 manifest=[];lang={}
 def copy(src,dst):
  dst.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(src,dst);manifest.append({'source':src.relative_to(ROOT).as_posix(),'runtime':dst.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(src.read_bytes()).hexdigest()})
 copy(SOURCE/'textures/models/newsafe.png',RES/'assets/jugcraft/textures/block/jimtafacts/personal/newsafe.png')
 for name,source in GUIS.items():copy(SOURCE/f'textures/gui/GUI{source}.png',RES/f'assets/jugcraft/textures/gui/jimtafacts/{name}.png')
 for name,source in DEVICES.items():
  ident='jimta_'+name;src=SOURCE/f'textures/blocks/personal/block{source}.png';copy(src,RES/f'assets/jugcraft/textures/block/jimtafacts/personal/{name}.png')
  with Image.open(src) as im:w,h=im.size
  tiles=w//h;faces={face:{'texture':'#sheet','uv':[16*i/tiles,0,16*(i+1)/tiles,16]} for face,i in zip(('down','up','north','south','west','east'),(0,1,3,5,4,2))}
  textures={'sheet':f'jugcraft:block/jimtafacts/personal/{name}','particle':'#sheet'}
  dump(RES/f'assets/jugcraft/models/block/{ident}.json',{'parent':'minecraft:block/block','textures':textures,'elements':[{'from':[0,0,0],'to':[16,16,16],'faces':faces}]})
  if name=='personal_safe':
   dump(RES/f'assets/jugcraft/models/block/{ident}_entity.json',{'textures':textures,'elements':[]})
   manifest.append({'source':'ic2/core/block/personal/ModelPersonalChest.class','runtime':'src/client/java/io/github/jimbozoomer/jugcraft/client/jimtafacts/PersonalSafeRenderer.java','adaptation':'Recovered six-box 64x64 mirrored UV mesh and original cubic eased hinged door; source facing transforms. Original block atlas provides inventory icon.'})
  dump(RES/f'assets/jugcraft/blockstates/{ident}.json',{'variants':{f'facing={face},open={str(opened).lower()}':{'model':'jugcraft:block/'+ident+('_entity' if name=='personal_safe' else ''),'y':rot} for face,rot in [('north',0),('east',90),('south',180),('west',270)] for opened in (False,True)}})
  dump(RES/f'assets/jugcraft/items/{ident}.json',{'model':{'type':'minecraft:model','model':'jugcraft:block/'+ident}})
  dump(RES/f'data/jugcraft/loot_table/blocks/{ident}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:'+ident}],'condition':{'type':'minecraft:survives_explosion'}}]});lang['block.jugcraft.'+ident]=name.replace('_',' ').title()
 recipes={'personal_safe':['minecraft:chest']+['jugcraft:steel_plate']*6+['jugcraft:iron_gear'],
  'barter_station':['minecraft:chest','jugcraft:machine_casing','jugcraft:basic_circuit','minecraft:comparator'],
  'energy_vendor':['jugcraft:jimta_bat_box','jugcraft:machine_casing','jugcraft:basic_circuit','minecraft:comparator']}
 for name,ingredients in recipes.items():dump(RES/f'data/jugcraft/recipe/jimtafacts_personal/{name}.json',{'type':'minecraft:crafting_shapeless','category':'redstone','ingredients':ingredients,'result':{'id':'jugcraft:jimta_'+name}})
 if shared:
  p=RES/'assets/jugcraft/lang/en_us.json';v=json.loads(p.read_text(encoding='utf-8'));v.update(lang);dump(p,v)
  p=RES/'data/minecraft/tags/block/mineable/pickaxe.json';v=json.loads(p.read_text(encoding='utf-8'));v['values']=sorted(set(v['values'])|{'jugcraft:'+n for n in blocks()});dump(p,v)
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/personal-runtime-copies.json',manifest)
 return len(DEVICES)
if __name__=='__main__':print('Generated',generate(),'personal workshop housings')
