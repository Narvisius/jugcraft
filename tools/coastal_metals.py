"""Coastal magnesium, staged titanium and reusable polymer tooling. Game quantities."""
from starter_chemistry import form
from biorefinery import liquid

FLUIDS = {
 'seawater': liquid('Coastal Seawater', [(32,68,85),(48,104,124),(81,143,153),(139,187,182)]),
 'magnesium_brine': liquid('Magnesium-bearing Brine', [(58,81,73),(91,121,101),(135,160,126),(187,199,164)]),
 'magnesium_chloride_solution': liquid('Prepared Magnesium Chloride Solution', [(77,83,89),(119,129,139),(167,179,184),(213,222,221)]),
 'crude_titanium_chloride': liquid('Crude Titanium Chloride', [(79,63,36),(125,104,59),(172,150,94),(215,200,149)]),
 'purified_titanium_chloride': liquid('Purified Titanium Chloride', [(68,78,82),(110,128,136),(155,178,186),(205,222,224)]),
 'spent_mineral_wash': liquid('Spent Mineral Wash', [(53,63,49),(90,105,72),(137,145,110),(181,187,149)]),
}
ITEMS = {'magnesium_hydroxide':'Magnesium Hydroxide', 'dry_magnesium_chloride':'Dry Magnesium Chloride',
 'magnesium_ingot':'Magnesium Ingot', 'polymer_housing':'Molded Polymer Housing',
 'polymer_fitting':'Molded Pipe Fitting', 'cable_insulation':'Molded Cable Insulation',
 'flexible_hose':'Flexible Hose', 'polymer_vehicle_panel':'Molded Vehicle Panel'}
MOLDS = ['housing','fitting','insulation','gasket','hose','panel']
ITEMS.update({f'{n}_mold': n.title()+' Mold' for n in MOLDS})
P='#c:plates/steel';M='jugcraft:electric_motor';C='jugcraft:basic_circuit';T='jugcraft:steel_tank';K='jugcraft:machine_casing';G='jugcraft:steel_gear';W='jugcraft:copper_cable';B='minecraft:bricks'

def station(display,depth,height,ins,outs,items,cap,pattern,**extra):
 ports=[('power','ENERGY_IN',0,0,0,0,'FRONT')]
 ports += [(f'feed_{i}','FLUID_IN',i,i,depth-1,1,'BACK') for i in range(len(ins))]
 ports += [(f'product_{i}','FLUID_OUT',i,i,depth-1,0,'BACK') for i in range(len(outs))]
 ports += [(f'solid_in_{i}','ITEM_IN',i,2,0,i+1,'FRONT') for i in range(items[0])]
 ports += [(f'solid_out_{i}','ITEM_OUT',i,2,depth-1,i+1,'BACK') for i in range(items[1])]
 key={s:dict(P=P,M=M,C=C,T=T,K=K,G=G,W=W,B=B)[s] for s in sorted(set(''.join(pattern))) if s!=' '}
 return form(display,extra.pop('family','chemical_reactor'),3,depth,height,ins,outs,cap,ports,(pattern,key),items=items,**extra)

FORMS={
 'coastal_seawater_intake':station('Coastal Seawater Intake',3,2,[],['seawater'],(0,0),'coastal_pumping',['PGP','MCT','WKW']),
 'brine_concentrator':station('Brine Concentrator',3,3,['seawater'],['magnesium_brine','recovered_water'],(0,0),'seawater_concentration',['BTB','TCT','WMW']),
 'mineral_conditioner':station('Mineral Conditioning Station',3,3,['mineral_feed','conditioning_reagent'],['conditioned_liquid','recovered_water'],(1,1),'mineral_conditioning',['PTP','CMC','BKB']),
 'chloride_dryer':station('Chloride Drying Oven',3,3,['chloride_solution'],['recovered_water'],(0,1),'chloride_drying',['BGB','TCT','WKW']),
 'magnesium_electrolysis_cell':station('Magnesium Electrolysis Cell',3,3,[],['chlorine'],(1,1),'magnesium_electrolysis',['WBW','BKB','PCP'],family='electrolytic_cell'),
 'titanium_chlorinator':station('Titanium Chlorination Vessel',4,4,['chlorine'],['crude_chloride','carbon_dioxide'],(2,0),'titanium_chlorination',['BTP','MCM','WKW']),
 'titanium_reduction_retort':station('Titanium Reduction Retort',4,3,['purified_chloride'],[],(1,2),'titanium_reduction',['PBP','TCT','MKM'],family='electrolytic_cell'),
 'polymer_molding_press':station('Polymer Molding Press',4,3,[],[],(1,1),'polymer_molding',['PGP','MCM','BKB'],sockets=[('mold','jugcraft:polymer_molds')]),
}
FORMS['coastal_seawater_intake']['upgrades']=False
FORMS['coastal_seawater_intake']['warmup']=0
FORMS['titanium_chlorinator']['gas_vent']=(1,'jugcraft:carbon_dioxide')

def rec(name,station,ins,outs,ticks,power,**extra):
 return dict(name=name,capability=FORMS[station]['capabilities'][0],fluids=ins,fluid_results=outs,ticks=ticks,energy_per_tick=power,features=['machines'],**extra)
RECIPES={
 'brine_concentrator':[rec('concentrate_coastal_seawater','brine_concentrator',[('jugcraft:seawater',1000)],[('jugcraft:magnesium_brine',250,0),('minecraft:water',750,1)],200,32)],
 'mineral_conditioner':[
  rec('precipitate_magnesium','mineral_conditioner',[('jugcraft:magnesium_brine',250),('jugcraft:lye',250)],[('jugcraft:spent_mineral_wash',500,0)],160,24,results=[('jugcraft:magnesium_hydroxide',1)]),
  rec('prepare_magnesium_chloride','mineral_conditioner',[('jugcraft:hydrochloric_acid',500)],[('jugcraft:magnesium_chloride_solution',500,0),('minecraft:water',250,1)],160,32,source=250,items=[('jugcraft:magnesium_hydroxide',1)]),
  rec('settle_mineral_wash','mineral_conditioner',[('jugcraft:spent_mineral_wash',500)],[('minecraft:water',250,0)],120,16,results=[('jugcraft:salt',1)]),
 ],
 'chloride_dryer':[rec('dry_magnesium_chloride','chloride_dryer',[('jugcraft:magnesium_chloride_solution',500)],[('minecraft:water',500,0)],160,48,results=[('jugcraft:dry_magnesium_chloride',1)])],
 'magnesium_electrolysis_cell':[rec('electrolyze_dry_magnesium_chloride','magnesium_electrolysis_cell',[],[('jugcraft:chlorine',250,0)],400,64,source=250,items=[('jugcraft:dry_magnesium_chloride',1)],results=[('jugcraft:magnesium_ingot',1)])],
 'titanium_chlorinator':[rec('chlorinate_titanium_mineral','titanium_chlorinator',[('jugcraft:chlorine',500)],[('jugcraft:crude_titanium_chloride',1000,0),('jugcraft:carbon_dioxide',250,1)],240,64,source=750,items=[('jugcraft:raw_titanium',1),('jugcraft:coke',1)])],
 'titanium_reduction_retort':[rec('reduce_and_clean_titanium_sponge','titanium_reduction_retort',[('jugcraft:purified_titanium_chloride',750)],[],400,96,items=[('jugcraft:magnesium_ingot',2)],results=[('jugcraft:titanium_sponge',1),('jugcraft:dry_magnesium_chloride',2)])],
 'polymer_molding_press':[],
}
EXTRA_RECIPES={'chemical_distillation_station':[dict(name='purify_titanium_chloride',capability='jugcraft:chemical_distillation',fluids=[('jugcraft:crude_titanium_chloride',1000)],fluid_results=[('jugcraft:purified_titanium_chloride',750,0),('jugcraft:spent_mineral_wash',250,1)],ticks=240,energy_per_tick=48,features=['machines'])]}
PRODUCTS=[('housing','#jugcraft:moldable_rigid_polymers',4,'polymer_housing',1),('fitting','jugcraft:pvc_resin',2,'polymer_fitting',2),('insulation','jugcraft:rubber',2,'cable_insulation',3),('gasket','jugcraft:rubber',2,'gasket',2),('hose','jugcraft:rubber',3,'flexible_hose',1),('panel','#jugcraft:moldable_rigid_polymers',4,'polymer_vehicle_panel',1)]
for mold,stock,count,product,yield_ in PRODUCTS:
 RECIPES['polymer_molding_press'].append(rec('mold_'+mold,'polymer_molding_press',[],[],120,32,items=[(stock,count)],results=[('jugcraft:'+product,yield_)],tool='jugcraft:'+mold+'_mold'))
# Silicone (docs/features/silicone.md): the gasket mold also presses two silicone rubber into three heat-proof gaskets.
RECIPES['polymer_molding_press'].append(rec('mold_silicone_gasket','polymer_molding_press',[],[],120,32,items=[('jugcraft:silicone',2)],results=[('jugcraft:gasket',3)],tool='jugcraft:gasket_mold'))
TANK_NAMES={k:k.replace('_',' ').capitalize() for f in FORMS.values() for k in f['input_tanks']+f['output_tanks']}
TANK_NAMES.update(mineral_feed='Mineral feed / prepared acid',conditioning_reagent='Lye',conditioned_liquid='Conditioned liquid / spent wash',distilled_ethanol='Distilled product',stillage='Distillation residue')
HANDBOOK={
 'coastal_seawater_intake':['Renewable coastal resource collection: 200 mB seawater for 320 JE per one-second stroke (average 10 mB and 16 JE/t). Place the lower front-right intake against open water at sea level (water surface block Y = sea level - 1) in the Overworld. It samples a 3 by 3 water patch extending forward from its mouth, two source-water blocks deep. Every column must be beach/ocean-tagged and open to the sky; the loaded surrounding region must contain ocean biome. No shoreline blocks are removed.', 'An inland pond, elevated bucket pool, flowing water, sodium-chloride brine or pipe-fed ordinary water cannot provide this mineral feed. Coastal restoration of a qualifying shore is allowed; vanilla does not record historical ownership of water blocks. No remote intake or chunk loading. Power and output space are required; pause stops collection.'],
 'brine_concentrator':['1000 mB distinct coastal seawater -> 250 mB magnesium-bearing brine + 750 mB recovered water. 200 ticks at 32 JE/t. The short evaporator column, supported condenser and collection sump share one skid. Ordinary water and sodium-chloride brine are rejected.'],
 'mineral_conditioner':['250 mB magnesium brine + 250 mB lye -> one magnesium hydroxide + 500 mB spent mineral wash; 160 ticks at 24 JE/t. Then change feeds: one hydroxide + 500 mB existing hydrochloric acid -> 500 mB prepared chloride solution and 250 mB water; 160 ticks at 32 JE/t.', 'Drain old tanks before switching recipes. Settle 500 mB spent wash into one existing salt and 250 mB water for 120 ticks at 16 JE/t; the rest is evaporation/loss in game accounting. The same station treats titanium-distillation wash. Neither wash nor ordinary salt brine supplies magnesium.'],
 'chloride_dryer':['500 mB prepared magnesium chloride solution -> one Dry Magnesium Chloride and 500 mB recovered condensate. 160 ticks at 48 JE/t includes paid conditioning/drying. Feed the dry item to the magnesium cell, never aqueous solution.'],
 'magnesium_electrolysis_cell':['One dry prepared MgCl2 -> one Magnesium Ingot + 250 mB chlorine; 400 ticks at 64 JE/t. Automatic internal heating is included in paid work. Capture all chlorine: a full outlet stops the batch. Steel, copper and clay build the first cell. Titanium recovery returns the same dry chloride, not extra metal; the same paid recipe handles both supplies.'],
 'titanium_chlorinator':['One existing Raw Titanium + one Coke + 500 mB chlorine -> 1000 mB crude titanium chloride + 250 mB CO2. 240 ticks at 64 JE/t. Default captures CO2 that fits and vents excess; Keep gas stops for full storage. The crude product always needs room.', 'Existing Chemical Distillation Station: 1000 mB crude chloride -> 750 mB purified chloride + 250 mB spent mineral wash, 240 ticks at 48 JE/t. Empty ethanol/stillage before changing jobs. Process wash in the Mineral Conditioning Station.'],
 'titanium_reduction_retort':['750 mB purified titanium chloride + two Magnesium Ingots -> one existing clean Titanium Sponge + two Dry Magnesium Chloride. 400 ticks at 96 JE/t includes internal separation and sponge cleanup. Both drawers must fit. Re-electrolyze both salt items to recover two Mg and 500 mB chlorine; it costs power, never generates it.', 'The existing Arc Furnace melts each sponge into one titanium ingot. Existing titanium tools, armor and components use it. Old titanium recipes remain available. First construction uses steel/copper/ceramics, never its own titanium or magnesium output.'],
 'polymer_molding_press':['Fit one reusable mold in the mold socket; it is locked during a batch and never consumed or worn. All six products take 120 ticks at 32 JE/t. Housings/panels accept existing plastic pellets, PVC resin or new HDPE pellets through an explicit rigid-polymer tag. Fittings require PVC. Insulation, gaskets and hoses require rubber.', 'Four rigid stock -> one housing or panel; two PVC -> two fittings; two rubber -> three insulation pieces or two existing gaskets; three rubber -> one hose. Housing assembles machine casings; fittings reinforce bronze pipes into steel pipes; insulation assembles copper cable; hoses assemble pipe valves; panels combine with coke into existing carbon-composite building/vehicle panels. These are additional routes, not new gates on old recipes.', 'Six distinct molds are made from earlier steel plates and simple pattern items. There is no free offcut award: all allocated feed becomes the listed parts. The Polymer Extruder and Clean Polymer Reclaimer add continuous shaping and same-grade offcut recovery. Named ethylene, HDPE, LDPE and staged PVC now connect through the Monomer Preparation Unit and Polymerization Kettle.'],
}
for name,f in FORMS.items():
 HANDBOOK[name].append('Power socket: lower front-left. Solid feed hatches: front-right, bottom to top. Solid outputs: rear-right, bottom to top. Liquid inputs: rear middle height, left to right viewed from the front. Liquid outputs: rear bottom, left to right. Read JEI for exact materials. All tubes terminate at the parts they serve; machines share existing pipes, cables and automation. Game quantities are not real manufacturing instructions.')

def write_extras(write,assets,data,lang,condition):
 import shutil
 from pathlib import Path
 import model_writer
 from steampunk_models import box,cyl
 from starter_chemistry_models import RUBBER,STEEL,OLIVE
 textures={'magnesium_hydroxide':'alumina','dry_magnesium_chloride':'salt','magnesium_ingot':'aluminum_ingot'}
 mold_sources=['mold_packing_4','mold_rod','mold_wire','mold_gear','mold_bullet_casing','mold_plate']
 for mold,source in zip(MOLDS,mold_sources):
  shutil.copyfile(Path(__file__).resolve().parents[1]/'art/owner-library/originals/Blocks'/f'{source}.png',assets/'textures/item'/f'{mold}_mold.png')
  textures[mold+'_mold']=mold+'_mold'
 for name,texture in textures.items():
  write(assets/'models/item'/f'{name}.json',dict(parent='minecraft:item/generated',textures={'layer0':'jugcraft:item/'+texture}))
 models={
  'polymer_housing':[box((1,1,1),(15,4,15),RUBBER),box((1,4,1),(4,13,15),RUBBER),box((12,4,1),(15,13,15),RUBBER),box((4,4,12),(12,13,15),RUBBER)],
  'polymer_fitting':cyl('x',8,8,4,1,9,RUBBER)+cyl('z',8,8,4,8,15,RUBBER),
  'cable_insulation':cyl('z',8,8,4,1,15,RUBBER)+cyl('z',8,8,2,0,1,STEEL),
  'flexible_hose':cyl('x',4,8,3,3,13,RUBBER)+cyl('x',12,8,3,3,13,RUBBER)+cyl('y',3,8,3,4,12,RUBBER)+cyl('y',13,8,3,4,12,RUBBER),
  'polymer_vehicle_panel':[box((1,2,5),(15,14,8),OLIVE),box((2,3,8),(14,5,10),RUBBER),box((2,11,8),(14,13,10),RUBBER)],
 }
 writer=model_writer.StyleWriter(assets)
 for name,elements in models.items():
  writer.item_model(name,dict(parent='minecraft:block/block',textures=dict({t:'jugcraft:block/'+t for t in model_writer.texture_names(elements)},particle='jugcraft:block/dp_olive'),elements=model_writer.scaled_elements(elements)))
 for name,display in ITEMS.items():
  lang['item.jugcraft.'+name]=display
  write(assets/'items'/f'{name}.json',dict(model=dict(type='minecraft:model',model='jugcraft:item/'+name)))
 write(data/'tags/item/polymer_molds.json',dict(replace=False,values=['jugcraft:'+n+'_mold' for n in MOLDS]))
 write(data/'tags/item/moldable_rigid_polymers.json',dict(replace=False,values=['jugcraft:plastic_pellets','jugcraft:pvc_resin']))
 for mold,pattern in zip(MOLDS,['minecraft:chest','minecraft:bucket','jugcraft:copper_cable','minecraft:slime_ball','minecraft:string','minecraft:paper']):
  write(data/'recipe'/f'{mold}_mold.json',dict(type='minecraft:crafting_shaped',category='misc',pattern=[' P ','PXP',' P '],key={'P':P,'X':pattern},result={'id':'jugcraft:'+mold+'_mold','count':1},**{'fabric:load_conditions':condition('machines')}))
 uses=[('housing_casing',['jugcraft:polymer_housing','#c:plates/steel','#c:plates/steel'],'machine_casing',1),
       ('fitting_pipe',['jugcraft:polymer_fitting','jugcraft:bronze_fluid_pipe','#c:plates/steel'],'steel_fluid_pipe',3),
       ('insulated_copper_cable',['jugcraft:cable_insulation','#c:ingots/copper','#c:ingots/copper','#c:ingots/tin'],'copper_cable',16),
       ('hose_valve',['jugcraft:flexible_hose','jugcraft:steel_fluid_pipe','#c:plates/steel'],'fluid_valve',1),
       ('molded_composite_panel',['jugcraft:polymer_vehicle_panel','jugcraft:coke','jugcraft:coke','jugcraft:coke','jugcraft:coke'],'carbon_composite_panel',4)]
 for name,ingredients,result,count in uses:
  write(data/'recipe'/f'{name}.json',dict(type='minecraft:crafting_shapeless',category='misc',ingredients=ingredients,result={'id':'jugcraft:'+result,'count':count},**{'fabric:load_conditions':condition('machines')}))
 lang['container.jugcraft.form.reason.coastal_source']='Intake needs an open coastal source at sea level'
