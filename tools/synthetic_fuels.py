"""Package 6: reusable cobalt synthesis and bounded process-water recovery.

Abstract game batches. Existing gas preparation, refining and finished fuels are reused.
"""
from biorefinery import liquid

ITEMS = {
    'cobalt_concentrate': 'Cobalt Concentrate',
    'cobalt_ingot': 'Cobalt Ingot',
    'cobalt_catalyst_bed': 'Cobalt-Ceramic Catalyst Bed',
    'stabilized_process_residue': 'Stabilized Process Residue',
}
FLUIDS = {
    'synthetic_crude': liquid('Synthetic Crude', [(35,31,24),(66,57,39),(110,94,61),(158,139,95)]),
    'treated_process_water': liquid('Treated Process Water', [(43,62,59),(76,101,95),(119,146,135),(166,185,171)]),
    'recovery_concentrate': liquid('Process Recovery Concentrate', [(48,42,53),(82,70,86),(122,107,128),(166,150,170)]),
}

def rec(name, cap, items=(), fluids=(), results=(), fluid_results=(), ticks=240, power=48, tool=None):
    entry = dict(name='synthetic_'+name,capability='jugcraft:'+cap,items=list(items),fluids=list(fluids),
                 results=list(results),fluid_results=list(fluid_results),ticks=ticks,energy_per_tick=power,features=['machines'])
    if tool: entry['tool']='jugcraft:'+tool
    return entry

RECIPES = {
    'mineral_conditioner': [
        rec('wash_cobalt','synthetic_preparation',[('jugcraft:cobaltite',2)],[('minecraft:water',500)],
            [('jugcraft:cobalt_concentrate',2)],[('jugcraft:spent_mineral_wash',500,0)]),
        rec('treat_wafer_waste','process_recovery',[('minecraft:calcite',1)],
            [('jugcraft:spent_wafer_mixture',1000),('jugcraft:lye',250)],
            fluid_results=[('jugcraft:treated_process_water',1000,0),('jugcraft:recovery_concentrate',250,1)],ticks=320,power=64),
        rec('stabilize_concentrate','process_recovery',[('minecraft:clay_ball',1)],
            [('jugcraft:recovery_concentrate',1000)],[('jugcraft:stabilized_process_residue',4)],ticks=320,power=64),
    ],
    'chemical_infuser': [
        rec('crude','cobalt_synthesis',fluids=[('jugcraft:carbon_monoxide',500),('jugcraft:hydrogen',1000)],
            fluid_results=[('jugcraft:synthetic_crude',500,0),('minecraft:water',500,1)],ticks=400,power=128,
            tool='cobalt_catalyst_bed'),
        rec('shift_gas','syngas_balancing',fluids=[('jugcraft:carbon_monoxide',250),('minecraft:water',500)],
            fluid_results=[('jugcraft:hydrogen',250,0),('jugcraft:carbon_dioxide',250,1)],ticks=200,power=128,
            tool='cobalt_catalyst_bed'),
    ],
    'chemical_distillation_station': [
        rec('recover_water','process_distillation',fluids=[('jugcraft:treated_process_water',1000)],
            fluid_results=[('minecraft:water',750,0),('jugcraft:recovery_concentrate',250,1)],ticks=320,power=64),
    ],
}
# Uses the tower's existing five tank indices. No second fuel grade or duplicate engine code.
REFINERY_RECIPES = [dict(name='synthetic_crude_fractions',fluids=[('jugcraft:synthetic_crude',500)],
    fluid_results=[('jugcraft:refinery_gas',50,0),('jugcraft:naphtha',150,1),
                   ('jugcraft:diesel',250,2),('jugcraft:heavy_fuel_oil',50,3)],ticks=160,features=['machines','crude_oil'])]
MACHINE_RECIPES = {
    'arc_furnace': [dict(name='synthetic_cobalt_stock',inputs=[('jugcraft:cobalt_concentrate',2),('jugcraft:coke',1)],
        output='jugcraft:cobalt_ingot',count=2,ticks=320,features=['machines'])],
}
GUIDE_PAGES = [
    dict(title='Independent cobalt supply',icon='jugcraft:cobaltite',text=[
        'Find Cobaltite beneath windswept hills, windswept forest, stony peaks, Shield or Highland in newly generated Overworld chunks, from Y -48 to 32. Occasional surface outcrops identify the host province. Mine with iron or better, survey or trade; existing terrain is never rewritten. Nickel mining stays separate.',
        'Wash two Cobaltite with 500 mB water in the Mineral Conditioner into two Cobalt Concentrate and 500 mB Spent Mineral Wash. The existing wash-settling route accepts that waste. The Arc Furnace reduces two concentrate with one Coke into two Cobalt Ingots.',
        'Craft two cobalt ingots over three fired vanilla bricks into one Cobalt-Ceramic Catalyst Bed. Install it in the Chemical Infuser tool socket. It is retained without wear and cannot be removed during a running batch. No synthetic fuel is required to make the first bed. Nickel methanation and vanadium contact beds keep their separate jobs.']),
    dict(title='Clean gases to synthetic crude',icon='jugcraft:synthetic_crude_bucket',text=[
        'Existing Coal Gasifiers process coal, charcoal or Coke and water into dirty synthesis gas. The existing Gas Scrubber cleans it with wash water, giving separate carbon monoxide, hydrogen and mixed gas-cleaning residue. Keep the existing residue stabilization route; dirty gas cannot enter synthesis directly.',
        'Use existing electrolysis for extra hydrogen. Alternatively the Chemical Infuser, with the cobalt bed, converts 250 mB carbon monoxide and 500 mB water into 250 mB hydrogen (first outlet) and 250 mB CO2 (second). This trades carbon feed and power for hydrogen, not free fuel. The CO2 can use existing storage and methanation.',
        'With the cobalt bed, infuse 500 mB clean CO and 1000 mB hydrogen into 500 mB Synthetic Crude and 500 mB water, 400 ticks at base 128 JE/t. These are simplified game batches. Use Recipes and Lock recipe; clear old products before switching jobs. Product outlet is at the rear, with the second outlet below.',
        'The existing Infuser Drain excess water option applies only to plain water. Switch it off to capture all synthesis water and stop when full. CO2 from gas balancing is never silently drained. Existing gas tanks/cylinders, fluid tanks and pipes carry the supplies and products.']),
    dict(title='One refinery, standard finished fuels',icon='jugcraft:diesel_bucket',text=[
        'Feed 500 mB Synthetic Crude into the existing Distillation Tower. Collect 50 mB Refinery Gas in outlet 0, 150 mB Naphtha in outlet 1, 250 mB Diesel in outlet 2 and 50 mB Heavy Fuel Oil in outlet 3. All four products need space; outlet 4 remains unused. The compact tower uses its existing processing controls.',
        'Diesel is already usable in compatible engines and vehicles. Existing Catalytic Cracker recipes finish naphtha into gasoline and refinery gas, or crack heavy oil into lighter fractions. Existing vacuum distillation produces lubricant and asphalt binder; existing hydrocracking prepares rocket kerosene.',
        'Finished products retain the same IDs, energy values and compatibility as oil-derived fuels. The crude mixture itself is not a registered generator fuel. Refinery gas also supplies existing polymer chemistry. Account for gasification, wash, hydrogen, synthesis and refining energy together before judging the line profitable.']),
    dict(title='Recover process water, contain residues',icon='jugcraft:treated_process_water_bucket',text=[
        'Wafer jobs share one Spent Wafer Mixture, so recovery cannot assume that every batch contains recoverable acid, alcohol or metal. No purity upgrade or arbitrary reagent restoration is granted.',
        'The Mineral Conditioner treats 1000 mB spent mixture with 250 mB lye and one Calcite block into 1000 mB Treated Process Water (outlet 0) plus 250 mB Process Recovery Concentrate (outlet 1). The Chemical Distillation Station processes that treated water into 750 mB water and another 250 mB concentrate.',
        'Condition 1000 mB concentrate with one clay ball into four Stabilized Process Residue. Craft two residue, two sand and two gravel into four ordinary Gray Concrete Powder. This is fictional paid preparation for game construction, not a real waste-handling procedure. Raw concentrate has no fuel, fertilizer or food use.',
        'Both recovery stages use 320 ticks at base 64 JE/t. No waste is silently deleted. Fill every required output and the job waits; use shared recipe previews, locks and stock targets. Earlier gas-cleaning, mineral-wash, digestate and gypsum routes remain useful.']),
]

def write_extras(write,assets,data,lang,condition):
    write(data/'recipe/synthetic_cobalt_catalyst_bed.json',{'fabric:load_conditions':condition('machines'),
        'type':'minecraft:crafting_shaped','category':'misc','pattern':['C C','BBB'],
        'key':{'C':'jugcraft:cobalt_ingot','B':'minecraft:brick'},'result':dict(id='jugcraft:cobalt_catalyst_bed',count=1)})
    write(data/'recipe/synthetic_stabilized_fill.json',{'fabric:load_conditions':condition('machines'),
        'type':'minecraft:crafting_shapeless','category':'building',
        'ingredients':['jugcraft:stabilized_process_residue']*2+['minecraft:sand']*2+['minecraft:gravel']*2,
        'result':dict(id='minecraft:gray_concrete_powder',count=4)})
    write(data.parent/'c/tags/item/ingots/cobalt.json',dict(replace=False,values=['jugcraft:cobalt_ingot']))
    lang['tooltip.jugcraft.cobalt_catalyst_bed']='Reusable Chemical Infuser tooling for synthetic crude and gas balancing. Not consumed.'

def draw_all(save):
    import item_icons
    from pathlib import Path
    from PIL import Image
    for name in ITEMS:
        if name=='cobalt_ingot':
            # Direct reuse of owner-supplied sprite, with its design preserved.
            with Image.open(Path(__file__).resolve().parents[1]/'art/owner-library/originals/Blocks/Weapon Smithing/Items/materials/cobalt_ingot.png') as img:
                save(img.copy(),'item',name)
        else: save(item_icons.draw(name),'item',name)
