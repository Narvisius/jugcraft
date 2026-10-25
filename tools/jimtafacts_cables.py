"""Recovered source cable widths and untouched insulation/dye textures on native JE cables."""
from pathlib import Path
import json,hashlib,shutil
ROOT=Path(__file__).resolve().parents[1];RES=ROOT/'src/main/resources'
SOURCE=ROOT/'art/owner-library/originals/Oct23/Jimtafacts/industrialcraft reborn by jimbo/assets/ic2/textures/blocks/wiring/cable'
# Source cable type -> atlas and geometric diameter (TileEntityCable.getCableThickness).
CABLES={
 'copper':('blockCableO',4,False),'insulated_copper':('blockCable',6,True),
 'gold':('blockGoldCable',3,False),'insulated_gold':('blockGoldCableI',6,True),'double_insulated_gold':('blockGoldCableII',6,True),
 'iron':('blockIronCable',6,False),'insulated_iron':('blockIronCableI',10,True),'double_insulated_iron':('blockIronCableII',10,True),'quad_insulated_iron':('blockIronCableIIII',12,True),
 'glass':('blockGlassCable',4,True),'tin':('blockTinCable',4,False),'insulated_tin':('blockTinCableI',6,True),
 'detector':('blockDetectorCable',8,False),'splitter':('blockSplitterCable',8,False)}
COLORS=('white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black')
def blocks():return {'jimta_'+n+'_cable' for n in CABLES}
def items():return blocks()
def dump(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
def generate(shared=True):
 manifest=[];lang={}
 for name,(original,width,paint) in CABLES.items():
  id='jimta_'+name+'_cable';low=(16-width)/2;high=16-low;parts=[];choices=[]
  for color in range(17 if paint else 1):
   suffix='' if color==0 or COLORS[color-1]=='black' else '.'+{'light_blue':'lightBlue','light_gray':'lightGray'}.get(COLORS[color-1],COLORS[color-1])
   source=SOURCE/(original+suffix+'.png');target=RES/f'assets/jugcraft/textures/block/jimtafacts/cables/{original}{suffix}.png'
   target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source,target)
   manifest.append({'source':source.relative_to(ROOT).as_posix(),'runtime':target.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(source.read_bytes()).hexdigest()})
   tex='jugcraft:block/jimtafacts/cables/'+original+suffix;prefix=id+'_'+str(color)
   def model(suffix,a,b):
    dump(RES/f'assets/jugcraft/models/block/{prefix}{suffix}.json',{'parent':'minecraft:block/block','textures':{'particle':tex,'wire':tex},'elements':[
     {'from':a,'to':b,'faces':{face:{'texture':'#wire'} for face in ('north','south','east','west','up','down')}}]})
   model('',[low,low,low],[high,high,high]);model('_arm',[low,low,0],[high,high,low]);model('_item',[low,low,0],[high,high,16])
   def add(suffix,condition,rotation):
    when={**({'color':str(color)} if paint else {}),**condition};entry={'apply':{'model':'jugcraft:block/'+prefix+suffix,**rotation}}
    if when:entry['when']=when
    parts.append(entry)
   add('',{},{})
   for face,rotation in [('north',{}),('east',{'y':90}),('south',{'y':180}),('west',{'y':270}),('up',{'x':270}),('down',{'x':90})]:add('_arm',{face:'true'},rotation)
   choices.append({'when':str(color),'model':{'type':'minecraft:model','model':'jugcraft:block/'+prefix+'_item'}})
  dump(RES/f'assets/jugcraft/blockstates/{id}.json',{'multipart':parts})
  fallback=choices[0]['model'];dump(RES/f'assets/jugcraft/items/{id}.json',{'model':{'type':'minecraft:select','property':'minecraft:block_state','block_state_property':'color','cases':choices,'fallback':fallback} if paint else fallback})
  entry={'type':'minecraft:item','name':'jugcraft:'+id}
  if paint:entry['modifier']={'type':'minecraft:copy_state','block':'jugcraft:'+id,'properties':['color']}
  dump(RES/f'data/jugcraft/loot_table/blocks/{id}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[entry]}]})
  lang['block.jugcraft.'+id]='Industrial '+name.replace('_',' ').title()+' Cable'
  if name in ('copper','gold','iron','tin'):
   ingot='minecraft:'+name+'_ingot' if name in ('copper','gold','iron') else 'jugcraft:tin_ingot'
   inputs=[ingot,ingot,ingot,'minecraft:iron_nugget'];count=6
  elif name=='glass':inputs=['minecraft:glass','minecraft:glass','minecraft:diamond','minecraft:redstone'];count=4
  elif name in ('detector','splitter'):inputs=['jugcraft:jimta_glass_cable','jugcraft:advanced_circuit','minecraft:comparator' if name=='detector' else 'minecraft:repeater'];count=1
  else:
   previous={'insulated_copper':'copper','insulated_gold':'gold','double_insulated_gold':'insulated_gold','insulated_iron':'iron','double_insulated_iron':'insulated_iron','quad_insulated_iron':'double_insulated_iron','insulated_tin':'tin'}[name]
   inputs=['jugcraft:jimta_'+previous+'_cable']+['jugcraft:rubber']*(2 if name=='quad_insulated_iron' else 1);count=1
  dump(RES/f'data/jugcraft/recipe/jimtafacts_cables/{name}.json',{'type':'minecraft:crafting_shapeless','category':'redstone','ingredients':inputs,'result':{'id':'jugcraft:'+id,'count':count}})
 if shared:
  path=RES/'assets/jugcraft/lang/en_us.json';data=json.loads(path.read_text(encoding='utf-8'));data.update(lang);dump(path,data)
  path=RES/'data/minecraft/tags/block/mineable/pickaxe.json';data=json.loads(path.read_text(encoding='utf-8'))
  for id in sorted(blocks()):
   if 'jugcraft:'+id not in data['values']:data['values'].append('jugcraft:'+id)
  dump(path,data)
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/cable-runtime-copies.json',manifest)
 return items()
if __name__=='__main__':print('Generated industrial cable resources:',len(generate()))
