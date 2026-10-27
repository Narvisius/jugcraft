"""Read supplied Blockbench meshes/cubes without executing any supplied programs."""
import base64
import copy
import json
import shutil
from pathlib import Path
import bbmodel
from crafted_furniture import SOURCE, RES, TARGET, DATA, read, write, ingredient

ASSETS = SOURCE / 'assets/immersive_aircraft'
NAMES = ('airship', 'bamboo_hopper', 'biplane', 'cargo_airship', 'gyrodyne', 'quadrocopter', 'warship')

def adapt(v, resource=False):
    if isinstance(v, str):
        if v.startswith('#immersive_aircraft:'): return '#jugcraft:aircraft/' + v.split(':', 1)[1]
        if v.startswith('immersive_aircraft:'): return 'jugcraft:aircraft' + ('/' if resource else '_') + v.split(':', 1)[1]
        return v
    if isinstance(v, list): return [adapt(x, resource) for x in v]
    if isinstance(v, dict): return {k: adapt(x, resource) for k, x in v.items()}
    return v

def model(name):
    doc = read(ASSETS / f'objects/{name}.bbmodel')
    textures = []
    for i, texture in enumerate(doc['textures']):
        filename = texture.get('name', '')
        candidates = list((ASSETS / 'textures/entity').rglob(filename)) if filename else []
        raw = candidates[0].read_bytes() if len(candidates) == 1 else base64.b64decode(texture['source'].split(',', 1)[1])
        path = TARGET / f'textures/aircraft/model/{name}_{i}.png';path.parent.mkdir(parents=True, exist_ok=True);path.write_bytes(raw)
        from PIL import Image
        with Image.open(path) as image: size = image.size
        # Blockbench UV coordinates use the logical atlas, not PNG pixel size.
        resolution = doc.get('resolution', {})
        uv_size = (texture.get('uv_width', resolution.get('width', size[0])),
                   texture.get('uv_height', resolution.get('height', size[1])))
        textures.append((f'aircraft/model/{name}_{i}', uv_size))
    elements = {e['uuid']: e for e in doc['elements']}
    animation = {}
    for clip in doc.get('animations', []):
        for uid, animator in clip.get('animators', {}).items():
            for frame in animator.get('keyframes', []):
                animation.setdefault(uid, {})[frame['channel']] = frame.get('data_points', [{}])[0]
    def quads(e, origin):
        if not e.get('export', True): return []
        if 'vertices' in e: faces = bbmodel.mesh_faces(e, [], 'body')
        else: faces = [(p, uv, n, data) for _, p, uv, n, data in bbmodel.cube_faces(e, [], 'body')]
        result = []
        for points, uv, normal, data in faces:
            tex = data.get('texture')
            if tex is None: continue
            texture, size = textures[int(tex)]
            # Undo the armor helper's coordinate convention, retaining the source Blockbench coordinates.
            points = [(-p[0]-origin[0], 24-p[1]-origin[1], p[2]-origin[2]) for p in points]
            result.append({'texture': texture, 'tinted': '_color' in doc['textures'][int(tex)].get('name',''), 'cutout': True, 'normal': [-normal[0], -normal[1], normal[2]],
                'vertices': [[*p, t[0]/size[0], t[1]/size[1]] for p, t in zip(points, uv)]})
        return result
    def node(item, parent_origin):
        if isinstance(item, str):
            e = elements[item]
            return {'name': e.get('name', ''), 'offset': [0,0,0], 'rotation': [0,0,0], 'quads': quads(e, parent_origin), 'children': []}
        if item.get('name') == 'lod1': return None
        origin = item.get('origin', [0,0,0])
        children = [node(c, origin) for c in item.get('children', [])]
        return {'name': item.get('name', ''), 'offset': [a-b for a,b in zip(origin,parent_origin)],
                'rotation': item.get('rotation', [0,0,0]), 'animation': animation.get(item.get('uuid'), {}),
                'quads': [], 'children': [c for c in children if c is not None]}
    return [n for n in (node(v, [0,0,0]) for v in doc['outliner']) if n is not None]

def main():
    for p in (ASSETS/'sounds').rglob('*'):
        if p.is_file():
            target=TARGET/'sounds/aircraft'/p.relative_to(ASSETS/'sounds');target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,target)
    sounds=read(TARGET/'sounds.json')
    for key,value in read(ASSETS/'sounds.json').items(): sounds['aircraft_'+key]=adapt(value,True)
    write(TARGET/'sounds.json',sounds)
    for p in (ASSETS / 'textures').rglob('*'):
        if p.is_file():
            target=TARGET/'textures/aircraft'/p.relative_to(ASSETS/'textures');target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,target)
    for p in (ASSETS/'models').rglob('*.json'): write(TARGET/'models/aircraft'/p.relative_to(ASSETS/'models'),adapt(read(p),True))
    for p in (ASSETS/'items').glob('*.json'): write(TARGET/f'items/aircraft_{p.name}',adapt(read(p),True))
    atlas=read(RES/'assets/minecraft/atlases/blocks.json')
    source={'type':'minecraft:directory','source':'aircraft/item','prefix':'aircraft/item/'}
    if source not in atlas['sources']: atlas['sources'].append(source)
    write(RES/'assets/minecraft/atlases/blocks.json',atlas)
    catalog={n:read(SOURCE/f'data/immersive_aircraft/aircraft/{n}.json') for n in NAMES}
    upgrades={p.stem:read(p) for p in (SOURCE/'data/immersive_aircraft/aircraft_upgrades').glob('*.json')}
    write(RES/'jugcraft/aircraft.json',{'craft':catalog,'upgrades':upgrades})
    write(TARGET/'aircraft_models.json',{p.stem:model(p.stem) for p in (ASSETS/'objects').glob('*.bbmodel')})
    lang=read(TARGET/'lang/en_us.json')
    for k,v in read(ASSETS/'lang/en_us.json').items():
        if k.startswith(('item.immersive_aircraft.','entity.immersive_aircraft.')):
            category,_,name=k.split('.',2);lang[category+'.jugcraft.aircraft_'+name]=v
    write(TARGET/'lang/en_us.json',lang)
    for p in (SOURCE/'data/immersive_aircraft/recipe').glob('*.json'):
        d=adapt(read(p));
        if 'key' in d:d['key']={k:ingredient(v) for k,v in d['key'].items()}
        if 'ingredients' in d:d['ingredients']=[ingredient(v) for v in d['ingredients']]
        write(DATA/'recipe/aircraft'/p.name,d)
    print('Preserved aircraft meshes, animations, skins and specifications:',len(catalog))

if __name__=='__main__':main()
