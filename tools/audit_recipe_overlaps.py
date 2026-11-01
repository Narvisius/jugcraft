"""Report crafting overlaps, resolving item tags, mirrored patterns and shapeless permutations.

Read supplied vanilla/dependency jars before local resources (highest priority).
Conditions are inventoried, not executed: reports are candidates until runtime matching.
Special/custom recipes need their runtime matcher; their types are explicitly inventoried.
No recipes or originals are modified.
"""
import argparse
import collections
import functools
import itertools
import io
import json
from pathlib import Path
import zipfile

ROOT = Path(__file__).resolve().parents[1]

def audit(jars=()):
    recipes, tags = {}, {}
    def read(name, data):
        parts = name.split('/')
        if len(parts) < 4 or parts[0] != 'data' or not name.endswith('.json'): return
        if parts[2] == 'recipe': recipes[parts[1]+':'+ '/'.join(parts[3:])[:-5]] = data
        elif parts[2:4] == ['tags','item']:
            key = parts[1]+':'+ '/'.join(parts[4:])[:-5]
            tags[key] = ([] if data.get('replace') else tags.get(key, [])) + data.get('values', [])
    def archive_data(source):
        with zipfile.ZipFile(source) as archive:
            for name in archive.namelist():
                if name.startswith('data/') and ('/recipe/' in name or '/tags/item/' in name) and name.endswith('.json'):
                    read(name, json.loads(archive.read(name)))
                elif name.startswith('META-INF/jars/') and name.endswith('.jar'):
                    archive_data(io.BytesIO(archive.read(name)))
    for jar in jars: archive_data(jar)
    for path in sorted((ROOT/'src/main/resources/data').rglob('*.json')):
        if '/recipe/' in path.as_posix() or '/tags/item/' in path.as_posix():
            read(path.relative_to(ROOT/'src/main/resources').as_posix(), json.loads(path.read_text(encoding='utf-8')))
    unresolved = set()
    @functools.lru_cache(None)
    def tag(name, seen=frozenset()):
        if name in seen: return frozenset()
        if name not in tags: unresolved.add(name)
        values = set()
        for entry in tags.get(name, []):
            value = entry if isinstance(entry,str) else entry['id']
            values.update(tag(value[1:], seen|{name}) if value.startswith('#') else [value])
        return frozenset(values)
    def ingredient(value):
        if isinstance(value,list): return frozenset().union(*(ingredient(i) for i in value))
        if isinstance(value,dict):
            value = value.get('item') or ('#'+value['tag'] if 'tag' in value else '')
        if not value: return frozenset()
        return tag(value[1:]) if value.startswith('#') else frozenset([value])
    def output(data):
        r = data.get('result', {})
        if isinstance(r,str): r={'id':r}
        return dict(r, count=r.get('count',1))
    entries, skipped = [], []
    for name, data in recipes.items():
        kind = data.get('type')
        if kind not in ('minecraft:crafting_shaped','minecraft:crafting_shapeless'): continue
        rows = None
        if kind.endswith('_shaped'):
            rows = [[ingredient(data['key'][c]) if c != ' ' else None for c in row] for row in data['pattern']]
            while rows and not any(rows[0]): rows.pop(0)
            while rows and not any(rows[-1]): rows.pop()
            while rows and all(row[0] is None for row in rows): rows=[row[1:] for row in rows]
            while rows and all(row[-1] is None for row in rows): rows=[row[:-1] for row in rows]
            ingredients=[v for row in rows for v in row if v is not None]
        else: ingredients=[ingredient(v) for v in data['ingredients']]
        if not ingredients or any(not v for v in ingredients): skipped.append(name); continue
        entries.append(dict(id=name, rows=rows, ingredients=ingredients, output=output(data)))
    def assignment(a,b):
        possibilities = [[(j, min(v&w)) for j,w in enumerate(b) if v&w] for v in a]
        order=sorted(range(len(a)), key=lambda i:len(possibilities[i])); chosen={}
        def search(depth, used):
            if depth==len(a): return [chosen[i] for i in range(len(a))]
            i=order[depth]
            for j,item in possibilities[i]:
                if j not in used:
                    chosen[i]=item
                    found=search(depth+1,used|{j})
                    if found:return found
            return None
        return search(0,set())
    def witness(a,b):
        ar,br=a['rows'],b['rows']
        if ar is not None and br is not None:
            if len(ar)!=len(br) or len(ar[0])!=len(br[0]):return None
            for rows in (br,[r[::-1] for r in br]):
                result=[]
                for x,y in zip(itertools.chain.from_iterable(ar), itertools.chain.from_iterable(rows)):
                    if x is None and y is None:result.append(None)
                    elif x is None or y is None or not x&y:break
                    else:result.append(min(x&y))
                else:return dict(width=len(ar[0]),height=len(ar),items=result)
            return None
        if ar is None and br is not None:return witness(b,a)
        chosen=assignment(a['ingredients'],b['ingredients'])
        if not chosen:return None
        if ar is not None:
            it=iter(chosen);return dict(width=len(ar[0]),height=len(ar),items=[next(it) if v is not None else None for row in ar for v in row])
        width=min(3,len(chosen));height=(len(chosen)+width-1)//width
        return dict(width=width,height=height,items=chosen+[None]*(width*height-len(chosen)))
    inverted=collections.defaultdict(set)
    for i,e in enumerate(entries):
        for item in frozenset().union(*e['ingredients']):inverted[len(e['ingredients']),item].add(i)
    overlaps=[]
    for i,a in enumerate(entries):
        anchor=min(a['ingredients'],key=len)
        candidates=set().union(*(inverted[len(a['ingredients']),v] for v in anchor))
        for j in sorted(candidates):
            if j<=i:continue
            b=entries[j]
            if a['output']==b['output']:continue
            sample=witness(a,b)
            if sample:overlaps.append(dict(recipes=[a['id'],b['id']],outputs=[a['output'],b['output']],witness=sample))
    # Cooking/stonecutting exact descriptors are separate, not crafting-grid conflicts.
    processing=collections.defaultdict(list)
    for name,d in recipes.items():
        if d.get('type') in ('minecraft:smelting','minecraft:blasting','minecraft:smoking','minecraft:campfire_cooking','minecraft:stonecutting'):
            processing[d['type'],json.dumps(d.get('ingredient'),sort_keys=True)].append((name,output(d)))
    process_overlaps=[dict(type=t,recipes=[n for n,o in v],native_picker=t=='minecraft:stonecutting') for (t,k),v in processing.items() if len({json.dumps(o,sort_keys=True) for n,o in v})>1]
    cooking_tags=[]
    for kind in ('minecraft:smelting','minecraft:blasting','minecraft:smoking','minecraft:campfire_cooking'):
        candidates=[(name,ingredient(d['ingredient']),output(d)) for name,d in recipes.items() if d.get('type')==kind]
        for (an,ai,ao),(bn,bi,bo) in itertools.combinations(candidates,2):
            if ao!=bo and ai&bi:cooking_tags.append(dict(type=kind,recipes=[an,bn],witness=min(ai&bi)))
    return dict(total_recipes=len(recipes),types=dict(collections.Counter(d.get('type','?') for d in recipes.values())),
        standard_crafting_checked=len(entries),unresolved_tags=sorted(unresolved),skipped_empty_ingredients=skipped,
        crafting_overlaps=overlaps,processing_exact_overlaps=process_overlaps,cooking_tag_overlaps=cooking_tags,
        limitations='Conditional resources are candidates; specialized/machine recipes are inventoried by type, not proved equivalent. Runtime selector uses actual loaded Recipe.matches, including special recipes.')

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jar',action='append',default=[])
    parser.add_argument('--out',type=Path,default=ROOT/'build/recipe-overlaps.json')
    parser.add_argument('--witnesses',type=Path,help='Optionally refresh the native test witness snapshot')
    args=parser.parse_args();result=audit(args.jar)
    args.out.parent.mkdir(parents=True,exist_ok=True)
    args.out.write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
    if args.witnesses:
        args.witnesses.parent.mkdir(parents=True,exist_ok=True)
        args.witnesses.write_text(json.dumps(result['crafting_overlaps'],indent=2)+'\n',encoding='utf-8')
    print(f"{result['total_recipes']} recipes inventoried; {result['standard_crafting_checked']} standard crafting recipes checked; {len(result['crafting_overlaps'])} conflicting pairs; {len(result['unresolved_tags'])} unresolved tags. Report: {args.out}")
