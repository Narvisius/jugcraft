"""Bounded native robot programs using the supplied original geometry and textures.

The legacy distribution has robot APIs/art but registers no executable robot items.
Gameplay is Jugcraft-native; source programs and binaries are never loaded.
"""
from pathlib import Path
import hashlib
import json
import shutil

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
SOURCE=ROOT/'art/owner-library/originals/Oct23/Jimtafacts/buildcraft reborn by jimbo/assets'
ROB=SOURCE/'buildcraftrobotics'
# source skin, card tint, programming ingredient, player-facing operation
PROGRAMS={
 'clean':('base','clean','minecraft:feather','Collect dropped items in the marked area. Reserved player drops stay protected.'),
 'picker':('picker','yellow','minecraft:hopper','Gather dropped items and deliver them to a linked output.'),
 'lumberjack':('lumberjack','green','minecraft:oak_log','Cut logs with the tool in slot 1; deliver the actual drops.'),
 'planter':('planter','green','minecraft:wheat_seeds','Plant supplied seeds, carrots and potatoes on empty farmland.'),
 'leafcutter':('leaveCutter','green','minecraft:shears','Trim leaves using the tool in slot 1 and collect their actual drops.'),
 'carrier':('carrier','yellow','minecraft:chest','Carry items from the linked input face to the linked output face.'),
 'fluid_carrier':('fluidCarrier','blue','minecraft:bucket','Transport fluid between linked tank faces. Internal capacity: 16 buckets.'),
 'bomber':('bomber','red','minecraft:gunpowder','Consume gunpowder from slot 1 to damage nearby hostiles. Never damages terrain or players.'),
 'knight':('knight','red','minecraft:iron_sword','Defend the marked area against hostiles. Respects claims and combat protections.'),
 'miner':('miner','blue','minecraft:iron_pickaxe','Mine tagged ores with a suitable, charged tool in slot 1.'),
 'farmer':('farmer','green','minecraft:iron_hoe','Till exposed dirt or grass using a hoe in slot 1.'),
 'harvester':('harvester','green','minecraft:wheat','Harvest mature crops and collect their drops. A planter supplies replacement seeds.'),
 'shovelman':('shovelman','blue','minecraft:iron_shovel','Excavate shovel-mineable blocks with the tool in slot 1.'),
 'butcher':('butcher','red','minecraft:beef','Cull adult farm animals in the marked area. Tamed animals and players are excluded.'),
 'builder':('builder','blue','minecraft:bricks','Fit a printed Jugcraft blueprint in slot 1. Builds north from the linked anchor using cargo materials.'),
 'crafter':('crafter','yellow','minecraft:crafting_table','Top nine cargo slots are a real crafting grid; finished items and remainders use the bottom rows.'),
 'delivery':('delivery','yellow','minecraft:paper','Deliver only the item type shown in slot 1; an empty filter accepts every type.'),
 'pump':('pump','blue','minecraft:glass_bottle','Pump source water or lava into the finite internal tank and a linked fluid output.'),
 'stripes':('stripes','yellow','minecraft:stone_bricks','Place supplied building blocks into empty supported cells of the marked area.'),
 'breaker':('breaker','blue','minecraft:iron_ingot','Break ordinary blocks in the marked area. Never breaks block entities or unbreakable blocks.'),
}

def items():return {'jimta_workshop_robot','jimta_robot_linker'}|{'jimta_robot_'+p for p in PROGRAMS}|{'jimta_robot_board_'+p for p in PROGRAMS}
def dump(path,value):
 path.parent.mkdir(parents=True,exist_ok=True)
 path.write_text(json.dumps(value,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')

def recipes():
 result={}
 def craft(name,ingredients,out,count=1):
  result[name]={'type':'minecraft:crafting_shapeless','category':'equipment','ingredients':ingredients,'result':{'id':'jugcraft:'+out,'count':count}}
 craft('robot_frame',['jugcraft:steel_plate','jugcraft:steel_plate','jugcraft:advanced_circuit','jugcraft:electric_motor','minecraft:hopper','minecraft:redstone'],'jimta_workshop_robot')
 craft('robot_linker',['minecraft:compass','jugcraft:copper_wire','minecraft:redstone'],'jimta_robot_linker')
 for p,(_,_,ingredient,_) in PROGRAMS.items():
  craft('program_'+p,['minecraft:paper','minecraft:redstone',ingredient],'jimta_robot_board_'+p)
  craft('robot_'+p,['jugcraft:jimta_workshop_robot','jugcraft:jimta_robot_board_'+p],'jimta_robot_'+p)
  craft('reset_robot_'+p,['jugcraft:jimta_robot_'+p],'jimta_workshop_robot')
 return result

def generate(shared=True):
 manifest=[];lang={};seen=set()
 def copy(source,dest):
  if dest in seen:return
  seen.add(dest);dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source,dest)
  manifest.append({'source':source.relative_to(ROOT).as_posix(),'runtime':dest.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'changes':'None; source bytes retained.'})
 copy(ROB/'models/robot.json',RES/'assets/jugcraft/models/item/jimtafacts/robot_geometry.json')
 def robot(id,skin):
  copy(ROB/f'textures/entities/robot_{skin}.png',RES/f'assets/jugcraft/textures/entity/jimtafacts/robots/{skin}.png')
  dump(RES/f'assets/jugcraft/models/item/{id}.json',{'parent':'jugcraft:item/jimtafacts/robot_geometry','textures':{'all':'jugcraft:entity/jimtafacts/robots/'+skin,'particle':'#all'},
       'display':{'gui':{'rotation':[30,225,0],'scale':[1.2,1.2,1.2]},'ground':{'translation':[0,3,0],'scale':[.8,.8,.8]},'fixed':{'rotation':[0,180,0]}}})
  dump(RES/f'assets/jugcraft/items/{id}.json',{'model':{'type':'minecraft:model','model':'jugcraft:item/'+id}})
 def flat(id,texture,source):
  copy(source,RES/f'assets/jugcraft/textures/item/jimtafacts/robots/{texture}.png')
  dump(RES/f'assets/jugcraft/models/item/{id}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'jugcraft:item/jimtafacts/robots/'+texture}})
  dump(RES/f'assets/jugcraft/items/{id}.json',{'model':{'type':'minecraft:model','model':'jugcraft:item/'+id}})
 robot('jimta_workshop_robot','base');lang['item.jugcraft.jimta_workshop_robot']='Workshop Robot'
 flat('jimta_robot_linker','linker',SOURCE/'buildcraftcore/textures/items/map/zone.png');lang['item.jugcraft.jimta_robot_linker']='Robot Linker'
 lang['entity.jugcraft.jimta_workshop_robot']='Workshop Robot'
 for p,(skin,tint,_,description) in PROGRAMS.items():
  label=p.replace('_',' ').title();robot('jimta_robot_'+p,skin)
  flat('jimta_robot_board_'+p,'board_'+tint,ROB/f'textures/items/board/{tint}.png')
  lang['item.jugcraft.jimta_robot_'+p]=label+' Robot';lang['item.jugcraft.jimta_robot_board_'+p]=label+' Program Card'
  lang['tooltip.jugcraft.robot_program.'+p]=description
 for id,recipe in recipes().items():dump(RES/f'data/jugcraft/recipe/jimtafacts_robots/{id}.json',recipe)
 dump(RES/'jugcraft/jimtafacts/robots.json',{p:{'source_skin':skin,'card':tint,'operation':description} for p,(skin,tint,_,description) in PROGRAMS.items()})
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/robot-runtime-copies.json',manifest)
 if shared:
  path=RES/'assets/jugcraft/lang/en_us.json';current=json.loads(path.read_text(encoding='utf-8'));current.update(lang);dump(path,current)
 return manifest

if __name__=='__main__':
 import sys
 generate(shared='--no-shared' not in sys.argv)
 print(f'Generated {len(PROGRAMS)} robot programs, {len(items())} items and {len(recipes())} recipes using original model and skins.')
