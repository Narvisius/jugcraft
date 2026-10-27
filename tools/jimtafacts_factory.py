"""Original factory tank, pump and chute assets on native fluid/item networks."""
from pathlib import Path
import hashlib,json,shutil
ROOT=Path(__file__).resolve().parents[1];RES=ROOT/'src/main/resources'
SOURCE=ROOT/'art/owner-library/originals/Oct23/Jimtafacts/buildcraft reborn by jimbo/assets'
DEVICES={'jimta_glass_tank':'tank','jimta_factory_pump':'pump','jimta_chute':'chute'}
def blocks():
 import jimtafacts_factory_decor,jimtafacts_logistics_housings
 return jimtafacts_logistics_housings.blocks()|jimtafacts_factory_decor.blocks()|set(DEVICES)|{'jimta_factory_heat_'+p for p in ('start','middle','end')}|{'jimta_blueprint_library','jimta_zone_planner','jimta_water_gel','jimta_path_beacon','jimta_volume_beacon','jimta_power_tester'}
def items():return (blocks()-{'jimta_water_gel'})|{'jimta_'+k+'_gear' for k in ('wood','stone','gold','diamond')}|{'jimta_factory_list','jimta_map_location','jimta_factory_goggles','jimta_paintbrush','jimta_pipe_color_marker','jimta_pipe_waterproofing','jimta_water_gel_seed','jimta_gelled_water'}
def dump(path,value):path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(value,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
def generate(shared=True):
 manifest=[];lang={};copied=set()
 def copy(src,dst):
  if dst in copied:return
  copied.add(dst);dst.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(src,dst)
  manifest.append({'source':src.relative_to(ROOT).as_posix(),'runtime':dst.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(src.read_bytes()).hexdigest()})
  if Path(str(src)+'.mcmeta').exists():copy(Path(str(src)+'.mcmeta'),Path(str(dst)+'.mcmeta'))
 def model(name):
  source=SOURCE/f'buildcraftfactory/models/block/{name}.json';v=json.loads(source.read_text(encoding='utf-8'))
  for key,texture in v.get('textures',{}).items():
   if texture.startswith('#'):continue
   ns,path=texture.split(':');dst=f'block/jimtafacts/factory/{path.removeprefix("blocks/")}'
   copy(SOURCE/f'{ns}/textures/{path}.png',RES/f'assets/jugcraft/textures/{dst}.png');v['textures'][key]='jugcraft:'+dst
  parent=v.get('parent')
  if parent and parent.startswith('buildcraftfactory:'):v['parent']='jugcraft:block/jimtafacts/factory/'+parent.split(':')[1].removeprefix('block/');model(parent.split('/')[-1])
  elif parent and parent.startswith('block/'):v['parent']='minecraft:'+parent
  elif not parent:v['parent']='minecraft:block/block'
  dump(RES/f'assets/jugcraft/models/block/jimtafacts/factory/{name}.json',v)
  manifest.append({'source':source.relative_to(ROOT).as_posix(),'runtime':f'assets/jugcraft/models/block/jimtafacts/factory/{name}.json','adaptation':'Preserved original elements, UVs and texture pixels; native model identifiers and parent schema.'})
 for original in (*DEVICES.values(),'tank_joined_below','chute_connected'):model(original)
 copy(SOURCE/'buildcraftfactory/textures/blocks/pump/tube.png',RES/'assets/jugcraft/textures/block/jimtafacts/factory/pump/tube.png')
 for ident,original in DEVICES.items():
  dump(RES/f'assets/jugcraft/items/{ident}.json',{'model':{'type':'minecraft:model','model':'jugcraft:block/jimtafacts/factory/'+original}})
  entry={'type':'minecraft:item','name':'jugcraft:'+ident}
  if ident=='jimta_glass_tank':entry['modifier']={'type':'minecraft:copy_components','source':'block_entity','include':['jugcraft:stored_fluid']}
  dump(RES/f'data/jugcraft/loot_table/blocks/{ident}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[entry],'condition':{'type':'minecraft:survives_explosion'}}]})
  lang['block.jugcraft.'+ident]={'jimta_glass_tank':'Workshop Glass Tank','jimta_factory_pump':'Workshop Fluid Pump','jimta_chute':'Workshop Chute'}[ident]
 dump(RES/'assets/jugcraft/blockstates/jimta_factory_pump.json',{'variants':{'':{'model':'jugcraft:block/jimtafacts/factory/pump'}}})
 dump(RES/'assets/jugcraft/blockstates/jimta_glass_tank.json',{'variants':{f'joined_below={str(joined).lower()}':{'model':'jugcraft:block/jimtafacts/factory/tank'+('_joined_below' if joined else '')} for joined in (False,True)}})
 # Native FACING names the narrow output end; the source property named the wide intake instead.
 rotations={'down':(0,0),'up':(180,0),'north':(90,180),'south':(90,0),'west':(90,90),'east':(90,270)}
 parts=[]
 for side,(x,y) in rotations.items():
  parts.append({'when':{'facing':side},'apply':{'model':'jugcraft:block/jimtafacts/factory/chute','x':x,'y':y}})
  parts.append({'when':{'connected_'+side:'true'},'apply':{'model':'jugcraft:block/jimtafacts/factory/chute_connected','x':x,'y':y}})
 dump(RES/'assets/jugcraft/blockstates/jimta_chute.json',{'multipart':parts})
 copy(SOURCE/'buildcraftfactory/textures/gui/chute.png',RES/'assets/jugcraft/textures/gui/jimtafacts/chute.png')
 recipes={'jimta_glass_tank':['minecraft:glass']*6+['jugcraft:iron_plate']*2,
          'jimta_factory_pump':['jugcraft:machine_casing','jugcraft:electric_motor','jugcraft:bronze_fluid_pipe','jugcraft:basic_circuit'],
          'jimta_chute':['minecraft:hopper','jugcraft:iron_gear','minecraft:comparator']}
 for ident,ingredients in recipes.items():dump(RES/f'data/jugcraft/recipe/jimtafacts_factory/{ident}.json',{'type':'minecraft:crafting_shapeless','category':'redstone','ingredients':ingredients,'result':{'id':'jugcraft:'+ident}})
 if shared:
  p=RES/'assets/jugcraft/lang/en_us.json';v=json.loads(p.read_text(encoding='utf-8'));v.update(lang);dump(p,v)
  p=RES/'data/minecraft/tags/block/mineable/pickaxe.json';v=json.loads(p.read_text(encoding='utf-8'));v['values']=sorted(set(v['values'])|{'jugcraft:'+n for n in blocks()});dump(p,v)
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/factory-runtime-copies.json',manifest)
 import jimtafacts_factory_decor
 jimtafacts_factory_decor.generate(shared)
 import jimtafacts_logistics_housings
 jimtafacts_logistics_housings.generate(shared)
 import jimtafacts_factory_heat
 jimtafacts_factory_heat.generate(shared)
 import jimtafacts_planning
 jimtafacts_planning.generate(shared)
 import jimtafacts_field_tools
 jimtafacts_field_tools.generate(shared)
 import jimtafacts_water_gel
 jimtafacts_water_gel.generate(shared)
 return len(DEVICES)
if __name__=='__main__':print('Generated',generate(),'original factory housings')
