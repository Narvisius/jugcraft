"""Candy Cavity worldgen repair; use supplied template geometry and existing textures.

Called after the broad cave/landmark importers so regeneration cannot restore the
old fixed-height placeholder structures or generic mushroom-shaped lollipops.
Original owner files remain unchanged. See docs/features/candy-cavity-generation.md.
"""
from copy import deepcopy
from menagerie_content import DATA, read, write
import nouveau_nbt

COLORS = ['pink', 'purple', 'yellow', 'light_blue', 'orange', 'lime']


def recolor_template(source, target, replacements):
    root = deepcopy(nouveau_nbt.read((DATA / 'structure' / (source + '.nbt')).read_bytes()))
    for state in root[2]['palette'][1][1]:
        name = state['id'][1]
        if name in replacements:
            state['id'] = (8, replacements[name])
            state.pop('properties', None)
    path = DATA / 'structure' / (target + '.nbt')
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(nouveau_nbt.write(root))


def main():
    write(DATA / 'worldgen/feature/cavern_chamber_candy_cavity.json', {'type': 'jugcraft:candy_cavity'})
    path = DATA / 'worldgen/biome/cavern_candy_cavity.json'
    data = read(path)
    # Candy details now belong to the deterministic chunk-local pass. Vanilla
    # surface plants and ore replacements must not punch grass/gravel into cake.
    data['features'] = [['jugcraft:cavern_chamber_candy_cavity']] + [[] for _ in range(10)]
    floor_details = ['candy_ruins', 'chocolate_ice_cream_scoop', 'sweetberry_ice_cream_scoop', 'vanilla_ice_cream_scoop',
                     'cookie_shelf', 'peppermint_patch', 'sprinkles_pile', 'sundrop_patch', 'sweet_puff', 'encrusted_peppermint', 'spilled_ice_cream_cone']
    ceiling_details = ['ceiling_frostmint', 'ceiling_ice_cream_cone']
    for names, step, up in [(floor_details, 9, False), (ceiling_details, 10, True)]:
        for name in names:
            data['features'][step].append('jugcraft:cavern_' + name)
            write(DATA / f'worldgen/placed_feature/cavern_{name}.json', {
                'feature': 'jugcraft:cavern_' + name, 'placement': [
                    {'type': 'minecraft:rarity_filter', 'chance': 12 if name == 'candy_ruins' else 16 if name == 'encrusted_peppermint' else 8 if name == 'peppermint_patch' else 5 if 'scoop' in name else 4},
                    {'type': 'minecraft:in_square'},
                    {'type': 'minecraft:height_range', 'height': {'absolute': 8 if up else -8}},
                    {'type': 'minecraft:environment_scan', 'direction_of_search': 'up' if up else 'down', 'max_steps': 32,
                     'allowed_search_condition': {'type': 'minecraft:matching_blocks', 'blocks': ['minecraft:air', 'minecraft:cave_air']},
                     'target_condition': {'type': 'minecraft:solid'}},
                    {'type': 'minecraft:offset', 'x': 0, 'z': 0, 'y': -1 if up else 1},
                    {'type': 'minecraft:biome'}]})
    # Small details are sparse accents, not carpets of upright candy discs.
    for name in ['peppermint_patch', 'encrusted_peppermint', 'sundrop_patch', 'sprinkles_pile', 'sweet_puff']:
        path = DATA / f'worldgen/feature/cavern_{name}.json'
        detail = read(path)
        detail['radius'] = 1 if name == 'encrusted_peppermint' else 2
        if name in ['peppermint_patch', 'encrusted_peppermint']:
            detail['primary']['properties'] = {'facing': 'up'}
        write(path, detail)
    for flavor in ['chocolate', 'sweetberry', 'vanilla']:
        path = DATA / f'worldgen/feature/cavern_{flavor}_ice_cream_scoop.json'
        feature = read(path)
        feature['primary'] = {'id': f'jugcraft:cavern_{flavor}_ice_cream'}
        write(path, feature)
    data['features'][9].append('jugcraft:cavern_floating_gummy_ring')
    write(DATA / 'worldgen/placed_feature/cavern_floating_gummy_ring.json', {
        'feature': 'jugcraft:cavern_floating_gummy_ring', 'placement': [
            {'type': 'minecraft:in_square'},
            {'type': 'minecraft:height_range', 'height': {'absolute': -39}},
            {'type': 'minecraft:block_predicate_filter', 'predicate': {
                'type': 'minecraft:matching_blocks', 'offset': [0, -1, 0], 'blocks': ['jugcraft:cavern_purple_soda']}},
            {'type': 'minecraft:biome'}]})
    write(DATA / 'worldgen/biome/cavern_candy_cavity.json', data)
    for size, count in [('big', 5), ('small', 4)]:
        for variant in range(count):
            for color, primary in enumerate(COLORS):
                shades = [primary, COLORS[(color+1) % len(COLORS)], 'white', COLORS[(color+2) % len(COLORS)]]
                recolor_template(f'cavern_{size}_lollipop_top_{variant}', f'cavern/candy/{size}_lollipop_{variant}_{color}',
                                 {f'jugcraft:cavern_rock_candy_{old}': f'jugcraft:cavern_rock_candy_{new}'
                                  for old, new in zip(['black', 'gray', 'white', 'light_gray'], shades)})
    recolor_template('cavern_soda_bottle_side', 'cavern/candy/soda_bottle',
                     {f'minecraft:{color}_concrete': 'jugcraft:cavern_purple_soda' for color in ['purple', 'orange']})
    # Keep definitions/pools for old saved jigsaw pieces and explicit /place;
    # remove only automatic starts for shapes now generated with the terrain.
    for name in ['cake_cave', 'donut_arch', 'soda_bottle']:
        (DATA / f'worldgen/structure_set/cavern_{name}.json').unlink(missing_ok=True)
    write(DATA / 'tags/worldgen/structure_set/cavern_licowitch_tower_generates_far_from.json', {
        'values': ['jugcraft:cavern_gingerbread_town', 'jugcraft:cavern_licowitch_tower']})
    # 26.3 stores pot decorations by side, not the old four-item list.
    tower_path = DATA / 'structure/cavern_licowitch_tower.nbt'
    tower = nouveau_nbt.read(tower_path.read_bytes())
    for block in tower[2]['blocks'][1][1]:
        nbt = block.get('nbt', (10, {}))[1]
        old = nbt.get('sherds')
        if old and old[0] == 9:
            nbt['sherds'] = (10, {side: (10, {'id': (8, item)})
                                  for side, item in zip(['back', 'left', 'right', 'front'], old[1][1])
                                  if item != 'minecraft:brick'})
    tower_path.write_bytes(nouveau_nbt.write(tower))
    for name, template in [('licowitch_tower', 'cavern_licowitch_tower'), ('gingerbread_town', 'cavern/landmarks/gingerbread_town')]:
        write(DATA / f'worldgen/structure/cavern_{name}.json', {
            'type': 'jugcraft:candy_landmark', 'biomes': 'jugcraft:cavern_candy_cavity',
            'step': 'underground_structures', 'spawn_overrides': {}, 'terrain_adaptation': 'none',
            'template': 'jugcraft:' + template})


if __name__ == '__main__':
    main()
