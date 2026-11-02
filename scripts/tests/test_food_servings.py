import unittest,sys,json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools'))
import food_servings as f
class FoodServingAssets(unittest.TestCase):
 def test_catalog_and_models(self):
  rows=json.loads((ROOT/'tools/food_servings_catalog.json').read_text())
  manifest=json.loads((f.RES/'jugcraft/food_servings.json').read_text())
  self.assertEqual(len(rows),401);self.assertEqual(len({r['id'] for r in rows}),401)
  self.assertEqual({r['id'] for r in rows},{r['id'] for r in manifest})
  atlas=json.loads((f.RES/'assets/minecraft/atlases/blocks.json').read_text())['sources']
  for row in rows:
   with self.subTest(item=row['id']):
    ns,n=row['id'].split(':');path='food_serving/'+ns+'/'+n
    ref,source,tex,image=f.source(row['id'])
    model=json.loads((f.RES/'assets/jugcraft/models/block'/f'{path}.json').read_text())
    if not row.get('native_geometry'):
     self.assertEqual(model['elements'],f.geometry(n,row['family'],image))
    else:
     self.assertEqual(len(model['elements']),len(source['elements']))
     self.assertEqual(model['textures'],source['textures'])
    for e in model['elements']:
     for i in range(3):self.assertLess(e['from'][i],e['to'][i]);self.assertGreaterEqual(e['from'][i],0);self.assertLessEqual(e['to'][i],16)
     if row.get('native_geometry'):continue
     for face in e['faces'].values():
      u,v,U,V=face['uv'];self.assertEqual(image.getpixel((int((u+U)/32*image.width),int((v+V)/32*image.height)))[3],255)
    if not row.get('native_geometry'):
     sprite=model['textures']['food']
     self.assertTrue(any(a.get('sprite')==sprite and a.get('resource')==tex for a in atlas),'Missing block-atlas alias for '+tex)
    self.assertEqual(json.loads((f.RES/'data/jugcraft/loot_table/blocks'/f'{path}.json').read_text())['pools'],[])
 def test_reviewed_food_families(self):
  rows={r['id'].split(':')[1]:r['family'] for r in json.loads((ROOT/'tools/food_servings_catalog.json').read_text())}
  for name,family in [('chocolate_pie_slice','slice'),('jimta_eternal_steak','meat'),('toasted_marshmallow','candy'),('aquatic_sushi','sushi'),('pancakes','pancake')]:self.assertEqual(rows[name],family)
if __name__=='__main__':unittest.main()
