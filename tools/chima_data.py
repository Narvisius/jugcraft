"""Chima the lion cub's resources (docs/features/chima.md; Java: creatures/chima): its names, its spawn egg's item model,
the item tags for what tames it and what changes its look, and its textures (tools/chima_textures.py).

Called from generate_material_data.py.
"""
import chima_textures

MOD = "jugcraft"
# What tames and heals it.
FOOD = ["minecraft:beef", "minecraft:cooked_beef"]
# What changes its look, by look (Chima.Skin, tag jugcraft:chima_skins/<look>).
SKIN_ITEMS = {
    "pumpkin": ["minecraft:jack_o_lantern"],
    "zombie": ["minecraft:rotten_flesh"],
    "skeleton": ["minecraft:bone"],
    "shark": ["minecraft:cod", "minecraft:salmon", "minecraft:tropical_fish", "minecraft:pufferfish"],
}


def write_all(write, resources, lang):
    assets, data = resources / "assets" / MOD, resources / "data" / MOD
    lang[f"entity.{MOD}.chima"] = "Chima"
    lang[f"item.{MOD}.chima_spawn_egg"] = "Chima Spawn Egg"
    write(assets / "items" / "chima_spawn_egg.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:item/chima_spawn_egg"}})
    write(assets / "models" / "item" / "chima_spawn_egg.json",
          {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/chima_spawn_egg"}})
    write(data / "tags" / "item" / "chima_food.json", {"values": FOOD})
    for skin, items in SKIN_ITEMS.items():
        write(data / "tags" / "item" / "chima_skins" / f"{skin}.json", {"values": items})
    chima_textures.save_all(assets)
