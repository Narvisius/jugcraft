"""Original spatial-cell/port models and recovered SpatialPylonModel cube/overlay geometry."""
BLOCKS = ['spatial_io_port', 'spatial_pylon']
ITEMS = [f'spatial_{kind}_{size}' for size in (2, 16, 128) for kind in ('cell_component', 'storage_cell')]

def generate(source, assets, res, model, texture, dump, recipe):
    variants = {f'facing={direction}': {'model': model('block/spatial_io_port'), 'y': angle}
                for direction, angle in [('north', 0), ('east', 90), ('south', 180), ('west', 270)]}
    dump(assets/'blockstates/jimta_spatial_io_port.json', {'variants': variants})
    dump(assets/'items/jimta_spatial_io_port.json', {'model': {'type': 'minecraft:model', 'model': model('block/spatial_io_port')}})
    # The source dynamic renderer uses full cubes, with a transparent DIM overlay.
    # These isolated corner pylons use its NONE-axis appearance, not invented geometry.
    refs = {name: texture('ae2:block/spatial_pylon/' + name) for name in ('base', 'dim')}
    for lit in (False, True):
        elements = [{'from': [0, 0, 0], 'to': [16, 16, 16],
                     'light_emission': 15 if lit and layer == 'dim' else 0,
                     'faces': {face: {'texture': '#' + layer} for face in ('north', 'south', 'east', 'west', 'up', 'down')}}
                    for layer in ('base', 'dim')]
        dump(assets/f'models/block/jimtafacts/ae2/spatial_pylon_{str(lit).lower()}.json',
             {'parent': 'minecraft:block/block', 'textures': dict(refs, particle=refs['base']), 'elements': elements})
    dump(assets/'blockstates/jimta_spatial_pylon.json', {'variants': {
        'lit=' + str(lit).lower(): {'model': 'jugcraft:block/jimtafacts/ae2/spatial_pylon_' + str(lit).lower()} for lit in (False, True)}})
    dump(assets/'items/jimta_spatial_pylon.json', {'model': {'type': 'minecraft:model', 'model': model('item/spatial_pylon')}})
    for name in ITEMS:
        dump(assets/f'items/jimta_{name}.json', {'model': {'type': 'minecraft:model', 'model': model('item/' + name)}})
    def shaped(name, pattern, keys, count=1):
        recipe(name, {'type': 'minecraft:crafting_shaped', 'pattern': pattern, 'key': keys, 'result': {'id': 'jugcraft:jimta_' + name, 'count': count}})
    shaped('spatial_pylon', ['QFQ', 'QEQ', 'QFQ'], {'Q': 'jugcraft:jimta_quartz_glass', 'F': 'jugcraft:jimta_fluix_crystal', 'E': 'jugcraft:jimta_engineering_processor'}, 4)
    shaped('spatial_io_port', ['GGG', 'IPI', 'ICI'], {'G': 'jugcraft:jimta_quartz_glass', 'I': 'minecraft:iron_ingot', 'P': 'jugcraft:jimta_io_port', 'C': 'jugcraft:jimta_engineering_processor'})
    for size in (2, 16, 128):
        shaped(f'spatial_cell_component_{size}', ['FQF', 'QCQ', 'FEF'], {'F': 'jugcraft:jimta_fluix_pearl', 'Q': 'jugcraft:jimta_quartz_glass', 'E': 'jugcraft:jimta_engineering_processor', 'C': 'minecraft:ender_pearl' if size == 2 else 'jugcraft:jimta_spatial_cell_component_'+('2' if size==16 else '16')})
        shaped(f'spatial_storage_cell_{size}', ['IGI', 'RCR', 'III'], {'I': 'minecraft:iron_ingot', 'G': 'jugcraft:jimta_quartz_glass', 'R': 'minecraft:redstone', 'C': f'jugcraft:jimta_spatial_cell_component_{size}'})
