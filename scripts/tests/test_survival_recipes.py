"""Regression checks for the acquisition graph, not a substitute for game tests."""
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'tools'))
from audit_survival_recipes import expand, reachable, build


class SurvivalGraphTests(unittest.TestCase):
    def edge(self, out, *inputs):
        return {'source': out + '_recipe', 'outputs': [out], 'inputs': list(inputs)}

    def test_reversible_compression_cannot_bootstrap_itself(self):
        routes = [self.edge('ingot', ['nugget']), self.edge('nugget', ['ingot'])]
        self.assertEqual({}, reachable(routes, {}))
        routes.append(self.edge('ingot', ['ore', 'raw_ore'], ['furnace']))
        self.assertNotIn('ingot', reachable(routes, {'ore': 'world'}))
        self.assertIn('nugget', reachable(routes, {'ore': 'world', 'furnace': 'crafted'}))

    def test_one_reachable_alternative_suffices_but_every_slot_is_required(self):
        routes = [self.edge('tool', ['iron', 'steel'], ['handle']), self.edge('handle', ['wood'])]
        self.assertIn('tool', reachable(routes, {'iron': 'mining', 'wood': 'tree'}))
        self.assertNotIn('tool', reachable(routes, {'iron': 'mining'}))

    def test_missing_tag_and_self_drop_do_not_create_resources(self):
        self.assertEqual(set(), expand('#missing', {}))
        self.assertEqual({'stone'}, expand('#a', {'a': ['#b', 'stone'], 'b': ['#a']}))
        self.assertEqual({}, reachable([self.edge('block', ['block'])], {}))

    def test_empty_inputs_are_not_implicitly_creative_generators(self):
        self.assertNotIn('treasure', reachable([self.edge('treasure')], {}))

    def test_repository_ingredients_and_known_entry_routes(self):
        report = build()
        self.assertEqual([], report['invalid_ingredients'])
        self.assertEqual([], report['invalid_template_loot_links'])
        rows = {r['item']: r for r in report['items']}
        unresolved = [r['item'] for r in report['items'] if r['status'] in ('no_documented_route','recipe_chain_needs_review')]
        self.assertEqual([], unresolved, 'Every active survival chain needs a real source')
        self.assertEqual(78, report['summary']['deferred_acquisition_design'])
        for table in ('landscape_keep_loot_table','landscape_camp_loot_table','cavern_caveman_house','cavern_underground_cabin','cavern_gingerbread_town'):
            self.assertNotIn('jugcraft:chests/'+table, report['unbound_chest_tables'])
        for name in ('household_light_freezer', 'household_dark_freezer', 'household_package',
                     'fauna_bug_habitat_empty', 'cavern_spelunkery_table', 'cavern_nuclear_furnace', 'landscape_scoria'):
            self.assertEqual('recipe_chain_reachable', rows['jugcraft:' + name]['status'], name)
        self.assertEqual('intentional_noncraftable', rows['jugcraft:nouveau_creative_spell_book']['status'])
        # Treasure is not silently converted into an ordinary crafting product.
        self.assertFalse(any(e['source'].find('survival_completion') >= 0 and
                             any(o in {'jugcraft:needle_rapier', 'jugcraft:vesper_scythe'} for o in e['outputs'])
                             for e in report['edges']))


if __name__ == '__main__': unittest.main()
