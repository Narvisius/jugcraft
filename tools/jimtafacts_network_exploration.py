"""Original meteorite/tiny TNT art and data; native source-equation world generation."""
import copy
import json
import math

BLOCKS = ['tiny_tnt', 'mysterious_cube']
ITEMS = ['meteorite_compass']
ENTITIES = ['jimta_tiny_tnt']


def generate(source, assets, resources, model, texture, dump, recipe):
    def ji(name): return 'jugcraft:jimta_' + name
    def references(value):
        if isinstance(value, dict): return {k: model(v[4:]) if k == 'model' and isinstance(v, str) and v.startswith('ae2:') else references(v) for k, v in value.items()}
        if isinstance(value, list): return [references(v) for v in value]
        return value
    for name in BLOCKS:
        dump(assets / f'blockstates/jimta_{name}.json', references(json.loads((source / f'blockstates/{name}.json').read_text(encoding='utf-8'))))
        dump(assets / f'items/jimta_{name}.json', references(json.loads((source / f'items/{name}.json').read_text(encoding='utf-8'))))
    original = json.loads((source / 'models/item/meteorite_compass_pointer.json').read_text(encoding='utf-8'))
    pointer_ref = model('item/meteorite_compass_pointer')
    pointer = json.loads((assets / 'models/jimtafacts/ae2/item/meteorite_compass_pointer.json').read_text(encoding='utf-8'))
    base_ref = model('item/meteorite_compass_base')
    entries = []
    # 128 poses preserve every original face/UV; Fabric's existing mesh loader permits arbitrary Y rotations.
    for frame in range(128):
        angle = frame * math.tau / 128
        c, s = math.cos(angle), math.sin(angle)
        def stable(value):
            # Windows and Linux libm can differ by one ULP. Nine model-unit
            # decimals preserve the source shape while producing identical JSON.
            result = round(value, 9)
            return result if result else 0.0
        def rotate(p):
            x, y, z = p
            return [stable((x-8)*c + (z-8)*s + 8), y, stable(-(x-8)*s + (z-8)*c + 8)]
        def normal(n):
            x, y, z = n
            return [stable(x*c + z*s), y, stable(-x*s + z*c)]
        quads = []
        for element in original['elements']:
            a, b, d = element['from']; x, y, z = element['to']
            faces = {
                'north': ([(x,y,d),(x,b,d),(a,b,d),(a,y,d)], (0,0,-1)),
                'south': ([(a,y,z),(a,b,z),(x,b,z),(x,y,z)], (0,0,1)),
                'west': ([(a,y,d),(a,b,d),(a,b,z),(a,y,z)], (-1,0,0)),
                'east': ([(x,y,z),(x,b,z),(x,b,d),(x,y,d)], (1,0,0)),
                'up': ([(a,y,d),(a,y,z),(x,y,z),(x,y,d)], (0,1,0)),
                'down': ([(a,b,z),(a,b,d),(x,b,d),(x,b,z)], (0,-1,0)),
            }
            for face, data in element['faces'].items():
                corners, n = faces[face]; u0, v0, u1, v1 = [v / 16 for v in data['uv']]
                uv = [(u0,v0),(u0,v1),(u1,v1),(u1,v0)]
                turn = data.get('rotation', 0) // 90
                uv = uv[turn:] + uv[:turn]
                quads.append({'t': data['texture'].lstrip('#'), 'v': [rotate(point) + list(tex) + normal(n) for point, tex in zip(corners, uv)]})
        output = copy.deepcopy(pointer)
        output['fabric:type'] = {'id': 'jugcraft:mesh', 'optional': True}
        output['quads'] = quads
        name = f'item/jimtafacts/ae2/meteorite_pointer_{frame:03}'
        dump(assets / f'models/{name}.json', output)
        entries.append({'threshold': frame / 128, 'model': {'type': 'minecraft:model', 'model': 'jugcraft:' + name}})
    dump(assets / 'items/jimta_meteorite_compass.json', {'model': {'type': 'minecraft:composite', 'models': [
        {'type': 'minecraft:model', 'model': base_ref},
        {'type': 'minecraft:range_dispatch', 'property': 'jugcraft:jimta_meteorite_direction', 'entries': entries,
         'fallback': {'type': 'minecraft:model', 'model': pointer_ref}}]}})
    data = source.parents[1] / 'data/ae2'
    source_recipe = json.loads((data / 'recipe/misc/tiny_tnt.json').read_text(encoding='utf-8'))
    source_recipe['key']['a'] = ji('certus_quartz_dust')
    source_recipe['result']['id'] = ji('tiny_tnt')
    recipe('tiny_tnt', source_recipe)
    # The compass is obtained by the real native charger, same source input. No parallel crafting shortcut.
    dump(resources / 'jugcraft/jimtafacts/meteorite_compass_charging.json', {'machine': ji('charger'), 'ingredient': 'minecraft:compass', 'result': ji('meteorite_compass'), 'energy_je': 1000})
    meteor = json.loads((data / 'worldgen/structure/meteorite.json').read_text(encoding='utf-8'))
    meteor['type'] = 'jugcraft:jimtafacts/meteorite'; meteor['biomes'] = '#jugcraft:jimtafacts/has_meteorites'
    dump(resources / 'data/jugcraft/worldgen/structure/jimtafacts/meteorite.json', meteor)
    placements = json.loads((data / 'worldgen/structure_set/meteorite.json').read_text(encoding='utf-8'))
    placements['structures'][0]['structure'] = 'jugcraft:jimtafacts/meteorite'
    dump(resources / 'data/jugcraft/worldgen/structure_set/jimtafacts/meteorite.json', placements)
    dump(resources / 'data/jugcraft/tags/worldgen/biome/jimtafacts/has_meteorites.json', json.loads((data / 'tags/worldgen/biome/has_meteorites.json').read_text(encoding='utf-8')))
    dump(resources / 'data/jugcraft/tags/worldgen/structure/jimtafacts/meteorites.json', {'values': ['jugcraft:jimtafacts/meteorite']})
    # Source cube yields the four reusable dies, or itself when harvested with Silk Touch.
    nonsilk = {'type': 'minecraft:inverted', 'term': 'minecraft:tool/can_silk_touch'}
    pools = [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': ji('mysterious_cube')}], 'condition': 'minecraft:tool/can_silk_touch'}]
    for press in ('calculation_processor_press', 'engineering_processor_press', 'logic_processor_press', 'silicon_press'):
        pools.append({'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': ji(press)}], 'condition': {'type': 'minecraft:all_of', 'terms': [nonsilk, {'type': 'minecraft:survives_explosion'}]}})
    pools.append({'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'jugcraft:engineers_handbook'}], 'condition': nonsilk})
    dump(resources / 'data/jugcraft/loot_table/blocks/jimta_mysterious_cube.json', {'type': 'minecraft:block', 'pools': pools})
