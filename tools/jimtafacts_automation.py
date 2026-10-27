"""Native AND/OR controllers and transfer filters using source plug geometry and pixels."""
from pathlib import Path
import copy,json,shutil,hashlib
ROOT=Path(__file__).resolve().parents[1];RES=ROOT/'src/main/resources';SOURCE=ROOT/'art/owner-library/originals/Oct23/Jimtafacts/buildcraft reborn by jimbo/assets/buildcraftsilicon'
GATES={f'jimta_gate_{m}_{logic}':(m,logic,rows) for m,rows in [('clay_brick',1),('nether_brick',2),('iron',4),('gold',8)] for logic in ('and','or')}
def blocks():return set(GATES)|{'jimta_sorting_machine','jimta_item_filter','jimta_fluid_filter','jimta_light_sensor','jimta_timer','jimta_pulsar'}
EXPANSIONS=('light_sensor','timer','pulsar','clock','fader')
MODIFIERS=('lapis','quartz','diamond')
def items():return blocks()|{'jimta_gate_copier'}|{'jimta_gate_expansion_'+x for x in EXPANSIONS}|{'jimta_gate_modifier_'+x for x in MODIFIERS}

def dump(path,value):path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(value,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
def generate(shared=True):
 manifest=[];lang={}
 def asset(rel):
  src=SOURCE/'textures'/rel;dst=RES/'assets/jugcraft/textures/block/jimtafacts/automation'/rel;dst.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(src,dst)
  manifest.append({'source':src.relative_to(ROOT).as_posix(),'runtime':dst.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(src.read_bytes()).hexdigest()})
  sidecar=src.with_suffix('.png.mcmeta')
  if sidecar.exists():
   target=dst.with_suffix('.png.mcmeta');shutil.copyfile(sidecar,target)
   manifest.append({'source':sidecar.relative_to(ROOT).as_posix(),'runtime':target.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(sidecar.read_bytes()).hexdigest()})
  return 'jugcraft:block/jimtafacts/automation/'+rel[:-4]
 gate=json.loads((SOURCE/'models/plugs/gate.json').read_text(encoding='utf-8'))
 for id,(material,logic,rows) in GATES.items():
  model={'parent':'minecraft:block/block','textures':{'particle':'#material','material':'minecraft:block/'+{'clay_brick':'bricks','nether_brick':'nether_bricks','iron':'iron_block','gold':'gold_block'}[material],'logic':asset('gates/gate_'+logic+'.png')},'elements':[]}
  for element in gate['elements'][:2 if material!='clay_brick' else 1]:
   e=copy.deepcopy(element);e.pop('visible',None)
   for face in e['faces'].values():face['texture']='#logic' if 'logic' in face['texture'] else '#material'
   model['elements'].append(e)
  dump(RES/f'assets/jugcraft/models/block/{id}.json',model)
  variants={}
  for expansion,kind in enumerate(('none',)+EXPANSIONS):
   name=id if expansion==0 else id+'_'+kind
   if expansion:
    expanded=copy.deepcopy(model);expanded['textures']['expansion']=asset('items/gates/gate_expansion_'+kind+'.png')
    # Small fitted face plate retains the expansion icon and its source animation sidecar.
    expanded['elements'].append({'from':[1.7,5,5],'to':[1.75,11,11],'faces':{'west':{'texture':'#expansion','uv':[0,0,16,16]}}})
    dump(RES/f'assets/jugcraft/models/block/{name}.json',expanded)
   for modifier,gem in enumerate(('none',)+MODIFIERS):
    modified=name if modifier==0 else name+'_'+gem
    if modifier:
     body=copy.deepcopy(expanded if expansion else model)
     body['textures']['modifier']='minecraft:block/'+{'lapis':'lapis_block','quartz':'quartz_block_top','diamond':'diamond_block'}[gem]
     # Preserve the four supplied raised corner studs and their original UVs.
     for element in gate['elements'][2:]:
      if 'modifier' not in element.get('visible',''):continue
      e=copy.deepcopy(element);e.pop('visible',None)
      for face in e.get('faces',{}).values():face['texture']='#modifier'
      body['elements'].append(e)
     dump(RES/f'assets/jugcraft/models/block/{modified}.json',body)
    for face,y in [('west',0),('north',90),('east',180),('south',270)]:variants[f'facing={face},expansion={expansion},modifier={modifier}']={'model':'jugcraft:block/'+modified,'y':y}
  dump(RES/f'assets/jugcraft/blockstates/{id}.json',{'variants':variants})
  lang['block.jugcraft.'+id]=material.replace('_',' ').title()+' '+logic.upper()+f' Gate ({rows} rows)'
  parts=['minecraft:'+{'clay_brick':'brick','nether_brick':'nether_brick','iron':'iron_ingot','gold':'gold_ingot'}[material]]*2+['jugcraft:jimta_chipset_'+{'clay_brick':'red','nether_brick':'red','iron':'iron','gold':'gold'}[material],'minecraft:redstone_torch' if logic=='and' else 'minecraft:redstone']
  dump(RES/f'data/jugcraft/recipe/jimtafacts_automation/{id}.json',{'type':'minecraft:crafting_shapeless','category':'redstone','ingredients':parts,'result':{'id':'jugcraft:'+id,'count':1}})
 lens=json.loads((SOURCE/'models/plugs/lens.json').read_text(encoding='utf-8'));extra=json.loads((SOURCE/'models/plugs/filter.json').read_text(encoding='utf-8'))
 filtermodel={'parent':'minecraft:block/block','textures':{'main':asset('plugs/filter.png'),'particle':'#main'},'elements':[]}
 for element in lens['cutout']+extra['cutout']:
  e=copy.deepcopy(element)
  for face in e['faces'].values():
   face['texture']='#main'
   if 'rotation' in face:face['rotation']*=90
  filtermodel['elements'].append(e)
 for id in ('jimta_item_filter','jimta_fluid_filter'):
  model=copy.deepcopy(filtermodel)
  # The supplied plug occupies a pipe face. Opposed plugs and the existing native connector form one inline fitting.
  back=copy.deepcopy(filtermodel['elements'])
  for e in back:
   e['from'][0],e['to'][0]=16-e['to'][0],16-e['from'][0]
  model['elements']+=back
  connector=json.loads((RES/'assets/jugcraft/models/block/steel_fluid_pipe_arm.json').read_text(encoding='utf-8'))
  model['textures'].update(connector['textures'])
  core={'from':[2,6,6],'to':[14,10,10],'faces':{f:{'texture':'#cable','uv':[0,0,16,4]} for f in ('up','down','north','south','west','east')}}
  model['elements'].append(core)
  dump(RES/f'assets/jugcraft/models/block/{id}.json',model)
  if id=='jimta_item_filter':
   variants={f'facing={face}':{'model':'jugcraft:block/'+id,**rot} for face,rot in [('west',{}),('north',{'y':90}),('east',{'y':180}),('south',{'y':270}),('up',{'x':90,'y':90}),('down',{'x':270,'y':90})]}
   blockstate={'variants':variants}
  else:
   blockstate={'multipart':[{'apply':{'model':'jugcraft:block/'+id}}]}
   for face,rot in [('north',{}),('east',{'y':90}),('south',{'y':180}),('west',{'y':270}),('up',{'x':270}),('down',{'x':90})]:
    blockstate['multipart'].append({'when':{face:'true'},'apply':{'model':'jugcraft:block/steel_fluid_pipe_arm',**rot}})
  dump(RES/f'assets/jugcraft/blockstates/{id}.json',blockstate)
  native='item_sorter' if id=='jimta_item_filter' else 'fluid_filter'
  lang['block.jugcraft.'+id]='Workshop '+('Item' if id=='jimta_item_filter' else 'Fluid')+' Filter'
  dump(RES/f'data/jugcraft/recipe/jimtafacts_automation/{id}.json',{'type':'minecraft:crafting_shapeless','category':'redstone','ingredients':['minecraft:iron_bars','minecraft:iron_bars','jugcraft:basic_circuit','minecraft:chest' if id=='jimta_item_filter' else 'minecraft:bucket'],'result':{'id':'jugcraft:'+id,'count':1}})
 for name,source,texture,cost in [('light_sensor','light_sensor','daylight_sensor','minecraft:daylight_detector'),('timer','timer','timer','minecraft:clock'),('pulsar','pulsar_static','pulsar_static','minecraft:repeater')]:
  id='jimta_'+name;original=json.loads((SOURCE/f'models/plugs/{source}.json').read_text(encoding='utf-8'))
  model={'parent':'minecraft:block/block','textures':{'all':asset('plugs/'+texture+'.png'),'particle':'#all'},'elements':original['elements']}
  dump(RES/f'assets/jugcraft/models/block/{id}.json',model)
  variants={f'facing={face}':{'model':'jugcraft:block/'+id,**rot} for face,rot in [('west',{}),('north',{'y':90}),('east',{'y':180}),('south',{'y':270}),('up',{'x':90,'y':90}),('down',{'x':270,'y':90})]}
  dump(RES/f'assets/jugcraft/blockstates/{id}.json',{'variants':variants})
  dump(RES/f'data/jugcraft/recipe/jimtafacts_automation/{id}.json',{'type':'minecraft:crafting_shapeless','category':'redstone','ingredients':['jugcraft:basic_circuit','minecraft:iron_ingot',cost],'result':{'id':'jugcraft:'+id,'count':1}})
  lang['block.jugcraft.'+id]='Workshop '+name.replace('_',' ').title()
 for id in blocks():
  dump(RES/f'assets/jugcraft/items/{id}.json',{'model':{'type':'minecraft:model','model':'jugcraft:block/'+id}})
  dump(RES/f'data/jugcraft/loot_table/blocks/{id}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:'+id}],'condition':{'type':'minecraft:survives_explosion'}}]})
 empty={'type':'minecraft:model','model':'jugcraft:item/jimta_gate_copier_empty'}
 for state in ('empty','full'):
  tex=asset('items/gatecopier/'+state+'.png');dump(RES/f'assets/jugcraft/models/item/jimta_gate_copier_{state}.json',{'parent':'minecraft:item/handheld','textures':{'layer0':tex}})
 dump(RES/'assets/jugcraft/items/jimta_gate_copier.json',{'model':{'type':'minecraft:range_dispatch','property':'minecraft:custom_model_data','index':0,'entries':[{'threshold':1,'model':{'type':'minecraft:model','model':'jugcraft:item/jimta_gate_copier_full'}}],'fallback':empty}})
 lang['item.jugcraft.jimta_gate_copier']='Gate Copier'
 for kind in MODIFIERS:
  id='jimta_gate_modifier_'+kind
  if kind=='lapis':tex='minecraft:item/lapis_lazuli'
  else:tex=asset('items/gates/gate_material_'+kind+'.png')
  dump(RES/f'assets/jugcraft/models/item/{id}.json',{'parent':'minecraft:item/generated','textures':{'layer0':tex}})
  dump(RES/f'assets/jugcraft/items/{id}.json',{'model':{'type':'minecraft:model','model':'jugcraft:item/'+id}})
  ingredient='minecraft:'+('lapis_lazuli' if kind=='lapis' else kind)
  dump(RES/f'data/jugcraft/recipe/jimtafacts_automation/{id}.json',{'type':'minecraft:crafting_shapeless','category':'redstone','ingredients':['jugcraft:jimta_chipset_quartz',ingredient,ingredient],'result':{'id':'jugcraft:'+id}})
  lang['item.jugcraft.'+id]=kind.title()+' Gate Modifier'
 for kind in EXPANSIONS:
  id='jimta_gate_expansion_'+kind;tex=asset('items/gates/gate_expansion_'+kind+'.png')
  dump(RES/f'assets/jugcraft/models/item/{id}.json',{'parent':'minecraft:item/generated','textures':{'layer0':tex}})
  dump(RES/f'assets/jugcraft/items/{id}.json',{'model':{'type':'minecraft:model','model':'jugcraft:item/'+id}})
  cost={'light_sensor':'minecraft:daylight_detector','timer':'minecraft:clock','pulsar':'minecraft:repeater','clock':'minecraft:clock','fader':'minecraft:comparator'}[kind]
  dump(RES/f'data/jugcraft/recipe/jimtafacts_automation/{id}.json',{'type':'minecraft:crafting_shapeless','category':'redstone','ingredients':['jugcraft:jimta_chipset_quartz',cost,'minecraft:gold_nugget' if kind=='clock' else 'minecraft:redstone'],'result':{'id':'jugcraft:'+id}})
  lang['item.jugcraft.'+id]=kind.replace('_',' ').title()+' Gate Expansion'
 dump(RES/'data/jugcraft/recipe/jimtafacts_automation/gate_copier.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':['jugcraft:jimta_chipset_quartz','minecraft:iron_ingot','minecraft:iron_ingot'],'result':{'id':'jugcraft:jimta_gate_copier'}})
 dump(RES/'data/jugcraft/tags/block/jimtafacts_automation.json',{'replace':False,'values':['jugcraft:'+id for id in sorted(blocks())]})
 if shared:
  path=RES/'assets/jugcraft/lang/en_us.json';value=json.loads(path.read_text(encoding='utf-8'));value.update(lang);dump(path,value)
  for tag in ('mineable/pickaxe','needs_stone_tool'):
   path=RES/f'data/minecraft/tags/block/{tag}.json';value=json.loads(path.read_text(encoding='utf-8'));ref='#jugcraft:jimtafacts_automation'
   if ref not in value['values']:value['values'].append(ref)
   dump(path,value)
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/automation-runtime-copies.json',manifest)
 return items()
if __name__=='__main__':
 import sys
 print(f'Generated {len(generate("--no-shared" not in sys.argv))} functional gates and filters.')
