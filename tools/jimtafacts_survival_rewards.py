"""Native survival routes for the nine formerly preview-only spellbook items.

Append bounded rewards to the supplied loot, preserving original entries.
"""
PREFIX = 'jugcraft:jimta_spellbooks_'
REWARDS = {
    'entities/archevoker.json': [('archevoker_logbook_untranslated', 1.0)],
    'entities/dead_king_per_player.json': [('legendary_spell_book', .25), ('tarnished_helmet', .25)],
    'chests/evoker_fort.json': [('furled_map_ancient', .35)],
    'chests/catacombs/hidden_trough_treasure.json': [('lurker_ring', .20)],
    'chests/ice_spider_den/dungeon.json': [('music_disc_whispers_of_ice', .10)],
    'chests/citadel/citadel_tomes.json': [('unchained_book', .35)],
}

def add_rewards(path, table):
    if path == 'entities/dead_king_per_player_ominous.json':
        path = 'entities/dead_king_per_player.json'
    for item, chance in REWARDS.get(path, []):
        conditions = []
        if path.startswith('entities/'):
            conditions.append({'type': 'minecraft:killed_by_player'})
        if chance < 1:
            conditions.append({'type': 'minecraft:random_chance', 'chance': chance})
        pool = {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': PREFIX + item}]}
        if conditions:
            pool['condition'] = conditions[0] if len(conditions) == 1 else {'type': 'minecraft:all_of', 'terms': conditions}
        table.setdefault('pools', []).append(pool)

def generate_ore(dump, res):
    # New chunks only. Rare buried Nether ore, two attempts/chunk, at most two blocks/vein.
    dump(res/'data/jugcraft/worldgen/feature/jimtafacts/spellbooks/ore_arcane_debris.json', {
        'type': 'minecraft:scattered_ore', 'size': 2, 'discard_chance_on_air_exposure': 1.0,
        'targets': [{'target': {'predicate_type': 'minecraft:tag_match', 'tag': 'minecraft:base_stone_nether'},
                     'state': {'id': PREFIX + 'arcane_debris'}}]})
    dump(res/'data/jugcraft/worldgen/placed_feature/jimtafacts/spellbooks/ore_arcane_debris.json', {
        'feature': 'jugcraft:jimtafacts/spellbooks/ore_arcane_debris', 'placement': [
            {'type': 'minecraft:count', 'count': 2}, {'type': 'minecraft:in_square'},
            {'type': 'minecraft:height_range', 'height': {'type': 'minecraft:uniform',
                'min_inclusive': {'absolute': 8}, 'max_inclusive': {'absolute': 32}}},
            {'type': 'minecraft:biome'}]})

def debris_loot():
    return {'type': 'minecraft:block', 'pools': [{'rolls': 1,
        'condition': {'type': 'minecraft:survives_explosion'},
        'entries': [{'type': 'minecraft:alternatives', 'children': [
            {'type': 'minecraft:item', 'name': PREFIX + 'arcane_debris',
             'condition': {'type': 'minecraft:match_tool', 'predicate': {'predicates': {
                 'minecraft:enchantments': [{'enchantments': 'minecraft:silk_touch', 'levels': {'min': 1}}]}}}},
            {'type': 'minecraft:item', 'name': PREFIX + 'pyrium_ingot'}]}]}]}
