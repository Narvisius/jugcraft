"""Regenerate Jugcraft's material JSON resources from tools/materials.py.

Run from the repository root:  python3 tools/generate_material_data.py
The output is deterministic; CI re-runs it and fails if anything changes.
"""
import json
import shutil
from pathlib import Path

from materials import MOD, METALS, EXTRA_NAMES, ORE_GEN, blocks, items, all_blocks, all_items

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src" / "main" / "resources"
ASSETS = RES / "assets" / MOD
DATA = RES / "data"

GENERATED_DIRS = [
    ASSETS / "blockstates", ASSETS / "items", ASSETS / "models", ASSETS / "lang",
    DATA / MOD / "loot_table", DATA / MOD / "recipe", DATA / MOD / "worldgen",
    DATA / "c" / "tags", DATA / "minecraft" / "tags",
]


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, sort_keys=False) + "\n", encoding="utf-8")


def rid(path):
    return f"{MOD}:{path}"


def condition(metal):
    return [{"condition": f"{MOD}:feature_enabled", "feature": METALS[metal]["feature"]}]


def title(path):
    return " ".join(w.capitalize() for w in path.split("_"))


def block_name(block):
    if block.startswith("raw_") and block.endswith("_block"):
        return f"Block of Raw {METALS[block[4:-6]]['display']}"
    if block.endswith("_block"):
        return f"Block of {METALS[block[:-6]]['display']}"
    return title(block)


def assets():
    lang = {}
    for block in all_blocks():
        write(ASSETS / "blockstates" / f"{block}.json", {"variants": {"": {"model": rid(f"block/{block}")}}})
        write(ASSETS / "models" / "block" / f"{block}.json",
              {"parent": "minecraft:block/cube_all", "textures": {"all": rid(f"block/{block}")}})
        write(ASSETS / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{block}")}})
        lang[f"block.{MOD}.{block}"] = block_name(block)
    for item in all_items():
        write(ASSETS / "models" / "item" / f"{item}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{item}")}})
        write(ASSETS / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{item}")}})
        lang[f"item.{MOD}.{item}"] = EXTRA_NAMES.get(item, title(item))
    write(ASSETS / "lang" / "en_us.json", dict(sorted(lang.items())))


def self_drop(block):
    return {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1.0, "bonus_rolls": 0.0,
            "entries": [{"type": "minecraft:item", "name": rid(block)}],
            "conditions": [{"condition": "minecraft:survives_explosion"}],
        }],
        "random_sequence": rid(f"blocks/{block}"),
    }


def ore_drop(block, raw):
    silk = {"condition": "minecraft:match_tool", "predicate": {"predicates": {
        "minecraft:enchantments": [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}
    return {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1.0, "bonus_rolls": 0.0,
            "entries": [{"type": "minecraft:alternatives", "children": [
                {"type": "minecraft:item", "name": rid(block), "conditions": [silk]},
                {"type": "minecraft:item", "name": rid(raw), "functions": [
                    {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                    {"function": "minecraft:explosion_decay"},
                ]},
            ]}],
        }],
        "random_sequence": rid(f"blocks/{block}"),
    }


def shaped(metal, pattern, key, result, count=1, category="misc"):
    return {"fabric:load_conditions": condition(metal), "type": "minecraft:crafting_shaped", "category": category,
            "pattern": pattern, "key": key, "result": {"id": rid(result), "count": count}}


def shapeless(metal, ingredients, result, count, category="misc"):
    return {"fabric:load_conditions": condition(metal), "type": "minecraft:crafting_shapeless", "category": category,
            "ingredients": ingredients, "result": {"id": rid(result), "count": count}}


def cooking(metal, kind, ingredient, result, xp, time):
    return {"fabric:load_conditions": condition(metal), "type": f"minecraft:{kind}", "category": "misc",
            "ingredient": ingredient, "result": {"id": rid(result)}, "experience": xp, "cookingtime": time}


def data():
    recipes = DATA / MOD / "recipe"
    for metal, info in METALS.items():
        for block in blocks(metal):
            if block.endswith("_ore"):
                write(DATA / MOD / "loot_table" / "blocks" / f"{block}.json", ore_drop(block, f"raw_{metal}"))
            else:
                write(DATA / MOD / "loot_table" / "blocks" / f"{block}.json", self_drop(block))

        ingot, nugget, storage = f"{metal}_ingot", f"{metal}_nugget", f"{metal}_block"
        write(recipes / f"{storage}.json",
              shaped(metal, ["###", "###", "###"], {"#": f"#c:ingots/{metal}"}, storage, category="building"))
        write(recipes / f"{ingot}_from_{storage}.json", shapeless(metal, [rid(storage)], ingot, 9))
        write(recipes / f"{ingot}_from_nuggets.json",
              shaped(metal, ["###", "###", "###"], {"#": f"#c:nuggets/{metal}"}, ingot))
        write(recipes / f"{nugget}.json", shapeless(metal, [f"#c:ingots/{metal}"], nugget, 9))

        if info["mined"]:
            raw, raw_block = f"raw_{metal}", f"raw_{metal}_block"
            write(recipes / f"{raw_block}.json",
                  shaped(metal, ["###", "###", "###"], {"#": f"#c:raw_materials/{metal}"}, raw_block, category="building"))
            write(recipes / f"{raw}_from_{raw_block}.json", shapeless(metal, [rid(raw_block)], raw, 9))
            for kind, time in (("smelting", 200), ("blasting", 100)):
                write(recipes / f"{ingot}_from_{kind}_{raw}.json",
                      cooking(metal, kind, f"#c:raw_materials/{metal}", ingot, 0.7, time))
                write(recipes / f"{ingot}_from_{kind}_{metal}_ore.json",
                      cooking(metal, kind, f"#c:ores/{metal}", ingot, 0.7, time))

    # Bronze: 3 copper + 1 tin -> 4 blend (75/25, near historical bell bronze); 1 blend -> 1 ingot.
    write(recipes / "bronze_blend.json", shapeless(
        "bronze", ["#c:ingots/copper", "#c:ingots/copper", "#c:ingots/copper", "#c:ingots/tin"], "bronze_blend", 4))
    for kind, time in (("smelting", 200), ("blasting", 100)):
        write(recipes / f"bronze_ingot_from_{kind}_bronze_blend.json",
              cooking("bronze", kind, rid("bronze_blend"), "bronze_ingot", 0.1, time))

    tags()
    worldgen()


def tag(registry, namespace, path, values):
    write(DATA / namespace / "tags" / registry / f"{path}.json", {"replace": False, "values": values})


def tags():
    ingots, nuggets, storage_items, storage_blocks = [], [], [], []
    raws, ore_items, ore_blocks, stone, deepslate, singular = [], [], [], [], [], []
    pickaxe, stone_tool = [], []
    for metal, info in METALS.items():
        tag("item", "c", f"ingots/{metal}", [rid(f"{metal}_ingot")])
        tag("item", "c", f"nuggets/{metal}", [rid(f"{metal}_nugget")])
        ingots.append(f"#c:ingots/{metal}")
        nuggets.append(f"#c:nuggets/{metal}")
        storage = [f"{metal}_block"]
        if info["mined"]:
            ore, deep = f"{metal}_ore", f"deepslate_{metal}_ore"
            storage.append(f"raw_{metal}_block")
            tag("item", "c", f"raw_materials/{metal}", [rid(f"raw_{metal}")])
            raws.append(f"#c:raw_materials/{metal}")
            for registry in ("item", "block"):
                tag(registry, "c", f"ores/{metal}", [rid(ore), rid(deep)])
            ore_items.append(f"#c:ores/{metal}")
            ore_blocks.append(f"#c:ores/{metal}")
            stone.append(rid(ore))
            deepslate.append(rid(deep))
            singular += [rid(ore), rid(deep)]
            pickaxe += [rid(ore), rid(deep)]
            stone_tool += [rid(ore), rid(deep), rid(f"raw_{metal}_block")]
        for block in storage:
            path = f"storage_blocks/{block[:-6]}"
            for registry in ("item", "block"):
                tag(registry, "c", path, [rid(block)])
            storage_items.append(f"#c:{path}")
            storage_blocks.append(f"#c:{path}")
            pickaxe.append(rid(block))
            if block == f"{metal}_block":
                stone_tool.append(rid(block))

    tag("item", "c", "ingots", ingots)
    tag("item", "c", "nuggets", nuggets)
    tag("item", "c", "raw_materials", raws)
    tag("item", "c", "ores", ore_items)
    tag("block", "c", "ores", ore_blocks)
    tag("item", "c", "storage_blocks", storage_items)
    tag("block", "c", "storage_blocks", storage_blocks)
    for registry in ("item", "block"):
        tag(registry, "c", "ores_in_ground/stone", stone)
        tag(registry, "c", "ores_in_ground/deepslate", deepslate)
    tag("block", "c", "ore_rates/singular", singular)
    tag("block", "minecraft", "mineable/pickaxe", sorted(pickaxe))
    tag("block", "minecraft", "needs_stone_tool", sorted(stone_tool))


def worldgen():
    for metal, gen in ORE_GEN.items():
        write(DATA / MOD / "worldgen" / "configured_feature" / f"ore_{metal}.json", {
            "type": "minecraft:ore",
            "config": {
                "size": gen["size"],
                "discard_chance_on_air_exposure": 0.0,
                "targets": [
                    {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"},
                     "state": {"Name": rid(f"{metal}_ore")}},
                    {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables"},
                     "state": {"Name": rid(f"deepslate_{metal}_ore")}},
                ],
            },
        })
        write(DATA / MOD / "worldgen" / "placed_feature" / f"ore_{metal}.json", {
            "feature": rid(f"ore_{metal}"),
            "placement": [
                {"type": "minecraft:count", "count": gen["count"]},
                {"type": "minecraft:in_square"},
                {"type": "minecraft:height_range", "height": {
                    "type": "minecraft:trapezoid",
                    "min_inclusive": {"absolute": gen["min_y"]},
                    "max_inclusive": {"absolute": gen["max_y"]},
                }},
                {"type": "minecraft:biome"},
            ],
        })


def main():
    for directory in GENERATED_DIRS:
        if directory.exists():
            shutil.rmtree(directory)
    assets()
    data()


if __name__ == "__main__":
    main()
