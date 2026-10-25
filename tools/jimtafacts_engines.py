"""Port the supplied piston elements/UVs, unchanged textures and native JE recipes.

Only the source model's four known numeric expressions are translated here.
No source expression interpreter or supplied program is executed.
"""
from pathlib import Path
import hashlib,json,shutil
ROOT=Path(__file__).resolve().parents[1];RES=ROOT/'src/main/resources'
SOURCE=ROOT/'art/owner-library/originals/Oct23/Jimtafacts/buildcraft reborn by jimbo/assets'
KINDS={'redstone_engine':('buildcraftcore','engine_redstone','engine/wood'),
 'stirling_engine':('buildcraftenergy','engine_stone','engine/stone'),
 'combustion_engine':('buildcraftenergy','engine_iron','engine/iron'),
 'electric_engine':('buildcraftenergy','engine_rf','engine/rf'),
 'creative_engine':('buildcraftcore','engine_creative','engine/creative'),
 'kinetic_dynamo':('buildcraftenergy','mj_dynamo','mj_dynamo')}
def blocks():return {'jimta_'+n for n in KINDS}
def items():return blocks()|{'jimta_ashes'}
def dump(path,value):path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(value,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
def generate(shared=True):
 manifest=[];lang={};texture_root=RES/'assets/jugcraft/textures/block/jimtafacts/engines'
 def copy(src,dst):
  dst.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(src,dst)
  manifest.append({'source':src.relative_to(ROOT).as_posix(),'runtime':dst.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(src.read_bytes()).hexdigest()})
  sidecar=Path(str(src)+'.mcmeta')
  if sidecar.exists():copy(sidecar,Path(str(dst)+'.mcmeta'))
 for stage in ('blue','green','yellow','red','overheat'):copy(SOURCE/f'buildcraftlib/textures/blocks/engine/trunk_{stage}.png',texture_root/f'trunk_{stage}.png')
 copy(SOURCE/'buildcraftlib/textures/blocks/engine/chamber_base.png',texture_root/'chamber.png')
 for name,original in {'stirling':'steam_engine_gui','combustion':'combustion_engine_gui','electric':'rf_engine_gui','dynamo':'mj_dynamo_gui'}.items():
  copy(SOURCE/f'buildcraftenergy/textures/gui/{original}.png',RES/f'assets/jugcraft/textures/gui/jimtafacts/engine_{name}.png')
 for name,(namespace,model,folder) in KINDS.items():
  ident='jimta_'+name;textures={'trunk':'jugcraft:block/jimtafacts/engines/trunk_blue','chamber':'jugcraft:block/jimtafacts/engines/chamber'}
  for part in ('back','side')+(('front',) if name=='kinetic_dynamo' else ()):
   copy(SOURCE/f'{namespace}/textures/blocks/{folder}/{part}.png',texture_root/f'{name}_{part}.png');textures[part]=f'jugcraft:block/jimtafacts/engines/{name}_{part}'
  textures['particle']='#side'
  source=SOURCE/(f'{namespace}/models/block/{model}.json' if name=='kinetic_dynamo' else 'buildcraftlib/models/block/engine_base.json')
  raw=json.loads(source.read_text(encoding='utf-8'));elements=[]
  # Source item pose is progress=0.2: 0.2*(8*2-0.01), preserving the supplied dimensions.
  progress=.2*15.99;expressions={'progress_size':progress,'4 + progress_size':4+progress,'8 + progress_size':8+progress}
  def numeric(value):return expressions[value] if isinstance(value,str) else value
  for row in raw['elements']:
   faces={}
   for face,data in row['faces'].items():
    texture='#trunk' if data['texture']=='trunk_tex' else data['texture'].strip("'")
    faces[face]={'uv':[numeric(x) for x in data['uv']],'texture':texture}
   elements.append({'from':[numeric(x) for x in row['from']],'to':[numeric(x) for x in row['to']],'faces':faces})
  dump(RES/f'assets/jugcraft/models/block/{ident}.json',{'parent':'minecraft:block/block','textures':textures,'elements':elements})
  dump(RES/f'assets/jugcraft/models/block/{ident}_entity.json',{'textures':textures,'elements':[]})
  dump(RES/f'assets/jugcraft/items/{ident}.json',{'model':{'type':'minecraft:model','model':'jugcraft:block/'+ident}})
  dump(RES/f'assets/jugcraft/blockstates/{ident}.json',{'variants':{'':{'model':'jugcraft:block/'+ident+'_entity'}}})
  dump(RES/f'data/jugcraft/loot_table/blocks/{ident}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:'+ident}],'condition':{'type':'minecraft:survives_explosion'}}]})
  lang['block.jugcraft.'+ident]=name.replace('_',' ').title()
  manifest.append({'source':source.relative_to(ROOT).as_posix(),'runtime':f'assets/jugcraft/models/block/{ident}.json','adaptation':'Exact source element dimensions and face UVs at original inventory pose; native renderer animates the same triangular piston stroke and heat-stage textures. No source expression code executed.'})
 ashes=SOURCE.parent.parent/'industrialcraft reborn by jimbo/assets/ic2/textures/items/resources/itemAshes.png'
 copy(ashes,RES/'assets/jugcraft/textures/item/jimtafacts/ashes.png')
 dump(RES/'assets/jugcraft/models/item/jimta_ashes.json',{'parent':'minecraft:item/generated','textures':{'layer0':'jugcraft:item/jimtafacts/ashes'}})
 dump(RES/'assets/jugcraft/items/jimta_ashes.json',{'model':{'type':'minecraft:model','model':'jugcraft:item/jimta_ashes'}});lang['item.jugcraft.jimta_ashes']='Ashes'
 recipes={
  'redstone_engine':['minecraft:piston','minecraft:redstone','minecraft:glass']+['minecraft:oak_planks']*3,
  'stirling_engine':['minecraft:piston','minecraft:furnace','minecraft:glass','jugcraft:iron_gear','jugcraft:iron_gear'],
  'combustion_engine':['minecraft:piston','jugcraft:steel_plate','jugcraft:fluid_tank','jugcraft:iron_gear','jugcraft:basic_circuit'],
  'electric_engine':['minecraft:piston','jugcraft:electric_motor','jugcraft:basic_circuit','jugcraft:copper_wire'],
  'kinetic_dynamo':['jugcraft:electric_motor','jugcraft:iron_shaft','jugcraft:basic_circuit','jugcraft:copper_wire','jugcraft:steel_plate']}
 for name,ingredients in recipes.items():dump(RES/f'data/jugcraft/recipe/jimtafacts_engines/{name}.json',{'type':'minecraft:crafting_shapeless','category':'redstone','ingredients':ingredients,'result':{'id':'jugcraft:jimta_'+name}})
 if shared:
  p=RES/'assets/jugcraft/lang/en_us.json';v=json.loads(p.read_text(encoding='utf-8'));v.update(lang);dump(p,v)
  p=RES/'data/minecraft/tags/block/mineable/pickaxe.json';v=json.loads(p.read_text(encoding='utf-8'));v['values']=sorted(set(v['values'])|{'jugcraft:'+n for n in blocks()});dump(p,v)
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/engine-runtime-copies.json',manifest)
 return len(KINDS)
if __name__=='__main__':print('Generated',generate(),'original piston engines/dynamo')
