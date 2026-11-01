"""Class-based connection inventory for the imported native walls, fences and panes.
Authored furniture with custom connection properties is intentionally not a native wall.
"""
import json
from functools import lru_cache
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'

@lru_cache(maxsize=1)
def families():
    result={'wall':set(),'fence':set(),'pane':set(),'bars':set()}
    for file,prefix in [('newest_building',''),('newest_supps','supps_'),('newest_jugtania','jugtania_'),('newest_forbidden','forbidden_')]:
        path=ROOT/'jugcraft'/(file+'.json')
        if not path.exists():continue
        for name,d in json.loads(path.read_text(encoding='utf-8'))['blocks'].items():
            kind=d['kind'];name=prefix+name
            if name=='supps_wicker_fence':kind='wall'
            if kind=='window_pane':kind='pane'
            # Grass-capped walls use complete junction meshes, not separable arms.
            # Retain their authored cardinal joins until a dedicated diagonal mesh exists.
            if kind in ('thick_fence','wired_fence') and not name.endswith('_grass_topped_wall'):kind='fence'
            if kind in result and not name.endswith(('_wired_fence','_cheval_de_frise')):result[kind].add(name)
    path=ROOT/'jugcraft/ars_additions/decor.json'
    if path.exists():result['wall'].update('ars_additions_'+n for n in json.loads(path.read_text(encoding='utf-8')) if n.endswith('_wall'))
    result["fence"].add("ribbits_mossy_oak_planks_fence")
    return result

def wall_aliases(write,assets,name):
    state=json.loads((assets/'blockstates'/(name+'.json')).read_text(encoding='utf-8'))
    parts=state['multipart']
    for suffix,condition in [('post',{'up':'true'}),('side',{'north':'low'}),('side_tall',{'north':'tall'})]:
        part=next((p for p in parts if p.get('when',{})==condition),None)
        if part is None and suffix=='post':part=next((p for p in parts if not p.get('when')),None)
        if part is None:raise ValueError(f'{name}: missing original {suffix}')
        apply=part['apply']
        if isinstance(apply,list):raise ValueError(f'{name}: weighted wall part needs explicit adapter')
        if apply.get('x',0) or apply.get('y',0):raise ValueError(f'{name}: rotated north wall part')
        write(assets/'models/block'/(name+'_'+suffix+'.json'),{'parent':apply['model']})

def own_elements(assets,model):
    seen=set()
    while model.startswith('jugcraft:') and model not in seen:
        seen.add(model)
        data=json.loads((assets/'models'/(model.split(':',1)[1]+'.json')).read_text(encoding='utf-8'))
        if 'elements' in data:return data['elements']
        model=data.get('parent','')
    return None

def write_wall(write,assets,name,shared):
    import copy
    parts=json.loads((assets/'blockstates'/(name+'.json')).read_text(encoding='utf-8'))['multipart']
    result=[];arms={}
    for part in parts:
        condition=part.get('when',{})
        if not condition or condition=={'up':'true'}:
            result.append(copy.deepcopy(part));continue
        if len(condition)!=1:raise ValueError(f'{name}: unsupported wall condition {condition}')
        direction,height=next(iter(condition.items()))
        if direction not in ('north','south','east','west') or height not in ('low','tall'):continue
        result.append(dict(part,when={direction:'true','tall':str(height=='tall').lower()}))
        if direction!='north':continue
        variants=part['apply'] if isinstance(part['apply'],list) else [part['apply']]
        baked=[]
        for i,variant in enumerate(variants):
            if variant.get('x',0) or variant.get('y',0):raise ValueError(f'{name}: rotated north arm')
            suffix=('_tall' if height=='tall' else '')+('_variant_'+str(i) if i else '')
            target='block/diagonal/'+name+suffix
            elements=own_elements(assets,variant['model'])
            write(assets/'models'/(target+'.json'),{'parent':variant['model'],'elements':shared.turned(elements) if elements else shared.wall_arm(16 if height=='tall' else 14)})
            baked.append({**variant,'model':'jugcraft:'+target})
        arms[height]=baked
    for height,variants in arms.items():
        for direction,y in shared.DIAGONALS:
            apply=[{**v,'y':y} for v in variants]
            result.append({'when':{direction:'true','tall':str(height=='tall').lower()},'apply':apply if len(apply)>1 else apply[0]})
    write(assets/'blockstates'/('diagonal_'+name+'.json'),{'multipart':result})
