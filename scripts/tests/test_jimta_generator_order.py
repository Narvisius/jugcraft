"""The same supplied data must produce identical release resources on Windows/Linux."""
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
import jimtafacts_spellbook_recipes as recipes
import jimtafacts_spellbook_loot as loot
import jimtafacts_spellbook_lore as lore
from jimtafacts_spellbooks import SOURCE, ART, SPELLS


class GeneratorOrdering(unittest.TestCase):
    def generate(self, module, reverse):
        original_glob, original_rglob = Path.glob, Path.rglob

        def ordered(method):
            def enumerate_paths(path, *args, **kwargs):
                return iter(sorted(method(path, *args, **kwargs),
                                   key=lambda p: p.as_posix(), reverse=reverse))
            return enumerate_paths

        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            result = {}
            sounds = root / 'assets/jugcraft/sounds.json'
            sounds.parent.mkdir(parents=True)
            sounds.write_text('{"existing.sound":{"sounds":["jugcraft:existing"]}}', encoding='utf-8')

            def dump(path, value):
                result[path.relative_to(root).as_posix()] = json.dumps(value, ensure_ascii=False)

            with patch.object(Path, 'glob', ordered(original_glob)), \
                 patch.object(Path, 'rglob', ordered(original_rglob)):
                if module is recipes:
                    module.generate(dump, root, SOURCE)
                elif module is loot:
                    module.generate(dump, root, ART, SPELLS)
                else:
                    module.generate(lambda *args: None,
                                    lambda model, name: {'model': model, 'name': name},
                                    lambda *args: None, dump, root, ART, {})
            return result

    def test_directory_enumeration_cannot_reorder_generated_data(self):
        for module in (recipes, loot, lore):
            with self.subTest(generator=module.__name__):
                forward = self.generate(module, False)
                backward = self.generate(module, True)
                self.assertGreater(len(forward), 10)
                self.assertEqual(forward, backward)
