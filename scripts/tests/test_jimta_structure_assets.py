"""Layout migration contract, to run with the final combined validation."""
from pathlib import Path
import json
import sys
import unittest
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools'))
from nouveau_nbt import read
import jimtafacts_spellbook_structures as adapter

class JimtaStructures(unittest.TestCase):
    def test_all_original_layout_positions_and_palette_indices_survive(self):
        source=ROOT/'art/owner-library/originals/Oct23/Jimtafacts/data/irons_spellbooks/structure'
        target=ROOT/'src/main/resources/data/jugcraft/structure/jimtafacts/spellbooks'
        originals=list(source.rglob('*.nbt'));self.assertEqual(len(originals),236)
        for path in originals:
            before=read(path.read_bytes())[2];after=read((target/path.relative_to(source)).read_bytes())[2]
            self.assertEqual(before['size'],after['size'],str(path))
            self.assertEqual(len(before['blocks'][1][1]),len(after['blocks'][1][1]),str(path))
            for a,b in zip(before['blocks'][1][1],after['blocks'][1][1]):
                self.assertEqual(a['pos'],b['pos']);self.assertEqual(a['state'],b['state'])
            for a,b in zip(before.get('entities',(9,(10,[])))[1][1],after.get('entities',(9,(10,[])))[1][1]):
                self.assertEqual(a['pos'],b['pos']);self.assertEqual(a['blockPos'],b['blockPos'])

    def test_source_pools_keep_original_terrain_and_selection_weights(self):
        source=ROOT/'art/owner-library/originals/Oct23/Jimtafacts/data/irons_spellbooks/worldgen/template_pool'
        target=ROOT/'src/main/resources/data/jugcraft/worldgen/template_pool/jimtafacts/spellbooks'
        for path in source.rglob('*.json'):
            before=adapter.load(path);after=json.loads((target/path.relative_to(source)).read_text())
            self.assertEqual([e['weight'] for e in before['elements']],[e['weight'] for e in after['elements']])
            for a,b in zip(before['elements'],after['elements']):
                self.assertEqual(a['element'].get('terrain_adjustment'),b['element'].get('terrain_adjustment'))

    def test_old_scroll_components_and_inventory_equipment_are_migrated(self):
        stack={'id':(8,'irons_spellbooks:scroll'),'count':(3,1),'components':(10,{'irons_spellbooks:spell_container':(10,{'data':(9,(10,[{'id':(8,'irons_spellbooks:angel_wing'),'level':(3,4)}]))})})}
        converted=adapter.item(stack)
        self.assertEqual(converted['id'],(8,'jugcraft:jimta_spellbooks_scroll_angel_wings'))
        self.assertNotIn('components',converted)
        mob=adapter.entity({'id':(8,'irons_spellbooks:citadel_keeper'),'UUID':(11,[1,2,3,4]),'ArmorItems':(9,(10,[{},{},{},{}]))})
        self.assertNotIn('UUID',mob);self.assertNotIn('ArmorItems',mob)
        self.assertEqual(mob['equipment'][1]['mainhand'][1]['id'],(8,'jugcraft:jimta_spellbooks_keeper_flamberge'))

    def test_supplied_statues_are_restored_with_pose_data_and_original_orientation(self):
        report=json.loads((ROOT/'docs/audits/jimtafacts-spellbook-structures.json').read_text())
        self.assertFalse(report['omitted_statue_palettes'])
        self.assertTrue(report['restored_statue_palettes'])
        for row in report['restored_statue_palettes']:
            path=ROOT/'src/main/resources/data/jugcraft/structure/jimtafacts/spellbooks'/row['template']
            data=read(path.read_bytes())[2]
            source=read((ROOT/'art/owner-library/originals/Oct23/Jimtafacts/data/irons_spellbooks/structure'/row['template']).read_bytes())[2]
            before=source['palette'][1][1][row['palette_index']]
            after=data['palette'][1][1][row['palette_index']]
            self.assertEqual(after['id'],(8,'jugcraft:jimta_spellbooks_player_statue'))
            for property in ('rotation','y_offset'):
                self.assertEqual(before.get('properties',before.get('Properties'))[1][property],after['properties'][1][property])
            for block in data['blocks'][1][1]:
                if block['state'][1]!=row['palette_index'] or after['properties'][1]['y_offset'][1]!='0':continue
                nbt=block['nbt'][1]
                self.assertEqual(nbt['id'],(8,'jugcraft:jimta_spellbooks_player_statue'))
                self.assertIn(nbt['pose'][1],range(23))
