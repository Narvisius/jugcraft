"""Original sky utility meshes; closed chest item adapted from the source's Java cube UV layout."""
BLOCKS = ['sky_stone_chest', 'smooth_sky_stone_chest', 'sky_stone_tank', 'crank', 'crystal_resonance_generator', 'light_detector']

def generate(source, assets, res, model, texture, dump, recipe):
    def ji(name): return 'jugcraft:jimta_' + name
    def cube(origin, size, uv):
        x, y, z = origin; w, h, d = size; u, v = uv
        rectangles = {'west': [u, v+d, u+d, v+d+h], 'north': [u+d, v+d, u+d+w, v+d+h], 'east': [u+d+w, v+d, u+2*d+w, v+d+h], 'south': [u+2*d+w, v+d, u+2*d+2*w, v+d+h], 'up': [u+d, v, u+d+w, v+d], 'down': [u+d+w, v+d, u+d+2*w, v]}
        return {'from': [x, y, z], 'to': [x+w, y+h, z+d], 'faces': {face: {'texture': '#chest', 'uv': [n/4 for n in box]} for face, box in rectangles.items()}}
    for name, tex, block in [('sky_stone_chest', 'skychest', 'sky_stone_block'), ('smooth_sky_stone_chest', 'skyblockchest', 'smooth_sky_stone_block')]:
        ref = texture('ae2:block/' + tex)
        mesh = {'parent': 'minecraft:block/block', 'textures': {'particle': ref, 'chest': ref}, 'elements': [cube((1,0,1),(14,10,14),(0,19)), cube((1,10,1),(14,5,14),(0,0)), cube((7,8,15),(2,4,1),(0,0))]}
        target = f'block/jimtafacts/ae2/{name}_closed'
        dump(assets / f'models/{target}.json', mesh)
        dump(assets / f'items/jimta_{name}.json', {'model': {'type':'minecraft:model', 'model':'jugcraft:'+target}})
        dump(assets / f'blockstates/jimta_{name}.json', {'variants': {'': {'model': model('block/'+name)}}})
        recipe(name, {'type':'minecraft:crafting_shaped', 'pattern':['SSS','S S','SSS'], 'key': {'S': ji(block)}, 'result': {'id':ji(name)}})
    name = 'sky_stone_tank'
    ref = model('block/'+name)
    dump(assets / f'blockstates/jimta_{name}.json', {'variants': {'': {'model':ref}}})
    dump(assets / f'items/jimta_{name}.json', {'model': {'type':'minecraft:model', 'model':ref}})
    recipe(name, {'type':'minecraft:crafting_shaped', 'pattern':['SGS','G G','SGS'], 'key': {'S':ji('sky_stone_block'), 'G':ji('quartz_glass')}, 'result': {'id':ji(name)}})
    dump(res / f'data/jugcraft/loot_table/blocks/jimta_{name}.json', {'type':'minecraft:block', 'pools':[{'rolls':1, 'condition':{'type':'minecraft:survives_explosion'}, 'entries':[{'type':'minecraft:item','name':ji(name),'modifier':{'type':'minecraft:copy_components','source':'block_entity','include':['jugcraft:stored_fluid']}}]}]})
    faces = [('up', 0, 0), ('down', 180, 0), ('north', 90, 0), ('south', 90, 180), ('west', 90, 270), ('east', 90, 90)]
    for name, src in [('crank', 'block/crank_base'), ('light_detector', 'item/quartz_fixture'), ('crystal_resonance_generator', 'block/crystal_resonance_generator')]:
        ref = model(src)
        variants = {f'facing={face}': {'model':ref, 'x':x, 'y':y} for face,x,y in faces} if name != 'crystal_resonance_generator' else {'':{'model':ref}}
        dump(assets/f'blockstates/jimta_{name}.json', {'variants': variants})
        item = {'type':'minecraft:model', 'model':ref}
        if name == 'crank': item = {'type':'minecraft:composite', 'models':[item, {'type':'minecraft:model', 'model':model('block/crank_handle')}]}
        dump(assets/f'items/jimta_{name}.json', {'model':item})
    import json
    detector = json.loads((source/'blockstates/light_detector.json').read_text(encoding='utf-8'))
    for definition in detector['variants'].values(): definition['model'] = model(definition['model'].split(':', 1)[1])
    dump(assets/'blockstates/jimta_light_detector.json', detector)
    dump(assets/'items/jimtafacts/ae2/crank_handle.json', {'model':{'type':'minecraft:model','model':model('block/crank_handle')}})
    for name, parts in [('crank', ['minecraft:stick','minecraft:stick','minecraft:cobblestone']), ('light_detector',[ji('quartz_fixture'),'minecraft:redstone','minecraft:quartz']), ('crystal_resonance_generator',[ji('sky_stone_block'),ji('charged_certus_quartz_crystal'),ji('fluix_crystal'),'minecraft:copper_ingot'])]:
        recipe(name, {'type':'minecraft:crafting_shapeless','ingredients':parts,'result':{'id':ji(name)}})
