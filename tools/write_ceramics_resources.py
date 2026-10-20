"""Emit the ceramics slice without deleting or rebuilding unrelated resource trees.

This is resource authoring, not a build, audit or test runner. The normal full data
and texture generators also own all of these outputs.
Run from the repository root: python tools/write_ceramics_resources.py
"""
import json
from pathlib import Path

import generate_material_data as data
import industrial_forms
import handbook
import encyclopedia_routes
import recipe_view
import machine_rotors
import ceramics_art


def main():
    lang_path = data.ASSETS / 'lang/en_us.json'
    lang = json.loads(lang_path.read_text(encoding='utf-8'))
    industrial_forms.write_all(data.write, data.ASSETS, data.DATA / 'jugcraft', lang,
                               data.condition, data.self_drop)
    data.write(lang_path, dict(sorted(lang.items())))
    data.machine_recipe_files(data.DATA / 'jugcraft/recipe')
    book = handbook.build()
    data.write(data.ASSETS / 'handbook/en_us.json', book)
    data.write(data.ASSETS / 'encyclopedia/en_us.json', encyclopedia_routes.build(book))
    data.write(data.ASSETS / 'recipe_view.json', recipe_view.build())
    data.write(data.ASSETS / 'machine_rotor_quads.json', machine_rotors.export())

    def save(img, kind, name):
        path = data.ASSETS / 'textures' / kind / (name + '.png')
        path.parent.mkdir(parents=True, exist_ok=True)
        img.save(path, optimize=True)

    ceramics_art.draw_all(save)


if __name__ == '__main__':
    main()
