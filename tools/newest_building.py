"""Native building/painting resources from the preserved October 29 collection.

Original models and pixel data are retained. Only resource/registry references
and the item-definition schema are adapted; no supplied program is executed.
"""
from pathlib import Path
import json
import shutil
from menagerie_content import read

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'art/owner-library/originals/Edited-2026-10-29'
RES = ROOT / 'src/main/resources'
ASSET = RES / 'assets/jugcraft'
CAT = ROOT / 'art/owner-library/catalog/edited-2026-10-29'
FAMILIES = {'stoneworks': ('stoneworks', 'stoneworks'), 'paths': ('mcwpaths', 'pathworks'),
            'doors': ('mcwdoors', 'doorworks'), 'blocks': ('factory_blocks', 'factoryworks'),
            'fences': ('mcwfences', 'fenceworks'), 'fix': ('chisel', 'chiselworks'),
            'bridge': ('mcwbridges', 'bridgeworks'), 'windows': ('mcwwindows', 'windowworks'),
            'stairs': ('mcwstairs', 'stairworks'), 'lights': ('mcwlights', 'lightworks'),
            'furniture': ('mcwfurnitures', 'furnitureworks'), 'holiday': ('mcwholidays', 'holidayworks'),
            'twig': ('twigs', 'twigworks'), 'home': ('cozyhome', 'homeworks')}

# Registry declarations in the supplied reference distinguish actual blocks
# from the three engineer presentation variants which have no item or registry.
FACTORY_PRESENTATION_ONLY = {'engineer1', 'engineer2', 'engineer3'}


def repair_unused_faces(data):
    """Drop only zero-area placeholder faces; repair the vat faucet's real underside."""
    if not isinstance(data, dict): return data
    for element in data.get('elements', []):
        sizes = [b-a for a,b in zip(element.get('from', []),element.get('to', []))]
        for side,face in list(element.get('faces', {}).items()):
            if face.get('texture') != '#missing': continue
            axes = {'north':(0,1),'south':(0,1),'east':(1,2),'west':(1,2),'up':(0,2),'down':(0,2)}[side]
            if len(sizes)==3 and any(sizes[a]==0 for a in axes): del element['faces'][side]
            elif element.get('name')=='faucet_base' and '5' in data.get('textures',{}): face['texture']='#5'
    return data


def write(path, data):
    if '/loot_table/' in str(path).replace('\\','/'):
        from menagerie_data import loot as native_loot
        data=native_loot(data)
    if '/models/newest/homeworks/' in str(path).replace('\\','/'): data=repair_unused_faces(data)
    # Repair four supplied faces which otherwise sample adjacent sprites in the atlas.
    if any(str(path).replace('\\', '/').endswith('/models/newest/' + name + '.json') for name in (
        'holidayworks/block/hay_wheelbarrow', 'holidayworks/block/standing_broomstick',
        'lightworks/block/upgraded_torch', 'lightworks/block/upgraded_torch_wall')):
        from model_writer import fit_uvs
        fit_uvs(data.get('elements', []))
    path.parent.mkdir(parents=True, exist_ok=True)
    # This large runtime descriptor exceeds GitHub's file limit when indented.
    # Keep the identical JSON data compact in both standalone and shared generation.
    options = {'separators': (',', ':')} if path == RES / 'jugcraft/newest_building.json' else {'indent': 2}
    text = json.dumps(data, ensure_ascii=True, **options) + '\n'
    if not path.exists() or path.read_text(encoding='utf-8') != text:
        path.write_text(text, encoding='utf-8')


def map_value(value, ns, prefix, resource=False):
    if isinstance(value, str):
        if value == 'minecraft:chain': return 'minecraft:iron_chain'  # 26.3 registry rename.
        if resource and value == 'minecraft:block/air': return 'jugcraft:newest/empty'
        if value.startswith('#'): return '#' + map_value(value[1:], ns, prefix, resource)
        if value.startswith(ns + ':'):
            tail = value.split(':', 1)[1]
            return 'jugcraft:' + ('newest/' + prefix + '/' + tail if resource else prefix + '_' + tail)
        return value
    if isinstance(value, list): return [map_value(v, ns, prefix, resource) for v in value]
    if isinstance(value, dict): return {k: map_value(v, ns, prefix, resource) for k, v in value.items()}
    return value


def copy(source, target):
    target.parent.mkdir(parents=True, exist_ok=True)
    if not target.exists() or target.read_bytes() != source.read_bytes(): shutil.copyfile(source, target)


def merge_tag(path, entries, writer):
    data = read(path) if path.exists() else {'values': []}
    for value in entries:
        if value not in data['values']: data['values'].append(value)
    writer(path, data)


def kind_of(name, state):
    keys = ','.join(state.get('variants', {}))
    if 'part=' in keys: return 'garage'
    if 'hinge=' in keys: return 'door'
    if 'engraved=' in keys: return 'engraved'
    if 'flattened=' in keys: return 'flattened'
    if 'shape=' in keys: return 'stairs'
    if 'type=bottom' in keys: return 'slab'
    if 'axis=' in keys: return 'pillar'
    if 'multipart' in state: return 'wall'
    if 'facing=' in keys: return 'facing_path'
    return 'path' if name.endswith('_path') or name.endswith('_paving') else 'block'


def state_properties(state):
    props = {}
    for variant in state.get('variants', {}):
        for pair in variant.split(','):
            if '=' in pair:
                key, value = pair.split('='); props.setdefault(key, set()).update(value.split('|'))
    def conditions(value):
        if isinstance(value, dict):
            for key, child in value.items():
                if isinstance(child, (str, int, bool)): props.setdefault(key, set()).update(str(child).lower().split('|'))
                else: conditions(child)
        elif isinstance(value, list):
            for child in value: conditions(child)
    def visit(value):
        if isinstance(value, dict):
            for key, child in value.items():
                if key == 'when':
                    conditions(child)
                visit(child)
        elif isinstance(value, list):
            for child in value: visit(child)
    visit(state)
    # Multipart models often name only the true case. The absent branch is
    # still a real boolean state (e.g. Gothic windows with/without iron bars).
    for values in props.values():
        if values and values <= {'true','false'}: values.update({'true','false'})
    return props


def model_references(value, namespace):
    if isinstance(value, dict):
        for key, child in value.items():
            if key in ('model', 'parent') and isinstance(child, str) and child.startswith(namespace + ':'):
                yield child.split(':', 1)[1]
            yield from model_references(child, namespace)
    elif isinstance(value, list):
        for child in value: yield from model_references(child, namespace)


def missing_models(assets, namespace, value):
    pending, visited, missing = list(model_references(value, namespace)), set(), []
    while pending:
        model = pending.pop()
        if model in visited: continue
        visited.add(model)
        source = assets / 'models' / (model + '.json')
        if namespace == 'cozyhome':
            from newest_home import supplied_model
            value = supplied_model(assets, model)
            if value is None: missing.append(namespace + ':' + model)
            else: pending.extend(model_references(value, namespace))
            continue
        if not source.is_file():
            missing.append(namespace + ':' + model)
        else:
            pending.extend(model_references(read(source), namespace))
    return sorted(missing)


def prune_unregistered(prefix, allowed):
    # These paths contain this importer's generated copies only. Never touch originals.
    registered = {'jugcraft:' + prefix + '_' + name for name in allowed}
    for folder in (ASSET / 'items', ASSET / 'blockstates', RES / 'data/jugcraft/loot_table/blocks'):
        for path in folder.rglob('*.json'):
            relative = path.relative_to(folder).as_posix()[:-5]
            if not relative.startswith(prefix + '_'): continue
            if 'jugcraft:' + relative not in registered:
                if not path.resolve().is_relative_to(folder.resolve()): raise ValueError('Unsafe generated path')
                path.unlink()
    recipe_root = RES / 'data/jugcraft/recipe/newest' / prefix
    for path in recipe_root.rglob('*.json'):
        result = read(path).get('result', {})
        ident = result.get('id', result.get('item', '')) if isinstance(result, dict) else result
        if ident.startswith('jugcraft:' + prefix + '_') and ident not in registered:
            if not path.resolve().is_relative_to(recipe_root.resolve()): raise ValueError('Unsafe generated recipe')
            path.unlink()
    for path in (RES / 'data').glob('*/tags/**/*.json'):
        text = path.read_text(encoding='utf-8')
        if 'jugcraft:' + prefix + '_' not in text: continue
        tag = json.loads(text)
        def keep(entry):
            ident = entry if isinstance(entry, str) else entry.get('id', '')
            return not ident.startswith('jugcraft:' + prefix + '_') or ident in registered
        values = [entry for entry in tag.get('values', []) if keep(entry)]
        if values != tag.get('values', []):
            tag['values'] = values; write(path, tag)


def generate(writer=write):
    from menagerie_data import loot as native_loot
    original_writer = writer
    def writer(path, data):
        # 26.3 uses condition/modifier codecs, including for manually assembled
        # architecture drops. Convert once at the common output boundary.
        original_writer(path, native_loot(data) if '/loot_table/' in path.as_posix() else data)
    blocks, items, language, omitted, provenance = {}, {}, {}, [], []
    holiday_behaviors = read(CAT / 'holiday-behaviors.json')['entries']
    twig_behaviors = read(CAT / 'twigs-behaviors.json')['entries']
    writer(ASSET / 'models/newest/empty.json', {'textures': {'particle': 'minecraft:block/oak_planks'}, 'elements': []})
    for folder, (ns, prefix) in FAMILIES.items():
        base = SOURCE / folder
        assets = base / 'assets' / ns
        labels = read(assets / 'lang/en_us.json')
        allowed = set()
        used_models = set()
        for source in sorted((assets / 'blockstates').rglob('*.json')):
            state = read(source)
            if prefix == 'homeworks':
                def normalize_condition(condition):
                    return {k: [normalize_condition(c) for c in v] if k in ('OR', 'AND') else str(v).lower() for k, v in condition.items()}
                for part in state.get('multipart', []):
                    if 'when' in part: part['when'] = normalize_condition(part['when'])
            if prefix == 'factoryworks' and source.stem in FACTORY_PRESENTATION_ONLY: continue
            if prefix == 'windowworks' and not (assets / 'items' / source.name).exists() and not (assets / 'models/item' / source.name).exists():
                # Old crimson/warped aliases remain in the source alongside the
                # registered *_stem / *_planks versions; do not duplicate them.
                omitted.append({'source': source.relative_to(SOURCE).as_posix(), 'reason': 'Legacy window alias without registered item; current supplied stem/planks variant imported'})
                continue
            kind = kind_of(source.stem, state)
            if prefix == 'doorworks' and kind == 'door':
                if '_japanese' in source.stem or '_shoji' in source.stem: kind = 'sliding_door'
                elif '_stable' in source.stem: kind = 'stable_door'
            props = state_properties(state)
            if prefix == 'homeworks': kind = 'home_decor'
            if prefix == 'twigworks':
                from newest_twigs import classify
                reference = twig_behaviors[source.stem]['reference_class']
                kind = classify(reference, props)
            if prefix == 'holidayworks':
                reference = holiday_behaviors[source.stem]['reference_class']
                kind = {'Block': 'block', 'StairBlock': 'stairs', 'SlabBlock': 'slab',
                        'CarvedPumpkinBlock': 'holiday_pumpkin', 'WebBlock': 'holiday_web',
                        'TintedParticleLeavesBlock': 'holiday_leaves', 'TallGrassBlock': 'holiday_grass',
                        'DoublePlantBlock': 'holiday_plant', 'GrassBlock': 'holiday_soil',
                        'UnmeltableSnowLayer': 'holiday_snow'}.get(reference, 'holiday_decor')
            if prefix == 'furnitureworks': kind = 'joined_furniture'
            if prefix == 'lightworks':
                if 'part' in props: kind = 'light_column'
                elif 'hanging' in props: kind = 'light_lantern'
                elif 'face' in props: kind = 'light_mount'
                elif 'dye_color' in props: kind = 'light_dyeable'
                elif 'facing' in props: kind = 'light_wall'
                elif 'axis' in props: kind = 'light_chain'
                elif 'type' in props: kind = 'light_slab'
                else: kind = 'light_short'
            if prefix == 'stairworks':
                if 'north' in props:
                    kind = 'authored_balcony'
                    for direction in ('north', 'east', 'south', 'west'): props[direction].add('false')
                elif 'style' in props: kind = 'authored_railing'
                elif 'facing' in props: kind = 'authored_steps'
                else: kind = 'authored_platform'
            if prefix == 'windowworks':
                if 'north' in props: kind = 'window_pane'
                elif 'blindsstate' in props: kind = 'window_blinds'
                elif 'tied' in props: kind = 'window_curtain'
                elif 'hinge' in props: kind = 'window_shutter'
                elif 'windowstate' in props: kind = 'window_openable'
                elif 'flower' in props.get('part', ()):
                    kind = 'window_parapet'; props['part'].add('empty')
                elif 'single_l' in props.get('part', ()): kind = 'window_connected'
                elif 'part' in props: kind = 'window_column'
                else: kind = 'window_rod'
            if prefix == 'fenceworks':
                if 'hinge' in props: kind = 'stacked_gate'
                elif 'open' in props: kind = 'fence_gate'
                elif 'fencepart' in props: kind = 'stacked_fence'
                elif source.stem.endswith('_hedge') or source.stem.endswith('_grass_topped_wall'): kind = 'thick_fence'
                elif 'wired' in source.stem or 'cheval_de_frise' in source.stem: kind = 'wired_fence'
                else: kind = 'fence'
            if prefix == 'bridgeworks':
                if 'lightstate' in props: kind = 'bridge_light'
                elif 'powered' in props: kind = 'bridge_rail'
                elif props.get('connection') == {'base', 'double', 'left', 'right'}: kind = 'bridge_stairs'
                elif 'base_toggled' in props.get('connection', ()): kind = 'bridge_rope'
                elif 'connection' in props: kind = 'bridge_deck'
                else: kind = 'bridge_support'
            if prefix == 'factoryworks':
                if source.stem == 'medium_fan': kind = 'large_factory_fan'
                elif source.stem in ('fan', 'fan_four', 'fan_malfunction'): kind = 'powered_factory_fan'
                elif 'facing=' in ','.join(state.get('variants', {})): kind = 'factory_facing'
                else: kind = 'block'
            if kind is None:
                omitted.append({'source': source.relative_to(SOURCE).as_posix(), 'reason': 'Garage requires multi-part opening, not a vanilla door'})
                continue
            name = source.relative_to(assets / 'blockstates').as_posix()[:-5]
            ident = prefix + '_' + name
            missing = missing_models(assets, ns, state)
            if missing:
                omitted.append({'source': source.relative_to(SOURCE).as_posix(), 'reason': 'Missing supplied block models', 'missing': missing})
                continue
            allowed.add(name)
            used_models.update(model_references(state, ns))
            blocks[ident] = {'source': ns + ':' + name, 'kind': kind, 'collection': prefix,
                             'wood': (kind in ('door', 'sliding_door', 'stable_door') and not name.startswith('iron_')) or name.endswith('_planks_path') or name == 'wooden_portcullis',
                             'iron': kind in ('door', 'sliding_door', 'stable_door') and name.startswith('iron_')}
            if prefix == 'fenceworks':
                blocks[ident]['wood'] = name.startswith(tuple(w + '_' for w in
                    ('oak', 'dark_oak', 'pale_oak', 'spruce', 'birch', 'acacia', 'jungle', 'mangrove', 'cherry', 'bamboo', 'crimson', 'warped', 'poplar', 'wooden')))
                blocks[ident]['middle'] = 'middle' in props.get('fencepart', ())
            if prefix == 'chiselworks':
                material = {'purpur': 'purpur_block', 'quartz': 'quartz_block', 'nether_brick': 'nether_bricks'}.get(source.stem, source.stem)
                blocks[ident]['base'] = 'minecraft:' + material
                blocks[ident]['wood'] = material.endswith('_planks')
                if material == 'redstone_block': blocks[ident]['kind'] = 'powered_pillar' if kind == 'pillar' else 'powered_block'
                elif material == 'glass': blocks[ident]['kind'] = 'transparent'
                elif material == 'ice': blocks[ident]['kind'] = 'ice_pillar' if kind == 'pillar' else 'ice'
            if prefix == 'bridgeworks':
                from newest_shapes import variants
                blocks[ident]['shapes'] = variants(assets, ns, state)
                blocks[ident]['wood'] = any(w in name for w in ('oak', 'spruce', 'acacia', 'jungle', 'birch', 'cherry', 'mangrove', 'crimson', 'warped', 'bamboo', 'rope', 'asian'))
            if prefix == 'windowworks':
                from newest_shapes import variants
                blocks[ident]['properties'] = {key: sorted(values) for key, values in props.items()}
                if kind != 'window_pane': blocks[ident]['shapes'] = variants(assets, ns, state, props)
                blocks[ident]['wood'] = any(w in name for w in ('oak', 'spruce', 'acacia', 'jungle', 'birch', 'cherry', 'mangrove', 'crimson', 'warped', 'bamboo'))
            if prefix == 'stairworks':
                from newest_shapes import variants
                blocks[ident]['shapes'] = variants(assets, ns, state, props)
                blocks[ident]['wood'] = any(w in name for w in ('oak', 'spruce', 'acacia', 'jungle', 'birch', 'cherry', 'mangrove', 'crimson', 'warped', 'bamboo'))
            if prefix == 'lightworks':
                from newest_shapes import variants
                if kind not in ('light_chain', 'light_slab'): blocks[ident]['shapes'] = variants(assets, ns, state, props)
                blocks[ident]['flame'] = 'tiki_torch' in name or 'torch' in name or 'candle' in name
                blocks[ident]['soul'] = 'soul' in name
            if prefix == 'furnitureworks':
                from newest_shapes import variants
                blocks[ident]['properties'] = {key: sorted(values) for key, values in props.items()}
                blocks[ident]['shapes'] = variants(assets, ns, state, props)
                blocks[ident]['wood'] = True
                blocks[ident]['function'] = 'seat' if name.endswith(('_chair', '_couch', '_chaise')) else 'sink' if name.endswith('_sink') else (
                    'storage' if any(word in name for word in ('wardrobe', 'bookshelf', 'drawer', 'cupboard', 'cabinet')) else 'decor')
            if prefix == 'holidayworks':
                from newest_shapes import variants
                blocks[ident]['light'] = holiday_behaviors[name]['light']
                blocks[ident]['reference_class'] = reference
                blocks[ident]['wood'] = kind not in ('holiday_snow', 'holiday_web', 'holiday_soil')
                if kind == 'holiday_decor':
                    blocks[ident]['properties'] = {key: sorted(values) for key, values in props.items()}
                    blocks[ident]['shapes'] = variants(assets, ns, state, props)
            if prefix == 'twigworks':
                from newest_twigs import describe
                describe(blocks[ident], name, reference, props, assets, ns, state)
            if prefix == 'homeworks':
                from newest_home import describe
                describe(blocks[ident], name, props, assets, ns, state)
            # Plain full-block paving entries with a thin authored mesh are real low paths.
            if kind == 'path' and prefix == 'stoneworks': blocks[ident]['kind'] = 'block'
            runtime_state = map_value(state, ns, prefix, True)
            if kind == 'large_factory_fan':
                # Occupy all four blocks; draw the supplied 2x2 mesh only once.
                runtime_state = {'multipart': [{'when': {'column': '0', 'row': '0', 'facing': facing},
                    'apply': map_value(state['variants']['facing=' + facing], ns, prefix, True)}
                    for facing in ('north', 'south', 'east', 'west')]}
            writer(ASSET / 'blockstates' / (ident + '.json'), runtime_state)
            supplied_item = assets / 'items' / (name + '.json')
            if supplied_item.exists():
                used_models.update(model_references(read(supplied_item), ns))
                item = map_value(read(supplied_item), ns, prefix, True)
            elif (assets / 'models/item' / (name + '.json')).exists():
                used_models.add('item/' + name)
                item = {'model': {'type': 'minecraft:model', 'model': 'jugcraft:newest/' + prefix + '/item/' + name}}
            elif name == 'potted_azalea_flowers' and prefix == 'twigworks':
                item = None  # Native flower-pot interaction; not a separate block item.
            elif prefix == 'homeworks' and name == 'spruce_chandelier':
                original = 'block/chandelier/spruce/spruce_chandelier_upper_off'
                used_models.add(original)
                item = {'model': {'type': 'minecraft:model', 'model': 'jugcraft:newest/homeworks/' + original}}
            else:
                raise ValueError('Missing supplied item presentation: ' + ns + ':' + name)
            if prefix == 'fenceworks' and (name.endswith('_hedge') or name.endswith('_grass_topped_wall')):
                # These supplied PNGs are already colored (including oak/birch/
                # spruce), unlike vanilla grayscale foliage. Preserve their color.
                item['model']['tints'] = [{'type': 'minecraft:constant', 'value': -1}]
            if prefix == 'twigworks' and name == 'bronzed_seashell':
                used_models.add('item/playing_bronzed_seashell')
                item = {'model': {'type': 'minecraft:condition', 'property': 'minecraft:using_item',
                    'on_true': {'type': 'minecraft:model', 'model': 'jugcraft:newest/twigworks/item/playing_bronzed_seashell'},
                    'on_false': item['model']}}
            if prefix == 'homeworks':
                from newest_home import item_model
                item = item_model(name, blocks[ident], item)
            if item is not None: writer(ASSET / 'items' / (ident + '.json'), item)
            language['block.jugcraft.' + ident.replace('/', '.')] = labels.get('block.' + ns + '.' + name,
                labels.get('item.' + ns + '.' + name.replace('/', '.'), name.replace('_', ' ').replace('/', ' ').title()))
        # Door prints are consumed recipe ingredients, not blocks. Preserve their
        # supplied item definitions and their paper + vanilla-door crafting routes.
        extra_items = list((assets / 'items').glob('print_*.json'))
        if prefix == 'chiselworks': extra_items.append(assets / 'items/chisel.json')
        if prefix == 'bridgeworks': extra_items.append(assets / 'items/pliers.json')
        if prefix == 'windowworks': extra_items.extend(assets / 'items' / (name + '.json') for name in ('key', 'hammer', 'window_base', 'window_centre_bar_base', 'window_half_bar_base'))
        if prefix == 'furnitureworks': extra_items.extend(assets / 'items' / (name + '.json') for name in ('cabinet_door', 'cabinet_drawer'))
        if prefix == 'holidayworks': extra_items.append(assets / 'items/candy_cane.json')
        if prefix == 'twigworks': extra_items.extend(assets / 'models/item' / (name + '.json') for name in ('silt_ball', 'silt_brick', 'twigs'))
        if prefix == 'homeworks': extra_items.extend(assets / 'models/item' / (name + '.json') for name in ('cushion', 'hay_cushion', 'trader_cushion', 'paint_brush'))
        for source in sorted(extra_items):
            name, ident = source.stem, prefix + '_' + source.stem
            allowed.add(name)
            used_models.update(model_references(read(source), ns))
            items[ident] = {'source': ns + ':' + name, 'collection': prefix}
            if prefix == 'chiselworks': items[ident]['kind'] = 'chisel'
            if prefix == 'bridgeworks': items[ident]['kind'] = 'pliers'
            if prefix == 'windowworks': items[ident]['kind'] = 'window_tool' if name in ('key', 'hammer') else 'ingredient'
            if prefix == 'holidayworks': items[ident]['kind'] = 'candy_cane'
            if prefix in ('twigworks', 'homeworks'):
                items[ident]['kind'] = 'twig_novelty' if name == 'twigs' else 'home_brush' if name == 'paint_brush' else 'home_cushion' if name.endswith('cushion') else 'ingredient'
                used_models.add('item/' + name)
                native_item = {'type': 'minecraft:model', 'model': 'jugcraft:newest/' + prefix + '/item/' + name}
                if prefix == 'homeworks': native_item['tints'] = [{'type': 'minecraft:dye', 'default': [1, 1, 1]}]
                writer(ASSET / 'items' / (ident + '.json'), {'model': native_item})
            else: writer(ASSET / 'items' / (ident + '.json'), map_value(read(source), ns, prefix, True))
            language['item.jugcraft.' + ident] = labels.get('item.' + ns + '.' + name, name.replace('_', ' ').title())
        # Import only models reachable from implemented content. The supplied
        # folder also contains abandoned experiments (e.g. snow_path, no skin).
        pending, visited = list(used_models), set()
        while pending:
            model = pending.pop()
            if model in visited: continue
            source = assets / 'models' / (model + '.json')
            if ns == 'cozyhome':
                from newest_home import supplied_model
                value = supplied_model(assets, model)
            else: value = read(source) if source.is_file() else None
            if value is None: raise ValueError('Missing reachable model: ' + ns + ':' + model)
            visited.add(model)
            if prefix == 'homeworks' and value.get('parent') in ('builtin/entity', 'minecraft:builtin/entity'): value.pop('parent')
            if prefix in ('twigworks', 'homeworks'): value.pop('overrides', None)  # Converted to native item predicates above.
            pending.extend(model_references(value, ns))
            writer(ASSET / 'models/newest' / prefix / (model + '.json'), map_value(value, ns, prefix, True))
        model_root = (ASSET / 'models/newest' / prefix).resolve()
        for stale in model_root.rglob('*.json'):
            if stale.relative_to(model_root).as_posix()[:-5] not in visited:
                if not stale.resolve().is_relative_to(model_root): raise ValueError('Model path outside generated family')
                stale.unlink()  # Only stale generated copies; originals remain untouched.
        for source in sorted((assets / 'textures').rglob('*')):
            if source.is_file(): copy(source, ASSET / 'textures/newest' / prefix / source.relative_to(assets / 'textures'))
        data_root = base / 'data' / ns
        for category in ('recipe', 'loot_table'):
            for source in sorted((data_root / category).rglob('*.json')):
                value = read(source)
                if category == 'recipe':
                    # The source's Chipped workbench hook is replaced below by
                    # native stonecutting, not an unavailable recipe serializer.
                    if prefix == 'factoryworks' and value.get('type') == 'chipped:workbench': continue
                    result = value.get('result', {})
                    output = result.get('id', result.get('item', '')) if isinstance(result, dict) else result
                    if output.startswith(ns + ':') and output.split(':', 1)[1] not in allowed: continue
                if category == 'loot_table' and source.relative_to(data_root / category).as_posix().removeprefix('blocks/')[:-5] not in allowed: continue
                if prefix in ('twigworks', 'homeworks') and category == 'recipe':
                    from newest_twigs import recipe
                    value = recipe(value)
                writer(RES / 'data/jugcraft' / category / 'newest' / prefix / source.relative_to(data_root / category), map_value(value, ns, prefix))
        # Registered blocks point at their default loot location.
        for name in allowed:
            source = data_root / 'loot_table/blocks' / (name + '.json')
            if source.exists(): writer(RES / 'data/jugcraft/loot_table/blocks' / (prefix + '_' + name + '.json'), map_value(read(source), ns, prefix))
            if blocks.get(prefix + '_' + name, {}).get('kind') == 'garage':
                writer(RES / 'data/jugcraft/loot_table/blocks' / (prefix + '_' + name + '.json'),
                    {'type': 'minecraft:block', 'pools': [{'rolls': 1,
                     'entries': [{'type': 'minecraft:item', 'name': 'jugcraft:' + prefix + '_' + name}],
                     'conditions': [{'condition': 'minecraft:survives_explosion'},
                     {'condition': 'minecraft:block_state_property', 'block': 'jugcraft:' + prefix + '_' + name,
                      'properties': {'part': 'top'}}]}]})
            if blocks.get(prefix + '_' + name, {}).get('kind') == 'authored_balcony':
                # One consumed item per installed side, one returned per side on
                # any normal break (including explosions), without manual duplicates.
                writer(RES / 'data/jugcraft/loot_table/blocks' / (prefix + '_' + name + '.json'),
                    {'type': 'minecraft:block', 'pools': [{'rolls': 1,
                     'entries': [{'type': 'minecraft:item', 'name': 'jugcraft:' + prefix + '_' + name}],
                     'conditions': [{'condition': 'minecraft:survives_explosion'},
                     {'condition': 'minecraft:block_state_property', 'block': 'jugcraft:' + prefix + '_' + name,
                      'properties': {direction: 'true'}}]} for direction in ('north', 'south', 'east', 'west')]})
            if blocks.get(prefix + '_' + name, {}).get('kind') == 'bridge_rail':
                loot_path = RES / 'data/jugcraft/loot_table/blocks' / (prefix + '_' + name + '.json')
                loot = read(loot_path)
                loot['pools'].append({'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:redstone_torch'}],
                    'conditions': [{'condition': 'minecraft:survives_explosion'},
                    {'condition': 'minecraft:block_state_property', 'block': 'jugcraft:' + prefix + '_' + name, 'properties': {'powered': 'true'}}]})
                writer(loot_path, loot)
        if prefix == 'factoryworks':
            for name in sorted(allowed):
                # Every cosmetic finish uses the same base material. Four base
                # blocks pay for the four occupied cells of the large fan.
                if name not in ('factory', 'medium_fan'):
                    writer(RES / 'data/jugcraft/recipe/newest/factoryworks' / (name + '_cutting.json'),
                           {'type': 'minecraft:stonecutting', 'ingredient': 'jugcraft:factoryworks_factory',
                            'result': {'id': 'jugcraft:factoryworks_' + name, 'count': 1}})
                loot = {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [
                    {'type': 'minecraft:item', 'name': 'jugcraft:factoryworks_' + name}],
                    'conditions': [{'condition': 'minecraft:survives_explosion'}]}]}
                if name == 'medium_fan':
                    loot['pools'][0]['conditions'].append({'condition': 'minecraft:block_state_property',
                        'block': 'jugcraft:factoryworks_medium_fan', 'properties': {'column': '0', 'row': '0'}})
                writer(RES / 'data/jugcraft/loot_table/blocks' / ('factoryworks_' + name + '.json'), loot)
            writer(RES / 'data/jugcraft/recipe/newest/factoryworks/medium_fan.json',
                   {'type': 'minecraft:crafting_shaped', 'pattern': ['FF', 'FF'],
                    'key': {'F': 'jugcraft:factoryworks_factory'},
                    'result': {'id': 'jugcraft:factoryworks_medium_fan', 'count': 1}})
            merge_tag(RES / 'data/minecraft/tags/block/mineable/pickaxe.json',
                      ['jugcraft:factoryworks_' + name for name in sorted(allowed)], writer)
        if prefix == 'fenceworks':
            for category in ('block', 'item'):
                merge_tag(RES / ('data/minecraft/tags/' + category + '/fences.json'),
                    ['jugcraft:' + ident for ident, d in blocks.items() if d['collection'] == prefix and 'gate' not in d['kind']], writer)
                merge_tag(RES / ('data/minecraft/tags/' + category + '/wooden_fences.json'),
                    ['jugcraft:' + ident for ident, d in blocks.items() if d['collection'] == prefix and d['wood'] and 'gate' not in d['kind']], writer)
            merge_tag(RES / 'data/minecraft/tags/block/fence_gates.json',
                ['jugcraft:' + ident for ident, d in blocks.items() if d['collection'] == prefix and 'gate' in d['kind']], writer)
        if prefix == 'furnitureworks':
            merge_tag(RES / 'data/minecraft/tags/block/enchantment_power_provider.json',
                ['jugcraft:' + ident for ident, d in blocks.items() if d['collection'] == prefix and 'bookshelf' in ident], writer)
        if prefix == 'twigworks':
            from newest_twigs import resources
            resources(base, RES, writer, copy)
        if prefix == 'homeworks':
            from newest_home import resources
            resources(base, RES, writer, copy, blocks)
        if prefix == 'chiselworks':
            groups = {}
            for ident, descriptor in blocks.items():
                if descriptor['collection'] == prefix: groups.setdefault(descriptor['base'], []).append('jugcraft:' + ident)
            for base_id, variants in sorted(groups.items()):
                material = base_id.split(':', 1)[1]
                tag = 'jugcraft:chiselworks/' + material
                writer(RES / 'data/jugcraft/tags/item/chiselworks' / (material + '.json'), {'values': [base_id] + variants})
                # No material conversion: every recipe stays within one source
                # block family, costs one block and produces exactly one block.
                for output in [base_id] + variants:
                    writer(RES / 'data/jugcraft/recipe/newest/chiselworks/cutting' / (output.replace(':', '/') + '.json'),
                        {'type': 'minecraft:stonecutting', 'ingredient': '#' + tag,
                         'result': {'id': output, 'count': 1}})
                for ident in variants:
                    writer(RES / 'data/jugcraft/loot_table/blocks' / (ident.split(':', 1)[1] + '.json'),
                        {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': ident}],
                         'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
        for source in sorted((base / 'data').glob('*/tags/**/*.json')):
            value = read(source)
            entries = []
            for entry in value.get('values', []):
                name = entry if isinstance(entry, str) else entry.get('id', '')
                if name.startswith(ns + ':') and name.split(':', 1)[1] not in allowed: continue
                entries.append(map_value(entry, ns, prefix))
            parts = source.relative_to(base / 'data').parts
            category = {'blocks': 'block', 'items': 'item', 'entity_types': 'entity_type'}.get(parts[2], parts[2])
            target = RES / 'data' / ('jugcraft' if parts[0] == ns else parts[0]) / 'tags' / category
            rel = Path(*parts[3:])
            if parts[0] == ns: rel = Path(prefix + '_' + rel.as_posix())
            if parts[0] == 'c' and category == 'item' and rel.as_posix() == 'polished_calcite.json':
                entries = [v for v in entries if (v.get('id') if isinstance(v, dict) else v) != 'minecraft:polished_calcite']
                entries += ['jugcraft:twigworks_polished_calcite', 'jugcraft:stoneworks_polished_calcite']
            merge_tag(target / rel, entries, writer)
        prune_unregistered(prefix, allowed)
        provenance.append({'folder': folder, 'namespace': ns, 'blocks': sum(d['collection'] == prefix for d in blocks.values()), 'items': sum(d['collection'] == prefix for d in items.values()), 'runtime': 'newest/' + prefix,
                           'changes': 'Native block behaviors; remapped identifiers; original model coordinates and texture bytes retained'})
    painting_root = SOURCE / 'painting'
    paintings = []
    labels = read(painting_root / 'assets/mcwpaintings/lang/en_us.json')
    for source in sorted((painting_root / 'data/mcwpaintings/painting_variant').glob('*.json')):
        ident = 'gallery_' + source.stem
        value = read(source)
        value['asset_id'] = 'jugcraft:newest/' + source.stem
        for field in ('title', 'author'):
            if isinstance(value.get(field), dict) and 'translate' in value[field]:
                old = value[field]['translate']
                key = 'painting.jugcraft.' + ident + '.' + field
                language[key] = labels.get(old, source.stem.replace('_', ' ').title() if field == 'title' else 'Unknown')
                value[field]['translate'] = key
        writer(RES / 'data/jugcraft/painting_variant' / (ident + '.json'), value)
        copy(painting_root / 'assets/mcwpaintings/textures/painting' / (source.stem + '.png'), ASSET / 'textures/painting/newest' / (source.stem + '.png'))
        paintings.append('jugcraft:' + ident)
    merge_tag(RES / 'data/minecraft/tags/painting_variant/placeable.json', paintings, writer)
    writer(RES / 'jugcraft/newest_building.json', {'blocks': blocks, 'items': items, 'paintings': paintings})
    lang_path = ASSET / 'lang/en_us.json'
    merged = read(lang_path); merged.update(language)
    merged['itemGroup.jugcraft.newest_building'] = 'Jugcraft: Building Collection'
    merged['container.jugcraft.owner_chisel'] = 'Chisel — Choose a Finish'
    writer(lang_path, merged)
    writer(CAT / 'building-runtime.json', {'blocks': len(blocks), 'items': len(items), 'paintings': len(paintings), 'sources': provenance,
                                          'remaining': omitted, 'validation': 'pending combined checks'})
    from newest_connected import generate as generate_connected
    connected = generate_connected(SOURCE, RES, writer)
    print('Supplied connected-texture adaptations:', connected)
    print('Building behaviors/resources:', len(blocks), 'blocks;', len(paintings), 'paintings;', len(omitted), 'entries pending special mechanics or missing source models')
    return language


if __name__ == '__main__': generate()
