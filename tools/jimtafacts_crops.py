from jimtafacts_crafting import distinct
from pathlib import Path
import json,shutil,hashlib
ROOT=Path(__file__).resolve().parents[1];RES=ROOT/'src/main/resources';SOURCE=ROOT/'art/owner-library/originals/Oct23/Jimtafacts/industrialcraft reborn by jimbo/assets/ic2'
# Source registry names and original texture-stage counts. Breeding pairs are native, explicit and reachable.
CROPS={
 'weed':('jimta_weed','',0,0,'wheat','wheat'), 'wheat':('minecraft:wheat','',9,1,'wheat','wheat'),
 'pumpkin':('minecraft:pumpkin','',9,1,'pumpkin','pumpkin'),'melon':('minecraft:melon_slice','',9,1,'melon','melon'),
 'dandelion':('minecraft:yellow_dye','',9,1,'wheat','carrots'),'rose':('minecraft:red_dye','',9,1,'wheat','potato'),
 'blackthorn':('minecraft:black_dye','',9,2,'rose','brownMushroom'),'tulip':('minecraft:purple_dye','',9,2,'rose','cyazint'),
 'cyazint':('minecraft:cyan_dye','',9,2,'dandelion','reed'),'venomilia':('minecraft:purple_dye','',9,4,'tulip','netherWart'),
 'reed':('minecraft:sugar_cane','',9,1,'reed','reed'),'stickreed':('jugcraft:rubber','',9,4,'reed','blackthorn'),
 'cocoa':('minecraft:cocoa_beans','',9,2,'cocoa','cocoa'),'ferru':('minecraft:iron_nugget','iron',0,6,'wheat','redwheat'),
 'aurelia':('minecraft:gold_nugget','gold',0,8,'ferru','dandelion'),'redwheat':('minecraft:redstone','',0,6,'wheat','netherWart'),
 'netherWart':('minecraft:nether_wart','',0,3,'netherWart','netherWart'),'terraWart':('jimta_terra_wart','',0,5,'netherWart','brownMushroom'),
 'coffee':('jimta_coffee_beans','',9,7,'cocoa','blackthorn'),'hops':('jimta_hops','',9,5,'reed','wheat'),
 'carrots':('minecraft:carrot','',9,1,'carrots','carrots'),'potato':('minecraft:potato','',9,1,'potato','potato'),
 'redMushroom':('minecraft:red_mushroom','',0,2,'redMushroom','redMushroom'),'brownMushroom':('minecraft:brown_mushroom','',0,2,'brownMushroom','brownMushroom'),
 'eatingplant':('minecraft:cobweb','',0,6,'venomilia','blackthorn'),'cyprium':('minecraft:copper_nugget','copper',0,6,'ferru','rose'),
 'stagnium':('jugcraft:tin_nugget','tin',0,6,'ferru','reed'),'plumbiscus':('jugcraft:lead_nugget','lead',0,6,'ferru','blackthorn'),
 'shining':('jugcraft:silver_nugget','silver',0,8,'aurelia','cyazint')}
ART={'coffee_beans':'itemCofeeBeans','coffee_powder':'itemCofeePowder','hops':'itemHops','terra_wart':'itemTerraWart','weed':'itemWeed','empty_mug':'itemMugEmpty','coffee_mug':'itemMugCoffee_0','dark_coffee_mug':'itemMugCoffee_1','iced_coffee_mug':'itemMugCoffee_2','beer_mug':'ic2.itemMugBooze.beer.Beer','weed_ex':'itemWeedEx','weeding_trowel':'itemWeedingTrowel'}
ART['hydrating_cell']='itemCellHydrant'
def key(s):return ''.join('_'+c.lower() if c.isupper() else c for c in s)
def blocks():return {'jimta_crop_'+key(c) for c in CROPS}|{'jimta_crop_cross_sticks'}
def items():return {'jimta_seed_'+key(c) for c in CROPS}|{'jimta_'+c for c in ART}|{'jimta_crop_sticks'}
def dump(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
def generate(shared=True):
 manifest=[];lang={};catalog={}
 def copy(source,dest):dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source,dest);manifest.append({'source':source.relative_to(ROOT).as_posix(),'runtime':dest.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(source.read_bytes()).hexdigest()})
 stick='jugcraft:block/jimtafacts/crops/stick'
 copy(SOURCE/'textures/blocks/crop/stick.png',RES/'assets/jugcraft/textures/block/jimtafacts/crops/stick.png')
 for source,v in CROPS.items():
  name=key(source);product,anchor,light,tier,a,b=v
  if ':' not in product:product='jugcraft:'+product
  catalog[name]={'product':product,'anchor':anchor,'light':light,'tier':tier,'breed':[key(a),key(b)]}
  stages=sorted((SOURCE/'textures/blocks/crop').glob(source+'.*.png'))
  for i,path in enumerate(stages):copy(path,RES/f'assets/jugcraft/textures/block/jimtafacts/crops/{name}_{i}.png')
  variants={}
  for age in range(8):
   stage=min(len(stages)-1,age*len(stages)//8)
   texture=f'jugcraft:block/jimtafacts/crops/{name}_{stage}'
   # Crop and crop-stick pixels stay original; ordinary cutout crossed planes fit native crop rendering.
   model={'parent':'minecraft:block/crop','textures':{'crop':texture}}
   dump(RES/f'assets/jugcraft/models/block/jimta_crop_{name}_{age}.json',model)
   variants[f'age={age}']={'model':f'jugcraft:block/jimta_crop_{name}_{age}'}
  dump(RES/f'assets/jugcraft/blockstates/jimta_crop_{name}.json',{'variants':variants})
  copy(SOURCE/'textures/items/itemCropSeed.png',RES/f'assets/jugcraft/textures/item/jimtafacts/crops/seed_{name}.png')
  dump(RES/f'assets/jugcraft/models/item/jimta_seed_{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'jugcraft:item/jimtafacts/crops/seed_{name}'}})
  lang['item.jugcraft.jimta_seed_'+name]=name.replace('_',' ').title()+' Seed Bag';lang['block.jugcraft.jimta_crop_'+name]=name.replace('_',' ').title()+' Crop'
  dump(RES/f'data/jugcraft/loot_table/blocks/jimta_crop_{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:jimta_seed_'+name}]}]})
 dump(RES/'assets/jugcraft/models/block/jimta_crop_cross_sticks.json',{'parent':'minecraft:block/crop','textures':{'crop':stick}})
 dump(RES/'assets/jugcraft/blockstates/jimta_crop_cross_sticks.json',{'variants':{'':{'model':'jugcraft:block/jimta_crop_cross_sticks'}}})
 dump(RES/'assets/jugcraft/models/item/jimta_crop_sticks.json',{'parent':'minecraft:item/generated','textures':{'layer0':stick}})
 dump(RES/'data/jugcraft/loot_table/blocks/jimta_crop_cross_sticks.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:jimta_crop_sticks'}]}]})
 lang['item.jugcraft.jimta_crop_sticks']='Crossbreeding Crop Sticks';lang['block.jugcraft.jimta_crop_cross_sticks']='Crossbreeding Crop Sticks'
 for id,art in ART.items():
  copy(SOURCE/f'textures/items/{art}.png',RES/f'assets/jugcraft/textures/item/jimtafacts/crops/{id}.png')
  dump(RES/f'assets/jugcraft/models/item/jimta_{id}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'jugcraft:item/jimtafacts/crops/'+id}});lang['item.jugcraft.jimta_'+id]=id.replace('_',' ').title()
 for id in items():dump(RES/f'assets/jugcraft/items/{id}.json',{'model':{'type':'minecraft:model','model':'jugcraft:item/'+id}})
 dump(RES/'jugcraft/jimtafacts/crops.json',catalog)
 recipes={}
 def craft(id,parts,out,count=1):recipes[id]={'type':'minecraft:crafting_shapeless','category':'misc','ingredients':parts,'result':{'id':out,'count':count}}
 for name,item in {'wheat':'wheat_seeds','pumpkin':'pumpkin_seeds','melon':'melon_seeds','carrots':'carrot','potato':'potato','reed':'sugar_cane','cocoa':'cocoa_beans','nether_wart':'nether_wart','red_mushroom':'red_mushroom','brown_mushroom':'brown_mushroom'}.items():craft('seed_'+name,['minecraft:'+item,'minecraft:paper','minecraft:stick'],'jugcraft:jimta_seed_'+name)
 craft('sticks',['minecraft:stick']*4+['minecraft:string'],'jugcraft:jimta_crop_sticks',2)
 craft('mug',distinct('jimtafacts_crops/mug',['minecraft:brick']*3),'jugcraft:jimta_empty_mug')
 craft('coffee',['jugcraft:jimta_coffee_powder','jugcraft:jimta_empty_mug','minecraft:water_bucket'],'jugcraft:jimta_coffee_mug')
 craft('dark_coffee',['jugcraft:jimta_coffee_mug','jugcraft:jimta_coffee_powder','minecraft:sugar'],'jugcraft:jimta_dark_coffee_mug')
 craft('iced_coffee',['jugcraft:jimta_coffee_mug','minecraft:snowball','minecraft:sugar'],'jugcraft:jimta_iced_coffee_mug')
 craft('weeding_trowel',['minecraft:iron_ingot','minecraft:stick','minecraft:stick'],'jugcraft:jimta_weeding_trowel')
 craft('weed_ex',['minecraft:glass_bottle','minecraft:bone_meal','minecraft:spider_eye'],'jugcraft:jimta_weed_ex')
 craft('hydrating_cell',['jugcraft:tin_plate','minecraft:glass','jugcraft:rubber'],'jugcraft:jimta_hydrating_cell')
 recipes['coffee_powder']={'type':'jugcraft:crushing','ingredient':'jugcraft:jimta_coffee_beans','result':{'id':'jugcraft:jimta_coffee_powder','count':2},'time':100}
 recipes['brew_beer']={'type':'jugcraft:chemical_reaction','items':[{'ingredient':'jugcraft:jimta_hops'},{'ingredient':'jugcraft:jimta_empty_mug'}],'fluids':[{'fluid':'minecraft:water','amount':1000}],'results':[{'id':'jugcraft:jimta_beer_mug','count':1}],'energy_per_tick':4,'time':1200}
 recipes['ferment_hops']={'type':'jugcraft:chemical_reaction','capability':'jugcraft:controlled_fermentation','items':[{'ingredient':'jugcraft:jimta_hops','count':4}],'fluids':[{'fluid':'minecraft:water','amount':1000}],'fluid_results':[{'fluid':'jugcraft:ethanol_broth','amount':1000,'tank':0},{'fluid':'jugcraft:fermentation_exhaust','amount':250,'tank':1}],'energy_per_tick':8,'time':600}
 craft('compost_weeds',['jugcraft:jimta_weed']*4,'minecraft:bone_meal')
 for id,recipe in recipes.items():dump(RES/f'data/jugcraft/recipe/jimtafacts_crops/{id}.json',recipe)
 dump(RES/'data/jugcraft/tags/block/jimtafacts_crops.json',{'replace':False,'values':['jugcraft:'+b for b in sorted(blocks())]})
 lang['item.jugcraft.jimta_coffee_beans']='Cultivated Coffee Beans'
 if shared:
  path=RES/'assets/jugcraft/lang/en_us.json';value=json.loads(path.read_text(encoding='utf-8'));value.update(lang);dump(path,value)
  path=RES/'data/minecraft/tags/block/crops.json';value=json.loads(path.read_text(encoding='utf-8')) if path.exists() else {'replace':False,'values':[]};ref='#jugcraft:jimtafacts_crops'
  if ref not in value['values']:value['values'].append(ref)
  dump(path,value)
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/crop-runtime-copies.json',manifest)
 return catalog
if __name__=='__main__':
 import sys
 print(f'Generated {len(generate("--no-shared" not in sys.argv))} crop varieties, {len(items())} items.')
