"""Port the owner's supplied Handcrafted resources; preserve every source model and skin."""
import collections
import copy
import json
import shutil
from functools import lru_cache
from pathlib import Path
from native_shapes import model_boxes, multipart_shapes
from model_writer import finish_elements
from menagerie_data import loot

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'art/owner-library/originals/Oct22/Planes And Furniture'
ASSETS = SOURCE / 'assets/handcrafted'
RES = ROOT / 'src/main/resources'
TARGET = RES / 'assets/jugcraft'
DATA = RES / 'data/jugcraft'

def read(p): return json.loads(p.read_text(encoding='utf-8'))
def write(p, v):
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(v, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')

@lru_cache(None)
def finished_geometry(raw):
    elements = json.loads(raw)
    for e in elements:
        low, high = e['from'][:], e['to'][:]
        e['from'] = [min(a, b) for a, b in zip(low, high)]
        e['to'] = [max(a, b) for a, b in zip(low, high)]
    finish_elements(elements)
    return json.dumps(elements)

def runtime_model(d):
    d = adapt(d, True)
    d['textures'] = {k: ('jugcraft:crafted/vanilla_bow' if v == 'minecraft:item/bow' else v) for k,v in d.get('textures', {}).items()}
    if 'frame' in d['textures']: d['textures'].setdefault('particle', d['textures']['frame'])
    if d.get('elements'):
        d['elements'] = json.loads(finished_geometry(json.dumps(d['elements'], sort_keys=True)))
    return d

def finish_trophies(catalog):
    # Tall trophies were drawn by a custom source renderer. Draw the full supplied mesh once from
    # its upper block, with coordinates shifted into vanilla's supported range, rather than twice.
    for name, entry in catalog['blocks'].items():
        if 'upper' not in entry['properties'].get('half', []): continue
        source = ASSETS / 'models/block/trophy' / (entry['source'] + '.json')
        original = runtime_model(read(source))
        upper = copy.deepcopy(original)
        lower = copy.deepcopy(original)
        # Some tilted feet/weapons use a negative local Y even though their rotated shape sits
        # above the plinth. Keep those complete elements on the lower block, without clipping UVs.
        lower['elements']=[e for e in lower['elements'] if min(e['from'][1],e['to'][1]) < 0]
        upper['elements']=[e for e in upper['elements'] if min(e['from'][1],e['to'][1]) >= 0]
        for e in upper['elements']:
            e['from'][1] -= 16; e['to'][1] -= 16
            if 'rotation' in e: e['rotation']['origin'][1] -= 16
        write(TARGET / f'models/crafted/block/trophy/{entry["source"]}_upper.json', upper)
        write(TARGET / f'models/crafted/block/trophy/{entry["source"]}_lower.json', lower)
        # The inventory icon is a scaled version of the same mesh; no cut-off hat or missing body.
        if max(v for e in original['elements'] for v in e['from']+e['to']) > 32:
            for e in original['elements']:
                for key in ('from','to'): e[key]=[v*.72 for v in e[key]]
                if 'rotation' in e: e['rotation']['origin']=[v*.72 for v in e['rotation']['origin']]
            finish_elements(original['elements'])
        write(TARGET / f'models/crafted/block/trophy/{entry["source"]}.json', original)
        states=adapt(read(ASSETS / f'blockstates/{entry["source"]}.json'),True)
        for key,model in states['variants'].items(): model['model'] += '_upper' if 'half=upper' in key else '_lower'
        write(TARGET / f'blockstates/{name}.json',states)
        source_states=read(ASSETS / f'blockstates/{entry["source"]}.json')
        for key,model in source_states['variants'].items():
            reduced=','.join(c for c in key.split(',') if not c.startswith('waterlogged='))
            if 'half=upper' in key: entry['shapes'][reduced]=model_boxes(model,lambda ref: upper['elements'])

def adapt(v, resource=False):
    if isinstance(v, str):
        if v == 'minecraft:chain': return 'minecraft:iron_chain'
        if v == 'minecraft:block/chain': return 'minecraft:block/iron_chain'
        if v.startswith('#handcrafted:'): return '#jugcraft:crafted/' + v.split(':', 1)[1]
        if v.startswith('handcrafted:'): return 'jugcraft:crafted' + ('/' if resource else '_') + v.split(':', 1)[1]
        return v
    if isinstance(v, list): return [adapt(x, resource) for x in v]
    if isinstance(v, dict): return {adapt(k, resource): adapt(x, resource) for k, x in v.items()}
    return v

@lru_cache(None)
def geometry(ref):
    if not ref.startswith('handcrafted:'): return []
    p = ASSETS / 'models' / (ref.split(':', 1)[1] + '.json')
    if not p.exists(): raise ValueError('Missing model ' + ref)
    d = read(p)
    return d.get('elements', geometry(d.get('parent', '')))

def ingredient(v):
    if isinstance(v, list): return [ingredient(x) for x in v]
    if isinstance(v, dict) and set(v) == {'item'}: return adapt(v['item'])
    if isinstance(v, dict) and set(v) == {'tag'}: return adapt('#' + v['tag'])
    return adapt(v)

def extra_data():
    for p in (SOURCE/'data/minecraft/recipe').glob('*.json'):
        d=read(p)
        if not str(d.get('result',{}).get('id','')).startswith('handcrafted:'): continue
        if 'ingredients' in d: d['ingredients']=[ingredient(v) for v in d['ingredients']]
        write(DATA/'recipe/crafted'/p.name,adapt(d))
    for namespace in ('minecraft','c'):
        for p in (SOURCE/'data'/namespace/'tags').rglob('*.json'):
            d=read(p);values=[adapt(v) for v in d.get('values',[]) if isinstance(v,str) and v.lstrip('#').startswith('handcrafted:')]
            if not values: continue
            rel=p.relative_to(SOURCE/'data'/namespace/'tags');rel=Path('block' if rel.parts[0]=='blocks' else rel.parts[0],*rel.parts[1:])
            target=RES/'data'/namespace/'tags'/rel
            existing=read(target) if target.exists() else {'replace':False,'values':[]}
            for v in values:
                if v not in existing['values']: existing['values'].append(v)
            write(target,existing)

def kind(name):
    if name.endswith('fancy_bed'): return 'crafted_bed'
    if name.endswith(('bench', 'chair', 'couch', 'cushion')): return 'seat'
    if name.endswith(('drawer', 'cupboard', 'nightstand', 'counter', 'shelf')): return 'storage'
    if name == 'oven': return 'crafted_oven'
    return 'decor'

def main():
    catalog = {'blocks': {}, 'items': {}}
    for p in (ASSETS / 'textures').rglob('*'):
        if p.is_file():
            to = TARGET / 'textures/crafted' / p.relative_to(ASSETS / 'textures')
            to.parent.mkdir(parents=True, exist_ok=True); shutil.copyfile(p, to)
            if p.parent.name == 'painting':
                to = TARGET / 'textures/painting/crafted' / p.name
                to.parent.mkdir(parents=True, exist_ok=True); shutil.copyfile(p, to)
    for p in (ASSETS / 'models').rglob('*.json'):
        write(TARGET / 'models/crafted' / p.relative_to(ASSETS / 'models'), runtime_model(read(p)))
    lang = read(TARGET / 'lang/en_us.json'); names = read(ASSETS / 'lang/en_us.json')
    for p in (ASSETS / 'blockstates').glob('*.json'):
        name = p.stem; states = read(p); props = collections.defaultdict(set); shapes = {}
        for key, model in states.get('variants', {}).items():
            for clause in key.split(','):
                if '=' in clause:
                    k, v = clause.split('='); props[k].add(v)
            shapes[key] = model_boxes(model, geometry)
        def condition(obj):
            for k, v in obj.items():
                if k in ('OR', 'AND'):
                    for child in v: condition(child)
                else: props[k].update(str(v).split('|'))
        for part in states.get('multipart', []): condition(part.get('when', {}))
        for choices in props.values():
            if choices <= {'true', 'false'}: choices.update(('true', 'false'))
        # Texture/seed variants often share collision geometry. Collapse only exact-equal boxes.
        for omitted in ('waterlogged', 'color', 'seed', 'occupied', 'counter', 'trim', 'type'):
            reduced = {}; consistent = True
            for key, boxes in shapes.items():
                reduced_key = ','.join(clause for clause in key.split(',') if not clause.startswith(omitted + '='))
                if reduced_key in reduced and reduced[reduced_key] != boxes: consistent = False; break
                reduced[reduced_key] = boxes
            if consistent: shapes = reduced
        definition = {'source': name, 'collection': 'crafted', 'kind': kind(name),
                      'properties': {k: sorted(v) for k, v in props.items()}, 'shapes': shapes,
                      'parts': multipart_shapes(states, geometry)}
        catalog['blocks']['crafted_' + name] = definition
        write(TARGET / f'blockstates/crafted_{name}.json', adapt(states, True))
        pool = {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'jugcraft:crafted_' + name}]}
        functions = []
        for prop in ('pieces', 'jars', 'books'):
            for count in props.get(prop, []):
                functions.append({'function': 'minecraft:set_count', 'count': int(count), 'conditions': [
                    {'condition': 'minecraft:block_state_property', 'block': 'jugcraft:crafted_' + name, 'properties': {prop: count}}]})
        saved_props = [p for p in ('color', 'counter', 'trim', 'type') if p in props]
        if saved_props: functions.append({'function': 'minecraft:copy_state', 'block': 'jugcraft:crafted_' + name, 'properties': saved_props})
        if functions: pool['entries'][0]['functions'] = functions
        if 'part' in props: pool['conditions'] = [{'condition': 'minecraft:block_state_property', 'block': 'jugcraft:crafted_' + name, 'properties': {'part': 'head'}}]
        if 'half' in props and 'upper' in props['half']: pool['conditions'] = [{'condition': 'minecraft:block_state_property', 'block': 'jugcraft:crafted_' + name, 'properties': {'half': 'lower'}}]
        write(DATA / f'loot_table/blocks/crafted_{name}.json', loot({'type': 'minecraft:block', 'pools': [pool]}))
        lang['block.jugcraft.crafted_' + name] = names.get('block.handcrafted.' + name, name.replace('_', ' ').title())
    for p in (ASSETS / 'models/item').glob('*.json'):
        name = p.stem
        write(TARGET / f'items/crafted_{name}.json', {'model': {'type': 'minecraft:model', 'model': 'jugcraft:crafted/item/' + name}})
        if 'crafted_' + name not in catalog['blocks']:
            catalog['items']['crafted_' + name] = {'source': name}
            lang['item.jugcraft.crafted_' + name] = names.get('item.handcrafted.' + name, name.replace('_', ' ').title())
    for p in (SOURCE / 'data/handcrafted/recipe').rglob('*.json'):
        d = read(p)
        if 'key' in d: d['key'] = {k: ingredient(v) for k, v in d['key'].items()}
        if 'ingredients' in d: d['ingredients'] = [ingredient(v) for v in d['ingredients']]
        d = adapt(d)
        # Three wool alone belongs to the installed backpack sleeping bag.
        if p.stem.endswith('_sheet'): d['ingredients'].append('minecraft:string')
        write(DATA / 'recipe/crafted' / p.relative_to(SOURCE / 'data/handcrafted/recipe'), d)
    for p in (SOURCE / 'data/handcrafted/tags').rglob('*.json'):
        rel = p.relative_to(SOURCE / 'data/handcrafted/tags')
        write(DATA / 'tags' / rel.parts[0] / 'crafted' / Path(*rel.parts[1:]), adapt(read(p)))
    for p in (SOURCE / 'data/handcrafted/painting_variant').glob('*.json'):
        write(DATA / f'painting_variant/crafted_{p.name}', adapt(read(p), True))
    # Atlas directories are explicit in modern Minecraft.
    atlas = read(RES / 'assets/minecraft/atlases/blocks.json')
    entry = {'type': 'minecraft:directory', 'source': 'crafted', 'prefix': 'crafted/'}
    if entry not in atlas['sources']: atlas['sources'].append(entry)
    bow = {'type':'minecraft:single','resource':'minecraft:item/bow','sprite':'jugcraft:crafted/vanilla_bow'}
    if bow not in atlas['sources']: atlas['sources'].append(bow)
    write(RES / 'assets/minecraft/atlases/blocks.json', atlas)
    finish_trophies(catalog)
    extra_data()
    write(RES / 'jugcraft/menagerie/crafted.json', catalog)
    write(TARGET / 'lang/en_us.json', lang)
    print('Crafted furniture:', len(catalog['blocks']), 'blocks,', len(catalog['items']), 'items')

if __name__ == '__main__': main()
