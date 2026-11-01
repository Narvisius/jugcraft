"""Compatibility resources for the owner's supplied Twigs collection."""
from menagerie_content import read
from newest_shapes import variants


def classify(reference, props):
    native = {'Block': 'block', 'SlabBlock': 'slab', 'StairBlock': 'stairs', 'WallBlock': 'wall',
              'RotatedPillarBlock': 'pillar', 'AmethystBlock': 'twig_amethyst',
              'AzaleaFlowersBlock': 'twig_flowers', 'FlowerPotBlock': 'twig_potted',
              'GlowLichenBlock': 'twig_lichen', 'PillarOxidizableBlock': 'twig_copper',
              'SiltPotBlock': 'twig_pot', 'PaperLanternBlock': 'twig_lantern'}
    if reference in native: return native[reference]
    for values in props.values():
        if values == {'true'}: values.add('false')
    if reference in ('TableBlock', 'BambooMatBlock', 'BambooLeavesBlock', 'ColumnBlock', 'CompactedDripstoneBlock', 'FloorItemBlock', 'SeashellBlock'):
        props['waterlogged'] = {'true', 'false'}
    if reference == 'TableBlock': props['update'] = {'true', 'false'}
    return 'twig_decor'


def describe(d, name, reference, props, assets, ns, state):
    d['reference_class'] = reference
    d['wood'] = name.endswith('_table') or name in ('twig', 'bamboo_mat', 'bamboo_thatch', 'bamboo_thatch_slab')
    if reference in ('FloorItemBlock', 'SeashellBlock', 'AzaleaFlowersBlock', 'BambooLeavesBlock'): d['soft'] = True
    if name.endswith('shroomlamp') or reference == 'PaperLanternBlock': d['light'] = 15
    if reference == 'GlowLichenBlock': d['light'] = 10
    if reference == 'LampBlock': d['lamp'] = True
    if reference == 'FlowerPotBlock': d['no_item'] = True
    if d['kind'] in ('twig_decor', 'twig_pot', 'twig_lantern'):
        d['properties'] = {k: sorted(v) for k, v in props.items()}
        d['shapes'] = variants(assets, ns, state, props)


def recipe(value):
    """Only ingredient positions change schema; loot predicates retain their objects."""
    def ingredient(v):
        if isinstance(v, list):
            values = [ingredient(x) for x in v]
            if all(isinstance(x, str) for x in values) and set(values) == {'minecraft:glass', 'minecraft:glass_pane'}:
                return '#jugcraft:botanical_clear_glass'
            return values
        if isinstance(v, dict):
            if set(v) == {'item'}: return ingredient(v['item'])
            if set(v) == {'tag'}: return ingredient('#' + v['tag'])
        if isinstance(v, str):
            return {'minecraft:grass': 'minecraft:short_grass', 'minecraft:chain': 'minecraft:iron_chain',
                '#forge:chests/wooden': '#c:chests/wooden', '#forge:dyes': '#minecraft:dyes',
                '#forge:storage_blocks/quartz': 'minecraft:quartz_block',
                '#forge:gems/prismarine': 'minecraft:prismarine_crystals',
                '#forge:glass/colorless': 'minecraft:glass', '#forge:glass_panes/colorless': 'minecraft:glass_pane',
                '#forge:glass': '#c:glass_blocks'}.get(v, v)
        return v
    value = dict(value)
    for key in ('ingredient', 'ingredients', 'base', 'addition', 'template'):
        if key in value: value[key] = ingredient(value[key])
    if 'key' in value: value['key'] = {k: ingredient(v) for k, v in value['key'].items()}
    if isinstance(value.get('result'), dict) and 'item' in value['result']:
        value['result'] = dict(value['result']); value['result']['id'] = value['result'].pop('item')
    return value


def resources(base, res, writer, copier):
    # Preserve original audio; distinguish event references from sound-file paths.
    sounds = read(res / 'assets/jugcraft/sounds.json')
    for name, definition in read(base / 'assets/twigs/sounds.json').items():
        d = dict(definition); entries = []
        for entry in d['sounds']:
            if isinstance(entry, str): entries.append(entry.replace('twigs:', 'jugcraft:newest/twigworks/'))
            else:
                entry = dict(entry); entry['name'] = entry['name'].replace('twigs:', 'jugcraft:twigworks_' if entry.get('type') == 'event' else 'jugcraft:newest/twigworks/')
                entries.append(entry)
        d['sounds'] = entries; sounds['twigworks_' + name] = d
    writer(res / 'assets/jugcraft/sounds.json', sounds)
    for source in (base / 'assets/twigs/sounds').rglob('*'):
        if source.is_file(): copier(source, res / 'assets/jugcraft/sounds/newest/twigworks' / source.relative_to(base / 'assets/twigs/sounds'))
    from newest_building import map_value
    def mapped(v): return map_value(v, 'twigs', 'twigworks')
    root = base / 'data/twigs'
    for source in (root / 'worldgen/configured_feature').glob('*.json'):
        v = read(source)
        if v['type'] == 'minecraft:ore':
            v = {'type': v['type'], **v['config']}
            for target in v['targets']:
                state = target['state']; name = state['Name']
                props = state.get('Properties', {})
                target['state'] = {'id': name, 'properties': props} if props else name
            v = mapped(v)
        else: v = {'type': 'jugcraft:twigworks_natural', 'kind': source.stem}
        writer(res / 'data/jugcraft/worldgen/feature' / ('twigworks_' + source.name), v)
    for source in (root / 'worldgen/placed_feature').glob('*.json'):
        writer(res / 'data/jugcraft/worldgen/placed_feature' / ('twigworks_' + source.name), mapped(read(source)))
    for source in (root / 'tags/worldgen/biome').rglob('*.json'):
        writer(res / 'data/jugcraft/tags/worldgen/biome' / ('twigworks_' + source.relative_to(root / 'tags/worldgen/biome').as_posix()), mapped(read(source)))
    additions = [mapped(read(p)) for p in sorted((root / 'neoforge/biome_modifier').glob('*.json'))]
    writer(res / 'jugcraft/newest_twigs_biomes.json', additions)
    # The supplied one-piece NBT is complete; replace the unavailable custom
    # structure codec with native jigsaw placement at its authored Nether height.
    for category in ('structure', 'structure_set', 'template_pool'):
        for source in (root / 'worldgen' / category).glob('*.json'):
            value = mapped(read(source))
            if category == 'structure':
                value['type'] = 'minecraft:jigsaw'; value.pop('project_start_to_heightmap', None)
                value['max_distance_from_center'] = 80
            if category == 'template_pool':
                value.pop('name', None)
                for entry in value['elements']: entry['element']['location'] = 'jugcraft:twigworks_bloodstone_obelisk/bloodstone_obelisk'
            writer(res / 'data/jugcraft/worldgen' / category / ('twigworks_' + source.name), value)
    from nouveau_nbt import adapt
    for source in (root / 'structures').rglob('*.nbt'):
        target = res / 'data/jugcraft/structure' / ('twigworks_' + source.relative_to(root / 'structures').as_posix())
        data = adapt(source.read_bytes(), lambda s: s.replace('twigs:', 'jugcraft:twigworks_'))
        target.parent.mkdir(parents=True, exist_ok=True)
        if not target.exists() or target.read_bytes() != data: target.write_bytes(data)
