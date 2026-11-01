"""Adapt owner enchantment data; source programs are never executed."""
import json
from pathlib import Path
from menagerie_data import loot

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'art/owner-library/originals/Edited-2026-10-29/enchantments'
RES = ROOT / 'src/main/resources'

def read(path): return json.loads(path.read_text(encoding='utf-8-sig'))
def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
def remap(value):
    if isinstance(value, str): return value.replace('enchantmentsreforged:', 'jugcraft:reforged/').replace('enchantment.enchantmentsreforged.', 'enchantment.jugcraft.reforged.')
    if isinstance(value, list): return [remap(v) for v in value]
    if isinstance(value, dict): return {remap(k): remap(v) for k, v in value.items()}
    return value

def main():
    source = SOURCE / 'data/enchantmentsreforged'
    for f in source.rglob('*.json'):
        rel = f.relative_to(source); data = remap(read(f))
        parts = rel.parts
        target = RES / 'data/jugcraft' / parts[0]
        if parts[0] == 'tags': target = target / parts[1] / 'reforged' / Path(*parts[2:])
        else: target = target / 'reforged' / Path(*parts[1:])
        if parts[0] == 'loot_table': data = loot(data)
        write(target, data)
    for f in (SOURCE / 'data/minecraft/tags/enchantment').glob('*.json'):
        target = RES / 'data/minecraft/tags/enchantment' / f.name
        data = read(target) if target.exists() else {'replace': False, 'values': []}
        for entry in remap(read(f))['values']:
            if entry not in data['values']: data['values'].append(entry)
        write(target, data)
    # Native attributes replace loader-specific tick mutations for unconditional effects.
    for name, attribute, amount, step in [('steadfast','knockback_resistance',.05,.05)]:
        path = RES / 'data/jugcraft/enchantment/reforged' / (name + '.json')
        data = read(path)
        data['effects'] = {'minecraft:attributes': [{'id': 'jugcraft:reforged/' + name, 'attribute': 'minecraft:' + attribute,
            'amount': {'type': 'minecraft:linear', 'base': amount, 'per_level_above_first': step}, 'operation': 'add_value'}]}
        write(path, data)
    for name, base, step in [('firm_line', .08, .04), ('guarding', .12, .12)]:
        path = RES / 'data/jugcraft/enchantment/reforged' / (name + '.json'); data = read(path)
        data['effects'] = {'minecraft:item_damage': [{'effect': {'type': 'minecraft:remove_binomial',
            'chance': {'type': 'minecraft:linear', 'base': base, 'per_level_above_first': step}}}]}
        write(path, data)
    for f in (SOURCE / 'assets/enchantmentsreforged/lang').glob('*.json'):
        path = RES / 'assets/jugcraft/lang' / f.name; data = read(path) if path.exists() else {}
        data.update(remap(read(f))); write(path, data)
    # Preserve the supplied vanilla enchantment reference text for the integrated UI.
    write(RES / 'jugcraft/enchantment_reference.json', read(SOURCE / 'data/lucidenchants/enchants.json'))
    write(ROOT / 'art/owner-library/catalog/edited-2026-10-29/enchantment-integration.json', {
        'source': str(SOURCE.relative_to(ROOT)), 'definitions': 49,
        'adaptations': ['Jugcraft namespaced dynamic registry', '26.3 loot schema', 'native attributes and durability effects', 'Fabric runtime effects'],
        'validation': 'Awaiting the owner-requested combined final checks; generated data is not runtime verification.'})
    print('Generated owner enchantment resources and survival tags.')

if __name__ == '__main__': main()
