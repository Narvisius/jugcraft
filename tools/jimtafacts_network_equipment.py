"""Original AE handheld model/texture intake and native, reachable equipment recipes."""
import json

ITEMS = [f'{m}_{t}' for m in ('certus_quartz', 'nether_quartz', 'fluix') for t in ('axe', 'hoe', 'shovel', 'pickaxe', 'sword')]
ITEMS += [f'{m}_{t}' for m in ('certus_quartz', 'nether_quartz') for t in ('wrench', 'cutting_knife')]
ITEMS += ['name_press', 'fluix_upgrade_smithing_template', 'memory_card', 'network_tool', 'charged_staff', 'entropy_manipulator', 'color_applicator', 'matter_cannon', 'matter_ball', 'guide']

# Original AEColor dark / medium / bright variants, recovered statically from the supplied class.
COLORS = {'white': (0xB4B4B4, 0xE0E0E0, 0xF9F9F9), 'light_gray': (0x7E7E7E, 10526624, 0xC4C4C4), 'gray': (0x4F4F4F, 0x6C6B6C, 0x949294),
          'black': (0x131313, 0x272727, 0x3B3B3B), 'lime': (5161038, 7397977, 11794541), 'yellow': (16764736, 16769881, 16056192),
          'orange': (14252079, 15508028, 15907401), 'brown': (7227922, 8281110, 9334298), 'red': (11149611, 14106178, 15758949),
          'pink': (14184106, 0xFF99BB, 16501461), 'magenta': (12669321, 13988252, 15113919), 'purple': (7232696, 9526733, 11562973),
          'blue': (3375088, 3708159, 4243967), 'light_blue': (6928895, 7394047, 8452095), 'cyan': (2273454, 3132599, 6678729), 'green': (498539, 1554541, 3332176)}


def generate(source, assets, resources, model, texture, dump, recipe):
    def ji(n): return 'jugcraft:jimta_' + n
    def refs(value):
        if isinstance(value, dict): return {k: model(v[4:]) if k == 'model' and isinstance(v, str) and v.startswith('ae2:') else refs(v) for k, v in value.items()}
        if isinstance(value, list): return [refs(v) for v in value]
        return value
    for name in ITEMS:
        if name in ('color_applicator', 'memory_card'): continue
        raw = json.loads((source / f'items/{name}.json').read_text(encoding='utf-8'))
        dump(assets / f'items/jimta_{name}.json', refs(raw))
    # Source MemoryCardItemModel.buildColorQuads: eight1x1 faces at x8..12,y8..10,z7.5..8.5.
    # Native item tints replace its NeoForge-only runtime hash model cache.
    hash_texture = texture('ae2:item/memory_card_hash')
    elements = []
    for x in range(4):
        for y in range(2):
            for face,z in [('north',7.5),('south',8.5)]:
                elements.append({'from':[8+x,9-y,z],'to':[9+x,10-y,z],
                    'faces':{face:{'texture':'#hash','uv':[0,0,1,1],'tintindex':x*2+y+1}}})
    hash_ref = 'jugcraft:item/jimtafacts/ae2/memory_card_identity'
    dump(assets / 'models/item/jimtafacts/ae2/memory_card_identity.json', {'parent': 'minecraft:item/generated', 'textures': {'hash': hash_texture, 'particle': hash_texture}, 'elements': elements})
    memory_tints = [{'type': 'minecraft:custom_model_data', 'index': i, 'default': 9526733} for i in range(9)]
    dump(assets / 'items/jimta_memory_card.json', {'model': {'type': 'minecraft:composite', 'models': [
        {'type': 'minecraft:model', 'model': model('item/memory_card_base'), 'tints': [{'type': 'minecraft:constant', 'value': -1}, memory_tints[0]]},
        {'type': 'minecraft:model', 'model': hash_ref, 'tints': memory_tints}]}})
    colored_ref = model('item/color_applicator_colored')
    colored_path = assets / 'models/jimtafacts/ae2/item/color_applicator_colored.json'
    colored = json.loads(colored_path.read_text(encoding='utf-8'))
    original = json.loads((source / 'models/item/color_applicator_colored.json').read_text(encoding='utf-8'))
    for element, src in zip(colored['elements'], original['elements']):
        for face, data in src.get('faces', {}).items():
            if 'tintindex' in data: element['faces'][face]['tintindex'] = data['tintindex']
    dump(colored_path, colored)
    tints = [{'type': 'minecraft:constant', 'value': -1}] + [{'type': 'minecraft:custom_model_data', 'index': i, 'default': -1} for i in range(3)]
    dump(assets / 'items/jimta_color_applicator.json', {'model': {'type': 'minecraft:condition', 'property': 'minecraft:custom_model_data', 'index': 0,
         'on_true': {'type': 'minecraft:model', 'model': colored_ref, 'tints': tints}, 'on_false': {'type': 'minecraft:model', 'model': model('item/color_applicator')}}})
    texture('ae2:guis/quartzknife')
    for material in ('certus_quartz', 'nether_quartz', 'fluix'):
        crystal = 'minecraft:quartz' if material == 'nether_quartz' else ji(material + '_crystal')
        dump(resources / f'data/jugcraft/tags/item/jimta_{material}_tool_material.json', {'replace': False, 'values': [crystal]})
        for tool, pattern in [('axe', ['CC', 'CS', ' S']), ('hoe', ['CC', ' S', ' S']), ('shovel', ['C', 'S', 'S']), ('pickaxe', ['CCC', ' S ', ' S ']), ('sword', ['C', 'C', 'S'])]:
            if material == 'fluix':
                for base in ('certus_quartz', 'nether_quartz'):
                    recipe(f'fluix_{tool}_from_{base}', {'type': 'minecraft:smithing_transform', 'template': ji('fluix_upgrade_smithing_template'), 'base': ji(base + '_' + tool), 'addition': crystal, 'result': {'id': ji('fluix_' + tool)}})
            else: recipe(f'{material}_{tool}', {'type': 'minecraft:crafting_shaped', 'pattern': pattern, 'key': {'C': crystal, 'S': 'minecraft:stick'}, 'result': {'id': ji(material + '_' + tool)}})
        if material != 'fluix':
            for tool, pattern in [('wrench', ['C C', ' C ', ' C ']), ('cutting_knife', [' C', 'S '])]: recipe(f'{material}_{tool}', {'type': 'minecraft:crafting_shaped', 'pattern': pattern, 'key': {'C': crystal, **({'S': 'minecraft:stick'} if tool == 'cutting_knife' else {})}, 'result': {'id': ji(material + '_' + tool)}})
    shaped = {
        'fluix_upgrade_smithing_template': (['FCF', 'CDC', 'FCF'], {'F': ji('fluix_crystal'), 'C': ji('certus_quartz_crystal'), 'D': 'minecraft:diamond'}),
        'memory_card': (['IRI', 'GCG'], {'I': 'minecraft:iron_ingot', 'R': 'minecraft:redstone', 'G': 'minecraft:gold_ingot', 'C': ji('certus_quartz_crystal')}),
        'network_tool': ([' Q ', 'ICI', ' W '], {'Q': ji('certus_quartz_crystal'), 'I': 'minecraft:iron_ingot', 'C': ji('calculation_processor'), 'W': ji('certus_quartz_wrench')}),
        'charged_staff': ([' QC', ' IQ', 'I  '], {'Q': ji('charged_certus_quartz_crystal'), 'C': ji('energy_cell'), 'I': 'minecraft:iron_ingot'}),
        'entropy_manipulator': (['GCF', ' I ', ' I '], {'G': 'minecraft:glowstone_dust', 'C': ji('energy_cell'), 'F': ji('fluix_crystal'), 'I': 'minecraft:iron_ingot'}),
        'color_applicator': (['IFI', 'DCE', ' I '], {'I': 'minecraft:iron_ingot', 'F': ji('fluix_crystal'), 'D': 'minecraft:dispenser', 'C': ji('energy_cell'), 'E': ji('engineering_processor')}),
        'matter_cannon': (['IFE', 'DCI', ' I '], {'I': 'minecraft:iron_ingot', 'F': ji('fluix_crystal'), 'D': 'minecraft:dispenser', 'C': ji('energy_cell'), 'E': ji('engineering_processor')})}
    for name, (pattern, keys) in shaped.items(): recipe(name, {'type': 'minecraft:crafting_shaped', 'pattern': pattern, 'key': keys, 'result': {'id': ji(name)}})
    recipe('guide', {'type': 'minecraft:crafting_shapeless', 'ingredients': ['minecraft:book', ji('certus_quartz_crystal')], 'result': {'id': ji('guide')}})
    recipe('matter_ball', {'type': 'minecraft:crafting_shapeless', 'ingredients': ['minecraft:clay_ball', 'minecraft:cobblestone', ji('certus_quartz_dust')], 'result': {'id': ji('matter_ball'), 'count': 4}})
    # Data tables are read, never executed. Preserve all supplied atomic weights and tag selectors.
    ammo = []
    for path in sorted((source.parents[1] / 'data/ae2/recipe/matter_cannon/nuggets').glob('*.json')):
        data = json.loads(path.read_text(encoding='utf-8')); ammo.append((data['ammo'][1:], float(data['weight']), path.stem))
    root = resources.parents[2]
    java = root / 'src/main/java/io/github/jimbozoomer/jugcraft/jimtafacts/network/DigitalMatterAmmo.java'
    rows = ',\n'.join(f'        new Entry("{tag}", {weight}f, "{name}")' for tag, weight, name in ammo)
    colors = '\n'.join(f'            case {name.upper()} -> new int[]{{{a}, {b}, {c}}};' for name, (a, b, c) in COLORS.items())
    java.write_text('''package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;

/** Generated source data from supplied AE matter_cannon recipes and AEColor, not executable source code. */
public final class DigitalMatterAmmo {
    private DigitalMatterAmmo() {}
    private record Entry(String tag, float weight, String metal) {}
    private static final Entry[] WEIGHTS = {
''' + rows + '''
    };
    public static float weight(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        for (Entry e : WEIGHTS) if (stack.is(TagKey.create(Registries.ITEM, Identifier.parse(e.tag))) || path.equals(e.metal + "_nugget")) return e.weight;
        return 0;
    }
    public static java.util.List<Integer> colors(DyeColor color) {
        int[] palette = switch(color) {
''' + colors + '''
        };
        return java.util.List.of(palette[0], palette[1], palette[2]);
    }
}
''', encoding='utf-8')
