"""CozyHome owner assets: original state meshes plus recovered entity-model rigs."""
from newest_shapes import variants
from menagerie_content import read


def supplied_model(assets, name):
    path = assets / 'models' / (name + '.json')
    if path.is_file():
        value = read(path)
        if name == 'block/dye_vat':
            # Stale texture labels: the matching original skins are present.
            value['textures']['3'] = 'cozyhome:block/dye_vat/dye_vat_inner_side'
            value['textures']['4'] = 'cozyhome:block/dye_vat/dye_vat_inner_bottom'
        return value
    prefix = 'block/liquid/types/water_10_by_10_level_'
    if name.startswith(prefix) and name[len(prefix):] in ('3', '6', '9', '12', '15'):
        height = int(name[len(prefix):])
        # Supplied vat is complete, but its five fluid planes are absent. Native
        # water surfaces fit its authored ten-pixel opening; no substitute furniture.
        return {'textures': {'water': 'minecraft:block/water_still', 'particle': 'minecraft:block/water_still'},
                'elements': [{'from': [3, height, 3], 'to': [13, height, 13],
                              'faces': {'up': {'texture': '#water', 'tintindex': 0}}}]}
    return None


def function(name):
    for suffix in ('grandfather_clock', 'wall_clock', 'storage_counter', 'sink_counter',
                   'fountain_spout', 'large_stump', 'wall_mirror', 'mirror_stand',
                   'chair', 'sofa', 'couch', 'bench', 'drawer', 'desk', 'bathtub',
                   'sink', 'fountain', 'chimney', 'lamp', 'counter', 'table',
                   'lantern', 'zaisu', 'fireplace', 'chandelier'):
        if name.endswith(suffix): return suffix
    return name


def describe(d, name, props, assets, ns, state):
    f = function(name)
    for values in props.values():
        if values == {'true'}: values.add('false')
    props['waterlogged'] = {'false', 'true'}
    if f in ('chair', 'sofa', 'grandfather_clock'):
        props['rotation'] = set(map(str, range(16)))
    if f == 'chair': props['tucked'] = {'false', 'true'}
    if f in ('drawer', 'desk', 'storage_counter'): props['open'] = {'false', 'true'}
    if f == 'dye_vat':
        props['contains'] = {'none', 'water'}
        props['level'] = {'0', '1', '2', '3', '4', '5'}
    if f in ('wall_clock', 'telescope'): props['facing'] = {'north', 'east', 'south', 'west'}
    if f == 'grandfather_clock': props['part'] = {'bottom', 'middle', 'top'}
    if f == 'telescope' or name.startswith('ominous_'): props['triggered'] = {'false', 'true'}
    if 'contains' in props: props['contains'].add('none')
    if 'level' in props: props['level'].add('0')
    d.update(function=f, properties={k: sorted(v) for k, v in props.items()},
             shapes=variants(assets, ns, state, props), wood=any(name.startswith(w + '_') for w in
                 ('oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak', 'mangrove', 'cherry', 'bamboo', 'crimson', 'warped')))
    if f in ('lamp', 'lantern', 'chandelier', 'fireplace'): d['lamp'] = True
    if f == 'falling_liquid': d['no_item'] = True
    rig = {'chair': 'ChairModel', 'sofa': 'SofaModel', 'wall_clock': 'WallClockModel',
           'grandfather_clock': 'GrandfatherClockModel', 'telescope': 'TelescopeModel'}.get(f)
    # Stump/simple chairs are ordinary JSON meshes, not the chair renderer family.
    if rig and not state.get('variants') and not state.get('multipart'): d['rig'] = rig
    if f in ('chair', 'sofa') and name not in ('stump_chair',): d['rig'] = rig
    if f in ('grandfather_clock', 'wall_clock', 'telescope'): d['rig'] = rig
    if 'rig' in d:
        d['shapes'] = {'': [[2, 0, 2, 14, 10 if f in ('chair', 'sofa') else 16, 14]]}
    return d


def item_model(name, descriptor, fallback):
    if 'rig' not in descriptor:
        if fallback and fallback.get('model', {}).get('type') == 'minecraft:model': fallback['model']['tints'] = [{'type': 'minecraft:dye', 'default': [1, 1, 1]}]
        return fallback
    return {'model': {'type': 'minecraft:special',
        'base': 'jugcraft:newest/homeworks/item/' + name,
        'model': {'type': 'jugcraft:owner_home', 'design': name}}}


def finish_item_models():
    """Bind recovered rigs and compose both supplied bathtub halves for inventory.

    Source legacy item templates contain display transforms but no mesh. Do not
    confuse a successfully loaded empty JSON with a visible inventory item.
    """
    import copy
    import json
    from pathlib import Path
    root = Path(__file__).resolve().parents[1] / 'src/main/resources'
    assets = root / 'assets/jugcraft'
    def put(path, data):
        text = json.dumps(data, indent=2, ensure_ascii=False) + '\n'
        if path.read_text(encoding='utf-8') != text:
            path.write_text(text, encoding='utf-8')
    def resolve(ref):
        if not ref.startswith('jugcraft:'): return {}
        path = assets / 'models' / (ref.split(':', 1)[1] + '.json')
        if not path.exists(): return {}
        data = read(path)
        inherited = resolve(data.get('parent', ''))
        result = dict(inherited, **data)
        result['textures'] = dict(inherited.get('textures', {}), **data.get('textures', {}))
        return result
    for ident, descriptor in read(root / 'jugcraft/newest_building.json')['blocks'].items():
        if not ident.startswith('homeworks_'): continue
        name = ident.removeprefix('homeworks_')
        item = assets / 'items' / (ident + '.json')
        model = assets / 'models/newest/homeworks/item' / (name + '.json')
        if not item.exists() or not model.exists(): continue
        if 'rig' in descriptor:
            put(item, item_model(name, descriptor, read(item)))
        data = read(model)
        block = resolve('jugcraft:newest/homeworks/block/' + name)
        if block.get('textures', {}).get('particle'):
            data.setdefault('textures', {})['particle'] = block['textures']['particle']
        if descriptor.get('function') == 'bathtub':
            data['elements'] = []
            for suffix, offset, prefix in [('', -8, 'front_'), ('_back', 8, 'back_')]:
                part = resolve('jugcraft:newest/homeworks/block/' + name + suffix)
                if not part.get('elements'): raise ValueError('Missing supplied bathtub half: ' + name + suffix)
                for key, value in part.get('textures', {}).items():
                    data.setdefault('textures', {})[prefix + key] = '#' + prefix + value[1:] if value.startswith('#') else value
                for source in part.get('elements', []):
                    element = copy.deepcopy(source)
                    element['from'][2] += offset
                    element['to'][2] += offset
                    if 'rotation' in element: element['rotation']['origin'][2] += offset
                    for face in element.get('faces', {}).values():
                        if face['texture'].startswith('#'): face['texture'] = '#' + prefix + face['texture'][1:]
                    data['elements'].append(element)
        put(model, data)


def resources(base, res, writer, copier, blocks):
    from newest_building import merge_tag
    merge_tag(res / 'data/minecraft/tags/item/dyeable.json', ['jugcraft:homeworks_' + name for name in ('cushion', 'trader_cushion', 'paint_brush')], writer)
    # Source block-entity animation/audio remains data, never executed code.
    soundfile = base / 'assets/cozyhome/sounds.json'
    if soundfile.exists():
        sounds = read(res / 'assets/jugcraft/sounds.json')
        for name, definition in read(soundfile).items():
            d = dict(definition); out = []
            for entry in d['sounds']:
                if isinstance(entry, str): out.append(entry.replace('cozyhome:', 'jugcraft:newest/homeworks/'))
                else:
                    entry = dict(entry)
                    entry['name'] = entry['name'].replace('cozyhome:', 'jugcraft:homeworks_' if entry.get('type') == 'event' else 'jugcraft:newest/homeworks/')
                    out.append(entry)
            d['sounds'] = out; sounds['homeworks_' + name] = d
        writer(res / 'assets/jugcraft/sounds.json', sounds)
    for source in (base / 'assets/cozyhome/sounds').rglob('*'):
        if source.is_file(): copier(source, res / 'assets/jugcraft/sounds/newest/homeworks' / source.relative_to(base / 'assets/cozyhome/sounds'))
    for ident, d in blocks.items():
        if d['collection'] != 'homeworks': continue
        if d['function'] in ('grandfather_clock', 'chandelier', 'bathtub'):
            # Exactly one item for a three-cell clock, including middle/top breaks.
            writer(res / 'data/jugcraft/loot_table/blocks' / (ident + '.json'),
                {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'jugcraft:' + ident}],
                    'conditions': [{'condition': 'minecraft:survives_explosion'}, {'condition': 'minecraft:block_state_property',
                        'block': 'jugcraft:' + ident, 'properties': {'half': 'upper'} if d['function'] == 'chandelier' else {'part': 'front' if d['function'] == 'bathtub' else 'bottom'}}]}]})
    # Complete experimental meshes in this archive have no original registry recipe.
    # Native survival routes use paid vanilla materials, not creative-only aliases.
    for name, ingredients in {'spruce_chandelier': ['minecraft:chain', 'minecraft:lantern', 'minecraft:lantern', 'minecraft:spruce_planks'],
                              'dye_vat': ['minecraft:cauldron', 'minecraft:oak_planks', 'minecraft:iron_ingot']}.items():
        writer(res / 'data/jugcraft/recipe/newest/homeworks' / (name + '.json'), {'type': 'minecraft:crafting_shapeless',
            'ingredients': ingredients, 'result': {'id': 'jugcraft:homeworks_' + name, 'count': 1}})
        if 'homeworks_' + name not in blocks: continue
        if name != 'spruce_chandelier': writer(res / 'data/jugcraft/loot_table/blocks' / ('homeworks_' + name + '.json'),
            {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'jugcraft:homeworks_' + name}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
