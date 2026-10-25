from jimtafacts_crafting import distinct
"""Original compact reactor textures, GUI and component tuning on native JE/radiation."""
from pathlib import Path
import json,shutil,hashlib
import jimtafacts_industry as industry
ROOT=Path(__file__).resolve().parents[1];RES=ROOT/'src/main/resources';SOURCE=ROOT/'art/owner-library/originals/Oct23/Jimtafacts/industrialcraft reborn by jimbo/assets/ic2'
PARTS={}
def part(id,source,kind,*,cells=0,capacity=0,self=0,hull=0,neighbor=0,plating=0,life=0):PARTS['jimta_'+id]={'source':source,'kind':kind,'cells':cells,'capacity':capacity,'self':self,'hull':hull,'neighbor':neighbor,'plating':plating,'life':life}
for fuel,source in [('uranium','Uranium'),('mox','MOX')]:
 for tier,n in [('simple',1),('dual',2),('quad',4)]:
  part(f'{fuel}_rod_{tier}','reactor'+source+tier.title(),fuel,cells=n,life=20000 if fuel=='uranium' else 10000)
  part(f'{fuel}_rod_{tier}_depleted','reactor'+source+tier.title()+'depleted','spent',cells=n)
for name,source,capacity in [('coolant_10k','Simple',10000),('coolant_30k','Triple',30000),('coolant_60k','Six',60000)]:part(name,'reactorCoolant'+source,'coolant',capacity=capacity)
for name,source,capacity in [('reactor_plating','',1000),('heat_plating','Heat',2000),('containment_plating','Explosive',500)]:part(name,'reactorPlating'+source,'plating',plating=capacity)
for name,source,capacity,neighbor,hull in [('heat_exchanger','',2500,12,4),('core_heat_exchanger','Core',5000,0,72),('component_heat_exchanger','Spread',5000,36,0),('advanced_heat_exchanger','Diamond',10000,24,8)]:part(name,'reactorHeatSwitch'+source,'exchanger',capacity=capacity,neighbor=neighbor,hull=hull)
for name,source,cool,hull in [('heat_vent','',6,0),('reactor_heat_vent','Core',5,5),('overclocked_heat_vent','Gold',20,36),('advanced_heat_vent','Diamond',12,0)]:part(name,'reactorVent'+source,'vent',capacity=1000,self=cool,hull=hull)
part('component_heat_vent','reactorVentSpread','spread_vent',neighbor=4)
part('neutron_reflector','reactorReflector','reflector',life=10000);part('thick_neutron_reflector','reactorReflectorThick','reflector',life=40000)
part('redstone_condensator','reactorCondensator','condensator',capacity=20000);part('lapis_condensator','reactorCondensatorLap','condensator',capacity=100000)
part('lithium_cell','reactorLithiumCell','lithium');part('tritium_cell','itemTritiumCell','isotope');part('plutonium_pellet','itemPlutonium','plutonium');part('empty_fuel_rod','itemFuelRod','empty')
part('rtg_pellet','itemRTGPellet','rtg',life=20000)
BLOCKS={'jimta_compact_reactor':'generator/blockNuclearReactor','jimta_reactor_chamber':'reactor/blockReactorChamber','jimta_reactor_vessel':'reactor/blockreactorvessel','jimta_reactor_fluid_port':'reactor/blockReactorFluidPort','jimta_reactor_access_hatch':'reactor/blockReactorAccessHatch','jimta_reactor_redstone_port':'reactor/blockReactorRedstonePort'}
def items():return set(PARTS)|set(BLOCKS)
def blocks():return set(BLOCKS)
def dump(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def generate(shared=True):
 manifest=[];lang={}
 def copy(src,dest):dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(src,dest);manifest.append({'source':src.relative_to(ROOT).as_posix(),'runtime':dest.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(src.read_bytes()).hexdigest()})
 for id,part in PARTS.items():
  copy(SOURCE/f'textures/items/{part["source"]}.png',RES/f'assets/jugcraft/textures/item/jimtafacts/reactor/{id}.png')
  dump(RES/f'assets/jugcraft/models/item/{id}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'jugcraft:item/jimtafacts/reactor/'+id}})
  dump(RES/f'assets/jugcraft/items/{id}.json',{'model':{'type':'minecraft:model','model':'jugcraft:item/'+id}});lang['item.jugcraft.'+id]=id.removeprefix('jimta_').replace('_',' ').title()
 for id,source in BLOCKS.items():
  src=SOURCE/f'textures/blocks/{source}.png';dest=RES/f'assets/jugcraft/textures/block/jimtafacts/reactor/{id}.png';copy(src,dest)
  # Original six-face horizontal strip, with exact same per-face tiles as the source block renderer.
  from PIL import Image
  with Image.open(src) as image:w,h=image.size
  tile=h/w*16;faces={face:{'texture':'#all','uv':[(index%(w//h))*tile,0,(index%(w//h)+1)*tile,16]} for face,index in [('down',0),('up',1),('north',3),('south',5),('west',4),('east',2)]}
  dump(RES/f'assets/jugcraft/models/block/{id}.json',{'parent':'minecraft:block/block','textures':{'all':'jugcraft:block/jimtafacts/reactor/'+id,'particle':'#all'},'elements':[{'from':[0,0,0],'to':[16,16,16],'faces':faces}]})
  dump(RES/f'assets/jugcraft/blockstates/{id}.json',{'variants':{'':{'model':'jugcraft:block/'+id}}});dump(RES/f'assets/jugcraft/items/{id}.json',{'model':{'type':'minecraft:model','model':'jugcraft:block/'+id}})
  dump(RES/f'data/jugcraft/loot_table/blocks/{id}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:'+id}],'condition':{'type':'minecraft:survives_explosion'}}]});lang['block.jugcraft.'+id]=id.removeprefix('jimta_').replace('_',' ').title()
 copy(SOURCE/'textures/gui/GUINuclearReaktor.png',RES/'assets/jugcraft/textures/gui/jimtafacts/compact_reactor.png')
 dump(RES/'jugcraft/jimtafacts/reactor.json',{id:{k:v for k,v in p.items() if k!='source'} for id,p in PARTS.items()})
 recipes={}
 def craft(name,parts,out,count=1):recipes[name]={'type':'minecraft:crafting_shapeless','category':'misc','ingredients':distinct('jimtafacts_reactor/'+name,parts),'result':{'id':'jugcraft:'+out,'count':count}}
 craft('core',['jugcraft:steel_plate']*3+['jugcraft:lead_plate']*3+['jugcraft:advanced_circuit','jugcraft:heat_exchanger','jugcraft:electric_motor'],'jimta_compact_reactor')
 craft('chamber',['jugcraft:steel_plate']*4+['jugcraft:lead_plate']*4,'jimta_reactor_chamber')
 for part,extra in [('vessel','jugcraft:lead_plate'),('fluid_port','jugcraft:steel_fluid_pipe'),('access_hatch','minecraft:hopper'),('redstone_port','minecraft:comparator')]:
  craft(part,['jugcraft:steel_plate']*3+['jugcraft:lead_plate','jugcraft:machine_casing',extra],'jimta_reactor_'+part)
 craft('rtg_pellet',['jugcraft:jimta_plutonium_pellet']*3+['jugcraft:iron_plate']*3,'jimta_rtg_pellet')
 craft('empty_fuel_rod',['jugcraft:tin_plate']*2,'jimta_empty_fuel_rod')
 for fuel in ('uranium','mox'):
  for tier,n in [('simple',1),('dual',2),('quad',4)]:
   parts=['jugcraft:leu_fuel_assembly']*n+['jugcraft:jimta_empty_fuel_rod']*n
   if fuel=='mox':parts+=['jugcraft:jimta_plutonium_pellet']
   craft(f'{fuel}_{tier}',parts,f'jimta_{fuel}_rod_{tier}')
   craft(f'spent_{fuel}_{tier}',['jugcraft:'+f'jimta_{fuel}_rod_{tier}_depleted'],'spent_fuel_assembly',n)
 recipes['recover_plutonium']={'type':'jugcraft:jimta_thermal_centrifuging','ingredient':'jugcraft:spent_fuel_assembly','input_count':9,'result':{'id':'jugcraft:jimta_plutonium_pellet','count':1},'byproducts':[{'result':{'id':'jugcraft:uranium_dust','count':4},'chance':1}],'minimum_heat':2000,'time':2400}
 for id,p in PARTS.items():
  if p['kind'] in ('uranium','mox','spent','plutonium','isotope','empty','rtg'):continue
  if id=='jimta_lithium_cell':parts=['jugcraft:lithium_carbonate','jugcraft:jimta_empty_fuel_rod']
  elif p['kind']=='coolant':parts=['jugcraft:tin_plate']*(p['capacity']//10000)+['minecraft:water_bucket']
  elif p['kind']=='plating':parts=['jugcraft:lead_plate','jugcraft:steel_plate']+(['jugcraft:copper_plate'] if p['plating']==2000 else ['minecraft:obsidian'] if p['plating']==500 else [])
  elif p['kind']=='reflector':parts=['jugcraft:tin_plate','jugcraft:copper_plate','jugcraft:jimta_coal_dust']*(2 if p['life']>10000 else 1)
  elif p['kind']=='condensator':parts=['jugcraft:copper_plate','jugcraft:basic_circuit']+(['minecraft:lapis_lazuli']*4 if p['capacity']>20000 else ['minecraft:redstone']*4)
  else:parts=['jugcraft:copper_plate','minecraft:iron_bars','jugcraft:electric_motor']+(['minecraft:diamond'] if 'advanced' in id else ['minecraft:gold_ingot'] if 'overclocked' in id else ['jugcraft:tin_plate'])
  craft(id,parts,id)
 craft('service_redstone',['jugcraft:jimta_redstone_condensator','minecraft:redstone'],'jimta_redstone_condensator')
 craft('service_lapis',['jugcraft:jimta_lapis_condensator','minecraft:lapis_lazuli'],'jimta_lapis_condensator')
 craft('isotope_check_source',['jugcraft:jimta_tritium_cell','jugcraft:lead_plate'],'check_source')
 for name,value in recipes.items():dump(RES/f'data/jugcraft/recipe/jimtafacts_reactor/{name}.json',value)
 dump(RES/'data/jugcraft/tags/block/jimtafacts_reactor.json',{'replace':False,'values':['jugcraft:'+id for id in sorted(BLOCKS)]})
 lang['item.jugcraft.jimta_lithium_cell']='Reactor Lithium Cell'
 if shared:
  path=RES/'assets/jugcraft/lang/en_us.json';value=json.loads(path.read_text(encoding='utf-8'));value.update(lang);dump(path,value)
  for tag in ('mineable/pickaxe','needs_iron_tool'):
   path=RES/f'data/minecraft/tags/block/{tag}.json';value=json.loads(path.read_text(encoding='utf-8'));ref='#jugcraft:jimtafacts_reactor'
   if ref not in value['values']:value['values'].append(ref)
   dump(path,value)
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/reactor-runtime-copies.json',manifest)
 return items()
if __name__=='__main__':
 import sys
 print(f'Generated {len(generate("--no-shared" not in sys.argv))} original reactor items/blocks.')
