"""Original glass/covered cable meshes with supplied color atlases and real color-separated networks."""
import json

COLORS = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green', 'red', 'black']
KINDS = ['glass', 'covered', 'smart', 'dense_covered', 'dense_smart']
def suffix(kind): return kind.removeprefix('dense_') + '_dense' if kind.startswith('dense_') else kind
BLOCKS = [f'{color}_{suffix(kind)}_cable' for kind in KINDS for color in COLORS + ['fluix'] if (color, kind) != ('fluix', 'glass')]


def generate(source, assets, root, model, texture, dump, recipe):
    # Remove pre-integration spellings only; these never entered a published save.
    # Current registered names put "dense" after covered/smart, matching the supplied models.
    for color in COLORS + ['fluix']:
        for kind in ('covered','smart'):
            obsolete=f'jimta_{color}_dense_{kind}_cable'
            for path in (assets/f'blockstates/{obsolete}.json', assets/f'items/{obsolete}.json',
                         root/f'src/main/resources/data/jugcraft/loot_table/blocks/{obsolete}.json',
                         root/f'src/main/resources/data/jugcraft/recipe/jimtafacts/network/{obsolete[6:]}.json'):
                assert path.resolve().is_relative_to((root/'src/main/resources').resolve())
                if path.is_file():path.unlink()
    records = []
    for name in BLOCKS:
        kind = next(k for k in reversed(KINDS) if name.endswith('_' + suffix(k) + '_cable'))
        color = name.removesuffix('_' + suffix(kind) + '_cable')
        palette = 'transparent' if color == 'fluix' else color
        smart = kind.endswith('smart')
        parts = []
        for channels in (range(9) if smart else [None]):
            refs = {}
            for piece in ['center', 'connection']:
                src = source / f'models/part/cable/{kind}/{piece}.json'
                data = json.loads(src.read_text(encoding='utf-8'))
                data['textures'] = {k: texture(v.replace('_color_', palette).replace('/transparent', '/' + palette)) for k, v in data.get('textures', {}).items() if k != 'channels'}
                if smart:
                    bars = min(8, channels)
                    data['textures']['channels'] = texture(f'ae2:part/cable/{kind}/channels_0{min(4, bars)}')
                    data['textures']['channels_even'] = texture(f'ae2:part/cable/{kind}/channels_1{max(0, bars - 4)}')
                for element in data.get('elements', []):
                    for side, face in element.get('faces', {}).items():
                        face.pop('tintindex', None)
                        if face.get('texture') == '#channels' and side in ['east', 'west', 'north']: face['texture'] = '#channels_even'
                path = f'jimtafacts/ae2/part/cable/{kind}/{color}/{piece}' + (f'_{channels}' if smart else '')
                dest = assets / f'models/{path}.json'
                dump(dest, data)
                refs[piece] = 'jugcraft:' + path
                records.append({'source': src.relative_to(root).as_posix(), 'runtime': dest.relative_to(root).as_posix(), 'changes': 'Original geometry/UVs; supplied color and channel atlases. Dense indicators show four routed channels per bar; per-segment native routing.'})
            center = {'apply': {'model': refs['center']}}
            if smart: center['when'] = {'channels': str(channels)}
            parts.append(center)
            for face, x, y in [('north', 0, 0), ('east', 0, 90), ('south', 0, 180), ('west', 0, 270), ('up', 270, 0), ('down', 90, 0)]:
                when = {face: 'true'}
                if smart: when['channels'] = str(channels)
                parts.append({'when': when, 'apply': {'model': refs['connection'], 'x': x, 'y': y}})
        dump(assets / f'blockstates/jimta_{name}.json', {'multipart': parts})
        dump(assets / f'items/jimta_{name}.json', {'model': {'type': 'minecraft:model', 'model': model('item/' + name)}})
        ingredients = ['jugcraft:jimta_fluix_glass_cable']
        if kind == 'covered': ingredients.append('minecraft:white_wool')
        elif kind == 'smart': ingredients = [f'jugcraft:jimta_{color}_covered_cable', 'minecraft:redstone', 'minecraft:glowstone_dust']
        elif kind == 'dense_covered': ingredients = [f'jugcraft:jimta_{color}_covered_cable'] * 4
        elif kind == 'dense_smart': ingredients = [f'jugcraft:jimta_{color}_covered_dense_cable', 'minecraft:redstone', 'minecraft:glowstone_dust']
        if color != 'fluix' and kind in ['glass', 'covered']: ingredients.append('minecraft:' + color + '_dye')
        recipe(name, {'type': 'minecraft:crafting_shapeless', 'ingredients': ingredients, 'result': {'id': 'jugcraft:jimta_' + name}})
    return records
