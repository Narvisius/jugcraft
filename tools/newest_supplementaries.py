"""Integrate supplied Supplementaries meshes/recipes for native implemented block families."""
from pathlib import Path
import json
from newest_building import ROOT,SOURCE,RES,read,write,copy,map_value,state_properties
from newest_shapes import variants
from newest_twigs import recipe
S=SOURCE/'SUPPS';A=S/'assets/supplementaries';O=RES/'assets/jugcraft'
FUNCTIONS={'sack':'storage','safe':'safe','lunch_basket':'lunch','present':'storage','trapped_present':'trapped','urn':'storage','item_shelf':'display','pedestal':'display','crystal_display':'redstone_display','notice_board':'notice','clock_block':'clock','crank':'crank','turn_table':'turntable','hourglass':'hourglass'}
BASES=['ash_bricks','blackstone_tile','checker','fine_wood','gravel_bricks','lapis_bricks','stone_tile']
DECOR={'ashen_basalt','daub','daub_brace','daub_cross_brace','daub_frame','timber_brace','timber_cross_brace','timber_frame','feather_block','flint_block','sugar_cube','flax_block','gold_bars','wicker_fence','gold_door','gold_trapdoor','netherite_door','netherite_trapdoor'}
for base in BASES:
 for suffix in ['','_block','_slab','_stairs','_wall']:
  if (A/'blockstates'/(base+suffix+'.json')).exists():DECOR.add(base+suffix)
COLORS=['white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black']
for color in COLORS:
 for base in ['sack','present','trapped_present']:
  if (A/'blockstates'/(base+'_'+color+'.json')).exists():FUNCTIONS[base+'_'+color]='trapped' if base=='trapped_present' else 'storage'
FUNCTIONS.update({'flower_box':'flowers','faucet':'faucet','planter':'planter','wind_vane':'wind','relayer':'relayer','redstone_illuminator':'illuminator','stone_lamp':'lamp','deepslate_lamp':'lamp','end_stone_lamp':'rune_lamp','blackstone_lamp':'lamp','bamboo_spikes':'spikes','book_pile':'books','book_pile_horizontal':'books','speaker_block':'speaker','doormat':'doormat','statue':'statue','goblet':'goblet','sconce_lever':'sconce_lever','jar':'jar','cage':'cage','blackboard':'blackboard','bellows':'bellows','spring_launcher':'spring','spring_launcher_arm':'spring_arm','spring_launcher_head':'spring_head'})
for color in COLORS:FUNCTIONS['flag_'+color]='flag'
for name in ['timber_frame','timber_brace','timber_cross_brace']:FUNCTIONS[name]='frame'
for suffix in ['', '_soul', '_ender', '_glow', '_green', '_nether_brass']:
 for name in ['sconce'+suffix,'sconce_wall'+suffix]:
  if (A/'blockstates'/(name+'.json')).exists():FUNCTIONS[name]='sconce'
RODS={'stick','blaze_rod','breeze_rod','prismarine_rod','edelwood_stick','propelplant_cane'}
TERRAIN={'pancake','ash','fodder','raked_gravel','soap_block','fire_pit','cog_block','grouper','rope','confetti_litter'}
AWNINGS={p.stem for p in (A/'blockstates').glob('awning*.json')}
TERRAIN.update(AWNINGS|RODS)
CANDLES={p.stem for p in (A/'blockstates').glob('candle_holder*.json')}
DECOR.update(CANDLES)
DECOR.update({'flax','wild_flax','iron_gate','gold_gate'}|TERRAIN)
ACTIVE=set(FUNCTIONS)|DECOR
MATERIALS={'flax','flax_seeds','soap','ash_brick','flute','altimeter','confetti_popper','wrench','rope_arrow'}
CAPS={'storage':9,'safe':27,'lunch':6,'trapped':9,'display':1,'notice':1,'clock':0,'crank':0,'turntable':0,'hourglass':1,'redstone_display':0,'flowers':3,'faucet':0,'planter':0,'wind':0,'relayer':0,'illuminator':0,'lamp':0,'rune_lamp':0,'sconce':0,'sconce_lever':0,'frame':1,'jar':12,'cage':0,'blackboard':0,'bellows':0,'spring':0,'spring_arm':0,'spring_head':0,'spikes':1,'books':4,'speaker':0,'doormat':1,'statue':1,'goblet':1,'flag':0}
def reg(v):return map_value(v,'supplementaries','supps')
# Four compatibility rod sprites are absent from the supplied archive. Keep the
# original geometry and use existing material art; do not invent replacement bitmaps.
ROD_MATERIAL_TEXTURES={
 'jugcraft:newest/supps/block/prismarine_rod':'minecraft:block/prismarine',
 'jugcraft:newest/supps/block/prismarine_rod_cross':'minecraft:block/prismarine',
 'jugcraft:newest/supps/block/edelwood_stick':'jugcraft:newest/forbidden/block/edelwood_log/edelwood_log',
 'jugcraft:newest/supps/block/edelwood_stick_cross':'jugcraft:newest/forbidden/block/edelwood_log/edelwood_log',
}
def material_textures(value):
 if isinstance(value,str):return ROD_MATERIAL_TEXTURES.get(value,value)
 if isinstance(value,list):return [material_textures(v) for v in value]
 if isinstance(value,dict):return {k:material_textures(v) for k,v in value.items()}
 return value
def asset(v):
 result=map_value(v,'supplementaries','supps',True)
 if isinstance(result,dict) and 'textures' in result:result['textures']=material_textures(result['textures'])
 return result
def refs(v):
 if isinstance(v,str):return {v} if v.startswith('jugcraft:supps_') else set()
 if isinstance(v,list):return set().union(*(refs(x) for x in v))
 if isinstance(v,dict):return set().union(*(refs(x) for x in v.values()))
 return set()
def hourglass_data(p):
 original=read(p);value=reg(original)
 if 'texture' in original:value['texture']=asset(original['texture'])
 return value

def main():
 for folder in ['textures','models']:
  for p in (A/folder).rglob('*'):
   if not p.is_file():continue
   dest=O/folder/'newest/supps'/p.relative_to(A/folder)
   if p.suffix=='.json':
    data=read(p);loader=data.get('loader','')
    if loader=='supplementaries:extra_rotation':
     data.pop('loader',None)
     for e in data.get('elements',[]):
      if 'extra_rotation' in e:e.setdefault('rotation',{'axis':'x','origin':[8,8,8]})['angle']=e.get('rotation',{}).get('angle',0)+e.pop('extra_rotation')
    if loader=='supplementaries:book_pile':data={'textures':data.get('textures',{})}
    if data.get('parent')=='minecraft:block/template_farmland':
     data['parent']='minecraft:block/template_cube_bottom_top_indented';tex=data.setdefault('textures',{});dirt=tex.pop('dirt','minecraft:block/gravel');tex['bottom']=dirt;tex['side']=dirt
    if loader in {'supplementaries:faucet','supplementaries:flower_box','supplementaries:jar','supplementaries:goblet','supplementaries:frame_block','supplementaries:blackboard'}:
     data={'parent':data.get('model',data.get('box',data.get('overlay',data.get('frame')))),'textures':data.get('textures',{})}
    if p.parent.name=='item' and p.stem in {'jar','jar_tinted','cage'}:data={**data,'parent':'supplementaries:block/'+('jar' if p.stem=='jar_tinted' else p.stem)}
    if p.parent.name=='item' and p.stem=='flag_black':data['parent']='minecraft:block/block'
    write(dest,asset(data))
   else:copy(p,dest)
 tag=RES/'data/minecraft/tags/block/candles.json';candles=read(tag) if tag.exists() else {'replace':False,'values':[]}
 for name in sorted(CANDLES):
  if 'jugcraft:supps_'+name not in candles['values']:candles['values'].append('jugcraft:supps_'+name)
 write(tag,candles)
 sounds=read(O/'sounds.json')
 for name in ['bellows_blow','bellows_retract','soap_wash']:
  category='item' if name=='soap_wash' else 'block'
  copy(A/'sounds'/category/(name+'.ogg'),O/'sounds/newest/supps'/category/(name+'.ogg'))
  sounds['supps_'+name]={'sounds':['jugcraft:newest/supps/'+category+'/'+name]}
 for i in range(4):copy(A/'sounds/block'/('awning_bounce_'+str(i)+'.ogg'),O/'sounds/newest/supps/block'/('awning_bounce_'+str(i)+'.ogg'))
 sounds['supps_awning_bounce']={'sounds':['jugcraft:newest/supps/block/awning_bounce_'+str(i) for i in range(4)]}
 for i in range(3):copy(A/'sounds/item'/('confetti_popper_'+str(i)+'.ogg'),O/'sounds/newest/supps/item'/('confetti_popper_'+str(i)+'.ogg'))
 sounds['supps_confetti_popper']={'sounds':['jugcraft:newest/supps/item/confetti_popper_'+str(i) for i in range(3)]}
 for p in (A/'textures/particle').glob('confetti*.png'):copy(p,O/'textures/particle/newest/supps'/p.name)
 for name in ['confetti','confetti_dyed']:write(O/'particles'/('supps_'+name+'.json'),asset(read(A/'particles'/(name+'.json'))))
 source_sounds=read(A/'sounds.json')
 for key,source in {'block_rotate':'block.rotate','wrench_rotate':'item.wrench.rotate','wrench_fail':'item.wrench.fail','wrench_hit':'item.wrench.hit'}.items():sounds['supps_'+key]={'sounds':source_sounds[source]['sounds']}
 write(O/'sounds.json',sounds)
 definitions={}
 for name in sorted(ACTIVE):
  state=read(A/'blockstates'/(name+'.json'));props=state_properties(state)
  if name in RODS:state['variants']['axis_y=false,axis_x=false,axis_z=false']={'model':'supplementaries:block/'+name+'_y'}
  if name.startswith('book_pile'):props.update({'books':{'1','2','3','4'},'facing':{'north','south','east','west'}})
  if name in CANDLES:props['waterlogged']={'false','true'}
  if name in FUNCTIONS or name in TERRAIN or name.endswith('_gate'):props['waterlogged']={'false','true'}
  if name=='spring_launcher_arm':props.update({'facing':{'north','south','east','west','up','down'},'extending':{'false','true'}})
  if name=='turn_table':props['power']={str(i) for i in range(16)}
  if name=='bellows':props.update({'facing':{'north','south','east','west','up','down'},'power':{str(i) for i in range(16)}})
  if name.startswith('flag_'):props['facing']={'north','south','east','west'}
  if name=='statue':props.update({'lit':{'true','false'},'powered':{'true','false'}})
  if name=='hourglass':props['light_level']={str(i) for i in range(16)}
  for vs in props.values():
   if vs=={'true'}:vs.add('false')
  function=FUNCTIONS.get(name,'decor');kind='supps' if name in FUNCTIONS else 'block'
  if name in DECOR and name not in FUNCTIONS:
   kind='candle_holder' if name in CANDLES else 'supps_terrain' if name in TERRAIN else 'metal_gate' if name.endswith('_gate') else 'flax' if name=='flax' else 'wild_flax' if name=='wild_flax' else 'slab' if name.endswith('_slab') else 'stairs' if name.endswith('_stairs') else 'wall' if name.endswith('_wall') else 'door' if name.endswith('_door') else 'trapdoor' if name.endswith('_trapdoor') else 'fence' if name=='wicker_fence' else 'bars' if name=='gold_bars' else 'pillar' if 'axis' in props else 'block'
  definition={'source':name,'collection':'newest_supps','kind':kind,'function':function,'capacity':CAPS.get(function,0),'properties':{k:sorted(v) for k,v in props.items()},'wood':any(s in name for s in ['wood','daub','timber','wicker','board','shelf','present','sack'])}
  if kind in {'supps','supps_terrain','candle_holder'}:
   definition['shapes']=variants(A,'supplementaries',state,props)
  definitions[name]=definition;write(O/'blockstates'/('supps_'+name+'.json'),asset(state))
  if not name.startswith('sconce_wall') and name not in {'spring_launcher_head','spring_launcher_arm','book_pile','book_pile_horizontal','confetti_litter'}:write(O/'items'/('supps_'+name+'.json'),{'model':{'type':'minecraft:model','model':'jugcraft:newest/supps/item/'+name}})
  if name=='bamboo_spikes':write(O/'items'/('supps_'+name+'.json'),{'model':{'type':'minecraft:condition','property':'minecraft:has_component','component':'minecraft:potion_contents','on_true':{'type':'minecraft:model','model':'jugcraft:newest/supps/item/bamboo_spikes_tipped','tints':[{'type':'minecraft:constant','value':-1},{'type':'minecraft:potion','default':3694022},{'type':'minecraft:potion','default':3694022}]},'on_false':{'type':'minecraft:model','model':'jugcraft:newest/supps/item/bamboo_spikes'}}})
  if name in {'blackboard','jar','cage','goblet'} or name.startswith('flag_'):write(O/'items'/('supps_'+name+'.json'),{'model':{'type':'minecraft:special','base':'jugcraft:newest/supps/item/'+name,'model':{'type':'jugcraft:supps_contents','name':name}}})
  p=S/'data/supplementaries/loot_table/blocks'/(name+'.json')
  if name in {'spring_launcher_arm','spring_launcher_head','book_pile','book_pile_horizontal','confetti_litter'}:write(RES/'data/jugcraft/loot_table/blocks'/('supps_'+name+'.json'),{'type':'minecraft:block','pools':[]})
  elif name in RODS:write(RES/'data/jugcraft/loot_table/blocks'/('supps_'+name+'.json'),{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:supps_'+name}],'conditions':[{'condition':'minecraft:block_state_property','block':'jugcraft:supps_'+name,'properties':{'axis_'+axis:'true'}},{'condition':'minecraft:survives_explosion'}]} for axis in ['x','y','z']]})
  elif name in FUNCTIONS:write(RES/'data/jugcraft/loot_table/blocks'/('supps_'+name+'.json'),{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:supps_'+name.replace('sconce_wall','sconce')}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
  elif name in CANDLES or name in {'pancake','ash','fodder'}:
   key='candles' if name in CANDLES else 'pancakes' if name=='pancake' else 'layers'
   write(RES/'data/jugcraft/loot_table/blocks'/('supps_'+name+'.json'),{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:supps_'+name,'functions':[{'function':'minecraft:set_count','count':i},{'function':'minecraft:explosion_decay'}]}],'conditions':[{'condition':'minecraft:block_state_property','block':'jugcraft:supps_'+name,'properties':{key:str(i)}}]} for i in range(1,5 if name in CANDLES else 9)]})
  elif p.exists():write(RES/'data/jugcraft/loot_table/blocks'/('supps_'+name+'.json'),reg(read(p)))
 books=[]
 for p in (S/'data/supplementaries/supplementaries/placeable_books').glob('*.json'):
  book=read(p);visual_path=A/'placeable_books_visuals'/(book['book_visuals'].split(':')[-1]+'.json')
  if not visual_path.exists():continue
  visuals=[]
  for variant in read(visual_path).get('variants',[]):
   for model in variant.get('models',[]):
    key='supps_book_visual_'+model['model'].split('/')[-1]
    write(O/'items'/(key+'.json'),{'model':{'type':'minecraft:model','model':asset(model['model'])}})
    visuals.append({'item_model':'jugcraft:'+key,'glint':model.get('has_glint',False)})
  if visuals:books.append({'item':book['item'],'horizontal':book.get('horizontal',False),'power':book.get('enchant_power',0),'visuals':visuals})
 write(RES/'jugcraft/supps_books.json',{'books':books})
 write(RES/'jugcraft/newest_supps.json',{'blocks':definitions,'hourglass':[hourglass_data(p) for p in sorted((S/'data/supplementaries/hourglass_dusts').glob('*.json'))]})
 # Placed rods share ordinary items in the source; our dedicated block items
 # need an inventory model pointing at their original vertical geometry.
 for name in RODS | {'ashen_basalt'}:
  write(O/'models/newest/supps/item'/(name+'.json'),{'parent':'jugcraft:newest/supps/block/'+name+('_y' if name in RODS else '')})
 for name in MATERIALS:write(O/'items'/('supps_'+name+'.json'),{'model':{'type':'minecraft:model','model':'jugcraft:newest/supps/item/'+name}})
 write(RES/'jugcraft/supps_flute_songs.json',{'songs':[read(p) for p in sorted((S/'data/supplementaries/flute_songs').glob('*.json'))]})
 write(O/'items/supps_flute.json',{'model':{'type':'minecraft:select','property':'minecraft:display_context','cases':[{'when':['gui','ground','fixed'],'model':{'type':'minecraft:model','model':'jugcraft:newest/supps/special_models/item/flute_gui'}}],'fallback':{'type':'minecraft:model','model':'jugcraft:newest/supps/special_models/item/flute_in_hand'}}})
 from PIL import Image
 write(RES/'jugcraft/supps_altimeters.json',{p.stem:Image.open(p).height for p in (A/'textures/item/altimeter').glob('minecraft_*.png')})
 write(O/'items/supps_altimeter.json',{'model':{'type':'minecraft:composite','models':[{'type':'minecraft:model','model':'jugcraft:newest/supps/special_models/item/altimeter_template'},{'type':'minecraft:special','base':'jugcraft:newest/supps/special_models/item/altimeter_template','model':{'type':'jugcraft:supps_altimeter'}}]}})
 write(O/'items/supps_confetti_popper.json',{'model':{'type':'minecraft:select','property':'minecraft:display_context','cases':[{'when':'head','model':{'type':'minecraft:model','model':'jugcraft:newest/supps/special_models/item/confetti_popper_head'}}],'fallback':{'type':'minecraft:model','model':'jugcraft:newest/supps/special_models/item/confetti_popper_in_hand'}}})
 write(RES/'data/jugcraft/recipe/supps_rope_arrow_charge.json',{'type':'jugcraft:supps_rope_arrow_charge'})
 arrows=RES/'data/minecraft/tags/item/arrows.json';data=read(arrows) if arrows.exists() else {'replace':False,'values':[]}
 if 'jugcraft:supps_rope_arrow' not in data['values']:data['values'].append('jugcraft:supps_rope_arrow')
 write(arrows,data)
 write(RES/'data/jugcraft/recipe/supps_flag_patterns.json',{'type':'jugcraft:supps_flag_patterns'})
 write(RES/'data/jugcraft/recipe/supps_confetti_popper_dye.json',{'type':'jugcraft:supps_confetti_dye'})
 for p in (S/'data/supplementaries/recipe').rglob('*.json'):
  v=reg(read(p));conditions=v.pop('fabric:load_conditions',[]);v.pop('neoforge:conditions',None)
  if any(c.get('condition')!='jugcraft:supps_flag' for c in conditions):continue
  used=refs(v)
  if used and all(x.removeprefix('jugcraft:supps_') in ACTIVE|MATERIALS for x in used) and v.get('type','').startswith('minecraft:'):write(RES/'data/jugcraft/recipe'/('supps_'+p.relative_to(S/'data/supplementaries/recipe').as_posix()),recipe(v))
 lang=read(O/'lang/en_us.json')
 for k,v in read(A/'lang/en_us.json').items():
  if k.startswith(('block.supplementaries.','item.supplementaries.')) and k.split('.',2)[2] in ACTIVE|MATERIALS:lang[k.replace('.supplementaries.','.jugcraft.supps_')]=v
 lang.update({'block.jugcraft.supps_edelwood_stick': 'Edelwood Stick', 'block.jugcraft.supps_prismarine_rod': 'Prismarine Rod', 'block.jugcraft.supps_propelplant_cane': 'Propelplant Cane'})
 write(O/'lang/en_us.json',lang)
 write(RES/'data/jugcraft/recipe/supps_ash_from_charcoal.json',{'type':'minecraft:smelting','ingredient':'minecraft:charcoal','result':{'id':'jugcraft:supps_ash','count':1},'experience':0.0,'cookingtime':200})
 write(RES/'data/jugcraft/recipe/supps_pancake.json',{'type':'minecraft:crafting_shapeless','ingredients':['minecraft:sugar','minecraft:milk_bucket','minecraft:wheat','minecraft:egg'],'result':{'id':'jugcraft:supps_pancake','count':3}})
 for rod,material in {'stick':'minecraft:stick','blaze_rod':'minecraft:blaze_rod','breeze_rod':'minecraft:breeze_rod','prismarine_rod':'minecraft:prismarine_shard'}.items():
  for reverse in [False,True]:
   source,target=('jugcraft:supps_'+rod,material) if reverse else (material,'jugcraft:supps_'+rod)
   write(RES/'data/jugcraft/recipe'/('supps_'+rod+('_recover' if reverse else '_placeable')+'.json'),{'type':'minecraft:crafting_shapeless','ingredients':[source],'result':{'id':target,'count':1}})
 for name in ['flax','flax_seeds']:
  write(RES/'data/jugcraft/recipe'/('supps_shared_'+name+'.json'),{'type':'minecraft:crafting_shapeless','ingredients':['jugcraft:supps_'+name],'result':{'id':'jugcraft:'+name,'count':1}})
 write(RES/'data/jugcraft/recipe/supps_flax_seed_selection.json',{'type':'minecraft:crafting_shapeless','ingredients':['jugcraft:flax_seeds'],'result':{'id':'jugcraft:supps_flax_seeds','count':1}})
 for namespace in ['supplementaries','c']:
  for p in (S/'data'/namespace/'tags/item').rglob('*.json'):
   values=[]
   for v in reg(read(p)).get('values',[]):
    key=v if isinstance(v,str) else v['id']
    if key.startswith('jugcraft:supps_') and key.removeprefix('jugcraft:supps_') not in ACTIVE|MATERIALS:continue
    if key.startswith('#') or not key.startswith(('minecraft:','jugcraft:supps_')):v={'id':key,'required':False}
    values.append(v)
   if not values:continue
   relative=p.relative_to(S/'data'/namespace/'tags/item').as_posix();dest=RES/'data'/('jugcraft' if namespace=='supplementaries' else namespace)/'tags/item'/('supps_'+relative if namespace=='supplementaries' else relative)
   current=read(dest) if dest.exists() else {'replace':False,'values':[]}
   for value in values:
    if value not in current['values']:current['values'].append(value)
   write(dest,current)
 for p in (S/'data/supplementaries/tags/item').glob('hourglass_*.json'):write(RES/'data/jugcraft/tags/item'/('supps_'+p.name),reg(read(p)))
 for p in (S/'data/supplementaries/hourglass_dusts').glob('*.json'):write(RES/'jugcraft/supps_hourglass'/p.name,hourglass_data(p))
 write(RES/'data/jugcraft/worldgen/feature/supps_wild_flax_patch.json',{'type':'jugcraft:supps_wild_flax_patch'})
 write(RES/'data/jugcraft/worldgen/placed_feature/supps_wild_flax.json',reg(read(S/'data/supplementaries/worldgen/placed_feature/wild_flax.json')))
 # Source refers to has_wild_flax but supplies no such biome tag: native river/plains default.
 write(RES/'data/jugcraft/tags/worldgen/biome/supps_has_wild_flax.json',{'replace':False,'values':['minecraft:plains','minecraft:sunflower_plains','minecraft:river','minecraft:flower_forest']})
 reconciliation=read(ROOT/'art/owner-library/catalog/edited-2026-10-29/source-registration-reconciliation.json')['supplementaries']
 write(ROOT/'art/owner-library/catalog/edited-2026-10-29/supplementaries-local.json',{'implementedRegistrationScope':sorted(ACTIVE),'remainingBlockFamilies':sorted(p.stem for p in (A/'blockstates').glob('*.json') if p.stem not in ACTIVE and (p.stem not in reconciliation or reconciliation[p.stem]['status']=='remaining_active_source_system')),'sourceBlockReconciliation':reconciliation,'status':'Local gameplay/presentation implementation; uncompiled and unvalidated. Reconciled source registrations do not imply runtime completion.'})
 from newest_buntings import main as buntings
 buntings()
 from newest_globes import main as globes
 globes()
 from newest_coastal import main as coastal
 coastal()
 from newest_way_signs import main as way_signs
 way_signs()
 from newest_cannons import main as cannons
 cannons()
 from newest_heads import main as heads
 heads()
 from newest_lumisene import main as lumisene
 lumisene()
if __name__=='__main__':main()

import newest_household_tools
newest_household_tools.main()

import newest_field_tools
newest_field_tools.main()

# Original pirate ships and encounter markers.
if __name__ == "__main__":
 import newest_galleons
 newest_galleons.main()
