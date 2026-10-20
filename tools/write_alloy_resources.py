"""Author package 2 resources through the existing generators, without deleting trees.

Not a build or test runner. Normal full generators also include this package.
On Windows set JUGCRAFT_FONT_DIR to the installed DejaVu TTF directory.
"""
import json

import generate_material_data as data
import generate_textures as textures
import material_icons
import structural_alloys as alloys
import structural_alloys_art
import write_ceramics_resources
from materials import METALS, MINERALS


def main():
    assets, dest = data.ASSETS, data.DATA / 'jugcraft'
    blocks = [metal + '_block' for metal in alloys.METALS] + ['chromite_ore', 'deepslate_chromite_ore', 'chromite_block']
    items = [metal + suffix for metal in alloys.METALS for suffix in ('_ingot', '_nugget')]
    items += ['chromite', 'stainless_steel_plate', 'aluminum_magnesium_plate']
    lang_path = assets / 'lang/en_us.json'
    lang = json.loads(lang_path.read_text(encoding='utf-8'))
    for block in blocks:
        data.write(assets / 'blockstates' / (block + '.json'), {'variants': {'': {'model': data.rid('block/' + block)}}})
        data.write(assets / 'models/block' / (block + '.json'), material_icons.block_model(block)
                   or {'parent': 'minecraft:block/cube_all', 'textures': {'all': data.rid('block/' + block)}})
        data.write(assets / 'items' / (block + '.json'), {'model': {'type': 'minecraft:model', 'model': data.rid('block/' + block)}})
        lang['block.jugcraft.' + block] = data.block_name(block)
        loot = data.ore_drop(block, 'chromite', 1, 1) if block.endswith('_ore') else data.self_drop(block)
        data.write(dest / 'loot_table/blocks' / (block + '.json'), loot)
    for item in items:
        data.write(assets / 'models/item' / (item + '.json'), {'parent': 'minecraft:item/generated', 'textures': {'layer0': data.rid('item/' + item)}})
        data.write(assets / 'items' / (item + '.json'), {'model': {'type': 'minecraft:model', 'model': data.rid('item/' + item)}})
        lang['item.jugcraft.' + item] = data.item_name(item)
    data.write(lang_path, dict(sorted(lang.items())))
    out = dest / 'recipe'
    for metal in alloys.METALS:
        feature = METALS[metal]['feature']
        data.compaction(out, feature, '#c:ingots/' + metal, metal + '_ingot', metal + '_block')
        data.write(out / (metal + '_ingot_from_nuggets.json'), data.shaped(feature, ['###'] * 3,
                   {'#': '#c:nuggets/' + metal}, metal + '_ingot'))
        data.write(out / (metal + '_nugget.json'), data.shapeless(feature, ['#c:ingots/' + metal], metal + '_nugget', 9))
    data.compaction(out, 'machines', 'jugcraft:chromite', 'chromite', 'chromite_block')
    data.tags()
    gen = MINERALS['chromite']['gen']
    data.write(dest / 'worldgen/feature/ore_chromite.json', {
        'type': data.rid('surface_linked_ore'),
        'ore': {'size': gen['size'], 'discard_chance_on_air_exposure': 0.0,
                'targets': data.layered_targets('chromite_ore', 'deepslate_chromite_ore')},
        'surface_biomes': data.rid('has_surface_ore/chromite'),
        'outcrop': data.rid('chromite_ore'), 'outcrop_chance': gen['outcrop_chance']})
    data.placed_feature('chromite', gen)
    # Reuses the shared form recipe/guide/JEI writer, including both local packages.
    write_ceramics_resources.main()
    for (kind, name), image in material_icons.textures().items():
        if name in blocks or name in items:
            textures.save(image, kind, name)
    for metal in ('stainless_steel', 'aluminum_magnesium'):
        textures.save(textures.ms.plate(textures.part_palette(metal)), 'item', metal + '_plate')
    structural_alloys_art.draw_all(textures.save)


if __name__ == '__main__':
    main()
