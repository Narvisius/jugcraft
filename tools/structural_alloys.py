"""Owner package 2: chromite, chemical stainless and aluminum-magnesium structures.

All processing reuses existing stations. Ratios are approximate whole-ingot game
batches, not certified grades. Validation is deferred with packages 1-7.
"""

METALS = ('ferrochromium', 'stainless_steel', 'aluminum_magnesium')
ITEMS = {
    'chromite_concentrate': 'Chromite Concentrate',
    'stainless_fitting': 'Stainless Pipe Fitting',
    'stainless_vessel': 'Ceramic-lined Stainless Vessel',
    'stainless_valve_blank': 'Unfinished Stainless Valve',
    'precision_stainless_valve': 'Precision Stainless Valve',
    'lightweight_frame': 'Lightweight Alloy Frame',
    'alloy_panel': 'Formed Aluminum-Magnesium Panel',
}


def machine(name, inputs, output, count, ticks, features=()):
    return dict(name=name, inputs=inputs, output='jugcraft:' + output,
                count=count, ticks=ticks, features=['machines', *features])


MACHINE_RECIPES = {
    'arc_furnace': [machine('alloys_ferrochromium', [('jugcraft:chromite_concentrate', 2),
                                                  ('jugcraft:coke', 1)], 'ferrochromium_ingot', 2, 320)],
    'alloy_smelter': [
        machine('chemical_stainless_steel', [('jugcraft:steel_ingot', 6),
            ('jugcraft:ferrochromium_ingot', 2), ('jugcraft:nickel_ingot', 1)],
            'stainless_steel_ingot', 9, 400, ['nickel']),
        machine('structural_aluminum_magnesium', [('jugcraft:aluminum_ingot', 7),
            ('jugcraft:magnesium_ingot', 1)], 'aluminum_magnesium_ingot', 8, 240, ['aluminum']),
    ],
    'metal_press': [
        dict(name='alloys_stainless_fittings', input='jugcraft:stainless_steel_plate',
             output='jugcraft:stainless_fitting', count=2, ticks=120, features=['machines', 'nickel']),
        dict(name='alloys_structural_panel', input='jugcraft:aluminum_magnesium_plate',
             output='jugcraft:alloy_panel', count=1, ticks=120, features=['machines', 'aluminum']),
    ],
    'rocket_workshop': [machine('alloy_rocket_casing', [('jugcraft:alloy_panel', 2)],
                              'rocket_casing', 1, 160, ['aluminum'])],
}

EXTRA_RECIPES = {
    'mineral_conditioner': [dict(name='condition_chromite', capability='jugcraft:mineral_conditioning',
        items=[('jugcraft:chromite', 2)], fluids=[('minecraft:water', 250)],
        results=[('jugcraft:chromite_concentrate', 2)], fluid_results=[],
        ticks=160, energy_per_tick=32, features=['machines'])],
    'precision_finisher': [dict(name='finish_stainless_valve', capability='jugcraft:precision_finishing',
        items=[('jugcraft:stainless_valve_blank', 1)], fluids=[('minecraft:water', 125)],
        results=[('jugcraft:precision_stainless_valve', 1)],
        fluid_results=[('minecraft:water', 125, 0)], tool='jugcraft:polishing_head',
        ticks=240, energy_per_tick=48, features=['machines', 'nickel'])],
}

GUIDE_PAGES = [
    {'title': 'Regional chromite to stainless steel', 'icon': 'jugcraft:chromite', 'text': [
        'Find dark chromite in new chunks beneath windswept hills, windswept forest, stony peaks, stony shores, Shield and Highland. Its host province follows the surface biome even when you enter a cave. Occasional exposed outcrops mark a local source. Mine with an iron pickaxe or better, or trade with another player. Old chunks are not rewritten.',
        'Condition two chromite with 250 mB water into two concentrate, then reduce both with one Coke in the Arc Furnace into two Ferrochromium Ingots. Preparation water is spent, not free recoverable coolant. Ferrochromium is iron/chromium alloying stock, not pure chromium.',
        'The existing Alloy Smelter takes six Steel Ingots, two Ferrochromium Ingots and one Nickel Ingot to make nine Stainless Steel Ingots in 400 ticks. Nickel comes from its existing independent ore route. The existing Metal Press makes one plate per ingot. No stainless equipment is required to make the first batch.',
        'These convenient game ratios conserve input metal units; they do not represent a certified real-world grade. There is no reverse recipe separating alloys into their ingredients.']},
    {'title': 'Chemical vessels and precision valves', 'icon': 'jugcraft:stainless_vessel', 'text': [
        'Press one Stainless Steel Plate into two pipe fittings. Four plates and two finished Alumina Plates craft one Ceramic-lined Stainless Vessel. One fitting, a gasket and a Precision Shaft assemble an unfinished valve; finish it using the Felt Polishing Head, 125 mB returned cooling water, 240 ticks and 48 JE/t.',
        'Use these parts in alternate Steel Fluid Pipe, Steel Tank, Bulk Chemical Reactor and Lined Fluorochemical Reactor recipes. The lined reactor still needs its PTFE liner, seal and fitting. Stainless substitutes for selected construction parts; it does not bypass the first PTFE production route.',
        'The outputs are the existing machines and transport blocks, with their existing capacities, models and behavior. Stainless recipes offer different material bills; they do not secretly add corrosion resistance, extra throughput or storage. Every older recipe stays available.']},
    {'title': 'Lightweight structural alloy', 'icon': 'jugcraft:aluminum_magnesium_ingot', 'text': [
        'Seven Aluminum Ingots and one Magnesium Ingot make eight Aluminum-Magnesium Alloy Ingots in the Alloy Smelter, 240 ticks. Use the existing bauxite and coastal-seawater magnesium routes. This approximate whole-ingot game batch conserves all eight metal units.',
        'Press each ingot to a plate and each plate to one formed panel. Four alloy plates and one Precision Shaft craft two lightweight frames. Panels feed Rocket Workshop casings; frames support an alternate Zeppelin recipe. Frames and panels also provide an alternate Howitzer Walker assembly, keeping its Diesel Walker core, dispenser and pistons.',
        'These are manufacturing alternatives, not different vehicle stats or hidden weight bonuses. Existing vehicle construction and worlds remain supported. Titanium alloys and new armor/tool tiers remain separate future work.']},
    {'title': 'Alloy cladding and drone propellers', 'icon': 'jugcraft:aluminum_propeller_set', 'text': [
        'Four Aluminum-Magnesium Alloy Plates craft six Aluminum Cladding. Four plates around one stick craft two Aluminum Propeller Sets. These optional material-saving routes reuse the existing alloy, drone parts and cladding; the ordinary aluminum recipes remain available.',
        'The plates come from the established seven-aluminum/one-magnesium alloy batch. No second magnalium metal or parallel processing chain is needed. These alternatives change material costs, not drone performance.']},
]


def write_extras(write, assets, data, lang, condition):
    for name, display in ITEMS.items():
        lang['item.jugcraft.' + name] = display
        write(assets / 'models/item' / (name + '.json'), dict(parent='minecraft:item/generated',
              textures={'layer0': 'jugcraft:item/' + name}))
        write(assets / 'items' / (name + '.json'), dict(model=dict(type='minecraft:model', model='jugcraft:item/' + name)))
    for tag, members in {
        'chemical_process_parts': ['stainless_fitting', 'stainless_vessel', 'precision_stainless_valve'],
        'lightweight_structural_parts': ['lightweight_frame', 'alloy_panel'],
    }.items():
        write(data / 'tags/item' / (tag + '.json'), dict(values=['jugcraft:' + m for m in members]))

    def shaped(name, pattern, key, output, count=1, features=()):
        write(data / 'recipe' / ('alloys_' + name + '.json'), {
            'fabric:load_conditions': [c for f in ['machines', *features] for c in condition(f)],
            'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pattern,
            'key': key, 'result': dict(id='jugcraft:' + output, count=count)})

    shaped('stainless_vessel', ['PAP', 'PAP'],
           {'P': '#c:plates/stainless_steel', 'A': 'jugcraft:alumina_plate'},
           'stainless_vessel', features=['nickel', 'aluminum'])
    shaped('valve_blank', ['FGS'], {'F': 'jugcraft:stainless_fitting', 'G': 'jugcraft:gasket',
                                   'S': 'jugcraft:precision_shaft'}, 'stainless_valve_blank', features=['nickel'])
    shaped('lightweight_frames', ['P P', 'PSP'],
           {'P': '#c:plates/aluminum_magnesium', 'S': 'jugcraft:precision_shaft'},
           'lightweight_frame', 2, ['aluminum'])
    shaped('aluminum_cladding', ['PP', 'PP'], {'P': '#c:plates/aluminum_magnesium'},
           'aluminum_cladding', 6, ['aluminum'])
    shaped('aluminum_propeller_sets', [' P ', 'PSP', ' P '],
           {'P': '#c:plates/aluminum_magnesium', 'S': 'minecraft:stick'},
           'aluminum_propeller_set', 2, ['aluminum'])
    shaped('steel_fluid_pipe', ['FBF'], {'F': 'jugcraft:stainless_fitting', 'B': 'jugcraft:bronze_fluid_pipe'},
           'steel_fluid_pipe', 4, ['nickel'])
    shaped('steel_tank', ['V', 'T'], {'V': 'jugcraft:stainless_vessel', 'T': 'jugcraft:fluid_tank'},
           'steel_tank', features=['nickel', 'aluminum'])
    shaped('bulk_chemical_reactor', ['VFV', 'MCM', 'PKP'],
           {'V': 'jugcraft:stainless_vessel', 'F': 'jugcraft:precision_stainless_valve',
            'M': 'jugcraft:electric_motor', 'C': 'jugcraft:basic_circuit',
            'P': '#c:plates/steel', 'K': 'jugcraft:machine_casing'},
           'bulk_chemical_reactor', features=['nickel', 'aluminum'])
    shaped('lined_fluorochemical_reactor', ['SFL', 'VCQ', 'MKM'],
           {'S': 'jugcraft:ptfe_seal', 'F': 'jugcraft:ptfe_fitting', 'L': 'jugcraft:ptfe_liner',
            'V': 'jugcraft:stainless_vessel', 'C': 'jugcraft:basic_circuit',
            'Q': 'jugcraft:precision_stainless_valve', 'M': 'jugcraft:electric_motor',
            'K': 'jugcraft:machine_casing'}, 'lined_fluorochemical_reactor', features=['nickel', 'aluminum'])
    shaped('zeppelin', ['HHH', 'HEH', 'FCF'],
           {'H': 'jugcraft:hydrogen_lift_cell', 'E': 'jugcraft:diesel_engine',
            'F': 'jugcraft:lightweight_frame', 'C': 'minecraft:chest'}, 'zeppelin', features=['aluminum'])
    shaped('howitzer_walker', ['FDF', 'PWP', 'R R'],
           {'F': 'jugcraft:lightweight_frame', 'D': 'minecraft:dispenser', 'P': 'jugcraft:alloy_panel',
            'W': 'jugcraft:diesel_walker', 'R': 'minecraft:piston'}, 'howitzer_walker', features=['aluminum'])
