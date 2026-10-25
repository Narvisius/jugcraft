"""Original AE building meshes/blockstates and native certus growing chain."""
import json

FAMILIES = [('quartz', 'quartz_block'), ('quartz_brick', 'quartz_bricks'), ('chiseled_quartz', 'chiseled_quartz_block'),
            ('cut_quartz', 'cut_quartz_block'), ('smooth_quartz', 'smooth_quartz_block'), ('quartz_pillar', 'quartz_pillar'),
            ('fluix', 'fluix_block'), ('sky_stone', 'sky_stone_block'), ('sky_stone_brick', 'sky_stone_brick'),
            ('sky_stone_small_brick', 'sky_stone_small_brick'), ('smooth_sky_stone', 'smooth_sky_stone_block')]
BUDDING = [f'{grade}_budding_quartz' for grade in ('damaged', 'chipped', 'flawed', 'flawless')]
BUDS = ['small_quartz_bud', 'medium_quartz_bud', 'large_quartz_bud', 'quartz_cluster']
BLOCKS = [n for stem, base in FAMILIES for n in (base, stem + '_stairs', stem + '_slab', stem + '_wall')] + BUDDING + BUDS


def generate(source, assets, resources, model, dump, recipe):
    def references(value):
        if isinstance(value, dict):
            return {k: model(v[4:]) if k == 'model' and isinstance(v, str) and v.startswith('ae2:') else references(v) for k, v in value.items()}
        if isinstance(value, list): return [references(v) for v in value]
        return value
    def ji(n): return 'jugcraft:jimta_' + n
    for name in BLOCKS:
        data = json.loads((source / f'blockstates/{name}.json').read_text(encoding='utf-8'))
        dump(assets / f'blockstates/jimta_{name}.json', references(data))
        item = json.loads((source / f'items/{name}.json').read_text(encoding='utf-8'))
        dump(assets / f'items/jimta_{name}.json', references(item))
    for stem, base in FAMILIES:
        for suffix, count, pattern in [('slab', 6, ['BBB']), ('stairs', 4, ['B  ', 'BB ', 'BBB']), ('wall', 6, ['BBB', 'BBB'])]:
            name = stem + '_' + suffix
            recipe(name, {'type': 'minecraft:crafting_shaped', 'pattern': pattern, 'key': {'B': ji(base)}, 'result': {'id': ji(name), 'count': count}})
            recipe(name + '_cutting', {'type': 'minecraft:stonecutting', 'ingredient': ji(base), 'result': {'id': ji(name), 'count': 2 if suffix == 'slab' else 1}})
        if base not in ('quartz_block', 'fluix_block', 'sky_stone_block'):
            parent = 'sky_stone_block' if 'sky_stone' in base else 'quartz_block'
            recipe(base + '_cutting', {'type': 'minecraft:stonecutting', 'ingredient': ji(parent), 'result': {'id': ji(base)}})
    for base, material in [('quartz_block', 'certus_quartz_crystal'), ('fluix_block', 'fluix_crystal')]:
        recipe(base, {'type': 'minecraft:crafting_shaped', 'pattern': ['CC', 'CC'], 'key': {'C': ji(material)}, 'result': {'id': ji(base)}})
        recipe(material + '_from_block', {'type': 'minecraft:crafting_shapeless', 'ingredients': [ji(base)], 'result': {'id': ji(material), 'count': 4}})
    recipe('sky_stone_block', {'type': 'minecraft:crafting_shaped', 'pattern': ['DCD', 'COC', 'DCD'], 'key': {'D': 'minecraft:cobbled_deepslate', 'C': ji('certus_quartz_dust'), 'O': 'minecraft:obsidian'}, 'result': {'id': ji('sky_stone_block'), 'count': 8}})
    recipe('flawed_budding_quartz', {'type': 'minecraft:crafting_shaped', 'pattern': ['CCC', 'CAC', 'CCC'], 'key': {'C': ji('certus_quartz_crystal'), 'A': 'minecraft:amethyst_block'}, 'result': {'id': ji('flawed_budding_quartz')}})
    recipe('flawless_budding_quartz', {'type': 'minecraft:crafting_shaped', 'pattern': ['FDF', 'DBD', 'FDF'], 'key': {'F': ji('fluix_crystal'), 'D': 'minecraft:diamond', 'B': ji('flawed_budding_quartz')}, 'result': {'id': ji('flawless_budding_quartz')}})
    for damaged in ['damaged_budding_quartz', 'chipped_budding_quartz']:
        recipe(damaged + '_restoration', {'type': 'minecraft:crafting_shapeless', 'ingredients': [ji(damaged), ji('certus_quartz_crystal'), ji('certus_quartz_crystal')], 'result': {'id': ji('flawed_budding_quartz')}})
    # Double slabs return both blocks; clusters provide the renewable material instead of a plain block drop.
    for name in BLOCKS:
        entry = {'type': 'minecraft:item', 'name': ji('certus_quartz_crystal') if name == 'quartz_cluster' else ji(name)}
        if name.endswith('_slab'):
            entry['modifier'] = [{'type': 'minecraft:set_count', 'count': 2, 'condition': {'type': 'minecraft:match_block', 'blocks': ji(name), 'state': {'type': 'double'}}}]
        elif name == 'quartz_cluster':
            entry['modifier'] = [{'type': 'minecraft:set_count', 'count': 4}]
        dump(resources / f'data/jugcraft/loot_table/blocks/jimta_{name}.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [entry], 'condition': {'type': 'minecraft:survives_explosion'}}]})
    for tagname, suffix in [('walls', '_wall'), ('slabs', '_slab'), ('stairs', '_stairs')]:
        for registry in ['block', 'item']:
            path = resources / f'data/minecraft/tags/{registry}/{tagname}.json'
            data = json.loads(path.read_text(encoding='utf-8')) if path.exists() else {'replace': False, 'values': []}
            data['values'] = list(dict.fromkeys(data['values'] + [ji(n) for n in BLOCKS if n.endswith(suffix)])); dump(path, data)
