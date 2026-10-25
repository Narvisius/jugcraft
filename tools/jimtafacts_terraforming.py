"""Source terraformer face atlas and seven original reusable plan icons."""
from pathlib import Path
import hashlib,json,shutil
ROOT=Path(__file__).resolve().parents[1];RES=ROOT/'src/main/resources'
SOURCE=ROOT/'art/owner-library/originals/Oct23/Jimtafacts/industrialcraft reborn by jimbo/assets/ic2'
MODES=('blank','chilling','cultivation','desertification','flatification','irrigation','mushroom')
def blocks():return {'jimta_civil_terraformer'}
def items():return blocks()|{'jimta_terraform_'+n for n in MODES}
def dump(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
def generate(shared=True):
 manifest=[];lang={}
 def copy(src,dest):
  dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(src,dest)
  manifest.append({'source':src.relative_to(ROOT).as_posix(),'runtime':dest.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(src.read_bytes()).hexdigest()})
 id='jimta_civil_terraformer';copy(SOURCE/'textures/blocks/machine/blockTerra.png',RES/f'assets/jugcraft/textures/block/jimtafacts/civil/terraformer.png')
 states={}
 for lit in (False,True):
  model=id+('_active' if lit else '')
  faces={face:{'texture':'#sheet','uv':[16*(tile+6*int(lit))/12,0,16*(tile+6*int(lit)+1)/12,16],'cullface':face}
   for face,tile in zip(('down','up','north','south','west','east'),(0,1,3,5,4,2))}
  dump(RES/f'assets/jugcraft/models/block/{model}.json',{'parent':'minecraft:block/block','textures':{'sheet':'jugcraft:block/jimtafacts/civil/terraformer','particle':'#sheet'},'elements':[{'from':[0,0,0],'to':[16,16,16],'faces':faces}]})
  for face,rot in [('north',0),('east',90),('south',180),('west',270)]:states[f'facing={face},lit={str(lit).lower()}']={'model':'jugcraft:block/'+model,'y':rot}
 dump(RES/f'assets/jugcraft/blockstates/{id}.json',{'variants':states});dump(RES/f'assets/jugcraft/items/{id}.json',{'model':{'type':'minecraft:model','model':'jugcraft:block/'+id}})
 dump(RES/f'data/jugcraft/loot_table/blocks/{id}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:'+id}],'condition':{'type':'minecraft:survives_explosion'}}]})
 lang['block.jugcraft.'+id]='Terraformer'
 dump(RES/'data/jugcraft/recipe/jimtafacts_terraforming/terraformer.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':['jugcraft:steel_plate']*3+['jugcraft:advanced_circuit']*2+['jugcraft:electric_motor','jugcraft:machine_casing','minecraft:water_bucket'],'result':{'id':'jugcraft:'+id}})
 for mode in MODES:
  id='jimta_terraform_'+mode;src='itemTFBP'+('' if mode=='blank' else mode.title());copy(SOURCE/f'textures/items/{src}.png',RES/f'assets/jugcraft/textures/item/jimtafacts/civil/{mode}.png')
  dump(RES/f'assets/jugcraft/models/item/{id}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'jugcraft:item/jimtafacts/civil/'+mode}})
  dump(RES/f'assets/jugcraft/items/{id}.json',{'model':{'type':'minecraft:model','model':'jugcraft:item/'+id}});lang['item.jugcraft.'+id]=mode.title()+' Terraforming Plan'
  ingredients=['minecraft:paper','jugcraft:basic_circuit','minecraft:redstone'] if mode=='blank' else ['jugcraft:jimta_terraform_blank',{'chilling':'minecraft:snowball','cultivation':'minecraft:wheat_seeds','desertification':'minecraft:sand','flatification':'minecraft:iron_shovel','irrigation':'minecraft:water_bucket','mushroom':'minecraft:brown_mushroom'}[mode]]
  dump(RES/f'data/jugcraft/recipe/jimtafacts_terraforming/{mode}.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':ingredients,'result':{'id':'jugcraft:'+id}})
 if shared:
  path=RES/'assets/jugcraft/lang/en_us.json';data=json.loads(path.read_text(encoding='utf-8'));data.update(lang);dump(path,data)
  for tag in ('mineable/pickaxe','needs_iron_tool'):
   path=RES/f'data/minecraft/tags/block/{tag}.json';data=json.loads(path.read_text(encoding='utf-8'))
   if 'jugcraft:jimta_civil_terraformer' not in data['values']:data['values'].append('jugcraft:jimta_civil_terraformer')
   dump(path,data)
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/terraforming-runtime-copies.json',manifest)
 return items()
if __name__=='__main__':print('Generated terraforming resources:',len(generate()))
