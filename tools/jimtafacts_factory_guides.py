"""Original factory guide and guide-note art; native bounded field instructions."""
import json,hashlib,shutil
from jimtafacts_factory import ROOT,RES,SOURCE,dump

def generate(shared=True):
 manifest=[]
 for name,source,parts in [('factory_guide','guide_book',['minecraft:book','jugcraft:jimta_wood_gear']),('factory_guide_note','guide_note',['minecraft:paper','minecraft:ink_sac','jugcraft:jimta_wood_gear'])]:
  ident='jimta_'+name;src=SOURCE/f'buildcraftlib/textures/items/{source}.png';target=RES/f'assets/jugcraft/textures/item/jimtafacts/{ident}.png';target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(src,target)
  dump(RES/f'assets/jugcraft/models/item/{ident}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'jugcraft:item/jimtafacts/'+ident}})
  dump(RES/f'assets/jugcraft/items/{ident}.json',{'model':{'type':'minecraft:model','model':'jugcraft:item/'+ident}})
  dump(RES/f'data/jugcraft/recipe/jimtafacts_factory/{ident}.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':parts,'result':{'id':'jugcraft:'+ident}})
  manifest.append({'source':src.relative_to(ROOT).as_posix(),'runtime':target.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(src.read_bytes()).hexdigest()})
 if shared:
  p=RES/'assets/jugcraft/lang/en_us.json';data=json.loads(p.read_text());data.update({'item.jugcraft.jimta_factory_guide':'Factory Field Guide','item.jugcraft.jimta_factory_guide_note':'Factory Topic Note'});dump(p,data)
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/factory-guide-runtime-copies.json',manifest)
if __name__=='__main__':generate()
