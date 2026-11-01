"""Adapt the supplied physical backpack geometry, skins and recipes; never execute source classes."""
from pathlib import Path
import json,copy
from newest_building import write,copy as copy_file,map_value
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'art/owner-library/originals/Edited-2026-10-29/BACKPACKS'
RES=ROOT/'src/main/resources'
ASSET=RES/'assets/jugcraft'
NS='sophisticatedbackpacks'
TIERS=['backpack','copper_backpack','iron_backpack','gold_backpack','diamond_backpack','netherite_backpack']
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def mapped(d,resource=False):return map_value(d,NS,'packworks',resource)
def main():
 source=SRC/'assets'/NS
 names=sorted(p.stem for p in (source/'models/item').glob('*.json'))
 for p in (source/'textures').rglob('*'):
  if p.is_file():copy_file(p,ASSET/'textures/newest/packworks'/p.relative_to(source/'textures'))
 for p in (source/'models').rglob('*.json'):
  data=mapped(read(p),True);data.pop('loader',None)
  write(ASSET/'models/newest/packworks'/p.relative_to(source/'models'),data)
 # The supplied NeoForge loader combines these original Blockbench parts.
 # Flatten the very same elements and texture variables into native block models.
 for tier in TIERS:
  merged={'textures':{},'elements':[],'display':read(source/'models/block/backpack.json')['display']}
  merged['display'].pop('sophisticatedbackpacks:worn',None)
  for part in ['backpack_base','backpack_left_pouch','backpack_right_pouch','backpack_front_pouch','backpack_straps']:
   data=read(source/'models/block'/(part+'.json'));merged['textures'].update(data.get('textures',{}));merged['elements']+=data.get('elements',[])
  merged['textures'].update(read(source/'models/block'/(tier+'.json')).get('textures',{}))
  write(ASSET/'models/newest/packworks/block'/(tier+'.json'),mapped(merged,True))
  write(ASSET/'blockstates'/('packworks_'+tier+'.json'),{'variants':{f'facing={d}':{'model':f'jugcraft:newest/packworks/block/{tier}','y':r} for d,r in [('north',0),('east',90),('south',180),('west',270)]}})
 for name in names:
  model=f'jugcraft:newest/packworks/{"block" if name in TIERS else "item"}/{name}'
  definition={'type':'minecraft:model','model':model}
  if name in TIERS:definition['tints']=[{'type':'minecraft:dye','default':0x806347},{'type':'minecraft:constant','value':0x49382D}]
  write(ASSET/'items'/('packworks_'+name+'.json'),{'model':definition})
 upgrades=read(SRC/f'data/{NS}/tags/item/upgrade.json')['values']
 write(RES/'jugcraft/newest_backpacks.json',{'items':names,'upgrades':[i.split(':')[1] for i in upgrades]})
 preserved=[];pending=[]
 for p in (SRC/f'data/{NS}/recipe').glob('*.json'):
  d=mapped(read(p));d.pop('neoforge:conditions',None);kind=d['type']
  if kind in ('jugcraft:packworks_backpack_upgrade','sophisticatedcore:upgrade_next_tier'):
   # One native custom recipe keeps the physical carrier's components intact.
   preserved.append(d);d['type']='jugcraft:packworks_tier_upgrade'
  elif kind=='jugcraft:packworks_basic_backpack':d['type']='minecraft:crafting_shaped'
  elif kind=='jugcraft:packworks_backpack_dye':
   # Native dye recipes preserve every component, including the physical inventory.
   for tier in TIERS:
    write(RES/'data/jugcraft/recipe/newest/packworks'/(tier+'_dye.json'),{'type':'minecraft:crafting_dye','group':'packworks_dye','dye':'#minecraft:dyes','target':'jugcraft:packworks_'+tier,'result':{'id':'jugcraft:packworks_'+tier}})
   continue
  elif kind=='jugcraft:packworks_smithing_backpack_upgrade':d['type']='minecraft:smithing_transform'
  elif not kind.startswith('minecraft:'):pending.append({'name':p.stem,'type':kind});continue
  write(RES/'data/jugcraft/recipe/newest/packworks'/p.name,d)
 write(RES/'jugcraft/backpack_tier_recipes.json',preserved)
 dyeable=RES/'data/minecraft/tags/item/dyeable.json';tag=read(dyeable) if dyeable.exists() else {'values':[]}
 for tier in TIERS:
  name='jugcraft:packworks_'+tier
  if name not in tag['values']:tag['values'].append(name)
 write(dyeable,tag)
 langfile=ASSET/'lang/en_us.json';lang=read(langfile)
 for key,value in read(source/'lang/en_us.json').items():
  if key.startswith(('item.sophisticatedbackpacks.','block.sophisticatedbackpacks.')):
   prefix,_,tail=key.split('.',2);lang[prefix+'.jugcraft.packworks_'+tail]=value
 lang["key.jugcraft.owner_backpack"]="Open Backpack"
 lang["trinkets.slot.chest.backpack"]="Backpack"
 write(RES/"data/trinkets/slots/chest/backpack.json",{"amount":1,"validator_predicates":["trinkets:default"]})
 write(RES/"data/trinkets/entities/jugcraft_packworks.json",{"entities":["minecraft:player"],"slots":["chest/backpack"]})
 write(RES/"data/trinkets/tags/item/chest/backpack.json",{"values":["jugcraft:packworks_"+tier for tier in TIERS]})
 write(langfile,lang)
 write(ROOT/'art/owner-library/catalog/edited-2026-10-29/backpacks-integration.json',{'source':'BACKPACKS','items':names,'originalGeometry':'Native flattened original base, straps and pockets; original UVs and textures retained','tierRecipes':len(preserved),'remainingRecipeAdapters':pending,'validation':'Uncompiled and untested; upgrade systems still being implemented','sourceExecuted':False})
 print('Generated backpack resources:',len(names),'items;',len(preserved),'component-preserving tier patterns;',len(pending),'remaining recipe adapters')
if __name__=='__main__':main()
