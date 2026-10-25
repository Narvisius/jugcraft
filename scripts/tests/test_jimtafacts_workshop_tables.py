"""Deferred original-asset and table geometry checks; no gameplay/source executable loading."""
from pathlib import Path
import hashlib,json,unittest

ROOT=Path(__file__).resolve().parents[2]

class WorkshopArtTests(unittest.TestCase):
    def test_supplied_sprites_and_guis_remain_byte_identical(self):
        for manifest in ('table-runtime-copies.json','utility-runtime-copies.json','cable-runtime-copies.json','terraforming-runtime-copies.json','service-runtime-copies.json','personal-runtime-copies.json','engine-runtime-copies.json','factory-runtime-copies.json'):
            entries=json.loads((ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts'/manifest).read_text(encoding='utf-8'))
            if isinstance(entries,dict):entries=entries['copies']
            for row in entries:
                if 'sha256' not in row:continue
                with self.subTest(path=row['runtime']):
                    self.assertEqual((ROOT/row['source']).read_bytes(),(ROOT/row['runtime']).read_bytes())
                    self.assertEqual(hashlib.sha256((ROOT/row['runtime']).read_bytes()).hexdigest(),row['sha256'])
    def test_table_models_keep_every_original_element(self):
        source=ROOT/'art/owner-library/originals/Oct23/Jimtafacts/buildcraft reborn by jimbo/assets/buildcraftsilicon/models/block/table'
        for kind in ('charging','programming','advanced_crafting'):
            old=json.loads((source/(kind+'.json')).read_text(encoding='utf-8'))
            new=json.loads((ROOT/f'src/main/resources/assets/jugcraft/models/block/jimta_{kind}_table.json').read_text(encoding='utf-8'))
            if kind!='programming':self.assertEqual(old['elements'],new['elements'])
            else:
                self.assertEqual(len(old['elements']),len(new['elements']))
                for a,b in zip(old['elements'],new['elements']):
                    self.assertEqual(a.get('rotation'),b.get('rotation'))
                    for key in ('from','to'):
                        self.assertTrue(all(abs(x-y)<=0.101 for x,y in zip(a[key],b[key])))
    def test_portable_barrel_does_not_also_drop_empty_barrel(self):
        loot=json.loads((ROOT/'src/main/resources/data/jugcraft/loot_table/blocks/jimta_brewing_barrel.json').read_text(encoding='utf-8'))
        self.assertEqual(loot['pools'],[])
