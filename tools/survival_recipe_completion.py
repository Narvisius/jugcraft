"""Owner-requested survival recipe repairs. Run after the owner resource adapters.

Only explicit reviewed outputs receive recipes. No fallback crafting recipe is
invented from a display name; boss drops, treasure and technical items are excluded.
Original supplied files are never modified. OpenAI Codex (GPT-6), 19 October 2026.
"""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
DATA = RES / 'data/jugcraft'
CONDITION = [{'condition': 'jugcraft:feature_enabled', 'feature': 'agriculture'}]


def j(name):
    return name if ':' in name or name.startswith('#') else 'jugcraft:' + name


def definitions():
    recipes = {}

    def add(name, data):
        assert name not in recipes, name
        recipes[name] = {'fabric:load_conditions': CONDITION, **data}

    def shaped(out, pattern, key, count=1):
        add(out, {'type': 'minecraft:crafting_shaped', 'pattern': pattern,
                  'key': {k: j(v) for k, v in key.items()}, 'result': {'id': j(out), 'count': count}})

    def shapeless(out, ins, count=1, name=None):
        add(name or out, {'type': 'minecraft:crafting_shapeless', 'ingredients': [j(i) for i in ins],
                         'result': {'id': j(out), 'count': count}})

    def construction(out, ins):
        add(out, {'type': 'jugcraft:household_construction',
                  'ingredients': [{'ingredient': j(i), 'count': n} for i, n in ins],
                  'result': {'id': j(out)}})

    def cook(out, ingredient, kinds=('smelting', 'smoking', 'campfire_cooking')):
        for kind in kinds:
            add(out + '_' + kind, {'type': 'minecraft:' + kind, 'category': 'blocks' if out.startswith('landscape_') else 'food',
                'ingredient': j(ingredient), 'result': {'id': j(out)}, 'experience': .35,
                'cookingtime': {'smelting': 200, 'smoking': 100, 'campfire_cooking': 600}[kind]})

    def cut(name, ingredient, results):
        add(name, {'type': 'jugcraft:cutting', 'ingredient': j(ingredient), 'tool': '#jugcraft:knives',
                  'results': [{'id': j(item), 'count': count} for item, count in results]})

    # Same material scale as the supplied fridges. Both finishes are appliances,
    # not upper-half rendering blocks; snow/ice still fuels their actual operation.
    for color in ('light', 'dark'):
        construction('household_' + color + '_freezer', [('minecraft:iron_ingot', 9),
            ('minecraft:copper_ingot', 3), ('minecraft:redstone', 4), ('minecraft:packed_ice', 1)] +
            ([('minecraft:black_dye', 1)] if color == 'dark' else []))
    shaped('household_package', ['PPP', 'PSP', 'PPP'], {'P': 'minecraft:paper', 'S': 'minecraft:string'})
    construction('household_pale_oak_hedge', [('minecraft:pale_oak_leaves', 4), ('minecraft:stick', 2)])
    shaped('fauna_bug_habitat_empty', ['PPP', 'G G', 'PPP'], {'P': '#minecraft:wooden_slabs', 'G': 'minecraft:glass_pane'})
    cook('fauna_turkey_leg_cooked', 'fauna_turkey_leg_raw')
    cook('fauna_venison_cooked', 'fauna_venison_raw')
    cut('wildlife_cooked_fish_fillet', 'minecraft:cooked_cod', [('wildlife_cooked_fish_fillet', 1)])
    cut('ecology_split_coconut', 'ecology_coconut', [('ecology_coconut_slice', 2), ('ecology_coconut_husk', 1)])

    # End-themed carpentry and the existing boat-control utilities.
    shapeless('beast_purpur_planks', ['minecraft:purpur_block'], 4)
    shaped('beast_purpur_planks_slab', ['PPP'], {'P': 'beast_purpur_planks'}, 6)
    shaped('beast_purpur_planks_stairs', ['P  ', 'PP ', 'PPP'], {'P': 'beast_purpur_planks'}, 4)
    shaped('beast_purpur_planks_wall', ['P P', 'PSP', 'PSP'], {'P': 'beast_purpur_planks', 'S': 'minecraft:stick'}, 6)
    shaped('beast_end_pirate_door', ['PP', 'PP', 'PP'], {'P': 'beast_purpur_planks'}, 3)
    shaped('beast_end_pirate_trapdoor', ['PPP', 'PPP'], {'P': 'beast_purpur_planks'}, 2)
    shaped('beast_end_pirate_anchor', [' I ', ' I ', 'IBI'], {'I': 'minecraft:iron_ingot', 'B': 'minecraft:iron_block'})
    shaped('beast_end_pirate_anchor_winch', ['PIP', 'ICI', 'PIP'],
           {'P': 'beast_purpur_planks', 'I': 'minecraft:iron_ingot', 'C': 'minecraft:iron_chain'})
    shaped('beast_end_pirate_ship_wheel', [' S ', 'SIS', ' S '],
           {'S': 'minecraft:stick', 'I': 'minecraft:iron_ingot'})
    shaped('beast_end_pirate_flag', ['SW', 'SW', 'S '], {'S': 'minecraft:stick', 'W': 'minecraft:purple_wool'})
    # Native komodo harvesting produces a bottle while the supplied gear recipes
    # consume the solid reagent. Dry it on a board; the bottle is a second output.
    cut('beast_dry_komodo_spit', 'beast_komodo_spit_bottle', [('beast_komodo_spit', 1), ('minecraft:glass_bottle', 1)])
    cook('beast_crystalized_banana_slug_mucus', 'beast_banana_slug_slime', ('smelting',))
    shapeless('beast_warped_mixture', ['minecraft:warped_fungus', 'minecraft:crimson_fungus', 'minecraft:bone_meal'])
    # Preserve bottles explicitly through the cutting serializer rather than
    # baking a filled-bucket recipe that loses or duplicates its container.
    cut('beast_banana_peel', 'beast_banana', [('beast_banana_peel', 1)])

    shaped('cavern_spelunkery_table', ['FBF', 'PPP', 'P P'],
           {'F': 'minecraft:flint', 'B': 'minecraft:book', 'P': '#minecraft:planks'})
    shapeless('cavern_cave_codex', ['cavern_cave_book', 'minecraft:paper', 'minecraft:ink_sac'])
    # The native submarine is powered by existing engineering parts, with diving
    # glass and a propeller motor. No new machine/material framework is introduced.
    shaped('cavern_submarine', ['PGP', 'CMC', 'PPP'],
           {'P': 'steel_plate', 'G': 'minecraft:glass', 'C': 'minecraft:copper_block', 'M': 'electric_motor'})
    shaped('cavern_nuclear_furnace', ['III', 'IFI', 'CCC'],
           {'I': 'minecraft:iron_ingot', 'F': 'minecraft:blast_furnace', 'C': 'minecraft:copper_ingot'})
    # This single-block legacy furnace remains a normal blast furnace; the
    # separate uranium multiblock is not bypassed by this decorative alternative.
    for part, base, amount in [('helmet', 'helmet', 5), ('chestplate', 'chestplate', 8),
                               ('leggings', 'leggings', 7), ('boots', 'boots', 4)]:
        shapeless('cavern_gingerbread_' + part,
                  ['minecraft:leather_' + base] + ['cavern_gingerbread_crumbs'] * amount)
    shaped('cavern_dough_block', ['DDD', 'DDD', 'DDD'], {'D': 'pastry_dough'})
    # No reverse recipe: this block is a decorative supply, not an infinite food loop.
    # One whole cake yields seven slices; assembling all seven preserves four layers.
    shapeless('cavern_cake_layer', ['cake_slice'] * 7, 4)
    shaped('cavern_rusty_rebar', ['I I', 'III', 'I I'], {'I': 'cavern_rusty_scrap_metal'}, 8)
    shaped('cavern_rusty_scaffolding', ['SSS', 'R R', 'R R'],
           {'S': 'minecraft:string', 'R': 'cavern_rusty_rebar'}, 6)
    cook('landscape_cracked_scoria_bricks', 'landscape_scoria_bricks', ('smelting',))
    add('landscape_scoria_from_basalt', {'type': 'minecraft:stonecutting', 'ingredient': 'minecraft:basalt',
                                       'result': {'id': 'jugcraft:landscape_scoria', 'count': 1}})
    for name, ingredient in [('landscape_soul_ash_campfire', 'minecraft:campfire'),
                              ('landscape_soul_ash_soul_campfire', 'minecraft:soul_campfire')]:
        shapeless(name, [ingredient, 'landscape_ashed_soul_sand'])
    from survival_source_completion import recipes as complete
    complete(add, shaped, shapeless, construction, cook, cut)
    return recipes


def main():
    recipes = definitions()
    items = {'jugcraft:' + p.stem for p in (RES / 'assets/jugcraft/items').glob('*.json')}
    # Catch typos before writing anything. Vanilla and tag IDs are checked by the
    # audit and, ultimately, Minecraft's real data reload and ingredient tests.
    def refs(v):
        if isinstance(v, str): return [v] if v.startswith('jugcraft:') else []
        if isinstance(v, list): return [i for x in v for i in refs(x)]
        if isinstance(v, dict): return [i for k, x in v.items() if k not in ('type', 'fabric:load_conditions') for i in refs(x)]
        return []
    missing = sorted({i for d in recipes.values() for i in refs(d) if i not in items})
    if missing: raise ValueError('Unregistered recipe identifiers: ' + ', '.join(missing))
    folder = DATA / 'recipe/survival_completion'; folder.mkdir(parents=True, exist_ok=True)
    for name, data in recipes.items():
        (folder / (name + '.json')).write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')
        if data['type'].startswith('minecraft:'):
            refs = list(data.get('key', {}).values()) or data.get('ingredients', []) or [data['ingredient']]
            criteria = {'ingredient_' + str(i): {'trigger': 'minecraft:inventory_changed',
                        'conditions': {'items': [{'items': ref}]}} for i, ref in enumerate(dict.fromkeys(refs))}
            target = DATA / ('advancement/survival_recipes/' + name + '.json'); target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(json.dumps({'criteria': criteria, 'requirements': [list(criteria)],
                              'rewards': {'recipes': ['jugcraft:survival_completion/' + name]}}, indent=2) + '\n', encoding='utf-8')
    fixture = ROOT / 'src/gametest/resources/survival-recipe-cases.json'
    fixture.write_text(json.dumps(list(recipes), indent=2) + '\n', encoding='utf-8')
    # Ordinary animal reagents are drops, never crafting substitutes for hunting.
    # These six missing links fed multiple pre-existing equipment/ammunition recipes.
    for species, material, chance in [('hammerhead_shark', 'shark_tooth', .5),
            ('frilled_shark', 'serrated_shark_tooth', .5), ('cachalot_whale', 'cachalot_whale_tooth', .25),
            ('giant_squid', 'lost_tentacle', .5), ('alligator_snapping_turtle', 'spiked_scute', .5),
            ('crimson_mosquito', 'blood_sac', .5)]:
        path = DATA / ('loot_table/entities/beast_' + species + '.json')
        d = json.loads(path.read_text(encoding='utf-8')) if path.exists() else {'type': 'minecraft:entity', 'pools': []}
        pool_name = 'jugcraft_survival_' + material
        d['pools'] = [p for p in d.get('pools', []) if p.get('name') != pool_name]
        d['pools'].append({'name': pool_name, 'rolls': 1,
                          'condition': {'type': 'minecraft:random_chance', 'chance': chance},
                          'entries': [{'type': 'minecraft:item', 'name': j('beast_' + material)}]})
        path.write_text(json.dumps(d, indent=2) + '\n', encoding='utf-8')
    # Minecraft 26.3 no longer supplies minecraft:music_discs. Fabric's pinned
    # common tag is the real disc collection the Capsid conversion should use.
    path = RES / 'jugcraft/menagerie/beast.json'
    d = json.loads(path.read_text(encoding='utf-8'))
    for recipe in d['capsid']:
        recipe['ingredients'] = ['#c:music_discs' if i == '#minecraft:music_discs' else i for i in recipe['ingredients']]
    path.write_text(json.dumps(d, indent=2) + '\n', encoding='utf-8')
    from survival_source_completion import sources, soda_lake, structure_chests
    source_manifest = sources(DATA)
    soda_lake(DATA)
    chest_manifest=structure_chests(DATA)
    (ROOT / 'src/gametest/resources/survival-chest-cases.json').write_text(json.dumps(chest_manifest,indent=2)+'\n',encoding='utf-8')
    (ROOT / 'src/gametest/resources/survival-source-cases.json').write_text(json.dumps(source_manifest,indent=2)+'\n',encoding='utf-8')
    print('Wrote', len(recipes), 'explicit survival recipes and',len(source_manifest),'source repairs.')


if __name__ == '__main__': main()
