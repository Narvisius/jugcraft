"""26.3 runtime data adapter; preserved owner files are never edited."""
from menagerie_content import NATURE, DATA, read, write, remap


def loot(value):
    if isinstance(value, list):
        return [loot(x) for x in value]
    if not isinstance(value, dict):
        return value
    out = {k: loot(v) for k, v in value.items()}
    if 'function' in out:
        out['type'] = out.pop('function')
    if isinstance(out.get('condition'), str) and 'type' not in out:
        out['type'] = out.pop('condition')
    if out.get('type') in ('looting_enchant','block_state_property','random_chance_with_looting','tag'):
        out['type']='minecraft:'+out['type']
    if 'functions' in out:
        out['modifier'] = out.pop('functions')
    if 'conditions' in out:
        conditions = out.pop('conditions')
        if conditions:
            out['condition'] = conditions[0] if len(conditions) == 1 else {'type': 'minecraft:all_of', 'terms': conditions}
    if out.get('type') == 'minecraft:block_state_property':
        out['type'] = 'minecraft:match_block'
        out['blocks'] = out.pop('block')
        if 'properties' in out:
            out['state'] = out.pop('properties')
    if out.get('type') == 'minecraft:looting_enchant':
        out['type']='minecraft:enchanted_count_increase';out['enchantment']='minecraft:looting'
    if out.get('type') == 'minecraft:tag' and 'name' in out:
        out['items']='#'+out.pop('name').removeprefix('#')
    if out.get('type') == 'minecraft:random_chance_with_looting':
        base=out.pop('chance');extra=out.pop('looting_multiplier')
        out.update(type='minecraft:random_chance_with_enchanted_bonus',enchantment='minecraft:looting',unenchanted_chance=base,enchanted_chance={'type':'minecraft:linear','base':base+extra,'per_level_above_first':extra})
    return out


def write_loot():
    for source in (NATURE/'naturalist/loot_table').rglob('*.json'):
        relative = source.relative_to(NATURE/'naturalist/loot_table')
        value = loot(remap(read(source)))
        if relative.parent.name == 'blocks' and source.stem.endswith('_slab'):
            value['pools'][0]['entries'][0]['modifier'] = [{
                'type': 'minecraft:set_count', 'count': 2,
                'condition': {'type': 'minecraft:match_block', 'blocks': 'jugcraft:wildlife_'+source.stem, 'state': {'type': 'double'}}}]
        write(DATA/'loot_table'/relative.parent/('wildlife_'+source.name), value)


if __name__ == '__main__':
    write_loot()
