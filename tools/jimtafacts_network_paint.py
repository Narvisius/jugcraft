"""Source paintball tints/shimmer and statically recovered PaintSplotchesModel quads."""
import json

COLORS = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green', 'red', 'black']
ITEMS = [c + suffix for c in COLORS for suffix in ['_paint_ball', '_lumen_paint_ball']]
BLOCKS = ['paint']

def generate(source, assets, res, model, texture, dump, recipe):
    for name in ITEMS:
        data = json.loads((source / f'items/{name}.json').read_text(encoding='utf-8'))
        data['model']['model'] = model(data['model']['model'].split(':', 1)[1])
        dump(assets / f'items/jimta_{name}.json', data)
        color = name.split('_lumen_paint_ball')[0] if '_lumen_' in name else name.split('_paint_ball')[0]
        recipe(name, {'type': 'minecraft:crafting_shapeless', 'ingredients': ['jugcraft:jimta_' + color + '_paint_ball', 'minecraft:glowstone_dust'] if '_lumen_' in name else ['jugcraft:jimta_matter_ball', 'minecraft:' + color + '_dye'], 'result': {'id': 'jugcraft:jimta_' + name, 'count': 1 if '_lumen_' in name else 8}})
    multipart = []
    for pattern in range(3):
        ref = texture(f'ae2:block/paint{pattern + 1}')
        for lumen in [False, True]:
            name = f'block/jimtafacts/ae2/paint_{pattern}_{int(lumen)}'
            # Original source uses .2-block quads, an .001-block surface offset and one of these three textures.
            # Native bounded form keeps one centered splotch per air block; original colors/geometry dimensions retained.
            dump(assets / f'models/{name}.json', {'ambientocclusion': False, 'textures': {'paint': ref, 'particle': ref}, 'elements': [{'from': [6.4, 6.4, .016], 'to': [9.6, 9.6, .016], 'light_emission': 15 if lumen else 0, 'faces': {'south': {'texture': '#paint', 'uv': [0, 0, 16, 16], 'tintindex': 0}}}]})
            for face, x, y in [('north', 0, 0), ('east', 0, 90), ('south', 0, 180), ('west', 0, 270), ('up', 270, 0), ('down', 90, 0)]:
                multipart.append({'when': {'pattern': str(pattern), 'lumen': str(lumen).lower(), 'facing': face}, 'apply': {'model': 'jugcraft:' + name, 'x': x, 'y': y}})
    dump(assets / 'blockstates/jimta_paint.json', {'multipart': multipart})
    dump(assets / 'items/jimta_paint.json', {'model': {'type': 'minecraft:model', 'model': model('item/paint_ball'), 'tints': [{'type': 'minecraft:constant', 'value': 0xFFFFFFFF}]}})
    dump(res / 'data/jugcraft/loot_table/blocks/jimta_paint.json', {'type': 'minecraft:block', 'pools': []})
