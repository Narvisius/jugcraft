"""Adapt supplied counterflow exchanger geometry and pixels to native block models."""
import hashlib,json,shutil
from jimtafacts_factory import ROOT,RES,SOURCE,dump
PARTS=('start','middle','end')
def blocks():return {'jimta_factory_heat_'+p for p in PARTS}
def items():return blocks()
def generate(shared=True):
 manifest=[]
 for part in PARTS:
  src=SOURCE/f'buildcraftfactory/models/item/heat_exchange_{part}.json'
  model=json.loads(src.read_text(encoding='utf-8'));model['parent']='minecraft:block/block'
  # The inventory models include end caps on every piece. Remove those inside
  # the assembled pipe and retain one exterior cap on each endpoint.
  model['elements']=[e for e in model['elements'] if e.get('comment')!='Cap']
  if part!='middle':
   cap=json.loads((SOURCE/'buildcraftfactory/models/block/heat_exchange_cap.json').read_text(encoding='utf-8'))['elements']
   for e in cap:
    e['from'][0]-=14;e['to'][0]-=14
    if 'east' in e['faces']:e['faces']['west']=e['faces'].pop('east')
   model['elements']+=cap
  for key,value in list(model['textures'].items()):
   ns,path=value.split(':');source=SOURCE/f'{ns}/textures/{path}.png'
   dest=RES/f'assets/jugcraft/textures/block/jimtafacts/factory/heat_exchange/{source.name}'
   dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source,dest)
   model['textures'][key]='jugcraft:block/jimtafacts/factory/heat_exchange/'+source.stem
   manifest.append({'source':source.relative_to(ROOT).as_posix(),'runtime':dest.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(source.read_bytes()).hexdigest()})
  model['textures']['particle']=model['textures']['sprite_a']
  ident='jimta_factory_heat_'+part;ref='jugcraft:block/jimtafacts/factory/heat_'+part
  dump(RES/f'assets/jugcraft/models/block/jimtafacts/factory/heat_{part}.json',model)
  manifest.append({'source':src.relative_to(ROOT).as_posix(),'runtime':ref,'adaptation':'Original UVs/pixels/elements; internal inventory caps removed, source exterior cap retained; native facing rotations.'})
  dump(RES/f'assets/jugcraft/items/{ident}.json',{'model':{'type':'minecraft:model','model':ref}})
  dump(RES/f'assets/jugcraft/blockstates/{ident}.json',{'variants':{f'facing={side}':{'model':ref,'y':(rotation+(180 if part=='end' else 0))%360} for side,rotation in {'east':0,'south':90,'west':180,'north':270}.items()}})
  dump(RES/f'data/jugcraft/loot_table/blocks/{ident}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:'+ident}],'condition':{'type':'minecraft:survives_explosion'}}]})
  ingredients=['jugcraft:bronze_fluid_pipe','jugcraft:copper_plate','jugcraft:iron_plate']
  if part!='middle':ingredients+=['jugcraft:machine_casing','jugcraft:basic_circuit' if part=='start' else 'minecraft:glass']
  dump(RES/f'data/jugcraft/recipe/jimtafacts_factory/{ident}.json',{'type':'minecraft:crafting_shapeless','category':'redstone','ingredients':ingredients,'result':{'id':'jugcraft:'+ident}})
 if shared:
  p=RES/'assets/jugcraft/lang/en_us.json';data=json.loads(p.read_text(encoding='utf-8'))
  data.update({'block.jugcraft.jimta_factory_heat_'+part:'Heat Exchanger '+part.title() for part in PARTS});dump(p,data)
  p=RES/'data/minecraft/tags/block/mineable/pickaxe.json';data=json.loads(p.read_text(encoding='utf-8'));data['values']=sorted(set(data['values'])|{'jugcraft:'+b for b in blocks()});dump(p,data)
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/factory-heat-runtime-copies.json',manifest)
 return 3
if __name__=='__main__':print('Generated',generate(),'heat-exchanger parts')
