"""Regenerate Jugcraft's material JSON resources from tools/materials.py.

Run from the repository root:  python3 tools/generate_material_data.py
The output is deterministic; CI re-runs it and fails if anything changes.
"""
import json
import shutil
from pathlib import Path

from materials import (MOD, METALS, MINERALS, ROCKS, ITEMS, EXTRA_NAMES, MINERAL_TAGS, PROCESSING,
                       metal_blocks, metal_items, mineral_blocks, all_blocks, all_items, feature_of)

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
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def rid(path):
    return f"{MOD}:{path}"


def condition(feature):
    return [{"condition": f"{MOD}:feature_enabled", "feature": feature}]


def title(path):
    return " ".join(w.capitalize() for w in path.split("_"))


def block_name(block):
    for metal, info in METALS.items():
        if block == f"raw_{metal}_block":
            return f"Block of Raw {info['display']}"
        if block == f"{metal}_block":
            return f"Block of {info['display']}"
    for mineral, info in MINERALS.items():
        if block == f"{mineral}_ore":
            return f"{info['ore_display']} Ore"
        if block == f"deepslate_{mineral}_ore":
            return f"Deepslate {info['ore_display']} Ore"
        if block == f"{mineral}_block":
            return f"Block of {info['display']}"
    if block in ROCKS:
        return ROCKS[block]["display"]
    return title(block)


def item_name(item):
    if item in EXTRA_NAMES:
        return EXTRA_NAMES[item]
    if item in MINERALS:
        return MINERALS[item]["display"]
    if item in ITEMS:
        return ITEMS[item]["display"]
    return title(item)


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
        lang[f"item.{MOD}.{item}"] = item_name(item)
    write(ASSETS / "lang" / "en_us.json", dict(sorted(lang.items())))


# ---------------------------------------------------------------- loot tables

SILK = {"condition": "minecraft:match_tool", "predicate": {"predicates": {
    "minecraft:enchantments": [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}


def loot(block, entries, explosion_condition=False):
    pool = {"rolls": 1.0, "bonus_rolls": 0.0, "entries": entries}
    if explosion_condition:
        pool["conditions"] = [{"condition": "minecraft:survives_explosion"}]
    return {"type": "minecraft:block", "pools": [pool], "random_sequence": rid(f"blocks/{block}")}


def self_drop(block):
    return loot(block, [{"type": "minecraft:item", "name": rid(block)}], explosion_condition=True)


def ore_drop(block, item, low=1, high=1):
    functions = []
    if (low, high) != (1, 1):
        functions.append({"function": "minecraft:set_count",
                          "count": {"type": "minecraft:uniform", "min": float(low), "max": float(high)}})
    functions += [{"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                  {"function": "minecraft:explosion_decay"}]
    return loot(block, [{"type": "minecraft:alternatives", "children": [
        {"type": "minecraft:item", "name": rid(block), "conditions": [SILK]},
        {"type": "minecraft:item", "name": rid(item), "functions": functions},
    ]}])


def loot_tables():
    out = DATA / MOD / "loot_table" / "blocks"
    for metal, info in METALS.items():
        for block in metal_blocks(metal):
            table = ore_drop(block, f"raw_{metal}") if block.endswith("_ore") else self_drop(block)
            write(out / f"{block}.json", table)
    for mineral, info in MINERALS.items():
        low, high = info["drops"]
        for block in mineral_blocks(mineral):
            table = ore_drop(block, mineral, low, high) if block.endswith("_ore") else self_drop(block)
            write(out / f"{block}.json", table)
    for rock, info in ROCKS.items():
        drop = info["drop"]
        table = ore_drop(rock, drop["item"], drop["min"], drop["max"]) if drop else self_drop(rock)
        write(out / f"{rock}.json", table)


# ---------------------------------------------------------------- recipes

def shaped(feature, pattern, key, result, count=1, category="misc"):
    return {"fabric:load_conditions": condition(feature), "type": "minecraft:crafting_shaped", "category": category,
            "pattern": pattern, "key": key, "result": {"id": rid(result), "count": count}}


def shapeless(feature, ingredients, result, count, category="misc"):
    return {"fabric:load_conditions": condition(feature), "type": "minecraft:crafting_shapeless", "category": category,
            "ingredients": ingredients, "result": {"id": rid(result), "count": count}}


COOK_TIME = {"smelting": 200, "blasting": 100}


def cooking(feature, kind, ingredient, result, xp):
    return {"fabric:load_conditions": condition(feature), "type": f"minecraft:{kind}", "category": "misc",
            "ingredient": ingredient, "result": {"id": rid(result)}, "experience": xp, "cookingtime": COOK_TIME[kind]}


def compaction(recipes, feature, small_tag, small, big, category="building"):
    """9 small <-> 1 big, both directions lossless."""
    write(recipes / f"{big}.json", shaped(feature, ["###", "###", "###"], {"#": small_tag}, big, category=category))
    write(recipes / f"{small}_from_{big}.json", shapeless(feature, [rid(big)], small, 9))


def recipes():
    out = DATA / MOD / "recipe"
    for metal, info in METALS.items():
        feature = info["feature"]
        ingot, nugget = f"{metal}_ingot", f"{metal}_nugget"
        compaction(out, feature, f"#c:ingots/{metal}", ingot, f"{metal}_block")
        write(out / f"{ingot}_from_nuggets.json",
              shaped(feature, ["###", "###", "###"], {"#": f"#c:nuggets/{metal}"}, ingot))
        write(out / f"{nugget}.json", shapeless(feature, [f"#c:ingots/{metal}"], nugget, 9))
        if info["mined"]:
            raw = f"raw_{metal}"
            compaction(out, feature, f"#c:raw_materials/{metal}", raw, f"raw_{metal}_block")
            for kind in info["cook"]:
                write(out / f"{ingot}_from_{kind}_{raw}.json",
                      cooking(feature, kind, f"#c:raw_materials/{metal}", ingot, info["xp"]))
                write(out / f"{ingot}_from_{kind}_{metal}_ore.json",
                      cooking(feature, kind, f"#c:ores/{metal}", ingot, info["xp"]))

    for mineral, info in MINERALS.items():
        compaction(out, info["feature"], rid(mineral), mineral, f"{mineral}_block")

    for recipe in PROCESSING:
        if recipe["kind"] == "shapeless":
            write(out / f"{recipe['id']}.json",
                  shapeless(recipe["feature"], recipe["inputs"], recipe["result"], recipe["count"]))
        else:
            write(out / f"{recipe['id']}.json",
                  cooking(recipe["feature"], recipe["kind"], recipe["input"], recipe["result"], recipe["xp"]))


# ---------------------------------------------------------------- tags

class Tags:
    def __init__(self):
        self.entries = {}

    def add(self, registry, tag, value):
        self.entries.setdefault((registry, tag), [])
        if value not in self.entries[(registry, tag)]:
            self.entries[(registry, tag)].append(value)

    def both(self, tag, value):
        self.add("item", tag, value)
        self.add("block", tag, value)

    def write(self):
        for (registry, tag), values in sorted(self.entries.items()):
            namespace, path = tag.split(":")
            write(DATA / namespace / "tags" / registry / f"{path}.json", {"replace": False, "values": values})


TOOL_TAG = {"stone": "minecraft:needs_stone_tool", "iron": "minecraft:needs_iron_tool"}


def ore_tags(tags, name, ore, deep, tool, rate):
    for block_id, ground in ((ore, "stone"), (deep, "deepslate")):
        tags.both(f"c:ores/{name}", rid(block_id))
        tags.both(f"c:ores_in_ground/{ground}", rid(block_id))
        tags.add("block", f"c:ore_rates/{rate}", rid(block_id))
        tags.add("block", "minecraft:mineable/pickaxe", rid(block_id))
        tags.add("block", TOOL_TAG[tool], rid(block_id))
    tags.both("c:ores", f"#c:ores/{name}")


def storage_tags(tags, path, block_id, tool):
    tags.both(f"c:storage_blocks/{path}", rid(block_id))
    tags.both("c:storage_blocks", f"#c:storage_blocks/{path}")
    tags.add("block", "minecraft:mineable/pickaxe", rid(block_id))
    if tool in TOOL_TAG:
        tags.add("block", TOOL_TAG[tool], rid(block_id))


def tags():
    tags = Tags()
    for metal, info in METALS.items():
        tool = info.get("tool", "stone")
        tags.add("item", f"c:ingots/{metal}", rid(f"{metal}_ingot"))
        tags.add("item", "c:ingots", f"#c:ingots/{metal}")
        tags.add("item", f"c:nuggets/{metal}", rid(f"{metal}_nugget"))
        tags.add("item", "c:nuggets", f"#c:nuggets/{metal}")
        storage_tags(tags, metal, f"{metal}_block", tool)
        if info["mined"]:
            tags.add("item", f"c:raw_materials/{metal}", rid(f"raw_{metal}"))
            tags.add("item", "c:raw_materials", f"#c:raw_materials/{metal}")
            storage_tags(tags, f"raw_{metal}", f"raw_{metal}_block", tool)
            ore_tags(tags, metal, f"{metal}_ore", f"deepslate_{metal}_ore", tool, "singular")

    for mineral, info in MINERALS.items():
        rate = "singular" if info["drops"] == [1, 1] else "dense"
        ore_tags(tags, mineral, f"{mineral}_ore", f"deepslate_{mineral}_ore", info["tool"], rate)
        storage_tags(tags, mineral, f"{mineral}_block", "none")
        if mineral in MINERAL_TAGS:
            tags.add("item", f"c:{MINERAL_TAGS[mineral]}", rid(mineral))
            tags.add("item", "c:dusts", f"#c:{MINERAL_TAGS[mineral]}")

    for rock, info in ROCKS.items():
        tags.add("block", f"minecraft:mineable/{info['tool']}", rid(rock))

    for item, info in ITEMS.items():
        if info["tag"]:
            tags.add("item", f"c:{info['tag']}", rid(item))
            if info["tag"].startswith("dusts/"):
                tags.add("item", "c:dusts", f"#c:{info['tag']}")
    tags.write()


# ---------------------------------------------------------------- worldgen

def ore_feature(name, size, targets):
    write(DATA / MOD / "worldgen" / "configured_feature" / f"ore_{name}.json", {
        "type": "minecraft:ore",
        "config": {"size": size, "discard_chance_on_air_exposure": 0.0, "targets": targets},
    })


def placed_feature(name, gen):
    write(DATA / MOD / "worldgen" / "placed_feature" / f"ore_{name}.json", {
        "feature": rid(f"ore_{name}"),
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


def layered_targets(ore, deep):
    return [
        {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"},
         "state": {"Name": rid(ore)}},
        {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables"},
         "state": {"Name": rid(deep)}},
    ]


def worldgen():
    for name, info in list(METALS.items()) + list(MINERALS.items()):
        if "gen" not in info:
            continue
        ore_feature(name, info["gen"]["size"], layered_targets(f"{name}_ore", f"deepslate_{name}_ore"))
        placed_feature(name, info["gen"])
    for rock, info in ROCKS.items():
        gen = info["gen"]
        ore_feature(rock, gen["size"], [{"target": {"predicate_type": "minecraft:tag_match", "tag": gen["target"]},
                                         "state": {"Name": rid(rock)}}])
        placed_feature(rock, gen)


def main():
    for directory in GENERATED_DIRS:
        if directory.exists():
            shutil.rmtree(directory)
    assets()
    loot_tables()
    recipes()
    tags()
    worldgen()


if __name__ == "__main__":
    main()
