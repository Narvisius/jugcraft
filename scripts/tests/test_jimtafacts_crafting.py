"""Distinct native crafting bills, including competitors outside the collection."""
from collections import defaultdict
import json
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
from jimtafacts_crafting import distinct


class JimtafactsCraftingTests(unittest.TestCase):
    def test_refinement_preserves_callers_and_nine_slot_limit(self):
        original = ['minecraft:brick'] * 3
        self.assertEqual(distinct('jimtafacts_crops/mug', original), original + ['minecraft:stick'])
        self.assertEqual(original, ['minecraft:brick'] * 3)
        with self.assertRaises(ValueError):
            distinct('jimtafacts_crops/mug', ['minecraft:brick'] * 9)

    def test_no_identical_concrete_bills_with_conflicting_shapeless_outputs(self):
        groups = defaultdict(list)
        base = ROOT / 'src/main/resources/data/jugcraft/recipe'
        for path in base.rglob('*.json'):
            row = json.loads(path.read_text(encoding='utf-8'))
            kind = row.get('type')
            if kind == 'minecraft:crafting_shapeless':
                ingredients = row['ingredients']
            elif kind == 'minecraft:crafting_shaped':
                ingredients = [row['key'][c] for line in row['pattern'] for c in line if c != ' ']
            else:
                continue
            if not all(isinstance(x, str) and not x.startswith('#') for x in ingredients):
                continue  # Runtime fixtures cover tags using actual registry matching.
            output = row['result']
            groups[tuple(sorted(ingredients))].append((path.relative_to(base).as_posix(), kind,
                                                       output['id'], output.get('count', 1)))
        conflicts = []
        for ingredients, rows in groups.items():
            if not any('jimta' in row[0] for row in rows):
                continue
            if any(row[1].endswith('shapeless') for row in rows) and len({row[2:] for row in rows}) > 1:
                conflicts.append((ingredients, rows))
        self.assertEqual(conflicts, [])
