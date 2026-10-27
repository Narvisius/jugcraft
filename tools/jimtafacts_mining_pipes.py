"""Original drill-column sprites with source pipe/tip dimensions and finite retrieval drops."""
import hashlib,shutil,json
from jimtafacts_construction import ROOT,RES,SOURCE,dump

def generate(shared=True):
 manifest=[]
 for name,source in [('mining_pipe','blockMiningPipe'),('mining_tip','blockMiningTip')]:
  ident='jimta_'+name;src=SOURCE/f'textures/blocks/machine/{source}.png';target=RES/f'assets/jugcraft/textures/jimtafacts/construction/{name}.png';target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(src,target)
  tex='jugcraft:jimtafacts/construction/'+name
  model={'parent':'minecraft:block/cube_all','textures':{'all':tex}}
  if name=='mining_pipe':model={'parent':'minecraft:block/block','textures':{'all':tex,'particle':tex},'elements':[{'from':[6,0,6],'to':[10,16,10],'faces':{face:{'texture':'#all','uv':[0,0,16,16]} for face in ('down','up','north','south','west','east')}}]}
  dump(RES/f'assets/jugcraft/models/block/{ident}.json',model);dump(RES/f'assets/jugcraft/blockstates/{ident}.json',{'variants':{'':{'model':'jugcraft:block/'+ident}}});dump(RES/f'assets/jugcraft/items/{ident}.json',{'model':{'type':'minecraft:model','model':'jugcraft:block/'+ident}})
  if name=='mining_pipe':
   def element(low,high):return {'from':low,'to':high,'faces':{face:{'texture':'#all','uv':[0,0,16,16]} for face in ('down','up','north','south','west','east')}}
   parts=[{'apply':{'model':'jugcraft:block/'+ident+'_core'}}]
   dump(RES/f'assets/jugcraft/models/block/{ident}_core.json',{'parent':'minecraft:block/block','textures':{'all':tex,'particle':tex},'elements':[element([6,6,6],[10,10,10])]})
   for face,low,high in [('down',[6,0,6],[10,6,10]),('up',[6,10,6],[10,16,10]),('north',[6,6,0],[10,10,6]),('south',[6,6,10],[10,10,16]),('west',[0,6,6],[6,10,10]),('east',[10,6,6],[16,10,10])]:
    dump(RES/f'assets/jugcraft/models/block/{ident}_{face}.json',{'parent':'minecraft:block/block','textures':{'all':tex,'particle':tex},'elements':[element(low,high)]})
    parts.append({'when':{face:'true'},'apply':{'model':f'jugcraft:block/{ident}_{face}'}})
   dump(RES/f'assets/jugcraft/blockstates/{ident}.json',{'multipart':parts})
  dump(RES/f'data/jugcraft/loot_table/blocks/{ident}.json',{'type':'minecraft:block','pools':[{'rolls':1,'condition':{'type':'minecraft:survives_explosion'},'entries':[{'type':'minecraft:item','name':'jugcraft:jimta_mining_pipe'}]}]})
  manifest.append({'source':src.relative_to(ROOT).as_posix(),'runtime':target.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(src.read_bytes()).hexdigest()})
 dump(RES/'data/jugcraft/recipe/jimtafacts_construction/mining_pipe.json',{'type':'minecraft:crafting_shaped','category':'misc','pattern':['I I','I I','I I'],'key':{'I':'minecraft:iron_ingot'},'result':{'id':'jugcraft:jimta_mining_pipe','count':8}})
 if shared:
  p=RES/'assets/jugcraft/lang/en_us.json';data=json.loads(p.read_text());data.update({'block.jugcraft.jimta_mining_pipe':'Mining Pipe','block.jugcraft.jimta_mining_tip':'Mining Drill Tip'});dump(p,data)
  p=RES/'data/minecraft/tags/block/mineable/pickaxe.json';data=json.loads(p.read_text());data['values']=list(dict.fromkeys(data['values']+['jugcraft:jimta_mining_pipe','jugcraft:jimta_mining_tip']));dump(p,data)
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/mining-pipe-runtime-copies.json',manifest)
 return {'jimta_mining_pipe','jimta_mining_tip'}
if __name__=='__main__':generate()
