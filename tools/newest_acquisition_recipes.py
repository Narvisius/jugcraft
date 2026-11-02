"""Restore reviewed supplied recipes omitted by legacy dependency/serializer filters."""
import json
from pathlib import Path
from newest_building import SOURCE, RES, map_value, write
from newest_twigs import recipe


def build():
    outputs={}
    def add(folder, ns, prefix, name, target, custom=None):
        source=SOURCE/folder/'data'/ns/name
        value=recipe(map_value(json.loads(source.read_text(encoding='utf-8-sig')),ns,prefix))
        value.pop('fabric:load_conditions',None);value.pop('neoforge:conditions',None)
        value.pop('neoforge:load_conditions',None)
        if custom: value['type']='jugcraft:'+custom
        outputs[target]=value
    # These source predicates concern their standalone mod flags/backports. Jugcraft
    # registers these native blocks unconditionally; no external addon ingredients.
    for name in ('blackboard','daub','fine_wood','fine_wood_2','fire_pit','fire_pit_2','globe_sepia','gold_gate','item_shelf','planter','awnings/awning'):
        add('SUPPS','supplementaries','supps','recipe/'+name+'.json','supps_'+name)
    for shape in ('fence','fence_gate','slab','stairs'):
        for suffix in ('','_from_moss_block'):
            name='mossy_oak_planks_'+shape+suffix
            add('frogs','ribbits','ribbits','recipe/'+name+'.json','newest/ribbits/'+name)
    for part in ('helmet','chestplate','leggings','boots'):
        name='terrasteel_'+part
        add('Jugtania','botania','jugtania','recipes/'+name+'.json','jugtania/'+name,'botanical_armor_upgrade')
    add('Jugtania','botania','jugtania','recipes/water_rod.json','jugtania/water_rod','botanical_water_shaped')
    # Native variants were registered without their ordinary material recipes.
    # Keep each source family's pattern and quantities rather than inventing a
    # cheap generic recipe for functional equipment or boss rewards.
    mirror=recipe(map_value(json.loads((SOURCE/'home/data/cozyhome/recipe/oak_wall_mirror.json').read_text()),'cozyhome','homeworks'))
    outputs['newest/homeworks/jungle_wall_mirror']=json.loads(json.dumps(mirror).replace('oak_planks','jungle_planks').replace('homeworks_oak_wall_mirror','homeworks_jungle_wall_mirror'))
    def shapeless(name, ingredients, count=1):
        outputs['acquisition/'+name]={'type':'minecraft:crafting_shapeless','ingredients':ingredients,'result':{'id':'jugcraft:'+name,'count':count}}
    # These are actual separate color block IDs, not an NBT tint on the plain box.
    for color in 'white orange magenta light_blue yellow lime pink gray light_gray cyan purple blue brown green red black'.split():
        for kind in ('present','trapped_present'):
            shapeless('supps_'+kind+'_'+color,['jugcraft:supps_'+kind,'minecraft:'+color+'_dye'])
    outputs['acquisition/forbidden_edelwood_stick']={'type':'minecraft:crafting_shaped','pattern':['P','P'],'key':{'P':'jugcraft:forbidden_edelwood_planks'},'result':{'id':'jugcraft:forbidden_edelwood_stick','count':4}}
    for material, plank, slab in (('glass','glass','glass_pane'),('iron','iron_block','iron_ingot')):
        for form in ('chair','wall_clock','grandfather_clock'):
            source=SOURCE/'home/data/cozyhome/recipe'/('oak_'+form+'.json')
            value=recipe(map_value(json.loads(source.read_text()),'cozyhome','homeworks'))
            value=json.loads(json.dumps(value).replace('minecraft:oak_planks','minecraft:'+plank).replace('minecraft:oak_slab','minecraft:'+slab).replace('homeworks_oak_','homeworks_'+material+'_'))
            outputs['newest/homeworks/'+material+'_'+form]=value
    for style, material in (('undead','rotten_flesh'),('ominous','echo_shard')):
        for form in ('chair','wall_clock','grandfather_clock','lamp','table'):
            shapeless('homeworks_'+style+'_'+form,['jugcraft:homeworks_dark_oak_'+form,'minecraft:'+material])
    shapeless('homeworks_mangrove_lantern',['minecraft:lantern','minecraft:mangrove_planks'])
    shapeless('homeworks_trader_cushion',['jugcraft:homeworks_cushion','minecraft:blue_carpet','minecraft:yellow_carpet'])
    for color in ('blue','brown','green','red','white'):
        shapeless('holidayworks_couple_'+color+'_stockings',['jugcraft:holidayworks_'+color+'_stocking']*2)
    # Pose changes reuse the same paid decoration, with one unambiguous output.
    for base, target in (('bat_sleeping','bat_awake'),('bat_awake','bat_sleeping'),('ghost','happy_ghost'),('happy_ghost','haunting_ghost'),('haunting_ghost','ghost')):
        shapeless('holidayworks_'+target,['jugcraft:holidayworks_'+base])
    # Owner approved authoring missing progression on November 1. Ordinary
    # decorative adaptations use paid native materials, never external mod IDs.
    bills={
        'forbidden_arcane_crystal_dust_speck':['jugcraft:forbidden_arcane_crystal_dust'],
        'forbidden_splash_aureal_bottle':['jugcraft:forbidden_aureal_bottle','minecraft:gunpowder'],
        'forbidden_corrupted_arcane_crystal':['jugcraft:forbidden_arcane_crystal','jugcraft:forbidden_corrupt_soul','minecraft:chorus_fruit'],
        'forbidden_mortar':['minecraft:stone_slab','minecraft:stone_slab','minecraft:stone_slab','minecraft:stick'],
        'jugtania_soulscribe':['jugcraft:jugtania_mana_pearl','jugcraft:jugtania_manasteel_ingot','jugcraft:jugtania_livingwood_twig','minecraft:ender_eye'],
        'jugtania_lens_storm':['jugcraft:jugtania_lens_normal','jugcraft:jugtania_rune_air','minecraft:lightning_rod'],
        'jugtania_root':['minecraft:mangrove_roots','jugcraft:jugtania_redstone_root'],
        'jugtania_living_root':['jugcraft:jugtania_root','jugcraft:jugtania_mana_powder'],
        'jugtania_pebble':['minecraft:cobblestone'],
        'twigworks_twigs':['jugcraft:twigworks_twig','jugcraft:twigworks_twig','jugcraft:twigworks_twig'],
        'twigworks_black_packed_silt':['jugcraft:twigworks_packed_silt','minecraft:black_dye'],
        'twigworks_petrified_lichen':['minecraft:stone','minecraft:glow_lichen'],
        'supps_edelwood_stick':['jugcraft:forbidden_edelwood_stick','minecraft:string'],
        'supps_propelplant_cane':['minecraft:sugar_cane','minecraft:chorus_fruit','minecraft:string'],
        'supps_suspicious_gravel_bricks':['jugcraft:supps_gravel_bricks','minecraft:clay_ball'],
        'ars_additions_memory_crystal':['minecraft:amethyst_shard','minecraft:diamond','minecraft:ender_pearl','minecraft:ender_pearl'],
        'ars_additions_enchanting_wixie_cauldron':['jugcraft:nouveau_wixie_cauldron','minecraft:enchanting_table','minecraft:diamond'],
    }
    for name, bill in bills.items():shapeless(name,bill,9 if name.endswith('dust_speck') else 4 if name=='jugtania_pebble' else 1)
    for variant,material in {'ender':'ender_pearl','glow':'glow_ink_sac','green':'copper_ingot','nether_brass':'gold_ingot'}.items():
        shapeless('supps_sconce_'+variant,['jugcraft:supps_sconce','minecraft:'+material])
    for variant,material in {'ender':'ender_pearl','soul':'soul_sand','cupric':'copper_ingot','spectacle':'amethyst_shard'}.items():
        shapeless('supps_candle_holder_'+variant,['minecraft:candle','minecraft:iron_nugget','minecraft:iron_nugget','minecraft:'+material])
    return outputs


def build_rituals():
    """Paid forge routes: existing forge tiers, essences and eight-pedestal limit."""
    outputs={}
    def ritual(name,main,parts,tier=2):
        def item(n):return n if ':' in n else 'jugcraft:forbidden_'+n
        outputs[name]={'forge_tier':tier,'magic_circle':'jugcraft:forbidden_create_item',
            'main_ingredient':{'item':item(main)},
            'inputs':[{'amount':amount,'ingredient':{'item':item(n)}} for n,amount in parts],
            'essences':{'aureal':200*tier,'blood':250*tier,'souls':tier,'experience':100*tier},
            'result':{'type':'jugcraft:forbidden_transmute_input','result_item':{'id':item(name),'count':1}}}
    for tool in ('pickaxe','axe','shovel','hoe','sword'):
        ritual('reinforced_deorum_'+tool,'minecraft:diamond_'+tool,[('deorum_ingot',4),('stellarite_piece',2)],2)
        ritual('draco_arcanus_'+tool,'minecraft:netherite_'+tool,[('dragon_scale',2),('obsidiansteel_ingot',3)],3)
    ritual('reinforced_deorum_blacksmith_gavel','diamond_blacksmith_gavel',[('deorum_ingot',4),('stellarite_piece',2)],2)
    for part in ('helmet','chestplate','leggings','boots'):
        # Mortem is weaker but does not wear out; require advanced forge investment.
        ritual('mortem_'+part,'minecraft:diamond_'+part,[('obsidiansteel_ingot',3),('enchanted_soul',2),('stellarite_piece',1)],3)
    for name in ('draco_arcanus_scepter','draco_arcanus_staff'):
        ritual(name,'magic_wand',[('dragon_scale',4),('dark_nether_star',1),('obsidiansteel_ingot',2)],4)
    ritual('aurealic_obsidian_skull','obsidian_skull',[('deorum_ingot',4),('aureal_bottle',2)],3)
    ritual('eternal_obsidian_skull','aurealic_obsidian_skull',[('eternal_stella',1),('dragon_scale',2),('obsidiansteel_ingot',4)],4)
    ritual('aureal_wardstone','chiseled_arcane_polished_darkstone',[('deorum_ingot',4),('aureal_bottle',2)],2)
    ritual('corrupted_arcane_crystal_obelisk','arcane_crystal_obelisk',[('corrupted_arcane_crystal',4),('corrupt_soul',2)],3)
    ritual('crescent_moon','minecraft:clock',[('stellarite_piece',2),('deorum_ingot',4),('enchanted_soul',1)],3)
    ritual('divine_pact','minecraft:enchanted_book',[('stellarite_piece',2),('dark_nether_star',1),('deorum_ingot',4)],4)
    ritual('omega_arcoin','deorum_block',[('dragon_scale',2),('dark_nether_star',1),('stellarite_piece',4)],4)
    ritual('soul_crimson_stone','minecraft:crimson_nylium',[('enchanted_soul',2),('obsidiansteel_ingot',4)],3)
    return outputs


def build_rewards():
    # Preserve existing encounter rewards. Named additions make regeneration
    # idempotent and keep trophies/XP discoveries out of ordinary crafting.
    additions={
        'botanical_gaia/normal_killer':[('jugtania_gaia_head',.05)],
        'botanical_gaia/hard_killer':[('jugtania_gaia_head',.2)],
        'entities/wilds_umvuthi':[('wilds_sunblock_staff',.15)],
        'ars_additions_chests/arcane_library':[('ars_additions_lost_codex_entry',.15),('ars_additions_ancient_codex_entry',.05)],
        'ars_additions_chests/nexus_tower':[('ars_additions_lost_codex_entry',.2),('ars_additions_ancient_codex_entry',.1)],
    }
    out={}
    for name, rewards in additions.items():
        path=RES/'data/jugcraft/loot_table'/(name+'.json')
        data=json.loads(path.read_text())
        data['pools']=[p for p in data['pools'] if not p.get('name','').startswith('acquisition_nov1_')]
        for item,chance in rewards:
            pool={'name':'acquisition_nov1_'+item,'rolls':1,
                  'entries':[{'type':'minecraft:item','name':'jugcraft:'+item}],
                  'condition':{'type':'minecraft:random_chance','chance':chance}}
            if name.startswith('entities/'):
                pool['condition']={'type':'minecraft:all_of','terms':[pool['condition'],{'type':'minecraft:killed_by_player'}]}
            data['pools'].append(pool)
        out[name]=data
    return out


def main():
    for name,data in build().items():write(RES/'data/jugcraft/recipe'/(name+'.json'),data)
    for name,data in build_rituals().items():write(RES/'data/jugcraft/arcane_ritual'/('acquisition_'+name+'.json'),data)
    for name,data in build_rewards().items():write(RES/'data/jugcraft/loot_table'/(name+'.json'),data)
    write(RES.parents[2]/'src/gametest/resources/newest-acquisition-cases.json',{'crafting':sorted(build()),'rituals':sorted(build_rituals())})

if __name__=='__main__':main()
