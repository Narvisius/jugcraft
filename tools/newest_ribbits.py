"""Native Ribbits resources from preserved files; never executes library programs."""
from pathlib import Path
import json, shutil
from newest_building import ROOT, RES, SOURCE, read, write, map_value
from newest_twigs import recipe
import nouveau_nbt
from menagerie_data import loot
BASE=SOURCE/'frogs'
A=BASE/'assets/ribbits'
D=BASE/'data/ribbits'
OUT=RES/'assets/jugcraft'
def mapped(v,resource=False):return map_value(v,'ribbits','ribbits',resource)

def chest_loot():
 # Supplied 1.21+ files use singular loot_table; the old importer only read
 # loot_tables, leaving all 59 village chest references without their six tables.
 result={}
 for folder in ('loot_tables','loot_table'):
  for path in (D/folder/'chests').glob('*.json'):
    value=loot(mapped(read(path)))
    def empty_entries(node):
     if isinstance(node,list):
      for child in node:empty_entries(child)
     elif isinstance(node,dict):
      if node.get('type')=='minecraft:item' and node.get('name')=='minecraft:air':
       node['type']='minecraft:empty';node.pop('name')
      for child in node.values():empty_entries(child)
    empty_entries(value)
    result['ribbits_chests/'+path.stem]=value
 return result
def copy(a,b):
 b.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(a,b)
def tag(path,values):
 dest=RES/'data'/path;d=read(dest) if dest.exists() else {'replace':False,'values':[]}
 combined=d['values']+values
 if '/entity_type/' in path:
  roles=['jugcraft:ribbit']+['jugcraft:ribbit_'+n for n in ['nitwit','gardener','sorcerer','fisherman','merchant']]
  combined=[n for value in combined for n in (roles if value=='jugcraft:ribbits_ribbit' else [value])]
 d['values']=list(dict.fromkeys(combined));write(dest,d)
def state(v):
 name=mapped(v.get('Name',v.get('id','minecraft:air')));props=v.get('Properties',v.get('properties',{}))
 return name+('['+','.join(k+'='+x for k,x in props.items())+']' if props else '')
def data(v):
 if isinstance(v,list):return [data(x) for x in v]
 if isinstance(v,str):return mapped(v).replace('yungsapi:yung_single_element','minecraft:single_pool_element')
 if not isinstance(v,dict):return v
 if 'Name' in v:return {'id':mapped(v['Name']),**({'properties':v['Properties']} if 'Properties' in v else {})}
 return {k:data(x) for k,x in v.items()}
def main():
 for name,value in chest_loot().items():write(RES/'data/jugcraft/loot_table'/(name+'.json'),value)
 for folder in ('textures','sounds'):
  for p in (A/folder).rglob('*'):
   if p.is_file():copy(p,OUT/folder/'newest/ribbits'/p.relative_to(A/folder))
 for folder,native,suffix in [('geo','geckolib/models','.geo.json'),('animations','geckolib/animations','.animation.json')]:
  for p in (A/folder).rglob('*.json'):
   rel=p.relative_to(A/folder).as_posix()
   copy(p,OUT/native/'entity/ribbits'/rel)
 for p in (A/'models').rglob('*.json'):write(OUT/'models/newest/ribbits'/p.relative_to(A/'models'),mapped(read(p),True))
 for p in (A/'blockstates').glob('*.json'):write(OUT/'blockstates'/('ribbits_'+p.name),mapped(read(p),True))
 for p in (A/'models/item').glob('*.json'):
  if p.stem=='maraca_in_hand':continue
  model={'type':'minecraft:model','model':'jugcraft:newest/ribbits/item/'+p.stem}
  if p.stem=='maraca':model={'type':'minecraft:select','property':'minecraft:display_context','cases':[{'when':['firstperson_lefthand','firstperson_righthand','thirdperson_lefthand','thirdperson_righthand'],'model':{'type':'minecraft:model','model':'jugcraft:newest/ribbits/item/maraca_in_hand'}}],'fallback':model}
  if p.stem.endswith('_spawn_egg'):
   # The old template_spawn_egg model was removed; native tinted egg uses the registered entity.
   model={'type':'minecraft:model','model':'minecraft:item/frog_spawn_egg'}
  write(OUT/'items'/('ribbits_'+p.name),{'model':model})
 sounds=read(OUT/'sounds.json');original=read(A/'sounds.json')
 for name,value in original.items():
  d=dict(value);d['sounds']=mapped(d['sounds'],True)
  if 'subtitle' in d:d['subtitle']='jugcraft.'+d['subtitle']
  sounds['ribbits_'+name]=d
 write(OUT/'sounds.json',sounds);write(RES/'jugcraft/newest_ribbits.json',{'sounds':list(original)})
 lang=read(OUT/'lang/en_us.json')
 for k,v in read(A/'lang/en_us.json').items():
  if k.startswith('subtitles.'):k='jugcraft.'+k
  else:k=k.replace('.ribbits.','.jugcraft.ribbits_')
  lang[k]=v
 for role in ['nitwit','gardener','sorcerer','fisherman','merchant']:lang['entity.jugcraft.ribbit_'+role]='Ribbit '+role.title()
 lang['entity.jugcraft.ribbit']='Ribbit';write(OUT/'lang/en_us.json',lang)
 for old,new in [('recipes','recipe'),('loot_tables','loot_table'),('advancements','advancement')]:
  for p in (D/old).rglob('*.json'):
   v=mapped(read(p));v=recipe(v) if old=='recipes' else v
   write(RES/'data/jugcraft'/new/('ribbits_'+p.relative_to(D/old).as_posix()),v)
 # Entity breathing is native behavior; biome tags are written below with stable prefix paths.
 for ns in ['minecraft','c']:
  for p in (BASE/'data'/ns/'tags').rglob('*.json'):
   rel=p.relative_to(BASE/'data'/ns/'tags').as_posix();rel=rel.replace('blocks/','block/').replace('items/','item/').replace('entity_types/','entity_type/')
   tag(ns+'/tags/'+rel,mapped(read(p))['values'])
 for p in (D/'worldgen/template_pool').rglob('*.json'):write(RES/'data/jugcraft/worldgen/template_pool'/('ribbits_'+p.relative_to(D/'worldgen/template_pool').as_posix()),data(read(p)))
 for p in (D/'worldgen/structure').glob('*.json'):
  v=data(read(p));v['type']='minecraft:jigsaw';v['use_expansion_hack']=False;v.pop('enhanced_terrain_adaptation',None);v['terrain_adaptation']='beard_thin'
  write(RES/'data/jugcraft/worldgen/structure'/('ribbits_'+p.name),v)
 for p in (D/'worldgen/structure_set').glob('*.json'):
  v=data(read(p));v['placement']['type']='minecraft:random_spread';v['placement'].pop('enhanced_exclusion_zone',None);v['placement']['exclusion_zone']={'other_set':'minecraft:villages','chunk_count':6}
  write(RES/'data/jugcraft/worldgen/structure_set'/('ribbits_'+p.name),v)
 # Keep authored processor palettes and probabilities, implemented by a native bounded processor.
 for p in (D/'worldgen/processor_list').glob('*.json'):
  write(RES/'jugcraft/ribbits_processors'/p.name,mapped(read(p)))
  write(RES/'data/jugcraft/worldgen/processor_list'/('ribbits_'+p.name),{'processors':[{'processor_type':'jugcraft:ribbits_village','palette':p.stem}]})
 for p in (D/'worldgen/configured_feature').glob('*.json'):
  v=read(p)['config'];write(RES/'data/jugcraft/worldgen/feature'/('ribbits_'+p.name),{'type':'jugcraft:ribbits_vegetation','kind':p.stem,'tries':v['tries'],'spread':v['xz_spread'],'height':v['y_spread']})
 for p in (D/'worldgen/placed_feature').glob('*.json'):write(RES/'data/jugcraft/worldgen/placed_feature'/('ribbits_'+p.name),data(read(p)))
 for p in (D/'structure').rglob('*.nbt'):
  kind,name,root=nouveau_nbt.read(nouveau_nbt.adapt(p.read_bytes(),lambda x:mapped(x)))
  for entity in root.get('entities',(9,(10,[])))[1][1]:
   nbt=entity.get('nbt',(10,{}))[1];nbt.pop('UUID',None);nbt.pop('Attributes',None);nbt.pop('Brain',None)
   if nbt.get('id')==(8,'jugcraft:ribbits_ribbit'):
    nbt['id']=(8,'jugcraft:ribbit');d=nbt.pop('RibbitData',(10,{}))[1]
    role=d.get('profession',(8,'jugcraft:ribbits_nitwit'))[1].split('_')[-1]
    nbt['profession']=(3,['nitwit','gardener','sorcerer','fisherman','merchant'].index(role))
    nbt['umbrella']=(3,int(d.get('umbrella',(8,'umbrella_1'))[1][-1]))
  target=RES/'data/jugcraft/structure'/('ribbits_'+p.relative_to(D/'structure').as_posix());target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(nouveau_nbt.write((kind,name,root)))
 # Native tag paths match every mapped structure biome reference.
 for p in (D/'tags/worldgen/biome').rglob('*.json'):write(RES/'data/jugcraft/tags/worldgen/biome'/('ribbits_'+p.relative_to(D/'tags/worldgen/biome').as_posix()),mapped(read(p)))
 write(RES/'data/jugcraft/tags/entity_type/can_breathe_under_water.json',{'replace':False,'values':['jugcraft:ribbit']+['jugcraft:ribbit_'+n for n in ['nitwit','gardener','sorcerer','fisherman','merchant']]})
if __name__=='__main__':main()
