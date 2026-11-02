"""Stitch model-referenced owner textures without pulling GUI/entity sheets into the atlas.

26.3 discovers block/ and item/ by default, not arbitrary importer directories.
Keep source PNGs, UVs, geometry and animation sidecars unchanged.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets'
ATLAS = ASSETS / 'minecraft/atlases/blocks.json'


def finish_models():
    """26.3 metadata repairs; keep original geometry, UVs and image bytes."""
    root = ASSETS / 'jugcraft/models'
    # Custom Gecko items still need a particle material in their native base.
    from nouveau_content import CATALOG, presentation
    particles = {}
    displays = presentation()
    for group in ('items', 'blocks'):
        for name, spec in CATALOG[group].items():
            geo = spec.get('geo')
            if not geo:
                continue
            tex = displays[geo]['texture'] if geo in displays else {
                'sword': 'enchanters_sword', 'spellbook_closed': 'spellbook_purple',
                'enchanters_rod': 'enchanters_rod_stowed'}.get(geo, geo)
            path = f'nouveau/{"block" if geo in displays else "item"}/{tex}'
            if (ASSETS / 'jugcraft/textures' / (path + '.png')).is_file():
                particles[f'nouveau/item/{name}'] = 'jugcraft:' + path
    for statue in ('player', 'keeper', 'ice_spider'):
        particles[f'item/jimta_spellbooks_{statue}_statue'] = 'jugcraft:block/jimtafacts/statue_particle'
    changed = 0
    for path in root.rglob('*.json'):
        original = path.read_text(encoding='utf-8')
        model = json.loads(original)
        ref = path.relative_to(root).as_posix()[:-5]
        textures = model.get('textures', {})
        for key, value in list(textures.items()):
            if value == 'minecraft:block/quartz_block':
                textures[key] = 'minecraft:block/quartz_block_side'
            if value in ('minecraft:block/chain', 'block/chain'):
                textures[key] = 'minecraft:block/iron_chain'
        if ref == 'jimtafacts/spellbooks/block/firefly_jar':
            # It owns its two cuboids; cube_all's inherited #all slots are invalid.
            model['parent'] = 'minecraft:block/block'
        if ref == 'newest/supps/item/lunch_basket':
            # This native item has no legacy entity renderer. Use the supplied
            # closed basket mesh for inventory, hand and Jade presentation.
            model['parent'] = 'jugcraft:newest/supps/block/lunch_basket'
            model['display'] = {'gui': {'rotation':[30,225,0], 'translation':[0,0,0], 'scale':[0.625,0.625,0.625]}}
        if ref == 'block/diagonal/jimta_iron_fence':
            model.setdefault('textures', {})['texture'] = '#bars'
        if ref in particles and not textures.get('particle'):
            model.setdefault('textures', {})['particle'] = particles[ref]
        elif not model.get('parent') and textures and not textures.get('particle'):
            # Use a real face's existing material for breaking/hit particles.
            key = next((face['texture'][1:] for element in model.get('elements', [])
                        for face in element.get('faces', {}).values()
                        if face.get('texture', '').startswith('#') and face['texture'][1:] in textures),
                       next(iter(textures)))
            textures['particle'] = '#' + key
        if model != json.loads(original):
            path.write_text(json.dumps(model, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
            changed += 1
    return changed


def references():
    return {value for path in (ASSETS / 'jugcraft/models').rglob('*.json')
            for value in json.loads(path.read_text(encoding='utf-8')).get('textures', {}).values()
            if isinstance(value, str) and value.startswith('jugcraft:')}



def drawn_references():
    """Effective materials of native item/block roots, after child overrides.
    Unused template defaults are not sprites that the game will draw.
    """
    from functools import lru_cache
    root=ASSETS/'jugcraft'
    @lru_cache(maxsize=None)
    def resolved(ref):
        if not isinstance(ref,str) or not ref.startswith('jugcraft:'):return {},None
        path=root/'models'/(ref.split(':',1)[1]+'.json')
        if not path.exists():return {},None
        data=json.loads(path.read_text(encoding='utf-8'))
        textures,elements=resolved(data.get('parent',''))
        return dict(textures,**data.get('textures',{})),data.get('elements',elements)
    def roots(value):
        if isinstance(value,dict):
            for key,v in value.items():
                if key in ('model','base') and isinstance(v,str):yield v
                else:yield from roots(v)
        elif isinstance(value,list):
            for v in value:yield from roots(v)
    used=set()
    for folder in ('blockstates','items'):
        for path in (root/folder).rglob('*.json'):used.update(roots(json.loads(path.read_text(encoding='utf-8'))))
    required={}
    for ref in used:
        textures,elements=resolved(ref)
        variables={f.get('texture','') for e in elements or [] for f in e.get('faces',{}).values()}
        variables.update('#'+k for k in textures if k=='particle' or k.startswith('layer'))
        for texture in variables:
            seen=set()
            while isinstance(texture,str) and texture.startswith('#') and texture not in seen:
                seen.add(texture);texture=textures.get(texture[1:],'')
            if isinstance(texture,str) and ':' in texture:required.setdefault(texture,ref)
    return required


def covered(texture, sources):
    path = texture.split(':', 1)[1]
    # Both vanilla atlases are searched by an unqualified model material.
    if path.startswith(('block/', 'item/')):
        return True
    return any((s['type'] == 'minecraft:directory' and path.startswith(s['prefix']))
               or (s['type'] == 'minecraft:single' and texture == s.get('sprite', s.get('resource')))
               for s in sources)


def generate(write=None):
    from imported_geometry_repairs import apply as repair_geometry
    repair_geometry()
    from newest_client_compat import finish
    finish()
    from newest_home import finish_item_models
    finish_item_models()
    finish_models()
    atlas = json.loads(ATLAS.read_text(encoding='utf-8'))
    # The mimic's supplied cuboids use the vanilla chest sheet. The chest atlas
    # isn't searched for ordinary item/block materials; stitch this reference too.
    chest = {'type': 'minecraft:single', 'resource': 'minecraft:entity/chest/normal'}
    if chest not in atlas['sources']:
        atlas['sources'].append(chest)
    particle_atlas = ASSETS / 'minecraft/atlases/particles.json'
    particles = json.loads(particle_atlas.read_text(encoding='utf-8'))
    wisp = {'type':'minecraft:single','resource':'jugcraft:newest/jugtania/particle/wisp'}
    if wisp not in particles['sources']:
        particles['sources'].append(wisp)
        if write is None: particle_atlas.write_text(json.dumps(particles,indent=2)+'\n',encoding='utf-8')
        else: write(particle_atlas,particles)
    required = drawn_references()
    for texture in sorted(references() | {t for t in required if t.startswith('jugcraft:')}):
        if covered(texture, atlas['sources']):
            continue
        source = ASSETS / 'jugcraft/textures' / (texture.split(':', 1)[1] + '.png')
        if not source.is_file():
            if texture in required:raise ValueError(f'Drawn model {required[texture]} has no texture: {texture}')
            continue  # Unused source asset or overridden parent-template default.
        atlas['sources'].append({'type': 'minecraft:single', 'resource': texture})
    if write is None:
        ATLAS.write_text(json.dumps(atlas, indent=2) + '\n', encoding='utf-8')
    else:
        write(ATLAS, atlas)


if __name__ == '__main__':
    generate()
