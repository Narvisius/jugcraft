from jimtafacts_crafting import distinct
"""Original tool, toolbox and barrel art with explicit native item/recipe registration."""
from pathlib import Path
import hashlib,json,shutil
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
SOURCE=ROOT/'art/owner-library/originals/Oct23/Jimtafacts/industrialcraft reborn by jimbo/assets/ic2'
TOOLS={'forge_hammer':'itemToolForgeHammer','wire_cutters':'itemToolCutter','toolbox':'itemToolbox','lathing_tool':'itemLathingTool','containment_box':'itemContainmentbox','obscurator':'obscurator','mfsu_upgrade_kit':'itemMFSUupgradekit'}
DRINKS={'brew':'Brew','youngster':'Youngster','beer':'Beer','ale':'Ale','dragonblood':'Dragonblood','black_stuff':'Black Stuff','rum':None}
BOATS={'rubber_boat':'boatRubber','carbon_boat':'boatCarbon','electric_boat':'boatElectric','broken_rubber_boat':'boatRubberBroken'}
def blocks():return {'jimta_brewing_barrel','jimta_luminator','jimta_lathe'}
def items():return blocks()|{'jimta_'+name for name in TOOLS}|{'jimta_barrel_'+name for name in DRINKS}|{'jimta_'+name for name in BOATS}
def dump(path,value):
 path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(value,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
def generate(shared=True):
 manifest=[];lang={}
 def copy(source,target):
  target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source,target)
  manifest.append({'source':source.relative_to(ROOT).as_posix(),'runtime':target.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(source.read_bytes()).hexdigest()})
 def icon(name,source,hand=False,definition=True):
  copy(SOURCE/f'textures/items/{source}.png',RES/f'assets/jugcraft/textures/item/jimtafacts/utilities/{name}.png')
  dump(RES/f'assets/jugcraft/models/item/{name}.json',{'parent':'minecraft:item/handheld' if hand else 'minecraft:item/generated','textures':{'layer0':'jugcraft:item/jimtafacts/utilities/'+name}})
  if definition:dump(RES/f'assets/jugcraft/items/{name}.json',{'model':{'type':'minecraft:model','model':'jugcraft:item/'+name}})
 for name,source in TOOLS.items():
  icon('jimta_'+name,source,name not in ('toolbox','containment_box'));lang['item.jugcraft.jimta_'+name]=name.replace('_',' ').title()
 copy(SOURCE/'textures/gui/GUIContainmentbox.png',RES/'assets/jugcraft/textures/gui/jimtafacts/containment_box.png')
 icon('jimta_toolbox_open','itemToolbox.open',definition=False)
 dump(RES/'assets/jugcraft/items/jimta_toolbox.json',{'model':{'type':'minecraft:range_dispatch','property':'minecraft:custom_model_data','index':0,'fallback':{'type':'minecraft:model','model':'jugcraft:item/jimta_toolbox'},'entries':[{'threshold':1,'model':{'type':'minecraft:model','model':'jugcraft:item/jimta_toolbox_open'}}]}})
 for name,source in DRINKS.items():
  icon('jimta_barrel_'+name,'ic2.itemMugBooze.rum' if source is None else 'ic2.itemMugBooze.beer.'+source)
  lang['item.jugcraft.jimta_barrel_'+name]='Barrel '+name.replace('_',' ').title()
 for name,source in BOATS.items():
  icon('jimta_'+name,source);lang['item.jugcraft.jimta_'+name]=name.replace('_',' ').title()
  if not name.startswith('broken'):
   material=name.removesuffix('_boat');copy(SOURCE/f'textures/models/{source}.png',RES/f'assets/jugcraft/textures/entity/jimtafacts/boat_{material}.png')
   lang['entity.jugcraft.jimta_'+name]=name.replace('_',' ').title()
 icon('jimta_brewing_barrel','itemBarrel');lang['block.jugcraft.jimta_brewing_barrel']='Brewing Barrel'
 for tapped in (False,True):
  name='jimta_brewing_barrel'+('_tapped' if tapped else '')
  for suffix in ('','.tap') if tapped else ('',):
   copy(SOURCE/f'textures/blocks/blockBarrel{suffix}.png',RES/f'assets/jugcraft/textures/block/jimtafacts/utilities/barrel{suffix}.png')
  faces={}
  for face,tile in zip(('down','up','north','south','west','east'),(0,1,3,5,4,2)):
   faces[face]={'texture':'#tap' if tapped and face=='north' else '#sheet','uv':[16*tile/6,0,16*(tile+1)/6,16],'cullface':face}
  textures={'sheet':'jugcraft:block/jimtafacts/utilities/barrel','particle':'#sheet'}
  if tapped:textures['tap']='jugcraft:block/jimtafacts/utilities/barrel.tap'
  dump(RES/f'assets/jugcraft/models/block/{name}.json',{'parent':'minecraft:block/block','textures':textures,'elements':[{'from':[0,0,0],'to':[16,16,16],'faces':faces}]})
 dump(RES/'assets/jugcraft/blockstates/jimta_brewing_barrel.json',{'variants':{
  f'facing={face},tapped={str(tapped).lower()}':{'model':'jugcraft:block/jimta_brewing_barrel'+('_tapped' if tapped else ''),'y':rot}
  for tapped in (False,True) for face,rot in [('north',0),('east',90),('south',180),('west',270)]}})
 # Node removal returns exactly one portable barrel with its batch/tap data. No second empty loot item.
 dump(RES/'data/jugcraft/loot_table/blocks/jimta_brewing_barrel.json',{'type':'minecraft:block','pools':[]})
 # Original source bounds: floor plate, ceiling pad or wall fixture; cable support uses its compact silhouette.
 for lit in (False,True):
  copy(SOURCE/f'textures/blocks/wiring/blockLuminator{"" if lit else "Dark"}.png',RES/f'assets/jugcraft/textures/block/jimtafacts/utilities/luminator_{str(lit).lower()}.png')
 variants={}
 for compact in (False,True):
  boxes={'up':([0,0,0],[16,1,16]),'down':([4,13,4],[12,16,12]) if compact else ([0,15,0],[16,16,16]),
   'north':([6,3,14],[10,13,16]) if compact else ([0,0,15],[16,16,16]),'south':([6,3,0],[10,13,2]) if compact else ([0,0,0],[16,16,1]),
   'west':([14,3,6],[16,13,10]) if compact else ([15,0,0],[16,16,16]),'east':([0,3,6],[2,13,10]) if compact else ([0,0,0],[1,16,16])}
  for facing,(low,high) in boxes.items():
   for lit in (False,True):
    name=f'jimta_luminator_{facing}_{str(compact).lower()}_{str(lit).lower()}';x,y,z=low;X,Y,Z=high
    coords={'down':[x,16-Z,X,16-z],'up':[x,z,X,Z],'north':[16-X,16-Y,16-x,16-y],'south':[x,16-Y,X,16-y],'west':[z,16-Y,Z,16-y],'east':[16-Z,16-Y,16-z,16-y]}
    faces={}
    for face,tile in zip(('down','up','north','south','west','east'),(0,1,3,5,4,2)):
     u,v,U,V=coords[face];faces[face]={'texture':'#sheet','uv':[(16*tile+u)/6,v,(16*tile+U)/6,V]}
    dump(RES/f'assets/jugcraft/models/block/{name}.json',{'parent':'minecraft:block/block','textures':{'sheet':f'jugcraft:block/jimtafacts/utilities/luminator_{str(lit).lower()}','particle':'#sheet'},'elements':[{'from':low,'to':high,'faces':faces}]})
    variants[f'facing={facing},compact={str(compact).lower()},lit={str(lit).lower()}']={'model':'jugcraft:block/'+name}
 dump(RES/'assets/jugcraft/blockstates/jimta_luminator.json',{'variants':variants})
 dump(RES/'assets/jugcraft/items/jimta_luminator.json',{'model':{'type':'minecraft:model','model':'jugcraft:block/jimta_luminator_north_false_false'}})
 dump(RES/'data/jugcraft/loot_table/blocks/jimta_luminator.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:jimta_luminator'}],'condition':{'type':'minecraft:survives_explosion'}}]})
 lang['block.jugcraft.jimta_luminator']='Industrial Luminator'
 copy(SOURCE/'textures/blocks/machine/blockLathe.png',RES/'assets/jugcraft/textures/block/jimtafacts/utilities/lathe.png')
 copy(SOURCE/'textures/gui/GUILathe.png',RES/'assets/jugcraft/textures/gui/jimtafacts/lathe.png')
 with Image.open(SOURCE/'textures/blocks/machine/blockLathe.png') as sheet:w,h=sheet.size
 tiles=w//h;variants={}
 for lit in (False,True):
  name='jimta_lathe'+('_active' if lit else '');faces={}
  for face,index in zip(('down','up','north','south','west','east'),(0,1,3,5,4,2)):
   tile=index+(6 if lit and tiles>=12 else 0);faces[face]={'texture':'#sheet','uv':[16*tile/tiles,0,16*(tile+1)/tiles,16],'cullface':face}
  dump(RES/f'assets/jugcraft/models/block/{name}.json',{'parent':'minecraft:block/block','textures':{'sheet':'jugcraft:block/jimtafacts/utilities/lathe','particle':'#sheet'},'elements':[{'from':[0,0,0],'to':[16,16,16],'faces':faces}]})
  for face,y in [('north',0),('east',90),('south',180),('west',270)]:variants[f'facing={face},lit={str(lit).lower()}']={'model':'jugcraft:block/'+name,'y':y}
 dump(RES/'assets/jugcraft/blockstates/jimta_lathe.json',{'variants':variants})
 dump(RES/'assets/jugcraft/items/jimta_lathe.json',{'model':{'type':'minecraft:model','model':'jugcraft:block/jimta_lathe'}})
 dump(RES/'data/jugcraft/loot_table/blocks/jimta_lathe.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:jimta_lathe'}],'condition':{'type':'minecraft:survives_explosion'}}]})
 lang['block.jugcraft.jimta_lathe']='Kinetic Lathe'
 # Retain source turnable sprite crops, including each changed diameter in inventory and held displays.
 for material,max_width in [('wood',3),('iron',5)]:
  source=SOURCE/f'textures/items/turnables/{material.upper()}.png'
  copy(source,RES/f'assets/jugcraft/textures/gui/jimtafacts/turnable_{material}.png')
  copy(source,RES/f'assets/jugcraft/textures/item/jimtafacts/utilities/turnable_{material}.png')
  segments=[]
  for i in range(5):
   entries=[]
   for width in range(1,max_width+1):
    size=int(32*width/max_width+.5);offset=int((32-size)/2+.5);name=f'jimta_turnable_{material}_{i}_{width}'
    uv=[24*i/16,offset/16,24*(i+1)/16,(offset+size)/16]
    element={'from':[.5+3*i,8-size/8,8],'to':[.5+3*(i+1),8+size/8,8],
      'faces':{'north':{'texture':'#blank','uv':[uv[2],uv[1],uv[0],uv[3]]},'south':{'texture':'#blank','uv':uv}}}
    dump(RES/f'assets/jugcraft/models/item/{name}.json',{'parent':'minecraft:block/block','gui_light':'front','ambientocclusion':False,'display':{'gui':{'rotation':[0,0,0],'translation':[0,0,0],'scale':[1,1,1]}},'textures':{'blank':f'jugcraft:item/jimtafacts/utilities/turnable_{material}','particle':'#blank'},'elements':[element]})
    entries.append({'threshold':width,'model':{'type':'minecraft:model','model':'jugcraft:item/'+name}})
   segments.append({'type':'minecraft:range_dispatch','property':'minecraft:custom_model_data','index':i,'fallback':entries[-1]['model'],'entries':entries})
  dump(RES/f'assets/jugcraft/items/jimta_{material}_turning_blank.json',{'model':{'type':'minecraft:composite','models':segments}})
 for name,profile in [('thin_fluted_axle',[3,2,3,2,3]),('turned_handle',[4,3,2,2,2]),('turned_drill',[5,4,3,2,1])]:
  dump(RES/f'assets/jugcraft/items/jimta_{name}.json',{'model':{'type':'minecraft:composite','models':[{'type':'minecraft:model','model':f'jugcraft:item/jimta_turnable_iron_{i}_{width}'} for i,width in enumerate(profile)]}})
 recipes={
  'forge_hammer':['minecraft:iron_ingot']*5+['minecraft:stick']*2,
  'wire_cutters':['minecraft:iron_ingot']*3+['minecraft:stick']*2,
  'lathing_tool':['jugcraft:steel_plate']*2+['minecraft:stick'],
  'lathe':['jugcraft:iron_plate']*3+['jugcraft:machine_casing','jugcraft:iron_shaft','jugcraft:iron_gear'],
  'rubber_boat':['jugcraft:rubber']*5,
  'carbon_boat':['jugcraft:jimta_carbon_plate']*5,
  'electric_boat':['jugcraft:jimta_carbon_boat','jugcraft:electric_motor','jugcraft:basic_circuit','jugcraft:copper_wire','jugcraft:steel_plate'],
  'toolbox':['minecraft:chest','jugcraft:bronze_plate','jugcraft:bronze_plate','jugcraft:bronze_plate'],
  'containment_box':['minecraft:chest']+['jugcraft:lead_plate']*4+['jugcraft:rubber']*2,
  'obscurator':['jugcraft:advanced_circuit','jugcraft:jimta_re_battery','minecraft:glass_pane']+['jugcraft:steel_plate']*2+['jugcraft:copper_wire']*2,
  'mfsu_upgrade_kit':['jugcraft:advanced_circuit']*2+['jugcraft:jimta_advanced_alloy']*2+['jugcraft:lithium_cell']*4,
  'brewing_barrel':['minecraft:barrel','jugcraft:rubber','jugcraft:rubber'],
  'luminator':['minecraft:glass_pane']*3+['jugcraft:copper_wire']*2+['minecraft:glowstone_dust']*2+['jugcraft:tin_plate']}
 for name,ingredients in recipes.items():dump(RES/f'data/jugcraft/recipe/jimtafacts_utilities/{name}.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':distinct('jimtafacts_utilities/'+name,ingredients),'result':{'id':'jugcraft:jimta_'+name}})
 dump(RES/'data/jugcraft/recipe/jimtafacts_utilities/repair_rubber_boat.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':['jugcraft:jimta_broken_rubber_boat']+['jugcraft:rubber']*3,'result':{'id':'jugcraft:jimta_rubber_boat'}})
 lang['item.jugcraft.jimta_wire_cutters']='Industrial Wire Cutters'
 if shared:
  path=RES/'assets/jugcraft/lang/en_us.json';value=json.loads(path.read_text(encoding='utf-8'));value.update(lang);dump(path,value)
  path=RES/'data/minecraft/tags/block/mineable/axe.json';value=json.loads(path.read_text(encoding='utf-8'))
  if 'jugcraft:jimta_brewing_barrel' not in value['values']:value['values'].append('jugcraft:jimta_brewing_barrel')
  dump(path,value)
  path=RES/'data/minecraft/tags/block/mineable/pickaxe.json';value=json.loads(path.read_text(encoding='utf-8'))
  for block in ('jimta_luminator','jimta_lathe'):
   if 'jugcraft:'+block not in value['values']:value['values'].append('jugcraft:'+block)
  dump(path,value)
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/utility-runtime-copies.json',manifest)
 return items()
if __name__=='__main__':print('Generated workshop utility resources:',len(generate()))
