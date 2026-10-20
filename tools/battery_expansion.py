"""Owner package 3. Abstract paid game chemistry, existing stations and saved stocks."""
ITEMS = {
    'galena_concentrate': 'Galena Concentrate',
    'lead_electrode': 'Lead Electrode Grid',
    'lead_oxide_electrode': 'Lead Oxide Electrode',
    'battery_separator': 'Polymer Battery Separator',
    'lead_acid_cell': 'Sealed Lead-acid Cell',
    'activated_spodumene': 'Activated Spodumene',
    'lithium_fluoride': 'Lithium Fluoride',
    'fluorophosphate_precursor': 'Fluorophosphate Precursor',
    'lipf6_salt': 'Lithium Hexafluorophosphate Salt',
    'carbonate_solvent_charge': 'Dry Carbonate Solvent Charge',
    'lithium_electrolyte_charge': 'Formulated Lithium Electrolyte Charge',
    'lithium_cathode': 'Lithium Iron Phosphate Cathode',
    'carbon_anode': 'Carbon Anode',
    'vanadyl_sulfate': 'Vanadyl Sulfate Charge',
    'flow_cell_stack': 'Flow Battery Cell Stack',
}
BLOCKS = {
    'lead_acid_battery_bank': 'Lead-acid Battery Bank',
    'flow_tank_module': 'Paired Flow Tank Module',
    'flow_stack_module': 'Flow Cell-stack Module',
}

def recipe(name, items=(), fluids=(), results=(), fluid_results=(), ticks=200, features=()):
    return dict(name='battery_' + name, capability='jugcraft:battery_preparation', items=list(items),
                fluids=list(fluids), results=list(results), fluid_results=list(fluid_results),
                ticks=ticks, energy_per_tick=64, features=['machines', *features])

# No new chemical machines. The existing shared Conditioner/Oven/Bulk Reactor expose
# this additional capability without changing inventories, tank order or saved ports.
RECIPES = {
 'mineral_conditioner': [
    recipe('galena', [('jugcraft:raw_lead', 2)], [('minecraft:water', 250)], [('jugcraft:galena_concentrate', 2)], features=['lead']),
    recipe('spodumene_carbonate', [('jugcraft:activated_spodumene', 2)], [('jugcraft:sulfuric_acid', 500)], [('jugcraft:lithium_carbonate', 2)], features=['lithium','sulfur']),
 ],
 'alumina_calciner': [
    recipe('galena_lead', [('jugcraft:galena_concentrate', 2), ('jugcraft:coke', 1)], results=[('jugcraft:lead_ingot', 2)], ticks=240, features=['lead']),
    recipe('activate_spodumene', [('jugcraft:spodumene', 1)], results=[('jugcraft:activated_spodumene', 1)], ticks=240, features=['lithium']),
 ],
 'bulk_chemical_reactor': [
    recipe('lead_oxide_electrode', [('jugcraft:lead_electrode', 1)], [('jugcraft:oxygen', 250)], [('jugcraft:lead_oxide_electrode', 1)], features=['lead']),
    recipe('lead_acid_cell', [('jugcraft:lead_electrode', 1), ('jugcraft:lead_oxide_electrode', 1), ('jugcraft:battery_separator', 1)], [('jugcraft:sulfuric_acid', 250)], [('jugcraft:lead_acid_cell', 1)], features=['lead','sulfur']),
    recipe('lithium_fluoride', [('jugcraft:lithium_carbonate', 1)], [('jugcraft:hydrogen_fluoride', 250)], [('jugcraft:lithium_fluoride', 1)], features=['lithium']),
    recipe('fluorophosphate', [('jugcraft:phosphate', 1)], [('jugcraft:hydrogen_fluoride', 500)], [('jugcraft:fluorophosphate_precursor', 1)], features=['phosphate']),
    recipe('lipf6', [('jugcraft:lithium_fluoride', 1), ('jugcraft:fluorophosphate_precursor', 1)], results=[('jugcraft:lipf6_salt', 1)], ticks=240, features=['lithium','phosphate']),
    recipe('carbonate_solvent', fluids=[('jugcraft:bioethanol', 500), ('jugcraft:carbon_dioxide', 250)], results=[('jugcraft:carbonate_solvent_charge', 2)], ticks=240),
    recipe('lithium_electrolyte', [('jugcraft:lipf6_salt', 1), ('jugcraft:carbonate_solvent_charge', 2)], results=[('jugcraft:lithium_electrolyte_charge', 2)], features=['lithium']),
    recipe('lfp_cathode', [('jugcraft:lithium_carbonate', 1), ('minecraft:iron_ingot', 1), ('jugcraft:phosphate', 1)], results=[('jugcraft:lithium_cathode', 2)], ticks=240, features=['lithium','phosphate']),
    recipe('vanadyl_sulfate', [('jugcraft:vanadium_oxide', 1), ('jugcraft:coke', 1)], [('jugcraft:sulfuric_acid', 500)], [('jugcraft:vanadyl_sulfate', 1)], features=['sulfur']),
    recipe('pentoxide_sulfate', [('jugcraft:vanadium_pentoxide', 1), ('jugcraft:coke', 1)], [('jugcraft:sulfuric_acid', 500)], [('jugcraft:vanadyl_sulfate', 1)], features=['sulfur']),
    recipe('formulated_vanadium', [('jugcraft:vanadyl_sulfate', 1)], [('jugcraft:sulfuric_acid', 500), ('minecraft:water', 500)], fluid_results=[('jugcraft:vanadium_electrolyte', 1000, 0)], features=['sulfur']),
 ],
}
MACHINE_RECIPES = {
 'metal_press': [dict(name='battery_lead_grid', input='jugcraft:lead_plate', output='jugcraft:lead_electrode', count=1, ticks=120, features=['machines','lead']),
                 dict(name='battery_separator', input='jugcraft:plastic_sheet', output='jugcraft:battery_separator', count=4, ticks=120, features=['machines']),
                 dict(name='battery_carbon_anode', input='jugcraft:coke', output='jugcraft:carbon_anode', count=1, ticks=120, features=['machines'])],
 'circuit_assembler': [dict(name='battery_lithium_cell', inputs=[('jugcraft:lithium_cell_assembly', 1), ('jugcraft:lithium_electrolyte_charge', 1)], output='jugcraft:lithium_cell', count=1, ticks=240, features=['machines','lithium'])],
}
ITEMS['lithium_cell_assembly'] = 'Dry Lithium Cell Assembly'
ITEMS['dry_lead_cell'] = 'Dry Lead-acid Cell Assembly'
ITEMS['lfp_cathode_mix'] = 'Lithium Iron Phosphate Cathode Mix'
# Respect the released one-item form hatches. Multi-solid jobs use the existing
# two-input Chemical Reactor; three-solid assemblies are prepared at a crafting table.
RECIPES['alumina_calciner'] = [r for r in RECIPES['alumina_calciner'] if r['name'] != 'battery_galena_lead']
MACHINE_RECIPES['arc_furnace'] = [dict(name='battery_galena_lead', inputs=[('jugcraft:galena_concentrate',2),('jugcraft:coke',1)], output='jugcraft:lead_ingot', count=2, ticks=240, features=['machines','lead'])]
for r in RECIPES['bulk_chemical_reactor']:
    if r['name'] == 'battery_lead_acid_cell':
        r['items'] = [('jugcraft:dry_lead_cell',1)]
    if r['name'] == 'battery_lfp_cathode':
        r['items'] = [('jugcraft:lfp_cathode_mix',1)]
REACTOR_RECIPES = [r for r in RECIPES['bulk_chemical_reactor'] if len(r['items']) > 1]
RECIPES['bulk_chemical_reactor'] = [r for r in RECIPES['bulk_chemical_reactor'] if len(r['items']) <= 1]

for station, recipes in RECIPES.items():
    for entry in recipes:
        entry['capability'] = 'jugcraft:battery_' + station

GUIDE_PAGES = [
 dict(title='Lead-acid workshop storage', icon='jugcraft:lead_acid_battery_bank', text=[
  'An early bank stores 2,000,000 JE and transfers up to 1,024 JE/t in each direction. It starts empty. Existing Battery Boxes and Capacitor Banks remain unchanged and all original lithium recipes remain available.',
  'Existing raw lead represents galena feed. Condition two raw lead with 250 mB water, then reduce two concentrate with one Coke in the Arc Furnace into two Lead Ingots. Old lead refining still works. Press lead plates to grids; oxidize one grid with 250 mB oxygen in the Bulk Chemical Reactor.',
  'Press one Plastic Sheet into four separators. Craft a lead grid, an oxide electrode and a separator into a dry cell, then fill it with 250 mB sulfuric acid in the Bulk Chemical Reactor. Craft four filled cells with steel plates, copper wire and a basic circuit into a bank. Acid is incorporated once; there is no cycle-aging upkeep.']),
 dict(title='Expanded lithium cells', icon='jugcraft:lithium_cell', text=[
  'Fire Spodumene in the Industrial Oven, then condition two activated mineral with 500 mB sulfuric acid for two Lithium Carbonate. Existing lepidolite and lithium recipes remain available.',
  'In the Bulk Chemical Reactor prepare Lithium Fluoride from carbonate and HF, and a fluorophosphate precursor from phosphate and HF. Combine both in the two-input Chemical Reactor to LiPF6 salt. Separately make Dry Carbonate Solvent Charges from bioethanol and carbon dioxide in the Bulk Reactor. One salt plus two solvent charges in the Chemical Reactor yields two formulated electrolyte charges. These sealed process doses are abstract game batches, not buckets or real laboratory instructions.',
  'Craft lithium carbonate, iron and phosphate into a Cathode Mix, then process it in the Bulk Reactor for two Lithium Iron Phosphate Cathodes. Press Coke to Carbon Anodes and plastic sheets to separators. Craft cathode, anode, separator and aluminum plate into a dry cell assembly, then fill it with one electrolyte charge in the Circuit Assembler. It produces the existing Lithium Cell, for the existing 32,000,000 JE / 16,384 JE/t bank.']),
 dict(title='Paired flow modules and mineral electrolyte', icon='jugcraft:flow_tank_module', text=[
  'Existing vanadium-bearing iron concentrate and its roasted Vanadium Oxide now feed Vanadyl Sulfate with Coke and sulfuric acid in the two-input Chemical Reactor; the existing Pentoxide is an alternative. Formulate one sulfate charge with 500 mB acid and 500 mB water in the Bulk Reactor into 1,000 mB of the existing Vanadium Electrolyte. Refinery-residue electrolyte stays available.',
  'A Flow Battery still begins with two internal sides totaling 64 buckets / 64,000,000 JE. Place up to four paired tank modules in a straight row immediately behind the middle bottom block, facing the same way as the controller. Each paired module adds 64 buckets, equally split between both sides. Maximum: 320 buckets / 320,000,000 JE. Fill through the controller; modules never own a second copy of fluid or energy.',
  'Place one Cell-stack Module directly above the middle front top block to raise both input and output from 8,192 to 16,384 JE/t. Capacity and rate are separate upgrades. Attached joints align with the module manifolds; modules need no standalone cables or fluid pipes.',
  'Removing or unloading required tank modules suspends transfers when stored electrolyte exceeds connected capacity, preserving fluid and charge until the modules return. Breaking the controller keeps electrolyte but loses charge, as before. Detached modules are empty. No electrolyte is generated or consumed by charging.']),
 dict(title='Local battery controls', icon='jugcraft:battery_box', text=[
  'Open any battery to select Automatic, Charge only, Discharge only or Off. Reserve changes in 10% steps; discharge stops at that reserve and resumes after charging five percentage points above it. At 0% reserve the original automatic behavior remains.',
  'Charge priority chooses Low, Normal or High on the local cable network; higher priority is offered power first, with unused power falling through to the next group. This does not schedule generators or remotely command other grids. Output limit cycles 100%, 50% and 25%. Tick budgets are shared across every face and part.',
  'All controls are server-authoritative, saved with the placed bank, and shown on its screen. Batteries start empty; energy transfer remains lossless for compatibility, subject to finite storage, cable limits, mode, reserve and rate. No charging cycles create energy.']),
]

def write_extras(write, assets, data, lang, condition):
    words = {
      'mode':'Mode: %s', 'mode.0':'Automatic', 'mode.1':'Charge only', 'mode.2':'Discharge only', 'mode.3':'Off',
      'reserve':'Reserve: %s%%', 'priority':'Charge: %s', 'priority.0':'Low', 'priority.1':'Normal', 'priority.2':'High',
      'output':'Output limit: %s%%', 'reserve.help':'Keep 0–90% in reserve. At a nonzero reserve, discharge resumes 5% above the floor.',
      'priority.help':'Local cable insertion preference. Higher groups are offered energy first; unused power reaches lower groups.',
      'output.help':'Caps discharge to 100%, 50% or 25% of this bank\'s rate. Every face shares the same tick budget.',
      'mode.help':'Automatic, charge only, discharge only or off. Front outputs; other faces charge. Settings save with this placed bank.',
      'modules':'Tank pairs: %s/4; rate: %s JE/t. %s',
      'detached':'Transfers suspended: restore missing tank modules.', 'connected':'Connected capacity supports the stored electrolyte.',
    }
    lang.update({'container.jugcraft.battery.'+key:value for key,value in words.items()})
    lang['container.jugcraft.terminal.modules_missing'] = 'MODULES MISSING'
    for name, display in ITEMS.items():
        lang['item.jugcraft.' + name] = display
        write(assets / 'models/item' / (name+'.json'), dict(parent='minecraft:item/generated', textures={'layer0':'jugcraft:item/'+name}))
        write(assets / 'items' / (name+'.json'), dict(model=dict(type='minecraft:model', model='jugcraft:item/'+name)))
    for name, display in BLOCKS.items():
        lang['block.jugcraft.'+name] = display
        lang['container.jugcraft.'+name] = display
    recipes = {
      'dry_lead_cell': (['GOS'],dict(G='jugcraft:lead_electrode',O='jugcraft:lead_oxide_electrode',S='jugcraft:battery_separator'),'dry_lead_cell'),
      'lfp_cathode_mix': (['LIP'],dict(L='jugcraft:lithium_carbonate',I='minecraft:iron_ingot',P='jugcraft:phosphate'),'lfp_cathode_mix'),
      'lead_acid_battery_bank': (['PCP','BBB','WBW'],dict(P='#c:plates/steel',C='jugcraft:basic_circuit',B='jugcraft:lead_acid_cell',W='jugcraft:copper_wire'), 'lead_acid_battery_bank'),
      'lithium_cell_assembly': (['CAS',' P '],dict(C='jugcraft:lithium_cathode',A='jugcraft:carbon_anode',S='jugcraft:battery_separator',P='#c:plates/aluminum'),'lithium_cell_assembly'),
      'flow_cell_stack': (['PAP','SCS','PAP'],dict(P='jugcraft:ptfe_liner',A='jugcraft:carbon_anode',S='jugcraft:battery_separator',C='jugcraft:copper_wire'),'flow_cell_stack'),
      'flow_tank_module': (['TVT','PFP'],dict(T='jugcraft:steel_tank',V='jugcraft:precision_stainless_valve',P='jugcraft:stainless_fitting',F='jugcraft:lightweight_frame'),'flow_tank_module'),
      'flow_stack_module': (['SCS','PFP'],dict(S='jugcraft:flow_cell_stack',C='jugcraft:advanced_circuit',P='jugcraft:stainless_fitting',F='jugcraft:lightweight_frame'),'flow_stack_module'),
    }
    for name,(pattern,key,result) in recipes.items():
        write(data / 'recipe' / ('battery_'+name+'.json'), {'fabric:load_conditions':condition('machines'), 'type':'minecraft:crafting_shaped','category':'misc','pattern':pattern,'key':key,'result':dict(id='jugcraft:'+result,count=1)})

def draw_all(save):
    import item_icons
    for name in ITEMS:
        save(item_icons.draw(name), 'item', name)
