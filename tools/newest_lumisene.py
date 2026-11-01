"""Native finite fuel uses the supplied bottle, bucket, fluid and fire artwork."""
from newest_building import ROOT, SOURCE, RES, read, write, map_value, copy
from newest_twigs import recipe


def main():
    source = SOURCE / 'SUPPS'
    assets = source / 'assets/supplementaries'
    out = RES / 'assets/jugcraft'
    write(out / 'blockstates/supps_lumisene.json',
          map_value(read(assets / 'blockstates/lumisene.json'), 'supplementaries', 'supps', True))
    for name in ['lumisene_bottle', 'lumisene_bucket']:
        write(out / 'items' / ('supps_' + name + '.json'),
              {'model': {'type': 'minecraft:model', 'model': 'jugcraft:newest/supps/item/' + name}})
        data = map_value(read(source / 'data/supplementaries/recipe' / (name + '.json')), 'supplementaries', 'supps')
        data.pop('fabric:load_conditions', None)
        data.pop('neoforge:conditions', None)
        write(RES / 'data/jugcraft/recipe' / ('supps_' + name + '.json'), recipe(data))
    copy(assets / 'textures/mob_effect/flammable.png', out / 'textures/mob_effect/supps_flammable.png')
    lang = read(out / 'lang/en_us.json')
    lang.update({'item.jugcraft.supps_lumisene_bottle': 'Lumisene Bottle',
                 'item.jugcraft.supps_lumisene_bucket': 'Lumisene Bucket',
                 'block.jugcraft.supps_lumisene': 'Lumisene',
                 'effect.jugcraft.supps_flammable': 'Flammable'})
    write(out / 'lang/en_us.json', lang)
    atlas = RES / 'assets/minecraft/atlases/blocks.json'
    data = read(atlas)
    entry = {'type': 'minecraft:single', 'resource': 'jugcraft:newest/supps/block/lumisene/lumisene'}
    if entry not in data['sources']:
        data['sources'].append(entry)
    write(atlas, data)


if __name__ == '__main__':
    main()
