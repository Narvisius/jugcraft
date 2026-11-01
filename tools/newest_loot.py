"""Native Lootr models/textures and data, preserving source originals."""
import json, shutil, re
from pathlib import Path
from recover_wildlife_models import geometry
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'art/owner-library/originals/Edited-2026-10-29/LOOT/assets/lootr'
RES=ROOT/'src/main/resources'
NAMES=['chest','trapped_chest','barrel','shulker_box','copper_chest','exposed_copper_chest','weathered_copper_chest','oxidized_copper_chest','decorated_pot','suspicious_sand','suspicious_gravel','inventory']
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def write(p,d):
 p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,indent=2)+'\n',encoding='utf8')
def remap(d):
 if isinstance(d,str):return d.replace('lootr:', 'jugcraft:newest/personal_loot/')
 if isinstance(d,list):return [remap(v) for v in d]
 if isinstance(d,dict):return {k:remap(v) for k,v in d.items() if k!='render_type'}
 return d
def main():
 for f in (SRC/'textures').rglob('*'):
  if f.is_file():
   target=RES/'assets/jugcraft/textures/newest/personal_loot'/f.relative_to(SRC/'textures');target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(f,target)
 for f in (SRC/'models').rglob('*.json'):
  if f.stem=='trophy':continue # source geometry references two absent trophy textures
  write(RES/'assets/jugcraft/models/newest/personal_loot'/f.relative_to(SRC/'models'),remap(read(f)))
 write(RES/'assets/jugcraft/models/newest/personal_loot/empty.json',{'textures':{'particle':'jugcraft:newest/personal_loot/block/gold_planks'}})
 for name in NAMES:
  variants={'model=false':{'model':'jugcraft:newest/personal_loot/empty'}}
  for seen in [False,True]:
   for opened in [False,True]:
    for face,rotation in [('north',{'x':90,'y':180}),('south',{'x':90}),('west',{'x':90,'y':90}),('east',{'x':90,'y':270}),('up',{}),('down',{'x':180})]:
     if name=='barrel':model=('opened_barrel' if seen else 'barrel_unopened')+('_open' if opened else '')
     elif name.startswith('suspicious_'):model=name+'_open' if seen else 'minecraft:block/'+name+'_0';rotation={}
     else:model='empty';rotation={}
     model=model if ':' in model else 'jugcraft:newest/personal_loot/block/'+model if model!='empty' else 'jugcraft:newest/personal_loot/empty'
     variants[f'model=true,seen={str(seen).lower()},open={str(opened).lower()},facing={face}']={'model':model,**rotation}
  write(RES/'assets/jugcraft/blockstates'/('personal_loot_'+name+'.json'),{'variants':variants})
  if name in ('barrel','suspicious_sand','suspicious_gravel'):
   model='jugcraft:newest/personal_loot/block/barrel_unopened' if name=='barrel' else 'minecraft:block/'+name+'_0'
   item={'model':{'type':'minecraft:model','model':model}}
  else:item={'model':{'type':'minecraft:special','base':'minecraft:item/'+('chest' if name=='inventory' else name),'model':{'type':'jugcraft:personal_loot','design':name}}}
  write(RES/'assets/jugcraft/items'/('personal_loot_'+name+'.json'),item)
  # Structure/admin containers never become survival loot generators via a crafting recipe.
  write(RES/'data/jugcraft/loot_table/blocks'/('personal_loot_'+name+'.json'),{'type':'minecraft:block','pools':[]})
 langpath=RES/'assets/jugcraft/lang/en_us.json';lang=read(langpath)
 for name,model in [('item_frame','jugcraft:newest/personal_loot/block/item_frame'),('item_frame_open','jugcraft:newest/personal_loot/block/item_frame_open'),('minecart','minecraft:item/chest_minecart')]:
  write(RES/f'assets/jugcraft/items/personal_loot_{name}.json',{'model':{'type':'minecraft:model','model':model}})
 for name in ('item_frame','minecart'):
  lang['item.jugcraft.personal_loot_'+name]='Personal Loot '+name.replace('_',' ').title()
  lang['entity.jugcraft.personal_loot_'+name]='Personal Loot '+name.replace('_',' ').title()
 for name in NAMES:lang['block.jugcraft.personal_loot_'+name]='Personal Loot '+name.replace('_',' ').title()
 write(langpath,lang)
 source=ROOT/'build/owner-oct29-reference/noobanidus/mods/lootr/common/client/block/LootrDecoratedPotRenderer.java'
 text=source.read_text(encoding='utf8')
 text=re.sub(r'(?m)^(\s*)(\w+)\.addOrReplaceChild\("([^"\n]+)"',lambda m:m[1]+'PartDefinition recovered_'+re.sub(r'\W','_',m[3])+' = '+m[2]+'.addOrReplaceChild("'+m[3]+'"',text)
 write(RES/'assets/jugcraft/newest_loot_models/open_pot.json',geometry(text))
 write(ROOT/'art/owner-library/catalog/edited-2026-10-29/loot-integration.json',{'source':'LOOT','blocks':NAMES,'original_model_recovery':'Decorated pot opened geometry; native chest/shulker layers as used by supplied renderers','missing':['trophy_steve.png','trophy_chest.png'],'validation':'Uncompiled and untested; combined validation pending','sourceExecuted':False})
 print('Generated personal loot resources and recovered original opened-pot geometry.')
if __name__=='__main__':main()
