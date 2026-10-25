"""Preserve the source crafting graph and translate its ingredients/components to Minecraft 26.3."""
import json
def generate(dump,RES,SOURCE):
 source=SOURCE/'data/irons_spellbooks';records=[]
 def ident(value):return value.replace('irons_spellbooks:','jugcraft:jimta_spellbooks_').replace('minecraft:chain','minecraft:iron_chain')
 def tag(value):return value.replace('irons_spellbooks:','jugcraft:jimtafacts/spellbooks/')
 def ingredient(value):
  if isinstance(value,str):return '#'+tag(value[1:]) if value.startswith('#') else ident(value)
  if isinstance(value,list):return [ingredient(x) for x in value]
  if 'item' in value:return ident(value['item'])
  if 'tag' in value:return '#'+tag(value['tag'])
  return value
 def result(value):
  if isinstance(value,str):return {'id':ident(value),'count':1}
  value=dict(value);value['id']=ident(value.get('id',value.get('item')));value.pop('item',None)
  components=value.get('components',{})
  # Native equipment carries the same single-slot capability intrinsically; no empty source-only component survives.
  if 'irons_spellbooks:spell_container' in components:
   content=components.pop('irons_spellbooks:spell_container');names=[x.get('id',x.get('spell','')).split(':')[-1] for x in content.get('data',[])];names=[n for n in names if n]
   if names:components['jugcraft:jimtafacts_spellbook_contents']=names
  if not components:value.pop('components',None)
  return value
 for path in sorted((source/'recipe').rglob('*.json'),key=lambda p:p.as_posix()):
  if path.stem in ('chronicle','patchouli_book'):
   # These require book systems not registered by the native port yet.
   # Keep the source, but do not ship a recipe which prevents worlds from loading.
   obsolete=RES/'data/jugcraft/recipe/jimtafacts/source'/path.relative_to(source/'recipe')
   if obsolete.is_file():obsolete.unlink()
   continue
  row=json.loads(path.read_text(encoding='utf-8'));kind=row.get('type','')
  if not kind.startswith('minecraft:'):continue
  row=dict(row);row.pop('neoforge:conditions',None)
  if 'key' in row:row['key']={k:ingredient(v) for k,v in row['key'].items()}
  if 'ingredients' in row:row['ingredients']=[ingredient(v) for v in row['ingredients']]
  for field in ('ingredient','addition','base','template'):
   if field in row:row[field]=ingredient(row[field])
  if 'result' in row:row['result']=result(row['result'])
  # Each exact source recipe has a unique path. Remove native bootstrap shortcuts only for its produced item.
  product=row.get('result',{}).get('id','')
  if product.startswith('jugcraft:jimta_spellbooks_'):
   shortcut=RES/f'data/jugcraft/recipe/{product.split(":")[1]}.json'
   if shortcut.exists():shortcut.unlink()
  dump(RES/'data/jugcraft/recipe/jimtafacts/source'/path.relative_to(source/'recipe'),row)
  records.append({'source':str(path.relative_to(SOURCE)).replace('\\','/'),'type':kind,'result':product})
 # Source c: material aliases are merged, not allowed to erase the other native industries' tags.
 for path in sorted((SOURCE/'data/c/tags/item').rglob('*.json'),key=lambda p:p.as_posix()):
  target=RES/'data/c/tags/item'/path.relative_to(SOURCE/'data/c/tags/item');data=json.loads(path.read_text(encoding='utf-8'));old=json.loads(target.read_text(encoding='utf-8')) if target.exists() else {'replace':False,'values':[]}
  for entry in data.get('values',[]):
   if isinstance(entry,str):mapped='#'+tag(entry[1:]) if entry.startswith('#') else ident(entry)
   else:mapped={**entry,'id':('#'+tag(entry['id'][1:]) if entry['id'].startswith('#') else ident(entry['id']))}
   if mapped not in old['values']:old['values'].append(mapped)
  dump(target,old)
 dump(RES/'jugcraft/jimtafacts_spellbooks/source_recipes.json',{'recipes':records,'create_compatibility':'Source Create filling/emptying pairs use the native cauldron bottle transactions; no Create blocks are registered.'})
 # Exact original ore frequency/altitudes. Biome hook only affects newly generated terrain.
 for directory,name in [('configured_feature','ore_mithril_feature'),('placed_feature','ore_mithril_placement')]:
  path=source/f'worldgen/{directory}/{name}.json'
  text=path.read_text(encoding='utf-8').replace('irons_spellbooks:ore_mithril_feature','jugcraft:jimtafacts/spellbooks/ore_mithril_feature').replace('irons_spellbooks:mithril_ore','jugcraft:jimta_spellbooks_mithril_ore').replace('irons_spellbooks:deepslate_mithril_ore','jugcraft:jimta_spellbooks_deepslate_mithril_ore')
  data=json.loads(text)
  if directory=='configured_feature':
   data={'type':data['type'],**data['config']}
   for target in data['targets']:
    state=target['state'];target['state']={'id':state['Name'],**({'properties':state['Properties']} if state.get('Properties') else {})}
   obsolete=RES/f'data/jugcraft/worldgen/configured_feature/jimtafacts/spellbooks/{name}.json'
   if obsolete.exists():obsolete.unlink()
   directory='feature'
  dump(RES/f'data/jugcraft/worldgen/{directory}/jimtafacts/spellbooks/{name}.json',data)
