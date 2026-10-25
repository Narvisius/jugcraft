"""Reproduce the bounded native artisan runtime resources from owner supplied art.
Source models/textures are copied, never executed. See the feature record for limits.
"""
from pathlib import Path
import json
import shutil
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
SOURCE=ROOT/'art/owner-library/originals/Oct22/Planes And Furniture/assets/immersive_furniture'
# Native registry/recipe wrappers; authored geometry stays in the source library.
RESOURCES={'assets/jugcraft/blockstates/artisans_workstation.json': {'variants': {'': {'model': 'jugcraft:artisan/block/artisans_workstation'}}}, 'assets/jugcraft/blockstates/artisan_furniture.json': {'variants': {'': {'model': 'jugcraft:block/artisan_furniture_particle'}}}, 'assets/jugcraft/items/artisans_workstation.json': {'model': {'type': 'minecraft:model', 'model': 'jugcraft:artisan/item/artisans_workstation'}}, 'assets/jugcraft/items/artisan_furniture.json': {'model': {'type': 'minecraft:model', 'model': 'jugcraft:crafted/item/oak_chair'}}, 'assets/jugcraft/items/furniture_material.json': {'model': {'type': 'minecraft:model', 'model': 'jugcraft:artisan/item/crafting_material'}}, 'assets/jugcraft/models/artisan/block/artisans_workstation.json': None, 'assets/jugcraft/models/artisan/item/artisans_workstation.json': None, 'assets/jugcraft/models/artisan/item/crafting_material.json': None, 'data/jugcraft/loot_table/blocks/artisans_workstation.json': {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'jugcraft:artisans_workstation'}], 'condition': {'type': 'minecraft:survives_explosion'}}]}, 'data/jugcraft/loot_table/blocks/artisan_furniture.json': {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'jugcraft:artisan_furniture'}], 'condition': {'type': 'minecraft:survives_explosion'}}]}, 'data/jugcraft/recipe/artisans_workstation.json': {'type': 'minecraft:crafting_shaped', 'pattern': ['IPI', 'PCP', 'P P'], 'key': {'I': 'minecraft:iron_ingot', 'P': '#minecraft:planks', 'C': 'minecraft:crafting_table'}, 'result': {'id': 'jugcraft:artisans_workstation', 'count': 1}, 'fabric:load_conditions': [{'condition': 'jugcraft:feature_enabled', 'feature': 'agriculture'}]}, 'data/jugcraft/recipe/furniture_material.json': {'type': 'minecraft:crafting_shapeless', 'ingredients': ['#minecraft:planks', '#minecraft:planks', 'minecraft:stick'], 'result': {'id': 'jugcraft:furniture_material', 'count': 8}, 'fabric:load_conditions': [{'condition': 'jugcraft:feature_enabled', 'feature': 'agriculture'}]}, 'assets/jugcraft/models/block/artisan_furniture_particle.json': {'textures': {'particle': 'minecraft:block/oak_planks'}, 'elements': []}}

def generate():
    atlas=RES/'assets/minecraft/atlases/blocks.json'
    data=json.loads(atlas.read_text(encoding='utf-8'))
    source={'type':'minecraft:directory','source':'artisan','prefix':'artisan/'}
    if source not in data['sources']: data['sources'].append(source)
    atlas.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
    for relative,data in RESOURCES.items():
        target=RES/relative
        if '/models/artisan/' in relative:
            source=SOURCE/'models'/relative.split('/models/artisan/')[1]
            data=json.loads(source.read_text(encoding='utf-8').replace('immersive_furniture:','jugcraft:artisan/'))
        target.parent.mkdir(parents=True,exist_ok=True)
        target.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
    for relative in ('block/artisans_workstation.png','item/crafting_material.png'):
        source=SOURCE/'textures'/relative
        target=RES/'assets/jugcraft/textures/artisan'/relative
        target.parent.mkdir(parents=True,exist_ok=True)
        shutil.copyfile(source,target)

if __name__=='__main__': generate()
