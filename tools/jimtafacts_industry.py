from jimtafacts_crafting import distinct
"""Native industrial consolidation and missing processes; supplied art is copied unchanged.

The old binaries are never loaded. Original six/twelve-face horizontal texture sheets
remain whole; model UVs select the same face tiles as BlockMultiID's source mapping.
"""
from pathlib import Path
import hashlib
import json
import shutil
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
SOURCE = ROOT / 'art/owner-library/originals/Oct23/Jimtafacts'
IC = SOURCE / 'industrialcraft reborn by jimbo/assets/ic2'
BC = SOURCE / 'buildcraft reborn by jimbo/assets/buildcraftfactory'
MACHINES = {
 'jimta_integration_table': ('CIRCUIT_ASSEMBLER','Integration Table','bc:table/integration','circuit_assembler'),
 'jimta_assembly_table': ('CIRCUIT_ASSEMBLER','Assembly Table','bc:table/assembly','circuit_assembler'),
 'jimta_advanced_crafting_table': ('AUTO_CRAFTER','Advanced Crafting Table','bc:table/advanced_crafting','auto_crafter'),
 'jimta_generator': ('COAL_GENERATOR','Industrial Generator','generator/blockGenerator','coal_generator'),
 'jimta_bat_box': ('BATTERY_BOX','Industrial Battery Box','wiring/blockBatBox','battery_box'),
 'jimta_cesu': ('LEAD_ACID_BATTERY_BANK','Industrial CESU Bank','wiring/blockCESU','lead_acid_battery_bank'),
 'jimta_mfe': ('CAPACITOR_BANK','Industrial Capacitor Bank','wiring/blockMFE','capacitor_bank'),
 'jimta_mfsu': ('LITHIUM_BATTERY_BANK','Industrial Lithium Bank','wiring/blockMFSU','lithium_battery_bank'),
 'jimta_induction_furnace': ('ELECTRIC_FURNACE','Industrial Induction Furnace','machine/blockInduction','electric_furnace'),
 'jimta_blast_furnace': ('STEEL_FOUNDRY','Industrial Blast Furnace','machine/blockBlastFurnace','steel_foundry'),
 'jimta_crop_harvester': ('CROP_HARVESTER','Industrial Crop Harvester','machine/blockCropHavester','crop_harvester'),
 'jimta_electrolyzer': ('ELECTROLYTIC_CELL','Industrial Electrolyzer','machine/blockElectrolyzer','electrolytic_cell'),
 'jimta_electric_furnace': ('ELECTRIC_FURNACE','Industrial Electric Furnace','machine/blockElecFurnace','electric_furnace'),
 'jimta_macerator': ('CRUSHER','Macerator','machine/blockMacerator','crusher'),
 'jimta_ore_washing_plant': ('ORE_WASHER','Industrial Ore Washer','machine/blockOreWashingPlant','ore_washer'),
 'jimta_block_cutting_machine': ('SAWMILL','Industrial Cutting Mill','machine/blockBlockCutter','sawmill'),
 'jimta_solar_generator': ('SOLAR_PANEL','Industrial Solar Generator','generator/blockSolarGenerator','solar_panel'),
 'jimta_geothermal_generator': ('GEOTHERMAL_GENERATOR','Industrial Geothermal Generator','generator/blockGeoGenerator','geothermal_generator'),
 'jimta_auto_workbench': ('AUTO_CRAFTER','Automatic Workbench',None,'auto_crafter'),
 'jimta_metal_former_rolling': ('METAL_PRESS','Metal Former: Rolling','machine/blockMetalFormer','metal_press'),
 'jimta_metal_former_extruding': ('WIRE_DRAWER','Metal Former: Extruding','machine/blockMetalFormer','wire_drawer'),
 'jimta_recycler': ('JIMTA_RECYCLER','Recycler','machine/blockRecycler',None),
 'jimta_metal_cutter': ('JIMTA_METAL_CUTTER','Metal Former: Cutting','machine/blockMetalFormer',None),
 'jimta_compressor': ('JIMTA_COMPRESSOR','Compressor','machine/blockCompressor',None),
 'jimta_extractor': ('JIMTA_EXTRACTOR','Extractor','machine/blockExtractor',None),
 'jimta_thermal_centrifuge': ('JIMTA_THERMAL_CENTRIFUGE','Thermal Centrifuge','machine/blockCentrifuge',None),
}
MATERIALS = {
 'jimta_oil_glob': ('Oil Glob','bce:glob_oil'),
 'jimta_iron_coin': ('Iron Trade Token','itemCoin'),
 **{f'jimta_chipset_{n}': (n.title()+' Redstone Chipset','bc:redstone_chipset/'+n) for n in ('red','iron','gold','diamond','quartz')},
 'jimta_coal_dust': ('Coal Dust','resources/itemDustCoal'),
 'jimta_stone_dust': ('Stone Dust','resources/itemDustStone'),
 'jimta_carbon_fibre': ('Raw Carbon Fibre','itemPartCarbonFibre'),
 'jimta_carbon_mesh': ('Raw Carbon Mesh','itemPartCarbonMesh'),
 'jimta_carbon_plate': ('Carbon Plate','itemPartCarbonPlate'),
 'jimta_coal_ball': ('Coal Ball','itemPartCoalBall'),
 'jimta_compressed_coal_ball': ('Compressed Coal Ball','itemPartCoalBlock'),
 'jimta_coal_chunk': ('Coal Chunk','itemPartCoalChunk'),
}
CASING_METALS={'copper':'Copper','tin':'Tin','bronze':'Bronze','gold':'Gold','iron':'Iron','steel':'AdvIron','lead':'Lead'}
DENSE_METALS={**CASING_METALS,'obsidian':'Obsidian','lapis':'Lapi'}
DUSTS={'bronze':'Bronze','obsidian':'Obsidian','lapis':'Lapi','silica':'silicondioxide','diamond':'diamond','energium':'energium'}
for material,source in DUSTS.items():MATERIALS['jimta_'+material+'_dust']=(material.title()+' Dust','resources/itemDust'+source)
SMALL_DUSTS={'iron':('Iron','jugcraft:iron_dust'),'copper':('Copper','jugcraft:copper_dust'),'gold':('Gold','jugcraft:gold_dust'),'tin':('Tin','jugcraft:tin_dust'),'silver':('Silver','jugcraft:silver_dust'),'lead':('Lead','jugcraft:lead_dust'),'sulfur':('Sulfur','jugcraft:sulfur_dust'),'lithium':('Lithium','jugcraft:lithium_carbonate'),'bronze':('Bronze','jugcraft:jimta_bronze_dust'),'lapis':('Lapi','jugcraft:jimta_lapis_dust'),'obsidian':('Obsidian','jugcraft:jimta_obsidian_dust')}
for material,(source,whole) in SMALL_DUSTS.items():MATERIALS['jimta_small_'+material+'_dust']=(('Small Lithium Carbonate Pile' if material=='lithium' else 'Small '+material.title()+' Dust Pile'),'resources/itemDust'+source+'Small')
for metal,source in CASING_METALS.items():MATERIALS['jimta_'+metal+'_casing']=(metal.title()+' Casing','resources/itemCasing'+source)
for metal,source in DENSE_METALS.items():MATERIALS['jimta_dense_'+metal+'_plate']=('Dense '+metal.title()+' Plate','resources/itemDensePlate'+source)
for metal,source in [('gold','Gold'),('obsidian','Obsidian'),('lapis','Lapi')]:MATERIALS['jimta_'+metal+'_plate']=(metal.title()+' Plate','resources/itemPlate'+source)
MATERIALS.update({
 'jimta_heat_conductor':('Heat Conductor','resources/itemheatconductor'),
 'jimta_copper_boiler':('Copper Boiler Vessel','resources/itemcopperboiler'),
 'jimta_turbine_blade':('Steam Turbine Blade','itemSteamTurbineBlade'),
 'jimta_turbine_rotor':('Steam Turbine Rotor','itemSteamTurbine'),
 'jimta_small_power_unit':('Small Power Unit','resources/itempowerunitsmall'),
 'jimta_power_unit':('Power Unit','resources/itempowerunit'),
 'jimta_wood_turning_blank':('Wood Turning Blank','itemTurningBlanksWOOD'),
 'jimta_iron_turning_blank':('Iron Turning Blank','itemTurningBlanksIRON'),
 'jimta_thin_fluted_axle':('Thin Fluted Axle','itemTurningBlanksIRON'),
 'jimta_turned_handle':('Turned Handle','itemTurningBlanksIRON'),
 'jimta_turned_drill':('Turned Drill Head','itemTurningBlanksIRON'),
})

MATERIALS.update({
 'jimta_mixed_metal_ingot':('Mixed Metal Ingot','resources/itemIngotAlloy'),
 'jimta_advanced_alloy':('Advanced Alloy Plate','itemPartAlloy'),
 'jimta_iridium_shard':('Iridium Shard','itemShardIridium'),
 'jimta_iridium':('Refined Iridium','itemOreIridium'),
 'jimta_iridium_composite':('Iridium Composite Plate','itemPartIridium'),
 'jimta_slag':('Industrial Slag','itemSlag'),
 'jimta_grin_powder':('Grin Powder','itemGrinPowder'),
 'jimta_iron_cutting_blade':('Iron Cutting Blade','resources/itemIronBlockCuttingBlade'),
 'jimta_steel_cutting_blade':('Steel Cutting Blade','resources/itemAdvIronBlockCuttingBlade'),
 'jimta_diamond_cutting_blade':('Diamond Cutting Blade','resources/itemDiamondBlockCuttingBlade'),
})

def plate(metal):return 'jugcraft:'+('jimta_' if metal in ('gold','obsidian','lapis') else '')+metal+'_plate'

def blocks(): return set(MACHINES)
def items(): return blocks() | set(MATERIALS)
def dump(path, value):
 path.parent.mkdir(parents=True,exist_ok=True)
 path.write_text(json.dumps(value,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')

def recipes():
 result={}
 def process(name,kind,ingredient,output,count=1,inputs=1,time=200,extras=(),heat=0):
  value={'type':'jugcraft:'+kind,'ingredient':ingredient,'result':{'id':output,'count':count},'time':time}
  if inputs!=1:value['input_count']=inputs
  if heat:value['minimum_heat']=heat
  if extras:value['byproducts']=[{'result':{'id':item,'count':n},'chance':1} for item,n in extras]
  result[name]=value
 def craft(name,ingredients,output,count=1):
  if ':' not in output:output='jugcraft:'+output
  result[name]={'type':'minecraft:crafting_shapeless','category':'misc','ingredients':distinct('jimtafacts_industry/'+name,ingredients),'result':{'id':output,'count':count}}
 craft('mint_iron_tokens',['minecraft:iron_ingot'],'jugcraft:jimta_iron_coin',9)
 craft('recover_iron_tokens',['jugcraft:jimta_iron_coin']*9,'minecraft:iron_ingot')
 for material,(source,whole) in SMALL_DUSTS.items():
  small='jugcraft:jimta_small_'+material+'_dust'
  craft('divide_'+material+'_dust',[whole],small,9)
  craft('combine_'+material+'_dust',[small]*9,whole)
 for material,feed in [('bronze','jugcraft:bronze_ingot'),('obsidian','minecraft:obsidian'),('lapis','minecraft:lapis_lazuli'),('diamond','minecraft:diamond'),('silica','minecraft:quartz')]:
  process('pulverize_'+material,'crushing',feed,'jugcraft:jimta_'+material+'_dust',time=200)
 process('recover_bronze_dust','jimta_compressing','jugcraft:jimta_bronze_dust','jugcraft:bronze_ingot',time=200)
 process('obsidian_dust_plate','jimta_compressing','jugcraft:jimta_obsidian_dust','jugcraft:jimta_obsidian_plate',inputs=4,time=200)
 process('lapis_dust_plate','jimta_compressing','jugcraft:jimta_lapis_dust','jugcraft:jimta_lapis_plate',inputs=4,time=200)
 craft('energium_dust',['jugcraft:jimta_diamond_dust','minecraft:redstone_block'],'jugcraft:jimta_energium_dust')
 craft('energium_crystal',['jugcraft:jimta_energium_dust','jugcraft:jimta_advanced_battery'],'jugcraft:jimta_energy_crystal')
 result['smelt_silica_glass']={'type':'minecraft:smelting','category':'blocks','ingredient':'jugcraft:jimta_silica_dust','result':{'id':'minecraft:glass'},'experience':.1,'cookingtime':200}
 for metal,feed in [('gold','minecraft:gold_ingot'),('obsidian','minecraft:obsidian'),('lapis','minecraft:lapis_lazuli')]:
  process('compress_'+metal+'_plate','jimta_compressing',feed,plate(metal),inputs=1 if metal=='gold' else 4)
 for metal in CASING_METALS:
  process('cut_'+metal+'_casings','jimta_metal_cutting',plate(metal),'jugcraft:jimta_'+metal+'_casing',count=2,time=120)
  process('recover_'+metal+'_casings','jimta_compressing','jugcraft:jimta_'+metal+'_casing',plate(metal),inputs=2,time=120)
 for metal in DENSE_METALS:
  process('dense_'+metal+'_plate','jimta_compressing',plate(metal),'jugcraft:jimta_dense_'+metal+'_plate',inputs=9,time=400)
  process('split_dense_'+metal+'_plate','jimta_metal_cutting','jugcraft:jimta_dense_'+metal+'_plate',plate(metal),count=9,time=400)
 craft('heat_conductor',['jugcraft:copper_plate']*6+['jugcraft:rubber']*2,'jugcraft:jimta_heat_conductor',2)
 craft('copper_boiler',['jugcraft:jimta_copper_casing']*6+['jugcraft:jimta_fluid_cell'],'jugcraft:jimta_copper_boiler')
 craft('turbine_blade',['jugcraft:steel_plate']*8+['jugcraft:steel_ingot'],'jugcraft:jimta_turbine_blade')
 craft('turbine_rotor',['jugcraft:jimta_turbine_blade']*3,'jugcraft:jimta_turbine_rotor')
 craft('small_power_unit',['jugcraft:electric_motor','jugcraft:jimta_re_battery','jugcraft:jimta_iron_casing']*1,'jugcraft:jimta_small_power_unit')
 craft('power_unit',['jugcraft:electric_motor','jugcraft:jimta_advanced_battery','jugcraft:advanced_circuit','jugcraft:jimta_steel_casing']*1,'jugcraft:jimta_power_unit')
 craft('wood_turning_blank',['#minecraft:planks']*2+['minecraft:flint'],'jugcraft:jimta_wood_turning_blank')
 craft('iron_turning_blank',['jugcraft:iron_plate']*2,'jugcraft:jimta_iron_turning_blank')
 # Original component routes supplement existing reachable machines. They never convert a whole machine back to ingredients.
 for name,parts,output in [
  ('cased_machine',['jugcraft:jimta_iron_casing']*8,'machine_casing'),
  ('dense_steel_panel',['jugcraft:jimta_dense_steel_plate','jugcraft:polymer_vehicle_panel'],'aircraft_hull_reinforcement'),
  ('assembled_heat_exchanger',['jugcraft:jimta_heat_conductor']*3+['jugcraft:jimta_tin_casing']*2,'heat_exchanger'),
  ('assembled_boiler',['jugcraft:jimta_copper_boiler','jugcraft:machine_casing','jugcraft:basic_circuit','jugcraft:jimta_heat_conductor'],'jimta_steam_generator'),
  ('assembled_turbine',['jugcraft:jimta_turbine_rotor','jugcraft:machine_casing','jugcraft:precision_shaft','jugcraft:basic_circuit'],'jimta_steam_kinetic_generator'),
  ('small_power_drill',['jugcraft:jimta_small_power_unit','minecraft:iron_pickaxe','jugcraft:basic_circuit','jugcraft:copper_wire','jugcraft:steel_plate'],'jimta_industrial_drill'),
  ('small_power_saw',['jugcraft:jimta_small_power_unit','minecraft:iron_axe','jugcraft:basic_circuit','jugcraft:copper_wire','jugcraft:steel_plate'],'jimta_industrial_chainsaw'),
  ('power_unit_laser',['jugcraft:jimta_power_unit','jugcraft:jimta_energy_crystal','minecraft:diamond','minecraft:redstone_block','jugcraft:advanced_circuit'],'jimta_mining_laser')]:craft(name,parts,'jugcraft:'+output)
 craft('lathed_hand_crank',['jugcraft:jimta_turned_handle','jugcraft:iron_gear','minecraft:oak_planks'],'hand_crank')
 craft('lathed_drill',['jugcraft:jimta_turned_drill','jugcraft:jimta_small_power_unit','jugcraft:basic_circuit','jugcraft:steel_plate'],'jimta_industrial_drill')
 craft('lathed_manual_generator',['jugcraft:jimta_thin_fluted_axle','jugcraft:jimta_turned_handle','jugcraft:machine_casing'],'jimta_manual_kinetic_generator')
 # The original assembly table uses the shared circuit processor and its recipe picker/automation.
 for name,material in [('red','minecraft:redstone'),('iron','minecraft:iron_ingot'),('gold','minecraft:gold_ingot'),('diamond','minecraft:diamond'),('quartz','minecraft:quartz')]:
  ingredients=[{'ingredient':'minecraft:redstone','count':2},{'ingredient':'jugcraft:silicon','count':1}]
  ingredients.append({'ingredient':material,'count':1})
  result['chipset_'+name]={'type':'jugcraft:circuit_assembly','ingredients':ingredients,'result':{'id':'jugcraft:jimta_chipset_'+name},'time':400 if name in ('diamond','quartz') else 200}
 for mode in ('and','or'):
  for tier,chip in [('clay_brick','red'),('nether_brick','red'),('iron','iron'),('gold','gold')]:
   result[f'integrate_gate_{mode}_{tier}']={'type':'jugcraft:circuit_assembly','ingredients':[{'ingredient':'jugcraft:jimta_chipset_'+chip,'count':1},{'ingredient':'jugcraft:basic_circuit','count':1},{'ingredient':'jugcraft:copper_wire','count':2}],'result':{'id':f'jugcraft:jimta_gate_{tier}_{mode}'},'time':200}
 craft('chipset_red_basic',['jugcraft:jimta_chipset_red','jugcraft:copper_wire','jugcraft:solder_ingot'],'jugcraft:basic_circuit')
 craft('chipset_diamond_processor',['jugcraft:jimta_chipset_diamond','jugcraft:advanced_circuit'],'jugcraft:processor')
 craft('chipset_quartz_timer',['jugcraft:jimta_chipset_quartz','minecraft:clock'],'jugcraft:jimta_timer')
 # Source material families use existing processors, not another parallel factory chain.
 craft('mixed_metal_ingot',['minecraft:iron_ingot']*3+['jugcraft:bronze_ingot']*3+['jugcraft:tin_ingot']*3,'jimta_mixed_metal_ingot',2)
 process('compress_advanced_alloy','jimta_compressing','jugcraft:jimta_mixed_metal_ingot','jugcraft:jimta_advanced_alloy',time=400)
 # Trace metal recovery is a deliberately expensive native route, not duplicate ore generation.
 process('iridium_trace','jimta_thermal_centrifuging','jugcraft:washed_uranium_ore','jugcraft:uranium_dust',count=8,inputs=9,time=2400,heat=2000,extras=[('jugcraft:jimta_iridium_shard',1)])
 craft('iridium',['jugcraft:jimta_iridium_shard']*9,'jimta_iridium')
 craft('iridium_shards',['jugcraft:jimta_iridium'],'jimta_iridium_shard',9)
 craft('iridium_composite',['jugcraft:jimta_iridium']*4+['jugcraft:jimta_advanced_alloy']*4+['minecraft:diamond'],'jimta_iridium_composite')
 process('alloy_slag_recovery','jimta_thermal_centrifuging','jugcraft:jimta_mixed_metal_ingot','jugcraft:jimta_advanced_alloy',time=600,heat=1500,extras=[('jugcraft:jimta_slag',1)])
 process('slag_recovery','jimta_thermal_centrifuging','jugcraft:jimta_slag','jugcraft:jimta_stone_dust',count=4,inputs=9,time=600,heat=1500,extras=[('minecraft:iron_nugget',1)])
 process('grin_from_potato','crushing','minecraft:poisonous_potato','jugcraft:jimta_grin_powder',time=100)
 process('grin_from_eye','crushing','minecraft:spider_eye','jugcraft:jimta_grin_powder',count=2,time=100)
 craft('grin_weed_ex',['jugcraft:jimta_grin_powder','minecraft:redstone','minecraft:glass_bottle'],'jimta_weed_ex')
 craft('advanced_alloy_panel',['jugcraft:jimta_advanced_alloy','jugcraft:polymer_vehicle_panel'],'carbon_composite_panel')
 for tier,plate_input in [('iron','jugcraft:iron_plate'),('steel','jugcraft:steel_plate'),('diamond','minecraft:diamond')]:
  craft(tier+'_cutting_blade',[plate_input]*4+['jugcraft:iron_gear'],'jimta_'+tier+'_cutting_blade')
  craft(tier+'_assembled_cutting_mill',['jugcraft:jimta_'+tier+'_cutting_blade','jugcraft:machine_casing','jugcraft:electric_motor','jugcraft:basic_circuit'],'jimta_block_cutting_machine')
 craft('iridium_drill_composite',['jugcraft:jimta_diamond_drill','jugcraft:jimta_iridium_composite','jugcraft:jimta_lapotron_crystal','jugcraft:advanced_circuit'],'jimta_iridium_drill')
 # Source block-cutter tiers, using native plate material costs. Existing wood recipes need no retrofit.
 for metal,hardness in [('lead',2),('gold',2),('bronze',2),('tin',2),('copper',2),('iron',5),('steel',8)]:
  namespace='minecraft' if metal in ('iron','gold','copper') else 'jugcraft'
  name='blade_cut_'+metal
  process(name,'sawing',namespace+':'+metal+'_block',plate(metal),count=9,time=450)
  result[name]['blade_hardness']=hardness
 # Native obsidian/lapis plates each cost four input units. Preserve that ratio rather than importing an inflation loop.
 for metal,hardness,inputs,count in [('obsidian',8,4,1),('lapis',5,4,9)]:
  name='blade_cut_'+metal
  process(name,'sawing','minecraft:'+('lapis_block' if metal=='lapis' else metal),plate(metal),count=count,inputs=inputs,time=450)
  result[name]['blade_hardness']=hardness
 # Source compressor batches, translated to flattened IDs. Input counts are real, never approximated by yield.
 for name,ingredient,output,n in [
  ('clay','clay_ball','clay',4),('bricks','brick','bricks',4),('glowstone','glowstone_dust','glowstone',4),
  ('snow','snowball','snow_block',4),('ice','snow_block','ice',1),('sandstone','sand','sandstone',4),
  ('red_sandstone','red_sand','red_sandstone',4),('nether_bricks','nether_brick','nether_bricks',4),
  ('blaze_rod','blaze_powder','blaze_rod',5)]:
  process('compress_'+name,'jimta_compressing','minecraft:'+ingredient,'minecraft:'+output,inputs=n)
 for metal in ('iron','gold','copper','tin','lead','bronze','steel'):
  ns='minecraft' if metal in ('iron','gold','copper') else 'jugcraft'
  process('compress_'+metal,'jimta_compressing',ns+':'+metal+'_ingot',ns+':'+metal+'_block',inputs=9)
 process('crush_coal','crushing','minecraft:coal','jugcraft:jimta_coal_dust',time=160)
 craft('oil_globs',['jugcraft:crude_oil_bucket'],'jugcraft:jimta_oil_glob',8)
 craft('oil_globs_recombine',['jugcraft:jimta_oil_glob']*8+['minecraft:bucket'],'jugcraft:crude_oil_bucket')
 craft('carbon_fibre',['jugcraft:jimta_coal_dust']*4,'jugcraft:jimta_carbon_fibre')
 craft('carbon_mesh',['jugcraft:jimta_carbon_fibre']*2,'jugcraft:jimta_carbon_mesh')
 process('carbon_plate','jimta_compressing','jugcraft:jimta_carbon_mesh','jugcraft:jimta_carbon_plate')
 craft('coal_ball',['jugcraft:jimta_coal_dust']*8+['minecraft:flint'],'jugcraft:jimta_coal_ball')
 process('compressed_coal_ball','jimta_compressing','jugcraft:jimta_coal_ball','jugcraft:jimta_compressed_coal_ball')
 craft('coal_chunk',['jugcraft:jimta_compressed_coal_ball']*8+['minecraft:obsidian'],'jugcraft:jimta_coal_chunk')
 process('industrial_diamond','jimta_compressing','jugcraft:jimta_coal_chunk','minecraft:diamond',time=800)
 # Carbon plate joins the existing vehicle/building material economy rather than a separate armor currency.
 craft('carbon_composite_panel',['jugcraft:jimta_carbon_plate','jugcraft:polymer_vehicle_panel'],'jugcraft:carbon_composite_panel',2)
 for name,ingredient,output,n in [('bricks','bricks','brick',4),('nether_bricks','nether_bricks','nether_brick',4),
       ('clay','clay','clay_ball',4),('snow','snow_block','snowball',4)]:
  process('extract_'+name,'jimta_extracting','minecraft:'+ingredient,'minecraft:'+output,count=n)
 process('extract_sulfur','jimta_extracting','minecraft:gunpowder','jugcraft:sulfur_dust')
 process('extract_rubber_log','jimta_extracting','jugcraft:rubber_log','jugcraft:rubber')
 process('extract_rubber_sapling','jimta_extracting','jugcraft:rubber_tree_sapling','jugcraft:rubber')
 process('extract_latex','jimta_extracting','jugcraft:latex_bottle','jugcraft:rubber',count=3,extras=[('minecraft:glass_bottle',1)])
 for color in ('orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black'):
  process('extract_'+color+'_wool','jimta_extracting','minecraft:'+color+'_wool','minecraft:white_wool')
 # Nine washed units convert the source's tiny byproduct dust to one existing shared dust.
 # No uranium/plutonium shortcut: radioactive reprocessing remains in Jugcraft's containment/fuel systems.
 for metal,extra,heat in [('iron','gold',1500),('gold','silver',2000),('copper','tin',500),('tin','iron',1000),('lead','copper',2000),('silver','lead',1000)]:
  process('centrifuge_'+metal,'jimta_thermal_centrifuging','jugcraft:washed_'+metal+'_ore','jugcraft:'+metal+'_dust',
          count=9,inputs=9,time=900,extras=[('jugcraft:'+extra+'_dust',1)],heat=heat)
 process('centrifuge_stone','jimta_thermal_centrifuging','minecraft:cobblestone','jugcraft:jimta_stone_dust',heat=100)
 craft('recover_stone_dust',['jugcraft:jimta_stone_dust']*4,'minecraft:cobblestone')
 process('recycle_junk','jimta_recycling','#jugcraft:jimta_recyclable','jugcraft:jimta_scrap',inputs=6,time=120)
 for metal in ('copper','gold','iron','tin'):
  if (RES/f'assets/jugcraft/items/{metal}_wire.json').exists():process('cut_'+metal+'_wire','jimta_metal_cutting','jugcraft:'+metal+'_plate','jugcraft:'+metal+'_wire',count=2,time=160)
 process('extrude_fluid_cell','wire_drawing','jugcraft:tin_plate','jugcraft:jimta_fluid_cell',count=1,time=120)
 # Casing variants use the complete native machine as their cost and convert back one-for-one.
 for id,(_,_,_,native) in MACHINES.items():
  if native:
   craft(id+'_housing',['jugcraft:'+native],'jugcraft:'+id)
   craft(id+'_restore',['jugcraft:'+id],'jugcraft:'+native)
 for id,material in [('jimta_compressor','minecraft:piston'),('jimta_extractor','minecraft:shears'),('jimta_thermal_centrifuge','jugcraft:advanced_circuit'),('jimta_recycler','minecraft:composter'),('jimta_metal_cutter','minecraft:iron_bars')]:
  result[id]={'type':'minecraft:crafting_shaped','category':'misc','pattern':['IPI','CMC','IPI'],
    'key':{'I':'jugcraft:steel_plate','P':material,'C':'jugcraft:copper_wire','M':'jugcraft:machine_casing'},'result':{'id':'jugcraft:'+id}}
 return result

TAGLINES = {'jimta_compressor':'Compress complete batches into blocks and carbon components.',
    'jimta_extractor':'Extract rubber, reclaim materials and remove wool dye. Empty bottles return to the upper output slots.',
    'jimta_thermal_centrifuge':'Heat washed ore to its required temperature, then separate nine-unit batches. Keep both outputs clear.',
    'jimta_recycler':'Convert six pieces of permitted scrap material into one reusable scrap.',
    'jimta_metal_cutter':'Cut metal sheets into the shared wiring components.'}

def generate(shared=True):
 lang={}; manifest=[]
 def copy(source,dest):
  dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source,dest)
  manifest.append({'source':source.relative_to(ROOT).as_posix(),'runtime':dest.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(source.read_bytes()).hexdigest()})
 catalog={}
 for id,(kind,label,texture,native) in MACHINES.items():
  catalog[id]={'machine':kind,'source_texture':texture,'native_equivalent':native}
  lang['block.jugcraft.'+id]=label
  if native is None:
   lang['container.jugcraft.'+id]=label
   lang['container.jugcraft.tagline.'+id]=TAGLINES[id]
  states={}
  for active in (False,True):
   model=id+('_active' if active else '')
   if texture and texture.startswith('bc:'):
    source=SOURCE/'buildcraft reborn by jimbo/assets/buildcraftsilicon';body=json.loads((source/f'models/block/{texture[3:]}.json').read_text(encoding='utf-8'));body['parent']='minecraft:block/block'
    for face,ref in body.get('textures',{}).items():
     if ref.startswith('#'):continue
     ns,path=ref.split(':',1);destination=RES/f'assets/jugcraft/textures/block/jimtafacts/industry/buildcraftsilicon/{path}.png'
     if not active:copy(source/f'textures/{path}.png',destination)
     body['textures'][face]='jugcraft:block/jimtafacts/industry/buildcraftsilicon/'+path
   elif texture:
    original=IC/'textures/blocks'/f'{texture}.png';dest=RES/f'assets/jugcraft/textures/block/jimtafacts/industry/{id}.png'
    if not active:copy(original,dest)
    with Image.open(original) as img:width,height=img.size
    tiles=width//height
    # Decompiled BlockMultiID facing NORTH: down/up/north/south/west/east = 0/1/3/5/4/2.
    faces={}
    for face,tile in zip(('down','up','north','south','west','east'),(0,1,3,5,4,2)):
     at=(tile+(6 if active else 0)) if tiles>=12 else tile if tiles>=6 else (int(active) if tiles==2 else 0)
     faces[face]={'texture':'#sheet','uv':[16*at/tiles,0,16*(at+1)/tiles,16],'cullface':face}
    body={'parent':'minecraft:block/block','textures':{'particle':'#sheet','sheet':'jugcraft:block/jimtafacts/industry/'+id},
          'elements':[{'from':[0,0,0],'to':[16,16,16],'faces':faces}]}
   else:
    for part in ('top','side','bottom'):
     if not active:copy(BC/f'textures/blocks/auto_workbench_item/{part}.png',RES/f'assets/jugcraft/textures/block/jimtafacts/industry/workbench_{part}.png')
    body={'parent':'minecraft:block/cube','textures':{face:'jugcraft:block/jimtafacts/industry/workbench_'+part for face,part in
           [('up','top'),('down','top'),('north','side'),('south','side'),('west','side'),('east','side'),('particle','side')]}}
   dump(RES/f'assets/jugcraft/models/block/{model}.json',body)
   for facing,rot in [('north',0),('east',90),('south',180),('west',270)]:
    states[f'facing={facing},lit={str(active).lower()}']={'model':'jugcraft:block/'+model,'y':rot}
  dump(RES/f'assets/jugcraft/blockstates/{id}.json',{'variants':states})
  dump(RES/f'assets/jugcraft/items/{id}.json',{'model':{'type':'minecraft:model','model':'jugcraft:block/'+id}})
  # Supplied housings retain their original appearance under either machine style.
  if native is None:
   alternate=RES/'resourcepacks/alternate_machines/assets/jugcraft'
   dump(alternate/f'blockstates/{id}.json',{'variants':states})
   dump(alternate/f'items/{id}.json',{'model':{'type':'minecraft:model','model':'jugcraft:block/'+id}})
  dump(RES/f'data/jugcraft/loot_table/blocks/{id}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:'+id}],
       'condition':{'type':'minecraft:survives_explosion'}}]})
 for id,(label,texture) in MATERIALS.items():
  origin=(SOURCE/'buildcraft reborn by jimbo/assets/buildcraftenergy'/f'textures/items/{texture[4:]}.png') if texture.startswith('bce:') else (SOURCE/'buildcraft reborn by jimbo/assets/buildcraftsilicon'/f'textures/items/{texture[3:]}.png') if texture.startswith('bc:') else IC/f'textures/items/{texture}.png'
  copy(origin,RES/f'assets/jugcraft/textures/item/jimtafacts/industry/{id}.png')
  dump(RES/f'assets/jugcraft/models/item/{id}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'jugcraft:item/jimtafacts/industry/'+id}})
  dump(RES/f'assets/jugcraft/items/{id}.json',{'model':{'type':'minecraft:model','model':'jugcraft:item/'+id}})
  lang['item.jugcraft.'+id]=label
 dump(RES/'jugcraft/jimtafacts/industry.json',catalog)
 dump(RES/'jugcraft/jimtafacts/industry-materials.json',list(MATERIALS))
 for name,recipe in recipes().items():dump(RES/f'data/jugcraft/recipe/jimtafacts_industry/{name}.json',recipe)
 dump(RES/'data/jugcraft/tags/block/jimtafacts_industry.json',{'replace':False,'values':['jugcraft:'+id for id in MACHINES]})
 if shared:
  for tag in ('mineable/pickaxe','needs_stone_tool'):
   path=RES/f'data/minecraft/tags/block/{tag}.json';value=json.loads(path.read_text(encoding='utf-8')) if path.exists() else {'replace':False,'values':[]}
   ref='#jugcraft:jimtafacts_industry'
   if ref not in value['values']:value['values'].append(ref)
   dump(path,value)
  path=RES/'assets/jugcraft/lang/en_us.json';value=json.loads(path.read_text(encoding='utf-8'));value.update(lang)
  value['container.jugcraft.industrial_heat']='Heat: %s / %s'
  value['jei.jugcraft.minimum_heat']='Minimum heat: %s'
  dump(path,value)
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/industry-runtime-copies.json',manifest)
 # Lathed blanks have dynamic profiles; restore their specialized definitions after the base material pass.
 import jimtafacts_utilities
 jimtafacts_utilities.generate(False)
 import jimtafacts_nuclear_charge
 jimtafacts_nuclear_charge.generate(shared)
 import jimtafacts_magnetizer
 jimtafacts_magnetizer.generate(shared)
 import jimtafacts_survey_markers
 jimtafacts_survey_markers.generate(shared)
 return catalog

if __name__=='__main__':
 import sys
 print(f'Generated {len(generate(shared="--no-shared" not in sys.argv))} industrial machines, {len(MATERIALS)} materials and {len(recipes())} recipes.')
