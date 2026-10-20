"""Package 7: staged cooling, shared storage and usable atmospheric rocket motors.

Recipes are fictional game batches, not chemical production instructions.
"""
from biorefinery import liquid

ITEMS = {
    'cryogenic_insulation': 'Cryogenic Insulation',
    'methane_cooling_module': 'Methane Cooling Module',
    'hydrogen_cooling_module': 'Advanced Hydrogen Cooling Module',
    'methane_tank': 'Sealed Liquid Methane Tank',
    'hydrogen_tank': 'Sealed Liquid Hydrogen Tank',
    'hydrazine_tank': 'Sealed Hydrazine Tank',
    'n2o4_tank': 'Sealed N2O4 Tank',
    'methalox_motor': 'Loaded Methalox Motor',
    'hydrolox_motor': 'Loaded Hydrolox Motor',
    'hydrazine_thruster': 'Loaded Hydrazine Thruster',
    'storable_bipropellant_motor': 'Loaded Storable Bipropellant Motor',
    'propellant_residue': 'Propellant Process Residue',
}
FLUIDS = {
    'liquid_methane': liquid('Liquid Methane', [(42,62,70),(72,101,109),(111,145,151),(161,184,187)]),
    'liquid_hydrogen': liquid('Liquid Hydrogen', [(46,55,78),(81,96,124),(125,145,172),(178,192,206)]),
    'hydrazine': liquid('Hydrazine', [(62,66,48),(96,103,73),(141,149,109),(183,190,150)]),
    'dinitrogen_tetroxide': liquid('Dinitrogen Tetroxide (N2O4)', [(72,44,33),(115,72,48),(158,110,72),(192,151,109)]),
}
BLOCKS = {'insulated_fluid_tank':'Insulated Fluid Tank', 'vacuum_insulated_tank':'Vacuum-insulated Fluid Tank'}
LIQUEFIER_RECIPES = [
    dict(name='cryo_liquid_methane', fluids=[('jugcraft:methane',1000)],
         fluid_results=[('jugcraft:liquid_methane',250)], ticks=160,
         tool='#jugcraft:methane_cooling_modules', features=['machines']),
    dict(name='cryo_liquid_hydrogen', fluids=[('jugcraft:hydrogen',1000)],
         fluid_results=[('jugcraft:liquid_hydrogen',250)], ticks=320,
         tool='jugcraft:hydrogen_cooling_module', features=['machines']),
]
TANK_RECIPES = [dict(name='cryo_'+tank,items=[('jugcraft:rocket_casing',1)],fluids=[('jugcraft:'+fluid,1000)],
    results=[('jugcraft:'+tank,1)],ticks=80,features=['machines'])
    for tank,fluid in [('methane_tank','liquid_methane'),('hydrogen_tank','liquid_hydrogen'),
                      ('hydrazine_tank','hydrazine'),('n2o4_tank','dinitrogen_tetroxide')]]

def rec(name, cap, items=(), fluids=(), results=(), fluid_results=(), ticks=240, power=64):
    return dict(name='cryo_'+name,capability='jugcraft:'+cap,items=list(items),fluids=list(fluids),
                results=list(results),fluid_results=list(fluid_results),ticks=ticks,energy_per_tick=power,features=['machines'])

RECIPES = {
    'bulk_chemical_reactor': [
        rec('hydrazine','storable_propellants',[('minecraft:redstone',1)],
            [('jugcraft:ammonia',500),('jugcraft:nitrogen',250)],[('jugcraft:propellant_residue',1)],
            [('jugcraft:hydrazine',250,0)],ticks=400,power=96),
        rec('n2o4','storable_propellants',[('minecraft:redstone',1)],
            [('jugcraft:nitric_acid',500)], [('jugcraft:propellant_residue',1)],
            [('jugcraft:dinitrogen_tetroxide',250,0)],ticks=320,power=96),
    ],
    'mineral_conditioner': [
        rec('residue_recovery','propellant_recovery',[('jugcraft:propellant_residue',2)],
            [('minecraft:water',500)],fluid_results=[('jugcraft:recovery_concentrate',500,0)]),
    ],
    'chemical_distillation_station': [
        rec('vaporize_'+liquid_id,'cryogenic_vaporization',fluids=[('jugcraft:'+liquid_id,250)],
            fluid_results=[('jugcraft:'+gas,1000,0)],ticks=160,power=32)
        for liquid_id,gas in [('liquid_oxygen','oxygen'),('liquid_methane','methane'),('liquid_hydrogen','hydrogen')]]
}
MACHINE_RECIPES = {'rocket_workshop': [
    dict(name='cryo_'+motor,inputs=[('jugcraft:'+item,count) for item,count in ingredients],
         output='jugcraft:'+motor,count=yield_count,ticks=240,features=['machines'])
    for motor,ingredients,yield_count in [
        ('methalox_motor',[('methane_tank',1),('lox_tank',1),('rocket_nozzle',2)],3),
        ('hydrolox_motor',[('hydrogen_tank',1),('lox_tank',1),('rocket_nozzle',2)],4),
        ('hydrazine_thruster',[('hydrazine_tank',1),('rocket_nozzle',1),('porcelain_insulator',1)],1),
        ('storable_bipropellant_motor',[('hydrazine_tank',1),('n2o4_tank',1),('rocket_nozzle',2)],3),
    ]
]}
GUIDE_PAGES = [
    dict(title='One liquefier, three cooling stages',icon='jugcraft:hydrogen_cooling_module',text=[
        'Keep the existing oxygen route: 1000 mB oxygen becomes 250 mB Liquid Oxygen in 80 ticks at base 96 JE/t. No cooling module is needed. Electrolysis and air separation remain the gas supplies.',
        'Install a retained Methane Cooling Module in the Cryogenic Liquefier tool slot for 1000 mB methane to 250 mB liquid methane, 160 ticks at base 96 JE/t. Existing cleaned biogas and methanation supply methane. The advanced Hydrogen Cooling Module also supports methane.',
        'The advanced module enables 1000 mB hydrogen to 250 mB liquid hydrogen, 320 ticks at base 96 JE/t. It builds on the methane module, precision bearings, ceramic insulation and an advanced chip. Both modules are manual tooling, never consumed or exposed to hoppers.',
        'Clear incompatible fluid before changing recipes. Changing a module discards cooling progress, never fluid. Missing tooling pauses production. These compact machines use their existing tank controls; the formed chemistry machines retain their recipe picker, previews and locks.']),
    dict(title='Insulated storage and gas recovery',icon='jugcraft:vacuum_insulated_tank',text=[
        'Craft Cryogenic Insulation from PTFE liners, an alumina plate and a porcelain insulator. The Insulated Fluid Tank holds 32 buckets; the Vacuum-insulated Fluid Tank holds 64. Silver jackets, olive collars and six centered service faces match the factory palette.',
        'Both use the existing fluid pipes, pumps, tank groups and gauge. Connected compatible tanks share capacity. Break and replace a tank to retain its contents. Craft new tanks from raw parts, never from a filled tank. Existing tanks and cylinders retain their existing fluid compatibility.',
        'No boil-off timer, hidden pressure system or world damage is introduced. Insulated storage is an optional compact capacity upgrade; old fuel factories keep working.',
        'The Chemical Distillation Station vaporizes 250 mB LOX, liquid methane or liquid hydrogen into 1000 mB of the matching gas, using 160 ticks at base 32 JE/t. Lock the desired recipe and empty the first output before switching gases. The cooling/vaporization loop returns only its starting gas and consumes power.']),
    dict(title='Storable propellants and residue',icon='jugcraft:hydrazine_tank',text=[
        'These are fictional game recipes. In the Bulk Chemical Reactor, one redstone, 500 mB ammonia and 250 mB nitrogen produce 250 mB hydrazine and one Propellant Process Residue. The N2O4 job uses one redstone and 500 mB nitric acid for 250 mB N2O4 and one residue.',
        'Both jobs use base 96 JE/t; hydrazine takes 400 ticks and N2O4 takes 320. The process outlet and solid output must both have room. Existing ammonia, nitrogen and acid production supplies this branch.',
        'Wash two residue with 500 mB water in the Mineral Conditioner to obtain 500 mB Process Recovery Concentrate. The existing clay stabilization and concrete-fill recipe then give it a construction use. Raw propellant residue is not fertilizer or fuel.',
        'Hydrazine supplies the monopropellant thruster; N2O4 is only paired with hydrazine in the storable bipropellant assembly. MMH, orbital vehicles, destination progression and maneuvering simulation remain future work.']),
    dict(title='Loaded motors for rockets you already have',icon='jugcraft:hydrolox_motor',text=[
        'The compact Chemical Reactor seals 1000 mB of liquid methane, liquid hydrogen, hydrazine or N2O4 into one Rocket Casing. These sealed items are manufacturing ingredients, not refillable fluid containers. Existing kerosene and LOX tank recipes remain available.',
        'Rocket Workshop: one methane tank, one LOX tank and two nozzles yield three loaded Methalox Motors. Replace methane with hydrogen to yield four Hydrolox Motors. One hydrazine tank, one N2O4 tank and two nozzles yield three Storable Bipropellant Motors.',
        'One hydrazine tank, one nozzle and one Porcelain Insulator make one Hydrazine Thruster. All four loaded motor options substitute for the ordinary motor in Survey, Cloud-seeding, Clear-sky, Delivery and Line Rocket recipes. Mixing incompatible propellant tank pairs never matches a recipe.',
        'Payload behavior and range stay the same: the choice changes factory inputs and batch yield. Solid motors and RP-1/LOX remain valid. A loaded motor is consumed when assembling a rocket, including its propellant and casing; it is not an empty engine or a planet-launch unlock.']),
]

def write_extras(write, assets, data, lang, condition):
    def shaped(name, pattern, keys, count=1):
        write(data/'recipe'/('cryo_'+name+'.json'),{'fabric:load_conditions':condition('machines'),
            'type':'minecraft:crafting_shaped','category':'misc','pattern':pattern,'key':keys,
            'result':dict(id='jugcraft:'+name,count=count)})
    shaped('cryogenic_insulation',['PAP',' I '],{'P':'jugcraft:ptfe_liner','A':'jugcraft:alumina_plate','I':'jugcraft:porcelain_insulator'},4)
    shaped('methane_cooling_module',['SIS','BVB','SIS'],{'S':'jugcraft:stainless_steel_plate','I':'jugcraft:cryogenic_insulation',
        'B':'jugcraft:precision_bearing','V':'jugcraft:precision_stainless_valve'})
    shaped('hydrogen_cooling_module',['ICI','BMB','III'],{'I':'jugcraft:cryogenic_insulation','C':'jugcraft:advanced_chip',
        'B':'jugcraft:ceramic_bearing_set','M':'jugcraft:methane_cooling_module'})
    shaped('insulated_fluid_tank',['SIS','IVI','SIS'],{'S':'jugcraft:stainless_steel_plate','I':'jugcraft:cryogenic_insulation','V':'jugcraft:precision_stainless_valve'})
    shaped('vacuum_insulated_tank',['SIS','VLV','SIS'],{'S':'jugcraft:stainless_steel_plate','I':'jugcraft:cryogenic_insulation',
        'V':'jugcraft:precision_stainless_valve','L':'jugcraft:stainless_vessel'})
    write(data/'tags/item/methane_cooling_modules.json',dict(replace=False,values=['jugcraft:methane_cooling_module','jugcraft:hydrogen_cooling_module']))
    write(data/'tags/item/atmospheric_rocket_motors.json',dict(replace=False,values=['jugcraft:'+i for i in
        ['rocket_motor','methalox_motor','hydrolox_motor','hydrazine_thruster','storable_bipropellant_motor']]))
    lang['container.jugcraft.cooling_module']='Cooling module: methane tier, or advanced hydrogen tier (also supports methane). LOX needs no module.'
    lang['tooltip.jugcraft.methane_cooling_module']='Retained Cryogenic Liquefier tool: enables liquid methane. LOX needs no tool.'
    lang['tooltip.jugcraft.hydrogen_cooling_module']='Retained Cryogenic Liquefier tool: enables liquid hydrogen and liquid methane.'
    lang['tooltip.jugcraft.cryogenic_insulation']='PTFE and ceramic insulation for cooling modules and shared insulated tanks.'
    lang['tooltip.jugcraft.propellant_residue']='Wash two with water in the Mineral Conditioner; stabilize the resulting Recovery Concentrate with clay.'
    for name in ['methane_tank','hydrogen_tank','hydrazine_tank','n2o4_tank']:
        lang['tooltip.jugcraft.'+name]='Sealed 1,000 mB manufacturing charge. Consumed in a compatible Rocket Workshop assembly; not a refillable container.'
    for name in ['methalox_motor','hydrolox_motor','hydrazine_thruster','storable_bipropellant_motor']:
        lang['tooltip.jugcraft.'+name]='Fueled assembly: replaces one ordinary motor in survey, weather, delivery or line rocket crafting. Same payload range.'
    from cryogenic_models import tank_model
    import generate_material_data as material_data
    for name,display in BLOCKS.items():
        lang['block.jugcraft.'+name]=display
        write(assets/'blockstates'/(name+'.json'),{'variants':{'':{'model':'jugcraft:block/'+name}}})
        write(assets/'models/block'/(name+'.json'),tank_model(name=='vacuum_insulated_tank'))
        write(assets/'items'/(name+'.json'),{'model':{'type':'minecraft:model','model':'jugcraft:block/'+name}})
        write(data/'loot_table/blocks'/(name+'.json'),material_data.tank_drop(name))

def draw_all(save):
    import item_icons
    for name in ITEMS: save(item_icons.draw(name),'item',name)
