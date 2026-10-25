"""Source regeneration and fixture freshness for the crafting/processor conflict repairs."""
import json
from fractions import Fraction
from pathlib import Path
import sys
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
RES = ROOT / 'src/main/resources'


def read(path):
    return json.loads(path.read_text(encoding='utf-8'))


class RecipeConflicts(unittest.TestCase):
    def test_repaired_crafting_does_not_create_essence(self):
        from audit_survival_recipes import expand
        tags = read(ROOT / 'tools/data/survival-reference-26.3.json')['tags']
        for path in (RES / 'data').glob('*/tags/item/**/*.json'):
            parts = path.relative_to(RES / 'data').parts
            name = parts[0] + ':' + '/'.join(parts[3:])[:-5]
            data = read(path)
            tags[name] = ([] if data.get('replace') else tags.get(name, [])) + data.get('values', [])
        values = read(RES / 'data/jugcraft/alchemy/values.json')['items']

        def value(item):
            entry = values.get(item, {'numerator': 1, 'denominator': 1})
            return Fraction(entry['numerator'], entry['denominator'])

        for case in read(ROOT / 'src/gametest/resources/recipe_conflict_cases.json'):
            if case['recipe'].startswith('minecraft:'):
                continue
            cost = sum(min(value(item) for item in expand(ref, tags)) for ref in case['grid'] if ref)
            self.assertLessEqual(value(case['output']) * case['count'], cost, case['recipe'])

    def test_crafting_regression_fixtures_match_shipped_recipes(self):
        cases = read(ROOT / 'src/gametest/resources/recipe_conflict_cases.json')
        self.assertGreaterEqual(len(cases), 174)
        self.assertEqual(len({case['recipe'] for case in cases}), len(cases))
        for case in cases:
            ns, name = case['recipe'].split(':', 1)
            if ns == 'minecraft':
                continue  # Real vanilla recipes are checked by the server GameTest.
            recipe = read(RES / 'data' / ns / 'recipe' / (name + '.json'))
            if 'pattern' in recipe:
                grid = [recipe['key'].get(c, '') for row in recipe['pattern'] for c in row.ljust(3)]
            else:
                grid = recipe['ingredients'][:]
            self.assertEqual(case['grid'], grid + [''] * (9 - len(grid)), case['recipe'])
            self.assertEqual((case['output'], case['count']),
                             (recipe['result']['id'], recipe['result'].get('count', 1)))

    def test_main_generator_keeps_repaired_recipes_and_citrus_tag(self):
        import generate_material_data as generator
        cases = {c['recipe'] for c in read(ROOT / 'src/gametest/resources/recipe_conflict_cases.json')}
        checked = set()

        def capture(path, data):
            if '/recipe/' in path.as_posix():
                name = 'jugcraft:' + path.relative_to(RES / 'data/jugcraft/recipe').with_suffix('').as_posix()
                if name in cases:
                    self.assertEqual(data, read(path), name)
                    checked.add(name)
            elif path.name == 'compact_fermentable.json':
                self.assertEqual(data, read(path))
                checked.add('compact_fermentable')

        with patch.object(generator, 'write', capture):
            generator.recipes()
            generator.tags()
        self.assertGreaterEqual(len(checked), 40)
        self.assertIn('compact_fermentable', checked)

    def test_other_authored_generators_keep_distinct_inputs(self):
        import generate_material_data as generator
        import concordance_alchemy, concordance_ember, starter_workshop
        import industrial_forms, bunkerworks
        checked = set()
        wanted = {'stirring_rod', 'pyromaniacs_tunic', 'soldering_tool'}

        def capture(path, data):
            if '/recipe/' in path.as_posix() and path.stem in wanted:
                self.assertEqual(data, read(path))
                checked.add(path.stem)

        for source in (concordance_alchemy, concordance_ember):
            source.write_all(capture, RES / 'assets/jugcraft', RES / 'data/jugcraft', {}, generator.condition, generator.self_drop)
        starter_workshop.write_all(capture, RES / 'assets/jugcraft', RES / 'data/jugcraft', {})
        self.assertEqual(checked, wanted)
        for name in ('polymer_molding_press', 'precision_finisher'):
            pattern, key = industrial_forms.FORMS[name]['recipe']
            actual = read(RES / f'data/jugcraft/recipe/{name}.json')
            self.assertEqual((pattern, key), (actual['pattern'], actual['key']))
        row = next(r for r in bunkerworks.RECIPES if r[0] == 'corrugated_iron')
        actual = read(RES / 'data/jugcraft/recipe/corrugated_iron.json')
        self.assertEqual((row[2], row[3], row[4]), (actual['pattern'], actual['key'], actual['result']['count']))

    def test_owner_adapters_do_not_restore_duplicate_pet_or_gravestone_patterns(self):
        import pocket_content, menagerie_seasonal
        for name in pocket_content.PET_TOKENS:
            self.assertEqual(pocket_content.fallback_recipe(name), read(RES / f'data/jugcraft/recipe/{name}.json'))
        for style in ('cross', 'upright'):
            for material in ('stone', 'mossy'):
                actual = read(RES / f'data/jugcraft/recipe/herbal_gravestone_{style}_{material}.json')
                actual.pop('fabric:load_conditions')
                self.assertEqual(menagerie_seasonal.gravestone_recipe(style, material), actual)

    def test_compact_fermentation_is_distinct_and_viewer_is_current(self):
        import petro, recipe_view
        broad = set(read(RES / 'data/jugcraft/tags/item/fermentable.json')['values'])
        compact = set(read(RES / 'data/jugcraft/tags/item/compact_fermentable.json')['values'])
        self.assertEqual(broad - compact, {'jugcraft:lemon', 'jugcraft:orange'})
        self.assertFalse(compact - broad)
        actual = read(RES / 'data/jugcraft/recipe/chemical_reaction/bioethanol.json')
        generated = next(data for kind, name, data in petro.fluid_recipe_files(lambda f: [
            {'condition': 'jugcraft:feature_enabled', 'feature': f}]) if name == 'bioethanol')
        self.assertEqual(actual, generated)
        self.assertEqual(read(RES / 'assets/jugcraft/recipe_view.json'), recipe_view.build())


if __name__ == '__main__':
    unittest.main()
