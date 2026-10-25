"""Native computer resources using the supplied model hierarchy and unchanged textures."""
from pathlib import Path
import hashlib
import json
import shutil

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'art/owner-library/originals/Oct23/Jimtafacts/computers/assets/computercraft'
RES = ROOT / 'src/main/resources'
DEVICES = ('computer_normal', 'computer_advanced', 'computer_command', 'disk_drive', 'monitor_normal', 'monitor_advanced',
           'printer', 'redstone_relay', 'speaker', 'turtle_normal', 'turtle_advanced', 'wired_modem', 'wired_modem_full',
           'wireless_modem_normal', 'wireless_modem_advanced')
ITEMS = ('disk', 'treasure_disk', 'pocket_computer_normal', 'pocket_computer_advanced', 'printed_page', 'printed_pages', 'printed_book')


def blocks(): return ['jimta_computer_' + n for n in (*DEVICES, 'cable')]
def items(): return ['jimta_' + n for n in ITEMS]
def dump(path, data):
    if 'recipe' in path.parts and isinstance(data,dict) and 'type' in data:
        data.setdefault('fabric:load_conditions',[{'condition':'jugcraft:feature_enabled','feature':'machines'}])
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


def remap(value):
    if isinstance(value, str): return value.replace('computercraft:', 'jugcraft:jimtafacts/computers/')
    if isinstance(value, list): return [remap(x) for x in value]
    if isinstance(value, dict): return {k: remap(v) for k, v in value.items() if k != 'render_type'}
    return value


def model(name, lit=False):
    if name.startswith('computer_'): return name + ('_on' if lit else '_off')
    if name == 'disk_drive': return 'disk_drive_full' if lit else 'disk_drive_empty'
    if name.startswith('monitor_'): return name
    if name == 'printer': return 'printer_bottom_full' if lit else 'printer_empty'
    if 'modem' in name: return name + ('_on' if lit else '_off')
    return name


def apply():
    copies = []
    for source in sorted((SOURCE / 'textures').rglob('*')):
        if not source.is_file() or source.suffix not in ('.png', '.mcmeta'): continue
        destination = RES / 'assets/jugcraft/textures/jimtafacts/computers' / source.relative_to(SOURCE / 'textures')
        destination.parent.mkdir(parents=True, exist_ok=True); shutil.copyfile(source, destination)
        copies.append({'source': source.relative_to(ROOT).as_posix(), 'runtime': destination.relative_to(ROOT).as_posix(),
                       'sha256': hashlib.sha256(source.read_bytes()).hexdigest(), 'changes': 'None; original pixels/sidecar.'})
    # Native GUI sprite loader needs its nine-slice sprite under textures/gui/sprites.
    for suffix in ('.png', '.png.mcmeta'):
        source = SOURCE / ('textures/gui/border_normal' + suffix)
        dest = RES / ('assets/jugcraft/textures/gui/sprites/jimtafacts/computer_border' + suffix)
        dest.parent.mkdir(parents=True, exist_ok=True); shutil.copyfile(source, dest)
        copies.append({'source': source.relative_to(ROOT).as_posix(), 'runtime': dest.relative_to(ROOT).as_posix(),
                       'sha256': hashlib.sha256(source.read_bytes()).hexdigest(), 'changes': 'Relocated for native nine-slice GUI sprite loading; bytes unchanged.'})
    for source in sorted((SOURCE / 'models').rglob('*.json')):
        destination = RES / 'assets/jugcraft/models/jimtafacts/computers' / source.relative_to(SOURCE / 'models')
        dump(destination, remap(json.loads(source.read_text(encoding='utf-8'))))
        copies.append({'source': source.relative_to(ROOT).as_posix(), 'runtime': destination.relative_to(ROOT).as_posix(),
                       'sha256': hashlib.sha256(source.read_bytes()).hexdigest(), 'changes': 'Namespace/model references remapped; source render_type removed. Geometry and UVs unchanged.'})
    for name in DEVICES:
        variants = {}
        for face, rotation in [('north', 0), ('east', 90), ('south', 180), ('west', 270)]:
            for lit in (False, True):
                if name.startswith('monitor_'):
                    for mask in range(16):
                        suffix = ''.join(letter for bit, letter in enumerate('lrud') if mask & (1 << bit))
                        variants[f'facing={face},lit={str(lit).lower()},connections={mask}'] = {'model': 'jugcraft:jimtafacts/computers/block/' + name + ('_' + suffix if suffix else ''), 'y': rotation}
                else:
                    variants[f'facing={face},lit={str(lit).lower()}'] = {'model': 'jugcraft:jimtafacts/computers/block/' + model(name, lit), 'y': rotation}
        dump(RES / f'assets/jugcraft/blockstates/jimta_computer_{name}.json', {'variants': variants})
        dump(RES / f'assets/jugcraft/items/jimta_computer_{name}.json', {'model': {'type': 'minecraft:model', 'model': 'jugcraft:jimtafacts/computers/block/' + model(name)}})
    parts = [{'apply': {'model': 'jugcraft:jimtafacts/computers/block/cable_core_any'}}]
    for side, rotate in [('south', {}), ('north', {'y': 180}), ('east', {'y': 270}), ('west', {'y': 90}), ('up', {'x': 90}), ('down', {'x': 270})]:
        parts.append({'when': {side: 'true'}, 'apply': {'model': 'jugcraft:jimtafacts/computers/block/cable_arm', **rotate}})
    dump(RES / 'assets/jugcraft/blockstates/jimta_computer_cable.json', {'multipart': parts})
    dump(RES / 'assets/jugcraft/items/jimta_computer_cable.json', {'model': {'type': 'minecraft:model', 'model': 'jugcraft:jimtafacts/computers/item/cable'}})
    for name in ITEMS:
        dump(RES / f'assets/jugcraft/items/jimta_{name}.json', {'model': {'type': 'minecraft:model',
             'model': 'jugcraft:jimtafacts/computers/item/' + name,
             'tints': [{'type': 'minecraft:constant', 'value': -1}, {'type': 'minecraft:constant', 'value': -1}, {'type': 'minecraft:constant', 'value': -15658735}]}})
    for part in ('crafting_table', 'speaker', 'modem_normal_off', 'modem_normal_on', 'modem_advanced_off', 'modem_advanced_on'):
        for side in ('left', 'right'):
            dump(RES / f'assets/jugcraft/items/jimtafacts/turtle_{part}_{side}.json', {'model': {'type': 'minecraft:model',
                 'model': f'jugcraft:jimtafacts/computers/block/turtle_{part}_{side}'}})
    language = json.loads((SOURCE / 'lang/en_us.json').read_text(encoding='utf-8'))
    target = RES / 'assets/jugcraft/lang/en_us.json'; lang = json.loads(target.read_text(encoding='utf-8'))
    for name in (*DEVICES, 'cable'): lang['block.jugcraft.jimta_computer_' + name] = language['block.computercraft.' + name]
    for name in ITEMS: lang['item.jugcraft.jimta_' + name] = language['item.computercraft.' + name]
    lang['item.jugcraft.jimta_treasure_disk'] = 'Program Library Disk'
    lang['block.jugcraft.jimta_computer_wireless_modem_advanced'] = 'Advanced Wireless Modem'
    dump(target, lang)
    for name in blocks():
        dump(RES / f'data/jugcraft/loot_table/blocks/{name}.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1,
             'entries': [{'type': 'minecraft:item', 'name': 'jugcraft:' + name}], 'condition': {'type': 'minecraft:survives_explosion'}}]})
    # Existing factory components provide the entry path; no parallel ore/energy currency.
    recipes = {
        'computer_normal': ['jugcraft:logic_controller', 'minecraft:glass_pane', 'minecraft:redstone'],
        'computer_advanced': ['jugcraft:jimta_computer_computer_normal', 'minecraft:gold_ingot', 'minecraft:glowstone_dust'],
        'disk_drive': ['minecraft:stone', 'minecraft:iron_ingot', 'minecraft:redstone'],
        'monitor_normal': ['minecraft:glass_pane', 'minecraft:iron_ingot', 'minecraft:redstone'],
        'monitor_advanced': ['jugcraft:jimta_computer_monitor_normal', 'minecraft:gold_ingot'],
        'printer': ['minecraft:iron_ingot', 'minecraft:dropper', 'minecraft:redstone'],
        'redstone_relay': ['jugcraft:relay', 'minecraft:redstone'],
        'speaker': ['minecraft:note_block', 'minecraft:redstone'],
        'turtle_normal': ['jugcraft:jimta_computer_computer_normal', 'minecraft:chest', 'minecraft:iron_block'],
        'turtle_advanced': ['jugcraft:jimta_computer_turtle_normal', 'minecraft:gold_block'],
        'wired_modem': ['jugcraft:data_cable', 'minecraft:redstone', 'minecraft:stone'],
        'wired_modem_full': ['jugcraft:jimta_computer_wired_modem', 'minecraft:stone'],
        'wireless_modem_normal': ['minecraft:ender_pearl', 'minecraft:redstone', 'minecraft:stone'],
        'wireless_modem_advanced': ['jugcraft:jimta_computer_wireless_modem_normal', 'minecraft:ender_eye', 'minecraft:gold_ingot'],
        'cable': ['jugcraft:data_cable'],
    }
    for name, ingredients in recipes.items():
        dump(RES / f'data/jugcraft/recipe/jimta_computer_{name}.json', {'type': 'minecraft:crafting_shapeless', 'category': 'redstone',
             'ingredients': ingredients, 'result': {'id': 'jugcraft:jimta_computer_' + name}})
    for name, ingredients in {
        'disk': ['minecraft:paper', 'minecraft:redstone', 'minecraft:iron_nugget'],
        'treasure_disk': ['jugcraft:jimta_disk', 'minecraft:book'],
        'pocket_computer_normal': ['jugcraft:jimta_computer_computer_normal', 'minecraft:stone_button'],
        'pocket_computer_advanced': ['jugcraft:jimta_pocket_computer_normal', 'minecraft:gold_ingot'],
    }.items():
        dump(RES / f'data/jugcraft/recipe/jimta_{name}.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
             'ingredients': ingredients, 'result': {'id': 'jugcraft:jimta_' + name}})
    tag = RES / 'data/minecraft/tags/block/mineable/pickaxe.json'; data = json.loads(tag.read_text(encoding='utf-8')) if tag.exists() else {'replace': False, 'values': []}
    data['values'] = sorted(set(data['values']) | {'jugcraft:' + name for name in blocks()}); dump(tag, data)
    atlas = RES / 'assets/minecraft/atlases/blocks.json'
    data = json.loads(atlas.read_text(encoding='utf-8')) if atlas.exists() else {'sources': []}
    for folder in ('block', 'item', 'entity'):
        source = {'type': 'minecraft:directory', 'source': 'jimtafacts/computers/' + folder, 'prefix': 'jimtafacts/computers/' + folder + '/'}
        if source not in data['sources']: data['sources'].append(source)
    dump(atlas, data)
    dump(ROOT / 'art/owner-library/catalog/edited-2026-10-23-jimtafacts/computer-copies.json', copies)


if __name__ == '__main__': apply()
