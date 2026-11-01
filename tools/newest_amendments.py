"""Adapt the supplied Amendments placement meshes; native mechanics live in OwnerAmendments."""
from pathlib import Path
import json
from newest_building import SOURCE,RES,read,write,copy,map_value,state_properties
from newest_shapes import variants
from newest_twigs import recipe
S=SOURCE/'amends';A=S/'assets/amendments';O=RES/'assets/jugcraft'
NAMES=['tool_hook','hanging_flower_pot','wall_lantern','wall_soul_lantern','carpet_slab','carpet_stairs','directional_cake','double_cake','dye_cauldron','liquid_cauldron','skull_pile','skull_candle','skull_candle_wall','water_lily_pad']
COLORS=['white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black']
NAMES+=['ceiling_banner_'+color for color in COLORS]
def asset(v):return map_value(v,'amendments','amends',True)
def model_part(part):
 """Source custom loaders accept either a model ID or an inline model object."""
 if isinstance(part,str):return {"parent":part}
 if isinstance(part,dict):return dict(part)
 raise ValueError("Unsupported inline model component")

def main():
 for folder in ['textures','models']:
  for p in (A/folder).rglob('*'):
   if not p.is_file():continue
   dest=O/folder/'newest/amends'/p.relative_to(A/folder)
   if p.suffix!='.json':copy(p,dest);continue
   v=read(p);loader=v.get('loader','')
   if loader=='amendments:tool_hook':v=model_part(v['hook'])
   elif loader=='amendments:wall_lantern':v=model_part(v['support'])
   elif loader=='amendments:hanging_pot':v=v['rope']
   elif loader=='amendments:carpet_overlay':v=v['carpet']
   elif loader=='amendments:waterlogged_lily':v={'textures':{'particle':'minecraft:block/lily_pad','pad':'minecraft:block/lily_pad'},'elements':[{'from':[0,15.99,0],'to':[16,15.99,16],'faces':{face:{'uv':[0,0,16,16],'texture':'#pad','tintindex':0} for face in ['up','down']}}]}
   write(dest,asset(v))
 for p in (A/'textures/particle').glob('*.png'):copy(p,O/'textures/particle/newest/amends'/p.name)
 for name in ['fireball_trail','dragon_fireball_trail','fireball_explosion','boiling_bubble']:write(O/'particles'/('amends_'+name+'.json'),asset(read(A/'particles'/(name+'.json'))))
 sounds=read(O/'sounds.json')
 for i in range(4):copy(A/'sounds'/('fireball_impact_'+str(i)+'.ogg'),O/'sounds/newest/amends'/('fireball_impact_'+str(i)+'.ogg'))
 sounds['amends_fireball_impact']={'sounds':['jugcraft:newest/amends/fireball_impact_'+str(i) for i in range(4)]};write(O/'sounds.json',sounds)
 heat=read(S/'data/amendments/tags/block/cauldron_heat_source.json')
 heat['values'] += [{'id':'jugcraft:supp_fire_pit','required':False},{'id':'jugcraft:supp_blaze_rod','required':False}]
 write(RES/'data/jugcraft/tags/block/amends_cauldron_heat_source.json',heat)
 write(RES/'data/jugcraft/damage_type/amends_boiling.json',dict(read(S/'data/amendments/damage_type/boiling.json'),message_id='jugcraft.amends_boiling'))
 write(RES/'data/jugcraft/tags/block/amends_bubble_column_passable.json',read(S/'data/amendments/tags/block/bubble_column_passable.json'))
 write(RES/'data/jugcraft/tags/entity_type/amends_pets_follow_teleports_whitelist.json',read(S/'data/amendments/tags/entity_type/pets_follow_teleports_whitelist.json'))
 definitions={}
 for name in NAMES:
  state=read(A/'blockstates'/f'{name}.json');props=state_properties(state)
  if name.startswith('skull') or name.startswith('ceiling_banner'):
   props['rotation']={str(i) for i in range(16)}
   if name.endswith('wall'):props['facing']={'north','south','east','west'}
   if name.startswith('skull_candle'):props.update({'candles':{'1','2','3','4'},'lit':{'false','true'},'waterlogged':{'false','true'}})
   # The native block-entity renderer supplies component-aware heads/banner patterns.
   state={'variants':{'':{'model':'jugcraft:newest/amends/block/empty'}}}
   write(O/'models/newest/amends/block/empty.json',{'textures':{'particle':'minecraft:block/oak_planks'},'elements':[]})
  if name=='hanging_flower_pot':props['waterlogged']={'false','true'}
  if name in ['tool_hook','wall_lantern','wall_soul_lantern']:props['waterlogged']={'false','true'}
  if 'cake' in name:props['waterlogged']={'false','true'}
  if name.endswith('cauldron'):
   props={'level':{'1','2','3'}};state={'variants':{}}
   kinds=['default'] if name=='dye_cauldron' else ['default','mushroom_stew','rabbit_stew','beetroot_soup','suspicious_stew']
   if name=='liquid_cauldron':props['kind']=set(kinds)
   for kind in kinds:
    for level,y in [(1,9),(2,12),(3,15)]:
     key=name+'_native_'+str(level)+('' if kind=='default' else '_'+kind);model=read(A/'models/block/cauldron.json');fluid=read(A/'models/block'/('cauldron_level_'+str(y)+'.json'))
     for el in fluid['elements']:
      for face in el.get('faces',{}).values():face['tintindex']=0
     model['elements']+=fluid['elements'];model['textures']['content']='jugcraft:newest/amends/block/'+('dye_still' if name=='dye_cauldron' else 'potion_cauldron' if kind=='default' else kind+'_cauldron')
     write(O/'models/newest/amends/block'/(key+'.json'),asset(model));state['variants']['level='+str(level)+(',kind='+kind if name=='liquid_cauldron' else '')]={'model':'jugcraft:newest/amends/block/'+key}
  if name.startswith('carpet'):
   new={}
   for color in COLORS:
    for key,v in state['variants'].items():
     v=dict(v);source=v['model'].split('/')[-1];v['model']=f'jugcraft:newest/amends/block/{source}_{color}'
     new[(key+',' if key else '')+'color='+color]=v
     write(O/'models/newest/amends/block'/f'{source}_{color}.json',{'parent':'jugcraft:newest/amends/block/'+source+'_overlay','textures':{'side':'minecraft:block/'+color+'_wool','particle':'minecraft:block/'+color+'_wool'}})
   state={'variants':new}
  definitions[name]={'source':name,'collection':'newest_amends','kind':'amends','properties':{k:sorted(v) for k,v in props.items()},'shapes':variants(A,'amendments',read(A/'blockstates'/f'{name}.json'),props)}
  write(O/'blockstates'/f'amends_{name}.json',asset(state))
  write(RES/'data/jugcraft/loot_table/blocks'/f'amends_{name}.json',{'type':'minecraft:block','pools':[]})
 write(O/'items/amends_dragon_charge.json',{'model':{'type':'minecraft:model','model':'jugcraft:newest/amends/item/dragon_charge'}})
 v=read(S/'data/amendments/recipe/dragon_charge.json');v.pop('fabric:load_conditions',None);v.pop('neoforge:conditions',None);write(RES/'data/jugcraft/recipe/amends_dragon_charge.json',recipe(map_value(v,'amendments','amends')))
 write(O/'items/amends_dye_bottle.json',{'model':{'type':'minecraft:model','model':'jugcraft:newest/amends/item/dye_bottle','tints':[{'type':'minecraft:dye','default':16777215}]}})
 write(RES/'data/jugcraft/recipe/amends_dye_bottle.json',{'type':'jugcraft:amends_dye_bottle'})
 write(RES/'jugcraft/newest_amends.json' ,{'blocks':definitions})
 lang=read(O/'lang/en_us.json');lang['item.jugcraft.amends_dye_bottle']='Dye Bottle';lang['item.jugcraft.amends_dragon_charge']='Dragon Charge'
 lang['death.attack.jugcraft.amends_boiling']='%1$s boiled away'
 lang['death.attack.jugcraft.amends_boiling.player']='%1$s boiled away while escaping %2$s'
 for name in NAMES:lang['block.jugcraft.amends_'+name]=name.replace('_',' ').title()
 write(O/'lang/en_us.json',lang)
if __name__=='__main__':main()
