"""All supplied chest/spawner/magic-item loot, retaining rolls/weights and supported source filters."""
import json
from menagerie_data import loot

def generate(dump,RES,ART,SPELLS):
 source=ART.parents[1]/'data/irons_spellbooks';reports=[]
 def ident(value):return value.replace('irons_spellbooks:','jugcraft:jimta_spellbooks_')
 def adapt(value):
  if isinstance(value,list):return [adapt(x) for x in value]
  if value=='minecraft:chain':return 'minecraft:iron_chain'
  if isinstance(value,str) and value.startswith('#irons_spellbooks:'):
   return value.replace('#irons_spellbooks:','#jugcraft:jimtafacts/spellbooks/')
  if not isinstance(value,dict):return value
  value=dict(value)
  if set(value)=={'min','max'}:value['type']='minecraft:uniform'
  if value.get('function')=='irons_spellbooks:randomize_spell':
   filt=value.get('spell_filter',{});names=[('angel_wings' if x.split(':')[-1]=='angel_wing' else x.split(':')[-1]) for x in filt.get('spells',SPELLS)]
   if filt.get('school'):names=[n for n in names if SPELLS[n][0]==filt['school'].split(':')[-1]]
   reports.append({'source':value,'native_level':1,'spells':names})
   return {'function':'jugcraft:jimtafacts_spell_scroll','spells':names}
  if value.get('function')=='irons_spellbooks:randomize_ring_enhancement':return {'function':'jugcraft:jimtafacts_ring_enhancement','school':value.get('school',value.get('spell_filter',{}).get('school','')).split(':')[-1]}
  if value.get('function')=='irons_spellbooks:set_furled_map':return {'function':'jugcraft:jimtafacts_furled_map','destination':value['destination'].split(':')[-1]}
  for k,v in list(value.items()):
   if isinstance(v,str) and v.startswith('irons_spellbooks:'):
    if k=='type' and v=='irons_spellbooks:chilled':value[k]='jugcraft:jimtafacts_chilled'
    elif k=='value' or k=='name' and value.get('type')=='minecraft:loot_table':value[k]='jugcraft:jimtafacts/spellbooks/'+v.split(':')[-1]
    elif k=='name' and value.get('type')=='minecraft:tag':value[k]='jugcraft:jimtafacts/spellbooks/'+v.split(':')[-1]
    else:value[k]=ident(v)
   else:value[k]=adapt(v)
  if value.get('name')=='jugcraft:jimta_spellbooks_scroll':value['name']='jugcraft:jimta_spellbooks_scroll_firebolt'
  return value
 count=0
 for path in sorted((source/'loot_table').rglob('*.json'),key=lambda p:p.as_posix()):
  rel=path.relative_to(source/'loot_table')
  if rel.parts[0]=='blocks':
   if path.stem not in ('mithril_ore','deepslate_mithril_ore'):continue
   dump(RES/f'data/jugcraft/loot_table/blocks/jimta_spellbooks_{path.name}',loot(adapt(json.loads(path.read_text(encoding='utf-8')))));continue
  converted=loot(adapt(json.loads(path.read_text(encoding='utf-8'))))
  dump(RES/'data/jugcraft/loot_table/jimtafacts/spellbooks'/rel,converted);count+=1
  if rel.parts[0]=='entities' and not path.stem.startswith('additional_'):dump(RES/f'data/jugcraft/loot_table/entities/jimtafacts_{path.name}',converted)
 for path in sorted((source/'tags/item').rglob('*.json'),key=lambda p:p.as_posix()):
  # Focus and component ingredient tags are the source's own crafting graph.
  rel=path.relative_to(source/'tags/item');data=json.loads(path.read_text(encoding='utf-8'));values=[]
  for x in data.get('values',[]):
   if isinstance(x,str):
    if x.startswith('#irons_spellbooks:'):x=x.replace('#irons_spellbooks:','#jugcraft:jimtafacts/spellbooks/')
    elif x.startswith('irons_spellbooks:'):x=ident(x)
   if isinstance(x,dict) and 'id' in x:x={**x,'id':x['id'].replace('irons_spellbooks:','jugcraft:jimta_spellbooks_')}
   if isinstance(x,dict) and x.get('id')=='jugcraft:jimta_spellbooks_arcane_salvage' and not x.get('required',True):continue
   if x=='#c:ingots/gold':x='minecraft:gold_ingot'
   values.append(x)
  dump(RES/'data/jugcraft/tags/item/jimtafacts/spellbooks'/rel,{'replace':False,'values':values})
 for path in sorted((source/'tags/instrument').rglob('*.json'),key=lambda p:p.as_posix()):
  dump(RES/'data/jugcraft/tags/instrument/jimtafacts/spellbooks'/path.relative_to(source/'tags/instrument'),adapt(json.loads(path.read_text(encoding='utf-8'))))
 dump(RES/'jugcraft/jimtafacts_spellbooks/source_loot.json',{'tables':count,'scroll_adaptation':'Original school/name filters and all table rolls/weights retained. Native workings currently use level one; source quality-level scaling is not claimed.','filtered_scrolls':reports})
