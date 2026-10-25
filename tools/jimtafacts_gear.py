"""Original industrial equipment art on shared native tools, armor and JE systems."""
from pathlib import Path
import json,shutil,hashlib
ROOT=Path(__file__).resolve().parents[1];RES=ROOT/'src/main/resources'
SOURCE=ROOT/'art/owner-library/originals/Oct23/Jimtafacts/industrialcraft reborn by jimbo/assets/ic2'
GEAR={}
def add(id,label,icon,behavior,**more):GEAR['jimta_'+id]={'label':label,'icon':icon,'behavior':behavior,**more}
for id,label,icon,capacity in [('re_battery','Rechargeable Battery','itemBatRE',10000),('advanced_battery','Advanced Battery','itemAdvBat',100000),('energy_crystal','Energy Crystal','itemBatCrystal',1000000),('lapotron_crystal','Lapotron Crystal','itemBatLamaCrystal',10000000)]:
 add(id,label,icon,'battery',capacity=capacity,frames=5)
for base,icon,capacity in [('re_battery','itemBatChargeRE',40000),('advanced_battery','itemBatChargeAdv',400000),('energy_crystal','itemBatChargeCrystal',4000000),('lapotron_crystal','itemBatChargeLamaCrystal',40000000)]:
 add('charging_'+base,'Charging '+base.replace('_',' ').title(),icon,'auto_battery',capacity=capacity,frames=5,base=base)
add('single_use_battery','Single-use Battery','itemBatSU','single_battery')
add('nano_saber','Nano Saber','itemNanoSaber.off','saber',capacity=200000,icons=['itemNanoSaber.off','itemNanoSaber.active'])
for id,label,icon,asset,capacity in [('battery_pack','Battery Pack','itemArmorBatpack','batpack',60000),('advanced_battery_pack','Advanced Battery Pack','itemArmorAdvBatpack','advbatpack',600000),('lap_pack','LapPack','itemArmorLappack','lappack',2000000),('energy_pack','Energy Pack','itemArmorEnergypack','energypack',10000000)]:
 add(id,label,icon,'battery_pack',asset=asset,slot='CHESTPLATE',capacity=capacity)
add('electric_jetpack','Electric Jetpack','itemArmorJetpackElectric','jetpack',asset='jetpack',slot='CHESTPLATE',capacity=200000)
add('biogas_jetpack','Biogas Jetpack','itemArmorJetpack','fuel_jetpack',asset='jetpack',slot='CHESTPLATE')
add('construction_foam_pack','Construction Foam Backpack','itemArmorCFPack','foam_pack',asset='batpack',slot='CHESTPLATE')
for family,capacity in [('nano',1000000),('quantum',10000000)]:
 for part,old,slot in [('helmet','Helmet','HELMET'),('chestplate','Chestplate','CHESTPLATE'),('leggings','Legs','LEGGINGS'),('boots','Boots','BOOTS')]:
  add(f'{family}_{part}',family.title()+' '+part.title(),f'itemArmor{family.title()}{old}','powered_armor',asset=family,slot=slot,capacity=capacity,family=family)
for part,old,slot in [('helmet','Helmet','HELMET'),('chestplate','Chestplate','CHESTPLATE'),('leggings','Legs','LEGGINGS'),('boots','Boots','BOOTS')]:
 add('industrial_bronze_'+part,'Industrial Bronze '+part.title(),'itemArmorBronze'+old,'armor',asset='bronze',slot=slot)
for part,icon,asset,slot in [('hood','itemArmorHazmatHelmet','hazmat','HELMET'),('jacket','itemArmorHazmatChestplate','hazmat','CHESTPLATE'),('trousers','itemArmorHazmatLeggings','hazmat','LEGGINGS'),('boots','itemArmorRubBoots','rubber','BOOTS')]:
 add('hazmat_'+part,'Industrial Hazmat '+part.title(),icon,'hazmat',asset=asset,slot=slot)
add('alloy_chestplate','Composite Chestplate','itemArmorAlloyChestplate','armor',asset='alloy',slot='CHESTPLATE')
add('industrial_night_vision_goggles','Industrial Night Vision Goggles','itemNightvisionGoggles','nightvision',asset='nightvision',slot='HELMET',capacity=200000)
add('solar_helmet','Solar Helmet','itemSolarHelmet','solar',asset='solar',slot='HELMET')
add('static_boots','Static Boots','itemStaticBoots','static',asset='rubber',slot='BOOTS')
for id,label,icon,behavior,capacity in [('industrial_drill','Industrial Drill','itemToolDrill','drill',30000),('diamond_drill','Diamond Drill','itemToolDDrill','drill',100000),('iridium_drill','Iridium Drill','itemToolIridiumDrill','drill',500000),('industrial_chainsaw','Industrial Chainsaw','itemToolChainsaw','chainsaw',100000),('electric_hoe','Electric Hoe','itemToolHoe','hoe',10000),('mining_laser','Mining Laser','itemToolMiningLaser','laser',200000),('electric_wrench','Electric Wrench','itemToolWrenchElectric','wrench',10000),('electric_treetap','Electric Treetap','itemTreetapElectric','tap',10000),('crop_analyzer','Crop Analyzer','itemCropnalyzer','analyzer',10000),('ore_scanner','Ore Scanner','itemScanner','scanner',10000),('advanced_ore_scanner','Advanced Ore Scanner','itemScannerAdv','scanner',100000)]:
 add(id,label,icon,behavior,capacity=capacity)
for id,label,icon,behavior in [('industrial_wrench','Industrial Wrench','itemToolWrench','wrench'),('hand_treetap','Hand Treetap','itemTreetap','tap'),('energy_meter','Energy Meter','itemToolMEter','meter'),('wind_meter','Wind Meter','windmeter','wind')]:add(id,label,icon,behavior)
for kind,icon in [('pickaxe','Pickaxe'),('axe','Axe'),('shovel','Spade'),('hoe','Hoe'),('sword','Sword')]:add('industrial_bronze_'+kind,'Industrial Bronze '+kind.title(),'itemToolBronze'+icon,'bronze_tool',kind=kind)

def items():return set(GEAR)
def item_tags():
 out={kind+'s':['jugcraft:jimta_industrial_bronze_'+kind] for kind in ('hoe','axe','pickaxe','shovel','sword')}
 out['hoes'].append('jugcraft:jimta_electric_hoe')
 out['swords'].append('jugcraft:jimta_nano_saber')
 return out
def dump(path,obj):path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(obj,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
def recipes():
 out={}
 def craft(id,parts):out[id]={'type':'minecraft:crafting_shapeless','category':'equipment','ingredients':parts,'result':{'id':'jugcraft:'+id,'count':1}}
 for id,row in GEAR.items():
  b=row['behavior'];parts=['jugcraft:steel_plate','jugcraft:basic_circuit','jugcraft:copper_wire']
  if b=='battery':parts={'jimta_re_battery':['jugcraft:tin_ingot','jugcraft:copper_wire','minecraft:redstone','minecraft:redstone'],
   'jimta_advanced_battery':['jugcraft:jimta_re_battery','jugcraft:sulfur_dust','jugcraft:lead_ingot','jugcraft:basic_circuit'],
   'jimta_energy_crystal':['jugcraft:jimta_advanced_battery','minecraft:diamond','minecraft:redstone_block'],
   'jimta_lapotron_crystal':['jugcraft:jimta_energy_crystal','jugcraft:advanced_circuit','minecraft:lapis_block','minecraft:diamond']}[id]
  elif b=='auto_battery':parts=['jugcraft:jimta_'+row['base']]*4+['jugcraft:copper_wire']
  elif b=='saber':parts=['jugcraft:jimta_carbon_plate','jugcraft:jimta_carbon_plate','jugcraft:jimta_energy_crystal','jugcraft:advanced_circuit','minecraft:diamond']
  elif b=='single_battery':parts=['jugcraft:tin_ingot','minecraft:redstone','minecraft:coal']
  elif b=='battery_pack':parts=['jugcraft:steel_plate','jugcraft:steel_plate','jugcraft:copper_wire','jugcraft:'+('jimta_re_battery' if row['capacity']<100000 else 'jimta_advanced_battery' if row['capacity']<1000000 else 'jimta_energy_crystal' if row['capacity']<10000000 else 'jimta_lapotron_crystal')]*1
  elif b=='powered_armor':parts=['minecraft:iron_'+id.rsplit('_',1)[1],'jugcraft:jimta_carbon_plate','jugcraft:advanced_circuit','jugcraft:jimta_energy_crystal']
  elif b=='armor':parts=['jugcraft:bronze_ingot']*{'HELMET':5,'CHESTPLATE':8,'LEGGINGS':7,'BOOTS':4}[row['slot']]+['minecraft:black_dye']
  elif b=='hazmat':parts=['jugcraft:rubber']*{'HELMET':5,'CHESTPLATE':8,'LEGGINGS':7,'BOOTS':4}[row['slot']]
  elif b=='bronze_tool':
   pattern={'axe':['XXD','XS ',' S '],'pickaxe':['XXX','DS ',' S '],'shovel':['DX ',' S ',' S '],'hoe':['XXD',' S ',' S '],'sword':['DX ',' X ',' S ']}[row['kind']]
   out[id]={'type':'minecraft:crafting_shaped','category':'equipment','pattern':pattern,'key':{'X':'jugcraft:bronze_ingot','S':'minecraft:stick','D':'minecraft:black_dye'},'result':{'id':'jugcraft:'+id,'count':1}};continue
  elif b=='laser':parts=['jugcraft:advanced_circuit','jugcraft:advanced_circuit','jugcraft:jimta_energy_crystal','minecraft:diamond','minecraft:redstone_block','jugcraft:steel_plate']
  elif b=='jetpack':parts=['jugcraft:electric_motor','jugcraft:electric_motor','jugcraft:jimta_advanced_battery','jugcraft:steel_plate','jugcraft:steel_plate','jugcraft:advanced_circuit']
  elif b=='fuel_jetpack':parts=['jugcraft:steel_plate']*4+['jugcraft:jimta_fluid_cell']*2+['jugcraft:basic_circuit','minecraft:furnace']
  elif b=='foam_pack':parts=['jugcraft:tin_plate']*4+['jugcraft:rubber']*2+['jugcraft:jimta_fluid_cell','minecraft:leather']
  elif b in ('drill','chainsaw','hoe'):parts+=['jugcraft:electric_motor','jugcraft:jimta_re_battery',{'drill':'minecraft:iron_pickaxe','chainsaw':'minecraft:iron_axe','hoe':'minecraft:iron_hoe'}[b]]
  elif b=='tap':parts=['minecraft:oak_planks','minecraft:oak_planks','minecraft:stick']+(['jugcraft:jimta_re_battery','jugcraft:basic_circuit'] if row.get('capacity') else [])
  elif b=='wrench':parts=['jugcraft:bronze_ingot']*4+['minecraft:stick']+(['jugcraft:jimta_re_battery','jugcraft:basic_circuit'] if row.get('capacity') else [])
  elif b in ('analyzer','scanner','meter','wind','nightvision','solar','static'):parts += [{'analyzer':'minecraft:wheat_seeds','scanner':'minecraft:compass','meter':'minecraft:redstone_torch','wind':'minecraft:feather','nightvision':'minecraft:golden_carrot','solar':'minecraft:daylight_detector','static':'minecraft:leather_boots'}[b]]
  if id=='jimta_alloy_chestplate':parts=['minecraft:iron_chestplate','jugcraft:steel_plate','jugcraft:jimta_carbon_plate']
  if id=='jimta_diamond_drill':parts=['jugcraft:jimta_industrial_drill','minecraft:diamond','minecraft:diamond','jugcraft:advanced_circuit']
  if id=='jimta_iridium_drill':parts=['jugcraft:jimta_diamond_drill','minecraft:netherite_ingot','jugcraft:jimta_carbon_plate','jugcraft:advanced_circuit']
  if id=='jimta_advanced_ore_scanner':parts=['jugcraft:jimta_ore_scanner','jugcraft:jimta_energy_crystal','jugcraft:advanced_circuit']
  if b=='powered_armor' and row['family']=='quantum':parts=['jugcraft:jimta_nano_'+id.rsplit('_',1)[1],'jugcraft:jimta_lapotron_crystal','minecraft:netherite_ingot','jugcraft:advanced_circuit']
  craft(id,parts)
 return out

def generate(shared=True):
 manifest=[];seen=set();lang={}
 def copy(source,target):
  if target in seen:return
  seen.add(target);target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source,target)
  manifest.append({'source':source.relative_to(ROOT).as_posix(),'runtime':target.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'changes':'None; original pixels and animation retained.'})
 for id,row in GEAR.items():
  icons=row.get('icons') or [row['icon']+'.'+str(i) for i in range(row.get('frames',0))] or [row['icon']]
  for i,icon in enumerate(icons):
   suffix='_'+str(i) if len(icons)>1 else '';dest=RES/f'assets/jugcraft/textures/item/jimtafacts/gear/{id}{suffix}.png'
   source=SOURCE/f'textures/items/{icon}.png';copy(source,dest)
   if source.with_suffix('.png.mcmeta').exists():copy(source.with_suffix('.png.mcmeta'),dest.with_suffix('.png.mcmeta'))
   dump(RES/f'assets/jugcraft/models/item/{id}{suffix}.json',{'parent':'minecraft:item/handheld' if row['behavior'] in ('bronze_tool','drill','chainsaw','hoe','laser','wrench','tap','saber') else 'minecraft:item/generated','textures':{'layer0':'jugcraft:item/jimtafacts/gear/'+id+suffix}})
  model={'type':'minecraft:model','model':'jugcraft:item/'+id+('_0' if len(icons)>1 else '')}
  if len(icons)>1:model={'type':'minecraft:range_dispatch','property':'minecraft:custom_model_data','index':0,'fallback':model,'entries':[{'threshold':i,'model':{'type':'minecraft:model','model':'jugcraft:item/'+id+'_'+str(i)}} for i in range(1,len(icons))]}
  dump(RES/f'assets/jugcraft/items/{id}.json',{'model':model});lang['item.jugcraft.'+id]=row['label']
  if 'asset' in row:
   asset=row['asset'];layers={}
   for number,layer in [(1,'humanoid'),(2,'humanoid_leggings')]:
    src=SOURCE/f'textures/armor/{asset}_{number}.png'
    if src.exists():copy(src,RES/f'assets/jugcraft/textures/entity/equipment/{layer}/jimtafacts/{asset}.png');layers[layer]=[{'texture':'jugcraft:jimtafacts/'+asset}]
   dump(RES/f'assets/jugcraft/equipment/jimtafacts/{asset}.json',{'layers':layers})
 for id,recipe in recipes().items():dump(RES/f'data/jugcraft/recipe/jimtafacts_gear/{id}.json',recipe)
 dump(RES/'jugcraft/jimtafacts/gear.json',GEAR);dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/gear-runtime-copies.json',manifest)
 if shared:
  path=RES/'assets/jugcraft/lang/en_us.json';current=json.loads(path.read_text(encoding='utf-8'));current.update(lang);dump(path,current)
  for tag,ids in item_tags().items():
   path=RES/f'data/minecraft/tags/item/{tag}.json';current=json.loads(path.read_text(encoding='utf-8')) if path.exists() else {'replace':False,'values':[]}
   for id in ids:
    if id not in current['values']:current['values'].append(id)
   dump(path,current)
 return manifest
if __name__=='__main__':
 import sys
 generate(shared='--no-shared' not in sys.argv);print(f'Generated {len(GEAR)} functional industrial gear entries with original icons and armor.')
