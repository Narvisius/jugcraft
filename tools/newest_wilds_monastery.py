"""Convert the supplied monastery data without executing its source programs."""
import json
import re
import nouveau_nbt
from newest_building import SOURCE, RES, write, map_value
from menagerie_data import loot

SOURCE_DATA = SOURCE / 'jugziesmobs/data/mowziesmobs'
COLORS = ['white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black']
BANNERS = {'mc':'circle','bo':'border','ss':'small_stripes','bri':'bricks','cbo':'curly_border','flo':'flower'}


def json_tag(value):
    if isinstance(value, bool):
        return 1, int(value)
    if isinstance(value, str):
        return 8, value
    if isinstance(value, int):
        return 3, value
    if isinstance(value, float):
        return 6, value
    if isinstance(value, dict):
        return 10, {k: json_tag(v) for k, v in value.items() if v is not None}
    if isinstance(value, list):
        tags = [json_tag(v) for v in value]
        if len({t[0] for t in tags}) > 1:
            tags = [(10, {'text': t}) if t[0] == 8 else t for t in tags]
        return 9, (tags[0][0] if tags else 10, [t[1] for t in tags])
    raise ValueError(f'Unsupported authored text value: {value!r}')


def text_tag(tag):
    if tag[0] != 8:
        return tag
    try:
        return json_tag(json.loads(tag[1]))
    except json.JSONDecodeError:
        return 8, tag[1]


def source_json(path):
    # These authored pool files use JSON comments and trailing commas.
    text = re.sub(r'/\*.*?\*/|//[^\n]*', '', path.read_text(encoding='utf-8'), flags=re.S)
    return json.loads(re.sub(r',\s*([}\]])', r'\1', text))


def remap(value):
    # Connector names and pools are graph keys, not registered block identifiers.
    if value.startswith('mowziesmobs:monastery/'):
        return value
    if value == 'mowziesmobs:chests/monastery_chest':
        return 'jugcraft:chests/wilds_monastery'
    if value == 'mowziesmobs:gong_entity':
        return 'jugcraft:wilds_gong'
    return map_value(value, 'mowziesmobs', 'wilds', False)


def modernize(tag):
    kind, value = tag
    if kind == 9:
        return kind, (value[0], [modernize((value[0], item))[1] for item in value[1]])
    if kind != 10:
        return tag
    out = {key: modernize(child) for key, child in value.items() if key not in ('ForgeCaps', 'ForgeData', 'CanUpdate')}
    if 'Patterns' in out:
        patterns = []
        for layer in out.pop('Patterns')[1][1]:
            code = layer['Pattern'][1]
            if code not in BANNERS:
                raise ValueError(f'Unmapped authored banner pattern {code}')
            patterns.append({'pattern': (8, 'minecraft:' + BANNERS[code]), 'color': (8, COLORS[layer['Color'][1]])})
        out['patterns'] = (9, (10, patterns))
    if 'Text1' in out:
        lines = []
        for index in range(1, 5):
            line = text_tag(out.pop('Text' + str(index), (8, '{"text":""}')))
            lines.append({'text': line} if line[0] == 8 else line[1])
        out['front_text'] = (10, {'messages': (9, (10, lines)), 'color': out.pop('Color', (8, 'black')), 'has_glowing_text': out.pop('GlowingText', (1, 0))})
    if 'CustomName' in out:
        out['CustomName'] = text_tag(out['CustomName'])
    if all(key in out for key in ('TileX', 'TileY', 'TileZ')):
        # Native StructureTemplate transforms block_pos; leaving old absolute Tile*
        # coordinates detached the original frames from their authored walls.
        out['block_pos'] = (11, [out.pop('Tile' + axis)[1] for axis in 'XYZ'])
    if 'id' in out and 'Count' in out:
        out['count'] = (3, out.pop('Count')[1])
        old = out.pop('tag', (10, {}))[1]
        components = out.setdefault('components', (10, {}))[1]
        if 'Damage' in old:
            components['minecraft:damage'] = old.pop('Damage')
        if 'Enchantments' in old:
            components['minecraft:enchantments'] = (10, {e['id'][1]: (3, e['lvl'][1]) for e in old.pop('Enchantments')[1][1]})
        if 'StoredEnchantments' in old:
            components['minecraft:stored_enchantments'] = (10, {e['id'][1]: (3, e['lvl'][1]) for e in old.pop('StoredEnchantments')[1][1]})
        display = old.pop('display', (10, {}))[1]
        if 'Name' in display:
            components['minecraft:custom_name'] = text_tag(display.pop('Name'))
        if 'Lore' in display:
            lines = [text_tag((8, line)) for line in display.pop('Lore')[1][1]]
            components['minecraft:lore'] = (9, (10, [{'text': line} if line[0] == 8 else line[1] for line in lines]))
        if display:
            old['display'] = (10, display)
        if old:
            components['minecraft:custom_data'] = (10, old)
        if not components:
            out.pop('components')
    return kind, out


def main():
    pools = {v['name']: v for p in sorted((SOURCE_DATA / 'worldgen/template_pool/monastery').rglob('*.json')) if (v := source_json(p))}
    write(RES / 'jugcraft/newest_wilds_monastery.json', {'pools': pools})
    for source in sorted((SOURCE_DATA / 'structures/monastery').rglob('*.nbt')):
        root = nouveau_nbt.read(nouveau_nbt.adapt(source.read_bytes(), remap))
        root = root[0], root[1], modernize((root[0], root[2]))[1]
        # Source chest processor supplies the loot at placement; carry that into native NBT.
        palette = root[2].get('palette', (9, (10, [])))[1][1]
        for state in palette:
            if state.get('id', (8, ''))[1] in ('jugcraft:wilds_gong', 'jugcraft:wilds_gong_part'):
                props = state.setdefault('properties', (10, {}))[1]
                part = props.pop('gong_part', (8, 'center'))[1]
                props['side'] = (8, str({'side_left': 0, 'side_right': 2}.get(part, 1)))
                props['height'] = props.pop('y_offset', (8, '0'))
        for block in root[2].get('blocks', (9, (10, [])))[1][1]:
            state = palette[block['state'][1]]
            if state.get('id', (8, ''))[1] in ('minecraft:chest', 'minecraft:trapped_chest'):
                nbt = block.setdefault('nbt', (10, {}))[1]
                nbt['LootTable'] = (8, 'jugcraft:chests/wilds_monastery')
        for entity in root[2].get('entities', (9, (10, [])))[1][1]:
            data = entity.get('nbt', (10, {}))[1]
            data.pop('UUID', None)
            data.pop('Attributes', None)
            # 26.3 ItemFrame and ItemEntity still use uppercase Item; only the
            # nested stack's Count/tag fields are migrated to count/components.
            equipment = {}
            for legacy, slots in (('ArmorItems', ('feet', 'legs', 'chest', 'head')), ('HandItems', ('mainhand', 'offhand'))):
                for slot, stack in zip(slots, data.pop(legacy, (9, (10, [])))[1][1]):
                    if stack.get('id', (8, 'minecraft:air'))[1] != 'minecraft:air':
                        equipment[slot] = (10, stack)
            if equipment:
                data['equipment'] = (10, equipment)
        destination = RES / 'data/jugcraft/structure/wilds/monastery' / source.relative_to(SOURCE_DATA / 'structures/monastery')
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_bytes(nouveau_nbt.write(root))
    write(RES / 'data/jugcraft/loot_table/chests/wilds_monastery.json', loot(map_value(source_json(SOURCE_DATA / 'loot_tables/chests/monastery_chest.json'), 'mowziesmobs', 'wilds', False)))
    write(RES / 'data/jugcraft/worldgen/structure/wilds_monastery.json', {
        'type': 'jugcraft:wilds_monastery', 'biomes': ['minecraft:jagged_peaks', 'minecraft:frozen_peaks', 'minecraft:stony_peaks'],
        'step': 'surface_structures', 'terrain_adaptation': 'none', 'spawn_overrides': {}})
    write(RES / 'data/jugcraft/worldgen/structure_set/wilds_monastery.json', {
        'structures': [{'structure': 'jugcraft:wilds_monastery', 'weight': 1}],
        'placement': source_json(SOURCE_DATA / 'worldgen/structure_set/monasteries.json')['placement']})


if __name__ == '__main__':
    main()
