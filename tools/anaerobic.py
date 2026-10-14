"""Owner-selected bulk farm-waste digestion. Fixed game mixtures, no pressure simulation.

All new quantities are local playtest balance. Reuses the industrial form backend and existing Infuser.
"""
from starter_chemistry import form

def recipe(name, form_name, fluids, outputs, ticks, energy, **extras):
    capability = 'jugcraft:starter_gas_infusing' if form_name == 'chemical_infuser' else FORMS[form_name]['capabilities'][0]
    return dict(name=name, capability=capability, fluids=fluids, fluid_results=outputs,
                ticks=ticks, energy_per_tick=energy, features=['machines'], **extras)

GASES = {
    'raw_biogas': dict(display='Raw Biogas (60% Methane / 40% CO2)', feature='machines', colors=[(67,75,47),(105,116,70),(149,158,106),(194,204,155)]),
    'clean_biogas': dict(display='Cleaned Biogas (60% Methane / 40% CO2)', feature='machines', colors=[(79,104,63),(118,150,94),(164,188,138),(211,226,189)]),
}
FLUIDS = {'digestate': dict(display='Digestate', feature='machines', colors=[(60,49,30),(93,78,47),(128,109,69),(165,147,100)],tick_delay=12,slope=2,drop_off=2)}
ITEMS = {'digestate_fiber':'Digestate Fiber'}
FEEDSTOCK = ['minecraft:wheat','minecraft:carrot','minecraft:potato','minecraft:beetroot','minecraft:rotten_flesh',
             'jugcraft:corn','jugcraft:barley','jugcraft:oats','jugcraft:cabbage','jugcraft:tomato','jugcraft:sweet_potato']

FORMS = {
 'anaerobic_digester': form('Anaerobic Digester','chemical_reactor',6,6,6,['digestion_water'],['raw_biogas','digestate'], 'anaerobic_digestion',[
     ('biomass_in','ITEM_IN',0,0,1,2,'LEFT'),('water_in','FLUID_IN',0,5,5,0,'BACK'),
     ('biogas_out','FLUID_OUT',0,5,5,4,'BACK'),('digestate_out','FLUID_OUT',1,0,5,0,'BACK'),
     ('power','ENERGY_IN',0,0,0,0,'FRONT')],
     (['PTP','TMT','PCP'],{'P':'#c:plates/steel','T':'jugcraft:steel_tank','M':'jugcraft:electric_motor','C':'jugcraft:basic_circuit'}),items=(1,0)),
 'biogas_cleaner': form('Biogas Cleaner','chemical_reactor',2,3,3,['raw_biogas','cleaning_water'],['clean_biogas','wash_digestate'],'biogas_cleaning',[
     ('raw_in','FLUID_IN',0,1,2,2,'BACK'),('water_in','FLUID_IN',1,0,2,0,'BACK'),
     ('clean_out','FLUID_OUT',0,1,0,2,'FRONT'),('wash_out','FLUID_OUT',1,1,0,0,'FRONT'),
     ('power','ENERGY_IN',0,0,0,0,'FRONT')],
     (['PTP','MCK','PWP'],{'P':'#c:plates/steel','T':'jugcraft:fluid_tank','M':'jugcraft:electric_motor','C':'jugcraft:basic_circuit','K':'jugcraft:machine_casing','W':'jugcraft:steel_fluid_pipe'})),
 'digestate_separator': form('Digestate Separator','chemical_reactor',2,3,3,['digestate_feed'],['recovered_water'],'digestate_separation',[
     ('slurry_in','FLUID_IN',0,0,2,1,'BACK'),('water_out','FLUID_OUT',0,1,2,0,'BACK'),
     ('fiber_out','ITEM_OUT',0,1,0,0,'FRONT'),('power','ENERGY_IN',0,0,0,0,'FRONT')],
     (['PTP','GCM','PKP'],{'P':'#c:plates/steel','G':'jugcraft:steel_gear','M':'jugcraft:electric_motor','C':'jugcraft:basic_circuit','T':'jugcraft:fluid_tank','K':'jugcraft:machine_casing'}),items=(0,1)),
 'bio_generator': form('Bio-Generator','gas_turbine',3,4,3,['fuel'],[],'unused',[
     ('biofuel_in','FLUID_IN',0,2,1,0,'RIGHT'),('liquid_fuel_in','FLUID_IN',0,2,3,0,'BACK'),('power_out','ENERGY_OUT',0,0,3,1,'BACK')],
     (['PGP','MCT','PKP'],{'P':'#c:plates/steel','G':'jugcraft:iron_gear','M':'jugcraft:electric_motor','C':'jugcraft:basic_circuit','T':'jugcraft:fluid_tank','K':'jugcraft:machine_casing'})),
}
FORMS['anaerobic_digester'].update(profile='EXPANDED',tank_mb=8000,warmup=80)
FORMS['bio_generator'].update(capabilities=[],fuels=[('jugcraft:clean_biogas',192,64),('jugcraft:bioethanol',192,64)],upgrades=False,warmup=0)

RECIPES = {
 'anaerobic_digester':[recipe('digest_farm_material','anaerobic_digester',[('minecraft:water',1000)],
     [('jugcraft:raw_biogas',1000,0),('jugcraft:digestate',750,1)],400,32,items=[('#jugcraft:digester_feedstock',16)],source=750)],
 'biogas_cleaner':[recipe('clean_biogas','biogas_cleaner',[('jugcraft:raw_biogas',1000),('minecraft:water',250)],
     [('jugcraft:clean_biogas',1000,0),('jugcraft:digestate',250,1)],100,24)],
 'digestate_separator':[recipe('separate_digestate','digestate_separator',[('jugcraft:digestate',1000)],
     [('minecraft:water',750,0)],100,16,results=[('jugcraft:digestate_fiber',1)])],
}
for key,rows in RECIPES.items():
    for row in rows:row['capability']=FORMS[key]['capabilities'][0]
UPGRADING = recipe('infuser_biogas_upgrading','chemical_infuser', [('jugcraft:clean_biogas',1000),('jugcraft:hydrogen',1600)],
    [('jugcraft:methane',1000,0),('minecraft:water',1600,1)],600,128,tool='jugcraft:nickel_catalyst_bed')

TANK_NAMES = {'digestion_water':'Digester water','raw_biogas':'Raw biogas (60/40)','digestate':'Digestate',
 'cleaning_water':'Wash water','clean_biogas':'Cleaned biogas (60/40)','wash_digestate':'Wash digestate',
 'digestate_feed':'Digestate feed','recovered_water':'Recovered water'}
HANDBOOK = {
 'anaerobic_digester':[
  'Six blocks wide, deep and tall: a substantial sealed vessel, insulated shell, access ladder, top mixing drive and feed hopper. Build it from ordinary steel, tanks, a motor and a basic circuit. The Bio-Generator can sustain it after an existing power source commissions the first batch.',
  'Feed 16 wheat, carrots, potatoes, beetroot, rotten flesh, corn, barley, oats, cabbage, tomatoes or sweet potatoes through the left hopper. Supply 1000 mB water at the lower rear right and electricity at the front panel. One batch produces 1000 mB raw 60/40 methane/CO2 biogas and 750 mB digestate. Neither output is duplicated as separated gases.',
  'The upper rear right collar gives raw gas; the lower rear left gives digestate. Both must have room. Two independent lanes, 8000 mB tanks, 400 ticks per lane at 29 JE/t before upgrades. Feedstock is consumed once and saved work survives a reload. Finished products are not vented.'],
 'biogas_cleaner':[
  'Two wide, three deep and three tall. The sealed wash tower and demister clean biogas without splitting its 60/40 composition. Raw gas enters upper rear right; wash water enters lower rear left. Cleaned gas leaves upper front right; dirty wash leaves lower front right. Power enters front left.',
  '1000 mB raw biogas + 250 mB water -> 1000 mB cleaned biogas + 250 mB wash digestate, 100 ticks at 24 JE/t. Route that wash to the Digestate Separator with the digester residue. No disposable filter is needed. Full product tanks stop processing without wasting inputs.'],
 'digestate_separator':[
  'Two wide, three deep and three tall. A closed screw press with guarded drive and collection drawer. Feed digestate at the rear left; recovered water leaves the rear right; fiber leaves the lower front right drawer. Power enters the front left.',
  '1000 mB digestate -> 750 mB reusable water + one Digestate Fiber in 100 ticks at 16 JE/t. Eight fibers craft one existing Fertilizer, usable in the farming and hydroponics systems. Fertilizer and recovered water are never accepted as biomass feed, and water alone cannot produce fuel.'],
 'bio_generator':[
  'Three wide, four deep and three tall. A modest farm engine with a guarded flywheel, generator coupling, radiator and control cabinet. Either the lower right collar or the low rear fuel fitting accepts cleaned biogas or existing bioethanol into the same tank; drain before switching fuels. Electricity leaves the rear left cabinet. Its crafting bill uses an iron gear, distinct from the industrial gas generator.',
  'Both fuels give 192 JE per mB at 64 JE/t, below the industrial Gas Burning Generator. Raw biogas, methane and hydrogen are rejected. Fuel is spent only when electricity fits; fractional fuel credit survives saving.',
  'Optional upgrade: the existing Chemical Infuser and reusable nickel bed combine 1000 mB cleaned biogas with 1600 mB hydrogen into 1000 mB pure methane and 1600 mB recovered water, 600 ticks at 128 JE/t. Its water overflow setting applies. This preserves the original 600 mB methane and converts the 400 mB CO2 rather than throwing it away. Pure methane uses the existing Gas Burning Generator.']}

def write_extras(write,assets,data,lang,condition):
    write(data/'tags/item/digester_feedstock.json',dict(replace=False,values=FEEDSTOCK))
    write(data/'recipe/digestate_fertilizer.json',dict(type='minecraft:crafting_shapeless',category='misc',
       **{'fabric:load_conditions':condition('machines')},ingredients=['jugcraft:digestate_fiber']*8,result=dict(id='jugcraft:fertilizer',count=1)))
    # Owner-supplied bio-fuel surface, kept unchanged, on a small fibrous cake with a paper band.
    from steampunk_models import box
    import model_writer
    out=model_writer.StyleWriter(assets)
    elements=[box((3,3,4),(13,7,12),'bio_fiber_owner'),box((7,2.9,3.9),(9,7.1,12.1),'el_white')]
    out.item_model('digestate_fiber',dict(parent='minecraft:block/block',textures=dict({n:'jugcraft:block/'+n for n in model_writer.texture_names(elements)},particle='jugcraft:block/dp_olive'),elements=model_writer.scaled_elements(elements)))
    out.item('digestate_fiber','jugcraft:item/digestate_fiber')
