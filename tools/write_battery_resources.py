"""Author package 3 resources without builds, tests or deleting generated directories."""
import json
import generate_material_data as data
import battery_expansion as battery
import model_writer
import gui_textures
import write_ceramics_resources
import petro

def main():
    # Shared recipe/resource authoring includes all three local packages.
    write_ceramics_resources.main()
    lang_path = data.ASSETS / 'lang/en_us.json'
    lang = json.loads(lang_path.read_text(encoding='utf-8'))
    battery.write_extras(data.write, data.ASSETS, data.DATA / 'jugcraft', lang, data.condition)
    subset = {name:dict(display=display,lit=False) for name,display in battery.BLOCKS.items()}
    subset['flow_battery'] = data.MACHINES['flow_battery']
    model_writer.WRITERS[model_writer.DEFAULT_STYLE](data.ASSETS, subset, {}, {})
    for kind, name, recipe in petro.fluid_recipe_files(data.condition):
        if name.startswith('battery_'):
            data.write(data.DATA / 'jugcraft/recipe' / kind / (name+'.json'), recipe)
    for name in battery.BLOCKS:
        data.write(data.DATA / 'jugcraft/loot_table/blocks' / (name+'.json'), data.self_drop(name))
    # Use the same GUI generator and tags as a normal full generation pass.
    gui_textures.write_all(data.write, data.ASSETS, lang, data.MACHINES)
    data.tags()
    data.write(lang_path, dict(sorted(lang.items())))
    def save(image, kind, name):
        path = data.ASSETS / 'textures' / kind / (name+'.png')
        path.parent.mkdir(parents=True,exist_ok=True)
        image.save(path,optimize=True)
    battery.draw_all(save)

if __name__ == '__main__':
    main()
