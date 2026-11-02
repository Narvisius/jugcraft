import unittest,json,sys,zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'tools'))
import thanksgiving_data as t
class HarvestAssets(unittest.TestCase):
 def test_all_harvest_model_materials_exist(self):
  # Pinned 26.3 materials, verified against the client JAR locally; Foundation CI has no Gradle cache.
  vanilla_names={'black_wool','brown_wool','gold_block','green_concrete','orange_terracotta','sandstone_top','white_terracotta','white_wool'}
  client=Path.home()/'.gradle/caches/fabric-loom/26.3/minecraft-client.jar'
  vanilla_files=None
  if client.exists():
   with zipfile.ZipFile(client) as vanilla: vanilla_files=set(vanilla.namelist())
  for p in (t.RES/'assets/jugcraft/models/block/harvest').glob('*.json'):
   d=json.loads(p.read_text())
   for ref in d['textures'].values():
    if ref.startswith('#'):continue
    ns,name=ref.split(':');asset='assets/'+ns+'/textures/'+name+'.png'
    if ns=='minecraft':
     self.assertIn(name,{'block/'+n for n in vanilla_names},str(p)+': unreviewed vanilla material '+ref)
     if vanilla_files is not None:self.assertIn(asset,vanilla_files)
    else:self.assertTrue((t.RES/asset).exists(),str(p)+': '+ref)
 def test_complete_portion_and_rotation_models(self):
  d=json.loads((t.RES/'assets/jugcraft/blockstates/giant_turkey_feast.json').read_text())['multipart']
  for facing in ['north','east','south','west']:
   for part in range(18):
    for remaining in range(25):
     matches=[r for r in d if r['when']['facing']==facing and r['when']['part']==str(part) and str(remaining) in r['when']['remaining'].split('|')]
     self.assertEqual(len(matches),1)
     model=matches[0]['apply']['model'].split(':')[1];self.assertTrue((t.RES/'assets/jugcraft/models'/ (model+'.json')).exists())
 def test_costume_trunk_and_head_assets(self):
  tags=json.loads((t.RES/'data/jugcraft/tags/item/trick_or_treat_costumes.json').read_text())['values']
  for n in ['pilgrim_coat_costume','pilgrim_dress_costume']:
   self.assertIn('jugcraft:'+n,tags)
   self.assertTrue((t.RES/'data/jugcraft/recipe'/(n+'.json')).exists())
  self.assertEqual(json.loads((t.RES/'assets/jugcraft/equipment/harvest_costume.json').read_text()),{'layers':{}})
 def test_recipe_portion_conservation(self):
  d=json.loads((t.RES/'data/jugcraft/recipe/giant_turkey_feast.json').read_text())
  self.assertEqual(d['ingredients'],['jugcraft:roast_turkey']*4)
  loot=json.loads((t.RES/'data/jugcraft/loot_table/blocks/giant_turkey_feast.json').read_text())['pools'][0]['entries'][0]
  self.assertEqual(loot['modifier']['properties'],['remaining'])
  self.assertIn({'type':'minecraft:match_block','blocks':'jugcraft:giant_turkey_feast','state':{'part':'0'}},loot['condition']['terms'])
if __name__=='__main__':unittest.main()
