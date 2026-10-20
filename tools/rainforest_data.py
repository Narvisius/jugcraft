"""Non-timber rainforest stems; shared trees_data owns the saplings and fronds."""
from decor_data import rid, self_drop


def assets(root, write, lang):
    pillar(root, write, 'rubber_log')
    pillar(root, write, 'tree_fern_trunk')
    lang['block.jugcraft.tree_fern_trunk'] = 'Tree Fern Trunk'
    lang['block.jugcraft.rubber_log'] = 'Rubber Log'
    lang['block.jugcraft.tree_tap'] = 'Tree Tap'
    lang['item.jugcraft.latex_bottle'] = 'Latex Bottle'
    lang['message.jugcraft.tree_tap.fill'] = 'Latex: %s/%s bottles (+%s%% toward the next bottle)'
    lang['message.jugcraft.tree_tap.inventory_full'] = 'Make room for Latex Bottles before collecting.'
    for level in range(5):
        def cube(lo, hi, texture):
            return {'from': lo, 'to': hi, 'faces': {f: {'texture': texture} for f in ('up', 'down', 'north', 'south', 'east', 'west')}}
        elements = [cube([5, 1, 8], [11, 3, 15], '#wood'), cube([4, 1, 8], [5, 7, 15], '#wood'),
                    cube([11, 1, 8], [12, 7, 15], '#wood'), cube([5, 1, 8], [11, 7, 9], '#wood'),
                    cube([5, 1, 14], [11, 7, 15], '#wood'), cube([7, 6, 14], [9, 11, 16], '#metal'),
                    cube([7, 10, 11], [9, 12, 16], '#metal')]
        if level: elements.append(cube([5, 3, 9], [11, 3+level*.75, 14], '#latex'))
        write(root / 'models' / 'block' / f'tree_tap_{level}.json', {'parent': 'minecraft:block/block',
            'textures': {'particle': rid('block/tree_tap'), 'wood': rid('block/tree_tap'), 'metal': 'minecraft:block/iron_block', 'latex': rid('block/latex')}, 'elements': elements})
    for level in range(33):
        # A visibly larger iron collector, with an open top and a rising cream surface.
        elements = [cube([4, 0, 5], [12, 2, 14], '#metal'),
                    cube([3, 2, 4], [4, 9, 15], '#metal'), cube([12, 2, 4], [13, 9, 15], '#metal'),
                    cube([4, 2, 4], [12, 9, 5], '#metal'), cube([4, 2, 14], [12, 9, 15], '#metal'),
                    cube([3, 9, 4], [4, 10, 15], '#metal'), cube([12, 9, 4], [13, 10, 15], '#metal'),
                    cube([4, 9, 4], [12, 10, 5], '#metal'), cube([4, 9, 14], [12, 10, 15], '#metal'),
                    cube([7, 9, 15], [9, 12, 16], '#metal'), cube([7, 12, 10], [9, 13, 16], '#metal'),
                    cube([6, 1, 3.5], [10, 9.5, 4], '#wood')]
        if level:
            elements.append(cube([4, 2, 5], [12, 2 + level * 7 / 32, 14], '#latex'))
            # A narrow front gauge stays readable when the tap is mounted above eye level.
            elements.append(cube([7, 2, 3.25], [9, 2 + level * 7 / 32, 3.5], '#latex'))
        write(root / 'models' / 'block' / f'tree_tap_bucket_{level}.json', {'parent': 'minecraft:block/block',
            'textures': {'particle': 'minecraft:block/iron_block', 'wood': rid('block/tree_tap'), 'metal': 'minecraft:block/iron_block', 'latex': rid('block/latex')}, 'elements': elements})
    write(root / 'blockstates' / 'tree_tap.json', {'variants': {
        f'facing={side},latex={level},upgraded={str(upgraded).lower()}': {
            'model': rid(f'block/tree_tap_bucket_{level}' if upgraded else f'block/tree_tap_{min(level, 4)}'), 'y': rotation}
        for side, rotation in [('north', 0), ('east', 90), ('south', 180), ('west', 270)]
        for level in range(33) for upgraded in (False, True)}})
    write(root / 'items' / 'tree_tap.json', {'model': {'type': 'minecraft:select', 'property': 'minecraft:block_state',
        'block_state_property': 'upgraded', 'cases': [{'when': 'true', 'model': {'type': 'minecraft:model', 'model': rid('block/tree_tap_bucket_0')}}],
        'fallback': {'type': 'minecraft:model', 'model': rid('block/tree_tap_0')}}})
    write(root / 'models' / 'item' / 'latex_bottle.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': rid('item/latex_bottle')}})
    write(root / 'items' / 'latex_bottle.json', {'model': {'type': 'minecraft:model', 'model': rid('item/latex_bottle')}})


def pillar(root, write, stem):
    for suffix, parent in [('', 'cube_column'), ('_horizontal', 'cube_column_horizontal')]:
        write(root / 'models' / 'block' / f'{stem}{suffix}.json', {'parent': 'minecraft:block/' + parent,
            'textures': {'end': rid('block/' + stem + '_top'), 'side': rid('block/' + stem)}})
    write(root / 'blockstates' / f'{stem}.json', {'variants': {
        'axis=y': {'model': rid('block/' + stem)},
        'axis=x': {'model': rid('block/' + stem + '_horizontal'), 'x': 90, 'y': 90},
        'axis=z': {'model': rid('block/' + stem + '_horizontal'), 'x': 90}}})
    write(root / 'items' / f'{stem}.json', {'model': {'type': 'minecraft:model', 'model': rid('block/' + stem)}})


def loot(out, write):
    for block in ('tree_fern_trunk', 'rubber_log', 'tree_tap'):
        data = self_drop(block)
        if block == 'tree_tap':
            # Keep the paid bucket upgrade when moved; contents are lost on breaking.
            data['pools'][0]['entries'][0]['modifier'] = {'type': 'minecraft:copy_state', 'block': rid(block), 'properties': ['upgraded']}
        write(out / f'{block}.json', data)


def tags(tags):
    for registry in ('block', 'item'):
        tags.add(registry, 'minecraft:logs', rid('tree_fern_trunk'))
        tags.add(registry, 'minecraft:logs_that_burn', rid('tree_fern_trunk'))
    tags.add('block', 'minecraft:mineable/axe', rid('tree_fern_trunk'))
    tags.add('block', 'minecraft:mineable/axe', rid('rubber_log'))
    tags.add('block', 'minecraft:mineable/axe', rid('tree_tap'))
    for registry in ('block', 'item'): tags.add(registry, 'minecraft:logs', rid('rubber_log'))
