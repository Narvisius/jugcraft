import json
from pathlib import Path
from decor_data import box
from turkey_data import roast
from decor17_data import grown,cell_part,frame_cell,FACINGS
ROOT=Path(__file__).resolve().parents[1]; RES=ROOT/'src/main/resources'
FOODS=['mashed_potatoes_and_gravy','harvest_pea_casserole','candied_sweet_potatoes','dinner_roll']
def generate(writer=None):
 def w(p,d):
  p=RES/p
  if writer: writer(p,d)
  else:p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,indent=2)+'\n',encoding='utf-8')
 def model(n,m):
  w(Path('assets/jugcraft/models/block/harvest')/(n+'.json'),m)
  w(Path('assets/jugcraft/items')/(n+'.json'),{'model':{'type':'minecraft:model','model':'jugcraft:block/harvest/'+n}})
 display={'gui':{'rotation':[30,225,0],'translation':[0,0,0],'scale':[.9,.9,.9]},'ground':{'translation':[0,3,0],'scale':[.5,.5,.5]},'fixed':{'rotation':[90,0,0],'scale':[.7,.7,.7]}}
 for n in FOODS:
  textures={'vessel':'minecraft:block/white_terracotta','cream':'minecraft:block/sandstone_top','gravy':'jugcraft:block/roast_turkey_skin','green':'minecraft:block/green_concrete','orange':'minecraft:block/orange_terracotta','bread':'jugcraft:block/roast_turkey_skin','particle':'minecraft:block/sandstone_top'}
  if n=='dinner_roll':
   e=[box((3,0,4),(13,2,12),'#bread'),box((4,2,4.5),(12,4,11.5),'#bread'),box((5,4,5),(11,4.8,11),'#bread')]
   for x in (6,9):e.append(box((x,4.8,6),(x+.5,4.95,10),'#cream'))
  else:
   e=[box((4,0,4),(12,.8,12),'#vessel')]
   for a,b in [((3,.8,3),(13,4,4)),((3,.8,12),(13,4,13)),((3,.8,4),(4,4,12)),((12,.8,4),(13,4,12))]:e.append(box(a,b,'#vessel'))
   mat='#cream' if n==FOODS[0] else '#green' if n==FOODS[1] else '#orange'
   e.append(box((4,.8,4),(12,3.7,12),mat))
   for i,(x,z) in enumerate(((4.5,5),(8,4.5),(6,8),(9,9))):
    e.append(box((x,3.7,z),(x+2,4.4,z+2),mat))
    e.append(box((x+.3,4.4,z+.3),(x+1.5,4.55,z+1.6),'#gravy' if n==FOODS[0] else '#cream'))
  model(n,{'textures':textures,'elements':e,'display':display})
 inputs=[['minecraft:bowl','minecraft:baked_potato','minecraft:baked_potato','jugcraft:onion','minecraft:brown_mushroom'],['minecraft:bowl','jugcraft:peas','jugcraft:peas','minecraft:baked_potato','jugcraft:onion','minecraft:brown_mushroom'],['minecraft:bowl','jugcraft:baked_sweet_potato','jugcraft:baked_sweet_potato','minecraft:sugar']]
 for n,ingredients in zip(FOODS,inputs):
  counts={v:ingredients.count(v) for v in dict.fromkeys(ingredients)}
  w(Path('data/jugcraft/recipe')/('harvest_'+n+'.json'),{'type':'jugcraft:pot_cooking','ingredients':[{'ingredient':v,'count':c} for v,c in counts.items()],'result':{'id':'jugcraft:'+n},'time':200})
 w(Path('data/jugcraft/recipe/harvest_dinner_roll.json'),{'type':'minecraft:crafting_shapeless','ingredients':['minecraft:bread','minecraft:sugar'],'result':{'id':'jugcraft:dinner_roll','count':2}})
 parts=[]
 for stage,counts in [('',range(17,25)),('_carved',range(9,17)),('_breast',range(1,9)),('_carcass',[0])]:
  m=roast(stage);elements=grown(m['elements'],3,(3,2,3))
  for part in range(18):
   name='giant_turkey_feast'+stage+'_'+str(part)
   w(Path('assets/jugcraft/models/block/harvest')/(name+'.json'),{'textures':m['textures'],'elements':cell_part(elements,frame_cell((3,2,3),part),(3,2,3))})
   for f,y in FACINGS.items():parts.append({'when':{'part':str(part),'facing':f,'remaining':'|'.join(map(str,counts))},'apply':{'model':'jugcraft:block/harvest/'+name,'y':y}})
 w(Path('assets/jugcraft/blockstates/giant_turkey_feast.json'),{'multipart':parts})
 model('giant_turkey_feast',{**roast(''),'display':display})
 w(Path('data/jugcraft/loot_table/blocks/giant_turkey_feast.json'),{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:giant_turkey_feast','condition':{'type':'minecraft:all_of','terms':[{'type':'minecraft:survives_explosion'},{'type':'minecraft:match_block','blocks':'jugcraft:giant_turkey_feast','state':{'part':'0'}}]},'modifier':{'type':'minecraft:copy_state','block':'jugcraft:giant_turkey_feast','properties':['remaining']}}]}]})
 w(Path('data/jugcraft/recipe/giant_turkey_feast.json'),{'type':'minecraft:crafting_shapeless','ingredients':['jugcraft:roast_turkey']*4,'result':{'id':'jugcraft:giant_turkey_feast'}})
 w(Path('assets/jugcraft/equipment/harvest_costume.json'),{'layers':{}})
 for n,dress in [('pilgrim_coat_costume',False),('pilgrim_dress_costume',True)]:
  e=[box((2,1,2),(14,2,14),'#cloth'),box((4,2,4),(12,9,12),'#cloth'),box((3.8,3,3.8),(12.2,4,12.2),'#band'),box((7,3,3.6),(9,4.5,3.8),'#buckle')]
  model(n,{'textures':{'cloth':'minecraft:block/white_wool' if dress else 'minecraft:block/black_wool','band':'minecraft:block/brown_wool','buckle':'minecraft:block/gold_block','particle':'minecraft:block/white_wool' if dress else 'minecraft:block/black_wool'},'elements':e,'display':display})
  w(Path('data/jugcraft/recipe')/(n+'.json'),{'type':'minecraft:crafting_shaped','pattern':[' W ','WLW',' S '],'key':{'W':'minecraft:white_wool' if dress else 'minecraft:black_wool','L':'minecraft:leather','S':'minecraft:string'},'result':{'id':'jugcraft:'+n}})
 tagpath=Path('data/jugcraft/tags/item/trick_or_treat_costumes.json')
 tag=json.loads((RES/tagpath).read_text(encoding='utf-8'))
 for n in ['pilgrim_coat_costume','pilgrim_dress_costume']:
  if 'jugcraft:'+n not in tag['values']:tag['values'].append('jugcraft:'+n)
 w(tagpath,tag)
 langpath=RES/'assets/jugcraft/lang/en_us.json';lang=json.loads(langpath.read_text(encoding='utf-8'))
 for n in FOODS+['pilgrim_coat_costume','pilgrim_dress_costume']:lang['item.jugcraft.'+n]=n.replace('_',' ').title()
 lang['block.jugcraft.giant_turkey_feast']='Giant Turkey Feast'
 lang['tooltip.jugcraft.giant_turkey_feast']='%s / 24 portions remaining'
 lang['tooltip.jugcraft.giant_turkey_feast.controls']='3 x 3 floor, 2 high. Eat empty-handed; carve with a knife; sneak empty-handed to pack.'
 w(Path('assets/jugcraft/lang/en_us.json'),lang)
if __name__=='__main__':generate()
