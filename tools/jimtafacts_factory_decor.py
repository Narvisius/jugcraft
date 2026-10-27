"""Recover supplied workshop panels and six-direction frame geometry without repainting."""
from pathlib import Path
import hashlib,json,shutil
ROOT=Path(__file__).resolve().parents[1];RES=ROOT/'src/main/resources'
SOURCE=ROOT/'art/owner-library/originals/Oct23/Jimtafacts/buildcraft reborn by jimbo/assets'
PANELS={'blueprint':'blueprint','template':'template','paper':'paper','leather':'leather','warning':'destroy','laser':'laser_back'}
def blocks():return {'jimta_factory_frame'}|{'jimta_factory_panel_'+name for name in PANELS}
def dump(path,value):
 path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')
def generate(shared=True):
 copies=[];seen=set();lang={}
 def texture(ref):
  ns,path=ref.split(':',1);source=SOURCE/ns/('textures/'+path+'.png');relative='block/jimtafacts/factory_decor/'+ns+'/'+path
  target=RES/('assets/jugcraft/textures/'+relative+'.png')
  if target not in seen:
   seen.add(target);target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source,target)
   copies.append({'source':source.relative_to(ROOT).as_posix(),'runtime':target.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'changes':'None; original pixels retained.'})
  return 'jugcraft:'+relative
 def model(ns,path,target):
  source=SOURCE/ns/('models/block/'+path+'.json');v=json.loads(source.read_text());v['parent']='minecraft:block/cube_all'
  v['textures']={k:ref if ref.startswith('#') else texture(ref) for k,ref in v.get('textures',{}).items()}
  # The source frame core lies inside the block; boundary culling would erase it beside solid blocks.
  for element in v.get('elements',[]):
   for face in element.get('faces',{}).values():face.pop('cullface',None)
  dest=RES/f'assets/jugcraft/models/block/{target}.json';dump(dest,v)
  copies.append({'source':source.relative_to(ROOT).as_posix(),'runtime':dest.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'changes':'Namespace/parent adaptation and internal-face culling removed; original coordinates, inward faces and UV definitions retained.'})
 for name,source in PANELS.items():
  ident='jimta_factory_panel_'+name;model('buildcraftcore','decorated/'+source,ident)
  dump(RES/f'assets/jugcraft/blockstates/{ident}.json',{'variants':{'':{'model':'jugcraft:block/'+ident}}})
  lang['block.jugcraft.'+ident]='Workshop '+name.replace('_',' ').title()+' Panel'
 ident='jimta_factory_frame';model('buildcraftbuilders','frame/base',ident);model('buildcraftbuilders','frame/connection',ident+'_arm')
 parts=[{'apply':{'model':'jugcraft:block/'+ident}}]
 for side,rot in {'north':{},'south':{'y':180},'east':{'y':90},'west':{'y':270},'up':{'x':270},'down':{'x':90}}.items():parts.append({'when':{side:'true'},'apply':{'model':'jugcraft:block/'+ident+'_arm',**rot}})
 dump(RES/f'assets/jugcraft/blockstates/{ident}.json',{'multipart':parts});lang['block.jugcraft.'+ident]='Workshop Structural Frame'
 for ident in sorted(blocks()):
  dump(RES/f'assets/jugcraft/items/{ident}.json',{'model':{'type':'minecraft:model','model':'jugcraft:block/'+ident}})
  dump(RES/f'data/jugcraft/loot_table/blocks/{ident}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:'+ident}],'condition':{'type':'minecraft:survives_explosion'}}]})
 def craft(name,ingredients,count):dump(RES/f'data/jugcraft/recipe/jimtafacts_factory/{name}.json',{'type':'minecraft:crafting_shapeless','category':'building','ingredients':ingredients,'result':{'id':'jugcraft:jimta_factory_'+name,'count':count}})
 craft('frame',['minecraft:iron_bars','jugcraft:iron_plate'],4)
 for name,material in {'blueprint':'blue_dye','template':'yellow_dye','paper':'paper','leather':'leather','warning':'red_dye','laser':'redstone'}.items():craft('panel_'+name,['jugcraft:iron_plate','minecraft:'+material],4)
 if shared:
  path=RES/'assets/jugcraft/lang/en_us.json';v=json.loads(path.read_text());v.update(lang);dump(path,v)
  path=RES/'data/minecraft/tags/block/mineable/pickaxe.json';v=json.loads(path.read_text());v['values']=sorted(set(v['values'])|{'jugcraft:'+ident for ident in blocks()});dump(path,v)
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/factory-decor-runtime-copies.json',{'copies':copies,'adaptation':'Six decorative panel types and six-direction connecting structural frames. No fake machine function or destructiveness for the source destroy texture. Frames connect only to other frames, with matching collision arms; quarry animation remains separately owned by its machine.'})
if __name__=='__main__':generate()
