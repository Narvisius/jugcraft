"""Preserve owner arcane resources and adapt only explicitly implemented native entries."""
from pathlib import Path
import re
from newest_building import SOURCE, RES, CAT, read, write, copy, map_value, merge_tag
from newest_twigs import recipe
from menagerie_data import loot
S=SOURCE/'forbidden aracane'; A=S/'assets/forbidden_arcanus'; D=S/'data/forbidden_arcanus'; O=RES/'assets/jugcraft'
ROOT=Path(__file__).resolve().parents[1]
NATIVE_BLOCKS={'Block':'block','SlabBlock':'slab','StairBlock':'stairs','WallBlock':'wall','PressurePlateBlock':'plate','ButtonBlock':'button','PillarBlock':'pillar','RotatedPillarBlock':'pillar','MysterywoodLogBlock':'mystery_log','DropExperienceBlock':'ore','TransparentBlock':'glass','IronBarsBlock':'pane','LanternBlock':'lantern','DoorBlock':'door','TrapDoorBlock':'trapdoor','FenceBlock':'fence','FenceGateBlock':'gate','LadderBlock':'ladder','ChainBlock':'chain','MagicalFarmlandBlock':'farmland','SoullessSandBlock':'soulless_sand'}
NATIVE_ITEMS={'Item','PickaxeItem','ShovelItem','SwordItem','AxeItem','HoeItem','ArmorItem'}
NATIVE_BLOCKS.update(HephaestusForgeBlock='workshop',PedestalBlock='workshop')
NATIVE_BLOCKS['MagnetizedPedestalBlock']='workshop'
NATIVE_BLOCKS['StellaArcanumBlock']='stella'
NATIVE_BLOCKS.update(LeavesBlock='leaves',SaplingBlock='sapling',FlowerBlock='flower',FlowerPotBlock='pot')
NATIVE_ITEMS.update({'XpetrifiedOrbItem','SoulExtractorItem','AurealBottleItem','BloodTestTubeItem','MundabiturDustItem'})
NATIVE_ITEMS.update({'FerrogneticMixtureItem','SmelterPrismItem'})
NATIVE_BLOCKS.update(EdelwoodLogBlock='edelwood_log',CarvedEdelwoodLogBlock='carved_edelwood',EdelwoodBranchBlock='edelwood_branch',HugeMushroomBlock='mushroom_cap',UtremJarBlock='jar',EssenceUtremJarBlock='jar')
NATIVE_ITEMS.add('EdelwoodOilItem')
NATIVE_BLOCKS.update(GrowingEdelwoodBlock='edelwood_plant',FungyssBlock='fungyss')
NATIVE_ITEMS.update({'ArcaneBoneMealItem','DarkSoulItem'})
NATIVE_ITEMS.add('QuantumCatcherItem')
NATIVE_BLOCKS['ArcaneCrystalObeliskBlock']='obelisk'
NATIVE_ITEMS.update({'MagicWandItem','DracoArcanusScepterItem'})
NATIVE_ITEMS.add('ModBoatItem')
NATIVE_ITEMS.add('SpectralEyeAmuletItem')
NATIVE_ITEMS.update({'WhirlwindPrismItem','ModArrowItem','SplashAurealBottleItem','DarkMatterItem'})
NATIVE_BLOCKS.update(WhirlwindBlock='whirlwind',UpwindBlock='upwind',BlackHoleBlock='black_hole',HeavyCoreBlock='heavy_core')
NATIVE_ITEMS.update({'CapacityBucketItem','CapacityMilkBucketItem','SolidCapacityBucketItem'})
NATIVE_BLOCKS.update(MortarBlock='mortar',QuantumCoreBlock='block',ArcaneDragonEggBlock='arcane_egg')
NATIVE_BLOCKS['QuantumInjectorBlock']='injector'
NATIVE_BLOCKS['ObsidianSkullBlock']='skull'
NATIVE_BLOCKS.update(ClibanoCoreBlock='clibano_core',ClibanoMainPartBlock='clibano_main',ClibanoCenterBlock='clibano_center',ClibanoSideBlock='clibano_side',ClibanoCornerBlock='clibano_corner')
SKULLS=['obsidian','cracked_obsidian','fragmented_obsidian','fading_obsidian','aurealic_obsidian','eternal_obsidian']
SOULS=['lost_soul','corrupt_lost_soul','enchanted_lost_soul']
def mapped(v,resource=False):return map_value(v,'forbidden_arcanus','forbidden',resource)
def declarations(file):
 result={}
 for line in file.read_text().splitlines():
  match=re.search(r'HELPER.register\("([^"]+)".*?new (\w+)\(',line)
  if match:result[match[1]]=(match[2],line)
 return result
def main():
 for form,name in [('ingots','deorum_ingot'),('nuggets','deorum_nugget')]:
  write(RES/'data/c/tags/item'/form/'deorum.json',{'values':['jugcraft:forbidden_'+name]})
 refs=ROOT/'build/owner-oct29-reference/com/stal111/forbidden_arcanus/core/init'
 block_refs=declarations(refs/'ModBlocks.java');item_refs=declarations(refs/'ModItems.java')
 # Native generated/handheld wrappers for supplied sprites without model exports.
 for name in ['omega_arcoin']+['reinforced_deorum_'+tool for tool in ['axe','hoe','pickaxe','shovel','sword']]:
  tool=name.startswith('reinforced_deorum_')
  write(O/'models/newest/forbidden/item'/(name+'.json'),{'parent':'minecraft:item/handheld' if tool else 'minecraft:item/generated','textures':{'layer0':'jugcraft:newest/forbidden/item/'+('tool/' if tool else '')+name}})
  write(O/'items'/('forbidden_'+name+'.json'),{'model':{'type':'minecraft:model','model':'jugcraft:newest/forbidden/item/'+name}})
 for name in SKULLS:
  block_refs[name+'_skull']=('ObsidianSkullBlock','.withItem()')
  block_refs[name+'_wall_skull']=('ObsidianSkullBlock','')
 blocks={name:{'kind':NATIVE_BLOCKS[cls],'source_class':cls,'wood':any(w in name for w in ['aurum','edelwood','fungyss']),'light':15 if 'lantern' in name and 'soul' not in name else 10 if 'lantern' in name else 0} for name,(cls,line) in block_refs.items() if cls in NATIVE_BLOCKS}
 blocks={name:d for name,d in blocks.items() if d['kind']!='pot' or name.replace('potted_','') in blocks}
 for name,d in blocks.items():
  line=block_refs[name][1];d['cutout']='CUTOUT' in line or 'noOcclusion' in line;d['no_item']='.withItem()' not in line
  strength=re.search(r'\.strength\(([\d.]+)f(?:, ([\d.]+)f)?\)',line)
  if strength:d.update(strength=float(strength[1]),resistance=float(strength[2] or strength[1]))
  resistance=re.search(r'\.explosionResistance\(([\d.]+)f\)',line)
  if resistance:d['resistance']=float(resistance[1])
 # Plain ingredients are original ingredients, not substitutions for custom tools.
 items={name:{'source_class':cls} for name,(cls,line) in item_refs.items() if cls in NATIVE_ITEMS and '.food(' not in line and name not in blocks}
 items['apply_modifier_smithing_template']={'source_class':'Item'}
 for color in ['white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black']:items[color+'_quantum_catcher']={'source_class':'QuantumCatcherItem'}
 for name in ['bat_wing','bat_soup','tentacle','cooked_tentacle']:items[name]={'source_class':'Food'}
 for name,d in items.items():
  line=item_refs.get(name,('',''))[1];stack=re.search(r'\.stacksTo\((\d+)\)',line)
  if stack:d['stack']=int(stack[1])
  d['fireproof']='.fireResistant()' in line
 known=set(blocks)|set(items)
 for name in SOULS:write(RES/'data/jugcraft/loot_table/entities'/('forbidden_'+name+'.json'),loot(mapped(read(D/'loot_table/entities'/(name+'.json')))))
 sounds=read(A/'sounds.json');runtime_sounds=read(O/'sounds.json')
 for name,data in sounds.items():
  data=dict(data);data['sounds']=[mapped(s,True) if isinstance(s,str) else {**s,'name':mapped(s['name'],s.get('type')!='event')} for s in data.get('sounds',[])];runtime_sounds['forbidden_'+name]=data
 write(O/'sounds.json',runtime_sounds)
 for path in (A/'sounds').rglob('*'):
  if path.is_file():copy(path,O/'sounds/newest/forbidden'/path.relative_to(A/'sounds'))
 for folder in ['models','textures']:
  for path in (A/folder).rglob('*'):
   if not path.is_file():continue
   target=O/folder/'newest/forbidden'/path.relative_to(A/folder)
   if path.suffix=='.json':
    v=read(path);v.pop('overrides',None);v.pop('render_type',None);write(target,mapped(v,True))
   else:copy(path,target)
 for name,d in blocks.items():
  if (A/'blockstates'/(name+'.json')).exists():write(O/'blockstates'/('forbidden_'+name+'.json'),mapped(read(A/'blockstates'/(name+'.json')),True))
  path=D/'loot_table/blocks'/(name+'.json')
  if path.exists():write(RES/'data/jugcraft/loot_table/blocks'/('forbidden_'+name+'.json'),loot(mapped(read(path))))
 for name in sorted(known):
  path=A/'models/item'/(name+'.json')
  if path.exists():write(O/'items'/('forbidden_'+name+'.json'),{'model':{'type':'minecraft:model','model':'jugcraft:newest/forbidden/item/'+name}})
 for name in SKULLS:
  write(O/'items'/('forbidden_'+name+'_skull.json'),{'model':{'type':'minecraft:special','base':'minecraft:item/template_skull','model':{'type':'jugcraft:arcane_skull','kind':name}}})
  for suffix in ['_skull','_wall_skull']:
   write(RES/'data/jugcraft/loot_table/blocks'/('forbidden_'+name+suffix+'.json'),loot({'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:forbidden_'+name+'_skull','functions':[{'function':'minecraft:copy_components','source':'block_entity','include':['jugcraft:arcane_skull_ticks']}]}]}]}))
 # Formed parts return the one original construction block that was broken.
 write(O/'models/newest/forbidden/block/clibano_empty.json',{'textures':{'particle':'jugcraft:newest/forbidden/block/polished_darkstone'},'elements':[]})
 write(O/'blockstates/forbidden_clibano_main_part.json',{'variants':{'':{'model':'jugcraft:newest/forbidden/block/clibano_empty'}}})
 write(O/'blockstates/forbidden_whirlwind.json',{'variants':{'':{'model':'jugcraft:newest/forbidden/block/whirlwind'}}})
 for name in ['upwind','black_hole']:
  write(O/'blockstates'/('forbidden_'+name+'.json'),{'variants':{'':{'model':'jugcraft:newest/forbidden/block/clibano_empty'}}})
 for name in ['clibano_corner','clibano_side_horizontal','clibano_side_vertical','clibano_center','clibano_main_part']:
  entries=[]
  if name=='clibano_corner':entries=[{'type':'minecraft:item','name':'jugcraft:forbidden_polished_darkstone'}]
  elif name!='clibano_main_part':
   if name=='clibano_center':
    entries=[{'type':'minecraft:alternatives','children':[{'type':'minecraft:item','name':'jugcraft:forbidden_clibano_core','conditions':[{'condition':'minecraft:any_of','terms':[{'condition':'minecraft:block_state_property','block':'jugcraft:forbidden_clibano_center','properties':{'type':kind}} for kind in ['front_off','front_fire','front_soul_fire','front_enchanted_fire']]}]},{'type':'minecraft:item','name':'jugcraft:forbidden_polished_darkstone_bricks'}]}]
   else:entries=[{'type':'minecraft:item','name':'jugcraft:forbidden_polished_darkstone_bricks'}]
  write(RES/'data/jugcraft/loot_table/blocks'/('forbidden_'+name+'.json'),loot({'type':'minecraft:block','pools':[{'rolls':1,'entries':entries}] if entries else []}))
 # Original jar shell and animated liquid textures; sixteen visible fill levels.
 jarparts=[{'apply':{'model':'jugcraft:newest/forbidden/block/utrem_jar'}}]
 for kind,liquid in [(0,'aureal'),(2,'blood'),(3,'experience')]:
  for fill in range(1,17):
   model=f'newest/forbidden/block/jar_fill_{kind}_{fill}'
   write(O/'models'/(model+'.json'),{'textures':{'still':f'jugcraft:newest/forbidden/block/liquid/{liquid}_still','flow':f'jugcraft:newest/forbidden/block/liquid/{liquid}_flow'},'elements':[{'from':[3.5,.5,3.5],'to':[12.5,.5+12*fill/16,12.5],'faces':{face:{'texture':'#still' if face in ['up','down'] else '#flow','uv':[0,0,16,16]} for face in ['up','down','north','south','east','west']}}]})
   jarparts.append({'when':{'essence':str(kind),'fill':str(fill)},'apply':{'model':'jugcraft:'+model}})
 for name in ['utrem_jar','essence_utrem_jar']:
  write(O/'blockstates'/('forbidden_'+name+'.json'),{'multipart':jarparts})
  write(O/'items'/('forbidden_'+name+'.json'),{'model':{'type':'minecraft:model','model':'jugcraft:newest/forbidden/item/utrem_jar'}})
  write(RES/'data/jugcraft/loot_table/blocks'/('forbidden_'+name+'.json'),loot({'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:forbidden_essence_utrem_jar','functions':[{'function':'minecraft:copy_components','source':'block_entity','include':['jugcraft:forbidden_essence_storage']}]}]}]}))
 shell={'type':'minecraft:model','model':'jugcraft:newest/forbidden/item/utrem_jar'}
 entries=[]
 for kind in range(4):
  entries.append({'threshold':17*kind,'model':shell})
  for fill in range(1,17):
   if kind==1:
    entries.append({'threshold':17*kind+fill,'model':{'type':'minecraft:composite','models':[shell,{'type':'minecraft:special','base':'jugcraft:newest/forbidden/item/utrem_jar','model':{'type':'jugcraft:arcane_jar_souls'}}]}});continue
   entries.append({'threshold':17*kind+fill,'model':{'type':'minecraft:composite','models':[shell,{'type':'minecraft:model','model':f'jugcraft:newest/forbidden/block/jar_fill_{kind}_{fill}'}]}})
 write(O/'items/forbidden_essence_utrem_jar.json',{'model':{'type':'minecraft:range_dispatch','property':'jugcraft:arcane_jar_fill','entries':entries,'fallback':shell}})
 for family in ['draco_arcanus','tyr','mortem']:
  layers={}
  for layer,number in [('humanoid',1),('humanoid_leggings',2)]:
   copy(A/f'textures/models/armor/{family}_layer_{number}.png',O/f'textures/entity/equipment/{layer}/forbidden_{family}.png')
   layers[layer]=[{'texture':'jugcraft:forbidden_'+family}]
   overlay=A/f'textures/models/armor/{family}_layer_{number}_overlay.png'
   if overlay.exists():
    copy(overlay,O/f'textures/entity/equipment/{layer}/forbidden_{family}_overlay.png');layers[layer].append({'texture':'jugcraft:forbidden_'+family+'_overlay'})
  write(O/'equipment'/('forbidden_'+family+'.json'),{'layers':layers})
 def available(v):
  if isinstance(v,str):return not v.startswith('forbidden_arcanus:') or v.split(':',1)[1] in known
  if isinstance(v,list):return all(available(x) for x in v)
  if isinstance(v,dict):return all(available(x) for k,x in v.items() if k not in ['type','group','tag','magic_circle','enhancers','random_sequence'])
  return True
 recipes=[]
 for path in (D/'recipe').rglob('*.json'):
  v=read(path)
  if v.get('type','').startswith('minecraft:') and available(v) and 'neoforge:' not in str(v):write(RES/'data/jugcraft/recipe/forbidden'/path.relative_to(D/'recipe'),recipe(mapped(v)));recipes.append(path.relative_to(D/'recipe').as_posix())
 write(RES/'data/jugcraft/recipe/forbidden/aureal_bottle.json',{'type':'jugcraft:arcane_aureal_bottle'})
 for modifier in ['eternal','aquatic','demolishing','fiery','magnetized','soulbound']:write(RES/f'data/jugcraft/recipe/forbidden/apply_{modifier}_modifier.json',{'type':'jugcraft:arcane_modifier','modifier':modifier})
 rituals=[]
 combustion=[]
 for path in (D/'recipe/clibano_combustion').glob('*.json'):
  v=read(path)
  if available(v):write(RES/'data/jugcraft/arcane_combustion'/path.name,mapped(v));combustion.append(path.stem)
 for path in (D/'forbidden_arcanus/hephaestus_forge/ritual').glob('*.json'):
  v=read(path)
  if available(v):
   write(RES/'data/jugcraft/arcane_ritual'/path.name,mapped(v));rituals.append(path.stem)
 features=[]
 write(RES/'data/jugcraft/worldgen/feature/forbidden_edelwood.json',{'type':'jugcraft:arcane_growth'})
 for mega in [False,True]:
  for variant in range(2):write(RES/'data/jugcraft/worldgen/feature'/f'forbidden_{"mega" if mega else "big"}_fungyss_{variant}.json',{'type':'jugcraft:arcane_growth','fungus':True,'mega':mega})
 def provider(v):
  if isinstance(v,dict):
   if 'Name' in v:return {'id':v['Name'],**({'properties':v['Properties']} if v.get('Properties') else {})}
   if v.get('type')=='minecraft:simple_state_provider':return provider(v['state'])
   v={k:provider(x) for k,x in v.items()}
   if v.get('type')=='minecraft:weighted_state_provider':v['type']='minecraft:weighted'
  elif isinstance(v,list):return [provider(x) for x in v]
  return v
 aurum=provider(read(D/'worldgen/configured_feature/aurum.json')['config']);aurum.pop('dirt_provider',None);aurum.pop('force_dirt',None);aurum['below_trunk_provider']='minecraft:soil_beneath_tree';aurum['type']='minecraft:tree'
 write(RES/'data/jugcraft/worldgen/feature/forbidden_aurum.json',mapped(aurum));features.append('aurum')
 for path in (D/'worldgen/configured_feature').glob('ore_*.json'):
  v=read(path)
  if not available(v):continue
  v={'type':v['type'],**v['config']}
  for target in v['targets']:
   state=target['state'];props=state.get('Properties',{});target['state']=state['Name']+('['+','.join(k+'='+val for k,val in props.items())+']' if props else '')
  write(RES/'data/jugcraft/worldgen/feature'/('forbidden_'+path.name),mapped(v));features.append(path.stem)
 placements=[];vegetation=[]
 for path in (D/'worldgen/placed_feature').glob('*.json'):
  v=read(path)
  if v['feature'].split(':',1)[-1] in features:
   write(RES/'data/jugcraft/worldgen/placed_feature'/('forbidden_'+path.name),mapped(v))
   (vegetation if v['feature'].endswith(':aurum') else placements).append(path.stem)
 write(RES/'data/jugcraft/worldgen/feature/forbidden_flower_yellow_orchid.json',{'type':'minecraft:simple_block','to_place':{'id':'jugcraft:forbidden_yellow_orchid'}})
 orchid=mapped(read(D/'worldgen/placed_feature/yellow_orchid.json'))
 orchid['placement'].extend([{'type':'minecraft:count','count':64},{'type':'minecraft:offset',**{axis:{'type':'minecraft:trapezoid','min':-spread,'max':spread,'plateau':0} for axis,spread in [('x',6),('y',2),('z',6)]}},{'type':'minecraft:block_predicate_filter','predicate':{'type':'minecraft:matching_block_tag','tag':'minecraft:air'}}])
 write(RES/'data/jugcraft/worldgen/placed_feature/forbidden_yellow_orchid.json',orchid);vegetation.append('yellow_orchid')
 # Supplied Fungyss has no placement hook. Add a sparse underground survival source.
 write(RES/'data/jugcraft/worldgen/feature/forbidden_wild_fungyss.json',{'type':'minecraft:simple_block','to_place':{'id':'jugcraft:forbidden_fungyss'}})
 write(RES/'data/jugcraft/worldgen/placed_feature/forbidden_wild_fungyss.json',{'feature':'jugcraft:forbidden_wild_fungyss','placement':[{'type':'minecraft:rarity_filter','chance':12},{'type':'minecraft:in_square'},{'type':'minecraft:height_range','height':{'type':'minecraft:uniform','min_inclusive':{'absolute':-40},'max_inclusive':{'absolute':40}}},{'type':'minecraft:environment_scan','direction_of_search':'down','max_steps':12,'target_condition':{'type':'minecraft:solid'},'allowed_search_condition':{'type':'minecraft:matching_block_tag','tag':'minecraft:air'}},{'type':'minecraft:offset','x':0,'y':1,'z':0},{'type':'minecraft:block_predicate_filter','predicate':{'type':'minecraft:matching_block_tag','tag':'minecraft:air'}},{'type':'minecraft:biome'}]})
 placements.append('wild_fungyss')
 write(RES/'data/jugcraft/tags/entity_type/forbidden_test_tube_blacklisted.json',mapped(read(D/'tags/entity_type/test_tube_blacklisted.json')))
 injections=[]
 def table_targets(v):
  if isinstance(v,dict):
   if v.get('condition')=='neoforge:loot_table_id':return [v['loot_table_id']]
   return [target for x in v.values() for target in table_targets(x)]
  if isinstance(v,list):return [target for x in v for target in table_targets(x)]
  return []
 for path in (D/'loot_modifiers').glob('*.json'):
  v=read(path)
  if v.get('type')!='neoforge:add_table':continue
  original=D/'loot_table'/(v['table'].split(':',1)[1]+'.json');content=read(original)
  if not available(content):continue
  conditions=[c for c in v.get('conditions',[]) if not table_targets(c)]
  for pool in content.get('pools',[]):pool.setdefault('conditions',[]).extend(conditions)
  target='forbidden_inject/'+path.stem
  write(RES/'data/jugcraft/loot_table'/(target+'.json'),loot(mapped(content)))
  for vanilla in table_targets(v):injections.append({'target':vanilla,'table':'jugcraft:'+target})
 for category in ['block','item','enchantment','entity_type']:
  for path in (D/'tags'/category).rglob('*.json'):
   v=read(path);v['values']=[mapped(x) for x in v.get('values',[]) if isinstance(x,str) and (not x.startswith('forbidden_arcanus:') or x.split(':',1)[1] in ((set(SOULS)|{'boom_arrow','draco_arcanus_arrow','aureal_bottle'}) if category=='entity_type' else known))]
   write(RES/'data/jugcraft/tags'/category/('forbidden_'+path.relative_to(D/'tags'/category).as_posix()),v)
 for tag,values in {'forbidden_draco_repair':['dragon_scale'],'forbidden_deorum_repair':['stellarite_piece'],'forbidden_tyr_repair':['golden_dragon_scale','aquatic_dragon_scale'],'forbidden_mortem_repair':[]}.items():write(RES/'data/jugcraft/tags/item'/(tag+'.json'),{'values':['jugcraft:forbidden_'+n for n in values]})
 for tag,kind in [('slabs','slab'),('stairs','stairs'),('walls','wall'),('fences','fence'),('fence_gates','gate'),('doors','door'),('trapdoors','trapdoor'),('buttons','button')]:merge_tag(RES/'data/minecraft/tags/block'/(tag+'.json'),['jugcraft:forbidden_'+n for n,d in blocks.items() if d['kind']==kind],write)
 for tag,wood in [('mineable/axe',True),('mineable/pickaxe',False)]:merge_tag(RES/'data/minecraft/tags/block'/(tag+'.json'),['jugcraft:forbidden_'+n for n,d in blocks.items() if d['wood']==wood and d['kind'] not in ['farmland','soulless_sand']],write)
 merge_tag(RES/'data/minecraft/tags/item/arrows.json',['jugcraft:forbidden_boom_arrow','jugcraft:forbidden_draco_arcanus_arrow'],write)
 for tag,suffix in [('swords','_sword'),('axes','_axe'),('pickaxes','_pickaxe'),('shovels','_shovel'),('hoes','_hoe')]:merge_tag(RES/'data/minecraft/tags/item'/(tag+'.json'),['jugcraft:forbidden_'+n for n in items if n.endswith(suffix)],write)
 for tag,kind in [('leaves','leaves'),('saplings','sapling'),('small_flowers','flower'),('flower_pots','pot')]:
  merge_tag(RES/'data/minecraft/tags/block'/(tag+'.json'),['jugcraft:forbidden_'+n for n,d in blocks.items() if d['kind']==kind],write)
  if kind!='pot':merge_tag(RES/'data/minecraft/tags/item'/(tag+'.json'),['jugcraft:forbidden_'+n for n,d in blocks.items() if d['kind']==kind],write)
 merge_tag(RES/'data/minecraft/tags/block/logs.json',['jugcraft:forbidden_'+n for n,d in blocks.items() if d['wood'] and d['kind']=='pillar' and n.endswith(('_log','_wood'))],write)
 for tag in ['mineable/hoe','hoe_efficient']:merge_tag(RES/'data/minecraft/tags/block'/(tag+'.json'),['jugcraft:forbidden_'+n for n,d in blocks.items() if d['kind']=='leaves'],write)
 lang=read(O/'lang/en_us.json')
 for key,value in read(A/'lang/en_us.json').items():
  if key.startswith(('block.forbidden_arcanus.','item.forbidden_arcanus.','entity.forbidden_arcanus.')):
   head,name=key.split('.forbidden_arcanus.',1)
   if name in known or name in SOULS:lang[head+'.jugcraft.forbidden_'+name]=value
 lang.update({'item.jugcraft.forbidden_blood_test_tube': 'Blood Test Tube', 'item.jugcraft.forbidden_edelwood_powder_snow_bucket': 'Edelwood Bucket of Powder Snow', 'block.jugcraft.forbidden_essence_utrem_jar': 'Essence Utrem Jar', 'item.jugcraft.forbidden_omega_arcoin': 'Omega Arcoin', 'block.jugcraft.forbidden_hephaestus_forge_tier_1': 'Hephaestus Forge (Tier 1)', 'block.jugcraft.forbidden_hephaestus_forge_tier_2': 'Hephaestus Forge (Tier 2)', 'block.jugcraft.forbidden_hephaestus_forge_tier_3': 'Hephaestus Forge (Tier 3)', 'block.jugcraft.forbidden_hephaestus_forge_tier_4': 'Hephaestus Forge (Tier 4)', 'block.jugcraft.forbidden_hephaestus_forge_tier_5': 'Hephaestus Forge (Tier 5)', 'item.jugcraft.forbidden_reinforced_deorum_axe': 'Reinforced Deorum Axe', 'item.jugcraft.forbidden_reinforced_deorum_hoe': 'Reinforced Deorum Hoe', 'item.jugcraft.forbidden_reinforced_deorum_pickaxe': 'Reinforced Deorum Pickaxe', 'item.jugcraft.forbidden_reinforced_deorum_shovel': 'Reinforced Deorum Shovel', 'item.jugcraft.forbidden_reinforced_deorum_sword': 'Reinforced Deorum Sword'})
 write(O/'lang/en_us.json',lang)
 enhancers={p.stem:mapped(read(p)) for p in (D/'forbidden_arcanus/enhancer/definition').glob('*.json')}
 residues={p.stem:mapped(read(p)) for p in (D/'forbidden_arcanus/residue_type').glob('*.json') if available(read(p))}
 write(RES/'jugcraft/newest_forbidden.json',{'blocks':blocks,'items':items,'ore_placements':placements,'vegetation':vegetation,'enhancers':enhancers,'loot_injections':injections,'sounds':list(sounds),'residues':residues,'combustion_recipes':combustion})
 disabled=read(CAT/'source-registration-reconciliation.json')['forbidden']
 write(CAT/'forbidden-local.json',{'implemented_native_blocks':blocks,'implemented_native_items':items,'recipes':recipes,'rituals':rituals,'ore_placements':placements,'loot_injections':injections,'sourceDisabledEntries':disabled,'remaining_blocks':sorted(({p.stem for p in (A/'blockstates').glob('*.json')}|set(block_refs))-set(blocks)-set(disabled)),'remaining_items':sorted(set(item_refs)-set(items)-set(blocks)-set(disabled)),'validated':False})
 print(f'Generated native arcane subset: {len(blocks)} blocks, {len(items)} ingredients/tools/armor; custom devices are still in progress. No runtime validation.')
if __name__=='__main__':main()
