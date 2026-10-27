"""Targeted refresh; the normal material generator calls the same resource writer."""
import json
import generate_material_data as gen
import storage_crates
import handbook
import encyclopedia_routes
import recipe_view

lang = json.loads((gen.ASSETS/'lang/en_us.json').read_text(encoding='utf-8'))
crate_lang = {}
storage_crates.write_all(gen.write, gen.ASSETS, gen.DATA/gen.MOD, crate_lang)
# Match the normal generator: storage follows starter-workshop language, before
# the town, vehicle-bay and authored-resource passes append their entries.
keys = [key for key in lang if key not in crate_lang]
split = max(i for i, key in enumerate(keys) if key.startswith('screen.jugcraft.workshop.')) + 1
lang = {**{key: lang[key] for key in keys[:split]}, **crate_lang,
        **{key: lang[key] for key in keys[split:]}}
gen.write(gen.ASSETS/'lang/en_us.json', lang)
# The canonical generation pass finishes with authored language patches, which
# retain literal Unicode. Preserve that format in this targeted refresh too.
(gen.ASSETS/'lang/en_us.json').write_text(json.dumps(lang, indent=2, ensure_ascii=False)+'\n', encoding='utf-8')
book = handbook.build()
gen.write(gen.ASSETS/'handbook/en_us.json', book)
gen.write(gen.ASSETS/'encyclopedia/en_us.json', encyclopedia_routes.build(book))
gen.write(gen.ASSETS/'recipe_view.json', recipe_view.build())
previous = set(json.loads(gen.RESOURCE_MANIFEST.read_text()))
gen.RESOURCE_MANIFEST.write_text(json.dumps(sorted(previous | gen.WRITTEN_RESOURCES),indent=2)+'\n',encoding='utf-8')
