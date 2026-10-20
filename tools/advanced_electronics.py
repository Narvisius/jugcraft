"""Owner package 4: optional wafer specialty and local machine automation.

Abstract game batches, not laboratory procedures. Existing electronics stay available.
"""
from biorefinery import liquid
from coastal_metals import station

ITEMS = {
 'high_purity_silicon': 'High-purity Silicon',
 'high_purity_boule': 'High-purity Silicon Boule',
 'rough_process_wafer': 'Sliced Process Wafer',
 'polished_process_wafer': 'Polished Process Wafer',
 'clean_process_wafer': 'Clean Process Wafer',
 'coated_process_wafer': 'Resist-coated Process Wafer',
 'exposed_process_wafer': 'Exposed Process Wafer',
 'developed_process_wafer': 'Developed Process Wafer',
 'etched_process_wafer': 'Etched Process Wafer',
 'general_chip_pattern': 'Reusable General-chip Pattern Plate',
 'advanced_chip': 'General-purpose Advanced Chip',
 'novolac_resin': 'Novolac Resist Resin',
 'dnq_additive': 'DNQ Photoactive Additive',
 'developer_charge': 'TMAH Developer Charge',
}
FLUIDS = {
 'oxygenated_process_water': liquid('Oxygenated Process Water', [(47,72,78),(76,110,119),(127,159,166),(185,207,211)]),
 'hydrogen_peroxide': liquid('Hydrogen Peroxide', [(52,70,80),(92,123,139),(149,181,193),(206,226,230)]),
 'electronic_sulfuric_acid': liquid('Electronic-grade Sulfuric Acid', [(71,73,51),(117,123,81),(164,171,127),(207,214,176)]),
 'tmah_developer': liquid('TMAH Developer', [(50,72,61),(83,119,99),(133,166,144),(188,210,194)]),
 'semiconductor_photoresist': liquid('Novolac-DNQ Photoresist', [(73,43,28),(125,76,44),(177,120,68),(222,174,114)]),
 'spent_wafer_mixture': liquid('Spent Wafer Mixture', [(50,47,62),(83,78,103),(127,122,148),(174,170,194)]),
}
FORMS = {'wet_processing_bench': station('Wafer Wet-processing Bench',3,3,
    ['wafer_reagent','wafer_rinse'],['wafer_waste'],(1,1),'wafer_processing',
    ['PTP','MCM','WKW'])}
# Earlier supplies only: the first advanced chip never gates its own workshop.

def rec(name, cap, items=(), fluids=(), results=(), fluid_results=(), ticks=200, power=64, tool=None):
    r = dict(name='electronics_'+name, capability='jugcraft:'+cap, items=list(items), fluids=list(fluids),
             results=list(results), fluid_results=list(fluid_results), ticks=ticks, energy_per_tick=power,
             features=['machines','silicon'])
    if tool: r['tool']='jugcraft:'+tool
    return r

RECIPES = {
 'mineral_conditioner': [
    rec('purify_silicon','electronics_conditioning',[('jugcraft:silicon',4)],
        [('jugcraft:hydrochloric_acid',500),('minecraft:water',500)], [('jugcraft:high_purity_silicon',2)], [('jugcraft:spent_wafer_mixture',1000,0)],ticks=320),
    rec('purify_sulfuric','electronics_conditioning',fluids=[('jugcraft:concentrated_sulfuric_acid',1000),('minecraft:water',250)],
        fluid_results=[('jugcraft:electronic_sulfuric_acid',750,0),('jugcraft:spent_wafer_mixture',500,1)],ticks=280),
 ],
 'precision_finisher': [
    rec('slice_boule','precision_finishing',[('jugcraft:high_purity_boule',1)],[('minecraft:water',250)],
        [('jugcraft:rough_process_wafer',8)],[('minecraft:water',250,0)],ticks=320,tool='abrasive_head'),
    rec('polish_wafer','precision_finishing',[('jugcraft:rough_process_wafer',1)],[('minecraft:water',125)],
        [('jugcraft:polished_process_wafer',1)],[('minecraft:water',125,0)],tool='polishing_head'),
 ],
 'bulk_chemical_reactor': [
    rec('oxygenate_water','electronics_reagents',fluids=[('minecraft:water',750),('jugcraft:oxygen',250)],
        fluid_results=[('jugcraft:oxygenated_process_water',1000,0)]),
    rec('novolac','electronics_reagents',[('jugcraft:coke',1)],[('jugcraft:bioethanol',250)], [('jugcraft:novolac_resin',2)],ticks=280),
    rec('dnq','electronics_reagents',[('jugcraft:coke',1)],[('jugcraft:nitric_acid',250)], [('jugcraft:dnq_additive',2)],ticks=280),
    rec('developer_charge','electronics_reagents',[('jugcraft:ammonium_nitrate',1)],[('jugcraft:bioethanol',250),('jugcraft:lye',250)], [('jugcraft:developer_charge',2)]),
    rec('developer','electronics_reagents',[('jugcraft:developer_charge',1)],[('minecraft:water',1000)],fluid_results=[('jugcraft:tmah_developer',1000,0)]),
 ],
 'electrolytic_separator': [
    rec('peroxide','electronics_peroxide',fluids=[('jugcraft:oxygenated_process_water',1000)],
        fluid_results=[('jugcraft:hydrogen_peroxide',250,0),('minecraft:water',750,1)],ticks=320,power=96),
 ],
 'wet_processing_bench': [],
}
# Compact two-solid reactor blends resin and the light-sensitive additive.
REACTOR_RECIPES = [rec('photoresist','unused',[('jugcraft:novolac_resin',1),('jugcraft:dnq_additive',1)],
    [('jugcraft:bioethanol',500)],fluid_results=[('jugcraft:semiconductor_photoresist',500,0)],ticks=240)]
for r in REACTOR_RECIPES: r.pop('capability')
for name, feed, product, fluids in [
 ('clean_wafer','polished_process_wafer','clean_process_wafer',[('jugcraft:electronic_sulfuric_acid',125),('jugcraft:hydrogen_peroxide',125)]),
 ('coat_wafer','clean_process_wafer','coated_process_wafer',[('jugcraft:semiconductor_photoresist',125)]),
 ('develop_wafer','exposed_process_wafer','developed_process_wafer',[('jugcraft:tmah_developer',125),('minecraft:water',125)]),
 ('etch_wafer','developed_process_wafer','etched_process_wafer',[('jugcraft:hydrogen_fluoride',125),('minecraft:water',125)]),
]:
    RECIPES['wet_processing_bench'].append(rec(name,'wafer_processing',[('jugcraft:'+feed,1)],fluids,
        [('jugcraft:'+product,1)],[('jugcraft:spent_wafer_mixture',sum(mb for _,mb in fluids),0)],ticks=160))
LITHOGRAPHY_RECIPES = [rec('expose_wafer','unused',[('jugcraft:coated_process_wafer',1)],
    results=[('jugcraft:exposed_process_wafer',1)],tool='general_chip_pattern')]
for r in LITHOGRAPHY_RECIPES: r.pop('capability')
MACHINE_RECIPES = {
 'arc_furnace': [dict(name='electronics_pull_boule', inputs=[('jugcraft:high_purity_silicon',4),('jugcraft:phosphate',1)],
    output='jugcraft:high_purity_boule',count=1,ticks=480,features=['machines','silicon','phosphate'])],
 'circuit_assembler': [dict(name='electronics_bond_chips',inputs=[('jugcraft:etched_process_wafer',1),('jugcraft:copper_wire',2),('jugcraft:solder_ingot',1)],
    output='jugcraft:advanced_chip',count=4,ticks=280,features=['machines','silicon','lead'])],
}
TANK_NAMES = {'wafer_reagent':'Process reagent','wafer_rinse':'Rinse / second reagent','wafer_waste':'Spent wafer mixture'}
HANDBOOK = {'wet_processing_bench': [
 'One shared 3 x 3 x 3 bench cleans, coats, develops and etches prepared wafers. The separate existing Lithography Station exposes the coated wafer between coating and developing. Use Recipes and Lock recipe to select a stage. Empty unused input tanks when changing jobs.',
 'Front lower-left power; front-right wafer drawer; rear-left reagent and rear-middle rinse at middle height; rear-left low waste outlet; rear-right finished wafer drawer. All fluid services connect to the contained wash tray and sump. There are no unused decorative pipe ends.',
 'Every wet job reserves space for its spent mixture before starting. Store and transport this mixture in existing tanks; recovery belongs to package 6. No automatic vent, free drain or recovery loop is introduced.',
]}
GUIDE_PAGES = [
 dict(title='From silicon to a polished wafer',icon='jugcraft:high_purity_boule',text=[
  'Existing silicon, wafer, microchip and circuit routes remain available. For the optional advanced line, condition four Silicon with 500 mB hydrochloric acid and 500 mB water into two High-purity Silicon and 1000 mB spent mixture. Water conditioning is internal; there is no new purified-water currency.',
  'Pull four high-purity silicon with one Phosphate in the Arc Furnace into one boule. The Precision Finisher slices it into eight process wafers with a reusable Diamond Abrasive Head and 250 mB returned cooling water. Change to the reusable Felt Polishing Head to polish each wafer with 125 mB returned water.']),
 dict(title='Wafer reagents and waste',icon='jugcraft:semiconductor_photoresist_bucket',text=[
  'The Mineral Conditioner purifies sulfuric acid. The Bulk Reactor combines oxygen and water into process feed; the powered Electrolytic Separator turns it into peroxide and returned water. This is its own paid process, separate from ordinary water electrolysis.',
  'Abstract game recipes use Coke and bioethanol for Novolac Resin, Coke and nitric acid for DNQ Additive, and ammonium nitrate, bioethanol and lye for Developer Charges. Dissolve the charges with water in the Bulk Reactor; blend Novolac and DNQ with bioethanol in the compact Chemical Reactor for photoresist. These doses simplify industrial chemistry for gameplay.',
  'The bench cleans with electronic-grade sulfuric acid and peroxide, coats with photoresist, develops exposed wafers with TMAH and water, then etches with HF and water. Each step outputs spent mixture. Store it for the later recovery chain; a full waste outlet stops work.']),
 dict(title='Exposure and useful chips',icon='jugcraft:advanced_chip',text=[
  'Craft a General-chip Pattern Plate from earlier glass lenses, gold ingot and a basic circuit. Install it manually in the new upper pattern slot of the existing Lithography Station. It is retained without wear. The station exposes a coated wafer into an exposed wafer; old acid-etch microchip recipes do not need a plate.',
  'After developing and etching, the Circuit Assembler bonds each etched wafer with two copper wire and one solder ingot into four General-purpose Advanced Chips. Assemble those with precision parts for two Speed Upgrades, ceramic insulation and wire for two Efficiency Upgrades, or a basic circuit and polymer housing for one Automation Card.',
  'Speed and Efficiency keep their existing four-card caps and effects. Automation works in powered formed machines with upgrade slots; compact machines keep their existing controls. It never remotely operates another machine.']),
 dict(title='Recipe priorities and stock targets',icon='jugcraft:automation_upgrade',text=[
  'Install an Automation Card in a formed machine upgrade slot, open Recipes, select a job and use Priority or Stock. Up to eight jobs per machine may have non-default settings. Priority 0 is normal; 1, 2 and 3 run first when their inputs, tool and output room are available. Recipe Lock still restricts work to the locked job.',
  'Stock limits count the first item output, or first fluid output for fluid-only recipes, inside this machine plus matching outputs already promised by running lanes. They do not count remote chests or tanks. A batch starts only if its entire matching output fits below the target. Exporting products makes room to resume.',
  'Item targets cycle Off, 16, 32, 64; fluid targets Off, 250, 500, 1000, 2000 mB. Choose a target at least as large as one batch. Running batches finish unchanged when settings change. Removing the card disables its rules but preserves them for reinsertion and world reload. Reset both settings to release a rule slot.']),
]

def write_extras(write, assets, data, lang, condition):
    for name, display in {**ITEMS,'automation_upgrade':'Automation Card'}.items():
        lang['item.jugcraft.'+name]=display
        write(assets/'models/item'/f'{name}.json',dict(parent='minecraft:item/generated',textures={'layer0':'jugcraft:item/'+name}))
        write(assets/'items'/f'{name}.json',dict(model=dict(type='minecraft:model',model='jugcraft:item/'+name)))
    crafts = {
      'pattern': (['LGL',' C '],dict(L='jugcraft:polished_lens',G='minecraft:gold_ingot',C='jugcraft:basic_circuit'),'general_chip_pattern',1),
      'speed': ([' BP','SCS'],dict(B='jugcraft:precision_bearing',P='jugcraft:precision_shaft',S='#c:plates/steel',C='jugcraft:advanced_chip'),'speed_upgrade',2),
      'efficiency': (['AWA',' C '],dict(A='jugcraft:alumina_plate',W='jugcraft:copper_wire',C='jugcraft:advanced_chip'),'efficiency_upgrade',2),
      'automation': ([' C ','BHB'],dict(C='jugcraft:advanced_chip',B='jugcraft:basic_circuit',H='jugcraft:polymer_housing'),'automation_upgrade',1),
    }
    for name,(pattern,key,result,count) in crafts.items():
        write(data/'recipe'/f'electronics_{name}.json',{'fabric:load_conditions':condition('machines')+condition('silicon'),
            'type':'minecraft:crafting_shaped','category':'misc','pattern':pattern,'key':key,'result':dict(id='jugcraft:'+result,count=count)})
    words = {'automation_priority':'Priority: %s','automation_stock':'Stock: %s',
      'automation_reset':'Reset rules', 'automation_reset_help':'Clear all saved priorities and stock targets, including removed datapack recipes. Running batches and Recipe Lock are unchanged.',
      'automation_off':'Off','automation_units':'%s items','automation_mb':'%s mB',
      'automation_help':'Requires an Automation Card. At most 8 customized recipes; reset both values to free one. Lock wins over priority. Stock counts local outputs and running batches, not external storage.',
      'automation_summary':'Automation: priority %s; stock %s. First output only; running batches are preserved.',
      'automation_missing':'Install an Automation Card to enable saved priorities and stock targets.'}
    lang.update({'container.jugcraft.form.'+k:v for k,v in words.items()})
    lang['container.jugcraft.form.reason.stock_target']='Local stock target reached'
    lang['container.jugcraft.pattern']='Reusable pattern plate'

def draw_all(save):
    import item_icons
    for name in [*ITEMS,'automation_upgrade']: save(item_icons.draw(name),'item',name)
