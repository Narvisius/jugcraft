"""Supplied industrial service housings and fluid pixels; native recipes and ports."""
from pathlib import Path
import hashlib,json,shutil
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
SOURCE=ROOT/'art/owner-library/originals/Oct23/Jimtafacts/industrialcraft reborn by jimbo/assets/ic2'
DEVICES={'fluid_regulator':'FluidRegulator','fluid_distributor':'FluidDistributor','solar_distiller':'SolarDestiller','steam_condenser':'Condenser'}
TRANSFORMERS={'jimta_'+n+'_transformer':n.upper() for n in ('lv','mv','hv','ev')}
def blocks():return {'jimta_'+n for n in DEVICES}|set(TRANSFORMERS)|{'distilled_water'}
def items():return {'jimta_'+n for n in DEVICES}|set(TRANSFORMERS)|{'distilled_water_bucket'}
def fluids():return {'distilled_water','flowing_distilled_water'}
def dump(path,value):
 path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(value,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
def generate(shared=True):
 manifest=[];lang={}
 def copy(src,dst):
  dst.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(src,dst)
  manifest.append({'source':src.relative_to(ROOT).as_posix(),'runtime':dst.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(src.read_bytes()).hexdigest()})
 for name,source in DEVICES.items():
  ident='jimta_'+name;src=SOURCE/f'textures/blocks/machine/block{source}.png'
  copy(src,RES/f'assets/jugcraft/textures/block/jimtafacts/services/{name}.png')
  copy(SOURCE/f'textures/gui/GUI{source}.png',RES/f'assets/jugcraft/textures/gui/jimtafacts/{name}.png')
  with Image.open(src) as im:w,h=im.size
  tiles=w//h;variants={}
  for active in (False,True):
   model=ident+('_active' if active else '');faces={}
   for face,index in zip(('down','up','north','south','west','east'),(0,1,3,5,4,2)):
    tile=index+(6 if active and tiles>=12 else 0);faces[face]={'texture':'#sheet','uv':[16*tile/tiles,0,16*(tile+1)/tiles,16],'cullface':face}
   dump(RES/f'assets/jugcraft/models/block/{model}.json',{'parent':'minecraft:block/block','textures':{'sheet':f'jugcraft:block/jimtafacts/services/{name}','particle':'#sheet'},'elements':[{'from':[0,0,0],'to':[16,16,16],'faces':faces}]})
   for facing,rot in [('north',0),('east',90),('south',180),('west',270)]:variants[f'facing={facing},lit={str(active).lower()}']={'model':'jugcraft:block/'+model,'y':rot}
  dump(RES/f'assets/jugcraft/blockstates/{ident}.json',{'variants':variants})
  dump(RES/f'assets/jugcraft/items/{ident}.json',{'model':{'type':'minecraft:model','model':'jugcraft:block/'+ident}})
  dump(RES/f'data/jugcraft/loot_table/blocks/{ident}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:'+ident}],'condition':{'type':'minecraft:survives_explosion'}}]})
  lang['block.jugcraft.'+ident]=name.replace('_',' ').title()
 copy(SOURCE/'textures/gui/GUITransfomer.png',RES/'assets/jugcraft/textures/gui/jimtafacts/transformer.png')
 for ident,tier in TRANSFORMERS.items():
  src=SOURCE/f'textures/blocks/wiring/blockTransformer{tier}.png';copy(src,RES/f'assets/jugcraft/textures/block/jimtafacts/services/{ident}.png')
  with Image.open(src) as im:w,h=im.size
  tiles=w//h
  faces={face:{'texture':'#sheet','uv':[16*tile/tiles,0,16*(tile+1)/tiles,16],'cullface':face} for face,tile in zip(('down','up','north','south','west','east'),(0,1,3,5,4,2))}
  dump(RES/f'assets/jugcraft/models/block/{ident}.json',{'parent':'minecraft:block/block','textures':{'sheet':f'jugcraft:block/jimtafacts/services/{ident}','particle':'#sheet'},'elements':[{'from':[0,0,0],'to':[16,16,16],'faces':faces}]})
  dump(RES/f'assets/jugcraft/blockstates/{ident}.json',{'variants':{f'facing={face}':{'model':'jugcraft:block/'+ident,'x':x,'y':y} for face,x,y in [('north',0,0),('east',0,90),('south',0,180),('west',0,270),('up',270,0),('down',90,0)]}})
  dump(RES/f'assets/jugcraft/items/{ident}.json',{'model':{'type':'minecraft:model','model':'jugcraft:block/'+ident}})
  dump(RES/f'data/jugcraft/loot_table/blocks/{ident}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:'+ident}],'condition':{'type':'minecraft:survives_explosion'}}]})
  lang['block.jugcraft.'+ident]=tier+' Transformer'
  ingredients={'LV':['jugcraft:copper_wire']*3+['jugcraft:rubber']*2+['jugcraft:iron_plate'],
   'MV':['jugcraft:jimta_lv_transformer','jugcraft:machine_casing','jugcraft:basic_circuit','jugcraft:copper_wire'],
   'HV':['jugcraft:jimta_mv_transformer','jugcraft:advanced_circuit','jugcraft:steel_plate','jugcraft:jimta_gold_cable'],
   'EV':['jugcraft:jimta_hv_transformer','jugcraft:advanced_circuit','jugcraft:jimta_dense_steel_plate','jugcraft:jimta_glass_cable']}[tier]
  dump(RES/f'data/jugcraft/recipe/jimtafacts_services/{ident}.json',{'type':'minecraft:crafting_shapeless','category':'redstone','ingredients':ingredients,'result':{'id':'jugcraft:'+ident}})
 for kind in ('still','flow'):
  for ext in ('.png','.png.mcmeta'):copy(SOURCE/f'textures/blocks/fluids/distilledwater_{kind}{ext}',RES/f'assets/jugcraft/textures/block/distilled_water_{kind}{ext}')
 copy(SOURCE/'textures/items/cell/itemCellDistilledWater.png',RES/'assets/jugcraft/textures/item/jimtafacts/services/distilled_water_bucket.png')
 dump(RES/'assets/jugcraft/models/item/distilled_water_bucket.json',{'parent':'minecraft:item/generated','textures':{'layer0':'jugcraft:item/jimtafacts/services/distilled_water_bucket'}})
 dump(RES/'assets/jugcraft/items/distilled_water_bucket.json',{'model':{'type':'minecraft:model','model':'jugcraft:item/distilled_water_bucket'}})
 dump(RES/'assets/jugcraft/blockstates/distilled_water.json',{'variants':{'':{'model':'minecraft:block/water'}}})
 lang.update({'block.jugcraft.distilled_water':'Distilled Water','item.jugcraft.distilled_water_bucket':'Distilled Water Container'})
 recipes={
  'fluid_regulator':['jugcraft:machine_casing','jugcraft:basic_circuit','jugcraft:electric_motor','jugcraft:jimta_copper_casing','minecraft:glass'],
  'fluid_distributor':['jugcraft:machine_casing']+['jugcraft:jimta_copper_casing']*4+['minecraft:glass'],
  'solar_distiller':['minecraft:glass']*3+['jugcraft:jimta_copper_boiler','jugcraft:jimta_copper_casing','jugcraft:machine_casing'],
  'steam_condenser':['jugcraft:jimta_heat_conductor']*2+['jugcraft:jimta_copper_boiler','jugcraft:machine_casing','jugcraft:jimta_tin_casing']}
 for name,ingredients in recipes.items():dump(RES/f'data/jugcraft/recipe/jimtafacts_services/{name}.json',{'type':'minecraft:crafting_shapeless','category':'redstone','ingredients':ingredients,'result':{'id':'jugcraft:jimta_'+name}})
 # Distillation has a real downstream use and can rejoin the shared finite steam loop.
 from jimtafacts_fluid_recipes import recipes as coolant_recipes
 dump(RES/'data/jugcraft/recipe/chemical_reaction/jimta_distilled_coolant.json',coolant_recipes()['jimta_distilled_coolant'])
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/service-runtime-copies.json',{'copies':manifest,'adaptation':'Original pixels retained; six-face atlases referenced by UV. GUI replaces obsolete source container widgets with native tank controls. No supplied program executed.'})
 if shared:
  path=RES/'assets/jugcraft/lang/en_us.json';values=json.loads(path.read_text(encoding='utf-8'));values.update(lang);dump(path,values)
  path=RES/'data/minecraft/tags/block/mineable/pickaxe.json';values=json.loads(path.read_text(encoding='utf-8'));values['values']=sorted(set(values['values'])|{'jugcraft:'+n for n in blocks() if n!='distilled_water'});dump(path,values)
 return len(DEVICES)+len(TRANSFORMERS)
if __name__=='__main__':print('Generated',generate(),'industrial service housings')
