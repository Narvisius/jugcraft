"""Source teleporter, Tesla and four charge pads connected to native JE and equipment."""
from pathlib import Path
import json,shutil,hashlib
ROOT=Path(__file__).resolve().parents[1];RES=ROOT/'src/main/resources';SOURCE=ROOT/'art/owner-library/originals/Oct23/Jimtafacts/industrialcraft reborn by jimbo/assets/ic2'
DEVICES={'jimta_teleporter':'machine/blockTeleporter','jimta_tesla':'machine/blockTesla','jimta_chargepad_batbox':'wiring/blockChargepadBatBox','jimta_chargepad_cesu':'wiring/blockChargepadCESU','jimta_chargepad_mfe':'wiring/blockChargepadMFE','jimta_chargepad_mfsu':'wiring/blockChargepadMFSU'}
DEVICES['jimta_cropmatron']='machine/blockCropmatron'
DEVICES['jimta_civil_advanced_miner']='machine/blockAdvMiner'
DEVICES['jimta_civil_miner']='machine/blockMiner'
DEVICES['jimta_iron_furnace']='machine/blockIronFurnace'
def items():return set(DEVICES)|{'jimta_magnetizer'}|{'jimta_frequency_transmitter'}
def blocks():return set(DEVICES)|{'jimta_magnetizer'}
def dump(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
def generate(shared=True):
 manifest=[];lang={}
 def copy(src,dest):dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(src,dest);manifest.append({'source':src.relative_to(ROOT).as_posix(),'runtime':dest.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(src.read_bytes()).hexdigest()})
 from PIL import Image
 for id,source in DEVICES.items():
  src=SOURCE/f'textures/blocks/{source}.png';dest=RES/f'assets/jugcraft/textures/block/jimtafacts/infrastructure/{id}.png';copy(src,dest)
  with Image.open(src) as im:w,h=im.size
  tiles=w//h
  states={}
  for active in (False,True):
   offset=6 if active and tiles>=12 else 0;model=id+('_active' if active else '')
   faces={face:{'texture':'#all','uv':[16*(index+offset)/tiles,0,16*(index+offset+1)/tiles,16]} for face,index in [('down',0),('up',1),('north',3),('south',5),('west',4),('east',2)]}
   dump(RES/f'assets/jugcraft/models/block/{model}.json',{'parent':'minecraft:block/block','textures':{'all':'jugcraft:block/jimtafacts/infrastructure/'+id,'particle':'#all'},'elements':[{'from':[0,0,0],'to':[16,16,16],'faces':faces}]})
   for facing,rot in [('north',0),('east',90),('south',180),('west',270)]:states[f'facing={facing},lit={str(active).lower()}']={'model':'jugcraft:block/'+model,'y':rot}
  dump(RES/f'assets/jugcraft/blockstates/{id}.json',{'variants':states});dump(RES/f'assets/jugcraft/items/{id}.json',{'model':{'type':'minecraft:model','model':'jugcraft:block/'+id}})
  dump(RES/f'data/jugcraft/loot_table/blocks/{id}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:'+id}],'condition':{'type':'minecraft:survives_explosion'}}]});lang['block.jugcraft.'+id]=id.removeprefix('jimta_').replace('_',' ').title()

 id='jimta_frequency_transmitter';copy(SOURCE/'textures/items/itemFreq.png',RES/f'assets/jugcraft/textures/item/jimtafacts/infrastructure/{id}.png');dump(RES/f'assets/jugcraft/models/item/{id}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'jugcraft:item/jimtafacts/infrastructure/'+id}});dump(RES/f'assets/jugcraft/items/{id}.json',{'model':{'type':'minecraft:model','model':'jugcraft:item/'+id}});lang['item.jugcraft.'+id]='Frequency Transmitter'
 def craft(name,parts,result):dump(RES/f'data/jugcraft/recipe/jimtafacts_infrastructure/{name}.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':parts,'result':{'id':'jugcraft:'+result,'count':1}})
 craft('transmitter',['jugcraft:basic_circuit','minecraft:redstone','minecraft:ender_pearl'],'jimta_frequency_transmitter')
 craft('teleporter',['jugcraft:advanced_circuit']*2+['minecraft:ender_pearl']*2+['minecraft:diamond','jugcraft:steel_plate','jugcraft:machine_casing'],'jimta_teleporter')
 craft('tesla',['jugcraft:advanced_circuit','jugcraft:machine_casing','jugcraft:steel_plate']+['jugcraft:copper_wire']*4,'jimta_tesla')
 craft('iron_furnace',['minecraft:furnace']+['minecraft:iron_ingot']*4,'jimta_iron_furnace')
 craft('miner',['jugcraft:jimta_civil_mining_well','jugcraft:copper_wire'],'jimta_civil_miner')
 craft('advanced_miner',['jugcraft:jimta_civil_quarry','jugcraft:advanced_circuit','jugcraft:jimta_item_filter'],'jimta_civil_advanced_miner')
 craft('cropmatron',['jugcraft:machine_casing','jugcraft:basic_circuit','jugcraft:electric_motor','jugcraft:bronze_fluid_pipe','minecraft:dispenser'],'jimta_cropmatron')
 craft('magnetizer',['jugcraft:machine_casing','jugcraft:basic_circuit','jugcraft:jimta_iron_fence']+['jugcraft:copper_wire']*3,'jimta_magnetizer')
 dump(RES/'data/jugcraft/tags/item/magnetizer_boots.json',{'replace':False,'values':['minecraft:'+n+'_boots' for n in ('chainmail','iron','golden','diamond','netherite')]+['jugcraft:bronze_boots']+['jugcraft:jimta_'+n+'_boots' for n in ('industrial_bronze','nano','quantum')]})
 for tier,battery in [('batbox','jimta_bat_box'),('cesu','jimta_cesu'),('mfe','jimta_mfe'),('mfsu','jimta_mfsu')]:
  craft('chargepad_'+tier,['jugcraft:'+battery,'jugcraft:steel_plate','jugcraft:basic_circuit','minecraft:stone_pressure_plate']+(['jugcraft:advanced_circuit'] if tier=='cesu' else []),'jimta_chargepad_'+tier)
 dump(RES/'data/jugcraft/tags/block/jimtafacts_infrastructure.json',{'replace':False,'values':['jugcraft:'+id for id in DEVICES]})
 if shared:
  p=RES/'assets/jugcraft/lang/en_us.json';v=json.loads(p.read_text(encoding='utf-8'));v.update(lang);dump(p,v)
  for tag in ('mineable/pickaxe','needs_iron_tool'):
   p=RES/f'data/minecraft/tags/block/{tag}.json';v=json.loads(p.read_text(encoding='utf-8'));ref='#jugcraft:jimtafacts_infrastructure'
   if ref not in v['values']:v['values'].append(ref)
   dump(p,v)
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/infrastructure-runtime-copies.json',manifest)
 return items()
if __name__=='__main__':
 import sys
 print(f'Generated {len(generate("--no-shared" not in sys.argv))} infrastructure items.')
