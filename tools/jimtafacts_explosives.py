"""Owner-supplied dynamite pixels, untouched, with native bounded gameplay and placement geometry."""
from pathlib import Path
import hashlib,json,shutil
ROOT=Path(__file__).resolve().parents[1];RES=ROOT/'src/main/resources'
SOURCE=ROOT/'art/owner-library/originals/Oct23/Jimtafacts/industrialcraft reborn by jimbo/assets/ic2'
ICONS={'dynamite':'itemDynamite','sticky_dynamite':'itemDynamiteSticky','dynamite_remote':'itemRemote'}
def items():return {'jimta_'+name for name in ICONS}|{'jimta_industrial_tnt'}
def dump(path,value):path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')
def generate(shared=True):
 copies=[];lang={}
 def copy(source,target):
  target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source,target)
  copies.append({'source':source.relative_to(ROOT).as_posix(),'runtime':target.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'changes':'None; original pixels retained.'})
 for name,icon in ICONS.items():
  id='jimta_'+name;copy(SOURCE/f'textures/items/{icon}.png',RES/f'assets/jugcraft/textures/item/jimtafacts/gear/{id}.png')
  dump(RES/f'assets/jugcraft/models/item/{id}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'jugcraft:item/jimtafacts/gear/'+id}})
  dump(RES/f'assets/jugcraft/items/{id}.json',{'model':{'type':'minecraft:model','model':'jugcraft:item/'+id}})
  lang['item.jugcraft.'+id]=name.replace('_',' ').title()
 for linked in (False,True):
  suffix='_remote' if linked else '';src='blockDynamiteRemote' if linked else 'blockDynamite'
  copy(SOURCE/f'textures/blocks/{src}.png',RES/f'assets/jugcraft/textures/block/jimtafacts/dynamite{suffix}.png')
  dump(RES/f'assets/jugcraft/models/block/jimta_dynamite_charge{suffix}.json',{'parent':'minecraft:block/block','textures':{'all':'jugcraft:block/jimtafacts/dynamite'+suffix,'particle':'#all'},'elements':[{'from':[6,0,6],'to':[10,10,10],'faces':{f:{'texture':'#all','uv':[0,0,16,16]} for f in ('up','down','north','south','east','west')}}]})
 variants={}
 for face,rotation in {'up':{},'down':{'x':180},'north':{'x':90},'south':{'x':90,'y':180},'east':{'x':90,'y':90},'west':{'x':90,'y':270}}.items():
  for linked in (False,True):variants[f'facing={face},linked={str(linked).lower()}']={'model':'jugcraft:block/jimta_dynamite_charge'+('_remote' if linked else ''),**rotation}
 dump(RES/'assets/jugcraft/blockstates/jimta_dynamite_charge.json',{'variants':variants})
 dump(RES/'data/jugcraft/loot_table/blocks/jimta_dynamite_charge.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:jimta_dynamite'}],'condition':{'type':'minecraft:survives_explosion'}}]})
 recipes={'dynamite':(['minecraft:gunpowder','minecraft:gunpowder','minecraft:paper','minecraft:string'],2),'sticky_dynamite':(['jugcraft:jimta_dynamite','minecraft:slime_ball'],1),'dynamite_remote':(['jugcraft:basic_circuit','jugcraft:copper_wire','minecraft:stone_button','minecraft:redstone'],1)}
 for name,(ingredients,count) in recipes.items():dump(RES/f'data/jugcraft/recipe/jimtafacts_gear/{name}.json',{'type':'minecraft:crafting_shapeless','category':'equipment','ingredients':ingredients,'result':{'id':'jugcraft:jimta_'+name,'count':count}})
 lang['block.jugcraft.jimta_dynamite_charge']='Placed Dynamite'
 copy(SOURCE/'textures/blocks/blockITNT.png',RES/'assets/jugcraft/textures/block/jimtafacts/industrial_tnt.png')
 faces={face:{'texture':'#sheet','uv':[16*tile/6,0,16*(tile+1)/6,16],'cullface':face} for face,tile in zip(('down','up','north','south','west','east'),range(6))}
 dump(RES/'assets/jugcraft/models/block/jimta_industrial_tnt.json',{'parent':'minecraft:block/block','textures':{'sheet':'jugcraft:block/jimtafacts/industrial_tnt','particle':'#sheet'},'elements':[{'from':[0,0,0],'to':[16,16,16],'faces':faces}]})
 dump(RES/'assets/jugcraft/blockstates/jimta_industrial_tnt.json',{'variants':{'':{'model':'jugcraft:block/jimta_industrial_tnt'}}})
 dump(RES/'assets/jugcraft/models/item/jimta_industrial_tnt.json',{'parent':'jugcraft:block/jimta_industrial_tnt'})
 dump(RES/'assets/jugcraft/items/jimta_industrial_tnt.json',{'model':{'type':'minecraft:model','model':'jugcraft:item/jimta_industrial_tnt'}})
 dump(RES/'data/jugcraft/loot_table/blocks/jimta_industrial_tnt.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:jimta_industrial_tnt'}],'condition':{'type':'minecraft:survives_explosion'}}]})
 dump(RES/'data/jugcraft/recipe/jimtafacts_gear/industrial_tnt.json',{'type':'minecraft:crafting_shapeless','category':'equipment','ingredients':['minecraft:tnt','minecraft:flint','minecraft:flint','jugcraft:rubber'],'result':{'id':'jugcraft:jimta_industrial_tnt'}})
 lang['block.jugcraft.jimta_industrial_tnt']='Industrial TNT'
 lang['item.jugcraft.jimta_industrial_tnt']='Industrial TNT'
 if shared:
  p=RES/'assets/jugcraft/lang/en_us.json';v=json.loads(p.read_text());v.update(lang);dump(p,v)
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/dynamite-runtime-copies.json',{'copies':copies,'adaptation':'Original dynamite/sticky/remote roles and icons; original full six-face Industrial TNT atlas. Four-second thrown/manual fuse, two-second remote fuse; sixteen UUID-bound remote links with loaded same-dimension 64-block range. Industrial TNT has a four-block bounded burst rather than dynamite two-block burst, with retained block drops. Blasts recheck owner/claims, skip inventories, liquids, TNT and high resistance. Bound dynamite dispenser automation is supported. No unrestricted legacy explosion or nuclear-device parity.'})
if __name__=='__main__':generate()
