"""Static IDs from descriptors actually consumed by native collection registrars.
No filesystem asset names are treated as proof of item registration.
"""
import json
from functools import lru_cache
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
@lru_cache(maxsize=1)
def entries():
    result={'item':set(),'block':set()}
    def catalog(name):return json.loads((ROOT/'src/main/resources/jugcraft'/('newest_'+name+'.json')).read_text())
    def add(kind,prefix,names):result[kind].update(prefix+n for n in names)
    d=catalog('building');add('block','',d['blocks']);add('item','',[n for n,v in d['blocks'].items() if 'no_item' not in v]);add('item','',d['items'])
    d=catalog('amends');add('block','amends_',d['blocks']);add('item','amends_',['dye_bottle'])
    d=catalog('backpacks');add('item','packworks_',d['items']);add('block','packworks_',[n for n in d['items'] if n.endswith('backpack')])
    d=catalog('jugtania');add('block','jugtania_',d['blocks']);add('item','jugtania_',[n for n,v in d['blocks'].items() if n not in {'gaia_wall_head','fake_air'} and v['kind'] not in {'pot','buried_petal','bifrost','mana_flame'}]);add('item','jugtania_',d['items'])
    d=catalog('forbidden');add('block','forbidden_',d['blocks']);add('item','forbidden_',[n for n,v in d['blocks'].items() if v['kind']!='skull' and (not v['no_item'] or v['kind']=='jar')]);add('item','forbidden_',d['items'])
    d=catalog('wilds');add('block','wilds_',d['blocks']);add('item','wilds_',[n for n in d['blocks'] if n!='gong_part']);add('item','wilds_',d['items'])
    add('item','skyweave_',catalog('wings')['items'])
    d=catalog('supps');add('block','supps_',d['blocks'])
    excluded={'pignata_anchor','moving_pulley_block','gunpowder','bubble_block','enderman_wall_head','spider_wall_head','way_sign','way_sign_wall','rope_buntings','confetti_litter','flax','potted_flax','spring_launcher_arm','spring_launcher_head'}
    add('item','supps_',[n for n in d['blocks'] if n not in excluded and not n.startswith(('bunting_wall_','sconce_wall','book_pile'))])
    add('item','supps_',['flax','flax_seeds','soap','ash_brick','flute','altimeter','confetti_popper','wrench','rope_arrow','key','bubble_blower','quiver','slingshot','bomb','bomb_blue','slice_map','candy','antique_ink','hat_stand','pignata','dispenser_minecart'])
    add('item','forbidden_',[n+'_skull' for n in ['obsidian','cracked_obsidian','fragmented_obsidian','fading_obsidian','aurealic_obsidian','eternal_obsidian']])
    from newest_ars_additions import CHARMS,UTILITIES,STATIONS
    decor=json.loads((ROOT/'src/main/resources/jugcraft/ars_additions/decor.json').read_text())
    add('block','ars_additions_',decor+STATIONS+['warp_nexus']);add('item','ars_additions_',decor+CHARMS+UTILITIES+STATIONS+['warp_nexus'])
    fishes=[v['name'] for v in catalog('aquatic')['fish']]
    add('item','aquatic_',[n+s for n in fishes for s in ['', '_bucket','_spawn_egg']])
    add('item','aquatic_',['fish_fillet_raw','fish_fillet_cooked','sushi','fish_bones','algae','worm','leech','fishing_line','bobber','neptunium_nugget','neptunium_ingot','driftwood','tin_can','goldfish','box_turtle','arrau_turtle','starshell_turtle','turtle_soup','box','lockbox','treasure_chest','message_in_a_bottle','fish_mount'])
    add('item','aquatic_',[n+'_fillet_knife' for n in ['wooden','stone','iron','gold','diamond','neptunium']]+[n+'_fishing_rod' for n in ['iron','gold','diamond','neptunium']]+[n+'_hook' for n in ['iron','gold','diamond','light','heavy','double','redstone','note','nether_star']])
    add('item','aquatic_',['neptunium_'+n for n in ['axe','pickaxe','shovel','hoe','sword','bow','helmet','chestplate','leggings','boots']]+[n+'_spawn_egg' for n in ['box_turtle','arrau_turtle','starshell_turtle']])
    add('block','aquatic_',['farmland','neptunium_block','worm_farm','tackle_box','neptunes_bounty']);add('item','aquatic_',['farmland','neptunium_block','worm_farm','tackle_box','neptunes_bounty'])
    result['entity_type']={'aquatic_'+n for n in fishes+['box_turtle','arrau_turtle','starshell_turtle']}
    ribbit_source=(ROOT/'src/main/java/io/github/jimbozoomer/jugcraft/ribbits/OwnerRibbits.java').read_text()
    import re
    names=set(re.findall(r'block\("([a-z_]+)"',ribbit_source))|{'brown_toadstool','red_toadstool','toadstool_stem'}
    add('block','ribbits_',names);add('item','ribbits_',names|{'maraca'})
    add('item','supps_',['lumisene_bottle','lumisene_bucket','blast_armor_trim_smithing_template'])
    add('item','amends_',['dragon_charge'])
    add('block','supps_',['lumisene'])
    add('item','supps_',['cannon_boat_'+w for w in ['oak','spruce','birch','jungle','acacia','dark_oak','mangrove','cherry','bamboo']])
    add('item','supps_',['way_sign_'+w for w in ['oak','spruce','birch','jungle','acacia','dark_oak','mangrove','cherry','bamboo','crimson','warped']])
    # Dynamic registry entries come from their actual native datapack directory.
    result['painting_variant']={p.relative_to(ROOT/'src/main/resources/data/jugcraft/painting_variant').as_posix()[:-5] for p in (ROOT/'src/main/resources/data/jugcraft/painting_variant').rglob('*.json')}
    result['fluid']={'supps_lumisene'}
    result['entity_type'].update({'botanical_gaia_guardian','botanical_light_ride','botanical_mana_burst','botanical_pink_wither','botanical_rod_effect','corporea_spark'})
    result['entity_type'].update({'ribbit','forbidden_boom_arrow','forbidden_draco_arcanus_arrow'}|{'ribbit_'+n for n in ['nitwit','gardener','sorcerer','fisherman','merchant']})
    return result
