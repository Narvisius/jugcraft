"""Extract declarative geometry/curves from the owner's decompiled client models.

Input is CFR output from Naturalist-Models-2026-10-19.zip. No supplied class is
loaded or executed. The deliberately narrow parser rejects unknown statements;
it preserves native box UVs, mirror flags, inflation, local pivots and radians.
Run with the path to the decompiled com/crispytwig/naturalist/client directory.
"""
import argparse
import hashlib
import json
import re
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'src/main/resources/assets/jugcraft/wildlife_models'


def number(value):
    value = re.sub(r'\((?:float|int|double)\)', '', value).strip()
    if not re.fullmatch(r'-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?[fFdD]?', value):
        raise ValueError('Not a numeric literal: ' + value)
    return float(value.rstrip('fFdD'))


def split_args(text):
    parts, start, depth = [], 0, 0
    for i, char in enumerate(text):
        if char in '({[': depth += 1
        elif char in ')}]': depth -= 1
        elif char == ',' and depth == 0:
            parts.append(text[start:i].strip()); start = i + 1
    parts.append(text[start:].strip())
    return parts


def calls(text):
    """Parse a fluent call chain without evaluating it."""
    pos = 0
    while pos < len(text):
        match = re.match(r'\.(\w+)\(', text[pos:])
        if not match: raise ValueError('Unexpected builder fragment: ' + text[pos:])
        begin = pos + match.end(); depth = 1; end = begin
        while depth:
            if text[end] == '(': depth += 1
            elif text[end] == ')': depth -= 1
            end += 1
        yield match[1], split_args(text[begin:end - 1]) if end > begin + 1 else []
        pos = end


def method_body(source):
    start = source.index('public static LayerDefinition createBodyLayer() {')
    start = source.index('{', start) + 1
    end = start; depth = 1
    while depth:
        if source[end] == '{': depth += 1
        elif source[end] == '}': depth -= 1
        end += 1
    return source[start:end - 1]


def geometry(source):
    parts, variables, texture = [], {}, None
    for statement in method_body(source).split(';'):
        statement = statement.strip()
        if not statement: continue
        if re.fullmatch(r'MeshDefinition \w+ = new MeshDefinition\(\)', statement): continue
        root = re.fullmatch(r'PartDefinition (\w+) = \w+\.getRoot\(\)', statement)
        if root: variables[root[1]] = '$mesh'; continue
        result = re.fullmatch(r'return LayerDefinition.create\(\(MeshDefinition\)\w+, \(int\)(\d+), \(int\)(\d+)\)', statement)
        if result: texture = [int(result[1]), int(result[2])]; continue
        part = re.fullmatch(r'PartDefinition (\w+) = (\w+)\.addOrReplaceChild\((.*)\)', statement)
        if not part: raise ValueError('Unknown geometry statement: ' + statement)
        name, builder, pose = split_args(part[3])
        name = json.loads(name)
        cubes, uv, mirror = [], [0, 0], False
        if not builder.startswith('CubeListBuilder'): raise ValueError(builder)
        for call, args in calls(builder[len('CubeListBuilder'):]):
            if call == 'create': assert not args
            elif call == 'texOffs': uv = [number(a) for a in args]
            elif call == 'mirror':
                assert not args or args[0] in ('true', 'false')
                mirror = not args or args[0] == 'true'
            elif call == 'addBox':
                assert len(args) == 7, args
                deformation = re.fullmatch(r'new CubeDeformation\((.*)\)', args[6])
                if not deformation: raise ValueError(args[6])
                inflate = [number(a) for a in split_args(deformation[1])]
                assert len(inflate) in (1, 3)
                cubes.append({'box': [number(a) for a in args[:6]], 'uv': uv[:],
                              'inflate': inflate * 3 if len(inflate) == 1 else inflate,
                              'mirror': mirror})
            else: raise ValueError(call)
        pm = re.fullmatch(r'PartPose\.(offset|offsetAndRotation)\((.*)\)', pose)
        if not pm: raise ValueError(pose)
        transform = [number(a) for a in split_args(pm[2])]
        if len(transform) == 3: transform += [0, 0, 0]
        assert len(transform) == 6
        parts.append({'name': name, 'parent': variables[part[2]], 'pose': transform, 'cubes': cubes})
        variables[part[1]] = name
    assert texture and parts
    return {'textureSize': texture, 'parts': parts}


def animations(source):
    result = {}
    for match in re.finditer(r'public static final AnimationDefinition (\w+) = (.*?);', source, re.S):
        expression = match[2].strip()
        assert expression.startswith('AnimationDefinition.Builder')
        clip = {'loop': False, 'channels': []}
        for call, args in calls(expression[len('AnimationDefinition.Builder'):]):
            if call == 'withLength': clip['length'] = number(args[0])
            elif call == 'looping': clip['loop'] = True
            elif call == 'build': assert not args
            elif call == 'addAnimation':
                bone = json.loads(args[0])
                channel = re.fullmatch(r'new AnimationChannel\(AnimationChannel.Targets\.(\w+), new Keyframe\[\]\{(.*)\}\)', args[1], re.S)
                if not channel: raise ValueError(args[1])
                frames = []
                for key in split_args(channel[2]):
                    key = re.fullmatch(r'new Keyframe\((.*)\)', key, re.S)
                    if not key: raise ValueError(channel[2])
                    time, vector, interpolation = split_args(key[1])
                    vector = re.fullmatch(r'\(Vector3fc\)KeyframeAnimations\.(\w+)\((.*)\)', vector)
                    if not vector: raise ValueError(key[1])
                    interp = interpolation.removeprefix('AnimationChannel.Interpolations.')
                    assert interp in ('LINEAR', 'CATMULLROM')
                    assert vector[1] in ('posVec', 'degreeVec', 'scaleVec')
                    frames.append({'time': number(time), 'vector': [number(a) for a in split_args(vector[2])], 'interpolation': interp})
                clip['channels'].append({'bone': bone, 'target': channel[1], 'frames': frames})
            else: raise ValueError(call)
        result[match[1]] = clip
    return result


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + '\n', encoding='utf-8')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('decompiled', type=Path)
    args = parser.parse_args()
    all_clips = {}
    for file in sorted((args.decompiled / 'animation/definitions').glob('*.java')):
        for name, clip in animations(file.read_text().strip()).items():
            all_clips[file.stem + '.' + name] = clip
    models = {}
    for file in sorted((args.decompiled / 'model/animal').rglob('*Model.java')):
        source = file.read_text()
        model = geometry(source)
        model['source'] = str(file.relative_to(args.decompiled)).replace('\\', '/')
        model['sourceSha256'] = hashlib.sha256(file.read_bytes()).hexdigest()
        fields = {}
        for field, path in re.findall(r'this\.(\w+) = ([^;]*getChild[^;]+);', source):
            children = re.findall(r'getChild\(\(String\)"([^"]+)"\)', path)
            if children: fields[field] = children[-1]
        look = re.search(r'applyHeadLook\(this\.(\w+),', source)
        model['headLook'] = fields.get(look[1], look[1]) if look else ''
        animation_root = re.search(r'getRootPartName\(\)\s*\{\s*return "([^"]+)";', source)
        model['animationRoot'] = animation_root[1] if animation_root else 'root'
        model['actions'] = {}
        states = re.findall(r'this\.animate\w*\(entity\.(\w+), (\w+Animation\.\w+),', source)
        semantics = {'idle': ['idle'], 'walk': ['walk', 'crawl', 'move', 'waddle'],
                     'run': ['run'], 'swim': ['swim'], 'fly': ['fly', 'flight'],
                     'sit': ['sit'], 'attack': ['attack', 'bite'], 'flop': ['flop']}
        for action, aliases in semantics.items():
            chosen = next((ref for alias in aliases for state, ref in states
                           if state.lower().startswith(alias)), None)
            if chosen: model['actions'][action] = all_clips[chosen]
        model['availableClips'] = sorted({ref for _, ref in states})
        key = file.stem.removesuffix('Model')
        models[key] = model
        write(OUT / (key.lower() + '.json'), model)
    catalog = json.loads((ROOT / 'src/main/resources/jugcraft/menagerie/catalog.json').read_text())
    by_name = {k.lower(): k for k in models}
    bindings, unavailable = {}, []
    texture_root = ROOT / 'src/main/resources/assets/jugcraft/textures'
    def size(texture):
        with Image.open(texture_root / (texture + '.png')) as image: return list(image.size)
    for species in catalog['species']:
        if not species['id'].startswith('wildlife_'): continue
        name = species['source'].replace('_', '')
        adult = by_name['blobfishpink' if name == 'blobfish' else name]
        baby = adult.removesuffix('Baby') + 'Baby'
        if baby not in models: baby = adult
        binding = {'adult': adult.lower(), 'baby': baby.lower(), 'variants': {}}
        base = next(v for v in species['variants'] if '/dye/' not in v['texture'])
        for variant in species['variants']:
            ages = {}
            for age, field in [('adult', 'texture'), ('baby', 'babyTexture')]:
                selected = adult if age == 'adult' else baby
                if name == 'bass':
                    selected = 'LargeBass' if variant['name'].endswith('_large') else 'MediumBass' if variant['name'].endswith('_medium') else 'Bass'
                skin = variant[field]
                dye = ''
                if '/dye/' in skin:
                    dye = skin.removesuffix('_baby')
                    if age == 'baby' and (texture_root / (dye + '_baby.png')).exists(): dye += '_baby'
                    skin = base[field]
                if size(skin) != models[selected]['textureSize']:
                    # Supplied bird skins currently omit the nestling atlas. Use
                    # the authored adult rig at infant scale, never guessed UVs.
                    if age != 'baby' or size(variant['texture'] if not dye else base['texture']) != models[adult]['textureSize']:
                        raise ValueError(('Incompatible skin', species['id'], variant['name'], age, skin))
                    selected = adult
                    skin = base['texture'] if dye else variant['texture']
                    unavailable.append([species['id'], variant['name'], 'missing compatible baby skin; scaled original adult'])
                if dye and size(dye) != models[selected]['textureSize']:
                    unavailable.append([species['id'], variant['name'], age + ': incompatible dye overlay omitted; base skin retained'])
                    dye = ''
                ages[age] = {'model': selected.lower(), 'skin': skin, 'dye': dye}
            binding['variants'][variant['name']] = ages
        bindings[species['id']] = binding
    write(OUT / 'bindings.json', bindings)
    # Keep all supplied curves as provenance even when Jugcraft does not yet have
    # the corresponding behavioral state. Runtime models use only bound actions.
    write(ROOT / 'art/owner-library/catalog/naturalist-models-2026-10-19/animations.json', all_clips)
    write(ROOT / 'art/owner-library/catalog/naturalist-models-2026-10-19/recovery.json', {
        'input': 'art/owner-library/originals/Naturalist-Models-2026-10-19.zip',
        'geometryCount': len(models), 'animationCount': len(all_clips), 'species': bindings,
        'method': 'CFR 0.152 static disassembly; restricted declarative parser; supplied classes never executed.',
        'implementation': 'OpenAI Codex (GPT-6)',
        'unavailableTexturePairings': unavailable,
    })
    print(f'Recovered {len(models)} model layers and {len(all_clips)} animation curves for {len(bindings)} species.')


if __name__ == '__main__': main()
