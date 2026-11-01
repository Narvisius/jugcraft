"""Losslessly repack supplied Continuity tiles for the already-approved Fusion renderer.
No source program is loaded; tile-index data is parsed from the static reference.
"""
from pathlib import Path
import re,json,zipfile,io
from PIL import Image
from newest_building import ROOT,SOURCE,RES,read,write,copy
SRC=SOURCE/'continuity/resourcepacks/default/assets/continuity/optifine/ctm'
PACK=RES/'resourcepacks/owner_connected_surfaces'

def uv(mask):
 l,b,r,t=bool(mask&1),bool(mask&4),bool(mask&16),bool(mask&64)
 bl,br,tr,tl=bool(mask&2),bool(mask&8),bool(mask&32),bool(mask&128)
 card=l+2*b+4*r+8*t
 simple={0:(0,0),1:(3,0),2:(0,1),4:(1,0),8:(0,3),5:(2,0),10:(0,2)}
 if card in simple:return simple[card]
 if card==9:return (3,3) if tl else (5,1)
 if card==12:return (1,3) if tr else (4,1)
 if card==6:return (1,1) if br else (4,0)
 if card==3:return (3,1) if bl else (5,0)
 if not l:return (1,2) if tr and br else (4,2) if tr else (6,2) if br else (6,0)
 if not t:return (2,1) if bl and br else (7,2) if bl else (5,2) if br else (7,0)
 if not r:return (3,2) if tl and bl else (7,3) if tl else (5,3) if bl else (7,1)
 if not b:return (2,3) if tl and tr else (4,3) if tl else (6,3) if tr else (6,1)
 return {15:(2,2),14:(7,5),13:(6,5),11:(7,4),7:(6,4),6:(0,4),9:(0,5),12:(3,4),5:(3,5),3:(2,5),10:(2,4),1:(5,5),2:(4,5),8:(4,4),4:(5,4),0:(1,4)}[tl+2*tr+4*bl+8*br]
def main(style="default"):
 global PACK
 PACK=RES/("resourcepacks/owner_connected_surfaces"+("_programmer" if style=="programmer_art" else ""))
 # Static tile-order data from the supplied CtmSpriteProvider, retained for reproducible imports.
 indices=[0, 3, 0, 3, 12, 5, 12, 15, 0, 3, 0, 3, 12, 5, 12, 15, 1, 2, 1, 2, 4, 7, 4, 29, 1, 2, 1, 2, 13, 31, 13, 14, 0, 3, 0, 3, 12, 5, 12, 15, 0, 3, 0, 3, 12, 5, 12, 15, 1, 2, 1, 2, 4, 7, 4, 29, 1, 2, 1, 2, 13, 31, 13, 14, 36, 17, 36, 17, 24, 19, 24, 43, 36, 17, 36, 17, 24, 19, 24, 43, 16, 18, 16, 18, 6, 46, 6, 21, 16, 18, 16, 18, 28, 9, 28, 22, 36, 17, 36, 17, 24, 19, 24, 43, 36, 17, 36, 17, 24, 19, 24, 43, 37, 40, 37, 40, 30, 8, 30, 34, 37, 40, 37, 40, 25, 23, 25, 45, 0, 3, 0, 3, 12, 5, 12, 15, 0, 3, 0, 3, 12, 5, 12, 15, 1, 2, 1, 2, 4, 7, 4, 29, 1, 2, 1, 2, 13, 31, 13, 14, 0, 3, 0, 3, 12, 5, 12, 15, 0, 3, 0, 3, 12, 5, 12, 15, 1, 2, 1, 2, 4, 7, 4, 29, 1, 2, 1, 2, 13, 31, 13, 14, 36, 39, 36, 39, 24, 41, 24, 27, 36, 39, 36, 39, 24, 41, 24, 27, 16, 42, 16, 42, 6, 20, 6, 10, 16, 42, 16, 42, 28, 35, 28, 44, 36, 39, 36, 39, 24, 41, 24, 27, 36, 39, 36, 39, 24, 41, 24, 27, 37, 38, 37, 38, 30, 11, 30, 32, 37, 38, 37, 38, 25, 33, 25, 26];tiles={}
 for mask,index in enumerate(indices):
  pos=uv(mask)
  if pos in tiles and tiles[pos]!=index:raise ValueError('Conflicting source CTM corner mapping')
  tiles[pos]=index
 jars=sorted((ROOT/'.gradle/loom-cache/minecraftMaven/net/minecraft').glob('minecraft-clientOnly-*/26.3/*.jar'))
 with zipfile.ZipFile(jars[0]) as native:
  def model(path):return json.loads(native.read('assets/minecraft/models/'+path+'.json'))
  def variants(value):
   if isinstance(value,dict):
    if 'model' in value:yield value['model']
    for child in value.values():yield from variants(child)
   elif isinstance(value,list):
    for child in value:yield from variants(child)
  entries=[];remaining=[]
  for p in sorted((SRC/style).rglob('*.properties')):
   d=dict(line.strip().split('=',1) for line in p.read_text().splitlines() if '=' in line and not line.startswith('#'))
   if d['method']=='top':
    name=d['matchTiles'];texture='block/owner_connected/'+name+'_top_join'
    with Image.open(io.BytesIO(native.read('assets/minecraft/textures/block/'+name+'.png'))) as plain,Image.open(p.parent/'0.png') as joined:
     # Vertical Fusion tile 0/1 is disconnected above; 2/3 is connected above.
     w,h=plain.size;sheet=Image.new('RGBA',(w,h*4))
     for row in range(4):sheet.paste((plain if row<2 else joined).convert('RGBA'),(0,row*h))
     dest=PACK/'assets/jugcraft/textures'/(texture+'.png');dest.parent.mkdir(parents=True,exist_ok=True);sheet.save(dest)
    write(dest.with_name(dest.name+'.mcmeta'),{'fusion':{'type':'connecting','layout':'vertical'}})
    # Only models using the exact supplied side sprite participate, preserving tops/bottoms.
    members=[];converted=[]
    for entry in native.namelist():
     if not entry.startswith('assets/minecraft/models/block/') or not entry.endswith('.json'):continue
     v=json.loads(native.read(entry));keys=[]
     for key,value in v.get('textures',{}).items():
      sprite=value.get('sprite','') if isinstance(value,dict) else value
      if sprite in ('minecraft:block/'+name,'block/'+name) and key!='particle':keys.append(key)
     if keys:converted.append((entry,v,keys));members.append(Path(entry).stem.removesuffix('_inner').removesuffix('_outer').removesuffix('_top'))
    connections={'type':'and','predicates':[{'type':'is_direction','direction':'top'},{'type':'or','predicates':[{'type':'match_block','block':'minecraft:'+block} for block in sorted(set(members)) if 'assets/minecraft/blockstates/'+block+'.json' in native.namelist()]}]}
    for entry,v,keys in converted:
     v.update({'loader':'fusion:model','type':'connecting','connections':connections})
     for key in keys:v['textures'][key]='jugcraft:'+texture
     write(PACK/entry,v)
    entries.append({'source':str(p.relative_to(SRC)),'layout':'vertical_top_only','matchingModels':len(converted),'plainTexture':'Minecraft 26.3 base art'})
    continue
   positions=tiles if d['method']=='ctm' else {(0,0):3,(1,0):0,(2,0):1,(3,0):2}
   cols,rows=(8,6) if d['method']=='ctm' else (4,1)
   with Image.open(p.parent/'0.png') as im:w,h=im.size
   sheet=Image.new('RGBA',(w*cols,h*rows))
   for (x,y),index in positions.items():
    with Image.open(p.parent/(str(index)+'.png')) as im:
     if im.size!=(w,h):raise ValueError('Mismatched source tiles '+str(p))
     sheet.paste(im.convert('RGBA'),(x*w,y*h))
   name=p.relative_to(SRC/style).with_suffix('').as_posix();texture='block/owner_connected/'+name
   dest=PACK/'assets/jugcraft/textures'/(texture+'.png');dest.parent.mkdir(parents=True,exist_ok=True);stream=io.BytesIO();sheet.save(stream,format='PNG');dest.write_bytes(stream.getvalue())
   meta={'fusion':{'type':'connecting','layout':'full' if cols==8 else 'horizontal'}}
   if 'glass' in name:meta['fusion']['render_type']='translucent'
   write(dest.with_name(dest.name+'.mcmeta'),meta)
   for block in d.get('matchBlocks',d.get('matchBlock','')).split():
    block=block.removeprefix('minecraft:');states=json.loads(native.read('assets/minecraft/blockstates/'+block+'.json'))
    for modelid in set(variants(states)):
     path=modelid.removeprefix('minecraft:');v=model(path)
     v.update({'loader':'fusion:model','type':'connecting','connections':{'type':'is_same_block'}})
     replace=set(d['matchTiles'].split())
     for key,value in v.get('textures',{}).items():
      sprite=value.get('sprite','') if isinstance(value,dict) else value
      if sprite.removeprefix('minecraft:').removeprefix('block/') in replace and key!='particle':
       if isinstance(value,dict):v['textures'][key]={**value,'sprite':'jugcraft:'+texture}
       else:v['textures'][key]='jugcraft:'+texture
     write(PACK/'assets/minecraft/models'/(path+'.json'),v)
   entries.append({'source':str(p.relative_to(SRC)),'layout':meta['fusion']['layout'],'blocks':d.get('matchBlocks',d.get('matchBlock',''))})
 write(PACK/'pack.mcmeta',{'pack':{'description':('Owner connected surfaces: '+style+' (Fusion required). Choose one owner surface pack; keep below custom art packs. Sandstone unjoined faces retain 26.3 base art.'),'min_format':71,'max_format':2048}})
 # The pane culling fix is original authored geometry, not a second renderer.
 for p in (SOURCE/'continuity/resourcepacks/glass_pane_culling_fix/assets').rglob('*.json'):copy(p,PACK/'assets'/p.relative_to(SOURCE/'continuity/resourcepacks/glass_pane_culling_fix/assets'))
 write(RES/('jugcraft/newest_continuity'+('_programmer' if style=='programmer_art' else '')+'.json'),{'connected':entries,'remaining':remaining,'selection':'Optional separate default/programmer-art packs. Player chooses one; custom art packs above override these models.'})
if __name__=='__main__':
 main()
 main('programmer_art')
