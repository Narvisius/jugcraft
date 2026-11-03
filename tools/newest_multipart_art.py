"""Exact, source-backed seams in imported models, checked with their runtime assemblies."""
from copy import deepcopy
from itertools import product
import json
from pathlib import Path
from PIL import Image

import model_writer

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/jugcraft'
MODELS = ASSETS / 'models/newest'
SOURCE = ROOT / 'art/owner-library/originals/Edited-2026-10-29'
FAMILIES = {'jugtania': ('Jugtania', 'botania'), 'homeworks': ('home', 'cozyhome'),
            'aquatic': ('Aquacultah', 'aquaculture'), 'packworks': ('BACKPACKS', 'sophisticatedbackpacks'),
            'supps': ('SUPPS', 'supplementaries')}
TURN = dict(north='east', east='south', south='west', west='north', up='up', down='down')
# These exact vanilla references were checked against the pinned 26.3 client
# archive: all 22 sheets have alpha255 throughout. Do not infer opacity from a
# namespace, suffix, or an unrecognised texture. Runtime overrides still win.
VANILLA_OPAQUE = {'minecraft:block/' + name + suffix
                  for name in ('acacia_log', 'bamboo_block', 'birch_log', 'cherry_log', 'crimson_stem',
                               'dark_oak_log', 'jungle_log', 'mangrove_log', 'oak_log', 'spruce_log', 'warped_stem')
                  for suffix in ('', '_top')}


def read(path):
    return json.loads(path.read_text(encoding='utf-8'))


def material_faces(path):
    """Resolve the rendered child's overrides, including inherited texture variables."""
    import art_check
    chain, textures = [], {}
    for _ in range(12):
        data = read(path)
        chain.append(data)
        parent = data.get('parent', '')
        if not parent.startswith('jugcraft:'):
            break
        path = ASSETS / 'models' / (parent.split(':', 1)[1] + '.json')
    elements = []
    for data in reversed(chain):
        textures.update(data.get('textures', {}))
        elements = data.get('elements', elements)
    native = set()
    def texture(reference):
        for _ in range(12):
            if not isinstance(reference, str) or not reference.startswith('#'):
                break
            reference = textures.get(reference[1:], '')
        if isinstance(reference, dict):
            reference = reference.get('sprite', '')
        if not isinstance(reference, str) or ':' not in reference:
            return None
        namespace, name = reference.split(':', 1)
        png = ASSETS.parent / namespace / 'textures' / (name + '.png')
        if reference in VANILLA_OPAQUE:
            native.add(png)
        return png
    faces, _ = art_check._model_faces(elements, texture)
    return faces, native


def opaque_region(face, native, uvs=None):
    import art_check
    pixels = art_check._image(face.tex)
    return (art_check._region_alpha(pixels, face.uvs if uvs is None else uvs)[0] == 255 if pixels is not None
            else face.tex in native and not face.tex.exists())


def opaque_elements(path):
    """Solid covering boxes need nonempty rendered faces with opaque sampled UVs."""
    faces, native = material_faces(path)
    checks = {}
    for face in faces:
        checks.setdefault(face.label[0], []).append(opaque_region(face, native))
    return {index for index, values in checks.items() if values and all(values)}


def exposed_seams(elements, candidates, opaque, surfaces=None):
    """Same bounded probes as the geometry proof, restricted to verified opaque covers."""
    boxes = [(index, element['from'], element['to']) for index, element in enumerate(elements)
             if abs(element.get('rotation', {}).get('angle', 0)) < 1e-9]
    drawn, native = surfaces if surfaces is not None else ({}, set())
    exposed = set()
    for index, face in candidates:
        element = elements[index]
        if index not in opaque:
            exposed.add((index, face))
            continue
        axis, sign = model_writer._FACE_DIR[face]
        plane = element['to' if sign > 0 else 'from'][axis]
        others = [k for k in range(3) if k != axis]
        covers = []
        for other, lo, hi in boxes:
            if other == index:
                continue
            if other in opaque:
                covers.append((other, lo, hi))
                continue
            # A backpack trim sheet can have transparency elsewhere in its UV
            # atlas. Require its outward face to be opaque over the ENTIRE
            # overlapping rectangle, not just a few sampled texels.
            surface = drawn.get((other, face))
            if surface is None or not (lo[axis] <= plane + sign * .01 <= hi[axis]):
                continue
            low = [max(element['from'][k], lo[k]) for k in others]
            high = [min(element['to'][k], hi[k]) for k in others]
            if any(a >= b for a, b in zip(low, high)):
                continue
            uvs = []
            for a, b in product((0, 1), repeat=2):
                point = [0.0, 0.0, 0.0]
                point[axis] = surface.pts[0][axis]
                point[others[0]], point[others[1]] = (low[0], high[0])[a], (low[1], high[1])[b]
                uvs.append(surface.uv_at(point))
            if opaque_region(surface, native, uvs):
                covers.append((other, lo, hi))
        for a, b in product(model_writer._PROBES, repeat=2):
            point = [0.0, 0.0, 0.0]
            point[axis] = plane + sign * .01
            for k, fraction in zip(others, (a, b)):
                point[k] = element['from'][k] + fraction * (element['to'][k] - element['from'][k])
            if not any(all(lo[k] - 1e-6 <= point[k] <= hi[k] + 1e-6 for k in range(3))
                       for other, lo, hi in covers if other != index):
                exposed.add((index, face))
                break
    return exposed


def resolve(path, models, namespace):
    for _ in range(12):
        data = read(path)
        if 'elements' in data:
            return data['elements']
        parent = data.get('parent', '')
        if not parent.startswith(namespace + ':'):
            return []
        path = models / (parent.split(':', 1)[1] + '.json')
    return []


def original(path, elements):
    """Locate a preserved model through native aliases, and require unchanged source geometry."""
    path = Path(path).resolve()
    for _ in range(12):
        if path.is_relative_to(MODELS):
            family, name = path.relative_to(MODELS).as_posix().split('/', 1)
            if family not in FAMILIES:
                return None
            folder, namespace = FAMILIES[family]
            models = SOURCE / folder / 'assets' / namespace / 'models'
            authored = resolve(models / name, models, namespace)
            if family == 'supps' and name == 'block/blackboard.json':
                if read(models / name) != {'loader': 'supplementaries:blackboard',
                                          'frame': 'supplementaries:special_models/block/blackboard_frame'}:
                    return None
                authored = resolve(models / 'special_models/block/blackboard_frame.json', models, namespace)
            if authored == elements:
                return family, name
            # Current main already carries bounded, hash-guarded source repairs.
            # Accept only their exact recorded output and prove the restored mesh
            # still equals the preserved source before considering hidden seams.
            import imported_geometry_repairs as repairs
            record = read(repairs.MANIFEST).get(path.relative_to(repairs.MODELS).as_posix())
            if record and repairs.digest(elements) == record['after_sha256']:
                restored = deepcopy(elements)
                for index, change in record['elements'].items():
                    restored[int(index)] = change['before']
                if restored == authored and repairs.digest(restored) == record['before_sha256']:
                    return family, name
            # Geometry finishing is deterministic and source-bounded; accept its
            # output too, without accepting arbitrary altered source meshes.
            import newest_model_finishing as finishing
            relative = family + '/' + name.removesuffix('.json')
            return (family, name) if relative in finishing.TARGETS and finishing.finalized_elements(relative) == elements else None
        data = read(path)
        parent = data.get('parent', '')
        if not parent.startswith('jugcraft:'):
            return None
        path = ASSETS / 'models' / (parent.split(':', 1)[1] + '.json')
    return None


def geometry(apply):
    if apply.get('x', 0) != 0 or apply.get('y', 0) not in (0, 90, 180, 270):
        raise ValueError('Non-quarter-turn multipart transform')
    identifier = apply['model']
    if not identifier.startswith('jugcraft:'):
        raise ValueError('Non-local multipart model')
    result = deepcopy(resolve(ASSETS / 'models' / (identifier.split(':', 1)[1] + '.json'), ASSETS / 'models', 'jugcraft'))
    for element in result:
        if element.get('rotation', {}).get('angle', 0) != 0:
            raise ValueError('Rotated multipart solid')
        element.pop('rotation', None)
    for _ in range(apply.get('y', 0) // 90):
        for element in result:
            f, t = element['from'], element['to']
            element['from'], element['to'] = [16-t[2], f[1], f[0]], [16-f[2], t[1], t[0]]
            element['faces'] = {TURN[k]: v for k, v in element.get('faces', {}).items()}
    return result


def wall_shape(elements, height, post=False):
    if len(elements) != 1:
        return False
    element = elements[0]
    return (element.get('from') == ([4, 0, 4] if post else [5, 0, 0])
            and element.get('to') == ([12, 16, 12] if post else [11, height, 8])
            and not element.get('rotation')
            and set(element.get('faces', {})) == ({'north', 'south', 'east', 'west', 'up', 'down'} if post
                                                else {'north', 'east', 'west', 'up', 'down'}))


def vanilla_wall_seam(name, elements):
    """Jugtania registers these as native WallBlock; preserve exact vanilla side/post composition.

    Bounds and omitted middle face match 26.3's template_wall_side/_tall. The
    canonical WallBlock multipart maps UP to its complete post and each side's
    LOW/TALL state to the corresponding rotated arm, including weighted variants.
    """
    stem = Path(name).stem
    if '_wall_side' not in stem:
        return False
    base = stem.split('_wall_side', 1)[0] + '_wall'
    height = 16 if '_wall_side_tall' in stem else 14
    if not wall_shape(elements, height):
        return False
    parts = read(ASSETS / 'blockstates' / ('jugtania_' + base + '.json')).get('multipart', [])
    expected = {('up', 'true', 0): 16}
    expected.update({(side, state, rotation): h for side, rotation in
                     [('north', 0), ('east', 90), ('south', 180), ('west', 270)]
                     for state, h in [('low', 14), ('tall', 16)]})
    seen = set()
    for part in parts:
        when = part.get('when', {})
        if len(when) != 1:
            return False
        side, state = next(iter(when.items()))
        choices = part.get('apply', [])
        choices = choices if isinstance(choices, list) else [choices]
        if not choices:
            return False
        for choice in choices:
            key = side, state, choice.get('y', 0)
            if key not in expected or choice.get('x', 0) != 0:
                return False
            identifier = choice.get('model', '')
            if not identifier.startswith('jugcraft:newest/jugtania/block/' + base + '_'):
                return False
            shape = resolve(ASSETS / 'models' / (identifier.split(':', 1)[1] + '.json'), ASSETS / 'models', 'jugcraft')
            if not wall_shape(shape, expected[key], side == 'up'):
                return False
            if opaque_elements(ASSETS / 'models' / (identifier.split(':', 1)[1] + '.json')) != {0}:
                return False
            seen.add(key)
    return seen == set(expected)


def composed_faces(parts, target, candidates):
    """Require every emitted instance's candidate seams to be covered in every Boolean state."""
    keys = sorted({key for part in parts for key in part.get('when', {})})
    if len(keys) > 8 or any(value not in ('true', 'false') for part in parts for value in part.get('when', {}).values()):
        return set()
    loaded = [(part.get('when', {}), part['apply'], geometry(part['apply']),
               opaque_elements(ASSETS / 'models' / (part['apply']['model'].split(':', 1)[1] + '.json'))) for part in parts]
    proven = set(candidates)
    emitted = False
    for values in product(('false', 'true'), repeat=len(keys)):
        state = dict(zip(keys, values))
        combined, targets, opaque = [], [], set()
        for when, apply, shapes, solid in loaded:
            if all(state[key] == value for key, value in when.items()):
                if apply['model'] == target:
                    emitted = True
                    turns = apply.get('y', 0) // 90
                    for index, face in candidates:
                        turned = face
                        for _ in range(turns):
                            turned = TURN[turned]
                        targets.append(((index, face), (len(combined) + index, turned)))
                opaque.update(len(combined) + index for index in solid)
                combined.extend(shapes)
        exposed = exposed_seams(combined, [transformed for _, transformed in targets], opaque)
        proven.difference_update(original for original, transformed in targets if transformed in exposed)
    return proven if emitted else set()


def backpack_seams(name, candidates):
    """These are source components; every actual backpack tier renders the complete assembly."""
    import newest_model_finishing as finishing
    part = Path(name).stem
    if part not in ('backpack_front_pouch', 'backpack_left_pouch'):
        return set()
    offset = 0
    for component in finishing.PACK_PARTS:
        if component == part:
            break
        offset += len(finishing.original_elements('packworks/block/' + component))
    proven = set(candidates)
    for tier in finishing.TIERS:
        relative = 'packworks/block/' + tier
        elements = resolve(MODELS / (relative + '.json'), ASSETS / 'models', 'jugcraft')
        if elements not in (finishing.original_elements(relative), finishing.finalized_elements(relative)):
            return set()
        variants = read(ASSETS / 'blockstates' / ('packworks_' + tier + '.json')).get('variants', {})
        expected = {'facing=' + direction: {'model': 'jugcraft:newest/' + relative, 'y': turn}
                    for direction, turn in [('north', 0), ('east', 90), ('south', 180), ('west', 270)]}
        if variants != expected:
            return set()
        composed = deepcopy(elements)
        for element in composed:
            if element.get('rotation', {}).get('angle', 0) == 0:
                element.pop('rotation', None)
        surface_faces, native = material_faces(MODELS / (relative + '.json'))
        surfaces = {face.label: face for face in surface_faces}
        exposed = exposed_seams(composed, [(offset + index, face) for index, face in candidates],
                                opaque_elements(MODELS / (relative + '.json')), (surfaces, native))
        proven.difference_update((index, face) for index, face in candidates if (offset + index, face) in exposed)
    return proven


def opaque_texture(path, variable):
    textures = {}
    for _ in range(12):
        data = read(path)
        textures = {**data.get('textures', {}), **textures}
        parent = data.get('parent', '')
        if not parent.startswith('jugcraft:'):
            break
        path = ASSETS / 'models' / (parent.split(':', 1)[1] + '.json')
    reference = variable
    for _ in range(12):
        if not reference.startswith('#'):
            break
        reference = textures.get(reference[1:], '')
    if not reference.startswith('jugcraft:'):
        return False
    with Image.open(ASSETS / 'textures' / (reference.split(':', 1)[1] + '.png')) as image:
        return image.convert('RGBA').getchannel('A').getextrema() == (255, 255)


def wicker_seams(name, elements, candidates):
    target = 'jugcraft:newest/supps/' + name.removesuffix('.json')
    parts = read(ASSETS / 'blockstates/supps_wicker_fence.json')['multipart']
    shell = [part for part in parts if not part.get('when')]
    content = [part for part in parts if part.get('apply', {}).get('model') == target]
    expected = {('north', 0), ('east', 90), ('south', 180), ('west', 270)}
    state = 'tall' if name.endswith('_tall.json') else 'low'
    if len(content) != 4 or {(next(iter(p.get('when', {})), ''), p['apply'].get('y', 0)) for p in content} != expected:
        return set()
    if any(len(p['when']) != 1 or next(iter(p['when'].values())) != state or p['apply'].get('x', 0) != 0 for p in content):
        return set()
    if len(shell) != 1 or shell[0]['apply'] != {'model': 'jugcraft:newest/supps/block/wicker_fence_stick'}:
        return set()
    frame = geometry(shell[0]['apply'])
    if original(MODELS / 'supps/block/wicker_fence_stick.json', frame) is None:
        return set()
    opaque = opaque_elements(MODELS / ('supps/' + name))
    opaque.update(len(elements) + index for index in opaque_elements(MODELS / 'supps/block/wicker_fence_stick.json'))
    exposed = exposed_seams(elements + frame, candidates, opaque)
    return set(candidates) - exposed


def intentional_open_faces(path, elements, candidates):
    """Return only proven source seams, never whole-model or whole-namespace exemptions."""
    try:
        source = original(path, elements)
        if source is None:
            return set()
        family, name = source
        if family == 'jugtania' and vanilla_wall_seam(name, elements):
            return set(candidates) & {(0, 'south')}
        if family == 'packworks':
            return backpack_seams(name, candidates)
        if family == 'supps' and name in ('block/wicker_fence_side.json', 'block/wicker_fence_side_tall.json'):
            return wicker_seams(name, elements, candidates)
        if family == 'supps' and name in ('block/blackboard.json', 'special_models/block/blackboard_frame.json'):
            if (len(elements) == 2 and elements[0]['from'] == [0, 0, 11] and elements[0]['to'] == [16, 16, 16]
                    and set(elements[0]['faces']) == {'up', 'down', 'east', 'west', 'south'}
                    and elements[1]['from'] == [0, 0, 11.25] and elements[1]['to'] == [16, 16, 11.25]
                    and set(elements[1]['faces']) == {'north'}
                    and opaque_texture(Path(path), elements[1]['faces']['north']['texture'])):
                return set(candidates) & {(0, 'north')}
        if family == 'homeworks' and name.startswith('block/') and name.endswith('_large_stump_corner_piece.json'):
            stem = Path(name).stem.removesuffix('_corner_piece')
            parts = read(ASSETS / 'blockstates' / ('homeworks_' + stem + '.json'))['multipart']
            return composed_faces(parts, 'jugcraft:newest/homeworks/' + name[:-5], candidates)
        if family == 'aquatic' and name.startswith('block/worm_farm_contents'):
            parts = read(ASSETS / 'blockstates/aquatic_worm_farm.json')['multipart']
            target = 'jugcraft:newest/aquatic/' + name[:-5]
            shell = [part for part in parts if not part.get('when')]
            content = [part for part in parts if part.get('apply', {}).get('model') == target]
            if len(shell) == 1 and shell[0]['apply'] == {'model': 'jugcraft:newest/aquatic/block/worm_farm'} and len(content) == 1:
                frame = geometry(shell[0]['apply'])
                if original(ASSETS / 'models/newest/aquatic/block/worm_farm.json', frame) is not None:
                    opaque = opaque_elements(Path(path))
                    opaque.update(len(elements) + index for index in opaque_elements(MODELS / 'aquatic/block/worm_farm.json'))
                    exposed = exposed_seams(elements + frame, candidates, opaque)
                    return set(candidates) - exposed
    except (OSError, ValueError, KeyError, TypeError):
        return set()
    return set()
