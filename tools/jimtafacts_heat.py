from jimtafacts_crafting import distinct
"""Original six-face IC heat/kinetic atlases; native heat buffers and existing JE/KE networks."""
from pathlib import Path
import hashlib, json, shutil
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
SOURCE=ROOT/'art/owner-library/originals/Oct23/Jimtafacts/industrialcraft reborn by jimbo/assets/ic2'
DEVICES={
 'radioisotope_generator':'generator/blockRTGenerator',
 'wind_kinetic_generator':'kineticgenerator/blockWindKineticGenerator',
 'water_kinetic_generator':'kineticgenerator/blockWaterKineticGenerator',
 'radioisotope_heat_generator':'heatgenerator/blockRTHeatGenerator',
 'fluid_heat_generator':'heatgenerator/blockFluidHeatGenerator',
 'liquid_heat_exchanger':'machine/blockLiquidHeatExchanger',
 'steam_generator':'machine/blockSteamGenerator',
 'steam_kinetic_generator':'kineticgenerator/blockSteamKineticGenerator',
 'solid_heat_generator':'heatgenerator/blockSolidHeatGenerator',
 'electric_heat_generator':'heatgenerator/blockElectricHeatGenerator',
 'stirling_generator':'generator/blockStirlingGenerator',
 'electric_kinetic_generator':'kineticgenerator/blockElectricKineticGenerator',
 'manual_kinetic_generator':'kineticgenerator/blockManualKineticGenerator',
 'stirling_kinetic_generator':'kineticgenerator/blockStirlingKineticGenerator',
}
def blocks():return {'jimta_'+name for name in DEVICES}|{'jimta_coolant','jimta_hot_coolant'}
def items():return {'jimta_'+name for name in DEVICES}|{'jimta_coolant_bucket','jimta_hot_coolant_bucket'}|{'jimta_'+n+'_rotor'+suffix for n in ('wood','iron','steel','carbon') for suffix in ('','_blade')}
def fluids():return {'jimta_coolant','flowing_jimta_coolant','jimta_hot_coolant','flowing_jimta_hot_coolant','jimta_steam','jimta_superheated_steam'}
def dump(p,d):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,indent=2)+'\n',encoding='utf-8')
def generate(shared=True):
 manifest=[];lang={}
 for name,source in DEVICES.items():
  ident='jimta_'+name;src=SOURCE/f'textures/blocks/{source}.png';dst=RES/f'assets/jugcraft/textures/block/jimtafacts/heat/{name}.png';dst.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(src,dst)
  manifest.append({'source':src.relative_to(ROOT).as_posix(),'runtime':dst.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(src.read_bytes()).hexdigest(),'changes':'None; original complete horizontal face atlas.'})
  with Image.open(src) as image:w,h=image.size
  tiles=w//h;variants={}
  for lit in (False,True):
   faces={}
   for face,index in zip(('down','up','north','south','west','east'),(0,1,3,5,4,2)):
    tile=index+(6 if lit and tiles>=12 else 0)
    faces[face]={'texture':'#sheet','uv':[16*tile/tiles,0,16*(tile+1)/tiles,16],'cullface':face}
   model=ident+('_active' if lit else '')
   dump(RES/f'assets/jugcraft/models/block/{model}.json',{'parent':'minecraft:block/block','textures':{'sheet':'jugcraft:block/jimtafacts/heat/'+name,'particle':'#sheet'},'elements':[{'from':[0,0,0],'to':[16,16,16],'faces':faces}]})
   for facing,rot in [('north',0),('east',90),('south',180),('west',270)]:variants[f'facing={facing},lit={str(lit).lower()}']={'model':'jugcraft:block/'+model,'y':rot}
  dump(RES/f'assets/jugcraft/blockstates/{ident}.json',{'variants':variants});dump(RES/f'assets/jugcraft/items/{ident}.json',{'model':{'type':'minecraft:model','model':'jugcraft:block/'+ident}})
  dump(RES/f'data/jugcraft/loot_table/blocks/{ident}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:'+ident}],'condition':{'type':'minecraft:survives_explosion'}}]})
  parts=['jugcraft:iron_plate']*3+['jugcraft:machine_casing','jugcraft:copper_wire']
  parts+=['minecraft:furnace'] if name=='solid_heat_generator' else ['minecraft:lever','jugcraft:iron_gear'] if name=='manual_kinetic_generator' else ['jugcraft:electric_motor','jugcraft:basic_circuit'] if name.startswith('electric_') else ['jugcraft:heat_exchanger','jugcraft:electric_motor']
  dump(RES/f'data/jugcraft/recipe/jimtafacts_heat/{name}.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':distinct('jimtafacts_heat/'+name,parts),'result':{'id':'jugcraft:'+ident}})
  lang['block.jugcraft.'+ident]=name.replace('_',' ').title()
 # Fluids retain source pixels and animation metadata, with namespace-only adaptation.
 def copy(src,dst):
  dst.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(src,dst)
  manifest.append({'source':src.relative_to(ROOT).as_posix(),'runtime':dst.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(src.read_bytes()).hexdigest(),'changes':'None'})
 for name,source in [('wood','Wood'),('iron','Iron'),('steel','Steel'),('carbon','Carbon')]:
  copy(SOURCE/f'textures/items/rotors/rotor{source}model.png',RES/f'assets/jugcraft/textures/block/jimtafacts/heat/rotor_{name}.png')
  for suffix,origin in [('',f'rotors/item{"wcarbon" if name=="carbon" else name}rotor'),('_blade',f'resources/item{name}rotorblade')]:
   ident=f'jimta_{name}_rotor'+suffix
   copy(SOURCE/f'textures/items/{origin}.png',RES/f'assets/jugcraft/textures/item/jimtafacts/heat/{ident}.png')
   dump(RES/f'assets/jugcraft/models/item/{ident}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'jugcraft:item/jimtafacts/heat/'+ident}})
   dump(RES/f'assets/jugcraft/items/{ident}.json',{'model':{'type':'minecraft:model','model':'jugcraft:item/'+ident}});lang['item.jugcraft.'+ident]=(name+' rotor'+suffix.replace('_',' ')).title()
  material={'wood':'minecraft:oak_planks','iron':'jugcraft:iron_plate','steel':'jugcraft:steel_plate','carbon':'jugcraft:jimta_carbon_plate'}[name]
  dump(RES/f'data/jugcraft/recipe/jimtafacts_heat/{name}_blade.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':[material]*3+['minecraft:iron_nugget'],'result':{'id':f'jugcraft:jimta_{name}_rotor_blade'}})
  dump(RES/f'data/jugcraft/recipe/jimtafacts_heat/{name}_rotor.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':[f'jugcraft:jimta_{name}_rotor_blade']*4+['jugcraft:iron_gear'],'result':{'id':f'jugcraft:jimta_{name}_rotor'}})
 for name,source,cell in [('coolant' ,'coolant','Coolant'),('hot_coolant','hotcoolant','HotCoolant')]:
  for kind in ('still','flow'):
   for ext in ('.png','.png.mcmeta'):copy(SOURCE/f'textures/blocks/fluids/{source}_{kind}{ext}',RES/f'assets/jugcraft/textures/block/jimta_{name}_{kind}{ext}')
  ident='jimta_'+name
  copy(SOURCE/f'textures/items/cell/itemCell{cell}.png',RES/f'assets/jugcraft/textures/item/jimtafacts/heat/{ident}_bucket.png')
  dump(RES/f'assets/jugcraft/models/item/{ident}_bucket.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'jugcraft:item/jimtafacts/heat/{ident}_bucket'}})
  dump(RES/f'assets/jugcraft/items/{ident}_bucket.json',{'model':{'type':'minecraft:model','model':'jugcraft:item/'+ident+'_bucket'}})
  dump(RES/f'assets/jugcraft/blockstates/{ident}.json',{'variants':{'':{'model':'minecraft:block/water'}}})
  lang['block.jugcraft.'+ident]=name.replace('_',' ').title();lang['item.jugcraft.'+ident+'_bucket']=name.replace('_',' ').title()+' Bucket'
 for ext in ('.png','.png.mcmeta'):
  src=SOURCE/f'textures/blocks/fluids/steam{ext}'
  if not src.exists():src=SOURCE/f'textures/blocks/fluids/steam_still{ext}'
  if src.exists():copy(src,RES/f'assets/jugcraft/textures/block/jimta_steam_still{ext}')
 lang['fluid.jugcraft.jimta_steam']='Steam'
 for ext in ('.png','.png.mcmeta'):
  copy(SOURCE/f'textures/blocks/fluids/superheatedsteam_still{ext}',RES/f'assets/jugcraft/textures/block/jimta_superheated_steam_still{ext}')
 lang['fluid.jugcraft.jimta_superheated_steam']='Superheated Steam'
 from jimtafacts_fluid_recipes import recipes as coolant_recipes
 dump(RES/'data/jugcraft/recipe/chemical_reaction/jimta_coolant.json',coolant_recipes()['jimta_coolant'])
 if shared:
  path=RES/'assets/jugcraft/lang/en_us.json';data=json.loads(path.read_text(encoding='utf-8'));data.update(lang);dump(path,data)
  for name in ('mineable/pickaxe','needs_stone_tool'):
   path=RES/f'data/minecraft/tags/block/{name}.json';data=json.loads(path.read_text(encoding='utf-8'));data['values']=list(dict.fromkeys(data['values']+['jugcraft:jimta_'+n for n in sorted(DEVICES)]));dump(path,data)
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/heat-runtime-copies.json',manifest)
 geometry=[]
 for name in ('ic2/core/block/KineticGeneratorRotor.class','ic2/core/block/KineticGeneratorRenderer.class','ic2/core/init/BlocksItems.class'):
  src=SOURCE.parents[1]/name
  geometry.append({'source':src.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(src.read_bytes()).hexdigest(),'runtime':'src/client/java/io/github/jimbozoomer/jugcraft/client/jimtafacts/IndustrialRotorRenderer.java','adaptation':'Statically recovered blade dimensions, pitches, mirror and 32x256 UV layout; source rotor diameters/lifetimes. Native connecting hub and client motion state. No source class is executed or bundled.'})
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/heat-model-sources.json',geometry)

 return items()
if __name__=='__main__':print(f'Generated {len(generate())} heat/kinetic workshops.')
