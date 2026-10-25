"""Native digital storage resources. Source meshes/UVs/textures/animation sidecars are retained."""
from pathlib import Path
import copy
import hashlib
import json
import shutil
import jimtafacts_network_decor as decor
import jimtafacts_network_fixtures as fixtures
import jimtafacts_network_equipment as equipment
import jimtafacts_network_cabling as cabling
import jimtafacts_network_paint as paint
import jimtafacts_network_utilities as utilities
import jimtafacts_network_exploration as exploration
import jimtafacts_network_quantum as quantum
import jimtafacts_network_facades as facades
import jimtafacts_network_spatial as spatial

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'art/owner-library/originals/Oct23/Jimtafacts/applied energentics reborn by jimbo/assets/ae2'
RES = ROOT / 'src/main/resources'
ASSETS = RES / 'assets/jugcraft'
PREFIX = 'jimtafacts/ae2/'
ORIENTATIONS = [('north', 0, 0), ('east', 0, 90), ('south', 0, 180), ('west', 0, 270), ('up', 270, 0), ('down', 90, 0)]
BLOCKS = ['controller', 'energy_acceptor', 'drive', 'me_chest', 'terminal', 'crafting_terminal', 'interface', 'import_bus', 'export_bus', 'fluix_glass_cable', 'pattern_provider', 'crafting_unit']
WORKSHOPS = ['energy_cell', 'dense_energy_cell', 'vibration_chamber', 'charger', 'inscriber', 'cell_workbench', 'io_port', 'molecular_assembler']
POWERED = ['controller', 'energy_acceptor', 'me_chest', 'energy_cell', 'dense_energy_cell', 'vibration_chamber', 'charger', 'inscriber', 'crank', 'crystal_resonance_generator']
BLOCKS += WORKSHOPS
BLOCKS += ['fluid_terminal', 'storage_bus']
BLOCKS += ['pattern_encoding_terminal']
BLOCKS += ['condenser', 'wireless_access_point']
BLOCKS += ['spatial_anchor']
BLOCKS += ['formation_plane', 'annihilation_plane']
BLOCKS += ['storage_monitor', 'conversion_monitor', 'monitor', 'semi_dark_monitor', 'dark_monitor', 'pattern_access_terminal', 'toggle_bus', 'inverted_toggle_bus']
BLOCKS += ['crafting_monitor']
BLOCKS += [f'{n}k_crafting_storage' for n in (1, 4, 16, 64, 256)] + ['crafting_accelerator']
BLOCKS += [f'{kind}_p2p_tunnel' for kind in ['me', 'item', 'fluid', 'fe', 'redstone', 'light']]
BLOCKS += ['level_emitter', 'energy_level_emitter', 'fluid_import_bus', 'fluid_export_bus', 'fluid_storage_bus']
BLOCKS += decor.BLOCKS
BLOCKS += fixtures.BLOCKS
BLOCKS += cabling.BLOCKS
BLOCKS += paint.BLOCKS
BLOCKS += utilities.BLOCKS
BLOCKS += exploration.BLOCKS
BLOCKS += quantum.BLOCKS
BLOCKS += spatial.BLOCKS
BLOCKS += ['quartz_fiber', 'cable_interface', 'cable_energy_acceptor', 'cable_pattern_provider']
POWERED += ['cable_energy_acceptor']
COMPONENTS = ['certus_quartz_crystal', 'certus_quartz_dust', 'fluix_crystal', 'fluix_dust', 'logic_processor', 'calculation_processor', 'engineering_processor', 'blank_pattern', 'crafting_pattern', 'wireless_terminal', 'wireless_crafting_terminal']
COMPONENTS += ['charged_certus_quartz_crystal', 'logic_processor_press', 'calculation_processor_press', 'engineering_processor_press']
COMPONENTS += ['processing_pattern']
COMPONENTS += quantum.ITEMS
COMPONENTS += spatial.ITEMS
COMPONENTS += ['stonecutting_pattern', 'smithing_table_pattern', 'energy_card', 'void_card', 'view_cell']
COMPONENTS += ['basic_card', 'advanced_card', 'speed_card', 'capacity_card', 'fuzzy_card', 'inverter_card', 'redstone_card', 'crafting_card', 'equal_distribution_card']
COMPONENTS += ['printed_logic_processor', 'printed_calculation_processor', 'printed_engineering_processor', 'printed_silicon', 'silicon_press', 'formation_core', 'annihilation_core', 'item_cell_housing', 'fluid_cell_housing', 'ender_dust', 'sky_dust', 'fluix_pearl', 'wireless_receiver', 'wireless_booster', 'singularity']
ITEMS = COMPONENTS + [f'cell_component_{n}k' for n in (1, 4, 16, 64, 256)] + [f'{kind}_storage_cell_{n}k' for kind in ('item', 'fluid') for n in (1, 4, 16, 64, 256)]
ITEMS += [f'portable_{kind}_cell_{n}k' for kind in ('item', 'fluid') for n in (1, 4, 16, 64, 256)]
ITEMS += ['creative_storage_cell']  # Creative-only original item; deliberately no acquisition recipe.

def ids():
    return ['jimta_' + n for n in BLOCKS + ITEMS + equipment.ITEMS + paint.ITEMS + exploration.ITEMS + facades.ITEMS]

def dump(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')

def generate():
    copies = []
    done = set()
    def texture(ref):
        if not ref.startswith('ae2:'):
            return ref
        path = ref.split(':', 1)[1]
        for suffix in ('.png', '.png.mcmeta'):
            src = SOURCE / ('textures/' + path + suffix)
            if suffix == '.png' and not src.is_file():
                raise FileNotFoundError(src)
            if src.is_file():
                dest = ASSETS / ('textures/' + PREFIX + path + suffix)
                dest.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(src, dest)
                copies.append({'source': src.relative_to(ROOT).as_posix(), 'runtime': dest.relative_to(ROOT).as_posix(), 'sha256': hashlib.sha256(src.read_bytes()).hexdigest(), 'changes': 'None; byte-identical texture/animation sidecar.'})
        return 'jugcraft:' + PREFIX + path

    def model(path):
        target = 'jugcraft:' + PREFIX + path
        if path in done:
            return target
        done.add(path)
        if path.startswith('native/plane_'):
            name, mode, mask = path.removeprefix('native/plane_').rsplit('_', 2)
            mask = int(mask)
            descriptor = SOURCE / f'ae2/parts/{name}.json'
            spec = json.loads(descriptor.read_text(encoding='utf-8'))['model']['models'][1]
            textures = {'front': texture(spec['front_on' if mode == 'on' else 'front_off']), 'back': texture(spec['back']), 'sides': texture(spec['sides'])}
            # PlanePartModel.buildQuads' disconnected 1..15 panel, one pixel thick at the actual cable face.
            faces = {face: {'texture': '#' + ('front' if face == 'north' else 'back' if face == 'south' else 'sides')} for face in ('north','south','east','west','up','down')}
            dest = ASSETS / ('models/' + PREFIX + path + '.json')
            dump(dest, {'textures': {**textures, 'particle': '#front'}, 'elements': [{'from': [0 if mask & 1 else 1,0 if mask & 4 else 1,0], 'to': [16 if mask & 2 else 15,16 if mask & 8 else 15,1], 'faces': faces}]})
            copies.append({'source': descriptor.relative_to(ROOT).as_posix(), 'runtime': dest.relative_to(ROOT).as_posix(), 'changes': 'Original PlanePartModel one-pixel panel, all 16 adjacent-edge permutations and supplied texture selection adapted to native JSON.'})
            return target
        src = SOURCE / ('models/' + path + '.json')
        data = json.loads(src.read_text(encoding='utf-8'))
        if 'loader' in data:
            raise ValueError(f'Unsupported dynamic model needs an explicit adapter: {src}')
        if data.get('parent', '').startswith('ae2:'):
            data['parent'] = model(data['parent'].split(':', 1)[1])
        data['textures'] = {k: texture(v) for k, v in data.get('textures', {}).items()}
        # NeoForge light metadata is platform-specific; preserve mesh coordinates and UVs.
        # Native model emission is attached to each emitting element, keeping supplied animated textures.
        for element in data.get('elements', []):
            for face in element.get('faces', {}).values():
                glow = face.pop('neoforge_data', None)
                if glow and glow.get('block_light', 0):
                    element['light_emission'] = max(element.get('light_emission', 0), glow['block_light'])
                face.pop('tintindex', None)  # Default transparent cable/network palette uses the authored texture colors.
                if face.get('texture') in data.get('textures', {}): face['texture'] = '#' + face['texture']
        data.pop('render_type', None)
        dest = ASSETS / ('models/' + PREFIX + path + '.json')
        from jimtafacts_model_repairs import repair, REPAIRED, CLOSED
        dump(dest, repair(PREFIX+path, data))
        copies.append({'source': src.relative_to(ROOT).as_posix(), 'runtime': dest.relative_to(ROOT).as_posix(), 'changes': 'Namespace references and native emission metadata; original elements and pixels retained.' + (' Audited overlapping faces separated and exposed box faces closed for the native renderer, UVs pinned.' if PREFIX+path in REPAIRED | CLOSED else ' Geometry and UVs unchanged.')})
        return target

    sources = {
        'controller': [('block/controller_block_offline', None), ('block/controller_block_online', True)],
        'energy_acceptor': [('block/energy_acceptor', None)],
        'drive': [('block/drive_base', None)],
        'me_chest': [('block/me_chest_base', None), ('block/me_chest_lights_on', True), ('block/me_chest_lights_off', False)],
        'interface': [('block/interface', None)],
        'pattern_provider': [('block/pattern_provider', None)],
        'crafting_unit': [('block/crafting/unit', None)],
        'crafting_monitor': [('block/crafting/monitor', None)],
        'terminal': [('part/display_base', None), ('part/terminal_on', True), ('part/terminal_off', False)],
        'crafting_terminal': [('part/display_base', None), ('part/crafting_terminal_on', True), ('part/crafting_terminal_off', False)],
        # This source version has one shared terminal mesh for item/fluid views.
        'fluid_terminal': [('part/display_base', None), ('part/terminal_on', True), ('part/terminal_off', False)],
        'import_bus': [('part/import_bus_base', None), ('part/import_bus_on', True), ('part/import_bus_off', False)],
        'export_bus': [('part/export_bus_base', None), ('part/export_bus_on', True), ('part/export_bus_off', False)],
        'storage_bus': [('part/storage_bus_base', None), ('part/storage_bus_on', True), ('part/storage_bus_off', False)],
        'pattern_encoding_terminal': [('part/display_base', None), ('part/pattern_encoding_terminal_on', True), ('part/pattern_encoding_terminal_off', False)],
        'condenser': [('block/condenser', None)],
        'spatial_anchor': [('block/spatial_anchor', False), ('block/spatial_anchor_on', True)],
        'formation_plane': [('native/plane_formation_plane_off', False), ('native/plane_formation_plane_on', True), ('part/transition_plane_has_channel', True), ('part/transition_plane_off', False)],
        'annihilation_plane': [('native/plane_annihilation_plane_off', False), ('native/plane_annihilation_plane_on', True), ('part/transition_plane_has_channel', True), ('part/transition_plane_off', False)],
        'wireless_access_point': [('block/wireless_access_point_chassis', None), ('block/wireless_access_point_off', False), ('block/wireless_access_point_on', True), ('block/wireless_access_point_status_off', False), ('block/wireless_access_point_status_on', True)],
    }
    for name in WORKSHOPS:
        sources[name] = [('block/' + name + ('_0' if name.endswith('energy_cell') else ''), None)]
    for n in (1, 4, 16, 64, 256): sources[f'{n}k_crafting_storage'] = [(f'block/crafting/{n}k_storage', None)]
    sources['crafting_accelerator'] = [('block/crafting/accelerator', None)]
    sources['quartz_fiber'] = [('part/quartz_fiber', None)]
    sources['cable_energy_acceptor'] = [('part/energy_acceptor', None)]
    for part, base in [('cable_interface', 'interface_base'), ('cable_pattern_provider', 'pattern_provider_base')]:
        sources[part] = [('part/' + base, None), ('part/interface_has_channel', True), ('part/interface_off', False)]
    for kind in ['me', 'item', 'fluid', 'fe', 'redstone', 'light']:
        # Typed model already inherits the complete base. Rendering both makes coincident faces flicker.
        sources[kind + '_p2p_tunnel'] = [('part/p2p_tunnel_' + kind, None), ('part/p2p_tunnel_status_on', True), ('part/p2p_tunnel_status_off', False)]
    for name in ['import_bus', 'export_bus', 'storage_bus']:
        sources['fluid_' + name] = sources[name]
    for name in ['vibration_chamber', 'io_port']:
        sources[name] = [('block/' + name, False), ('block/' + name + '_on', True)]
    sources['molecular_assembler'] = [('block/molecular_assembler', None), ('block/molecular_assembler_lights', True)]
    sources['pattern_access_terminal'] = [('part/display_base', None), ('part/pattern_access_terminal_on', True), ('part/pattern_access_terminal_off', False)]
    for name in ['storage_monitor', 'conversion_monitor']:
        sources[name] = [('part/display_base', None), ('part/' + name + '_on', True), ('part/' + name + '_off', False)]
    for name, shade in [('monitor', 'bright'), ('semi_dark_monitor', 'medium'), ('dark_monitor', 'dark')]:
        sources[name] = [('part/monitor_base', None), ('part/monitor_' + shade + '_on', True), ('part/monitor_' + shade + '_off', False)]
    for name in ['toggle_bus', 'inverted_toggle_bus']:
        sources[name] = [('part/' + name + '_base', None), ('part/toggle_bus_status_on', True), ('part/toggle_bus_status_off', False)]
    for name, parts in sources.items():
        multipart = []
        for source, lit in parts:
            # An online controller replaces the offline base rather than overlaying it.
            if name == 'controller' and lit is None:
                lit = False
            masks = range(16) if source.startswith('native/plane_') else [None]
            for mask in masks:
                ref = model(source + '_' + str(mask) if mask is not None else source)
                for facing, pitch, angle in ORIENTATIONS:
                    when = {'facing': facing}
                    if lit is not None:
                        when['lit'] = str(lit).lower()
                    if mask is not None:
                        when.update({key: str(bool(mask & 1 << bit)).lower() for bit, key in enumerate(['join_low_x','join_high_x','join_low_y','join_high_y'])})
                    multipart.append({'when': when, 'apply': {'model': ref, 'x': pitch, 'y': angle}})
        dump(ASSETS / f'blockstates/jimta_{name}.json', {'multipart': multipart})
        source = parts[0][0]
        if not name.endswith('_plane'):
            dump(ASSETS / f'items/jimta_{name}.json', {'model': {'type': 'minecraft:composite', 'models': [{'type': 'minecraft:model', 'model': model(p)} for p, lit in parts if lit is None or lit is False]}})
    for name in ['formation_plane', 'annihilation_plane']:
        dump(ASSETS / f'items/jimta_{name}.json', {'model': {'type': 'minecraft:model', 'model': model('item/' + name)}})
    for name in ['energy_cell', 'dense_energy_cell']:
        multipart = []
        for charge in range(5):
            ref = model(f'block/{name}_{charge}')
            for facing, pitch, angle in ORIENTATIONS:
                multipart.append({'when': {'facing': facing, 'charge': str(charge)}, 'apply': {'model': ref, 'x': pitch, 'y': angle}})
        dump(ASSETS / f'blockstates/jimta_{name}.json', {'multipart': multipart})
    for name in ['level_emitter', 'energy_level_emitter']:
        parts = []
        for facing, pitch, angle in ORIENTATIONS:
            for signal in [False, True]:
                parts.append({'when': {'facing': facing, 'signal': str(signal).lower()}, 'apply': {'model': model('part/level_emitter_base_' + ('on' if signal else 'off')), 'x': pitch, 'y': angle}})
            for lit in [False, True]:
                parts.append({'when': {'facing': facing, 'lit': str(lit).lower()}, 'apply': {'model': model('part/level_emitter_status_' + ('on' if lit else 'off')), 'x': pitch, 'y': angle}})
        dump(ASSETS / f'blockstates/jimta_{name}.json', {'multipart': parts})
        dump(ASSETS / f'items/jimta_{name}.json', {'model': {'type': 'minecraft:model', 'model': model('item/' + name)}})
    core = model('part/cable/glass/center')
    arm = model('part/cable/glass/connection')
    multipart = [{'apply': {'model': core}}]
    for direction, x, y in [('north', 0, 0), ('east', 0, 90), ('south', 0, 180), ('west', 0, 270), ('up', 270, 0), ('down', 90, 0)]:
        multipart.append({'when': {direction: 'true'}, 'apply': {'model': arm, 'x': x, 'y': y}})
    dump(ASSETS / 'blockstates/jimta_fluix_glass_cable.json', {'multipart': multipart})
    for name in ITEMS + ['fluix_glass_cable']:
        dump(ASSETS / f'items/jimta_{name}.json', {'model': {'type': 'minecraft:model', 'model': model('item/' + name)}})

    language = json.loads((ASSETS / 'lang/en_us.json').read_text(encoding='utf-8'))
    language['encyclopedia.jugcraft.page.jimta_network'] = 'Digital Storage Networks'
    source_lang = json.loads((SOURCE / 'lang/en_us.json').read_text(encoding='utf-8'))
    for name in BLOCKS + ITEMS + equipment.ITEMS + paint.ITEMS + exploration.ITEMS + facades.ITEMS:
        key = ('block' if name in BLOCKS else 'item')
        language[f'{key}.jugcraft.jimta_{name}'] = source_lang.get(f'{key}.ae2.{name}', name.replace('_', ' ').title())
    dump(ASSETS / 'lang/en_us.json', language)
    def recipe(name, data):
        data['fabric:load_conditions'] = [{'condition': 'jugcraft:feature_enabled', 'feature': 'machines'}]
        dump(RES / ('data/jugcraft/recipe/jimtafacts/network/' + name + '.json'), data)
    def shaped(name, pattern, key, count=1):
        recipe(name, {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pattern, 'key': key, 'result': {'id': 'jugcraft:jimta_' + name, 'count': count}})
    def shapeless(name, ingredients, count=1):
        recipe(name, {'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'ingredients': ingredients, 'result': {'id': 'jugcraft:jimta_' + name, 'count': count}})
    def ji(name): return 'jugcraft:jimta_' + name
    # Consolidated processing feeds existing silicon/electronics machinery rather than a duplicate power/material economy.
    shapeless('certus_quartz_crystal', ['minecraft:quartz', 'minecraft:amethyst_shard'], 2)
    shapeless('fluix_crystal', [ji('certus_quartz_crystal'), 'minecraft:quartz', 'minecraft:redstone'], 2)
    for output, source in [('ender_dust', 'minecraft:ender_pearl'), ('sky_dust', ji('sky_stone_block'))]:
        recipe(output, {'type': 'jugcraft:crushing', 'ingredient': source, 'result': {'id': ji(output)}, 'time': 100})
    for kind, quartz in [('formation', 'certus_quartz_crystal'), ('annihilation', 'fluix_crystal')]:
        shapeless(kind + '_core', [ji(quartz), ji('fluix_dust'), ji('logic_processor')], 2)
    shaped('silicon_press', ['III', 'ISI', 'III'], {'I': 'minecraft:iron_ingot', 'S': 'jugcraft:silicon'})
    shaped('fluix_pearl', ['FFF', 'FEF', 'FFF'], {'F': ji('fluix_dust'), 'E': 'minecraft:ender_pearl'})
    shaped('wireless_receiver', ['F', 'P', 'I'], {'F': ji('fluix_pearl'), 'P': ji('quartz_glass'), 'I': 'minecraft:iron_ingot'})
    shaped('wireless_booster', ['EQE', 'IWI'], {'E': ji('ender_dust'), 'Q': ji('certus_quartz_dust'), 'I': 'minecraft:iron_ingot', 'W': ji('fluix_glass_cable')})
    shaped('spatial_anchor', ['IFI', 'ECE', 'IPI'], {'I': 'minecraft:iron_ingot', 'F': ji('fluix_pearl'), 'E': 'minecraft:ender_pearl', 'C': ji('spatial_cell_component_16'), 'P': ji('engineering_processor')})
    for kind in ['item', 'fluid']:
        shaped(kind + '_cell_housing', ['IGI', 'R R', 'III'], {'I': 'minecraft:iron_ingot', 'G': ji('quartz_glass'), 'R': 'minecraft:bucket' if kind == 'fluid' else 'minecraft:redstone'})
    recipe('fluix_crystal_charged', {'type': 'minecraft:crafting_shapeless', 'ingredients': [ji('charged_certus_quartz_crystal'), 'minecraft:quartz', 'minecraft:redstone'], 'result': {'id': ji('fluix_crystal'), 'count': 4}})
    for crystal in ('certus_quartz', 'fluix'):
        recipe(crystal + '_dust', {'type': 'jugcraft:crushing', 'ingredient': ji(crystal + '_crystal'), 'result': {'id': ji(crystal + '_dust')}, 'time': 100})
    for name, metal in [('logic_processor', 'gold_ingot'), ('calculation_processor', 'iron_ingot'), ('engineering_processor', 'diamond')]:
        recipe(name, {'type': 'jugcraft:circuit_assembly', 'ingredients': [{'ingredient': 'jugcraft:silicon'}, {'ingredient': 'minecraft:' + metal}, {'ingredient': 'minecraft:redstone'}], 'result': {'id': ji(name)}, 'time': 200})
    shaped('cell_component_1k', ['RQR', 'QLQ', 'RQR'], {'R': 'minecraft:redstone', 'Q': ji('certus_quartz_crystal'), 'L': ji('logic_processor')})
    for lower, upper in zip((1, 4, 16, 64), (4, 16, 64, 256)):
        shaped(f'cell_component_{upper}k', ['RLR', 'CSC', 'RCR'], {'R': 'minecraft:redstone', 'L': ji('calculation_processor'), 'C': ji(f'cell_component_{lower}k'), 'S': 'minecraft:quartz'})
    for size in (1, 4, 16, 64, 256):
        for kind in ['item', 'fluid']:
            shapeless(f'{kind}_storage_cell_{size}k_from_housing', [ji(kind + '_cell_housing'), ji(f'cell_component_{size}k')])
            housing_recipe = RES / f'data/jugcraft/recipe/jimtafacts/network/{kind}_storage_cell_{size}k_from_housing.json'
            housing_data = json.loads(housing_recipe.read_text(encoding='utf-8')); housing_data['result']['id'] = ji(f'{kind}_storage_cell_{size}k'); dump(housing_recipe, housing_data)
        shaped(f'item_storage_cell_{size}k', ['IGI', 'RCR', 'III'], {'I': 'minecraft:iron_ingot', 'G': 'minecraft:glass', 'R': 'minecraft:redstone', 'C': ji(f'cell_component_{size}k')})
        shaped(f'fluid_storage_cell_{size}k', ['IGI', 'BCB', 'III'], {'I': 'minecraft:iron_ingot', 'G': 'minecraft:glass', 'B': 'minecraft:bucket', 'C': ji(f'cell_component_{size}k')})
        for kind in ['item', 'fluid']:
            shaped(f'portable_{kind}_cell_{size}k', ['GLG', 'PCP', 'III'], {'I': 'minecraft:iron_ingot', 'G': 'minecraft:glass', 'L': ji('logic_processor'), 'P': 'minecraft:bucket' if kind == 'fluid' else 'minecraft:redstone', 'C': ji(f'cell_component_{size}k')})
    shaped('fluix_glass_cable', ['FGF'], {'F': ji('fluix_crystal'), 'G': 'minecraft:glass'}, 4)
    common = {'I': 'minecraft:iron_ingot', 'F': ji('fluix_crystal'), 'L': ji('logic_processor'), 'E': ji('engineering_processor'), 'G': 'minecraft:glass', 'C': ji('fluix_glass_cable'), 'H': 'minecraft:chest', 'Q': ji('certus_quartz_crystal'), 'R': 'minecraft:redstone', 'P': 'minecraft:piston', 'B': 'minecraft:crafting_table'}
    for name, pattern in {
        'controller': ['IFI', 'FEF', 'IFI'], 'energy_acceptor': ['IGI', 'GFG', 'IGI'], 'drive': ['ILI', 'CHC', 'ILI'],
        'me_chest': ['IGI', 'CHC', 'ILI'], 'terminal': ['GLG', 'RFR', ' C '], 'interface': ['IGI', 'LHL', 'ICI'],
        'import_bus': ['IQI', ' P ', ' C '], 'export_bus': ['ILI', ' P ', ' C '],
        'pattern_provider': ['ILI', 'BHB', 'ICI'], 'crafting_unit': ['IEI', 'LFL', 'ICI'],
    }.items():
        used = set(''.join(pattern)) - {' '}
        shaped(name, pattern, {k: common[k] for k in sorted(used)})
    shapeless('crafting_terminal', [ji('terminal'), ji('calculation_processor'), 'minecraft:crafting_table'])
    shapeless('pattern_encoding_terminal', [ji('crafting_terminal'), ji('engineering_processor'), ji('blank_pattern')])
    shapeless('crafting_monitor', [ji('crafting_unit'), ji('terminal')])
    for n in (1, 4, 16, 64, 256): shapeless(f'{n}k_crafting_storage', [ji('crafting_unit'), ji(f'cell_component_{n}k')])
    shapeless('crafting_accelerator', [ji('crafting_unit'), ji('engineering_processor'), ji('calculation_processor')])
    for kind, ingredient in [('me', ji('fluix_crystal')), ('item', 'minecraft:hopper'), ('fluid', 'minecraft:bucket'), ('fe', 'minecraft:copper_ingot'), ('redstone', 'minecraft:redstone'), ('light', 'minecraft:glowstone_dust')]:
        shaped(kind + '_p2p_tunnel', ['III', 'EGE', ' C '], {'I': 'minecraft:iron_ingot', 'E': ji('engineering_processor'), 'G': ingredient, 'C': ji('fluix_glass_cable')})
    shaped('basic_card', ['RLI', 'RII'], {'R': 'minecraft:redstone', 'L': ji('logic_processor'), 'I': 'minecraft:iron_ingot'})
    shapeless('advanced_card', [ji('basic_card'), 'minecraft:diamond', ji('engineering_processor')])
    shapeless('energy_card', [ji('advanced_card'), ji('fluix_crystal'), 'minecraft:redstone'])
    shapeless('void_card', [ji('basic_card'), 'minecraft:lava_bucket'])
    shaped('view_cell', ['IGI', 'RLR', 'III'], {'I':'minecraft:iron_ingot','G':ji('quartz_glass'),'R':'minecraft:redstone','L':ji('logic_processor')})
    for name, ingredient, base in [('speed', 'minecraft:sugar', 'advanced'), ('capacity', 'minecraft:quartz', 'basic'), ('fuzzy', 'minecraft:white_wool', 'advanced'), ('inverter', 'minecraft:redstone_torch', 'basic'), ('redstone', 'minecraft:redstone', 'basic'), ('crafting', 'minecraft:crafting_table', 'basic'), ('equal_distribution', 'minecraft:comparator', 'advanced')]:
        shapeless(name + '_card', [ji(base + '_card'), ingredient])
    shapeless('pattern_access_terminal', [ji('terminal'), ji('engineering_processor'), ji('pattern_provider')])
    shapeless('storage_monitor', [ji('terminal'), ji('level_emitter')])
    shapeless('conversion_monitor', [ji('storage_monitor'), ji('formation_core'), ji('annihilation_core')])
    for name, dye in [('monitor', 'white'), ('semi_dark_monitor', 'gray'), ('dark_monitor', 'black')]:
        shapeless(name, [ji('quartz_glass'), 'minecraft:' + dye + '_dye', 'minecraft:redstone'])
    shaped('toggle_bus', ['CL', 'CP'], {'C': ji('fluix_glass_cable'), 'L': 'minecraft:lever', 'P': 'minecraft:piston'})
    shapeless('inverted_toggle_bus', [ji('toggle_bus'), 'minecraft:redstone_torch'])
    shapeless('fluid_terminal', [ji('terminal'), ji('calculation_processor'), 'minecraft:bucket'])
    shapeless('storage_bus', [ji('interface'), 'minecraft:sticky_piston', ji('fluix_glass_cable')])
    for name in ['import_bus', 'export_bus', 'storage_bus']:
        shapeless('fluid_' + name, [ji(name), 'minecraft:bucket'])
    shaped('level_emitter', [' LR', ' C '], {'L': ji('logic_processor'), 'R': 'minecraft:redstone_torch', 'C': ji('fluix_glass_cable')})
    shapeless('energy_level_emitter', [ji('level_emitter'), ji('fluix_crystal')])
    shaped('blank_pattern', ['IGI', 'GQG', 'IRI'], {'I': 'minecraft:iron_ingot', 'G': 'minecraft:glass', 'Q': ji('certus_quartz_crystal'), 'R': 'minecraft:redstone'})
    shaped('wireless_terminal', ['IFI', 'ITI', 'IEI'], {'I': 'minecraft:iron_ingot', 'F': ji('fluix_crystal'), 'T': ji('terminal'), 'E': ji('engineering_processor')})
    shapeless('wireless_crafting_terminal', [ji('wireless_terminal'), ji('calculation_processor'), 'minecraft:crafting_table'])
    shaped('wireless_access_point', [' W ', 'ICI'], {'W': ji('wireless_receiver'), 'I': 'minecraft:iron_ingot', 'C': ji('calculation_processor')})
    for name in ['interface', 'energy_acceptor', 'pattern_provider']:
        recipe('cable_' + name, {'type': 'jugcraft:jimta_compact_part', 'input': name, 'output': 'cable_' + name})
        recipe(name + '_from_cable', {'type': 'jugcraft:jimta_compact_part', 'input': 'cable_' + name, 'output': name})
    shaped('quartz_fiber', ['GGG', 'QQQ', 'GGG'], {'G': 'minecraft:glass', 'Q': ji('certus_quartz_dust')}, 3)
    shaped('condenser', ['IGI', 'GHG', 'IGI'], {'I': 'minecraft:iron_ingot', 'G': ji('quartz_glass'), 'H': ji('fluix_pearl')})
    for kind in ['formation', 'annihilation']:
        shaped(kind + '_plane', ['III', 'FFF', ' C '], {'I': 'minecraft:iron_ingot', 'F': ji('fluix_crystal'), 'C': ji(kind + '_core')})
    for kind, metal in [('logic', 'gold_ingot'), ('calculation', 'iron_ingot'), ('engineering', 'diamond')]:
        shaped(kind + '_processor_press', ['III', 'IMI', 'III'], {'I': 'minecraft:iron_ingot', 'M': 'minecraft:' + metal})
    for name, pattern in {
        'energy_cell': ['FQF', 'QRQ', 'FQF'], 'dense_energy_cell': ['CCC', 'CEC', 'CCC'],
        'vibration_chamber': ['IFI', 'LHL', 'ICI'], 'charger': ['IFI', '  F', 'IFI'],
        'inscriber': ['IPI', 'F F', 'IPI'], 'cell_workbench': ['IQI', 'LHL', 'ICI'],
        'io_port': ['IGI', 'LHL', 'ICI'], 'molecular_assembler': ['IPI', 'BEB', 'IFI'],
    }.items():
        key = dict(common)
        if name == 'dense_energy_cell': key['C'] = ji('energy_cell')
        if name == 'vibration_chamber': key['H'] = 'minecraft:furnace'
        if name == 'cell_workbench': key['H'] = 'minecraft:crafting_table'
        if name == 'io_port': key['H'] = ji('drive')
        shaped(name, pattern, {k: key[k] for k in sorted(set(''.join(pattern)) - {' '})})
    for name in BLOCKS:
        entry = {'type': 'minecraft:item', 'name': ji(name)}
        if name in POWERED:
            entry['modifier'] = {'type': 'minecraft:copy_components', 'source': 'block_entity', 'include': ['jugcraft:jimta_stored_energy']}
        if name == 'condenser': entry['modifier'] = {'type': 'minecraft:copy_components', 'source': 'block_entity', 'include': ['jugcraft:jimta_condensed_matter']}
        dump(RES / f'data/jugcraft/loot_table/blocks/jimta_{name}.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [entry], 'condition': {'type': 'minecraft:survives_explosion'}}]})
    decor.generate(SOURCE, ASSETS, RES, model, dump, recipe)
    fixtures.generate(SOURCE, ASSETS, RES, model, texture, dump, recipe)
    equipment.generate(SOURCE, ASSETS, RES, model, texture, dump, recipe)
    paint.generate(SOURCE, ASSETS, RES, model, texture, dump, recipe)
    utilities.generate(SOURCE, ASSETS, RES, model, texture, dump, recipe)
    exploration.generate(SOURCE, ASSETS, RES, model, texture, dump, recipe)
    quantum.generate(SOURCE, ASSETS, RES, model, texture, dump, recipe)
    facades.generate(SOURCE, ASSETS, RES, model, texture, dump, recipe)
    spatial.generate(SOURCE, ASSETS, RES, model, texture, dump, recipe)
    copies.extend(cabling.generate(SOURCE, ASSETS, ROOT, model, texture, dump, recipe))
    import jimtafacts_network_rendering
    jimtafacts_network_rendering.generate(SOURCE, ASSETS, model, dump)
    import jimtafacts_ae_registry
    jimtafacts_ae_registry.generate(SOURCE, ROOT, BLOCKS + ITEMS + equipment.ITEMS + paint.ITEMS + exploration.ITEMS + facades.ITEMS)
    tag = RES / 'data/minecraft/tags/block/mineable/pickaxe.json'
    data = json.loads(tag.read_text(encoding='utf-8'))
    data['values'] = list(dict.fromkeys(data['values'] + [ji(n) for n in BLOCKS])); dump(tag, data)
    unique = {x['runtime']: x for x in copies}
    dump(ROOT / 'art/owner-library/catalog/edited-2026-10-23-jimtafacts/network-runtime-copies.json', list(unique.values()))
    return list(unique.values())

if __name__ == '__main__':
    generate()
