"""Original BuildCraft laser geometry and eight-frame beam, native JE transfer; no source program execution."""
from pathlib import Path
import json,shutil,hashlib
ROOT=Path(__file__).resolve().parents[1];RES=ROOT/'src/main/resources';SOURCE=ROOT/'art/owner-library/originals/Oct23/Jimtafacts/buildcraft reborn by jimbo/assets'
def items():return {'jimta_workshop_laser'}
def blocks():return items()
def dump(p,d):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,indent=2)+'\n',encoding='utf-8')
def generate(shared=True):
 manifest=[]
 def copy(src,dst):
  dst.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(src,dst);manifest.append({'source':src.relative_to(ROOT).as_posix(),'runtime':dst.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(src.read_bytes()).hexdigest(),'changes':'None'})
 model=json.loads((SOURCE/'buildcraftsilicon/models/block/laser.json').read_text(encoding='utf-8'));model['parent']='minecraft:block/block'
 for name,ref in model['textures'].items():
  ns,path=ref.split(':');copy(SOURCE/ns/f'textures/{path}.png',RES/f'assets/jugcraft/textures/block/jimtafacts/laser/{path}.png');model['textures'][name]='jugcraft:block/jimtafacts/laser/'+path
 if 'thirdperson' in model.get('display',{}):model['display']['thirdperson_righthand']=model['display'].pop('thirdperson')
 dump(RES/'assets/jugcraft/models/block/jimta_workshop_laser.json',model)
 dump(RES/'assets/jugcraft/items/jimta_workshop_laser.json',{'model':{'type':'minecraft:model','model':'jugcraft:block/jimta_workshop_laser'}})
 dump(RES/'assets/jugcraft/blockstates/jimta_workshop_laser.json',{'variants':{'facing='+d:{'model':'jugcraft:block/jimta_workshop_laser',**rot} for d,rot in [('up',{}),('down',{'x':180}),('north',{'x':90}),('east',{'x':90,'y':90}),('south',{'x':90,'y':180}),('west',{'x':90,'y':270})]}})
 for suffix in ('.png','.png.mcmeta'):copy(SOURCE/f'buildcraftcore/textures/lasers/power_low{suffix}',RES/f'assets/jugcraft/textures/block/jimtafacts/laser/beam{suffix}')
 dump(RES/'data/jugcraft/loot_table/blocks/jimta_workshop_laser.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:jimta_workshop_laser'}],'condition':{'type':'minecraft:survives_explosion'}}]})
 dump(RES/'data/jugcraft/recipe/jimtafacts_laser/emitter.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':['jugcraft:iron_plate']*3+['minecraft:diamond','jugcraft:jimta_chipset_red','jugcraft:copper_wire'],'result':{'id':'jugcraft:jimta_workshop_laser'}})
 if shared:
  p=RES/'assets/jugcraft/lang/en_us.json';d=json.loads(p.read_text(encoding='utf-8'));d['block.jugcraft.jimta_workshop_laser']='Workshop Laser';dump(p,d)
  for name in ('mineable/pickaxe','needs_stone_tool'):
   p=RES/f'data/minecraft/tags/block/{name}.json';d=json.loads(p.read_text(encoding='utf-8'));d['values']=list(dict.fromkeys(d['values']+['jugcraft:jimta_workshop_laser']));dump(p,d)
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/laser-runtime-copies.json',manifest)
 return items()
if __name__=='__main__':print(f'Generated {len(generate())} original laser emitter.')
