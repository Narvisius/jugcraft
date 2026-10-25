"""Copy supplied charging/programming table meshes, sprites and GUIs without executing source code."""
from pathlib import Path
import hashlib
import json
import shutil

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
SOURCE = ROOT / 'art/owner-library/originals/Oct23/Jimtafacts/buildcraft reborn by jimbo/assets'

def blocks():
    return {'jimta_charging_table', 'jimta_programming_table'}

def items():
    return blocks() | {'jimta_blank_robot_board'}

def dump(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')

def generate(shared=True):
    manifest = []
    lang = {}
    def copy(source, target):
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, target)
        manifest.append({'source': source.relative_to(ROOT).as_posix(), 'runtime': target.relative_to(ROOT).as_posix(),
                         'sha256': hashlib.sha256(source.read_bytes()).hexdigest()})
    for kind in ('charging', 'programming'):
        name = 'jimta_' + kind + '_table'
        original = SOURCE / f'buildcraftsilicon/models/block/table/{kind}.json'
        model = json.loads(original.read_text(encoding='utf-8'))
        model['parent'] = 'minecraft:block/block'
        for field, reference in model['textures'].items():
            if reference.startswith('#'):
                continue
            if ':' not in reference:
                namespace, path = 'minecraft', reference
            else:
                namespace, path = reference.split(':', 1)
            if namespace == 'minecraft':
                # Legacy vanilla glass reference; no supplied pixel data is replaced.
                model['textures'][field] = 'minecraft:block/white_stained_glass' if path == 'blocks/glass_white' else reference
                continue
            target = RES / f'assets/jugcraft/textures/block/jimtafacts/tables/{namespace}/{path}.png'
            copy(SOURCE / namespace / f'textures/{path}.png', target)
            sidecar = (SOURCE / namespace / f'textures/{path}.png.mcmeta')
            if sidecar.exists():
                copy(sidecar, Path(str(target) + '.mcmeta'))
            model['textures'][field] = f'jugcraft:block/jimtafacts/tables/{namespace}/{path}'
        target = RES / f'assets/jugcraft/models/block/{name}.json'
        from jimtafacts_model_repairs import repair
        dump(target, repair('block/'+name, model))
        manifest.append({'source': original.relative_to(ROOT).as_posix(), 'runtime': target.relative_to(ROOT).as_posix(),
                         'adaptation': 'Original elements retained; namespaced runtime textures and modern vanilla white glass. Programming-table overlapping surface separated by 0.1 pixel with pinned UVs.'})
        dump(RES / f'assets/jugcraft/blockstates/{name}.json', {'variants': {'': {'model': 'jugcraft:block/' + name}}})
        dump(RES / f'assets/jugcraft/items/{name}.json', {'model': {'type': 'minecraft:model', 'model': 'jugcraft:block/' + name}})
        dump(RES / f'data/jugcraft/loot_table/blocks/{name}.json', {'type': 'minecraft:block', 'pools': [
            {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'jugcraft:' + name}],
             'condition': {'type': 'minecraft:survives_explosion'}}]})
        copy(SOURCE / f'buildcraftsilicon/textures/gui/{kind}_table.png', RES / f'assets/jugcraft/textures/gui/jimtafacts/{kind}_table.png')
        dump(RES / f'data/jugcraft/recipe/jimtafacts_tables/{kind}.json', {
            'type': 'minecraft:crafting_shaped', 'category': 'redstone', 'pattern': ['ICI', 'RMR', 'I I'],
            'key': {'I': 'jugcraft:steel_plate', 'C': 'jugcraft:advanced_circuit' if kind == 'programming' else 'jugcraft:basic_circuit',
                    'R': 'jugcraft:copper_wire', 'M': 'jugcraft:machine_casing'}, 'result': {'id': 'jugcraft:' + name}})
        lang['block.jugcraft.' + name] = kind.title() + ' Table'
    name = 'jimta_blank_robot_board'
    copy(SOURCE / 'buildcraftrobotics/textures/items/board/clean.png', RES / 'assets/jugcraft/textures/item/jimtafacts/tables/blank_robot_board.png')
    dump(RES / f'assets/jugcraft/models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'jugcraft:item/jimtafacts/tables/blank_robot_board'}})
    dump(RES / f'assets/jugcraft/items/{name}.json', {'model': {'type': 'minecraft:model', 'model': 'jugcraft:item/' + name}})
    dump(RES / 'data/jugcraft/recipe/jimtafacts_tables/blank_board.json', {'type': 'minecraft:crafting_shapeless', 'category': 'redstone',
        'ingredients': ['minecraft:paper', 'minecraft:redstone'], 'result': {'id': 'jugcraft:' + name}})
    lang['item.jugcraft.' + name] = 'Blank Robot Program Card'
    if shared:
        path = RES / 'assets/jugcraft/lang/en_us.json'
        value = json.loads(path.read_text(encoding='utf-8')); value.update(lang); dump(path, value)
        for tag in ('mineable/pickaxe', 'needs_stone_tool'):
            path = RES / f'data/minecraft/tags/block/{tag}.json'
            value = json.loads(path.read_text(encoding='utf-8'))
            for name in sorted(blocks()):
                if 'jugcraft:' + name not in value['values']: value['values'].append('jugcraft:' + name)
            dump(path, value)
    dump(ROOT / 'art/owner-library/catalog/edited-2026-10-23-jimtafacts/table-runtime-copies.json', manifest)
    return items()

if __name__ == '__main__':
    print('Generated workshop table resources:', len(generate()))
