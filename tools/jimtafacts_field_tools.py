"""Native field tools using the owner's exact list, map and goggle sprites."""
import json,shutil,hashlib
from jimtafacts_factory import ROOT,RES,SOURCE,dump
TOOLS={'factory_list':['list_clean','list_used'],'map_location':['map_location/clean','map_location/spot','map_location/area','map_location/path'],'factory_goggles':['goggles']}
def items():return {'jimta_factory_guide','jimta_factory_guide_note','jimta_volume_box','jimta_filler_planner','jimta_marker_connector','jimta_power_tester','jimta_factory_debugger'}|{'jimta_'+name for name in TOOLS}|{'jimta_paintbrush','jimta_pipe_color_marker','jimta_pipe_waterproofing'}|{'jimta_'+k+'_gear' for k in ('wood','stone','gold','diamond')}
def generate(shared=True):
 import jimtafacts_factory_guides
 jimtafacts_factory_guides.generate(shared)
 import jimtafacts_diagnostics
 jimtafacts_diagnostics.generate(shared)
 import jimtafacts_volumes
 jimtafacts_volumes.generate(shared)
 import jimtafacts_waterproofing
 jimtafacts_waterproofing.generate(shared)
 import jimtafacts_factory_gears
 jimtafacts_factory_gears.generate(shared)
 import jimtafacts_paintbrush
 jimtafacts_paintbrush.generate(shared)
 manifest=[]
 for name,models in TOOLS.items():
  entries=[]
  for i,model in enumerate(models):
   src=SOURCE/f'buildcraftcore/models/item/{model}.json';data=json.loads(src.read_text());data['parent']='minecraft:item/generated'
   for key,value in data['textures'].items():
    ns,path=value.split(':');png=SOURCE/f'{ns}/textures/{path}.png';target=RES/f'assets/jugcraft/textures/item/jimtafacts/field_tools/{name}_{i}_{key}.png';target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(png,target)
    data['textures'][key]='jugcraft:item/jimtafacts/field_tools/'+target.stem;manifest.append({'source':png.relative_to(ROOT).as_posix(),'runtime':target.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(png.read_bytes()).hexdigest()})
   ref='jugcraft:item/jimta_'+name+'_'+str(i);dump(RES/f'assets/jugcraft/models/item/jimta_{name}_{i}.json',data);entries.append({'threshold':i,'model':{'type':'minecraft:model','model':ref}})
  dump(RES/f'assets/jugcraft/items/jimta_{name}.json',{'model':entries[0]['model'] if len(entries)==1 else {'type':'minecraft:range_dispatch','property':'minecraft:custom_model_data','index':0,'entries':entries,'fallback':entries[0]['model']}})
 ingredients={'factory_list':['minecraft:paper','minecraft:ink_sac'],'map_location':['minecraft:paper','minecraft:compass','minecraft:ink_sac'],'factory_goggles':['minecraft:glass','minecraft:glass','minecraft:leather','jugcraft:basic_circuit']}
 for name,parts in ingredients.items():dump(RES/f'data/jugcraft/recipe/jimtafacts_factory/{name}.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':parts,'result':{'id':'jugcraft:jimta_'+name}})
 if shared:
  p=RES/'assets/jugcraft/lang/en_us.json';v=json.loads(p.read_text());v.update({'item.jugcraft.jimta_'+n:n.replace('_',' ').title() for n in TOOLS});dump(p,v)
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/field-tools-runtime-copies.json',manifest)
 return len(TOOLS)
if __name__=='__main__':print('Generated',generate(),'field tools')
