"""Connected thermoplastic production. Quantities are provisional game balance."""
from coastal_metals import station
from biorefinery import liquid

FLUIDS={'ethylene_dichloride':liquid('Ethylene Dichloride',[(68,83,65),(110,133,98),(161,182,143),(210,224,194)])}
GASES={'ethylene':dict(display='Ethylene',feature='machines',colors=[(58,89,89),(100,135,135),(158,181,175),(211,229,215)])}
DIES=['sheet','film','profile','hose','insulation']
GRADES={'hdpe':'HDPE','ldpe':'LDPE','pvc':'PVC','rubber':'Rubber'}
ITEMS={'hdpe_pellets':'HDPE Pellets','ldpe_pellets':'LDPE Pellets','ldpe_film':'LDPE Film Roll','pvc_profile':'Extruded PVC Profile',
       'hdpe_process_insert':'HDPE Catalyst Cartridge','ldpe_process_insert':'LDPE Process Insert'}
ITEMS.update({n+'_extrusion_die':n.title()+' Extrusion Die' for n in DIES})
ITEMS.update({n+'_offcut':label+' Clean Offcuts' for n,label in GRADES.items()})
FORMS={
 'monomer_preparation_unit':station('Monomer Preparation Unit',4,3,['monomer_feed','monomer_reagent'],['prepared_monomer','monomer_recovery'],(0,0),'monomer_preparation',['BTB','WCW','MKM']),
 'polymerization_kettle':station('Polymerization Kettle',3,4,['polymer_monomer'],[],(0,1),'graded_polymerization',['TGT','BCB','MKM'],sockets=[('process_insert','jugcraft:polymer_process_inserts')]),
 'polymer_extruder':station('Polymer Extruder',5,3,[],[],(1,2),'polymer_extrusion',['GPG','MCM','WKW'],sockets=[('extrusion_die','jugcraft:polymer_extrusion_dies')]),
 'polymer_reclaimer':station('Clean Polymer Reclaimer',3,3,[],[],(1,1),'polymer_reclaiming',['GPG','MCM','BKB']),
}
def recipe(name,machine,items=(),results=(),fluids=(),outs=(),ticks=160,power=48,tool=None):
 r=dict(name=name,capability=FORMS[machine]['capabilities'][0],items=list(items),results=list(results),fluids=list(fluids),fluid_results=list(outs),ticks=ticks,energy_per_tick=power,features=['machines'])
 if tool:r['tool']='jugcraft:'+tool
 return r
RECIPES={n:[] for n in FORMS}
def add(name,machine,**kw):RECIPES[machine].append(recipe(name,machine,**kw))
add('separate_refinery_ethylene','monomer_preparation_unit',fluids=[('jugcraft:refinery_gas',1000)],outs=[('jugcraft:ethylene',500,0)],ticks=200,power=64)
add('dehydrate_bioethanol_to_ethylene','monomer_preparation_unit',fluids=[('jugcraft:bioethanol',1000)],outs=[('jugcraft:ethylene',500,0),('minecraft:water',250,1)],ticks=240,power=64)
add('prepare_ethylene_dichloride','monomer_preparation_unit',fluids=[('jugcraft:ethylene',500),('jugcraft:chlorine',500)],outs=[('jugcraft:ethylene_dichloride',1000,0)],ticks=160,power=64)
add('crack_ethylene_dichloride','monomer_preparation_unit',fluids=[('jugcraft:ethylene_dichloride',1000)],outs=[('jugcraft:vinyl_chloride',500,0),('jugcraft:hydrogen_chloride',500,1)],ticks=240,power=96)
for grade,ticks,power in [('hdpe',200,64),('ldpe',240,80)]:
 add('polymerize_'+grade,'polymerization_kettle',fluids=[('jugcraft:ethylene',1000)],results=[('jugcraft:'+grade+'_pellets',4)],ticks=ticks,power=power,tool=grade+'_process_insert')
add('kettle_pvc','polymerization_kettle',fluids=[('jugcraft:vinyl_chloride',500)],results=[('jugcraft:pvc_resin',4)],ticks=160,power=64)
add('kettle_rubber','polymerization_kettle',fluids=[('jugcraft:butadiene',500)],results=[('jugcraft:rubber',4)],ticks=160,power=64)
# Four stock units divide into three useful units and one offcut; no full-yield-plus-free-scrap recipes.
EXTRUSIONS=[('sheet','hdpe_pellets','plastic_sheet','hdpe'),('film','ldpe_pellets','ldpe_film','ldpe'),('profile','pvc_resin','pvc_profile','pvc'),('hose','rubber','flexible_hose','rubber'),('insulation','rubber','cable_insulation','rubber')]
for die,feed,product,grade in EXTRUSIONS:
 count=1 if die=='hose' else 3
 add('extrude_'+die,'polymer_extruder',items=[('jugcraft:'+feed,4)],results=[('jugcraft:'+product,count),('jugcraft:'+grade+'_offcut',1)],tool=die+'_extrusion_die')
add('extrude_pvc_sheet','polymer_extruder',items=[('jugcraft:pvc_resin',4)],results=[('jugcraft:plastic_sheet',3),('jugcraft:pvc_offcut',1)],tool='sheet_extrusion_die')
for grade,stock in [('hdpe','hdpe_pellets'),('ldpe','ldpe_pellets'),('pvc','pvc_resin'),('rubber','rubber')]:
 add('reclaim_'+grade+'_offcuts','polymer_reclaimer',items=[('jugcraft:'+grade+'_offcut',4)],results=[('jugcraft:'+stock,1)],ticks=120,power=32)
for product,stock in [('ldpe_film','ldpe_pellets'),('pvc_profile','pvc_resin')]:
 add('reclaim_clean_'+product,'polymer_reclaimer',items=[('jugcraft:'+product,3)],results=[('jugcraft:'+stock,1)],ticks=120,power=32)
TANK_NAMES={'monomer_feed':'Monomer feed','monomer_reagent':'Chlorine reagent','prepared_monomer':'Prepared monomer','monomer_recovery':'Recovered water / hydrogen chloride','polymer_monomer':'Monomer supply'}
HANDBOOK={
 'monomer_preparation_unit':['Refinery gas (1000 mB) yields 500 mB named ethylene. Renewable route: 1000 mB bioethanol yields 500 mB ethylene and 250 mB water, with paid dehydration. All process heat is included in JE use; no manual pressure controls.', '500 mB ethylene + 500 mB chlorine -> 1000 mB ethylene dichloride; then 1000 mB dichloride -> 500 mB vinyl chloride and 500 mB hydrogen chloride gas. Absorb recovered HCl in the existing Chemical Reactor. Empty incompatible tank contents before switching jobs. Both outlets must fit; nothing is silently vented. Earlier PVC routes remain available.'],
 'polymerization_kettle':['1000 mB ethylene makes four HDPE or LDPE pellets according to the reusable insert in the protected socket. HDPE uses a catalyst cartridge; LDPE uses its process insert. These select distinct paid processing recipes; they are not consumed. PVC and rubber use existing vinyl chloride and butadiene respectively and need no insert.', 'HDPE substitutes for ordinary rigid stock in the existing Molding Press, making housings and vehicle panels. LDPE belongs to flexible film, not the rigid-stock tag. The kettle needs only earlier steel, copper and ceramics; HDPE tooling adds existing titanium/magnesium stock.'],
 'polymer_extruder':['Continuous shaping on a supported heated barrel, cooling bed and draw rollers. Install a reusable sheet, film, profile, hose or insulation die. Feed HDPE or existing PVC for sheets, LDPE for film, PVC for profiles, or rubber for hoses/insulation. Tool sockets lock during a batch; automation handles feed and both products through their marked hatches.', 'Each 160-tick batch at 48 JE/t consumes four stock units. It produces three sheets, films, profiles or insulation pieces, or one hose, plus one matching clean offcut. Both output drawers must have room before work starts. The existing Molding Press keeps its six molds and all prior recipes.'],
 'polymer_reclaimer':['Four same-grade clean offcuts return one stock unit for 120 ticks at 32 JE/t. Three unused LDPE films or PVC profiles also return one matching stock. There is no recipe for mixed trash, other grades, placed blocks or assembled machinery. Recycling pays power and loses material every cycle.', 'Film wraps cable insulation and cushions machine housings. PVC profiles brace housings and assemble existing colored-plastic blocks. Sheets, insulation and hoses use all their existing recipes. Look up exact construction uses in JEI.'],
}
for lines in HANDBOOK.values():lines.append('Power: lower front-left. Solid feed: front-right. Finished product: rear-right middle; extrusion offcuts: rear-right upper. Fluid feed: rear middle; fluid products: rear bottom. Pause, Cancel, progress, tank levels, energy, tool locks and upgrade slots use the shared factory screen. Recipes are fictional game quantities.')

def write_extras(write,assets,data,lang,condition):
 import shutil
 from pathlib import Path
 import model_writer
 from steampunk_models import box,cyl
 from starter_chemistry_models import STEEL,CERAMIC,OLIVE,RUBBER
 writer=model_writer.StyleWriter(assets)
 sources=dict(zip(DIES,['mold_plate','mold_unpacking','mold_rod','mold_bullet_casing','mold_wire']))
 for die,source in sources.items():
  target=die+'_extrusion_die'
  shutil.copyfile(Path(__file__).resolve().parents[1]/'art/owner-library/originals/Blocks'/f'{source}.png',assets/'textures/item'/f'{target}.png')
  write(assets/'models/item'/f'{target}.json',dict(parent='minecraft:item/generated',textures={'layer0':'jugcraft:item/'+target}))
 # Native material-kit models preserve texture density without adding painted raster art.
 models={
  'hdpe_pellets':[box((x,2,z),(x+3,5,z+3),CERAMIC) for x,z in [(2,2),(8,3),(4,9),(11,10)]],
  'ldpe_pellets':[box((x,2,z),(x+3,5,z+3),OLIVE) for x,z in [(2,2),(8,3),(4,9),(11,10)]],
  'ldpe_film':cyl('x',8,8,5,2,14,CERAMIC)+[box((2,3,8),(14,4,15),CERAMIC)],
  'pvc_profile':[box((2,2,2),(5,14,14),CERAMIC),box((5,2,2),(13,5,14),CERAMIC)],
  'hdpe_process_insert':[box((3,2,2),(13,12,14),STEEL),box((4,12,3),(12,14,13),CERAMIC)],
  'ldpe_process_insert':[box((3,2,2),(13,12,14),STEEL),box((4,12,3),(12,14,13),OLIVE)],
 }
 for grade,texture in [('hdpe',CERAMIC),('ldpe',OLIVE),('pvc',STEEL),('rubber',RUBBER)]:
  models[grade+'_offcut']=[box((2,2,2),(5,4,13),texture),box((7,2,5),(13,4,8),texture),box((9,2,10),(12,4,14),texture)]
 for name,elements in models.items():writer.item_model(name,dict(parent='minecraft:block/block',textures=dict({t:'jugcraft:block/'+t for t in model_writer.texture_names(elements)},particle='jugcraft:block/dp_olive'),elements=model_writer.scaled_elements(elements)))
 for name,label in ITEMS.items():
  lang['item.jugcraft.'+name]=label
  write(assets/'items'/f'{name}.json',dict(model=dict(type='minecraft:model',model='jugcraft:item/'+name)))
 for tag,values in [('polymer_extrusion_dies',[n+'_extrusion_die' for n in DIES]),('polymer_process_inserts',['hdpe_process_insert','ldpe_process_insert'])]:
  write(data/'tags/item'/f'{tag}.json',dict(replace=False,values=['jugcraft:'+n for n in values]))
 lang['container.jugcraft.form.socket.extrusion_die']='Reusable extrusion die'
 lang['container.jugcraft.form.socket.process_insert']='Polymer process insert'
 write(data/'tags/item/moldable_rigid_polymers.json',dict(replace=False,values=['jugcraft:plastic_pellets','jugcraft:pvc_resin','jugcraft:hdpe_pellets']))
 for die,pattern in zip(DIES,['minecraft:paper','minecraft:string','minecraft:iron_bars','minecraft:bucket','jugcraft:copper_cable']):
  write(data/'recipe'/f'{die}_extrusion_die.json',dict(type='minecraft:crafting_shaped',category='misc',pattern=['P P',' X ','P P'],key={'P':'#c:plates/steel','X':pattern},result={'id':'jugcraft:'+die+'_extrusion_die','count':1},**{'fabric:load_conditions':condition('machines')}))
 uses=[('hdpe_process_insert',['jugcraft:titanium_ingot','jugcraft:magnesium_ingot','minecraft:brick'],'hdpe_process_insert',1),
       ('ldpe_process_insert',['jugcraft:copper_cable','jugcraft:steel_plate','minecraft:brick'],'ldpe_process_insert',1),
       ('film_insulation',['jugcraft:ldpe_film','jugcraft:ldpe_film'],'cable_insulation',1),
       ('film_cushioned_housing',['jugcraft:ldpe_film','jugcraft:plastic_sheet','jugcraft:plastic_sheet','jugcraft:pvc_profile'],'polymer_housing',1),
       ('profile_building_plastic',['jugcraft:pvc_profile','jugcraft:plastic_sheet'],'white_plastic',2)]
 for name,ingredients,result,count in uses:
  gates=condition('machines')+(condition('crude_oil') if result=='white_plastic' else [])
  write(data/'recipe'/f'{name}.json',dict(type='minecraft:crafting_shapeless',category='misc',ingredients=ingredients,result={'id':'jugcraft:'+result,'count':count},**{'fabric:load_conditions':gates}))
