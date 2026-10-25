"""Source recycling and reusable universal cells, on native processing/fluids."""
from pathlib import Path
import json,shutil,hashlib
ROOT=Path(__file__).resolve().parents[1];RES=ROOT/'src/main/resources';SOURCE=ROOT/'art/owner-library/originals/Oct23/Jimtafacts/industrialcraft reborn by jimbo/assets/ic2/textures/items'
ART={'scrap':'itemScrap','scrapbox':'itemScrapbox','plant_ball':'itemFuelPlantBall','biochaff':'itemBiochaff','fluid_cell':'cell/itemCellEmpty','tin_can':'itemTinCan','filled_tin_can':'itemTinCanFilled'}
CELLS=['itemCellEmpty','itemCellWater','itemCellLava','itemCellDistilledWater','itemCellCoolant','itemCellHotCoolant','itemCellSteam','itemCellSuperheatedSteam','itemCellBiogas','itemCellBiomass','itemCellBioRef','itemCellCoal','itemFluidCell']
def items():return {'jimta_'+key for key in ART}
def dump(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def generate(shared=True):
 manifest=[];lang={}
 def copy(source,name):
  src=SOURCE/(source+'.png');dest=RES/f'assets/jugcraft/textures/item/jimtafacts/production/{name}.png';dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(src,dest)
  manifest.append({'source':src.relative_to(ROOT).as_posix(),'runtime':dest.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(src.read_bytes()).hexdigest()})
  return 'jugcraft:item/jimtafacts/production/'+name
 for key,art in ART.items():
  id='jimta_'+key;texture=copy(art,key);lang['item.jugcraft.'+id]=key.replace('_',' ').title()
  dump(RES/f'assets/jugcraft/models/item/{id}.json',{'parent':'minecraft:item/generated','textures':{'layer0':texture}})
  dump(RES/f'assets/jugcraft/items/{id}.json',{'model':{'type':'minecraft:model','model':'jugcraft:item/'+id}})
 frames=[]
 for n,source in enumerate(CELLS):
  texture=copy('cell/'+source,'cell_'+str(n));model='jimta_fluid_cell_'+str(n)
  dump(RES/f'assets/jugcraft/models/item/{model}.json',{'parent':'minecraft:item/generated','textures':{'layer0':texture}})
  frames.append({'threshold':n,'model':{'type':'minecraft:model','model':'jugcraft:item/'+model}})
 dump(RES/'assets/jugcraft/items/jimta_fluid_cell.json',{'model':{'type':'minecraft:range_dispatch','property':'minecraft:custom_model_data','index':0,'entries':frames,'fallback':frames[0]['model']}})
 recipes={}
 def craft(name,parts,result,count=1):recipes[name]={'type':'minecraft:crafting_shapeless','category':'misc','ingredients':parts,'result':{'id':result,'count':count}}
 craft('scrapbox',['jugcraft:jimta_scrap']*9,'jugcraft:jimta_scrapbox')
 craft('plant_ball',['#jugcraft:jimta_plant_material']*8,'jugcraft:jimta_plant_ball')
 craft('biochaff_fertilizer',['jugcraft:jimta_biochaff','minecraft:bone_meal'],'jugcraft:fertilizer',2)
 craft('tin_cans',['jugcraft:tin_plate']*3,'jugcraft:jimta_tin_can',6)
 recipes['grind_plant_ball']={'type':'jugcraft:crushing','ingredient':'jugcraft:jimta_plant_ball','result':{'id':'jugcraft:jimta_biochaff','count':2},'time':100}
 recipes['ferment_biochaff']={'type':'jugcraft:chemical_reaction','capability':'jugcraft:controlled_fermentation','items':[{'ingredient':'jugcraft:jimta_biochaff','count':2}],'fluids':[{'fluid':'minecraft:water','amount':1000}],'fluid_results':[{'fluid':'jugcraft:ethanol_broth','amount':1000,'tank':0},{'fluid':'jugcraft:fermentation_exhaust','amount':250,'tank':1}],'energy_per_tick':8,'time':600}
 for name,value in recipes.items():dump(RES/f'data/jugcraft/recipe/jimtafacts_production/{name}.json',value)
 dump(RES/'data/jugcraft/tags/item/jimta_recyclable.json',{'replace':False,'values':['minecraft:cobblestone','minecraft:cobbled_deepslate','minecraft:dirt','minecraft:gravel','minecraft:netherrack','minecraft:rotten_flesh','minecraft:poisonous_potato','minecraft:flint','jugcraft:jimta_stone_dust','jugcraft:jimta_weed']})
 dump(RES/'data/jugcraft/tags/item/jimta_plant_material.json',{'replace':False,'values':['minecraft:wheat','minecraft:carrot','minecraft:potato','minecraft:beetroot','minecraft:sugar_cane','minecraft:kelp','minecraft:seagrass','jugcraft:jimta_weed']})
 # Deliberately conservative recovery, separate from convenience transmutation. A box consumes 54 junk + power.
 entries=[{'type':'minecraft:item','name':'minecraft:'+item,'weight':weight} for item,weight in [('bone',12),('string',12),('wheat_seeds',12),('copper_nugget',12),('iron_nugget',10),('gold_nugget',5),('redstone',3),('coal',6),('leather',6),('slime_ball',2),('flint',10)]]
 dump(RES/'data/jugcraft/loot_table/jimtafacts/scrapbox.json',{'type':'minecraft:gift','pools':[{'rolls':1,'entries':entries}]})
 if shared:
  path=RES/'assets/jugcraft/lang/en_us.json';value=json.loads(path.read_text(encoding='utf-8'));value.update(lang);dump(path,value)
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/production-runtime-copies.json',manifest)
 return items()
if __name__=='__main__':
 import sys
 print(f'Generated {len(generate("--no-shared" not in sys.argv))} industrial production items.')
