"""Adapt all supplied Flowery Moobloom variants onto native cow behavior."""
from pathlib import Path
from newest_building import write,copy,map_value,read
ROOT=Path(__file__).resolve().parents[1];SRC=ROOT/'art/owner-library/originals/Edited-2026-10-29/moobloom';RES=ROOT/'src/main/resources'
def main():
 variants=[]
 for p in sorted((SRC/'data/friendsandfoes/moobloom_variants').glob('*.json')):
  d=read(p);d['biomes']=map_value(d['biomes'],'friendsandfoes','flowery');variants.append(d)
 for p in (SRC/'assets/friendsandfoes/textures/entity/moobloom').glob('*'):
  if p.is_file():copy(p,RES/'assets/jugcraft/textures/newest/flowery'/p.name)
 for p in (SRC/'data/friendsandfoes/tags/worldgen/biome/has_moobloom').glob('*.json'):
  write(RES/'data/jugcraft/tags/worldgen/biome/flowery_has_moobloom'/p.name,map_value(read(p),'friendsandfoes','flowery'))
 write(RES/'jugcraft/newest_mooblooms.json',{'variants':variants})
 write(RES/'data/jugcraft/loot_table/entities/flowery_moobloom.json',{'type':'minecraft:entity','pools':[{'rolls':1,'entries':[{'type':'minecraft:loot_table','value':'minecraft:entities/cow'}]}]})
 write(RES/'assets/jugcraft/items/flowery_moobloom_spawn_egg.json',{'model':{'type':'minecraft:model','model':'minecraft:item/cow_spawn_egg','tints':[{'type':'minecraft:constant','value':0xE1BE45},{'type':'minecraft:constant','value':0xFFF4C7}]}})
 path=RES/'assets/jugcraft/lang/en_us.json';lang=read(path);lang['entity.jugcraft.flowery_moobloom']='Moobloom';lang['item.jugcraft.flowery_moobloom_spawn_egg']='Moobloom Spawn Egg';write(path,lang)
 write(ROOT/'art/owner-library/catalog/edited-2026-10-29/mooblooms-integration.json',{'source':'moobloom','variants':variants,'sourceExecuted':False,'geometry':'Supplied skins use vanilla cow UVs. Add-on has no base entity model; native cow adult/baby models and flower block meshes are used.','validation':'Uncompiled; native behavior and rendering await combined validation.'})
 print('Generated',len(variants),'Moobloom variants with original adult and baby skins')
if __name__=='__main__':main()
