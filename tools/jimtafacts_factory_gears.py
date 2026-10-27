"""Original four factory gear sprites and the source-style progressive crafting chain."""
import json,hashlib,shutil
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1];RES=ROOT/'src/main/resources'
SOURCE=ROOT/'art/owner-library/originals/Oct23/Jimtafacts/buildcraft reborn by jimbo/assets/buildcraftcore'
def dump(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
def generate(shared=True):
 records=[];lang={}
 for kind in ('wood','stone','gold','diamond'):
  ident='jimta_'+kind+'_gear';src=SOURCE/f'textures/items/gear_{kind}.png';dst=RES/f'assets/jugcraft/textures/item/jimtafacts/gear_{kind}.png';dst.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(src,dst)
  records.append({'source':src.relative_to(ROOT).as_posix(),'runtime':dst.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(src.read_bytes()).hexdigest(),'changes':'None'})
  dump(RES/f'assets/jugcraft/models/item/{ident}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'jugcraft:item/jimtafacts/gear_'+kind}})
  dump(RES/f'assets/jugcraft/items/{ident}.json',{'model':{'type':'minecraft:model','model':'jugcraft:item/'+ident}})
  lang['item.jugcraft.'+ident]=kind.title()+' Factory Gear'
 for kind,material,core in [('wood','minecraft:stick',None),('stone','minecraft:cobblestone','jugcraft:jimta_wood_gear'),('iron','minecraft:iron_ingot','jugcraft:jimta_stone_gear'),('gold','minecraft:gold_ingot','jugcraft:iron_gear'),('diamond','minecraft:diamond','jugcraft:jimta_gold_gear')]:
  key={'M':material};pattern=[' M ','M M',' M ']
  if core:key['C']=core;pattern[1]='MCM'
  dump(RES/f'data/jugcraft/recipe/jimtafacts_factory/gear_{kind}.json',{'type':'minecraft:crafting_shaped','category':'misc','pattern':pattern,'key':key,'result':{'id':'jugcraft:'+('iron_gear' if kind=='iron' else 'jimta_'+kind+'_gear')}})
 dump(RES/'data/jugcraft/recipe/jimtafacts_factory/classic_quarry.json',{'type':'minecraft:crafting_shaped','category':'misc','pattern':['IGI','GPG','DGD'],'key':{'I':'jugcraft:iron_gear','G':'jugcraft:jimta_gold_gear','D':'jugcraft:jimta_diamond_gear','P':'minecraft:diamond_pickaxe'},'result':{'id':'jugcraft:jimta_civil_quarry'}})
 if shared:
  p=RES/'assets/jugcraft/lang/en_us.json';v=json.loads(p.read_text());v.update(lang);dump(p,v)
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/factory-gear-copies.json',{'copies':records,'adaptation':'Original progressive wood/stone/iron/gold/diamond crafting; iron consolidates the existing native iron gear. Gold/diamond also supply an optional gear-built quarry recipe. Existing machine recipes remain available.'})
if __name__=='__main__':generate()
