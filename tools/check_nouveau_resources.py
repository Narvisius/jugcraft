"""Validate the port's atlas visibility and Gecko 5 resource IDs, not just PNG existence."""
import json,re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets'

def check():
    errors=[]
    def read(p):return json.loads(p.read_text(encoding='utf-8'))
    def visible(ref,atlas):
        ns,name=ref.split(':',1)
        for s in read(ASSETS/'minecraft/atlases'/f'{atlas}.json')['sources']:
            if s['type']=='minecraft:single' and s.get('sprite',s.get('resource'))==ref and (ASSETS/ns/'textures'/(name+'.png')).exists():return True
            prefix=s.get('prefix','')
            if s['type']=='minecraft:directory' and name.startswith(prefix):
                p=ASSETS/ns/'textures'/s['source']/(name[len(prefix):]+'.png')
                if p.exists():return True
        return False
    textures=set()
    for p in (ASSETS/'jugcraft/models/nouveau').rglob('*.json'):
        model=read(p)
        if model.get('parent') in ('builtin/entity','minecraft:builtin/entity'):errors.append('Unsupported legacy entity parent: '+str(p.relative_to(ROOT)))
        if model.get('parent') in ('minecraft:item/generated','minecraft:item/handheld') and not model.get('textures',{}).get('layer0'):errors.append('Generated item missing layer0: '+str(p.relative_to(ROOT)))
        textures.update(v for v in model.get('textures',{}).values() if v.startswith('jugcraft:nouveau/'))
    for ref in textures:
        if not visible(ref,'blocks'):errors.append('Model sprite absent from block atlas: '+ref)
    for p in (ASSETS/'jugcraft/particles/nouveau').glob('*.json'):
        for ref in read(p).get('textures',[]):
            if ref.startswith('jugcraft:') and not visible(ref,'particles'):errors.append('Particle absent from atlas: '+ref)
    for p in (ASSETS/'jugcraft/geckolib/animations').rglob('nouveau/*.json'):
        value=read(p)
        if not value.get('animations'):errors.append('Empty animation file: '+str(p.relative_to(ROOT)))
        if 'swimcharge' in p.read_text(encoding='utf-8'):errors.append('Undefined legacy animation variable: '+str(p.relative_to(ROOT)))
    for name in ('NouveauVisuals.java','NouveauEquipmentRenderer.java'):
        text=(ROOT/'src/client/java/io/github/jimbozoomer/jugcraft/client'/name).read_text(encoding='utf-8')
        if re.search(r'geckolib/(?:models|animations)/|\.geo\.json|\.animation\.json',text):errors.append('Gecko 5 renderer must return logical IDs: '+name)
    fire=read(ASSETS/'jugcraft/blockstates/nouveau_magic_fire.json')
    if any('when' in part for part in fire.get('multipart',[])):errors.append('Native fire model may not require vanilla fire attachment properties')
    return errors,len(textures)
if __name__=='__main__':
    errors,count=check()
    if errors:print('\n'.join(errors));raise SystemExit(1)
    print(f'PASS: {count} Nouveau model sprite bindings, particle atlas, animation definitions and Gecko 5 IDs.')
