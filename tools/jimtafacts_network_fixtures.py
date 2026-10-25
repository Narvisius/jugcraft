"""Source quartz fixture/growth meshes and QuartzGlassModel edge masks, ported to vanilla multipart."""
import json

BLOCKS = ['quartz_fixture', 'quartz_glass', 'quartz_vibrant_glass', 'growth_accelerator']
EDGES = {'down': ('south', 'east', 'north', 'west'), 'up': ('south', 'west', 'north', 'east'),
         'north': ('up', 'west', 'down', 'east'), 'south': ('up', 'east', 'down', 'west'),
         'west': ('up', 'south', 'down', 'north'), 'east': ('up', 'north', 'down', 'south')}


def generate(source, assets, resources, model, texture, dump, recipe):
    def refs(value):
        if isinstance(value, dict): return {k: model(v[4:]) if k == 'model' and isinstance(v, str) and v.startswith('ae2:') else refs(v) for k, v in value.items()}
        if isinstance(value, list): return [refs(v) for v in value]
        return value
    def ji(name): return 'jugcraft:jimta_' + name
    for name in ['quartz_fixture', 'growth_accelerator']:
        dump(assets / f'blockstates/jimta_{name}.json', refs(json.loads((source / f'blockstates/{name}.json').read_text(encoding='utf-8'))))
    for name in BLOCKS:
        dump(assets / f'items/jimta_{name}.json', refs(json.loads((source / f'items/{name}.json').read_text(encoding='utf-8'))))
        dump(resources / f'data/jugcraft/loot_table/blocks/jimta_{name}.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': ji(name)}], 'condition': {'type': 'minecraft:survives_explosion'}}]})
    # Original face ordering and bit significance from QuartzGlassModel.makeBitmask; only the dynamic
    # NeoForge wrapper changes. Weighted source panes retain deterministic block-seeded variation.
    particle = texture('ae2:block/glass/quartz_glass_frame1111')
    multipart = []
    for face, edges in EDGES.items():
        variants = []
        for pane in 'abcd':
            ref = f'jugcraft:block/jimtafacts/ae2/glass/{face}_{pane}'
            tex = texture(f'ae2:block/glass/quartz_glass_{pane}')
            dump(assets / f'models/block/jimtafacts/ae2/glass/{face}_{pane}.json', {'ambientocclusion': False, 'textures': {'particle': particle, 'face': tex}, 'elements': [{'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': {face: {'uv': [0, 0, 16, 16], 'texture': '#face', 'cullface': face}}}]})
            variants.append({'model': ref})
        multipart.append({'when': {face: 'false'}, 'apply': variants})
        for mask in range(1, 16):
            ref = f'jugcraft:block/jimtafacts/ae2/glass/{face}_{mask:04b}'
            tex = texture(f'ae2:block/glass/quartz_glass_frame{mask:04b}')
            dump(assets / f'models/block/jimtafacts/ae2/glass/{face}_{mask:04b}.json', {'ambientocclusion': False, 'textures': {'particle': particle, 'face': tex}, 'elements': [{'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': {face: {'uv': [0, 0, 16, 16], 'texture': '#face', 'cullface': face}}}]})
            when = {face: 'false', **{edge: str(not bool(mask & (1 << i))).lower() for i, edge in enumerate(edges)}}
            multipart.append({'when': when, 'apply': {'model': ref}})
    for name in ['quartz_glass', 'quartz_vibrant_glass']: dump(assets / f'blockstates/jimta_{name}.json', {'multipart': multipart})
    for name, pattern, keys, count in [
        ('quartz_fixture', ['Q', 'I'], {'Q': ji('certus_quartz_crystal'), 'I': 'minecraft:iron_ingot'}, 2),
        ('quartz_glass', ['GQG', 'QGQ', 'GQG'], {'G': 'minecraft:glass', 'Q': ji('certus_quartz_dust')}, 4),
        ('quartz_vibrant_glass', [' G ', 'GQG', ' G '], {'G': 'minecraft:glowstone_dust', 'Q': ji('quartz_glass')}, 1),
        ('growth_accelerator', ['IGI', 'FCF', 'IGI'], {'I': 'minecraft:iron_ingot', 'G': ji('quartz_glass'), 'F': ji('fluix_crystal'), 'C': ji('quartz_block')}, 1)]:
        recipe(name, {'type': 'minecraft:crafting_shaped', 'pattern': pattern, 'key': keys, 'result': {'id': ji(name), 'count': count}})
