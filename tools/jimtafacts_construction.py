"""Original IndustrialCraft CF walls, scaffolds and construction tools, using native blocks."""
from pathlib import Path
import hashlib,json,shutil
ROOT=Path(__file__).resolve().parents[1]; RES=ROOT/'src/main/resources'
SOURCE=ROOT/'art/owner-library/originals/Oct23/Jimtafacts/industrialcraft reborn by jimbo/assets/ic2'
COLORS=('white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black')
BLOCKS={**{'cf_wall_'+c:'cf/blockWall.'+{'light_blue':'lightBlue','light_gray':'lightGray'}.get(c,c) for c in COLORS},
 'construction_foam':'cf/blockFoam','reinforced_foam':'cf/blockReinforcedFoam','reinforced_stone':'blockAlloy',
 'wood_scaffold':'blockScaffold','wood_scaffold_reinforced':'blockScaffold.reinforced',
 'iron_scaffold':'blockIronScaffold','iron_scaffold_reinforced':'blockIronScaffold.reinforced',
 'alloy_glass':'blockAlloyGlass','alloy_door':'blockDoorAlloy','iron_fence':'blockFenceIron'}
ART={'cf_sprayer':'itemFoamSprayer','painter':'itemToolPainter','cf_powder':'itemPartCFPowder'}
def blocks():return {'jimta_'+n for n in BLOCKS}
def items():return blocks()|{'jimta_'+n for n in ART}
def dump(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
def generate(shared=True):
 manifest=[];lang={}
 def texture(source,relative):
  src=SOURCE/f'textures/{source}.png';dst=RES/f'assets/jugcraft/textures/jimtafacts/construction/{relative}.png';dst.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(src,dst)
  manifest.append({'source':src.relative_to(ROOT).as_posix(),'runtime':dst.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(src.read_bytes()).hexdigest()})
  return 'jugcraft:jimtafacts/construction/'+relative
 def loot(name):
  ident='jimta_'+name;condition={'type':'minecraft:survives_explosion'}
  if name=='alloy_door':condition={'type':'minecraft:all_of','terms':[condition,{'type':'minecraft:match_block','blocks':'jugcraft:'+ident,'state':{'half':'lower'}}]}
  pools=[] if name in ('construction_foam','reinforced_foam') else [{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:'+ident}],'condition':condition}]
  dump(RES/f'data/jugcraft/loot_table/blocks/{ident}.json',{'type':'minecraft:block','pools':pools})
 for name,source in BLOCKS.items():
  ident='jimta_'+name;lang['block.jugcraft.'+ident]=name.replace('_',' ').replace('cf ','CF ').title();loot(name)
  if name=='alloy_door':
   top=texture('blocks/'+source+'.top',name+'_top');bottom=texture('blocks/'+source+'.bottom',name+'_bottom');states={}
   for half,part in [('lower','bottom'),('upper','top')]:
    for hinge in ('left','right'):
     for opened in (False,True):
      suffix=f'{part}_{hinge}'+('_open' if opened else '');model=ident+'_'+suffix
      dump(RES/f'assets/jugcraft/models/block/{model}.json',{'parent':'minecraft:block/door_'+suffix,'textures':{'top':top,'bottom':bottom}})
      for facing,rotation in [('east',0),('south',90),('west',180),('north',270)]:
       states[f'facing={facing},half={half},hinge={hinge},open={str(opened).lower()}']={'model':'jugcraft:block/'+model,'y':(rotation+(90 if hinge=='left' else -90) if opened else rotation)%360}
   dump(RES/f'assets/jugcraft/blockstates/{ident}.json',{'variants':states})
   icon=texture('items/itemDoorAlloy',name+'_item');dump(RES/f'assets/jugcraft/models/item/{ident}.json',{'parent':'minecraft:item/generated','textures':{'layer0':icon}})
   dump(RES/f'assets/jugcraft/items/{ident}.json',{'model':{'type':'minecraft:model','model':'jugcraft:item/'+ident}});continue
  tex=texture('blocks/'+source,name)
  if name=='iron_fence':
   parts=[]
   for variant in ('post','post_ends','cap','cap_alt','side','side_alt'):
    dump(RES/f'assets/jugcraft/models/block/{ident}_{variant}.json',{'parent':'minecraft:block/iron_bars_'+variant,'textures':{'bars':tex,'edge':tex,'particle':tex}})
   parts=[{'apply':{'model':f'jugcraft:block/{ident}_post_ends'}},{'when':{'north':'false','south':'false','east':'false','west':'false'},'apply':{'model':f'jugcraft:block/{ident}_post'}}]
   for direction,rotation in [('north',0),('east',90),('south',180),('west',270)]:parts.append({'when':{direction:'true'},'apply':{'model':f'jugcraft:block/{ident}_side','y':rotation}})
   dump(RES/f'assets/jugcraft/blockstates/{ident}.json',{'multipart':parts})
   dump(RES/f'assets/jugcraft/models/item/{ident}.json',{'parent':'minecraft:item/generated','textures':{'layer0':tex}})
   dump(RES/f'assets/jugcraft/items/{ident}.json',{'model':{'type':'minecraft:model','model':'jugcraft:item/'+ident}});continue
  if 'scaffold' in name:
   # Source atlases have six authored faces; retain the original 96x16 pixels.
   faces={face:{'texture':'#all','uv':[16*index/6,0,16*(index+1)/6,16]} for index,face in enumerate(('down','up','north','south','west','east'))}
   model={'parent':'minecraft:block/block','textures':{'all':tex,'particle':tex},'elements':[{'from':[0,0,0],'to':[16,16,16],'faces':faces}]}
  else:model={'parent':'minecraft:block/cube_all','textures':{'all':tex}}
  dump(RES/f'assets/jugcraft/models/block/{ident}.json',model);dump(RES/f'assets/jugcraft/blockstates/{ident}.json',{'variants':{'':{'model':'jugcraft:block/'+ident}}})
  dump(RES/f'assets/jugcraft/items/{ident}.json',{'model':{'type':'minecraft:model','model':'jugcraft:block/'+ident}})
 for name,source in ART.items():
  ident='jimta_'+name;tex=texture('items/'+source,name);model={'type':'minecraft:model','model':'jugcraft:item/'+ident}
  dump(RES/f'assets/jugcraft/models/item/{ident}.json',{'parent':'minecraft:item/handheld','textures':{'layer0':tex}})
  if name=='painter':
   variants=[{'threshold':0,'model':model}]
   for index,color in enumerate(COLORS):
    source_name={'light_blue':'Cloud','light_gray':'LightGrey','gray':'DarkGrey'}.get(color,color.title())
    tex=texture('items/itemToolPainter'+source_name,'painter_'+color);ref=ident+'_'+color
    dump(RES/f'assets/jugcraft/models/item/{ref}.json',{'parent':'minecraft:item/handheld','textures':{'layer0':tex}});variants.append({'threshold':index+1,'model':{'type':'minecraft:model','model':'jugcraft:item/'+ref}})
   model={'type':'minecraft:range_dispatch','property':'minecraft:custom_model_data','index':0,'entries':variants,'fallback':model}
  dump(RES/f'assets/jugcraft/items/{ident}.json',{'model':model});lang['item.jugcraft.'+ident]={'cf_sprayer':'Construction Foam Sprayer','cf_powder':'Construction Foam Powder','painter':'Painter'}[name]
 def craft(name,parts,result,count=1):dump(RES/f'data/jugcraft/recipe/jimtafacts_construction/{name}.json',{'type':'minecraft:crafting_shapeless','category':'building','ingredients':parts,'result':{'id':result,'count':count}})
 craft('powder',['minecraft:clay_ball','minecraft:sand','minecraft:redstone'],'jugcraft:jimta_cf_powder',3)
 craft('foam_canister',['jugcraft:jimta_cf_powder','jugcraft:tin_plate','minecraft:water_bucket'],'jugcraft:foam_canister')
 craft('sprayer',['jugcraft:tin_plate','jugcraft:iron_plate','minecraft:glass_bottle'],'jugcraft:jimta_cf_sprayer')
 craft('painter',['minecraft:white_wool','minecraft:stick','minecraft:iron_nugget'],'jugcraft:jimta_painter')
 craft('wood_scaffold',['#minecraft:planks']*3+['minecraft:stick']*3,'jugcraft:jimta_wood_scaffold',6)
 craft('iron_scaffold',['minecraft:iron_ingot']*3+['minecraft:iron_bars']*3,'jugcraft:jimta_iron_scaffold',6)
 craft('wood_scaffold_reinforced',['jugcraft:jimta_wood_scaffold','minecraft:stick','minecraft:stick'],'jugcraft:jimta_wood_scaffold_reinforced')
 craft('iron_scaffold_reinforced',['jugcraft:jimta_iron_scaffold','jugcraft:jimta_iron_fence'],'jugcraft:jimta_iron_scaffold_reinforced')
 craft('iron_fence',['minecraft:iron_bars','jugcraft:steel_plate'],'jugcraft:jimta_iron_fence',2)
 craft('alloy_glass',['minecraft:glass']*4+['jugcraft:steel_plate'],'jugcraft:jimta_alloy_glass',4)
 craft('alloy_door',['jugcraft:steel_plate']*6+['minecraft:iron_door'],'jugcraft:jimta_alloy_door')
 craft('reinforced_stone',['jugcraft:blastproof_concrete','jugcraft:steel_plate'],'jugcraft:jimta_reinforced_stone')
 for color in COLORS:craft('wall_'+color,['jugcraft:concrete','minecraft:'+color+'_dye'],'jugcraft:jimta_cf_wall_'+color)
 tags={'mineable/pickaxe':[n for n in BLOCKS if 'wood_scaffold' not in n],'mineable/axe':[n for n in BLOCKS if 'wood_scaffold' in n],'climbable':[n for n in BLOCKS if 'scaffold' in n],'doors':['alloy_door']}
 if shared:
  path=RES/'assets/jugcraft/lang/en_us.json';data=json.loads(path.read_text(encoding='utf-8'));data.update(lang);dump(path,data)
  for tag,names in tags.items():
   path=RES/f'data/minecraft/tags/block/{tag}.json';data=json.loads(path.read_text(encoding='utf-8')) if path.exists() else {'replace':False,'values':[]};data['values']=list(dict.fromkeys(data.get('values',[])+['jugcraft:jimta_'+n for n in names]));dump(path,data)
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/construction-runtime-copies.json',manifest)
 return items()
if __name__=='__main__':
 import sys
 print(f'Generated {len(generate("--no-shared" not in sys.argv))} construction entries.')
