"""Adapt supplied Ars additions into existing Jug Nouveau. Never execute owner files.

Only implemented entries get native registrations/recipes. Original art is retained
in runtime copies; that is not evidence that the remaining gameplay is complete.
"""
import json
import shutil
from pathlib import Path
from nouveau_content import walk, resource_id, asset_id

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'art/owner-library/originals/Edited-2026-10-29/ars adds'
ASSET = SOURCE / 'assets/ars_additions'
RES = ROOT / 'src/main/resources'
CHARMS = [k + '_charm' for k in ('fire_resistance', 'undying', 'dispel_protection',
    'fall_prevention', 'water_breathing', 'ender_mask', 'void_protection',
    'sonic_boom_protection', 'wither_protection', 'golden', 'night_vision', 'powdered_snow_walk')]
MACHINES = {'enchanting_wixie_cauldron', 'ender_source_jar', 'source_spawner'}
STATIONS = ['enchanting_wixie_cauldron','ender_source_jar','source_spawner']
DECOR = sorted(p.stem for p in (ASSET / 'blockstates').glob('*.json') if p.stem not in MACHINES)
UTILITIES = ['xp_jar','handy_haversack','memory_crystal','warp_index','stabilized_warp_index','advanced_dominion_wand','nexus_warp_scroll','unstable_reliquary','glyph_mark','glyph_recall','glyph_retaliate','thread_reach','imbued_spell_parchment','codex_entry','lost_codex_entry','ancient_codex_entry','wayfinder','exploration_warp_scroll','ritual_locate_structure','ritual_chunk_loading']

def read(path): return json.loads(path.read_text(encoding='utf-8-sig'))

GLYPH_LABELS = {
    'nouveau_mark': ('Mark', 'Mark an authorized position or creature for reliquary recall.'),
    'nouveau_recall': ('Recall', 'Deliver the spell through the valid mark stored in your reliquary.'),
    'nouveau_retaliate': ('Retaliate', 'Deliver the spell toward your recent attacker through your reliquary.')}

def glyph_definitions():
    definitions = {}
    for name, cost in [('mark', 2), ('recall', 4), ('retaliate', 2)]:
        data = {'schema': 1, 'capacity': 1, 'focus': cost, 'requires': {'research':'jugcraft:nouveau_arcana','state':'encountered'}}
        if name == 'mark': data.update(slot='operation', operation={'principle':'radiance','intent':'helpful','effect':'nouveau','status':'jugcraft:nouveau/mark','magnitude':1,'duration':2400})
        else: data.update(slot='delivery', delivery={'form':'touch','range':6})
        definitions['nouveau_' + name] = data
    return definitions

def write(path, data):
    if '/loot_table/' in str(path).replace('\\','/'):
        from menagerie_data import loot as native_loot
        data=native_loot(data)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')
def registry(value):
    if value.startswith('#'):return '#'+registry(value[1:])
    return 'jugcraft:ars_additions_' + value.split(':',1)[1] if value.startswith('ars_additions:') else resource_id(value)
def asset(value):
    if value.startswith('ars_additions:'): return 'jugcraft:newest/ars_additions/' + value.split(':',1)[1]
    return asset_id(value.replace('ars_nouveau:', 'ars_jimbaux:'))
def tag(name, values):
    path = RES / 'data' / (name + '.json')
    data = read(path) if path.exists() else {'replace':False, 'values':[]}
    unique = {json.dumps(v,sort_keys=True):v for v in data['values'] + values}
    data['values'] = list(unique.values()); write(path, data)
def native_ingredient(value):
    if isinstance(value, dict):
        if 'item' in value: return registry(value['item'])
        if 'tag' in value: return '#' + registry(value['tag'])
    if isinstance(value, list): return [native_ingredient(v) for v in value]
    return registry(value) if isinstance(value, str) else value

def main():
    implemented = set(CHARMS + DECOR + UTILITIES + STATIONS + ['warp_nexus'])
    for folder in ('textures', 'models', 'animations', 'geo'):
        for path in (ASSET / folder).rglob('*'):
            if not path.is_file(): continue
            dest = RES / 'assets/jugcraft' / folder / 'newest/ars_additions' / path.relative_to(ASSET / folder)
            if path.suffix == '.json':
                data = walk(read(path), asset)
                if isinstance(data,dict): data.pop('render_type',None)
                write(dest,data)
            else:
                dest.parent.mkdir(parents=True,exist_ok=True); shutil.copyfile(path,dest)
    for name in sorted(implemented):
        write(RES / 'assets/jugcraft/items' / ('ars_additions_' + name + '.json'),
              {'model':{'type':'minecraft:model','model':'jugcraft:newest/ars_additions/item/' + (name+'_open' if name=='handy_haversack' else name)}})
    # The supplied cauldron renderer explicitly uses the base apparatus geometry/animations.
    write(RES/'assets/jugcraft/items/ars_additions_enchanting_wixie_cauldron.json',
          read(RES/'assets/jugcraft/items/nouveau_enchanting_apparatus.json'))
    write(RES/'assets/jugcraft/items/ars_additions_handy_haversack.json',{'model':{'type':'minecraft:condition','property':'minecraft:has_component','component':'jugcraft:ars_haversack_loaded','on_true':{'type':'minecraft:model','model':'jugcraft:newest/ars_additions/item/handy_haversack_filled'},'on_false':{'type':'minecraft:model','model':'jugcraft:newest/ars_additions/item/handy_haversack_open'}}})
    # Native lodestone compass dispatch retains all 32 supplied direction frames.
    wayfinder={'type':'minecraft:range_dispatch','property':'minecraft:compass','target':'lodestone','wobble':True,'entries':[],'fallback':{'type':'minecraft:model','model':'jugcraft:newest/ars_additions/item/wayfinder/wayfinder_blank'}}
    for i in range(32):wayfinder['entries'].append({'threshold':max(0,(i-.5)/32),'model':{'type':'minecraft:model','model':f'jugcraft:newest/ars_additions/item/wayfinder/wayfinder_{(i+16)%32:02d}'}})
    write(RES/'assets/jugcraft/items/ars_additions_wayfinder.json',{'model':{'type':'minecraft:condition','property':'minecraft:has_component','component':'minecraft:lodestone_tracker','on_true':wayfinder,'on_false':wayfinder['fallback']}})
    write(RES/'data/jugcraft/recipe/ars_additions/exploration_warp_scroll.json',{'type':'minecraft:crafting_shapeless','ingredients':['jugcraft:nouveau_warp_scroll','minecraft:compass'],'result':{'id':'jugcraft:ars_additions_exploration_warp_scroll'}})
    for path in (SOURCE/'data/ars_additions/recipe/locate_structure').glob('*.json'):
        data=walk(read(path),registry); structure=data['structure'];loc=path.stem
        target=structure.get('key','#'+structure.get('tag',''))
        write(RES/f'data/jugcraft/tags/worldgen/structure/ars_additions/locate/{loc}.json',{'replace':False,'values':[{'id':target,'required':False}]})
        data['type']='ars_locate_structure';data['locator']=loc
        data['augments']=[{'item':x['key']} if 'key' in x else x for x in data.get('augments',[])]
        write(RES/f'data/jugcraft/nouveau_recipe/ars_additions/locate/{loc}.json',data)
    for path in (SOURCE/'data/ars_additions/tags/worldgen/structure').glob('*.json'):
        data=walk(read(path),registry);write(RES/f'data/jugcraft/tags/worldgen/structure/ars_additions_{path.stem}.json',data)
    # The Nexus uses the supplied two-block Gecko model and its open/spin/close clips.
    for group in ('block','item'):
        write(RES/f'assets/jugcraft/geckolib/models/{group}/nouveau/ars_additions_warp_nexus.geo.json',read(ASSET/'geo/warp_nexus.geo.json'))
        write(RES/f'assets/jugcraft/geckolib/animations/{group}/nouveau/ars_additions_warp_nexus.animation.json',read(ASSET/'animations/warp_nexus_animations.json'))
    skin=RES/'assets/jugcraft/textures/nouveau/block/ars_additions_warp_nexus.png';skin.parent.mkdir(parents=True,exist_ok=True)
    shutil.copyfile(ASSET/'textures/block/warp_nexus.png',skin)
    write(RES/'assets/jugcraft/items/ars_additions_warp_nexus.json',read(RES/'assets/jugcraft/items/nouveau_enchanting_apparatus.json'))
    write(RES/'assets/jugcraft/blockstates/ars_additions_warp_nexus.json',{'variants':{'':{'model':'jugcraft:newest/ars_additions/block/warp_nexus_particle'}}})
    write(RES/'assets/jugcraft/models/newest/ars_additions/block/warp_nexus_particle.json',{'textures':{'particle':'jugcraft:newest/ars_additions/block/warp_nexus'}})
    write(RES/'data/jugcraft/loot_table/blocks/ars_additions_warp_nexus.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:ars_additions_warp_nexus'}],'conditions':[{'condition':'minecraft:survives_explosion'},{'condition':'minecraft:block_state_property','block':'jugcraft:ars_additions_warp_nexus','properties':{'half':'lower'}}]}]})
    # A native craftable route supplements the supplied structure-only placement.
    write(RES/'data/jugcraft/recipe/ars_additions/warp_nexus.json',{'type':'minecraft:crafting_shaped','pattern':['QEQ','QSQ','OOO'],'key':{'Q':'minecraft:quartz_block','E':'minecraft:ender_eye','S':'jugcraft:nouveau_source_gem','O':'minecraft:obsidian'},'result':{'id':'jugcraft:ars_additions_warp_nexus'}})
    write(RES/'data/jugcraft/recipe/ars_additions/nexus_warp_scroll.json',{'type':'minecraft:crafting_shapeless','ingredients':['jugcraft:nouveau_stable_warp_scroll','minecraft:ender_eye'],'result':{'id':'jugcraft:ars_additions_nexus_warp_scroll'}})
    for name in DECOR + STATIONS:
        write(RES / 'assets/jugcraft/blockstates' / ('ars_additions_' + name + '.json'),walk(read(ASSET/'blockstates'/(name+'.json')),asset))
        loot={'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:ars_additions_'+name}],
            'conditions':[{'condition':'minecraft:survives_explosion'}]}]}
        if name.endswith('_door'):
            loot['pools'][0]['conditions'].append({'condition':'minecraft:block_state_property',
                'block':'jugcraft:ars_additions_'+name,'properties':{'half':'lower'}})
        write(RES/'data/jugcraft/loot_table/blocks'/('ars_additions_'+name+'.json'),loot)
    for path in (SOURCE/'data/ars_additions/recipe').rglob('*.json'):
        data = read(path); result=data.get('result',data.get('output',{}))
        result_id = result.get('id',result.get('item','')) if isinstance(result,dict) else result
        if not result_id.startswith('ars_additions:') or result_id.split(':',1)[1] not in implemented: continue
        kind=data['type']; data=walk(data,registry)
        if kind in ('ars_nouveau:enchanting_apparatus','ars_nouveau:glyph'):
            data['type']=kind.split(':',1)[1]
            write(RES/'data/jugcraft/nouveau_recipe/ars_additions'/path.relative_to(SOURCE/'data/ars_additions/recipe'),data)
        elif kind in ('minecraft:crafting_shaped','minecraft:crafting_shapeless','minecraft:smelting','minecraft:stonecutting'):
            if 'key' in data:data['key']={k:native_ingredient(v) for k,v in data['key'].items()}
            if 'ingredients' in data:data['ingredients']=[native_ingredient(v) for v in data['ingredients']]
            if 'ingredient' in data:data['ingredient']=native_ingredient(data['ingredient'])
            if isinstance(data.get('result'),str):data['result']={'id':data['result'],'count':data.pop('count',1)}
            if isinstance(data.get('result'),dict) and 'item' in data['result']:data['result']['id']=data['result'].pop('item')
            data.pop('conditions',None)
            write(RES/'data/jugcraft/recipe/ars_additions'/path.relative_to(SOURCE/'data/ars_additions/recipe'),data)
    # Spellweave keeps the original single socket with increasing thread strength.
    enchantment=walk(read(SOURCE/'data/ars_additions/enchantment/spellweave.json'),registry)
    enchantment['description']={'translate':'enchantment.jugcraft.ars_additions_spellweave'}
    write(RES/'data/jugcraft/enchantment/ars_additions_spellweave.json',enchantment)
    for path in (SOURCE/'data/ars_additions/recipe/apparatus').glob('spellweave_*.json'):
        data=walk(read(path),registry);data['type']='enchantment'
        write(RES/'data/jugcraft/nouveau_recipe/ars_additions/apparatus'/path.name,data)
    painting=read(SOURCE/'data/ars_additions/painting_variant/snoozebuncle.json')
    painting['asset_id']='jugcraft:newest/ars_additions/snoozebuncle'
    write(RES/'data/jugcraft/painting_variant/ars_additions_snoozebuncle.json',painting)
    tag('minecraft/tags/painting_variant/placeable',['jugcraft:ars_additions_snoozebuncle'])
    write(RES/'data/jugcraft/recipe/ars_additions/imbued_spell_parchment.json',{'type':'minecraft:crafting_shapeless','ingredients':['jugcraft:nouveau_blank_parchment','jugcraft:nouveau_source_gem'],'result':{'id':'jugcraft:ars_additions_imbued_spell_parchment'}})
    for key,data in glyph_definitions().items():
        name=key.removeprefix('nouveau_')
        write(RES/f'data/jugcraft/concordance/component/{key}.json',data)
        write(RES/f'data/jugcraft/spell_assignments/ars_additions_glyph_{name}.json',{'access':'NONE','access_param':''})
    marked=ASSET/'textures/mob_effect/marked.png'
    if marked.exists():shutil.copyfile(marked,RES/'assets/jugcraft/textures/mob_effect/nouveau_marked.png')
    tag('trinkets/tags/item/legs/charm',['jugcraft:ars_additions_'+name for name in CHARMS])
    tag('trinkets/tags/item/legs/charm',['jugcraft:ars_additions_warp_index','jugcraft:ars_additions_stabilized_warp_index'])
    tag('jugcraft/tags/entity_type/ars_additions_source_spawner_denylist',['#jugcraft:nouveau_jar_release_blacklist',{'id':'#c:bosses','required':False}])
    for suffix,category in [('_wall','walls'),('_door','doors'),('_trapdoor','trapdoors'),('_button','buttons')]:
        tag('minecraft/tags/block/'+category,['jugcraft:ars_additions_'+n for n in DECOR if n.endswith(suffix)])
    tag('minecraft/tags/block/mineable/pickaxe',['jugcraft:ars_additions_'+n for n in DECOR if n!='magebloom_carpet'])
    langpath=RES/'assets/jugcraft/lang/en_us.json'; lang=read(langpath)
    original=read(ASSET/'lang/en_us.json')
    for name in sorted(implemented):
        prefix='block' if name in DECOR + STATIONS + ['warp_nexus'] else 'item'
        lang[prefix+'.jugcraft.ars_additions_'+name]=original.get(prefix+'.ars_additions.'+name,name.replace('_',' ').title())
    lang['effect.jugcraft.nouveau_marked']='Marked'
    for key,(title,description) in GLYPH_LABELS.items():
        lang['component.jugcraft.'+key]=title
        lang['component.jugcraft.'+key+'.description']=description
    lang['enchantment.jugcraft.ars_additions_spellweave']='Spellweave'
    write(langpath,lang)
    write(RES/'jugcraft/ars_additions/decor.json',DECOR)
    from newest_ars_worldgen import generate
    generate(SOURCE,RES,read,write,registry)

if __name__=='__main__': main()
