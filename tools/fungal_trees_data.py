"""Batch 9 shared fungal blocks and bounded feature data (no bone-meal-grown marrow)."""
from agriculture import FUNGAL_BLOCKS
from trees_data import rid, SHEARS_OR_SILK


def assets(root, write, lang):
    for block, title in zip(FUNGAL_BLOCKS, ('Huge Glowcap', 'Blight Wart Block', 'Bramble Bush Leaves')):
        lang[f'block.jugcraft.{block}'] = title
        model = {'parent': 'minecraft:block/cube_all', 'textures': {'all': rid('block/' + block)}}
        if block == 'glowcap_block':
            # Native six-face mushroom state: cap exterior and pale pores on exposed cut faces.
            parts=[]
            for side, rotation in {'north': {}, 'east': {'y': 90}, 'south': {'y': 180}, 'west': {'y': 270}, 'up': {'x': 270}, 'down': {'x': 90}}.items():
                for value, face in (('true', 'outside'), ('false', 'inside')):
                    parts.append({'when': {side: value}, 'apply': {'model': rid('block/glowcap_' + face), **rotation}})
            for face, texture in (('outside','glowcap_block'), ('inside','glowcap_pores')):
                write(root/'models'/'block'/f'glowcap_{face}.json', {'parent':'minecraft:block/block',
                    'textures': {'particle':rid('block/'+texture), 'face':rid('block/'+texture)},
                    'elements':[{'from':[0,0,0], 'to':[16,16,16], 'faces':{'north':{'texture':'#face','cullface':'north'}}}]})
            write(root/'blockstates'/f'{block}.json', {'multipart':parts})
        else:
            write(root/'blockstates'/f'{block}.json', {'variants':{'':{'model':rid('block/'+block)}}})
        if block == 'bramble_bush_leaves':
            # Preserve the owner's grayscale original; dark red is applied at runtime, like pine needles.
            model={'parent':'minecraft:block/block', 'textures':{'all':rid('block/'+block), 'particle':rid('block/'+block)},
                   'elements':[{'from':[0,0,0],'to':[16,16,16],'faces':{side:{'texture':'#all','cullface':side,'tintindex':0}
                    for side in ('up','down','north','south','east','west')}}]}
        write(root/'models'/'block'/f'{block}.json', model)
        item={'type':'minecraft:model','model':rid('block/'+block)}
        if block=='bramble_bush_leaves': item['tints']=[{'type':'minecraft:constant','value':0x9b545e}]
        write(root/'items'/f'{block}.json', {'model':item})
    # Same shared tint registry as the pine, extended instead of replacing it.
    from agriculture import TREES
    write(root/'tree_leaf_tints.json', {**{i['leaves']:i['leaf_tint'] for i in TREES.values() if 'leaf_tint' in i},
                                      'bramble_bush_leaves':0x9b545e})


def loot(out, write):
    for block, seed, chance in (('glowcap_block','glowcap',.03),('blight_wart_block','blight_fungus',.05),('bramble_bush_leaves','bramble',.06)):
        tool=SHEARS_OR_SILK if block=='bramble_bush_leaves' else 'minecraft:tool/can_silk_touch'
        pools=[{'rolls':1,'entries':[{'type':'minecraft:alternatives','children':[
            {'type':'minecraft:item','name':rid(block),'condition':tool},
            {'type':'minecraft:item','name':rid(seed),'condition':{'type':'minecraft:all_of','terms':[
                {'type':'minecraft:random_chance','chance':chance},{'type':'minecraft:survives_explosion'}]}}]}]}]
        if block=='bramble_bush_leaves': pools.append({'rolls':1,'condition':{'type':'minecraft:inverted','term':tool},
            'entries':[{'type':'minecraft:item','name':'minecraft:stick','condition':{'type':'minecraft:random_chance','chance':.1},
                        'modifier':[{'type':'minecraft:explosion_decay'}]}]})
        write(out/f'{block}.json', {'type':'minecraft:block','pools':pools,'random_sequence':rid('blocks/'+block)})


def tags(tags):
    for registry in ('block','item'): tags.add(registry,'minecraft:leaves',rid('bramble_bush_leaves'))
    tags.add('block','minecraft:mineable/axe',rid('glowcap_block'))
    for block in ('blight_wart_block','bramble_bush_leaves'): tags.add('block','minecraft:mineable/hoe',rid(block))


def worldgen(data, write):
    folder=data/'jugcraft'/'worldgen'
    for shape in ('glowcap_dome','glowcap_parasol','blight','marrow','bramble'):
        write(folder/'feature'/f'huge_{shape}.json', {'type':'jugcraft:fungal_growth','shape':shape})
        write(folder/'placed_feature'/f'huge_{shape}.json', {'feature':rid('huge_'+shape),'placement':[]})
    write(folder/'feature'/'huge_glowcap.json', {'type':'minecraft:random_boolean_selector',
        'feature_true':rid('huge_glowcap_dome'),'feature_false':rid('huge_glowcap_parasol')})
    write(folder/'placed_feature'/'huge_glowcap.json', {'feature':rid('huge_glowcap'),'placement':[]})
