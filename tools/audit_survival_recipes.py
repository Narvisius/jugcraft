"""Inventory recipe routes without mistaking self-drops or conversion loops for sources.

This is a resource-level audit, not a simulation of world generation or gameplay.
Recipe graph reachability assumes ordinary vanilla survival resources and enabled
features. Loot/worldgen references are reported as evidence, not proof of access.
Run with --write to refresh docs/audits/survival-recipes.json and its CSV inventory.
"""
from __future__ import annotations

import argparse
from collections import Counter, defaultdict
import csv
import json
import re
from pathlib import Path
import zipfile

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
DATA = RES / 'data/jugcraft'


def read(path):
    return json.loads(path.read_text(encoding='utf-8'))


def strings(value):
    if isinstance(value, str):
        yield value
    elif isinstance(value, dict):
        for child in value.values():
            yield from strings(child)
    elif isinstance(value, list):
        for child in value:
            yield from strings(child)


def expand(value, tags, seen=frozenset()):
    if isinstance(value, dict):
        value = value.get('ingredient', value.get('item', value.get('id',
                '#' + value['tag'] if 'tag' in value else '')))
    if isinstance(value, list):
        return set().union(*(expand(v, tags, seen) for v in value))
    if not isinstance(value, str) or not value:
        return set()
    if not value.startswith('#'):
        return {value}
    key = value[1:]
    if key in seen:
        return set()
    return set().union(*(expand(v, tags, seen | {key}) for v in tags.get(key, [])))


def outputs(value):
    if isinstance(value, str):
        return [value] if ':' in value else []
    if isinstance(value, list):
        return [out for v in value for out in outputs(v)]
    if isinstance(value, dict):
        return outputs(value.get('id', value.get('item', value.get('result', []))))
    return []


def reachable(edges, seeds):
    """AND across slots, OR inside an ingredient/tag; a cycle never seeds itself."""
    reached = dict(seeds)
    while True:
        changed = False
        for edge in edges:
            if edge['inputs'] and all(any(i in reached for i in slot) for slot in edge['inputs']):
                for out in edge['outputs']:
                    if out not in reached:
                        reached[out] = edge['source']
                        changed = True
        if not changed:
            return reached


def vanilla_jar():
    jars = sorted((ROOT.parent / 'gradle-cache/caches/fabric-loom').glob('*/minecraft-common.jar'))
    preferred = [p for p in jars if p.parent.name == '26.3']
    if not preferred:
        raise SystemExit('Pinned Minecraft 26.3 common JAR required for vanilla item tags.')
    return preferred[0]


def reference_from_jars():
    tags = {}
    vanilla_items = set()
    with zipfile.ZipFile(vanilla_jar()) as jar:
        for name in jar.namelist():
            if name.startswith('data/minecraft/tags/item/') and name.endswith('.json'):
                tags['minecraft:' + name.split('/tags/item/')[1][:-5]] = json.loads(jar.read(name)).get('values', [])
            if name.startswith('assets/minecraft/items/') and name.endswith('.json'):
                vanilla_items.add('minecraft:' + Path(name).stem)
    # Vanilla item definitions can reside in the split client-only JAR.
    client = vanilla_jar().with_name('minecraft-client-only.jar')
    if client.exists():
        with zipfile.ZipFile(client) as jar:
            vanilla_items.update('minecraft:' + Path(n).stem for n in jar.namelist()
                                 if n.startswith('assets/minecraft/items/') and n.endswith('.json'))
    # Fabric's pinned convention module supplies vanilla members of c: tags.
    conventions = sorted((ROOT.parent / 'gradle-cache/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-convention-tags-v2').glob('4.10.3+6b5b47fd5d/*/*.jar'))
    if not conventions:
        raise SystemExit('Pinned Fabric convention-tags JAR required; missing tags must not be guessed.')
    with zipfile.ZipFile(conventions[0]) as jar:
        for name in jar.namelist():
            parts = name.split('/')
            if len(parts) > 4 and parts[0] == 'data' and parts[2:4] == ['tags', 'item'] and name.endswith('.json'):
                d = json.loads(jar.read(name)); key = parts[1] + ':' + '/'.join(parts[4:])[:-5]
                tags[key] = ([] if d.get('replace') else tags.get(key, [])) + d.get('values', [])
    return {"minecraft": "26.3", "fabric_conventions": "4.10.3+6b5b47fd5d", "items": sorted(vanilla_items), "tags": tags}


def build(registry_path=None):
    reference = read(ROOT / 'tools/data/survival-reference-26.3.json')
    if reference['minecraft'] != '26.3' or reference['fabric_conventions'] != '4.10.3+6b5b47fd5d':
        raise ValueError('Acquisition reference must match the pinned platform')
    tags = reference['tags']
    vanilla_items = set(reference['items'])
    recipes = {}
    for base in (RES, ROOT / 'src/main/generated'):
        for p in sorted((base / 'data').rglob('*.json')):
            parts = p.relative_to(base / 'data').parts
            if len(parts) > 3 and parts[1:3] == ('tags', 'item'):
                d = read(p); key = parts[0] + ':' + '/'.join(parts[3:])[:-5]
                tags[key] = ([] if d.get('replace') else tags.get(key, [])) + d.get('values', [])
            elif len(parts) > 2 and parts[1] == 'recipe':
                recipes[parts[0] + ':' + '/'.join(parts[2:])[:-5]] = (p, read(p))

    # Every custom serializer must declare the equipment that actually performs it.
    from recipe_view import LISTS
    import petro
    stations = defaultdict(list)
    for block, (_, kind) in LISTS.items():
        stations['jugcraft:' + kind].append('jugcraft:' + block)
    for block, spec in petro.FLUID_MACHINES.items():
        if spec['recipe_type']:
            stations['jugcraft:' + spec['recipe_type']].append('jugcraft:' + block)
    stations.update({
        'jugcraft:household_construction': ['jugcraft:household_workbench'],
        'jugcraft:cutting': ['jugcraft:cutting_board'],
        'jugcraft:pot_cooking': ['jugcraft:cooking_pot'],
        **{'jugcraft:household_' + kind: ['jugcraft:household_' + n for n in names]
           for kind, names in {'oven': ['light_oven', 'dark_oven'], 'freezer': ['light_freezer', 'dark_freezer'],
                              'pan': ['frying_pan'], 'microwave': ['light_microwave', 'dark_microwave'],
                              'toaster': ['light_toaster', 'dark_toaster']}.items()}})
    household = read(RES / 'jugcraft/menagerie/household.json')['blocks']
    for kind in ('oven', 'freezer', 'pan', 'microwave', 'toaster'):
        stations['jugcraft:household_' + kind] = ['jugcraft:' + k for k, v in household.items() if v['kind'] == kind]
    capabilities = defaultdict(list)
    form_source = (ROOT / 'src/main/java/io/github/jimbozoomer/jugcraft/machine/form/IndustrialForms.java').read_text(encoding='utf-8')
    for match in re.finditer(r'MachineForm\.builder\(Jugcraft\.id\("([^"]+)"\).*?\.build\(\)', form_source, re.S):
        for capability in re.findall(r'\.capability\("([^"]+)"\)', match.group(0)):
            capabilities[capability].append('jugcraft:' + match.group(1))
    edges = []
    unsupported = []
    invalid = []
    types = Counter()

    def edge(source, kind, ingredients, result, extra=()):
        ins = [expand(v, tags) for v in ingredients] + [set(v) for v in extra]
        for original, slot in zip(ingredients, ins):
            if not slot:
                invalid.append({'source': source, 'ingredient': original, 'problem': 'empty ingredient/tag'})
        edges.append({'source': source, 'type': kind, 'inputs': [sorted(s) for s in ins],
                      'outputs': sorted(set(result))})

    for rid, (p, d) in recipes.items():
        typ = d['type']; types[typ] += 1
        ins = []
        if typ == 'minecraft:crafting_shaped':
            ins = [d['key'][s] for s in ''.join(d['pattern']) if s != ' ']
        elif typ == 'minecraft:crafting_shapeless':
            ins = d['ingredients']
        elif typ == 'minecraft:smithing_transform':
            ins = [d[k] for k in ('template', 'base', 'addition') if k in d]
        elif typ == 'minecraft:brewing':
            ins = [d['input'], d['reagent']]
        elif typ == 'minecraft:crafting_dye':
            ins = [d['target'], d['dye']]
        elif any(k in d for k in ('ingredient', 'ingredients', 'items', 'fluids')):
            if 'ingredient' in d: ins.append(d['ingredient'])
            ins += d.get('ingredients', []) + d.get('items', [])
        else:
            unsupported.append({'source': p.relative_to(ROOT).as_posix(), 'type': typ})
            continue
        if d.get('tool'): ins.append(d['tool'])
        out = [v for key in ('result', 'results', 'output', 'byproducts') for v in outputs(d.get(key, []))]
        extra = []
        if d.get('capability'):
            extra.append(capabilities[d['capability']])
            if not extra[-1]: invalid.append({'source': rid, 'ingredient': d['capability'], 'problem': 'no machine capability adapter'})
        elif typ == 'jugcraft:starter_working':
            from starter_workshop import STATIONS
            extra.append(['jugcraft:' + STATIONS[d['station']]])
            if d['station'] == 2:
                extra += [['jugcraft:basic_ceramic_lining'], ['jugcraft:ceramic_crucible']]
        elif typ in stations: extra.append(stations[typ])
        if d.get('form'): extra.append([d['form']])
        for fluid in d.get('fluids', []): extra.append(['fluid:' + fluid['fluid']])
        out += ['fluid:' + f['fluid'] for f in d.get('fluid_results', [])]
        edge(p.relative_to(ROOT).as_posix(), typ, ins, out, extra)

    # The existing native workshop catalogue already records its real stations,
    # source-production bootstrap, and dynamic book/ritual transformations.
    import nouveau_progression
    nouveau = {}
    nouveau_progression.add_recipes(RES / 'data', nouveau)
    for out, routes in nouveau.items():
        for i, ins in enumerate(routes):
            edge('nouveau workshop:' + out + '/' + str(i), 'nouveau_workshop', ins, [out])
    for p in (DATA / 'villager_trade').rglob('*.json'):
        d = read(p)
        edge(p.relative_to(ROOT).as_posix(), 'trade', [d['wants']] + ([d['additional_wants']] if 'additional_wants' in d else []), outputs(d['gives']))

    evidence = defaultdict(list)
    def drops(value):
        if isinstance(value, dict):
            if value.get('type') in ('minecraft:item', 'item'):
                yield value.get('name', '')
            for v in value.values(): yield from drops(v)
        elif isinstance(value, list):
            for v in value: yield from drops(v)
    # A chest table alone is not an acquisition source: a generated container
    # (or native placer) must actually assign it.
    from nouveau_nbt import read as read_nbt
    chest_bindings = set()
    invalid_template_loot_links = []
    referenced_templates = set()
    for p in (DATA / 'worldgen').rglob('*.json'):
        referenced_templates.update(strings(read(p)))
    def nbt_strings(v):
        if isinstance(v, str): yield v
        elif isinstance(v, dict):
            for child in v.values(): yield from nbt_strings(child)
        elif isinstance(v, (tuple, list)):
            for child in v: yield from nbt_strings(child)
    for p in (DATA / 'structure').rglob('*.nbt'):
        key = 'jugcraft:' + p.relative_to(DATA / 'structure').as_posix()[:-4]
        if key in referenced_templates:
            template = read_nbt(p.read_bytes())[2]
            chest_bindings.update(s for s in nbt_strings(template) if s.startswith('jugcraft:chests/'))
            for block in template.get('blocks', (9, (10, [])))[1][1]:
                table = block.get('nbt', (10, {}))[1].get('LootTable', (8, ''))[1]
                if table.startswith('jugcraft:') and not (DATA / ('loot_table/' + table[9:] + '.json')).exists():
                    invalid_template_loot_links.append({'template':key, 'table':table})
    for p in (ROOT / 'src/main/java').rglob('*.java'):
        chest_bindings.update('jugcraft:' + s for s in re.findall(r'"(chests/[^"\s]+)"', p.read_text(encoding='utf-8')))
    unbound_chests = []
    for p in (DATA / 'loot_table').rglob('*.json'):
        if 'blocks' in p.relative_to(DATA / 'loot_table').parts: continue
        key = 'jugcraft:' + p.relative_to(DATA / 'loot_table').as_posix()[:-5]
        if key.startswith('jugcraft:chests/') and key not in chest_bindings:
            unbound_chests.append(key)
            continue
        for item in drops(read(p)):
            if item.startswith('jugcraft:'):
                evidence[item].append({'kind': 'loot', 'source': p.relative_to(ROOT).as_posix()})
    # Block loot is an edge requiring the block, NEVER a free source. This catches
    # isolated block->item->block cycles and lets genuinely gathered ore yield metal.
    for p in (DATA / 'loot_table/blocks').rglob('*.json'):
        out = sorted(set(drops(read(p))))
        if out: edge(p.relative_to(ROOT).as_posix(), 'block_drop', ['jugcraft:' + p.stem], out)
    for p in (DATA / 'worldgen').rglob('*.json'):
        if 'feature' not in p.parts and 'configured_feature' not in p.parts: continue
        for ref in set(strings(read(p))):
            if ref.startswith('jugcraft:'):
                evidence[ref].append({'kind': 'worldgen_reference', 'source': p.relative_to(ROOT).as_posix()})

    rules_path = ROOT / 'tools/data/survival_routes.json'
    rules = read(rules_path) if rules_path.exists() else {'sources': {}, 'routes': [], 'exclusions': {}}
    for row in rules.get('routes', []):
        edge(row['source'], row['kind'], row['inputs'], row['outputs'])
    model_items = {'jugcraft:' + p.stem for p in (RES / 'assets/jugcraft/items').glob('*.json')}
    registry_path = registry_path or ROOT / 'docs/audits/survival-item-registry.json'
    registry_items = set(read(registry_path)) if registry_path.exists() else None
    items = {i for i in registry_items if i.startswith('jugcraft:')} if registry_items is not None else model_items
    from survival_native_routes import extend
    native_exclusions = extend(ROOT, edge, evidence, items)
    forbidden = {'minecraft:air', 'minecraft:barrier', 'minecraft:bedrock', 'minecraft:structure_void',
                 'minecraft:structure_block', 'minecraft:jigsaw', 'minecraft:command_block',
                 'minecraft:chain_command_block', 'minecraft:repeating_command_block', 'minecraft:debug_stick',
                 'minecraft:light', 'minecraft:spawner', 'minecraft:end_portal_frame', 'minecraft:knowledge_book',
                 'minecraft:reinforced_deepslate', 'minecraft:budding_amethyst', 'minecraft:trial_spawner', 'minecraft:vault'}
    vanilla_seeds = {i: 'ordinary vanilla survival (assumed)' for i in vanilla_items
                     if i not in forbidden and not i.endswith('_spawn_egg') and not i.startswith('minecraft:test_')}
    seeds = {**vanilla_seeds, **{k: v['source'] for k, v in rules.get('sources', {}).items()},
             'fluid:minecraft:water': 'world water', 'fluid:minecraft:lava': 'world lava'}
    reached = reachable(edges, seeds)
    # Keep evidence-backed chains distinct from roots with fully explicit inputs.
    # This second pass helps prioritize real missing links without claiming that
    # a configured feature or loot table proves encounter/worldgen reachability.
    evidence_seeds = {item: refs[0]['source'] for item, refs in evidence.items() if refs}
    connected = reachable(edges, {**seeds, **evidence_seeds})
    producers = defaultdict(list)
    for e in edges:
        if e['type'] == 'block_drop': continue
        for out in e['outputs']:
            if e['type'] != 'block_drop': producers[out].append(e)
    rows = []
    for item in sorted(items):
        rs = producers[item]
        excluded = rules.get('exclusions', {}).get(item)
        if not rs: excluded = excluded or native_exclusions.get(item)
        if item.startswith('jugcraft:creative_') or item in {'jugcraft:nouveau_creative_source_jar', 'jugcraft:nouveau_creative_spell_book', 'jugcraft:nouveau_debug'}:
            excluded = 'Creative/debug entry; any internal transformation is not a survival unlock.'
        if 'spawn_egg' in item or item.endswith('_se') and item.startswith('jugcraft:nouveau_'):
            excluded = excluded or 'Creative spawn egg; acquire creatures through world encounters.'
        status = ('intentional_noncraftable' if excluded else 'source' if item in rules.get('sources', {})
                  else 'recipe_chain_reachable' if item in reached else 'recipe_chain_needs_review' if rs
                  else 'loot_only' if any(e['kind'] == 'loot' for e in evidence[item])
                  else 'worldgen_evidence' if evidence[item] else 'no_documented_route')
        if not excluded and item not in reached and item in connected:
            status = 'chain_with_acquisition_evidence' if rs else ('loot_only' if any(e['kind'] == 'loot' for e in evidence[item]) else 'natural_or_interaction_source')
        if excluded and excluded.startswith('Deferred acquisition design:'):
            status = 'deferred_acquisition_design'
        rows.append({'item': item, 'status': status, 'reason': excluded or '', 'route_count': len(rs),
                     'routes': [e['source'] for e in rs], 'evidence': evidence[item],
                     'blocked_by': sorted(set(i for e in rs for s in e['inputs'] if not any(x in connected for x in s) for i in s))})
    # Invalid direct item IDs are distinct from items which exist but lack a route.
    for e in edges:
        for slot in e['inputs']:
            if e['type'] == 'block_drop': continue
            for item in slot:
                if item.startswith('jugcraft:') and item not in items:
                    invalid.append({'source': e['source'], 'ingredient': item, 'problem': 'no item definition; verify registry'})
                elif item.startswith('minecraft:') and item not in vanilla_items:
                    invalid.append({'source': e['source'], 'ingredient': item, 'problem': 'unknown vanilla item'})
    return {'schema': 1, 'scope': 'All registered Jugcraft items from the game-test snapshot.' if registry_items is not None else 'All top-level item definitions; registry snapshot unavailable, presentation-only entries require review.',
            'registry_snapshot': str(registry_path.relative_to(ROOT)) if registry_path.is_relative_to(ROOT) else str(registry_path),
            'unregistered_model_definitions': sorted(model_items - items) if registry_items is not None else [],
            'assumptions': ['Vanilla survival items are available; feature switches enabled.',
                            'Worldgen/loot/interaction evidence seeds only the separately labeled acquisition-evidence graph, never the ordinary-input graph.',
                            'No inference of acquisition from self-drops, item names, or alchemy values.',
                            'Reachability is qualitative; energy, quantities, research and encounter difficulty require playtesting.'],
            'summary': dict(Counter(r['status'] for r in rows)), 'recipe_types': dict(types),
            'unsupported_dynamic_recipes': unsupported, 'invalid_ingredients': invalid,
            'unbound_chest_tables': sorted(unbound_chests),
            'invalid_template_loot_links': invalid_template_loot_links,
            'non_item_source_evidence': {k:v for k,v in sorted(evidence.items()) if k.startswith('fluid:') and v},
            'items': rows, 'edges': edges}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--write', action='store_true')
    parser.add_argument('--refresh-reference', action='store_true', help='Re-export pinned vanilla/Fabric item IDs and tag memberships from local development JARs for portable CI.')
    parser.add_argument('--registry', type=Path, help='Actual registry exported by SurvivalRecipeGameTests; --write preserves this snapshot.')
    args = parser.parse_args()
    if args.refresh_reference:
        (ROOT / 'tools/data/survival-reference-26.3.json').write_text(json.dumps(reference_from_jars(), indent=2) + '\n', encoding='utf-8')
    report = build(args.registry)
    print(json.dumps(report['summary'], indent=2))
    print('Ingredient diagnostics:', len(report['invalid_ingredients']))
    if args.write:
        folder = ROOT / 'docs/audits'; folder.mkdir(parents=True, exist_ok=True)
        if args.registry:
            snapshot = folder / 'survival-item-registry.json'
            snapshot.write_text(json.dumps(read(args.registry), indent=2) + '\n', encoding='utf-8')
            report['registry_snapshot'] = snapshot.relative_to(ROOT).as_posix()
        (folder / 'survival-recipes.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
        with (folder / 'survival-recipes.csv').open('w', encoding='utf-8', newline='') as f:
            w = csv.writer(f); w.writerow(['Item', 'Status', 'Reason', 'Routes', 'Blocked ingredients', 'Evidence'])
            for r in report['items']:
                w.writerow([r['item'], r['status'], r['reason'], ' | '.join(r['routes']),
                            ' | '.join(r['blocked_by']), ' | '.join(e['source'] for e in r['evidence'])])
        consumers = defaultdict(set)
        for edge in report['edges']:
            if edge['type'] == 'block_drop': continue
            for slot in edge['inputs']:
                for item in slot: consumers[item].update(edge['outputs'])
        with (folder / 'survival-base-sources.csv').open('w', encoding='utf-8', newline='') as f:
            w = csv.writer(f)
            w.writerow(['Resource', 'Acquisition status', 'Source or process', 'Source detail', 'Used by count', 'Used by'])
            for row in report['items']:
                if row['status'] in ('intentional_noncraftable', 'deferred_acquisition_design'): continue
                if not row['evidence'] and row['item'] not in consumers: continue
                refs = row['evidence']
                w.writerow([row['item'], row['status'], ' | '.join(sorted(set(e['source'] for e in refs))) or ' | '.join(row['routes']),
                            ' | '.join(sorted(set(e.get('note','') for e in refs if e.get('note')))),
                            len(consumers[row['item']]), ' | '.join(sorted(consumers[row['item']]))])
            for item in sorted(consumers):
                if item.startswith('minecraft:') or item in ('fluid:minecraft:water','fluid:minecraft:lava'):
                    w.writerow([item, 'vanilla survival prerequisite', 'Vanilla survival acquisition (assumed, not audited)', '', len(consumers[item]), ' | '.join(sorted(consumers[item]))])
                elif item.startswith('fluid:'):
                    refs = report['non_item_source_evidence'].get(item, [])
                    routes = [e['source'] for e in report['edges'] if item in e['outputs']]
                    w.writerow([item, 'fluid process/source', ' | '.join(sorted(set(routes + [e['source'] for e in refs]))),
                                ' | '.join(sorted(set(e.get('note','') for e in refs if e.get('note')))),
                                len(consumers[item]), ' | '.join(sorted(consumers[item]))])


if __name__ == '__main__': main()
