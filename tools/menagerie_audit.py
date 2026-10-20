"""Check that every shipped menagerie model has closed model/texture references."""
from menagerie_content import *

def main():
    errors=[]
    def resolve(value,kind,p):
        if not isinstance(value,str) or not value.startswith('jugcraft:'):return
        q=ASSET/kind/(value.split(':',1)[1]+('.png' if kind=='textures' else '.json'))
        if not q.exists():errors.append((p.relative_to(RES).as_posix(),kind,value))
    for p in (ASSET/'models/menagerie').rglob('*.json'):
        v=read(p);resolve(v.get('parent'),'models',p)
        if v.get('parent') in ('builtin/entity','minecraft:builtin/entity'):errors.append((str(p),'unsupported legacy entity renderer'))
        if v.get('parent') in ('minecraft:item/generated','minecraft:item/handheld') and not v.get('textures',{}).get('layer0'):errors.append((str(p),'generated model missing layer0'))
        for ref in v.get('textures',{}).values():resolve(ref,'textures',p)
    c=read(RES/'jugcraft/menagerie/catalog.json')
    grove=RES/'jugcraft/menagerie/grove.json'
    if grove.exists():c['species']+=read(grove)['species']
    fable=RES/'jugcraft/menagerie/fable.json'
    if fable.exists():c['species']+=read(fable)['species']
    companion=RES/'jugcraft/menagerie/companion.json'
    if companion.exists():c['species']+=read(companion)['species']
    for group in ('ecology','mutant','beast','landscape','cavern'):
        extra=RES/f'jugcraft/menagerie/{group}.json'
        if extra.exists():c['species']+=read(extra)['species']
    for s in c['species']:
        for v in s['variants']:
            for field in ['texture','babyTexture']:
                if not (ASSET/'textures'/(v[field]+'.png')).exists():errors.append((s['id'],field,v[field]))
            for field in ['model','babyModel']:
                if v.get(field) and not (ASSET/f'geckolib/models/entity/menagerie/{v[field]}.geo.json').exists():errors.append((s['id'],field,v[field]))
        for folder,suffix in [('models','.geo.json'),('animations','.animation.json')]:
            if not (ASSET/f'geckolib/{folder}/entity/menagerie'/(s['model']+suffix)).exists():errors.append((s['id'],folder,s['model']))
    # File existence alone cannot catch model-loader failures seen in the client.
    for scope in ['entity/menagerie','armor/menagerie']:
        for p in (ASSET/'geckolib/models'/scope).rglob('*.geo.json'):
            for geo in read(p).get('minecraft:geometry',[]):
                bones=geo.get('bones',[]);names=[b['name'] for b in bones]
                if len(names)!=len(set(names)):errors.append((str(p),'duplicate bone names'))
                parents={b['name']:b.get('parent') for b in bones}
                for name in names:
                    seen=set();node=name
                    while node:
                        if node in seen:errors.append((str(p),'cyclic bone hierarchy',name));break
                        if node not in parents:errors.append((str(p),'missing parent',node));break
                        seen.add(node);node=parents[node]
        for p in (ASSET/'geckolib/animations'/scope).rglob('*.animation.json'):
            if not read(p).get('animations'):errors.append((str(p),'empty animation definitions'))
    # The real client loader caught these despite the earlier model-only closure check.
    particle_sources=read(RES/'assets/minecraft/atlases/particles.json').get('sources',[])
    for p in (ASSET/'particles').glob('*.json'):
        for ref in read(p).get('textures',[]):
            if not ref.startswith('jugcraft:menagerie/'):continue
            sprite=ref.split(':',1)[1]
            matches=[source for source in particle_sources if source.get('type')=='minecraft:directory' and sprite.startswith(source.get('prefix','')) and (ASSET/'textures'/source['source']/(sprite.removeprefix(source.get('prefix',''))+'.png')).exists()]
            if not matches:errors.append((str(p),'missing particle atlas sprite',ref))
    for event,definition in read(ASSET/'sounds.json').items():
        for sound in definition.get('sounds',[]):
            if isinstance(sound,dict) and sound.get('type','file')!='file':continue
            ref=sound if isinstance(sound,str) else sound['name']
            if ref.startswith('jugcraft:menagerie/') and not (ASSET/'sounds'/(ref.split(':',1)[1]+'.ogg')).exists():errors.append((event,'missing audio',ref))
    for p in (ASSET/'equipment/menagerie').glob('*.json'):
        layers=read(p).get('layers',{})
        if not layers or any(not entries for entries in layers.values()):errors.append((str(p),'equipment requires nonempty layers'))
        for layer,entries in layers.items():
            for entry in entries:
                ref=entry['texture']
                if ref.startswith('jugcraft:') and not (ASSET/f'textures/entity/equipment/{layer}'/(ref.split(':',1)[1]+'.png')).exists():errors.append((str(p),'missing equipment texture',ref))
    write(ROOT/'art/owner-library/catalog/edited-2026-10-18/runtime-resource-audit.json',{'errors':errors,'species':len(c['species'])})
    print('Resource audit:',len(errors),'missing references')
    for error in errors[:30]:print(error)
    return bool(errors)

if __name__=='__main__':raise SystemExit(main())
