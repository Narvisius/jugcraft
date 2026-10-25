"""Keep source regeneration and the default viewer in step with the food repairs."""
import json
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
import agriculture
import household_content
import menagerie_content
import survival_recipe_completion
import station_recipe_view

RECIPES = ROOT / 'src/main/resources/data/jugcraft/recipe'


def read(path):
    return json.loads(path.read_text(encoding='utf-8'))


class FoodRecipeConflicts(unittest.TestCase):
    def test_authored_sources_and_whole_item_costs(self):
        for name, ingredient, count, output_count in (
                ('popcorn_ball', 'jugcraft:caramel_popcorn', 1, 1),
                ('orange_dye_from_marigold', 'jugcraft:marigold_petals', 4, 1)):
            source = next(r for r in agriculture.SHAPELESS if r['id'] == name)
            actual = read(RECIPES / (name + '.json'))
            self.assertEqual(source['inputs'], [ingredient] * count)
            self.assertEqual(actual['ingredients'], source['inputs'])
            self.assertEqual(actual['result']['count'], output_count)
        layer = survival_recipe_completion.definitions()['cavern_cake_layer']
        self.assertEqual(layer, read(RECIPES / 'survival_completion/cavern_cake_layer.json'))
        self.assertEqual(layer['ingredients'], ['jugcraft:cake_slice'] * 7)
        self.assertEqual(layer['result']['count'], 4)

    def test_household_reimport_keeps_distinct_routes(self):
        for name in ('pumpkin_seeds_from_pumpkin', 'stick_from_dead_bush'):
            source = household_content.adapt(household_content.read(
                household_content.SOURCE / 'recipe/slicing' / (name + '.json')))
            converted = household_content.resolve_slicing_conflict(dict(type='jugcraft:cutting',
                ingredient=source['ingredient'], tool='#jugcraft:knives', results=[source['result']]))
            actual = read(RECIPES / 'household/slicing' / (name + '.json'))
            actual.pop('fabric:load_conditions')
            self.assertEqual(converted, actual)
        seeds = read(RECIPES / 'household/slicing/pumpkin_seeds_from_pumpkin.json')
        self.assertEqual(seeds['ingredients'], ['jugcraft:pumpkin_slice'] * 4)
        self.assertEqual(seeds['result']['count'], 5)

    def test_wildlife_reimport_does_not_restore_competing_cooking_recipes(self):
        sources = list((menagerie_content.NATURE / 'naturalist/recipe').glob('cooked_egg*.json'))
        self.assertEqual(len(sources), 3)
        for path in sources:
            generated = menagerie_content.resolve_egg_recipe(menagerie_content.remap(menagerie_content.read(path)))
            target = RECIPES / ('wildlife_' + path.name)
            if generated is None:
                self.assertFalse(target.exists())
            else:
                self.assertEqual(generated, read(target))
                self.assertEqual(generated['type'], 'jugcraft:household_pan')
                self.assertEqual(generated['ingredient'], '#c:eggs')
        for method, time in (('smelting', 200), ('smoking', 100), ('campfire_cooking', 600)):
            suffix = '' if method == 'smelting' else '_from_' + method
            recipe = read(RECIPES / ('fried_egg' + suffix + '.json'))
            self.assertEqual(recipe['cookingtime'], time)
            self.assertEqual(recipe['result']['id'], 'jugcraft:fried_egg')

    def test_station_catalog_moves_recipes_instead_of_leaving_stale_displays(self):
        groups = {g['type']: g for g in station_recipe_view.build()}
        cuts = {r['id']: r for r in groups['cutting']['recipes']}
        self.assertNotIn('survival_completion/cavern_cake_layer', cuts)
        self.assertNotIn('household/slicing/pumpkin_seeds_from_pumpkin', cuts)
        self.assertEqual(cuts['household/slicing/stick_from_dead_bush']['inputs'][0]['ingredient'],
                         'jugcraft:landscape_dead_branch')
        eggs = [r for r in groups['household_pan']['recipes'] if r['id'] == 'wildlife_cooked_egg']
        self.assertEqual(len(eggs), 1)
        self.assertEqual(eggs[0]['outputs'], [{'id': 'jugcraft:wildlife_cooked_egg'}])


if __name__ == '__main__':
    unittest.main()
