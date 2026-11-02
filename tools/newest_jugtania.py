"""Original Jugtania resources for implemented native botanical workshop paths.

The descriptor is deliberately explicit: unused source equipment is not registered
as inert items. Original files remain unchanged; this generator is project code.
"""
from pathlib import Path
import re,copy as deepcopy_module
from newest_building import SOURCE,RES,read,write as raw_write,copy,map_value,merge_tag
from menagerie_data import loot as native_loot
def write(path,value):
 if '/loot_table/' in path.as_posix():
  value=native_loot(value)
  if '/blocks/jugtania_' in path.as_posix():value=flower_drop_components(value)
 raw_write(path,value)

def flower_drop_components(value):
 if isinstance(value,list):return [flower_drop_components(v) for v in value]
 if not isinstance(value,dict):return value
 if value.get('type')=='minecraft:copy_nbt':
  return {'type':'minecraft:copy_components','source':'block_entity','include':['jugcraft:botanical_flower_memory']}
 return {k:flower_drop_components(v) for k,v in value.items()}
from newest_twigs import recipe

S=SOURCE/'Jugtania';A=S/'assets/botania';D=S/'data/botania';O=RES/'assets/jugcraft'
COLORS='white orange magenta light_blue yellow lime pink gray light_gray cyan purple blue brown green red black'.split()
STATIONS={'pure_daisy','endoflame','mana_pool','diluted_pool','mana_spreader','redstone_spreader','elven_spreader','gaia_spreader','runic_altar','terra_plate','alfheim_portal','mana_pylon','natura_pylon'}
STATIONS|={'starfield','gaia_head','gaia_wall_head','light_launcher','prism','fabulous_pool','creative_pool','pump','mana_fluxfield','tiny_planet_block','tiny_potato','cacophonium_block','gaia_pylon'}
STATIONS|={'abstruse_platform','spectral_platform','infrangible_platform'}
STATIONS|={'red_string_container','red_string_dispenser','red_string_comparator','red_string_fertilizer','red_string_interceptor','red_string_relay'}
STATIONS|={'avatar','light_relay','detector_light_relay','fork_light_relay','toggle_light_relay'}
STATIONS|={'entropinnyum','cell_block','narslimmus','shulk_me_not','rafflowsia','dandelifeon'}
STATIONS|={'hydroangeas','thermalily','gourmaryllis','rosa_arcana','kekimurus','munchdew','spectrolus'}
STATIONS|={'agricarnation','agricarnation_chibi','clayconia','clayconia_chibi','fallen_kanade','jaded_amaranthus','orechid','orechid_ignem','marimorphosis','marimorphosis_chibi'}
STATIONS|={'hopperhock','hopperhock_chibi','bellethorn','bellethorn_chibi','dreadthorn','hyacidus','medumone','tangleberrie','tangleberrie_chibi','jiyuulia','jiyuulia_chibi','daffomill','tigerseye','heisei_dream','exoflame','pollidisiac','labellia','rannuncarpus','rannuncarpus_chibi','spectranthemum','bergamute','solegnolia','solegnolia_chibi','vinculotus','bubbell','bubbell_chibi','fake_air'}
MATERIALS={'livingwood_twig','dreamwood_twig','manasteel_ingot','manasteel_nugget','terrasteel_ingot','terrasteel_nugget','elementium_ingot','elementium_nugget','mana_diamond','mana_pearl','mana_powder','pixie_dust','redstone_root','dragonstone','quartz_mana','quartz_blaze','quartz_lavender','quartz_red','quartz_elf','quartz_sunny','gaia_ingot','life_essence','mana_string'}
MATERIALS.discard('quartz_elf');MATERIALS|={'quartz_elven','quartz_dark'}
MATERIALS|={'rune_'+r for r in 'air earth fire water mana spring summer autumn winter envy gluttony greed lust pride sloth wrath'.split()}
ITEMS=MATERIALS|{c+'_petal' for c in COLORS}|{'twig_wand','fertilizer','mana_tablet'}
ITEMS|={'manasteel_pick','manasteel_axe','manasteel_shovel','manasteel_hoe','manasteel_sword','manasteel_shears','soulscribe','ender_dagger','livingwood_bow'}
ITEMS|={'elementium_axe','elementium_sword','terra_pick','terra_axe','terra_sword','glass_pickaxe','star_sword','thunder_sword','temperance_stone'}
ITEMS|={'crystal_bow','elementium_pickaxe','elementium_shovel','elementium_hoe','elementium_shears'}
ITEMS|={f+'_'+p for f in ['manasteel','elementium','terrasteel','manaweave'] for p in ['helmet','chestplate','leggings','boots']}
ITEMS|={'manaweave_cloth','phantom_ink'}|{'ancient_will_'+w for w in ['ahrim','dharok','guthan','torag','verac','karil']}
ITEMS|={'corporea_spark','corporea_spark_master','corporea_spark_creative'}
STATIONS|={'corporea_index','corporea_funnel','corporea_crystal_cube','corporea_interceptor','corporea_retainer'}
ITEMS|={'magnet_ring', 'pixie_ring', 'reach_ring', 'ice_pendant', 'lava_pendant', 'swap_ring', 'holy_cloak', 'invisibility_cloak', 'super_travel_belt', 'goddess_charm', 'diva_charm', 'blood_pendant', 'water_ring', 'unholy_cloak', 'knockback_belt', 'speed_up_belt', 'mining_ring', 'balance_cloak', 'magnet_ring_greater', 'third_eye', 'travel_belt', 'super_lava_pendant'}
ITEMS|={'dodge_ring','cloud_pendant','super_cloud_pendant','flight_tiara'}
COSMETIC_HEAD='red_glasses engineer_goggles eyepatch wicked_eyepatch red_ribbons pink_flower_bud polka_dotted_bows blue_butterfly cat_ears googly_eyes clock_eye unicorn_horn devil_horns hyper_plus ancient_mask eerie_mask alien_antenna anaglyph_glasses orange_shades groucho_glasses thick_eyebrows tiny_potato_mask questgiver_mark thinking_hand'.split()
COSMETIC_BODY='black_bowtie black_tie puffy_scarf witch_pin devil_tail kamui_eye four_leaf_clover botanist_emblem lusitanic_shield'.split()
ITEMS|={'monocle','itemfinder'}|{'cosmetic_'+n for n in COSMETIC_HEAD+COSMETIC_BODY}
ITEMS|={'divining_rod'}
ITEMS|={'fire_rod','missile_rod','gravity_rod'}
ITEMS|={'exchange_rod','terraform_rod','rainbow_rod'}
ITEMS|={'dirt_rod','cobble_rod','skydirt_rod','water_rod','tornado_rod','smelt_rod'}
ITEMS|={'mana_ring','mana_ring_greater','aura_ring','aura_ring_greater','mana_mirror'}
ITEMS|={'spark','spark_upgrade_dominant','spark_upgrade_recessive','spark_upgrade_dispersive','spark_upgrade_isolated'}
ITEMS|={'black_lotus','blacker_lotus','overgrowth_seed','lexicon'}
STATIONS|={'animated_torch','forest_eye','ender_eye_block','teru_teru_bozu','spawner_claw','cocoon','spark_changer'}
STATIONS|={'enchanter','open_crate','crafty_crate'}
ITEMS|={'placeholder'}|{'pattern_'+n for n in ['1_1','2_2','1_2','2_1','1_3','3_1','2_3','3_2','donut']}
STATIONS|={'drum_wild','drum_canopy','drum_gathering'}
ITEMS|={'horn_grass','horn_leaves','horn_snow'}
STATIONS|={'bellows','turntable','hourglass','brewery','incense_plate'}
ITEMS|={'vial','flask','brew_vial','brew_flask','incense_stick'}
STATIONS|={'loonium','manastar','mana_void','mana_distributor'}
ITEMS|={n+'_seeds' for n in ['grass','podzol','mycelium','dry','golden','vivid','scorched','infused','mutated']}
ISLANDS=['grass','snow','podzol','mycel','dry','golden','vivid','scorched','infused','mutated']

def floating_model(name,island):
 """Bake the supplied loader's half-size/0.2-block flower transform into original cuboids."""
 slots={}
 def resolve(name):
  if name in ['minecraft:block/block','minecraft:block/cube','block/block']:return {}
  if not name.startswith('botania:'):raise ValueError('Unexpected botanical model parent '+name)
  v=read(A/'models'/(name[8:]+'.json'));base=resolve(v['parent']) if 'parent' in v else {}
  textures=dict(base.get('textures',{}));textures.update(v.get('textures',{}));base.update(v);base['textures']=textures;return base
 def flatten(model,prefix,transform=False):
  textures=model['textures']
  def texture(value):
   visited=set()
   while value.startswith('#'):
    if value in visited:raise ValueError('Cyclic texture '+value)
    visited.add(value);value=textures[value[1:]]
   return value
  result=[]
  def move(v):return [4+v[0]*.5,7.2+v[1]*.5,4+v[2]*.5]
  for original in model.get('elements',[]):
   e=deepcopy_module.deepcopy(original)
   if transform:
    e['from']=move(e['from']);e['to']=move(e['to'])
    if 'rotation' in e:e['rotation']['origin']=move(e['rotation']['origin'])
   for face in e.get('faces',{}).values():
    resolved=texture(face['texture']);slot=prefix+'_'+str(len(slots));slots[slot]=resolved;face['texture']='#'+slot;face.pop('cullface',None)
   result.append(e)
  return result
 source=read(A/'models/block'/(name+'.json'))
 flower=resolve(source['flower']['parent']);base=resolve('botania:block/islands/island_'+island)
 elements=flatten(base,'island')+flatten(flower,'flower',True)
 model={'parent':'minecraft:block/block','ambientocclusion':False,'textures':{'particle':flower['textures'].get('cross','minecraft:block/dirt'),**slots},'elements':elements}
 if 'display' in base:model['display']=base['display']
 return mapped(model,True)

def encounter_loot(value):
 if isinstance(value,list):return [encounter_loot(x) for x in value]
 if not isinstance(value,dict):return mapped(value)
 out={k:encounter_loot(x) for k,x in value.items()}
 if out.get('type')=='minecraft:loot_table' and 'name' in out:out['value']=out.pop('name')
 if out.get('function')=='minecraft:set_nbt':
  tag=value['tag'];components={}
  trim=re.search(r'Trim:\{material:"([^"]+)",pattern:"([^"]+)"\}',tag)
  color=re.search(r'display:\{color:(\d+)\}',tag)
  if trim:components['minecraft:trim']={'material':trim[1],'pattern':trim[2]}
  if color:components['minecraft:dyed_color']=int(color[1])
  if tag.startswith('{CustomPotionColor:'):
   effect=int(re.search(r'Id:(\d+)',tag)[1]);duration=int(re.search(r'Duration:(\d+)',tag)[1]);rgb=int(re.search(r'CustomPotionColor:(\d+)',tag)[1])
   components['minecraft:potion_contents']={'custom_color':rgb,'custom_effects':[{'id':{15:'minecraft:blindness',25:'minecraft:levitation',33:'minecraft:darkness'}[effect],'duration':duration,'amplifier':0}]}
  if not components:raise ValueError('Untranslated supplied encounter item NBT: '+tag)
  out={k:v for k,v in out.items() if k not in ('function','tag')};out.update({'function':'minecraft:set_components','components':components})
 return out

def mapped(v,asset=False):
 v=map_value(v,'botania','jugtania',asset)
 def common(x):
  if isinstance(x,str):return {'#minecraft:music_discs':'#c:music_discs','#forge:dyes':'#c:dyes','#forge:storage_blocks/quartz':'minecraft:quartz_block','minecraft:grass':'minecraft:short_grass'}.get(x,x)
  if isinstance(x,list):return [common(e) for e in x]
  if isinstance(x,dict):return {k:common(e) for k,e in x.items()}
  return x
 return common(v)
def refs(v):
 if isinstance(v,str):return {v[8:] for _ in [0] if v.startswith('botania:')}
 if isinstance(v,list):return set().union(*(refs(x) for x in v)) if v else set()
 if isinstance(v,dict):return set().union(*(refs(x) for k,x in v.items() if k not in ['type','tag','group'] and not k.endswith('_tag'))) if v else set()
 return set()

def gaia_loot(value,killer):
 # The encounter distributes a roll to each surviving participant. Resolve the
 # old encounter-only conditions here; ordinary random chances remain intact.
 if isinstance(value,list):return [v for x in value if (v:=gaia_loot(x,killer)) is not None]
 if not isinstance(value,dict):return value
 if value.get('function')=='botania:bind_uuid':return None # bound to recipient by server
 conditions=value.get('conditions',[])
 if not killer and any(x.get('condition')=='botania:true_guardian_killer' for x in conditions):return None
 result={k:gaia_loot(v,killer) for k,v in value.items() if k!='conditions'}
 conditions=[x for x in conditions if x.get('condition') not in {'botania:killed_by_player','botania:true_guardian_killer','botania:enable_relics'}]
 if conditions:result['conditions']=gaia_loot(conditions,killer)
 return result
ITEMS|={'water_bowl','pool_minecart','laputa_shard','dice','infinite_fruit','king_key','flugel_eye','thor_ring','odin_ring','loki_ring'}
ITEMS|={'crafting_halo','auto_crafting_halo','pinkinator'}
ITEMS|={'astrolabe','sextant','keep_ivy','vine_ball','thorn_chakram','flare_chakram','slingshot'}
ITEMS|={'cacophonium','tiny_planet','mana_gun','clip','open_bucket','world_seed','ender_hand','obedience_stick','slime_bottle','spell_cloth','dreamwood_wand','living_root','pebble','red_string','flower_bag','bauble_box','black_hole_talisman','spawner_mover','ender_air_bottle','mana_cookie','mana_bottle','record_gaia_1','record_gaia_2','virus_necrodermal','virus_nullodermal'}
ITEMS|={'lens_'+name for name in ['piston','warp','mine','weight','paint','fire','light','explosive','storm','normal','speed','power','time','efficiency','gravity','messenger','bounce','damage','phantom','magnet','influence','redirect','firework','flare','tripwire']}

def main():
 write(RES/'data/jugcraft/tags/item/botanical_clear_glass.json',{'values':['minecraft:glass','minecraft:glass_pane']})
 blocks={}
 write(O/'items/botanical_pump_head.json',{'model':{'type':'minecraft:model','model':'jugcraft:newest/jugtania/block/pump_head'}})
 write(RES/'data/jugcraft/recipe/jugtania/laputa_upgrade.json',{'type':'jugcraft:botanical_laputa_upgrade'})
 for difficulty,filename in [('normal','gaia_guardian'),('hard','gaia_guardian_2')]:
  for killer in [False,True]:
   value=encounter_loot(gaia_loot(read(D/'loot_tables'/(filename+'.json')),killer));value['type']='minecraft:chest'
   write(RES/'data/jugcraft/loot_table/botanical_gaia'/(difficulty+('_killer' if killer else '_helper')+'.json'),value)
 for path in (D/'loot_tables/gaia_guardian').glob('*.json'):
  write(RES/'data/jugcraft/loot_table/jugtania_gaia_guardian'/path.name,encounter_loot(read(path)))
 for i in range(12):write(O/'items'/f'botanical_gate_weapon_{i}.json',{'model':{'type':'minecraft:model','model':f'jugcraft:newest/jugtania/icon/gate_weapon_{i}'}})
 merge_tag(RES/'data/trinkets/tags/item/hand/ring.json',['jugcraft:jugtania_'+n+'_ring' for n in ['thor','odin','loki']],write)
 merge_tag(RES/'data/trinkets/tags/item/offhand/ring.json',['jugcraft:jugtania_'+n+'_ring' for n in ['thor','odin','loki']],write)
 for path in (D/'loot_tables/dice').glob('*.json'):write(RES/'data/jugcraft/loot_table/jugtania_dice'/path.name,mapped(read(path)))
 for i,length in [(1,202),(2,227)]:write(RES/'data/jugcraft/jukebox_song'/f'jugtania_gaia_{i}.json',{'sound_event':f'jugcraft:jugtania_music.gaia{i}','description':{'translate':f'item.botania.record_gaia_{i}.desc'},'length_in_seconds':length,'comparator_output':1})
 merge_tag(RES/'data/trinkets/tags/item/chest/necklace.json',['jugcraft:jugtania_tiny_planet'],write)
 write(RES/'data/jugcraft/recipe/jugtania/ivy_binding.json',{'type':'jugcraft:botanical_resolute_ivy'})
 write(RES/'data/jugcraft/recipe/jugtania/spell_cloth_crafting.json',{'type':'jugcraft:botanical_spell_cloth'})
 write(RES/'data/jugcraft/recipe/jugtania/blaster_crafting.json',{'type':'jugcraft:botanical_blaster_crafting'})
 write(RES/'data/jugcraft/recipe/jugtania/lens_crafting.json',{'type':'jugcraft:botanical_lens_crafting'})
 for p in sorted((A/'blockstates').glob('*.json')):
  n=p.stem;kind=None
  if n.startswith('floating_') and n[9:] in STATIONS or n.endswith('_floating_flower'):kind='floating'
  elif n in STATIONS or n.startswith('apothecary_'):kind='workshop'
  elif n.endswith('_buried_petals'):kind='buried_petal'
  elif n=='fel_pumpkin':kind='fel_pumpkin'
  elif n=='ghost_rail':kind='ghost_rail'
  elif n in ['root','mana_bomb']:kind='block'
  elif n.startswith('potted_'):kind='pot'
  elif n in ['daybloom_motif','nightshade_motif','hydroangeas_motif']:kind='flower'
  elif n in ['mana_glass_pane','elf_glass_pane','bifrost_pane']:kind='pane'
  elif n=='bifrost_perm':kind='glass'
  elif n.startswith(('chiseled_livingrock','chiseled_metamorphic','cracked_livingrock','mossy_','framed_','pattern_framed_','shimmerrock','shimmerwood')) or n=='blaze_block':
   kind='stairs' if n.endswith('_stairs') else 'slab' if n.endswith('_slab') else 'wall' if n.endswith('_wall') else 'block'
  elif n=='solid_vine':kind='solid_vine'
  elif n=='bifrost':kind='bifrost'
  elif n=='piston_relay':kind='force_relay'
  elif n=='mana_flame':kind='mana_flame'
  elif n=='mana_detector':kind='mana_detector'
  elif n in {i+'_grass' for i in ISLANDS[4:]}|{'enchanted_soil'}:kind='block'
  elif re.match(r'^(corporea_block|corporea_brick|corporea_stairs|corporea_slab|livingrock|polished_livingrock|livingwood|stripped_livingwood|dreamwood|stripped_dreamwood|glimmering_|metamorphic_|azulejo_|(?:chiseled_)?(?:mana|blaze|lavender|dark|red|elf|sunny)_quartz)',n) or n in {'manasteel_block','terrasteel_block','elementium_block','mana_diamond_block','dragonstone_block','alchemy_catalyst','conjuration_catalyst'} or any(n.startswith(c+'_pavement') or n==c+'_petal_block' for c in COLORS):
   kind='stairs' if n.endswith('_stairs') else 'slab' if n.endswith('_slab') else 'wall' if n.endswith('_wall') else 'fence_gate' if n.endswith('_fence_gate') else 'fence' if n.endswith('_fence') else 'pillar' if 'axis=' in str(read(p)) else 'block'
  elif any(n==c+s for c in COLORS for s in ['_mystical_flower','_glimmering_flower','_shiny_flower','_mushroom']):kind='flower'
  elif any(n==c+'_double_flower' for c in COLORS):kind='double_flower'
  elif n in ['mana_glass','elf_glass']:kind='glass'
  if kind=='block' and 'axis=' in str(read(p)):kind='pillar'
  if kind=='block' and 'facing=' in str(read(p)):kind='facing_decor'
  if kind:blocks[n]={'kind':kind,'light':15 if 'glimmering' in n or 'shiny' in n or kind=='floating' else 0}
 write(RES/'data/jugcraft/tags/block/jugtania_ghost_rail_barrier.json',{'values':['jugcraft:jugtania_'+n for n in blocks if 'dreamwood' in n and (n.endswith('_log') or n=='dreamwood')]})
 merge_tag(RES/'data/minecraft/tags/block/rails.json',['jugcraft:jugtania_ghost_rail'],write)
 known=set(blocks)|ITEMS
 for folder in ['models','textures']:
  for p in (A/folder).rglob('*'):
   if not p.is_file():continue
   to=O/folder/'newest/jugtania'/p.relative_to(A/folder)
   if p.suffix=='.json':
    v=read(p)
    if folder=='models':v.pop('overrides',None)
    write(to,mapped(v,True))
   else:copy(p,to)
 for n,d in blocks.items():
  state=mapped(read(A/'blockstates'/(n+'.json')),True)
  if d['kind']=='floating':
   state={'variants':{}}
   for index,island in enumerate(ISLANDS):
    model=n if index==0 else n+'_'+island
    write(O/'models/newest/jugtania/block'/(model+'.json'),floating_model(n,island))
    state['variants']['island='+str(index)]={'model':'jugcraft:newest/jugtania/block/'+model}
  if n=='corporea_crystal_cube':state={'variants':{'core=false':{'model':'jugcraft:newest/jugtania/block/'+n},'core=true':{'model':'jugcraft:newest/jugtania/block/corporea_crystal_cube_glass'}}}
  if n=='crafty_crate':
   patterns=['none','crafty_1_1','crafty_2_2','crafty_1_2','crafty_2_1','crafty_1_3','crafty_3_1','crafty_2_3','crafty_3_2','crafty_donut']
   state={'variants':{'pattern='+str(i):state['variants']['pattern='+name] for i,name in enumerate(patterns)}}
  if n in ['mana_pool','diluted_pool','fabulous_pool','creative_pool']:
   state={'variants':{'filled=false':{'model':'jugcraft:newest/jugtania/block/'+n},'filled=true':{'model':'jugcraft:newest/jugtania/block/'+n+'_full'}}}
  if n=='alfheim_portal':state={'variants':{'filled=false':{'model':'jugcraft:newest/jugtania/block/alfheim_portal'},'filled=true':{'model':'jugcraft:newest/jugtania/block/alfheim_portal_activated'}}}
  if n.endswith('_spreader'):state={'variants':{'core=false':{'model':'jugcraft:newest/jugtania/block/'+n},'core=true':{'model':'jugcraft:newest/jugtania/block/'+n+'_core'}}}
  if n in ['mana_pylon','natura_pylon','gaia_pylon']:
   state={'variants':{'':{'model':'jugcraft:newest/empty'}}}
  if n.startswith('apothecary_'):
   source_model=state['variants']['']['model'];original=read(O/'models'/(source_model.split(':')[1]+'.json'))
   def resolve_model(m):
    if 'elements' in m:return m['elements'],dict(m.get('textures',{}))
    parent=m.get('parent','');elements,textures=resolve_model(read(O/'models'/(parent.split(':')[1]+'.json')))
    textures.update(m.get('textures',{}));return elements,textures
   elements,textures=resolve_model(original);textures['water']='minecraft:block/water_still'
   elements=elements+[{'from':[3,13,3],'to':[13,13,13],'faces':{'up':{'uv':[0,0,16,16],'texture':'#water','tintindex':0}}}]
   write(O/'models/newest/jugtania/block'/(n+'_filled.json'),{'textures':textures,'elements':elements})
   state={'variants':{'filled=false':{'model':source_model},'filled=true':{'model':'jugcraft:newest/jugtania/block/'+n+'_filled'}}}
  write(O/'blockstates'/('jugtania_'+n+'.json'),state)
  loot= D/'loot_tables/blocks'/(n+'.json')
  if loot.exists():write(RES/'data/jugcraft/loot_table/blocks'/('jugtania_'+n+'.json'),mapped(read(loot)))
 write(O/'models/newest/jugtania/item/solid_vine.json',{'parent':'minecraft:item/vine'})
 write(O/'items/jugtania_solid_vine.json',{'model':{'type':'minecraft:model','model':'jugcraft:newest/jugtania/item/solid_vine'}})
 write(O/'models/newest/jugtania/item/soulscribe.json',{'parent':'jugcraft:newest/jugtania/item/ender_dagger'})
 write(O/'items/jugtania_soulscribe.json',{'model':{'type':'minecraft:model','model':'jugcraft:newest/jugtania/item/soulscribe'}})
 for n in sorted(known):
  if (A/'models/item'/(n+'.json')).exists():
   model={'type':'minecraft:model','model':'jugcraft:newest/jugtania/item/'+n}
   if n in blocks and blocks[n]['kind']=='floating':write(O/'models/newest/jugtania/item'/(n+'.json'),{'parent':'jugcraft:newest/jugtania/block/'+n})
   if n=='gaia_head':model={'type':'minecraft:special','base':'minecraft:item/template_skull','model':{'type':'jugcraft:botanical_gaia_head'}}
   if n=='fabulous_pool':model['tints']=[{'type':'jugcraft:botanical_rainbow'}]
   if n.startswith('lens_'):model['tints']=[{'type':'jugcraft:botanical_lens'},{'type':'minecraft:constant','value':16777215}]
   if n=='mana_gun':model={'type':'minecraft:condition','property':'minecraft:has_component','component':'jugcraft:botanical_blaster_clip','on_false':{'type':'minecraft:model','model':'jugcraft:newest/jugtania/item/mana_gun_noclip'},'on_true':{'type':'minecraft:model','model':'jugcraft:newest/jugtania/item/mana_gun_clip'}}
   if n=='bauble_box':model={'type':'minecraft:condition','property':'jugcraft:botanical_bag_open','on_false':model,'on_true':{'type':'minecraft:model','model':'jugcraft:newest/jugtania/item/bauble_box_open'}}
   if n=='auto_crafting_halo':model={'type':'minecraft:condition','property':'minecraft:has_component','component':'jugcraft:botanical_halo_active','on_false':model,'on_true':{'type':'minecraft:model','model':'jugcraft:newest/jugtania/item/auto_crafting_halo_active'}}
   if n=='mana_bottle':model={'type':'minecraft:range_dispatch','property':'minecraft:custom_model_data','index':0,'entries':[{'threshold':i,'model':{'type':'minecraft:model','model':f'jugcraft:newest/jugtania/item/mana_bottle_{i}'}} for i in range(1,6)],'fallback':model}
   if n=='incense_stick':model['tints']=[{'type':'minecraft:constant','value':16777215},{'type':'minecraft:custom_model_data','index':0,'default':16777215}]
   if n in ['spark','corporea_spark','corporea_spark_master','corporea_spark_creative']:model['tints']=[{'type':'minecraft:custom_model_data','index':0,'default':16777215}]
   if n=='spawner_mover':model={'type':'minecraft:condition','property':'minecraft:has_component','component':'jugcraft:botanical_spawner_payload','on_false':model,'on_true':{'type':'minecraft:model','model':'jugcraft:newest/jugtania/item/spawner_mover_full'}}
   if n=='black_hole_talisman':model={'type':'minecraft:condition','property':'minecraft:has_component','component':'jugcraft:botanical_black_hole_active','on_false':model,'on_true':{'type':'minecraft:model','model':'jugcraft:newest/jugtania/item/black_hole_talisman_active'}}
   if n=='slime_bottle':model={'type':'minecraft:condition','property':'minecraft:has_component','component':'jugcraft:botanical_slime_chunk','on_false':model,'on_true':{'type':'minecraft:model','model':'jugcraft:newest/jugtania/item/slime_bottle_active'}}
   if n in ['twig_wand','dreamwood_wand']:model['tints']=[{'type':'minecraft:constant','value':16777215},{'type':'minecraft:constant','value':16777215},{'type':'minecraft:constant','value':16777215}]
   if n in ['livingwood_bow','crystal_bow']:
    stages=[{'threshold':t,'model':{'type':'minecraft:model','model':'jugcraft:newest/jugtania/item/'+n+'_pulling_'+str(i)}} for t,i in [(0,1),(.65,2),(.9,3)]]
    model={'type':'minecraft:condition','property':'minecraft:using_item','on_false':model,'on_true':{'type':'minecraft:range_dispatch','property':'minecraft:use_duration','scale':(.1 if n=='crystal_bow' else .05),'entries':stages,'fallback':model}}
   write(O/'items'/('jugtania_'+n+'.json'),{'model':model})
   if n in ['brew_vial','brew_flask']:
    tint=[{'type':'minecraft:constant','value':16777215},{'type':'minecraft:custom_model_data','index':0,'default':16777215}]
    fallback={'type':'minecraft:model','model':'jugcraft:newest/jugtania/item/'+n,'tints':tint}
    entries=[{'threshold':i,'model':{'type':'minecraft:model','model':'jugcraft:newest/jugtania/item/'+n+'_'+str(i),'tints':tint}} for i in range(1,6 if n=='brew_flask' else 4)]
    write(O/'items'/('jugtania_'+n+'.json'),{'model':{'type':'minecraft:range_dispatch','property':'minecraft:custom_model_data','index':0,'entries':entries,'fallback':fallback}})
   if n in ['terra_pick','terra_axe','temperance_stone','tornado_rod']:write(O/'items'/('jugtania_'+n+'.json'),{'model':{'type':'minecraft:condition','property':'minecraft:custom_model_data','index':0,'on_true':{'type':'minecraft:model','model':'jugcraft:newest/jugtania/item/'+n+'_active'},'on_false':{'type':'minecraft:model','model':'jugcraft:newest/jugtania/item/'+n}}})
   if n=='animated_torch':write(O/'items'/('jugtania_'+n+'.json'),{'model':{'type':'minecraft:model','model':'minecraft:item/redstone_torch'}})
   if n in ['avatar','corporea_index','bellows','hourglass','brewery','teru_teru_bozu']:write(O/'items'/('jugtania_'+n+'.json'),{'model':{'type':'minecraft:special','base':'jugcraft:newest/jugtania/item/'+n,'model':{'type':'jugcraft:botanical_mechanism','kind':n}}})
   if n=='corporea_crystal_cube':
    glass=read(O/'models/newest/jugtania/block/corporea_crystal_cube_glass.json');base=read(O/'models/newest/jugtania/block/corporea_crystal_cube.json');base['elements']+=glass['elements'];base['textures'].update({k:v for k,v in glass['textures'].items() if k!='particle'});write(O/'models/newest/jugtania/item/corporea_crystal_cube.json',base)
   if n in ['mana_pylon','natura_pylon','gaia_pylon']:write(O/'items'/('jugtania_'+n+'.json'),{'model':{'type':'minecraft:special','base':'jugcraft:newest/jugtania/item/'+n,'model':{'type':'jugcraft:botanical_pylon','variant':n.removesuffix('_pylon')}}})
 for p in (A/'models/tiny_potato').glob('*.json'):
  if p.stem!='base':write(O/'items'/('jugtania_tater_'+p.stem+'.json'),{'model':{'type':'minecraft:model','model':'jugcraft:newest/jugtania/tiny_potato/'+p.stem}})
 write(O/'items/jugtania_tiny_potato.json',{'model':{'type':'minecraft:special','base':'jugcraft:newest/jugtania/item/tiny_potato','model':{'type':'jugcraft:botanical_potato'}}})
 display_recipes={}
 for p in sorted((D/'recipes').rglob('*.json')):
  v=read(p);typ=v.get('type','')
  if typ=='botania:gog_alternation':v=v['base'];typ=v['type']
  if typ in ['botania:mana_upgrade','botania:mana_upgrade_shapeless']:v['type']=typ='minecraft:crafting_shaped' if typ=='botania:mana_upgrade' else 'minecraft:crafting_shapeless'
  if typ in ('botania:armor_upgrade','botania:water_bottle_matching_shaped'):
   native='botanical_armor_upgrade' if typ=='botania:armor_upgrade' else 'botanical_water_shaped'
   converted=recipe(mapped(v));converted['type']='jugcraft:'+native
   write(RES/'data/jugcraft/recipe/jugtania'/p.relative_to(D/'recipes'),converted)
   continue
  if typ=='botania:twig_wand':v['type']=typ='minecraft:crafting_shaped'
  if refs({k:x for k,x in v.items() if k!='brew'})-known:continue
  if typ.startswith('minecraft:'):
   write(RES/'data/jugcraft/recipe/jugtania'/p.relative_to(D/'recipes'),recipe(mapped(v)))
  elif typ in ['botania:petal_apothecary','botania:pure_daisy','botania:state_copying_pure_daisy','botania:mana_infusion','botania:runic_altar','botania:terra_plate','botania:elven_trade','botania:orechid','botania:orechid_ignem','botania:marimorphosis','botania:brew']:
   v['type']=typ.split(':')[1];converted=mapped(v);write(RES/'data/jugcraft/jugtania_recipe'/p.relative_to(D/'recipes'),converted);display_recipes[p.relative_to(D/'recipes').as_posix()]=converted
 write(O/'botanical_recipe_view.json',display_recipes)
 write(O/'particles/botanical_luminizer_wisp.json',{'textures':['jugcraft:newest/jugtania/particle/wisp']})
 write(RES/'data/jugcraft/damage_type/botanical_armor_piercing.json',{'message_id':'player','scaling':'when_caused_by_living_non_player','exhaustion':0.1})
 merge_tag(RES/'data/minecraft/tags/damage_type/bypasses_armor.json',['jugcraft:botanical_armor_piercing'],write)
 merge_tag(RES/'data/minecraft/tags/item/piglin_safe_armor.json',['jugcraft:jugtania_terrasteel_'+p for p in ['helmet','chestplate','leggings','boots']],write)
 write(RES/'data/jugcraft/loot_table/blocks/jugtania_bifrost.json',{'type':'minecraft:block','pools':[]})
 write(RES/'data/jugcraft/recipe/jugtania/cosmetic_attachment.json',{'type':'jugcraft:botanical_cosmetic_attachment'})
 for tag,names in {'head/hat':['monocle']+['cosmetic_'+n for n in COSMETIC_HEAD],'chest/necklace':['itemfinder']+['cosmetic_'+n for n in COSMETIC_BODY]}.items():merge_tag(RES/'data/trinkets/tags/item'/(tag+'.json'),['jugcraft:jugtania_'+n for n in names],write)
 write(RES/'data/jugcraft/recipe/jugtania/armor_customization.json',{'type':'jugcraft:botanical_armor_customization'})
 for family in ['manasteel','elementium','terrasteel','manaweave']:
  # Native custom armor renderer supplies all layers; empty native equipment maps are invalid.
  obsolete=O/'equipment'/('jugtania_'+family+'.json')
  if obsolete.exists() and read(obsolete)=={'layers':{}}:obsolete.unlink()
  for piece in ['helmet','chestplate','leggings','boots']:
   name='jugcraft:jugtania_'+family+'_'+piece
   for tag in ['enchantable/armor','enchantable/durability',{'helmet':'head_armor','chestplate':'chest_armor','leggings':'leg_armor','boots':'foot_armor'}[piece]]:merge_tag(RES/'data/minecraft/tags/item'/(tag+'.json'),[name],write)
 write(RES/'data/jugcraft/tags/item/jugtania_manaweave_repair.json',{'values':['jugcraft:jugtania_manaweave_cloth']})
 # Keep only actually registered contents; unknown optional integrations never make tags fail loading.
 for name in ['common','rare','common_aquatic','rare_aquatic']:write(RES/'data/jugcraft/tags/entity_type/jugtania_cocoon'/(name+'.json'),read(D/'tags/entity_types/cocoon'/(name+'.json')))
 blacklist=read(D/'tags/entity_types/shaded_mesa_blacklist.json')
 native_blacklist={'corporea_spark':'corporea_spark','doppleganger':'botanical_gaia_guardian','flame_ring':'botanical_rod_effect','magic_landmine':'botanical_rod_effect','magic_missile':'botanical_rod_effect','mana_burst':'botanical_mana_burst','pink_wither':'botanical_pink_wither','spark':'botanical_spark','player_mover':'botanical_light_ride'}
 blacklist['values']=sorted(set('jugcraft:'+native_blacklist[v.split(':')[1]] if v.startswith('botania:') else v for v in blacklist['values'] if v!='botania:spark'))
 write(RES/'data/jugcraft/tags/entity_type/jugtania_shaded_mesa_blacklist.json',blacklist)
 for name in ['milkable','no_shearing']:write(RES/'data/jugcraft/tags/entity_type/jugtania_drum'/(name+'.json'),read(D/'tags/entity_types/drum'/(name+'.json')))
 for category in ['items','blocks']:
  for p in (D/'tags'/category).rglob('*.json'):
   entries=[]
   for x in read(p).get('values',[]):
    text=x if isinstance(x,str) else x['id']
    if text.startswith('botania:') and text[8:] not in known:continue
    if text.startswith('#botania:'):entries.append({'id':mapped(text),'required':False})
    elif text.startswith('minecraft:') or text.startswith('#minecraft:') or text.startswith('botania:'):entries.append(mapped(x))
   write(RES/'data/jugcraft/tags'/('item' if category=='items' else 'block')/('jugtania_'+p.relative_to(D/'tags'/category).as_posix()),{'replace':False,'values':entries})
 for n in ['terrasteel']:write(RES/'data/jugcraft/tags/item'/('jugtania_'+n+'_repair.json'),{'values':['jugcraft:jugtania_'+n+'_ingot']})
 for tag,names in {'pickaxes':['terra_pick','glass_pickaxe'],'axes':['elementium_axe','terra_axe'],'swords':['elementium_sword','terra_sword','star_sword','thunder_sword'],'enchantable/durability':['elementium_axe','elementium_sword','terra_pick','terra_axe','terra_sword','glass_pickaxe','star_sword','thunder_sword'],'enchantable/mining':['elementium_axe','terra_pick','terra_axe','glass_pickaxe'],'enchantable/sharp_weapon':['elementium_axe']}.items():merge_tag(RES/'data/minecraft/tags/item'/(tag+'.json'),['jugcraft:jugtania_'+n for n in names],write)
 for p in (D/'tags/worldgen/biome').glob('*.json'):
  aliases={'#forge:is_desert':['minecraft:desert'],'#forge:is_mushroom':['minecraft:mushroom_fields'],'#forge:is_underground':['minecraft:lush_caves','minecraft:dripstone_caves','minecraft:deep_dark'],'#forge:is_plains':['minecraft:plains','minecraft:sunflower_plains'],'#forge:is_swamp':['minecraft:swamp','minecraft:mangrove_swamp'],'#forge:is_coniferous':['#minecraft:is_taiga'],'#forge:is_cold':['minecraft:snowy_plains','minecraft:ice_spikes','minecraft:snowy_taiga','minecraft:frozen_peaks'],'#forge:is_snowy':['minecraft:snowy_plains','minecraft:snowy_taiga','minecraft:snowy_beach','minecraft:snowy_slopes','minecraft:grove','minecraft:jagged_peaks','minecraft:frozen_peaks']}
  values=[]
  for value in read(p).get('values',[]):
   key=value if isinstance(value,str) else value['id']
   values.extend(aliases.get(key,[mapped(value)]))
  write(RES/'data/jugcraft/tags/worldgen/biome'/('jugtania_'+p.name),{'replace':False,'values':values})
 for tag in ['mineable/pickaxe','mineable/axe','flowers','small_flowers','tall_flowers','fences','wooden_fences','fence_gates','walls']:
  entries=['jugcraft:jugtania_'+n for n,d in blocks.items() if (tag=='mineable/pickaxe' and d['kind'] not in ['flower','double_flower'] and 'wood' not in n) or (tag=='mineable/axe' and 'wood' in n) or (tag=='flowers' and d['kind'] in ['flower','double_flower']) or (tag=='small_flowers' and d['kind']=='flower') or (tag=='tall_flowers' and d['kind']=='double_flower') or (tag in ['fences','wooden_fences'] and d['kind']=='fence') or (tag=='fence_gates' and d['kind']=='fence_gate') or (tag=='walls' and d['kind']=='wall')]
  merge_tag(RES/'data/minecraft/tags/block'/(tag+'.json'),entries,write)
 for hand in ['hand','offhand']:merge_tag(RES/'data/trinkets/tags/item'/hand/'ring.json',['jugcraft:jugtania_'+n for n in ['mana_ring','mana_ring_greater','aura_ring','aura_ring_greater']],write)
 for tag,names in {'hand/ring': ['mining_ring', 'water_ring', 'magnet_ring', 'magnet_ring_greater', 'swap_ring', 'reach_ring', 'pixie_ring'], 'offhand/ring': ['mining_ring', 'water_ring', 'magnet_ring', 'magnet_ring_greater', 'swap_ring', 'reach_ring', 'pixie_ring'], 'chest/necklace': ['lava_pendant', 'super_lava_pendant', 'ice_pendant', 'blood_pendant'], 'legs/belt': ['travel_belt', 'super_travel_belt', 'speed_up_belt', 'knockback_belt'], 'chest/wings': ['invisibility_cloak', 'holy_cloak', 'unholy_cloak', 'balance_cloak'], 'head/hat': ['third_eye'], 'legs/charm': ['goddess_charm', 'diva_charm']}.items():merge_tag(RES/'data/trinkets/tags/item'/(tag+'.json'),['jugcraft:jugtania_'+n for n in names],write)
 for tag,names in {'hand/ring':['dodge_ring'],'offhand/ring':['dodge_ring'],'chest/necklace':['cloud_pendant','super_cloud_pendant'],'chest/wings':['flight_tiara']}.items():merge_tag(RES/'data/trinkets/tags/item'/(tag+'.json'),['jugcraft:jugtania_'+n for n in names],write)
 for icon in ['itemfinder_gem','ice_pendant_gem','lava_pendant_gem','super_lava_pendant_gem','cloud_pendant_gem','super_cloud_pendant_gem','blood_pendant_gem','blood_pendant_chain','third_eye_0','third_eye_1','third_eye_2']+['tiara_wing_'+str(i) for i in range(1,9)]:
  model={'type':'minecraft:model','model':'jugcraft:newest/jugtania/icon/'+icon}
  if icon=='blood_pendant_gem':
   source_model=read(O/'models/newest/jugtania/icon'/(icon+'.json'));source_model['elements']=source_model.get('elements',[]);source_model.pop('elements',None);write(O/'models/newest/jugtania/icon'/(icon+'.json'),source_model)
   model['tints']=[{'type':'minecraft:custom_model_data','index':0,'default':16777215}]
  write(O/'items'/('jugtania_worn_'+icon+'.json'),{'model':model})
 for cosmetic in ['monocle']+['cosmetic_'+n for n in COSMETIC_HEAD+COSMETIC_BODY]:write(O/'items'/('jugtania_worn_'+cosmetic+'.json'),{'model':{'type':'minecraft:model','model':'jugcraft:newest/jugtania/item/'+cosmetic,'tints':[{'type':'jugcraft:botanical_cosmetic'}]}})
 write(RES/'jugcraft/newest_jugtania.json',{'blocks':blocks,'items':sorted(ITEMS)})
 write(RES/'data/jugcraft/tags/item/jugtania_manasteel_repair.json',{'values':['jugcraft:jugtania_manasteel_ingot']})
 write(RES/'data/jugcraft/tags/item/jugtania_elementium_repair.json',{'values':['jugcraft:jugtania_elementium_ingot']})
 write(RES/'data/jugcraft/tags/item/jugtania_livingwood_repair.json',{'values':['jugcraft:jugtania_livingwood_twig']})
 for tag,names in {'pickaxes':['manasteel_pick','elementium_pickaxe'],'axes':['manasteel_axe'],'shovels':['manasteel_shovel','elementium_shovel'],'hoes':['manasteel_hoe','elementium_hoe'],'swords':['manasteel_sword','soulscribe'],'enchantable/bow':['livingwood_bow','crystal_bow'],'enchantable/durability':['manasteel_pick','manasteel_axe','manasteel_shovel','manasteel_hoe','manasteel_sword','manasteel_shears','soulscribe','ender_dagger','livingwood_bow','crystal_bow','elementium_pickaxe','elementium_shovel','elementium_hoe','elementium_shears'],'enchantable/mining':['manasteel_pick','manasteel_axe','manasteel_shovel','manasteel_hoe','manasteel_shears','elementium_pickaxe','elementium_shovel','elementium_hoe','elementium_shears']}.items():
  merge_tag(RES/'data/minecraft/tags/item'/(tag+'.json'),['jugcraft:jugtania_'+n for n in names],write)
 for p in (S/'data').glob('*/loonium_config/*.json'):
  namespace=p.parents[1].name
  write(RES/'data/jugcraft/jugtania_loonium_config'/namespace/p.name,read(p))
 for name in ['abandoned_mineshaft','desert_pyramid','jungle_temple','stronghold_corridor']:
  write(RES/'data/jugcraft/loot_table/jugtania_inject'/(name+'.json'),mapped(read(D/'loot_tables/inject'/(name+'.json'))))
 climbable=RES/'data/minecraft/tags/block/climbable.json';climbing=read(climbable)
 climbing['values']=[v for v in climbing['values'] if v!='jugcraft:jugtania_solid_vines'];write(climbable,climbing)
 merge_tag(climbable,['jugcraft:jugtania_solid_vine'],write)
 merge_tag(RES/'data/minecraft/tags/block/dirt.json',['jugcraft:jugtania_enchanted_soil']+['jugcraft:jugtania_'+n+'_grass' for n in ISLANDS[4:]],write)
 for folder in ['loonium','equipment/loonium']:
  for p in (D/'loot_tables'/folder).rglob('*.json'):
   write(RES/'data/jugcraft/loot_table'/('jugtania_'+p.relative_to(D/'loot_tables').as_posix()),encounter_loot(read(p)))
 write(RES/'data/jugcraft/worldgen/feature/botanical_flowers.json',{'type':'jugcraft:botanical_flowers'})
 flower_placement=read(D/'worldgen/placed_feature/mystical_flowers.json')
 flower_placement['feature']='jugcraft:botanical_flowers'
 write(RES/'data/jugcraft/worldgen/placed_feature/botanical_flowers.json',flower_placement)
 sounds=read(O/'sounds.json');supplied_sounds=read(A/'sounds.json')
 for name,entry in supplied_sounds.items():
  entry=deepcopy_module.deepcopy(entry);converted=[]
  for sound in entry['sounds']:
   if isinstance(sound,str):sound={'name':sound}
   sound=deepcopy_module.deepcopy(sound);name_value=sound['name']
   if name_value.startswith('botania:'):sound['name']='jugcraft:'+('jugtania_' if sound.get('type')=='event' else 'newest/jugtania/')+name_value[8:]
   converted.append(sound)
  entry['sounds']=converted;sounds['jugtania_'+name]=entry
 for sound in (A/'sounds').rglob('*'):
  if sound.is_file():copy(sound,O/'sounds/newest/jugtania'/sound.relative_to(A/'sounds'))
 write(O/'sounds.json',sounds);write(RES/'jugcraft/newest_jugtania_sounds.json',{'sounds':sorted(supplied_sounds)})
 lang=read(O/'lang/en_us.json');source=read(A/'lang/en_us.json')
 for key,value in source.items():
  if key.startswith('botania.subtitle.'):lang[key]=value
 write(O/'models/newest/jugtania/item/soulscribe.json',{'parent':'jugcraft:newest/jugtania/item/ender_dagger'})
 write(O/'items/jugtania_soulscribe.json',{'model':{'type':'minecraft:model','model':'jugcraft:newest/jugtania/item/soulscribe'}})
 for n in sorted(known):
  prefix='block' if n in blocks else 'item';lang[prefix+'.jugcraft.jugtania_'+n]=source.get(prefix+'.botania.'+n,n.replace('_',' ').title())
 for key in ['feather_feet','allure','soul_cross','emptiness','bloodthirst']:lang['effect.jugcraft.jugtania_'+key]=key.replace('_',' ').title()
 lang['encyclopedia.jugcraft.page.botanical']='Botanical Workshops'
 lang['encyclopedia.jugcraft.page.botanical_flowers']='Working Flowers'
 write(O/'lang/en_us.json',lang)
 print(f'Jugtania resources written for {len(blocks)} blocks and {len(ITEMS)} items; not a runtime validation.')
if __name__=='__main__':main()
