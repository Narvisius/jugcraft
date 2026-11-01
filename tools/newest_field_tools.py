"""Owner-supplied ranged tools, their original models/sounds, and reachable recipes."""
from newest_building import SOURCE,RES,read,write,copy,map_value
from menagerie_data import loot
A=SOURCE/'SUPPS/assets/supplementaries';O=RES/'assets/jugcraft'
def model(n):return {'type':'minecraft:model','model':'jugcraft:newest/supps/item/'+n}
def main():
 lang=read(O/'lang/en_us.json');sounds=read(O/'sounds.json')
 d=read(RES/'jugcraft/newest_supps.json');d['blocks']['pignata_anchor']={'source':'pignata_anchor','collection':'newest_supps','kind':'supps','function':'pignata_anchor','capacity':0,'wood':False,'properties':{'waterlogged':['false','true']},'shapes':{'':[]}};write(RES/'jugcraft/newest_supps.json',d)
 write(O/'blockstates/supps_pignata_anchor.json',{'variants':{'':{'model':'jugcraft:newest/empty'}}})
 write(RES/'data/jugcraft/loot_table/blocks/supps_pignata_anchor.json',{'type':'minecraft:block','pools':[]})
 for n in ['quiver','slingshot','bomb','bomb_blue','slice_map','candy','antique_ink','hat_stand','pignata','dispenser_minecart','dragon_banner_pattern','blast_armor_trim_smithing_template','music_disc_heave_ho']:
  write(O/'items'/('supps_'+n+'.json'),{'model':model(n)});lang['item.jugcraft.supps_'+n]=n.replace('_',' ').title()
 # Original undyed/dyed overlays and native worn-slot art.
 for name in ['quiver_in_hand_dyed','quiver_gui_dyed']:
  write(O/'models/newest/supps/item'/(name+'.json'),map_value(read(A/'models/special_models/item'/(name+'.json')),'supplementaries','supps',True))
 def quiver(dyed):
  def m(n):
   result=model(n+('_dyed' if dyed else ''))
   if dyed:result['tints']=[{'type':'minecraft:constant','value':-1},{'type':'minecraft:dye','default':0xA06540}]
   return result
  return {'type':'minecraft:select','property':'minecraft:display_context','cases':[{'when':['gui','ground','fixed'],'model':m('quiver_gui')}],'fallback':m('quiver_in_hand')}
 write(O/'items/supps_quiver.json',{'model':{'type':'minecraft:condition','property':'minecraft:has_component','component':'minecraft:dyed_color','on_true':quiver(True),'on_false':quiver(False)}})
 write(RES/'data/trinkets/entities/jugcraft_supps_quiver.json',{'entities':['player'],'slots':['legs/quiver']})
 write(RES/'data/trinkets/slots/legs/quiver.json',{'icon':'jugcraft:newest/supps/gui/slots/quiver','amount':1,'validator_predicates':['trinkets:default'],'cosmetic_slots':False})
 write(RES/'data/trinkets/tags/item/legs/quiver.json',{'values':['jugcraft:supps_quiver']})
 path=RES/'data/minecraft/tags/item/dyeable.json';tag=read(path) if path.exists() else {'values':[]};tag['values']=list(dict.fromkeys(tag['values']+['jugcraft:supps_quiver']));write(path,tag)
 pulling={'type':'minecraft:range_dispatch','property':'minecraft:use_duration','scale':.05,'fallback':model('slingshot_pulling_0'),'entries':[{'threshold':.55,'model':model('slingshot_pulling_1')},{'threshold':.9,'model':model('slingshot_pulling_2')}]}
 write(O/'items/supps_slingshot.json',{'model':{'type':'minecraft:condition','property':'minecraft:using_item','on_true':pulling,'on_false':model('slingshot')}})
 for n in ['bomb','bomb_blue']:
  entries=[{'threshold':1,'model':model(n+'_projectile')}]
  if n=='bomb_blue':entries.append({'threshold':2,'model':model('bomb_blue_baby')})
  write(O/'items'/('supps_'+n+'.json'),{'model':{'type':'minecraft:range_dispatch','property':'minecraft:custom_model_data','index':0,'fallback':model(n),'entries':entries}})
 for name,source in [('slingshot_charge','item.slingshot.charge_0'),('slingshot_shoot','item.slingshot.release'),('bomb_explode','explosion.bomb'),('heave_ho','music_disc.heave_ho')]:
  d=read(A/'sounds.json')[source];sounds['supps_'+name]=map_value(d,'supplementaries','supps',True)
  for spec in d['sounds']:
   raw=spec if isinstance(spec,str) else spec['name'];tail=raw.split(':')[-1];src=A/'sounds'/(tail+'.ogg')
   if src.exists():copy(src,O/'sounds/newest/supps'/(tail+'.ogg'))
 for n in ['slingshot','bomb','candy','slice_map']:
  d=read(SOURCE/'SUPPS/data/supplementaries/recipe'/(n+'.json'));d.pop('fabric:load_conditions',None);d.pop('neoforge:conditions',None)
  def ingredient(v):return '#'+v['tag'] if 'tag' in v else v.get('item',v)
  if 'key' in d:d['key']={k:ingredient(v) for k,v in d['key'].items()}
  if 'ingredients' in d:d['ingredients']=[ingredient(v) for v in d['ingredients']]
  d=map_value(d,'supplementaries','supps')
  if n=='slice_map':d['ingredients']=['jugcraft:supps_altimeter','minecraft:map']
  write(RES/'data/jugcraft/recipe'/('supps_'+n+'.json'),d)
 write(RES/'data/jugcraft/recipe/supps_quiver.json',{'type':'minecraft:crafting_shaped','pattern':['LSL','L L','LLL'],'key':{'L':'minecraft:leather','S':'minecraft:string'},'result':{'id':'jugcraft:supps_quiver'}})
 write(RES/'data/jugcraft/recipe/supps_bomb_blue.json',{'type':'minecraft:crafting_shapeless','ingredients':['jugcraft:supps_bomb','minecraft:lapis_lazuli','minecraft:gunpowder','minecraft:blaze_powder'],'result':{'id':'jugcraft:supps_bomb_blue'}})
 for tag in ['crossbow','durability']:
  path=RES/('data/minecraft/tags/item/enchantable/'+tag+'.json');data=read(path) if path.exists() else {'replace':False,'values':[]}
  if 'jugcraft:supps_slingshot' not in data['values']:data['values'].append('jugcraft:supps_slingshot')
  write(path,data)
 write(RES/'data/jugcraft/tags/item/supps_slingshot_blacklist.json',{'values':['minecraft:bedrock','minecraft:barrier','minecraft:command_block','minecraft:chain_command_block','minecraft:repeating_command_block','minecraft:structure_block','minecraft:jigsaw']})
 for source in (A/'font').rglob('*.json'):
  write(O/'font/newest/supps'/source.relative_to(A/'font'),map_value(read(source),'supplementaries','supps',True))
 sounds=read(O/'sounds.json');source_sounds=read(A/'sounds.json')
 for event in ['pignata_hit','pignata_burst']:
  source='entity.pignata.'+event.removeprefix('pignata_');entry=source_sounds[source];names=[]
  for sound in entry['sounds']:
   name=sound if isinstance(sound,str) else sound['name'];rel=name.split(':')[-1];copy(A/'sounds'/(rel+'.ogg'),O/'sounds/newest/supps'/(rel+'.ogg'));names.append('jugcraft:newest/supps/'+rel)
  sounds['supps_'+event]={'sounds':names}
 write(O/'sounds.json',sounds)
 for name in ['hat_stand','pignata']:copy(A/'textures/entity'/(name+'.png'),O/'textures/newest/supps/entity'/(name+'.png'))
 write(RES/'data/jugcraft/recipe/supps_antique_ink.json',{'type':'minecraft:crafting_shapeless','ingredients':['minecraft:ink_sac','minecraft:brown_dye','minecraft:honeycomb'],'result':{'id':'jugcraft:supps_antique_ink'}})
 write(RES/'data/jugcraft/recipe/supps_antique_book.json',{'type':'jugcraft:supps_antique_book'})
 write(RES/'data/jugcraft/recipe/supps_hat_stand.json',{'type':'minecraft:crafting_shaped','pattern':[' P ',' S ','LLL'],'key':{'P':'#minecraft:planks','S':'minecraft:stick','L':'#minecraft:wooden_slabs'},'result':{'id':'jugcraft:supps_hat_stand'}})
 write(RES/'data/jugcraft/recipe/supps_pignata.json',{'type':'minecraft:crafting_shaped','pattern':['PPP','P P','L L'],'key':{'P':'minecraft:paper','L':'minecraft:leather'},'result':{'id':'jugcraft:supps_pignata'}})
 write(RES/'data/jugcraft/recipe/supps_dispenser_minecart.json',{'type':'minecraft:crafting_shapeless','ingredients':['minecraft:minecart','minecraft:dispenser'],'result':{'id':'jugcraft:supps_dispenser_minecart'}})
 write(RES/'data/jugcraft/jukebox_song/supps_heave_ho.json',{'comparator_output':14,'description':{'text':'Heave Ho!'},'length_in_seconds':149,'sound_event':'jugcraft:supps_heave_ho'})
 write(RES/'data/jugcraft/banner_pattern/supps_dragon.json',{'asset_id':'jugcraft:supps_dragon','translation_key':'block.jugcraft.banner.supps_dragon'})
 write(RES/'data/jugcraft/tags/banner_pattern/supps_pattern/dragon.json',{'values':['jugcraft:supps_dragon']})
 for kind in ['banner','shield']:copy(A/'textures/entity'/kind/'dragon.png',O/'textures/entity'/kind/'supps_dragon.png')
 write(RES/'data/jugcraft/trim_pattern/supps_blast.json',{'asset_id':'jugcraft:supps_blast','decal':False,'description':{'text':'Blast'}})
 for original,target in [('blast','humanoid'),('blast_leggings','humanoid_leggings')]:
  dest=O/f'textures/trims/entity/{target}/supps_blast.png';copy(A/'textures/trims/models/armor'/(original+'.png'),dest);write(dest.with_suffix('.png.mcmeta'),{'palette':{'base_palette':'minecraft:trim_base'}})
 write(RES/'data/jugcraft/recipe/supps_blast_trim.json',{'type':'minecraft:smithing_trim','template':'jugcraft:supps_blast_armor_trim_smithing_template','base':'#minecraft:trimmable_armor','addition':'#minecraft:trim_materials','pattern':'jugcraft:supps_blast'})
 write(RES/'data/jugcraft/recipe/supps_blast_trim_copy.json',{'type':'minecraft:crafting_shaped','pattern':['DTD','DND','DDD'],'key':{'D':'minecraft:diamond','T':'jugcraft:supps_blast_armor_trim_smithing_template','N':'minecraft:netherrack'},'result':{'id':'jugcraft:supps_blast_armor_trim_smithing_template','count':2}})
 for color in ['white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black']:lang['block.jugcraft.banner.supps_dragon.'+color]=color.replace('_',' ').title()+' Dragon'

 copy(A/'textures/entity/plunderer.png',O/'textures/newest/supps/entity/plunderer.png')
 lang['entity.jugcraft.supps_plunderer']='Plunderer';lang['item.jugcraft.supps_plunderer_spawn_egg']='Plunderer Spawn Egg'
 write(O/'items/supps_plunderer_spawn_egg.json',{'model':{'type':'minecraft:model','model':'minecraft:item/vindicator_spawn_egg'}})
 for name in ['idle','hurt','death','celebrate']:
  d=read(A/'sounds.json')['entity.plunderer.'+('ambient' if name=='idle' else name)];sounds['supps_plunderer_'+name]=map_value(d,'supplementaries','supps',True)
  for spec in d['sounds']:
   raw=spec if isinstance(spec,str) else spec['name'];tail=raw.split(':')[-1];src=A/'sounds'/(tail+'.ogg')
   if src.exists():copy(src,O/'sounds/newest/supps'/(tail+'.ogg'))
 for wood in ['oak','spruce','birch','jungle','acacia','dark_oak','mangrove','cherry','bamboo']:
  n='cannon_boat_'+wood;texture=('cannon_raft_' if wood=='bamboo' else 'cannon_boat_')+wood
  write(O/'models/newest/supps/item'/(n+'.json'),{'parent':'minecraft:item/generated','textures':{'layer0':'jugcraft:newest/supps/item/cannon_boat/'+texture}})
  write(O/'items'/('supps_'+n+'.json'),{'model':model(n)})
  write(RES/'data/jugcraft/recipe'/('supps_'+n+'.json'),{'type':'minecraft:crafting_shapeless','ingredients':['minecraft:'+wood+('_raft' if wood=='bamboo' else '_boat'),'jugcraft:supps_cannon'],'result':{'id':'jugcraft:supps_'+n}})
  lang['item.jugcraft.supps_'+n]=wood.replace('_',' ').title()+' Cannon Boat';lang['entity.jugcraft.supps_'+n]=lang['item.jugcraft.supps_'+n]
 write(RES/'data/jugcraft/loot_table/entities/supps_plunderer.json',loot({'type':'minecraft:entity','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'minecraft:spyglass'}],'conditions':[{'condition':'minecraft:killed_by_player'},{'condition':'minecraft:random_chance','chance':.0025}]}]}))
 write(O/'lang/en_us.json',lang);write(O/'sounds.json',sounds)
if __name__=='__main__':main()
