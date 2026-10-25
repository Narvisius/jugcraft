"""The supplied 21 magic curios; original layered sprites and all nine affinity variants.

No source executables are loaded. Source class references only determine equipment
outcomes; the native game uses Trinkets, shared Focus, spell casting and permissions.
"""
import json
import re
from pathlib import Path

# source ID: native wearable slot, concise player behavior
CURIOS = {
    'mana_ring': ('hand/ring', 'Adds 20 maximum Focus while worn. Does not fill the additional capacity.'),
    'silver_ring': ('hand/ring', 'Adds 5 maximum Focus while worn. Does not fill the additional capacity.'),
    'cooldown_ring': ('hand/ring', 'Reduces spellbook cooldowns by fifteen percent, with diminishing returns above fifty percent.'),
    'cast_time_ring': ('hand/ring', 'Reduces spellbook cast times by fifteen percent, with diminishing returns above fifty percent.'),
    'heavy_chain_necklace': ('chest/necklace', 'Reduces incoming magic damage by fifteen percent.'),
    'emerald_stoneplate_ring': ('hand/ring', 'Defeated creatures drop twenty-five percent more experience.'),
    'fireward_ring': ('hand/ring', 'Prevents fire damage and extinguishes the wearer.'),
    'frostward_ring': ('hand/ring', 'Prevents freezing and freezing damage.'),
    'poisonward_ring': ('hand/ring', 'Prevents poison; existing poison is removed while worn.'),
    'conjurers_talisman': ('chest/necklace', 'Your summoned defenders deal fifteen percent more damage.'),
    'greater_conjurers_talisman': ('chest/necklace', 'Your summoned defenders deal fifteen percent more damage. Natural summon time-outs do not trigger recast cooldowns.'),
    'affinity_ring': ('hand/ring', 'Adds one level to the attuned spell. Use with a supported scroll in the other hand to consume it and attune the ring.'),
    'concentration_amulet': ('chest/necklace', 'Damage cannot interrupt your long spell casts.'),
    'lurker_ring': ('hand/ring', 'While invisible, your next hit deals fifty percent extra damage. Fifteen-second cooldown.'),
    'amethyst_resonance_charm': ('chest/necklace', 'Regenerate shared Focus fifteen percent faster while worn.'),
    'invisibility_ring': ('hand/ring', 'Reveals invisible creatures to you. Does not make you invisible.'),
    'expulsion_ring': ('hand/ring', 'Being hurt pushes permitted enemies within four blocks away. Two-second cooldown; does not break blocks.'),
    'visibility_ring': ('hand/ring', 'Prevents blindness and darkness.'),
    'teleportation_amulet': ('chest/necklace', 'A successful spell teleport grants one Evasion hit for three seconds.'),
    'betrayer_signet': ('hand/ring', 'Ten percent eldritch spell power. Hits against players with extra Focus capacity gain diminishing bonus damage, once per five seconds.'),
    'wicked_bone_ring': ('hand/ring', 'Compatible spell projectiles can ricochet one additional time.'),
}
SCHOOLS = ('evocation', 'fire', 'ice', 'lightning', 'holy', 'ender', 'blood', 'nature', 'eldritch')

def items():
    return ['jimta_spellbooks_' + name for name in CURIOS]

def generate(definition, model, texture, dump, RES, ART, lang):
    root = Path(__file__).resolve().parents[1]
    source_lang = json.loads((ART / 'lang/en_us.json').read_text(encoding='utf-8'))
    affinities = {}
    source_info = json.loads((root / 'tools/jimtafacts_spellbook_sources.json').read_text(encoding='utf-8'))
    review = root.parent / 'jimtafacts-review/decompiled/io/redspace/ironsspellbooks'
    records = {}
    for name, (slot, description) in CURIOS.items():
        ident = 'jimta_spellbooks_' + name
        if name == 'affinity_ring':
            variants = [{'threshold': index, 'model': {'type': 'minecraft:model',
                         'model': model('irons_spellbooks:item/affinity_ring_' + school)}}
                        for index, school in enumerate(SCHOOLS)]
            value = {'model': {'type': 'minecraft:range_dispatch', 'property': 'minecraft:custom_model_data',
                              'index': 0, 'entries': variants, 'fallback': variants[0]['model']}}
        else:
            value = definition('irons_spellbooks:item/' + name, ident)
        dump(RES / f'assets/jugcraft/items/{ident}.json', value)
        dump(RES / f'data/jugcraft/spell_assignments/{ident}.json', {'access': 'NONE', 'access_param': ''})
        lang['item.jugcraft.' + ident] = 'Affinity Ring' if name == 'affinity_ring' else source_lang.get('item.irons_spellbooks.' + name, name.replace('_', ' ').title())
        lang['tooltip.jugcraft.' + ident] = description
        records[name] = {'slot': slot, 'description': description}
        tag = RES / f'data/trinkets/tags/item/{slot}.json'
        existing = json.loads(tag.read_text(encoding='utf-8')) if tag.exists() else {'replace': False, 'values': []}
        existing['values'] = list(dict.fromkeys(existing.get('values', []) + ['jugcraft:' + ident]))
        dump(tag, existing)
    # Source DefaultConfig's explicit maximum, not a guessed universal level multiplier.
    for name, info in source_info.items():
        source = review / info['source_class'].replace('.class', '.java')
        if source.exists():
            match = re.search(r'\.setMaxLevel\((\d+)\)', source.read_text(encoding='utf-8'))
            if match and int(match.group(1)) > 1:
                affinities[name] = Path(info['source_class']).parts[1]
    # Keep the generated catalogue reproducible when the optional decompile-review
    # directory is absent on another contributor's machine.
    catalog = root / 'tools/jimtafacts_spellbook_affinities.json'
    if affinities:
        dump(catalog, dict(sorted(affinities.items())))
    elif catalog.exists():
        affinities = json.loads(catalog.read_text(encoding='utf-8'))
    else:
        raise RuntimeError('Missing checked-in source affinity catalogue')
    dump(RES / 'jugcraft/jimtafacts_spellbooks/curios.json', records)
    dump(RES / 'jugcraft/jimtafacts_spellbooks/affinities.json', dict(sorted(affinities.items())))
