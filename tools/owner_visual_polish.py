"""Native model repairs. Preserve source files, texture pixels, cavity openings and UV orientation."""
from copy import deepcopy
from itertools import product
from menagerie_content import ASSET, ROOT, read, write
import model_writer as mw
from art_check import _face_point, _uv_turn

OPPOSITE={'north':'south','south':'north','east':'west','west':'east','up':'down','down':'up'}

def face_panel(e, face, thickness=.1):
    """Turn an inward legacy face into a positively bounded wall, retaining its UV corner mapping."""
    axis,sign=mw._FACE_DIR[face];other=[i for i in range(3) if i!=axis]
    flipped=sum(e['to'][i]<e['from'][i] for i in other)%2
    target=OPPOSITE[face] if flipped else face
    normal=mw._FACE_DIR[target][1]
    plane=e['to'][axis] if sign>0 else e['from'][axis]
    lo=[min(a,b) for a,b in zip(e['from'],e['to'])];hi=[max(a,b) for a,b in zip(e['from'],e['to'])]
    lo[axis]=plane-thickness if normal>0 else plane
    hi[axis]=plane if normal>0 else plane+thickness
    spec=deepcopy(e['faces'][face]);spec.pop('cullface',None)
    old_uv=spec.get('uv',mw.auto_uv(face,e['from'],e['to']))
    corners=list(product((0,1),repeat=2));expected=[]
    for a,b in corners:
        p=_face_point(target,lo,hi,a,b)
        old=min(corners,key=lambda ab:sum((p[i]-_face_point(face,e['from'],e['to'],*ab)[i])**2 for i in range(3)))
        u,v=_uv_turn(*old,spec.get('rotation',0))
        expected.append((old_uv[0]+u*(old_uv[2]-old_uv[0]),old_uv[1]+v*(old_uv[3]-old_uv[1])))
    found=False
    for flip_u,flip_v,rotation in product((False,True),(False,True),(0,90,180,270)):
        uv=[old_uv[2] if flip_u else old_uv[0],old_uv[3] if flip_v else old_uv[1],old_uv[0] if flip_u else old_uv[2],old_uv[1] if flip_v else old_uv[3]]
        actual=[]
        for a,b in corners:
            u,v=_uv_turn(a,b,rotation);actual.append((uv[0]+u*(uv[2]-uv[0]),uv[1]+v*(uv[3]-uv[1])))
        if all(abs(x-y)<1e-5 for p,q in zip(actual,expected) for x,y in zip(p,q)):
            spec['uv']=uv;spec['rotation']=rotation;found=True;break
    if not found:raise ValueError('Cannot preserve face UV orientation')
    out={k:deepcopy(v) for k,v in e.items() if k not in ('from','to','faces')}
    out.update({'from':lo,'to':hi,'faces':{f:deepcopy(spec) for f in mw.FACES}})
    return out

def repair_mesh(d, name):
    elements=d.get('elements',[])
    reverse=[e for e in elements if any(a>b for a,b in zip(e['from'],e['to']))]
    out=[]
    for e in elements:
        inverted=e in reverse
        # Exterior surfaces enclosing a reversed interior are shells, not solid boxes.
        encloses=any(all(e['from'][i]<=min(c['from'][i],c['to'][i])+.11 and e['to'][i]>=max(c['from'][i],c['to'][i])-.11 for i in range(3)) for c in reverse) if not inverted else False
        hollow_bucket=name.endswith('/spile_bucket') and e is elements[0] and min(b-a for a,b in zip(e['from'],e['to']))>1
        if inverted or encloses or hollow_bucket:
            out.extend(face_panel(e,f) for f in e.get('faces',{}))
        else:out.append(deepcopy(e))
    if out:mw.finish_closed(out)
    # Inventory models spanning two blocks must remain within Minecraft's [-16,32] bounds.
    values=[v for e in out for side in ('from','to') for v in e[side]]
    if values:
        scale=min(1,32/max(32,max(values)),16/max(16,-min(values)))
        if scale<1:
            for e in out:
                for side in ('from','to'):e[side]=[round(v*scale,6) for v in e[side]]
                if 'rotation' in e:e['rotation']['origin']=[round(v*scale,6) for v in e['rotation']['origin']]
    d['elements']=out

def nouveau():
    changed=[]
    for p in (ASSET/'models/nouveau').rglob('*.json'):
        d=read(p);before=deepcopy(d);parent=d.get('parent','')
        if parent=='minecraft:block/cube_all' and d.get('elements') and 'all' not in d.get('textures',{}):
            d['parent']='minecraft:block/block'
        if parent in ('minecraft:builtin/entity','builtin/entity') or (parent=='minecraft:item/generated' and not d.get('textures',{}).get('layer0')):
            # These bases only supply display transforms to registered special renderers.
            # A generated parent requires layer0 even when the item uses Gecko geometry.
            d.pop('parent',None)
            if p.stem=='worn_notebook':
                # This one is a regular icon, not a special renderer.
                d['parent']='minecraft:item/generated'
                d['textures']={'layer0':'jugcraft:nouveau/item/worn_notebook'}
                d.pop('display',None)
        if parent=='minecraft:item/template_spawn_egg' or p.stem.endswith('_se'):
            texture=ASSET/'textures/nouveau/item'/f'{p.stem}.png'
            if texture.exists():d.update(parent='minecraft:item/generated',textures={'layer0':'jugcraft:nouveau/item/'+p.stem})
            else:
                d['parent']='minecraft:item/cow_spawn_egg';d.pop('textures',None)
                colors={'starbuncle':(0xB57B39,0xF7D383),'drygmy':(0x79533B,0xB8C17C),'whirlisprig':(0x4C7C47,0xC2D892),'alakarkinos':(0x416F93,0xD1AB5C),'wilden_guardian':(0x59645A,0xBBCAB5),'wilden_hunter':(0x4E493C,0xD1B67C),'wilden_stalker':(0x333B44,0x9EBBC7)}
                base,spots=colors[p.stem.removesuffix('_se')]
                write(ASSET/f'items/nouveau_{p.stem}.json',{'model':{'type':'minecraft:model','model':'jugcraft:nouveau/item/'+p.stem,'tints':[{'type':'minecraft:constant','value':base},{'type':'minecraft:constant','value':spots}]}})
        if d!=before:write(p,d);changed.append(p.relative_to(ASSET).as_posix())
    return changed

def legacy_bases():
    changed=[]
    for p in (ASSET/'models/menagerie').rglob('*.json'):
        d=read(p)
        if d.get('parent') not in ('builtin/entity','minecraft:builtin/entity'):continue
        d['parent']='minecraft:item/handheld'
        group=p.relative_to(ASSET/'models/menagerie').parts[0]
        name=p.stem.removesuffix('_throwing').removesuffix('_charging').removesuffix('_blocking')
        texture=ASSET/f'textures/menagerie/{group}/item/{name}.png'
        if texture.exists():d.setdefault('textures',{})['layer0']=f'jugcraft:menagerie/{group}/item/{name}'
        write(p,d);changed.append(p.relative_to(ASSET).as_posix())
    return changed

def main():
    changed=[]
    for scope in ('household','menagerie/beast','menagerie/ecology','menagerie/landscape','menagerie/cavern'):
        for p in (ASSET/'models'/scope).rglob('*.json'):
            d=read(p)
            if not d.get('elements'):continue
            before=deepcopy(d);repair_mesh(d,p.relative_to(ASSET/'models').with_suffix('').as_posix())
            if d!=before:write(p,d);changed.append(p.relative_to(ASSET).as_posix())
    # Native aliases of the imported wall models contain their own elements.
    for name in ('cavern_cinder_block','cavern_frosted_gingerbread_brick'):
        for suffix in ('_wall_side','_wall_side_tall'):
            p=ASSET/f'models/block/{name}{suffix}.json';d=read(p);before=deepcopy(d);repair_mesh(d,name+suffix)
            if d!=before:write(p,d);changed.append(p.relative_to(ASSET).as_posix())
    n=nouveau();legacy=legacy_bases()
    from owner_render_adapters import renderer_only_items
    renderer_only_items()
    record=ROOT/'art/owner-library/catalog/edited-2026-10-18-part2/visual-polish.json'
    prior=read(record) if record.exists() else {}
    write(record,{**prior,'models':sorted(set(changed+prior.get('models',[]))),'nouveauModels':sorted(set(n+prior.get('nouveauModels',[]))),'legacyBases':sorted(set(legacy+prior.get('legacyBases',[]))),'originalPixelsChanged':False,'method':'Inward faces reconstructed as bounded wall panels with identical UV corner mapping; cavity openings preserved; solid trim holes closed and surface depths separated. Two-block inventory forms fit native coordinate bounds. Legacy unnamespaced entity bases use supported native fallback parents.'})
    print('Polished mesh templates:',len(changed),'Nouveau metadata:',len(n))

if __name__=='__main__':main()
