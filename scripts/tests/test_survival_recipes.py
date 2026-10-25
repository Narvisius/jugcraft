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
        from survival_jimtafacts_routes import DEFERRED_ACQUISITION
        self.assertEqual(78+len(DEFERRED_ACQUISITION), report['summary']['deferred_acquisition_design'])
        for name in DEFERRED_ACQUISITION:
            self.assertEqual('deferred_acquisition_design', rows['jugcraft:jimta_spellbooks_'+name]['status'])
        for table in ('landscape_keep_loot_table','landscape_camp_loot_table','cavern_caveman_house','cavern_underground_cabin','cavern_gingerbread_town'):
            self.assertNotIn('jugcraft:chests/'+table, report['unbound_chest_tables'])
        for name in ('household_light_freezer', 'household_dark_freezer', 'household_package',
                     'fauna_bug_habitat_empty', 'cavern_spelunkery_table', 'cavern_nuclear_furnace', 'landscape_scoria'):
            self.assertEqual('recipe_chain_reachable', rows['jugcraft:' + name]['status'], name)
        self.assertEqual('intentional_noncraftable', rows['jugcraft:nouveau_creative_spell_book']['status'])
        for name in ('walnut_log', 'walnut_leaves', 'walnut_sapling', 'walnut_planks',
                     'stripped_walnut_log', 'walnut_husk', 'roasted_walnuts', 'walnut_mooncake'):
            self.assertIn('jugcraft:' + name, rows, 'Walnut items must exist in the real registry snapshot')
            self.assertNotIn('jugcraft:' + name, report['unregistered_model_definitions'])
        self.assertTrue(any(e['source'].endswith('/WalnutTree.java')
                            for e in rows['jugcraft:walnut_log']['evidence']))
        self.assertTrue(any(e['source'].endswith('/WalnutLeavesBlock.java')
                            and e['inputs'] == [['jugcraft:walnut_leaves']]
                            and set(e['outputs']) == {'jugcraft:ecology_walnut', 'jugcraft:walnut_husk'}
                            for e in report['edges']))
        for form in ('walnut_log', 'walnut_wood', 'stripped_walnut_log', 'stripped_walnut_wood',
                     'walnut_planks', 'walnut_stairs', 'walnut_slab', 'walnut_fence',
                     'walnut_fence_gate', 'walnut_leaves', 'walnut_sapling'):
            retired = rows['jugcraft:ecology_' + form]
            self.assertEqual('intentional_noncraftable', retired['status'])
            self.assertEqual([], retired['evidence'], 'Retired IDs must not seed acquisition chains')
        for name in ('ecology_walnut', 'ecology_walnut_door', 'ecology_walnut_cabinet',
                     'ecology_walnut_shelf', 'ecology_walnut_boat'):
            self.assertEqual('', rows['jugcraft:' + name]['reason'], 'Active walnut items must not be excluded')
        for name in ('hdpe_offcut', 'rubber_offcut'):
            self.assertEqual('intentional_noncraftable', rows['jugcraft:'+name]['status'])
            self.assertIn('compatibility offcut', rows['jugcraft:'+name]['reason'])
        # Treasure is not silently converted into an ordinary crafting product.
        self.assertFalse(any(e['source'].find('survival_completion') >= 0 and
                             any(o in {'jugcraft:needle_rapier', 'jugcraft:vesper_scythe'} for o in e['outputs'])
                             for e in report['edges']))


if __name__ == '__main__': unittest.main()
