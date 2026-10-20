"""Regression cases found by the final owner-collection registry reload."""
import sys
import unittest
from pathlib import Path

sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'tools'))
from menagerie_data import loot
from owner_worldgen_completion import modern


class OwnerDataMigrationTests(unittest.TestCase):
    def test_named_predicate_does_not_replace_item_entry(self):
        entry={'type':'minecraft:item','name':'jugcraft:landscape_algal_end_stone',
               'condition':'minecraft:tool/can_silk_touch'}
        self.assertEqual(loot(entry),entry)
        self.assertEqual(loot(loot(entry)),entry)

    def test_legacy_condition_object_still_migrates(self):
        self.assertEqual(loot({'condition':'minecraft:match_tool','predicate':{'items':'minecraft:shears'}}),
                         {'type':'minecraft:match_tool','predicate':{'items':'minecraft:shears'}})

    def test_unqualified_old_looting_function_migrates(self):
        self.assertEqual(loot({'function':'looting_enchant','count':1}),
                         {'type':'minecraft:enchanted_count_increase','count':1,'enchantment':'minecraft:looting'})

    def test_grass_block_rename_preserves_configured_feature(self):
        data={'feature':'minecraft:grass','state':{'id':'minecraft:grass'}}
        expected={'feature':'minecraft:grass','state':{'id':'minecraft:short_grass'}}
        self.assertEqual(modern(data),expected)
        self.assertEqual(modern(expected),expected)


if __name__=='__main__':unittest.main()
