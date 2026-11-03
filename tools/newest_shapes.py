"""Derive static selection/collision boxes from supplied architectural meshes."""
import itertools
import math
import json
import zipfile
from pathlib import Path
from functools import lru_cache
from menagerie_content import read


@lru_cache(maxsize=256)
def vanilla_model(path):
    if path=='block/template_farmland':path='block/template_cube_bottom_top_indented'
    # Read pinned game assets as shape references; never copy game art into the mod.
    root = Path(__file__).resolve().parents[1]
    jars = sorted((root / '.gradle/loom-cache/minecraftMaven/net/minecraft').glob('minecraft-clientOnly-*/26.3/*.jar'))
    # CI prepares the verified pinned client archive before Loom creates its split JARs.
    pinned = Path.home() / ".gradle/caches/fabric-loom/26.3/minecraft-client.jar"
    if pinned.is_file():
        jars.append(pinned)
    for jar in jars:
        with zipfile.ZipFile(jar) as archive:
            name = 'assets/minecraft/models/' + path + '.json'
            if name in archive.namelist(): return json.loads(archive.read(name))
    raise ValueError('Pinned vanilla shape reference unavailable: ' + path)


def boxes_for(assets, namespace, variant):
    if isinstance(variant, list):
        return [box for option in variant for box in boxes_for(assets, namespace, option)]
    model = variant['model']
    visited = set()
    while True:
        if model in visited: raise ValueError('Cyclic architectural model: ' + model)
        visited.add(model)
        ns, path = model.split(':', 1) if ':' in model else ('minecraft', model)
        if ns != namespace:
            if path == 'block/air': return []
            if ns != 'minecraft': raise ValueError('Unknown external architectural shape: ' + model)
            data = vanilla_model(path)
        elif namespace == 'cozyhome':
            from newest_home import supplied_model
            data = supplied_model(assets, path)
            if data is None: raise ValueError('Missing Home geometry: ' + path)
        else: data = read(assets / 'models' / (path + '.json'))
        if 'elements' in data: elements = data['elements']; break
        if namespace=='supplementaries' and data.get('loader') in {'supplementaries:faucet','supplementaries:flower_box','supplementaries:jar','supplementaries:goblet','supplementaries:frame_block','supplementaries:blackboard'}:
            model=data.get('model',data.get('box',data.get('overlay',data.get('frame'))));continue
        if 'parent' not in data: return []
        model = data['parent']

    def rotate(point, axis, degrees, origin):
        point = [point[i] - origin[i] for i in range(3)]
        a, b = {'x': (1, 2), 'y': (2, 0), 'z': (0, 1)}[axis]
        angle = math.radians(degrees); c, s = math.cos(angle), math.sin(angle)
        point[a], point[b] = point[a] * c - point[b] * s, point[a] * s + point[b] * c
        return [point[i] + origin[i] for i in range(3)]

    result = []
    for element in elements:
        points = list(itertools.product(*zip(element['from'], element['to'])))
        rotation = element.get('rotation')
        if rotation:
            origin = rotation.get('origin', [8, 8, 8])
            points = [rotate(p, rotation['axis'], rotation['angle'], origin) for p in points]
            if rotation.get('rescale'):
                fixed_axis = 'xyz'.index(rotation['axis'])
                factor = 1 / math.cos(math.radians(rotation['angle']))
                points = [[origin[i] + (p[i] - origin[i]) * (1 if i == fixed_axis else factor)
                           for i in range(3)] for p in points]
        # Blockstate rotations are clockwise in model space.
        for axis in ('x', 'y'):
            if variant.get(axis): points = [rotate(p, axis, -variant[axis], [8, 8, 8]) for p in points]
        low = [min(p[i] for p in points) for i in range(3)]
        high = [max(p[i] for p in points) for i in range(3)]
        if any(high[i] - low[i] < .001 for i in range(3)): continue  # Decorative flat faces are not walls.
        result.append([round(v, 5) for v in low + high])
    return result


def variants(assets, namespace, state, properties=None):
    if 'variants' in state:
        return {','.join(sorted(key.split(','))): boxes_for(assets, namespace, value)
                for key, value in state['variants'].items()}
    def matches(condition, values):
        return all(any(matches(c, values) for c in value) if key == 'OR' else
                   all(matches(c, values) for c in value) if key == 'AND' else values.get(key) in str(value).lower().split('|')
                   for key, value in condition.items())
    if properties is None: raise ValueError('Multipart architectural shapes require declared states')
    result = {}
    keys = sorted(properties)
    for combination in itertools.product(*(properties[key] for key in keys)):
        values = dict(zip(keys, combination)); boxes = []
        for part in state['multipart']:
            if matches(part.get('when', {}), values): boxes.extend(boxes_for(assets, namespace, part['apply']))
        result[','.join(key + '=' + values[key] for key in keys)] = boxes
    return result
