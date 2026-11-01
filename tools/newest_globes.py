"""Supplied globe stands, artwork and original wandering-trader acquisition."""
from newest_building import ROOT,SOURCE,RES,read,write,map_value,copy
from newest_shapes import variants
S=SOURCE/'SUPPS';A=S/'assets/supplementaries';O=RES/'assets/jugcraft'
def main():
 data=read(RES/'jugcraft/newest_supps.json');lang=read(O/'lang/en_us.json')
 for name in ['globe','globe_sepia']:
  state=read(A/'blockstates'/(name+'.json'));props={'facing':['north','south','east','west'],'rotation':['0','1','2','3'],'powered':['false','true'],'waterlogged':['false','true']}
  data['blocks'][name]={'source':name,'collection':'newest_supps','kind':'supps','function':'globe','capacity':0,'wood':True,'properties':props,'shapes':variants(A,'supplementaries',state,{'facing':props['facing']})}
  write(O/'blockstates'/('supps_'+name+'.json'),map_value(state,'supplementaries','supps',True))
  write(O/'items'/('supps_'+name+'.json'),{'model':{'type':'minecraft:special','base':'jugcraft:newest/supps/item/'+name,'model':{'type':'jugcraft:supps_contents','name':name}}})
  write(RES/'data/jugcraft/loot_table/blocks'/('supps_'+name+'.json'),{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:supps_'+name}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
  lang['block.jugcraft.supps_'+name]='Sepia Globe' if name.endswith('sepia') else 'Globe'
 write(RES/'jugcraft/newest_globes.json',{'textures':sorted(p.stem.removeprefix('globe_') for p in (A/'textures/entity/globes').glob('globe_*.png'))})
 sounds=read(O/'sounds.json')
 copy(A/"sounds/block/globe_spin.ogg",O/"sounds/newest/supps/block/globe_spin.ogg")
 sounds['supps_globe_spin']={'sounds':['jugcraft:newest/supps/block/globe_spin']}
 write(O/'sounds.json',sounds);write(O/'lang/en_us.json',lang);write(RES/'jugcraft/newest_supps.json',data)
 catalog=ROOT/'art/owner-library/catalog/edited-2026-10-29/supplementaries-local.json'
 if catalog.exists():
  value=read(catalog);value['implementedRegistrationScope']=sorted(data['blocks']);value['remainingBlockFamilies']=[n for n in value['remainingBlockFamilies'] if n not in data['blocks']];write(catalog,value)
if __name__=='__main__':main()
