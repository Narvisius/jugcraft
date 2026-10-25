from jimtafacts_crafting import distinct
"""Original matter device atlases and animated UU fluid, with native economy-aware replication."""
from pathlib import Path
import json,shutil,hashlib
ROOT=Path(__file__).resolve().parents[1];RES=ROOT/'src/main/resources';SOURCE=ROOT/'art/owner-library/originals/Oct23/Jimtafacts/industrialcraft reborn by jimbo/assets/ic2'
DEVICES={'jimta_mass_fabricator':'blockMatter','jimta_matter_scanner':'blockScanner','jimta_pattern_storage':'blockPatternStorage','jimta_matter_replicator':'blockReplicator','jimta_solid_canner':'blockSolidCanner','jimta_fluid_bottler':'blockFluidBottler'}
def items():return set(DEVICES)|{'jimta_raw_crystal_memory','jimta_crystal_memory','jimta_uu_matter_bucket'}
def blocks():return set(DEVICES)|{'jimta_uu_matter'}
def fluids():return {'jimta_uu_matter','flowing_jimta_uu_matter'}
def dump(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def generate(shared=True):
 manifest=[];lang={}
 def copy(src,dest):dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(src,dest);manifest.append({'source':src.relative_to(ROOT).as_posix(),'runtime':dest.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(src.read_bytes()).hexdigest()})
 from PIL import Image
 for id,source in DEVICES.items():
  src=SOURCE/f'textures/blocks/machine/{source}.png';dest=RES/f'assets/jugcraft/textures/block/jimtafacts/matter/{id}.png';copy(src,dest)
  with Image.open(src) as im:w,h=im.size
  tiles=w//h
  states={}
  for active in (False,True):
   offset=6 if active and tiles>=12 else 0;model=id+('_active' if active else '')
   faces={face:{'texture':'#all','uv':[16*(index+offset)/tiles,0,16*(index+offset+1)/tiles,16]} for face,index in [('down',0),('up',1),('north',3),('south',5),('west',4),('east',2)]}
   dump(RES/f'assets/jugcraft/models/block/{model}.json',{'parent':'minecraft:block/block','textures':{'all':'jugcraft:block/jimtafacts/matter/'+id,'particle':'#all'},'elements':[{'from':[0,0,0],'to':[16,16,16],'faces':faces}]})
   for facing,rot in [('north',0),('east',90),('south',180),('west',270)]:states[f'facing={facing},lit={str(active).lower()}']={'model':'jugcraft:block/'+model,'y':rot}
  dump(RES/f'assets/jugcraft/blockstates/{id}.json',{'variants':states});dump(RES/f'assets/jugcraft/items/{id}.json',{'model':{'type':'minecraft:model','model':'jugcraft:block/'+id}})
  dump(RES/f'data/jugcraft/loot_table/blocks/{id}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:'+id}],'condition':{'type':'minecraft:survives_explosion'}}]});lang['block.jugcraft.'+id]=id.removeprefix('jimta_').replace('_',' ').title()
 for id,source in [('jimta_raw_crystal_memory','resources/itemrawcrystalmemory'),('jimta_crystal_memory','resources/itemcrystalmemory'),('jimta_uu_matter_bucket','cell/itemCellUuMatter')]:
  copy(SOURCE/f'textures/items/{source}.png',RES/f'assets/jugcraft/textures/item/jimtafacts/matter/{id}.png');dump(RES/f'assets/jugcraft/models/item/{id}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'jugcraft:item/jimtafacts/matter/'+id}});dump(RES/f'assets/jugcraft/items/{id}.json',{'model':{'type':'minecraft:model','model':'jugcraft:item/'+id}});lang['item.jugcraft.'+id]=id.removeprefix('jimta_').replace('_',' ').title()
 for kind in ('still','flow'):
  for suffix in ('.png','.png.mcmeta'):copy(SOURCE/f'textures/blocks/fluids/uumatter_{kind}{suffix}',RES/f'assets/jugcraft/textures/block/jimta_uu_matter_{kind}{suffix}')
 dump(RES/'assets/jugcraft/blockstates/jimta_uu_matter.json',{'variants':{'':{'model':'minecraft:block/water'}}});lang['block.jugcraft.jimta_uu_matter']='UU-Matter';lang['item.jugcraft.jimta_uu_matter_bucket']='UU-Matter Bucket'
 recipes={}
 def craft(name,parts,result):recipes[name]={'type':'minecraft:crafting_shapeless','category':'misc','ingredients':distinct('jimtafacts_matter/'+name,parts),'result':{'id':'jugcraft:'+result,'count':1}}
 craft('raw_memory',['minecraft:quartz']*4+['minecraft:diamond','minecraft:redstone'],'jimta_raw_crystal_memory')
 recipes['crystal_memory']={'type':'minecraft:smelting','ingredient':'jugcraft:jimta_raw_crystal_memory','result':{'id':'jugcraft:jimta_crystal_memory'},'experience':0.1,'cookingtime':200}
 for id in DEVICES:
  parts=['jugcraft:basic_circuit','jugcraft:tin_plate','jugcraft:tin_plate','jugcraft:steel_plate','jugcraft:electric_motor','jugcraft:machine_casing','minecraft:glass'] if id in ('jimta_solid_canner','jimta_fluid_bottler') else ['jugcraft:advanced_circuit']*2+['jugcraft:steel_plate']*4+['jugcraft:machine_casing','minecraft:diamond','minecraft:glass']
  craft(id,parts,id)
 for name,value in recipes.items():dump(RES/f'data/jugcraft/recipe/jimtafacts_matter/{name}.json',value)
 dump(RES/'data/jugcraft/tags/block/jimtafacts_matter.json',{'replace':False,'values':['jugcraft:'+id for id in DEVICES]})
 if shared:
  p=RES/'assets/jugcraft/lang/en_us.json';v=json.loads(p.read_text(encoding='utf-8'));v.update(lang);dump(p,v)
  for tag in ('mineable/pickaxe','needs_iron_tool'):
   p=RES/f'data/minecraft/tags/block/{tag}.json';v=json.loads(p.read_text(encoding='utf-8'));ref='#jugcraft:jimtafacts_matter'
   if ref not in v['values']:v['values'].append(ref)
   dump(p,v)
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/matter-runtime-copies.json',manifest)
 return items()
if __name__=='__main__':
 import sys
 print(f'Generated {len(generate("--no-shared" not in sys.argv))} matter workshop items.')
