"""Fluorite -> HF -> fluoromonomer -> PTFE. Fictional, paid game batches."""
from coastal_metals import station
from biorefinery import liquid

FLUIDS = {
 'concentrated_sulfuric_acid': liquid('Concentrated Sulfuric Acid', [(72,68,32),(117,110,51),(168,158,91),(211,203,153)]),
 'chloroform': liquid('Chloroform', [(51,72,66),(86,119,105),(138,163,142),(203,219,196)]),
}
GASES = {n:dict(display=label,feature='machines',colors=colors) for n,label,colors in [
 ('hydrogen_fluoride','Hydrogen Fluoride',[(45,75,68),(78,123,103),(133,171,146),(199,220,194)]),
 ('chlorodifluoromethane','Chlorodifluoromethane',[(55,66,88),(89,112,141),(141,166,184),(204,225,226)]),
 ('tetrafluoroethylene','Tetrafluoroethylene',[(62,78,83),(105,128,138),(161,183,188),(221,231,227)]),
]}
ITEMS = {
 'fluorite_concentrate':'Fluorite Concentrate','calcium_sulfate':'Calcium Sulfate',
 'ptfe_powder':'PTFE Powder','ptfe_process_insert':'PTFE Process Insert','ptfe_paste_charge':'PTFE Extrusion Paste',
 'ptfe_seal_blank':'Unsintered PTFE Seal','ptfe_fitting_blank':'Unsintered PTFE Fitting','ptfe_liner_blank':'Unsintered PTFE Liner',
 'ptfe_seal':'PTFE Seal','ptfe_fitting':'PTFE Pipe Fitting','ptfe_liner':'PTFE Reactor Liner',
 'ptfe_offcut':'Clean Unsintered PTFE Offcuts',
 'gypsum_binder':'Prepared Gypsum Binder',
}
FORMS = {
 'acid_concentrator':station('Acid Concentrator',3,4,['dilute_acid'],['concentrated_acid','condensate'],(0,0),'acid_concentration',['TBT','WCW','BMB']),
 'chemical_dissolution_chamber':station('Chemical Dissolution Chamber',4,3,['dissolution_acid'],['fluoride_gas'],(1,1),'fluoride_dissolution',['BTB','PMP','WCK']),
 'fluorocarbon_reactor':station('Fluorocarbon Reactor',4,4,['carbon_feed','halogen_reagent'],['fluorocarbon_product','acid_gas_recovery'],(0,0),'fluorocarbon_synthesis',['PTB','WCM','BKT']),
 'fluoromonomer_cracker':station('Fluoromonomer Cracker',4,3,['fluorocarbon_feed'],['fluoromonomer','acid_gas_recovery'],(0,0),'fluoromonomer_cracking',['BPB','WGW','MCT']),
 'ptfe_sintering_oven':station('PTFE Sintering Oven',4,3,[],[],(1,1),'ptfe_sintering',['BGB','WCW','PKP']),
 'lined_fluorochemical_reactor':station('Lined Fluorochemical Reactor',5,5,['carbon_feed','halogen_reagent'],['fluorocarbon_product','acid_gas_recovery'],(0,0),'fluorocarbon_synthesis',['PTP','TCT','MKM']),
}
# The starter path is independent of its own products. Only the expanded upgrade consumes finished PTFE.
FORMS['lined_fluorochemical_reactor'].update(profile='EXPANDED',tank_mb=8000)
FORMS['lined_fluorochemical_reactor']['recipe']=(['SFL','TCT','MKM'],dict(S='jugcraft:ptfe_seal',F='jugcraft:ptfe_fitting',L='jugcraft:ptfe_liner',T='jugcraft:titanium_ingot',C='jugcraft:basic_circuit',M='jugcraft:electric_motor',K='jugcraft:machine_casing'))
RECIPES={n:[] for n in FORMS}
EXTRA_RECIPES={n:[] for n in ['mineral_conditioner','polymerization_kettle','polymer_molding_press','polymer_extruder','polymer_reclaimer']}
CAPS=dict(mineral_conditioner='mineral_conditioning',polymerization_kettle='graded_polymerization',polymer_molding_press='polymer_molding',polymer_extruder='polymer_extrusion',polymer_reclaimer='polymer_reclaiming')
def add(name,machine,items=(),results=(),fluids=(),outs=(),ticks=200,power=64,tool=None,source=0):
 cap=FORMS[machine]['capabilities'][0] if machine in FORMS else 'jugcraft:'+CAPS[machine]
 r=dict(name=name,capability=cap,items=list(items),results=list(results),fluids=list(fluids),fluid_results=list(outs),ticks=ticks,energy_per_tick=power,features=['machines'])
 if tool:r['tool']='jugcraft:'+tool
 if source:r['source']=source
 (RECIPES if machine in FORMS else EXTRA_RECIPES)[machine].append(r)

add('concentrate_sulfuric_acid','acid_concentrator',fluids=[('jugcraft:sulfuric_acid',1000)],outs=[('jugcraft:concentrated_sulfuric_acid',750,0),('minecraft:water',250,1)],ticks=240)
add('condition_fluorite','mineral_conditioner',items=[('jugcraft:fluorite',1)],fluids=[('minecraft:water',250)],results=[('jugcraft:fluorite_concentrate',1)],ticks=120,power=32)
add('prepare_gypsum_binder','mineral_conditioner',items=[('jugcraft:calcium_sulfate',2)],fluids=[('minecraft:water',250)],results=[('jugcraft:gypsum_binder',1)],ticks=160,power=48)
add('dissolve_fluorite','chemical_dissolution_chamber',items=[('jugcraft:fluorite_concentrate',1)],fluids=[('jugcraft:concentrated_sulfuric_acid',750)],outs=[('jugcraft:hydrogen_fluoride',500,0)],results=[('jugcraft:calcium_sulfate',1)],ticks=240,power=80)
add('prepare_chloroform','fluorocarbon_reactor',fluids=[('jugcraft:methane',500),('jugcraft:chlorine',1500)],outs=[('jugcraft:chloroform',500,0),('jugcraft:hydrogen_chloride',1500,1)],ticks=240)
add('fluorinate_chloroform','fluorocarbon_reactor',fluids=[('jugcraft:chloroform',500),('jugcraft:hydrogen_fluoride',1000)],outs=[('jugcraft:chlorodifluoromethane',500,0),('jugcraft:hydrogen_chloride',1000,1)],ticks=240,power=80)
add('crack_fluoromonomer','fluoromonomer_cracker',fluids=[('jugcraft:chlorodifluoromethane',1000)],outs=[('jugcraft:tetrafluoroethylene',500,0),('jugcraft:hydrogen_chloride',500,1)],ticks=240,power=96)
add('polymerize_ptfe','polymerization_kettle',fluids=[('jugcraft:tetrafluoroethylene',1000)],results=[('jugcraft:ptfe_powder',4)],tool='ptfe_process_insert',ticks=240,power=80)
for part,mold,count in [('seal','gasket',1),('fitting','fitting',2),('liner','panel',3)]:
 add('press_ptfe_'+part,'polymer_molding_press',items=[('jugcraft:ptfe_powder',count)],results=[('jugcraft:ptfe_'+part+'_blank',1)],tool=mold+'_mold',ticks=160,power=48)
 add('sinter_ptfe_'+part,'ptfe_sintering_oven',items=[('jugcraft:ptfe_'+part+'_blank',1)],results=[('jugcraft:ptfe_'+part,1)],ticks=240,power=80)
add('prepare_ptfe_paste','mineral_conditioner',items=[('jugcraft:ptfe_powder',4)],fluids=[('jugcraft:lubricant',250)],results=[('jugcraft:ptfe_paste_charge',4)],ticks=120,power=32)
add('extrude_ptfe_liner','polymer_extruder',items=[('jugcraft:ptfe_paste_charge',4)],results=[('jugcraft:ptfe_liner_blank',1),('jugcraft:ptfe_offcut',1)],tool='sheet_extrusion_die',ticks=200,power=64)
add('reclaim_unsintered_ptfe','polymer_reclaimer',items=[('jugcraft:ptfe_offcut',4)],results=[('jugcraft:ptfe_powder',1)],ticks=160,power=48)
# Same recipe capability, no duplicate recipe files; the larger form loads the starter reactor's two receipts.
TANK_NAMES={k:k.replace('_',' ').capitalize() for f in FORMS.values() for k in f['input_tanks']+f['output_tanks']}
HANDBOOK={
 'acid_concentrator':['Separate 1000 mB ordinary sulfuric acid into 750 mB concentrate and 250 mB recovered water. Both outlets must fit. Reuse the existing starter acid factory; this is paid concentration, not a second free acid source.'],
 'chemical_dissolution_chamber':['Mine fluorite in snowy regions, including occasional exposed high mountain veins. The Mineral Conditioning Station washes one fluorite with 250 mB water into concentrate. Concentrate plus 750 mB concentrated sulfuric acid yields 500 mB hydrogen fluoride gas and one calcium sulfate. The washed-out water is consumed; these are game units, not chemical instructions.', 'Optional residue recovery: two calcium sulfate + 250 mB water in the Conditioner yields one Prepared Gypsum Binder, paying 160 ticks at 48 JE/t with a material loss. One binder + two sand + gravel crafts two white concrete powder as game construction filler. The chamber is ceramic-lined starter equipment and needs no PTFE. Retain the gas in tanks or cylinders; this machine has no gas vent.'],
 'fluorocarbon_reactor':['500 mB methane + 1500 mB chlorine -> 500 mB chloroform + 1500 mB hydrogen chloride. Then 500 mB chloroform + 1000 mB hydrogen fluoride -> 500 mB chlorodifluoromethane + 1000 mB hydrogen chloride. Empty incompatible feeds/products before changing jobs; reserve room for every output.', 'Recovered hydrogen chloride goes to the existing Chemical Reactor absorption recipe. Methane can come from the biogas chain; chlorine comes from electrolysis or the magnesium line. No elemental fluorine is required for this route.'],
 'fluoromonomer_cracker':['1000 mB chlorodifluoromethane -> 500 mB tetrafluoroethylene (TFE) + 500 mB hydrogen chloride. Work and heat are paid by JE. Feed 1000 mB TFE to the Polymerization Kettle with its reusable PTFE Process Insert to make four PTFE powder.'],
 'ptfe_sintering_oven':['PTFE is shaped before sintering. Use the Molding Press gasket, fitting or panel mold with 1, 2 or 3 powder respectively to make one unsintered seal, fitting or liner. The oven finishes each blank for 240 ticks at 80 JE/t.', 'Alternative liner route: mix four powder with 250 mB lubricant in the Mineral Conditioner, yielding four paste charges. The Extruder sheet die consumes four charges for one liner blank plus one clean unsintered offcut. Four offcuts return one powder in the Reclaimer, paying energy and losing material. Finished PTFE parts are not melt-recycled or substituted for generic plastic.'],
 'lined_fluorochemical_reactor':['Build with one finished PTFE seal, fitting and liner, two titanium ingots, two motors, a basic circuit and machine casing. This upgrade runs the same two fluorocarbon recipes in two independent lanes with 8000 mB tanks and 90% energy per lane, rounded up each tick. The starter reactor can produce every input to this upgrade.'],
}
for lines in HANDBOOK.values():lines.append('Shared factory controls: Pause, Cancel, progress, energy, tanks and upgrades. Power at lower front-left; solid feed front-right and product rear-right; fluid feeds rear-middle and products rear-bottom. Processing is server-authoritative; no fluid is silently vented. Consult JEI for exact quantities and the encyclopedia for the connected route.')

def write_extras(write,assets,data,lang,condition):
 import json
 import model_writer
 from steampunk_models import box,cyl
 from starter_chemistry_models import STEEL,CERAMIC,OLIVE,CHROME
 writer=model_writer.StyleWriter(assets)
 models={}
 for name,texture in [('fluorite_concentrate',OLIVE),('calcium_sulfate',CERAMIC),('gypsum_binder',CERAMIC),('ptfe_powder',CERAMIC),('ptfe_paste_charge',CHROME)]:
  models[name]=[box((x,2,z),(x+4,2+h,z+4),texture) for x,z,h in [(2,2,3),(8,3,4),(4,8,5),(10,10,2)]]
 models['ptfe_process_insert']=[box((3,2,2),(13,12,14),STEEL),box((4,12,3),(12,14,13),CERAMIC),box((6,13,5),(10,15,11),OLIVE)]
 for suffix,texture in [('_blank',OLIVE),('',CERAMIC)]:
  models['ptfe_seal'+suffix]=[box(a,b,texture) for a,b in [((2,2,2),(14,4,5)),((2,2,11),(14,4,14)),((2,2,5),(5,4,11)),((11,2,5),(14,4,11))]]
  models['ptfe_fitting'+suffix]=cyl('z',8,8,4,2,12,texture)+cyl('y',8,10,4,8,14,texture)
  models['ptfe_liner'+suffix]=[box((2,2,2),(14,3,14),texture),box((2,3,2),(4,8,14),texture),box((12,3,2),(14,8,14),texture)]
 models['ptfe_offcut']=[box((2,2,2),(5,4,13),OLIVE),box((7,2,5),(13,4,8),OLIVE)]
 for name,elements in models.items():
  writer.item_model(name,dict(parent='minecraft:block/block',textures=dict({t:'jugcraft:block/'+t for t in model_writer.texture_names(elements)},particle='jugcraft:block/dp_olive'),elements=model_writer.scaled_elements(elements)))
  write(assets/'items'/f'{name}.json',dict(model=dict(type='minecraft:model',model='jugcraft:item/'+name)))
  lang['item.jugcraft.'+name]=ITEMS[name]
 p=data/'tags/item/polymer_process_inserts.json'
 tag=json.loads(p.read_text());tag['values'].append('jugcraft:ptfe_process_insert');write(p,tag)
 for name,ingredients,result,count in [
  ('ptfe_process_insert',['jugcraft:nickel_ingot','jugcraft:alumina','minecraft:brick'],'jugcraft:ptfe_process_insert',1),
  ('gypsum_bound_concrete',['jugcraft:gypsum_binder','minecraft:sand','minecraft:sand','minecraft:gravel'],'minecraft:white_concrete_powder',2)]:
  write(data/'recipe'/f'{name}.json',dict(type='minecraft:crafting_shapeless',category='misc',ingredients=ingredients,result=dict(id=result,count=count),**{'fabric:load_conditions':condition('machines')}))
