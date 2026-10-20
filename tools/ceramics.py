"""Owner tech package 1: shared ceramic processing and precision finishing.

Game quantities, not industrial instructions. Additive recipes preserve the starter workshop.
Validation is deliberately deferred until the owner's packages 1-7 are implemented.
"""
from coastal_metals import station

ITEMS = {
    'clay_powder': 'Clay Powder',
    'porcelain_batch': 'Porcelain Powder Blend',
    'porcelain_charge': 'Prepared Porcelain Charge',
    'porcelain_blank': 'Unfired Porcelain Blank',
    'fired_porcelain_blank': 'Fired Porcelain Blank',
    'porcelain_insulator': 'Porcelain Insulator',
    'alumina_charge': 'Prepared Alumina Charge',
    'alumina_blank': 'Unfired Alumina Blank',
    'fired_alumina_blank': 'Fired Alumina Blank',
    'alumina_plate': 'Finished Alumina Plate',
    'ceramic_bearing_set': 'Ceramic Bearing Balls',
    'zircon_concentrate': 'Zircon Concentrate',
    'zirconia_precursor': 'Prepared Zirconia Precursor',
    'zirconia_powder': 'Zirconia Powder',
    'zirconia_charge': 'Prepared Zirconia Charge',
    'zirconia_blank': 'Unfired Zirconia Blank',
    'fired_zirconia_blank': 'Fired Zirconia Blank',
    'zirconia_insert': 'Zirconia Cutting Insert',
    'steel_shaft_blank': 'Steel Shaft Blank',
    'precision_shaft': 'Precision Steel Shaft',
    'precision_bearing': 'Precision Bearing',
    'optical_blank': 'Optical Glass Blank',
    'polished_lens': 'Polished Glass Lens',
    'abrasive_head': 'Diamond Abrasive Head',
    'polishing_head': 'Felt Polishing Head',
}

FORMS = {
    'precision_finisher': station('Precision Finishing Station', 3, 3,
        ['finishing_water'], ['finishing_return'], (1, 1), 'precision_finishing',
        ['PGP', 'MCM', 'BKB'], sockets=[('finishing_head', 'jugcraft:finishing_heads')]),
}
# Earlier steel/circuits/motors start the machine: it never requires its own precision parts.
RECIPES = {'precision_finisher': []}
EXTRA_RECIPES = {'mineral_conditioner': [], 'alumina_calciner': []}


def add(name, machine, feed, count, product, amount=1, *, water=0, reagent=None,
        ticks=160, power=32, tool=None, features=()):
    caps = {'mineral_conditioner': 'mineral_conditioning',
            'alumina_calciner': 'ceramic_firing', 'precision_finisher': 'precision_finishing'}
    fluids = [('minecraft:water', water)] if water else []
    if reagent:
        fluids.append(reagent)
    r = dict(name=name, capability='jugcraft:' + caps[machine],
             items=[(feed, count)], results=[('jugcraft:' + product, amount)],
             fluids=fluids, fluid_results=[], ticks=ticks, energy_per_tick=power,
             features=['machines', *features])
    if machine == 'precision_finisher':
        # The closed splash tray returns its coolant: output space is mandatory, no free water.
        r['fluid_results'] = [('minecraft:water', water, 0)]
    if tool:
        r['tool'] = 'jugcraft:' + tool
    (RECIPES if machine in FORMS else EXTRA_RECIPES)[machine].append(r)


for grade, feed, feature in [('porcelain', 'porcelain_batch', 'silicon'),
                              ('alumina', 'alumina', 'aluminum'),
                              ('zirconia', 'zirconia_powder', 'salt')]:
    add('prepare_' + grade, 'mineral_conditioner', 'jugcraft:' + feed, 1,
        grade + '_charge', water=125, features=[feature])
    add('fire_' + grade, 'alumina_calciner', 'jugcraft:' + grade + '_blank', 1,
        'fired_' + grade + '_blank', ticks=240 if grade == 'porcelain' else 320,
        power=48 if grade == 'porcelain' else 80, features=[feature])

# Reachable, deliberately expensive trace-mineral route; future regional sand deposits can
# add a better receipt without becoming a mandatory gate or renaming this item.
add('recover_trace_zircon', 'mineral_conditioner', 'minecraft:sand', 16,
    'zircon_concentrate', water=1000, ticks=400, power=48)
add('recover_regional_zircon', 'mineral_conditioner', 'jugcraft:zircon_sand', 2,
    'zircon_concentrate', water=500, ticks=200, power=48, features=['mineral_sands'])
add('prepare_zirconia_precursor', 'mineral_conditioner', 'jugcraft:zircon_concentrate', 2,
    'zirconia_precursor', reagent=('jugcraft:lye', 500), ticks=240, power=64, features=['salt'])
add('calcine_zirconia', 'alumina_calciner', 'jugcraft:zirconia_precursor', 1,
    'zirconia_powder', ticks=320, power=80, features=['salt'])

FINISHING = [
    ('porcelain_insulator', 'fired_porcelain_blank', 2, 'abrasive_head', 'silicon'),
    ('alumina_plate', 'fired_alumina_blank', 1, 'abrasive_head', 'aluminum'),
    ('ceramic_bearing_set', 'fired_alumina_blank', 1, 'abrasive_head', 'aluminum'),
    ('zirconia_insert', 'fired_zirconia_blank', 2, 'abrasive_head', 'salt'),
    ('precision_shaft', 'steel_shaft_blank', 1, 'abrasive_head', None),
    ('polished_lens', 'optical_blank', 1, 'polishing_head', 'silicon'),
]
for product, feed, count, tool, feature in FINISHING:
    add('finish_' + product, 'precision_finisher', 'jugcraft:' + feed, 1,
        product, count, water=125, ticks=200, power=48, tool=tool,
        features=[feature] if feature else [])

# Single-input workshop machines use their existing serializers and normal power costs.
MACHINE_RECIPES = {
    'pulverizer': [dict(name='ceramics_clay_powder', input='minecraft:clay_ball',
                       output='jugcraft:clay_powder', count=1, ticks=80, features=['machines'])],
    'circuit_assembler': [dict(name='ceramics_processor_on_alumina', inputs=[('jugcraft:microchip',4),('jugcraft:advanced_circuit',1),('jugcraft:alumina_plate',1)], output='jugcraft:processor',count=1,ticks=200,features=['machines','silicon','aluminum'])],
    'metal_press': [dict(name='ceramics_' + grade + '_blank', input='jugcraft:' + grade + '_charge',
                        output='jugcraft:' + grade + '_blank', count=1, ticks=100,
                        features=['machines']) for grade in ('porcelain', 'alumina', 'zirconia')],
}
# Unique input avoids competing with the press's existing steel-ingot -> plate job.
# Shafts and optical blanks are hand-prepared assemblies, then finished to size.

TANK_NAMES = {'finishing_water': 'Clean cooling water', 'finishing_return': 'Recovered cooling water'}
HANDBOOK = {'precision_finisher': [
    'Three blocks wide, deep and tall. One shared station grinds ceramic blanks and steel shafts, and polishes optical glass. Build it with ordinary steel, motors and a basic circuit; no precision output is needed to start.',
    'Fit a Diamond Abrasive Head for insulators, alumina plates, bearing balls, zirconia inserts or shafts; fit a Felt Polishing Head for lenses. Both heads are reusable, manually installed tools. Their recipes use earlier supplies, not this station. They are never consumed by a batch.',
    'Each job takes one blank and 125 mB water, runs for 200 ticks at 48 JE/t before upgrades, and returns all 125 mB to its separate outlet. Both the product drawer and water outlet must fit. Pipe the return into the feed to reuse coolant; a full outlet pauses production rather than destroying it.',
    'Front lower left: power. Front right: blank input. Rear left, middle height: water inlet; directly below: returned water. Rear right: finished part. The visible splash tray, drain and coolant line serve those ports.',
    'Use Recipes to preview costs and required tools, then Lock recipe. Alumina blanks have two jobs: plates or bearing balls. Automatic defaults to the alphabetically first compatible job, so lock the product before automating. A running batch keeps its reserved inputs when you change the next job.',
]}

GUIDE_PAGES = [
    {'title': 'Ceramics: powder to finished part', 'icon': 'jugcraft:porcelain_insulator', 'text': [
        'Pulverize clay balls into clay powder. Mix four powder, two quartz and one borax into four Porcelain Powder Blends. The Mineral Conditioner prepares each blend with 125 mB water. The existing Metal Press shapes the charge; the shared Industrial Oven fires it. Finish each fired blank into two porcelain insulators.',
        'Alumina from the existing bauxite line follows the same conditioning, pressing and firing route. Finish one blank into an Alumina Plate or one set of Ceramic Bearing Balls. Lock the desired recipe; there is no separate ceramic press or kiln to build.',
        'Before ceramics, the original terracotta insulator, motor and upgrade recipes still work. Ordinary clay bricks still start steel and chemistry. Ceramics are an optional productive specialty, not a retroactive gate.']},
    {'title': 'Precision parts and their uses', 'icon': 'jugcraft:precision_bearing', 'text': [
        'Two steel plates hand-assemble two shaft blanks. Finish them with the abrasive head. A shaft, a steel plate and a set of ceramic balls assemble a Precision Bearing. Bearings and shafts make two motors per advanced assembly, or a Speed Upgrade with less gear-making.',
        'Finished Alumina Plates provide the insulating body of an alternate Efficiency Upgrade. Porcelain insulators make existing placed Ceramic Insulators and an alternate Copper Cable recipe. These are extra recipes: old recipes and saved equipment stay available.',
        'Two borosilicate glass make two Optical Blanks. The Felt Polishing Head finishes each into a lens; two lenses, a brass plate and a compass make the existing Range Finder. Lenses also make spyglasses. Advanced wafer equipment will use this same finishing specialty in a later package.']},
    {'title': 'Specialist zirconia tooling', 'icon': 'jugcraft:zirconia_insert', 'text': [
        'The Conditioner recovers one trace Zircon Concentrate from 16 sand and 1000 mB water. Two concentrate plus 500 mB lye make one precursor; calcine it in the Industrial Oven for zirconia powder. This expensive, deterministic starter route works in existing worlds without new chunks. Regional Zircon-rich Mineral Sand is the efficient route: two sand and 500 mB water produce one concentrate.',
        'Condition, press and fire zirconia like alumina, then finish one fired blank into two cutting inserts. An insert, steel plate and iron ingot craft a reusable polymer mold or extrusion die in a selected pattern. The original all-metal tool recipes remain available.',
        'Inputs and costs are simplified game balance. Consumed preparation liquids are spent; only the Finisher returns its cooling water. There is no recycling recipe that turns finished components back into more feedstock.']},
]


def write_extras(write, assets, data, lang, condition):
    for name, display in ITEMS.items():
        lang['item.jugcraft.' + name] = display
        write(assets / 'models/item' / (name + '.json'), dict(parent='minecraft:item/generated',
              textures={'layer0': 'jugcraft:item/' + name}))
        write(assets / 'items' / (name + '.json'), dict(model=dict(type='minecraft:model', model='jugcraft:item/' + name)))
    write(data / 'tags/item/finishing_heads.json', dict(values=['jugcraft:abrasive_head', 'jugcraft:polishing_head']))
    for tag, names in {'ceramic_insulating_parts': ['porcelain_insulator', 'alumina_plate'],
                       'precision_components': ['precision_shaft', 'precision_bearing', 'polished_lens', 'zirconia_insert']}.items():
        write(data / 'tags/item' / (tag + '.json'), dict(values=['jugcraft:' + n for n in names]))

    def shaped(name, pattern, key, result, count=1, features=()):
        write(data / 'recipe' / ('ceramics_' + name + '.json'), {
            'fabric:load_conditions': [c for f in ['machines', *features] for c in condition(f)],
            'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pattern,
            'key': key, 'result': dict(id=result, count=count)})

    def shapeless(name, ingredients, result, count=1, features=()):
        write(data / 'recipe' / ('ceramics_' + name + '.json'), {
            'fabric:load_conditions': [c for f in ['machines', *features] for c in condition(f)],
            'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'ingredients': ingredients,
            'result': dict(id=result, count=count)})

    shapeless('porcelain_batch', ['jugcraft:clay_powder'] * 4 + ['minecraft:quartz'] * 2 + ['jugcraft:borax'],
              'jugcraft:porcelain_batch', 4, ['silicon'])
    shaped('shaft_blanks', ['P', 'P'], {'P': '#c:plates/steel'}, 'jugcraft:steel_shaft_blank', 2)
    shapeless('optical_blanks', ['jugcraft:borosilicate_glass'] * 2, 'jugcraft:optical_blank', 2, ['silicon'])
    shaped('abrasive_head', [' D ', 'AIA', ' A '],
           {'D': 'minecraft:diamond', 'A': 'jugcraft:alumina', 'I': 'minecraft:iron_ingot'},
           'jugcraft:abrasive_head', features=['aluminum'])
    shaped('polishing_head', [' W ', 'AIA', ' W '],
           {'W': '#minecraft:wool', 'A': 'jugcraft:alumina', 'I': 'minecraft:iron_ingot'},
           'jugcraft:polishing_head', features=['aluminum'])
    shapeless('precision_bearing', ['jugcraft:precision_shaft', '#c:plates/steel', 'jugcraft:ceramic_bearing_set'],
              'jugcraft:precision_bearing', features=['aluminum'])
    shaped('motors', ['PWP', 'BSB', 'CIC'],
           {'P': '#c:plates/iron', 'W': '#c:wires/copper', 'B': 'jugcraft:precision_bearing',
            'S': 'jugcraft:precision_shaft', 'C': 'jugcraft:copper_cable', 'I': 'jugcraft:porcelain_insulator'},
           'jugcraft:electric_motor', 2, ['aluminum', 'silicon'])
    shaped('speed_upgrade', ['PRP', 'BCB', ' S '],
           {'P': '#c:plates/steel', 'R': 'minecraft:redstone', 'B': 'jugcraft:precision_bearing',
            'C': 'jugcraft:basic_circuit', 'S': 'jugcraft:precision_shaft'}, 'jugcraft:speed_upgrade', features=['aluminum'])
    shaped('efficiency_upgrade', ['AWA', 'PCP', 'AWA'],
           {'A': 'jugcraft:alumina_plate', 'W': '#c:wires/copper', 'P': '#c:plates/steel',
            'C': 'jugcraft:basic_circuit'}, 'jugcraft:efficiency_upgrade', features=['aluminum'])
    shaped('placed_insulators', ['I', 'I', 'I'], {'I': 'jugcraft:porcelain_insulator'},
           'jugcraft:ceramic_insulator', 6, ['silicon', 'drones'])
    shaped('copper_cable', ['WIW'], {'W': '#c:wires/copper', 'I': 'jugcraft:porcelain_insulator'},
           'jugcraft:copper_cable', 2, ['silicon'])
    shaped('range_finder', ['L L', ' BC'],
           {'L': 'jugcraft:polished_lens', 'B': '#c:plates/brass', 'C': 'minecraft:compass'},
           'jugcraft:range_finder', features=['silicon'])
    shaped('spyglass', ['L', 'C', 'C'], {'L': 'jugcraft:polished_lens', 'C': 'minecraft:copper_ingot'},
           'minecraft:spyglass', features=['silicon'])
    shaped('main_transformer', ['IAI', 'CBC', 'SSS'],
           {'I': 'jugcraft:porcelain_insulator', 'A': 'jugcraft:aluminum_cable',
            'C': 'minecraft:copper_block', 'B': '#c:storage_blocks/steel', 'S': '#c:plates/steel'},
           'jugcraft:main_transformer', features=['aluminum', 'silicon'])
    # Each tool has a unique arrangement, no overlapping shapeless choice.
    from coastal_metals import MOLDS
    from polymers import DIES
    # Minecraft accepts horizontal mirrors, so mirrored layouts must not name different tools.
    patterns = [['IP', 'C '], ['IP', ' C'], ['IC', 'P '], ['IC', ' P'],
                ['I P', ' C '], ['I C', ' P '], ['I  ', ' PC'], ['I  ', ' CP'],
                ['IP ', '  C'], ['IC ', '  P'], ['I', 'P', 'C']]
    for tool, pattern in zip([m + '_mold' for m in MOLDS] + [d + '_extrusion_die' for d in DIES], patterns, strict=True):
        shaped(tool, pattern, {'I': 'jugcraft:zirconia_insert', 'P': '#c:plates/steel', 'C': 'minecraft:iron_ingot'},
               'jugcraft:' + tool, features=['salt'])
