"""Expanded farm fuels and first staged aluminum refinery. Local playtest balance."""
from starter_chemistry import form

def liquid(display, colors):
    return dict(display=display,feature='machines',colors=colors,tick_delay=8,slope=3,drop_off=1)
FLUIDS={
 'grain_mash':liquid('Heated Crop Mash',[(73,51,27),(119,85,43),(165,126,69),(207,171,113)]),
 'ethanol_broth':liquid('Fermented Ethanol Broth',[(91,70,35),(139,114,64),(185,160,100),(225,206,157)]),
 'stillage':liquid('Distillery Stillage',[(55,40,25),(94,69,42),(133,101, 63),(173,141,98)]),
 'aluminate_slurry':liquid('Bauxite Aluminate Slurry',[(82, 40,28),(125,69,46),(174,110, 70),(217,156,111)]),
}
GASES={'fermentation_exhaust':dict(display='Wet Fermentation CO2',feature='machines',colors=[(101,96,79),(146,140,118),(186,182,159),(220,219,197)])}
ITEMS={'aluminum_hydroxide':'Aluminum Hydroxide','bauxite_residue':'Bauxite Residue'}

def station(name,w,d,h,inputs,outputs,cap,items,pattern,key):
    # Every port terminates at a real exposed face with the shared physical socket model.
    ports=[('power','ENERGY_IN',0,0,0,0,'FRONT')]
    ports += [(f'feed_{i}','FLUID_IN',i,i,d-1,0,'BACK') for i in range(len(inputs))]
    ports += [(f'product_{i}','FLUID_OUT',i,i,d-1,h-1,'BACK') for i in range(len(outputs))]
    ports += [(f'solid_in_{i}','ITEM_IN',i,w-1,0,i+1,'FRONT') for i in range(items[0])]
    ports += [(f'solid_out_{i}','ITEM_OUT',i,w-1,d-1,i+1,'BACK') for i in range(items[1])]
    return form(name,'chemical_reactor',w,d,h,inputs,outputs,cap,ports,(pattern,key),items=items)
P='#c:plates/steel'; M='jugcraft:electric_motor'; C='jugcraft:basic_circuit'; T='jugcraft:steel_tank'; K='jugcraft:machine_casing'; G='jugcraft:steel_gear'; W='jugcraft:copper_cable'; B='minecraft:bricks'
FORMS={
 'mashing_station':station('Heated Mashing Station',3,3,3,['mash_water'],['crop_mash'],'crop_mashing',(1,0),['PGP','TMT','PCP'],dict(P=P,G=G,T=T,M=M,C=C)),
 'industrial_fermenter':station('Industrial Fermenter',3,3,4,['crop_mash'],['ethanol_broth','fermentation_exhaust'],'controlled_fermentation',(0,0),['PTP','TCT','PMP'],dict(P=P,T=T,C=C,M=M)),
 'chemical_distillation_station':station('Chemical Distillation Station',4,4,6,['ethanol_broth'],['distilled_ethanol','stillage'],'chemical_distillation',(0,0),['TPT','MCK','WBW'],dict(T=T,P=P,M=M,C=C,K=K,W=W,B=B)),
 'bauxite_digester':station('Bauxite Digestion Vessel',3,4,3,['digestion_lye','digestion_water'],['aluminate_slurry'],'bauxite_digestion',(1,0),['PTP','BMB','WCW'],dict(P=P,T=T,B=B,M=M,W=W,C=C)),
 'aluminate_filter_press':station('Aluminate Recovery Press',3,4,3,['aluminate_slurry'],['recovered_lye','recovered_water'],'aluminate_recovery',(0,2),['PGP','MCM','TKT'],dict(P=P,G=G,M=M,C=C,T=T,K=K)),
 'alumina_calciner':station('Alumina Calciner',3,4,3,[],['recovered_water'],'alumina_calcination',(1,1),['BPB','MCM','WKW'],dict(B=B,P=P,M=M,C=C,W=W,K=K)),
 'aluminum_reduction_cell':station('Aluminum Reduction Cell',3,3,4,[],['carbon_dioxide'],'aluminum_reduction',(2,1),['WBW','BCB','PKP'],dict(W=W,B=B,C=C,P=P,K=K)),
}
# Place working outlets beside the process they serve, rather than on generic tall standpipes.
_PORTS = {
 'mashing_station':{('FLUID_OUT',0):(1,2,0)},
 'industrial_fermenter':{('FLUID_IN',0):(2,2,0),('FLUID_OUT',0):(0,2,0)},
 'chemical_distillation_station':{('FLUID_IN',0):(2,3,1),('FLUID_OUT',0):(0,3,1),('FLUID_OUT',1):(2,3,0)},
 'bauxite_digester':{('FLUID_IN',0):(0,3,1),('FLUID_IN',1):(1,3,1),('FLUID_OUT',0):(2,3,0)},
 'aluminate_filter_press':{('FLUID_IN',0):(1,3,1),('FLUID_OUT',0):(0,3,0),('FLUID_OUT',1):(1,3,0)},
 'alumina_calciner':{('FLUID_OUT',0):(0,3,1)},
 'aluminum_reduction_cell':{('FLUID_OUT',0):(0,2,1)},
}
for name,overrides in _PORTS.items():
 FORMS[name]['ports']=[(n,k,i,*overrides.get((k,i),(c,r,l)),side) for n,k,i,c,r,l,side in FORMS[name]['ports']]
FORMS['chemical_distillation_station']['family']='distillation_tower'
FORMS['aluminum_reduction_cell']['family']='electrolytic_cell'
FORMS['industrial_fermenter']['gas_vent']=(1,'jugcraft:fermentation_exhaust')
FORMS['aluminum_reduction_cell']['gas_vent']=(0,'jugcraft:carbon_dioxide')

def recipe(name,station,fluids,outputs,ticks,power,**extra):
    return dict(name=name,capability=FORMS[station]['capabilities'][0],fluids=fluids,fluid_results=outputs,ticks=ticks,energy_per_tick=power,features=['machines'],**extra)
RECIPES={
 'mashing_station':[recipe('heat_crop_mash','mashing_station',[('minecraft:water',1000)],[('jugcraft:grain_mash',1250,0)],200,24,items=[('#jugcraft:fermentable',16)],source=250)],
 'industrial_fermenter':[recipe('ferment_crop_mash','industrial_fermenter',[('jugcraft:grain_mash',1250)],[('jugcraft:ethanol_broth',1000,0),('jugcraft:fermentation_exhaust',250,1)],400,8)],
 'chemical_distillation_station':[recipe('distill_ethanol_broth','chemical_distillation_station',[('jugcraft:ethanol_broth',1000)],[('jugcraft:bioethanol',500,0),('jugcraft:stillage',500,1)],200,32)],
 'bauxite_digester':[recipe('digest_bauxite','bauxite_digester',[('jugcraft:lye',250),('minecraft:water',750)],[('jugcraft:aluminate_slurry',1000,0)],200,64,items=[('jugcraft:bauxite',1)])],
 'aluminate_filter_press':[recipe('recover_aluminate','aluminate_filter_press',[('jugcraft:aluminate_slurry',1000)],[('jugcraft:lye',200,0),('minecraft:water',500,1)],160,32,results=[('jugcraft:aluminum_hydroxide',2),('jugcraft:bauxite_residue',1)])],
 'alumina_calciner':[recipe('calcine_hydroxide','alumina_calciner',[],[('minecraft:water',250,0)],200,64,items=[('jugcraft:aluminum_hydroxide',2)],results=[('jugcraft:alumina',2)],source=250)],
 'aluminum_reduction_cell':[recipe('reduce_alumina','aluminum_reduction_cell',[],[('jugcraft:carbon_dioxide',250,0)],400,96,items=[('jugcraft:alumina',2),('jugcraft:coke',1)],results=[('jugcraft:aluminum_ingot',2)],source=250)],
}
EXTRA_RECIPES={
 'biogas_cleaner':[dict(name='clean_fermentation_co2',capability='jugcraft:biogas_cleaning',fluids=[('jugcraft:fermentation_exhaust',250),('minecraft:water',250)],fluid_results=[('jugcraft:carbon_dioxide',250,0),('minecraft:water',250,1)],ticks=100,energy_per_tick=24,features=['machines'])],
 'anaerobic_digester':[dict(name='digest_distillery_stillage',capability='jugcraft:anaerobic_digestion',fluids=[('jugcraft:stillage',1000)],fluid_results=[('jugcraft:raw_biogas',250,0),('jugcraft:digestate',500,1)],ticks=300,energy_per_tick=24,features=['machines'])],
}
TANK_NAMES={k:k.replace('_',' ').capitalize() for f in FORMS.values() for k in f['input_tanks']+f['output_tanks']}
TANK_NAMES.update(raw_biogas='Raw process gas',clean_biogas='Cleaned process gas',wash_digestate='Recovered wash liquid')
HANDBOOK={
 'mashing_station':['Three wide, deep and tall. Mill and heat 16 fermentable crops with 1000 mB water into 1250 mB mash, 200 ticks at 24 JE/t. The grain hopper, guarded milling drive and insulated mash kettle share one machine. Existing crop and water producers supply it; water alone cannot make fuel.'],
 'industrial_fermenter':['Three wide/deep, four tall. Process 1250 mB heated mash into 1000 mB broth and 250 mB wet CO2 in 400 ticks at 8 JE/t. Its sealed jacketed vessel and slow top agitator have a high rear gas outlet.', 'Default: collect exhaust that fits, vent only the excess. Select Keep gas before starting to stop for a full gas output instead. Running batches retain their selected mode through saves. Broth always needs space. The Biogas Cleaner uses 250 mB water to clean 250 mB wet gas into usable CO2, returning its water; empty its old biogas contents before changing jobs. CO2 feeds existing methane synthesis.'],
 'chemical_distillation_station':['Four wide/deep and six tall. Feed 1000 mB fermented broth; collect 500 mB existing Bioethanol and 500 mB Stillage, 200 ticks at 32 JE/t. A heated column, condenser, reboiler, pump and service ladder separate these products. Both outputs must fit.', 'Ethanol already fuels the Bio-Generator and existing compatible engines. The digester accepts 1000 mB stillage instead of solid farm feed, producing 250 mB raw biogas and 500 mB digestate in 300 ticks at 22 JE/t per expanded lane. Continue through the existing cleaner and separator. Pure water and digestate cannot make ethanol. Dehydration is reserved for a future real consumer; the original direct reactor ethanol recipe remains.'],
 'bauxite_digester':['Three wide, four deep and three tall. One existing Bauxite + 250 mB lye + 750 mB water -> 1000 mB aluminate slurry, 200 ticks at 64 JE/t. The sealed horizontal vessel has a locking cover, heat jacket, feed hatch and service pipes. Mine or trade bauxite and make lye with the copper-built Electrolytic Separator.'],
 'aluminate_filter_press':['Three wide, four deep and three tall. 1000 mB slurry -> two Aluminum Hydroxide, one Bauxite Residue, 200 mB recovered lye and 500 mB water, 160 ticks at 32 JE/t. Return lye to digestion, adding 50 mB makeup per bauxite. Both liquid and both solid outputs must have room.', 'One residue + one clay ball + two gravel craft two cobblestone as stabilized game filler. Raw bauxite residue is not fertilizer or digester feed. The press uses visible filter plates, clamping rods, a collection trough and separate output drawers.'],
 'alumina_calciner':['Three wide, four deep and three tall. Two hydroxide -> two existing Alumina + 250 mB recovered water, 200 ticks at 64 JE/t. Reuse this water with the press water. Enclosed ceramic kiln, guarded drive, insulated service hood and front charge door; all heating is paid electricity. Alumina keeps its existing catalyst and metal consumers.'],
 'aluminum_reduction_cell':['Three wide/deep and four tall. Two Alumina + one Coke -> two existing Aluminum Ingots + 250 mB CO2, 400 ticks at 96 JE/t. Copper busbars and clay bricks start the line without requiring its own aluminum. Carbon is consumed once per batch; there is no electrode repair meter.', 'Default collect-and-vent excess CO2, or select Keep gas before starting. Ingots must always fit. Existing aluminum wire, cable, plates and structural/vehicle recipes remain the useful destinations. The larger molten-salt hall, alloy expansion and new specialized products are later work.'],
}
_PORT_GUIDE={
 'mashing_station':'Rear: water low left, mash low middle.',
 'industrial_fermenter':'Rear: mash low right, broth low left, wet CO2 high middle.',
 'chemical_distillation_station':'Rear: broth midway up toward the right, ethanol midway up at the left, stillage low toward the right.',
 'bauxite_digester':'Rear: lye midway up at the left, water midway up in the middle, slurry low right.',
 'aluminate_filter_press':'Rear: slurry midway up in the middle, recovered lye low left and water low middle. Two solid output hatches sit on the right.',
 'alumina_calciner':'Rear: recovered water midway up at the left, alumina in the right hatch.',
 'aluminum_reduction_cell':'Rear: CO2 midway up at the left, ingots in the right hatch. Front right: lower hatch takes alumina, upper hatch takes coke.',
}
for key in FORMS:
    HANDBOOK[key].append('Power: lower front left socket. Left/right descriptions use the view from the front. '+_PORT_GUIDE[key]+' Solid inputs are front-right hatches and solid outputs rear-right hatches. Each pipe is physically routed into its vessel, pump or collection tray. Use existing pipes, tanks, cylinders, cables and conveyors; earlier steel/copper/ceramics supply all crafting bills.')

def write_extras(write,assets,data,lang,condition):
    for name in ITEMS:
        write(assets/'models/item'/f'{name}.json',dict(parent='minecraft:item/generated',textures={'layer0':f'jugcraft:item/{name}'}))
        write(assets/'items'/f'{name}.json',dict(model=dict(type='minecraft:model',model=f'jugcraft:item/{name}')))
    lang.update({'container.jugcraft.form.vent_on':'Vent excess','container.jugcraft.form.vent_off':'Keep gas','container.jugcraft.form.vent_hint':'Next batch: collect gas that fits, vent only excess. Keep gas stops for full storage. Running batches keep their setting.'})
    write(data/'recipe/stabilized_bauxite_residue.json',dict(type='minecraft:crafting_shapeless',category='misc',**{'fabric:load_conditions':condition('machines')},ingredients=['jugcraft:bauxite_residue','minecraft:clay_ball','minecraft:gravel','minecraft:gravel'],result=dict(id='minecraft:cobblestone',count=2)))

# Append a solution inlet without changing existing save roles, slots or port coordinates.
FORMS["alumina_calciner"]["input_tanks"] = ["oven_solution"]
FORMS["alumina_calciner"]["ports"].append(("solution_in", "FLUID_IN", 0, 1, 3, 0, "BACK"))
