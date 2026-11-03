"""Bounded native geometry repairs, verified against the preserved authored meshes."""
from copy import deepcopy
from pathlib import Path

from newest_building import ROOT, RES, SOURCE, read, map_value, write
import model_writer

FAMILIES = {
    'aquatic': ('Aquacultah', 'aquaculture'), 'ars_additions': ('ars adds', 'ars_additions'),
    'forbidden': ('forbidden aracane', 'forbidden_arcanus'), 'homeworks': ('home', 'cozyhome'),
    'jugtania': ('Jugtania', 'botania'), 'packworks': ('BACKPACKS', 'sophisticatedbackpacks'),
    'ribbits': ('frogs', 'ribbits'), 'supps': ('SUPPS', 'supplementaries'),
    'wilds': ('jugziesmobs', 'mowziesmobs'),
}
TIERS = ('backpack', 'copper_backpack', 'iron_backpack', 'gold_backpack', 'diamond_backpack', 'netherite_backpack')
PACK_PARTS = ('backpack_base', 'backpack_left_pouch', 'backpack_right_pouch', 'backpack_front_pouch', 'backpack_straps')
SEPARATE = {
    'aquatic/block/fish_mount', 'forbidden/block/edelwood_branch',
    'wilds/item/spear_in_hand', 'wilds/item/wrought_axe_in_hand',
    'supps/block/bamboo_spikes_tipped', 'supps/block/candle_holders/ceiling_31', 'supps/item/jar_boat',
    *('ars_additions/block/source_jar/source_jar' + str(n) for n in range(10, 101, 10)),
    'ars_additions/block/source_jar/source_jar_sliver',
    *('forbidden/block/hephaestus_forge_tier_' + str(n) for n in range(1, 6)),
    *('packworks/block/' + n for n in TIERS + PACK_PARTS[1:] + ('backpack_battery',)),
    *('ribbits/block/giant_lilypad' + s for s in ('', '_2', '_3', '_4')),
    *('ribbits/block/swamp_daisy' + s for s in ('', '_2', '_3')),
    *('ribbits/block/umbrella_leaf_' + str(n) for n in (2, 4)),
    *('supps/block/' + n + '_cross_2' for n in ('blaze_rod', 'breeze_rod', 'edelwood_stick', 'propelplant_cane', 'stick')),
}
BASKETS = {'supps/block/lunch_basket' + state + dye for state in ('', '_open', '_hanging', '_hanging_open')
           for dye in ('', '_dyed')}
CRYSTALS = {'jugtania/' + place + '/corporea_crystal_cube' for place in ('block', 'item')}
# These caps are actually exposed, unlike the separately composed wall/stump parts.
CLOSE_FACES = {
    'homeworks/block/large_stump/template_large_stump_inner_corner_piece': ((0, 'north'), (0, 'west')),
    'homeworks/block/lamp/bamboo_lamp': ((6, 'up'),),
    'homeworks/block/lamp/bamboo_lamp_head': ((6, 'up'),),
    'homeworks/block/lamp/jungle_lamp': ((6, 'up'),),
    'jugtania/block/shapes/spreader_scaffolding': tuple((i, 'up') for i in range(10, 14)),
    'supps/block/spring_launcher_head_small': ((1, 'south'),),
    'supps/block/stick_y_tomatoes': ((0, 'up'), (1, 'down')),
    'supps/special_models/block/books/book_and_quill': ((2, 'up'), (2, 'west')),
    'supps/special_models/block/books/book_enchanted': ((2, 'up'), (2, 'west')),
    **{'supps/special_models/plants/' + n + '_block': ((0, 'down'),) for n in ('cantaloupe', 'honeydew', 'squash')},
}
# Narrow release backport: pre-existing separation repairs remain authoritative.
TARGETS = {name for name in BASKETS if '_hanging' not in name} | CRYSTALS | CLOSE_FACES.keys()


def owner_relative(relative):
    family, name = relative.split('/', 1)
    folder, namespace = FAMILIES[family]
    if family == 'packworks' and name.removeprefix('block/') in TIERS:
        return relative
    models = SOURCE / folder / 'assets' / namespace / 'models'
    for _ in range(20):
        model = read(models / (name + '.json'))
        if 'elements' in model:
            return family + '/' + name
        parent = model.get('parent', '')
        if not parent.startswith(namespace + ':'):
            raise ValueError('Unexpected authored geometry parent: ' + relative)
        name = parent.split(':', 1)[1]
    raise ValueError('Cyclic authored geometry parents: ' + relative)


def original_elements(relative):
    relative = owner_relative(relative)
    family, name = relative.split('/', 1)
    folder, namespace = FAMILIES[family]
    models = SOURCE / folder / 'assets' / namespace / 'models'
    if family == 'packworks' and name.removeprefix('block/') in TIERS:
        # Match the existing native loader's documented source-part composition.
        elements = [e for part in PACK_PARTS for e in read(models / 'block' / (part + '.json'))['elements']]
    elif relative == 'jugtania/item/corporea_crystal_cube':
        # Existing native item combines the block pedestal and its separate glass core.
        elements = [e for part in ('corporea_crystal_cube', 'corporea_crystal_cube_glass')
                    for e in read(models / 'block' / (part + '.json'))['elements']]
    else:
        elements = read(models / (name + '.json'))['elements']
    return map_value(elements, namespace, family, True)


def surface_planes(element):
    """Express the same authored surfaces with normal bounds, retaining inward normals and UVs."""
    result = []
    reversed_box = all(a > b for a, b in zip(element['from'], element['to']))
    for face, material in element['faces'].items():
        axis, positive = {'north': (2, False), 'south': (2, True), 'west': (0, False),
                          'east': (0, True), 'down': (1, False), 'up': (1, True)}[face]
        plane = deepcopy(element)
        plane['from'] = [min(a, b) for a, b in zip(element['from'], element['to'])]
        plane['to'] = [max(a, b) for a, b in zip(element['from'], element['to'])]
        plane['from'][axis] = plane['to'][axis] = element['to' if positive else 'from'][axis]
        material = deepcopy(material)
        if reversed_box and 'uv' in material:
            u0, v0, u1, v1 = material['uv']
            material['uv'] = [u1, v1, u0, v0]
        plane['faces'] = {face: material}
        result.append(plane)
    return result


def finalized_elements(relative, original=None):
    relative = owner_relative(relative)
    elements = deepcopy(original_elements(relative) if original is None else original)
    for index, face in CLOSE_FACES.get(relative, ()):
        if face in elements[index]['faces']:
            raise ValueError('Authored missing cap changed: ' + relative + ' ' + str((index, face)))
        elements[index]['faces'][face] = model_writer._closing_face(elements[index], face)
        if relative == 'homeworks/block/large_stump/template_large_stump_inner_corner_piece':
            elements[index]['faces'][face]['texture'] = '#particle'
    if relative in BASKETS:
        # The last two source elements are the handle's outer/inner three-sided
        # surfaces. Do not fill its deliberately hollow opening or invert its skin.
        outer, inner = elements[-2:]
        if not all(a > b for a, b in zip(inner['from'], inner['to'])):
            raise ValueError('Authored basket handle changed: ' + relative)
        elements[-2:] = surface_planes(outer) + surface_planes(inner)
    if relative in CRYSTALS:
        elements[0]['faces']['up'] = model_writer._closing_face(elements[0], 'up')
        # The source uses 0.001px-thick surface strips, not solid miniature boxes.
        # Keep each face at its exact original plane rather than fabricating walls.
        elements = [elements[0]] + [plane for element in elements[1:]
                    for plane in (surface_planes(element) if min(b-a for a, b in zip(element['from'], element['to'])) <= .0011 else [element])]
    model_writer.finish_elements(elements)
    return elements


def generate(writer=write, resources=RES):
    pending = []
    for relative in sorted({owner_relative(target) for target in TARGETS}):
        target = Path(resources) / 'assets/jugcraft/models/newest' / (relative + '.json')
        model = read(target)
        original = original_elements(relative)
        final = finalized_elements(relative, original)
        if model.get('elements') not in (original, final):
            raise ValueError('Runtime geometry diverged from its preserved source: ' + relative)
        model['elements'] = final
        pending.append((target, model))
    for target, model in pending:
        writer(target, model)
    return [target for target, _ in pending]
