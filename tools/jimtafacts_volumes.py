"""Owner supplied volume-box geometry and marker texture; native persistent survey tool."""
import json,hashlib,shutil
from jimtafacts_factory import ROOT,RES,SOURCE,dump

def generate(shared=True):
 src=SOURCE/'buildcraftcore/models/item/volume_box.json';model=json.loads(src.read_text())
 texture=SOURCE/'buildcraftcore/textures/lasers/marker_volume_connected.png'
 target=RES/'assets/jugcraft/textures/item/jimtafacts/volume_box.png';target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(texture,target)
 model['parent']='minecraft:block/block';model['textures']={'main':'jugcraft:item/jimtafacts/volume_box','particle':'jugcraft:item/jimtafacts/volume_box'}
 modelpath=RES/'assets/jugcraft/models/item/jimta_volume_box.json';dump(modelpath,model)
 dump(RES/'assets/jugcraft/items/jimta_volume_box.json',{'model':{'type':'minecraft:model','model':'jugcraft:item/jimta_volume_box'}})
 dump(RES/'data/jugcraft/recipe/jimtafacts_factory/volume_box.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':['jugcraft:jimta_volume_marker','minecraft:glass','minecraft:redstone'],'result':{'id':'jugcraft:jimta_volume_box'}})
 planner_src=SOURCE/'buildcraftbuilders/models/item/filler_planner.json';planner=json.loads(planner_src.read_text());planner['parent']='minecraft:block/cube_all';planner['textures']={'all':'jugcraft:item/jimtafacts/filler_planner'}
 planner_texture=SOURCE/'buildcraftbuilders/textures/addons/filler_planner.png';planner_target=RES/'assets/jugcraft/textures/item/jimtafacts/filler_planner.png';shutil.copyfile(planner_texture,planner_target)
 planner_model=RES/'assets/jugcraft/models/item/jimta_filler_planner.json';dump(planner_model,planner)
 dump(RES/'assets/jugcraft/items/jimta_filler_planner.json',{'model':{'type':'minecraft:model','model':'jugcraft:item/jimta_filler_planner'}})
 dump(RES/'data/jugcraft/recipe/jimtafacts_factory/filler_planner.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':['minecraft:paper','jugcraft:basic_circuit','minecraft:redstone','minecraft:bricks'],'result':{'id':'jugcraft:jimta_filler_planner'}})
 connector_src=SOURCE/'buildcraftcore/textures/items/marker_connector.png';connector_target=RES/'assets/jugcraft/textures/item/jimtafacts/marker_connector.png';shutil.copyfile(connector_src,connector_target)
 dump(RES/'assets/jugcraft/models/item/jimta_marker_connector.json',{'parent':'minecraft:item/handheld','textures':{'layer0':'jugcraft:item/jimtafacts/marker_connector'}})
 dump(RES/'assets/jugcraft/items/jimta_marker_connector.json',{'model':{'type':'minecraft:model','model':'jugcraft:item/jimta_marker_connector'}})
 dump(RES/'data/jugcraft/recipe/jimtafacts_factory/marker_connector.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':['minecraft:redstone_torch','jugcraft:jimta_wood_gear','minecraft:iron_ingot'],'result':{'id':'jugcraft:jimta_marker_connector'}})
 if shared:
  path=RES/'assets/jugcraft/lang/en_us.json';data=json.loads(path.read_text());data['item.jugcraft.jimta_volume_box']='Survey Volume Box';data['item.jugcraft.jimta_filler_planner']='Filler Planner';data['item.jugcraft.jimta_marker_connector']='Marker Connector';dump(path,data)
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/volume-runtime-copies.json',[{'source':source.relative_to(ROOT).as_posix(),'runtime':runtime.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(source.read_bytes()).hexdigest()} for source,runtime in [(src,modelpath),(texture,target),(planner_src,planner_model),(planner_texture,planner_target),(connector_src,connector_target)]])
 return {'jimta_volume_box','jimta_filler_planner','jimta_marker_connector'}
if __name__=='__main__':generate()
