"""Original anchor and facade frame meshes with a native material-aware facade renderer."""
ITEMS = ['facade', 'cable_anchor']


def generate(source, assets, resources, model, texture, dump, recipe):
    anchor = model('part/cable_anchor')
    dump(assets / 'items/jimta_cable_anchor.json', {'model': {'type': 'minecraft:model', 'model': anchor}})
    frame = model('item/facade_base')
    dump(assets / 'items/jimta_facade.json', {'model': {'type': 'minecraft:composite', 'models': [
        {'type': 'minecraft:model', 'model': frame},
        {'type': 'minecraft:special', 'base': frame, 'model': {'type': 'jugcraft:jimta_facade'}}]}})
    recipe('facade', {'type': 'jugcraft:jimta_facade'})
    recipe('cable_anchors', {'type': 'jugcraft:jimta_cable_anchors'})
