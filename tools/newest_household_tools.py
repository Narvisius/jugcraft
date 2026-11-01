"""Original key/lock and bubble-tool resources for their native gameplay adapters."""
from newest_building import ROOT,SOURCE,RES,read,write,map_value,copy
A=SOURCE/'SUPPS/assets/supplementaries';O=RES/'assets/jugcraft'
def main():
 d=read(RES/'jugcraft/newest_supps.json');lang=read(O/'lang/en_us.json')
 d['blocks']['gravel_bricks']['kind']='crumbly'
 d['blocks']['suspicious_gravel_bricks']={'source':'suspicious_gravel_bricks','collection':'newest_supps','kind':'supps','function':'hidden_bricks','capacity':1,'wood':False,'properties':{},'shapes':{'':[[0,0,0,16,16,16]]}}
 write(O/'blockstates/supps_suspicious_gravel_bricks.json',{'variants':{'':{'model':'jugcraft:newest/supps/block/suspicious_gravel_bricks'}}})
 write(O/'items/supps_suspicious_gravel_bricks.json',{'model':{'type':'minecraft:model','model':'jugcraft:newest/supps/item/suspicious_gravel_bricks'}})
 write(RES/'data/jugcraft/loot_table/blocks/supps_suspicious_gravel_bricks.json',{'type':'minecraft:block','pools':[]})
 write(RES/'data/jugcraft/recipe/supps_hidden_item.json',{'type':'jugcraft:supps_hidden_item'})
 lang['block.jugcraft.supps_suspicious_gravel_bricks']='Suspicious Gravel Bricks'
 for name,function,kind,capacity,props in [
  ('pulley_block','pulley','supps',1,{'axis':['x','y','z'],'flipped':['false','true'],'winding':['none','rope','chain']}),
  ('slidy_block','slidy','supps',0,{'on_pressure_plate':['false','true']}),
  ('moving_pulley_block','assembly_motion','assembly_motion',0,{})]:
  d['blocks'][name]={'source':name,'collection':'newest_supps','kind':kind,'function':function,'capacity':capacity,'wood':False,'properties':props,'shapes':{'':[[0,0,0,16,16,16]]}}
  if name=='moving_pulley_block':
   write(O/'blockstates'/('supps_'+name+'.json'),{'variants':{'':{'model':'jugcraft:newest/empty'}}});continue
  write(O/'blockstates'/('supps_'+name+'.json'),map_value(read(A/'blockstates'/(name+'.json')),'supplementaries','supps',True))
  write(O/'items'/('supps_'+name+'.json'),{'model':{'type':'minecraft:model','model':'jugcraft:newest/supps/item/'+name}})
  write(RES/'data/jugcraft/loot_table/blocks'/('supps_'+name+'.json'),{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:supps_'+name}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
  lang['block.jugcraft.supps_'+name]='Pulley' if name=='pulley_block' else 'Sliding Block'
 write(RES/'data/jugcraft/recipe/supps_pulley_block.json',{'type':'minecraft:crafting_shaped','pattern':['PIP','IRI','PIP'],'key':{'P':'#minecraft:planks','I':'minecraft:iron_ingot','R':'jugcraft:supps_rope'},'result':{'id':'jugcraft:supps_pulley_block'}})
 write(RES/'data/jugcraft/recipe/supps_slidy_block.json',{'type':'minecraft:crafting_shaped','pattern':['NTN','TTT','NTN'],'key':{'N':'minecraft:iron_nugget','T':'minecraft:tuff'},'result':{'id':'jugcraft:supps_slidy_block'}})
 d['blocks']['lock_block']={'source':'lock_block','collection':'newest_supps','kind':'key_lock','function':'key_lock','capacity':0,'wood':False,'properties':{'powered':['false','true']}}
 d['blocks']['gunpowder']={'source':'gunpowder','collection':'newest_supps','kind':'supps','function':'fuse','capacity':0,'wood':False,'properties':{**{n:['none','side','up'] for n in ['north','east','south','west']},'burning':[str(i) for i in range(9)]},'shapes':{'':[[0,0,0,16,1,16]]}}
 write(O/'blockstates/supps_gunpowder.json',map_value(read(A/'blockstates/gunpowder.json'),'supplementaries','supps',True))
 write(RES/'data/jugcraft/loot_table/blocks/supps_gunpowder.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'minecraft:gunpowder'}],'conditions':[{'condition':'minecraft:survives_explosion'},{'condition':'minecraft:block_state_property','block':'jugcraft:supps_gunpowder','properties':{'burning':'0'}}]}]})
 lang['block.jugcraft.supps_gunpowder']='Gunpowder Fuse'
 d['blocks']['bubble_block']={'source':'bubble_block','collection':'newest_supps','kind':'supps','function':'bubble','capacity':0,'wood':False,'properties':{},'shapes':{'':[[0,0,0,16,16,16]]}}
 write(O/'blockstates/supps_lock_block.json',map_value(read(A/'blockstates/lock_block.json'),'supplementaries','supps',True))
 write(O/'blockstates/supps_bubble_block.json',{'variants':{'':{'model':'jugcraft:newest/empty'}}})
 for n in ['key','lock_block','bubble_blower']:
  write(O/'items'/('supps_'+n+'.json'),{'model':{'type':'minecraft:model','model':'jugcraft:newest/supps/item/'+n}})
  lang[('block' if n=='lock_block' else 'item')+'.jugcraft.supps_'+n]=n.replace('_',' ').title()
 write(O/'items/supps_bubble_blower.json',{'model':{'type':'minecraft:condition','property':'minecraft:using_item','on_true':{'type':'minecraft:model','model':'jugcraft:newest/supps/item/bubble_blower_using'},'on_false':{'type':'minecraft:model','model':'jugcraft:newest/supps/item/bubble_blower'}}})
 write(RES/'data/jugcraft/loot_table/blocks/supps_lock_block.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:supps_lock_block'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
 write(RES/'data/jugcraft/loot_table/blocks/supps_bubble_block.json',{'type':'minecraft:block','pools':[]})
 write(RES/'data/jugcraft/recipe/supps_key.json',{'type':'minecraft:crafting_shaped','pattern':[' G ',' N ',' N '],'key':{'G':'minecraft:gold_ingot','N':'minecraft:gold_nugget'},'result':{'id':'jugcraft:supps_key'}})
 write(RES/'data/jugcraft/recipe/supps_lock_block.json',{'type':'minecraft:crafting_shaped','pattern':['IPI','PRP','IPI'],'key':{'I':'minecraft:iron_ingot','P':'#minecraft:planks','R':'minecraft:redstone'},'result':{'id':'jugcraft:supps_lock_block'}})
 write(RES/'data/jugcraft/recipe/supps_bubble_blower.json',{'type':'minecraft:crafting_shaped','pattern':[' NN',' NN','I  '],'key':{'I':'minecraft:iron_ingot','N':'minecraft:iron_nugget'},'result':{'id':'jugcraft:supps_bubble_blower'}})
 write(RES/'data/jugcraft/recipe/supps_bubble_blower_fill.json',{'type':'jugcraft:supps_bubble_fill'})
 ench=read(SOURCE/'SUPPS/data/supplementaries/enchantment/stasis.json');ench['effects']={};ench.pop('exclusive_set',None);ench['slots']=['mainhand','offhand'];ench['supported_items']='#jugcraft:enchantable/supps_stasis';ench['description']={'translate':'enchantment.jugcraft.supps_stasis'}
 write(RES/'data/jugcraft/enchantment/supps_stasis.json',ench);write(RES/'data/jugcraft/tags/item/enchantable/supps_stasis.json',{'values':['jugcraft:supps_bubble_blower','jugcraft:supps_slingshot']})
 write(RES/'data/jugcraft/tags/enchantment/supps_prevents_bubble_pop.json',{'values':['minecraft:feather_falling']})
 # Source stasis is an enchanting-table/book enchantment; preserve a survival route.
 for tag in ['in_enchanting_table','tradeable','on_random_loot']:
  p=RES/'data/minecraft/tags/enchantment'/(tag+'.json');v=read(p) if p.exists() else {'values':[]}
  if 'jugcraft:supps_stasis' not in v['values']:v['values'].append('jugcraft:supps_stasis')
  write(p,v)
 sounds=read(O/'sounds.json')
 for event,source_event in [('bubble_blow','item.bubble_blower'),('bubble_pop','block.bubble_block.break'),('bubble_place','block.bubble_block.place')]:
  original=read(A/'sounds.json').get(source_event)
  if original:
   sounds['supps_'+event]=map_value(original,'supplementaries','supps',True)
   for spec in original.get('sounds',[]):
    name=spec if isinstance(spec,str) else spec.get('name','');name=name.split(':')[-1]
    src=A/'sounds'/(name+'.ogg')
    if src.exists():copy(src,O/'sounds/newest/supps'/(name+'.ogg'))
 write(O/'sounds.json',sounds)
 for event,stem,count in [('supps_suds','suds_',4),('supps_bubble_pop','bubble_block_pop_',7)]:
  write(O/'particles'/(event+'.json'),{'textures':['jugcraft:newest/supps/'+stem+str(i) for i in range(count)]})
  for i in range(count):copy(A/'textures/particle'/(stem+str(i)+'.png'),O/'textures/particle/newest/supps'/(stem+str(i)+'.png'))
 lang['block.jugcraft.supps_bubble_block']='Soap Bubble';lang['enchantment.jugcraft.supps_stasis']='Stasis'
 write(O/'lang/en_us.json',lang);write(RES/'jugcraft/newest_supps.json',d)
if __name__=='__main__':main()
