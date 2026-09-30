"""Offline checks for Jugcraft's data files. This is NOT a Minecraft build or game test.

Verifies that every material ID has its blockstate/model/item definition/loot
table/texture/name, that every reference resolves, that Java registration matches
tools/materials.py, and that no recipe creates metal from nothing.
"""
import json
import re
import sys
from pathlib import Path

from PIL import Image

from materials import MOD, METALS, all_blocks, all_items

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src" / "main" / "resources"
ASSETS = RES / "assets" / MOD
DATA = RES / "data"
JAVA = ROOT / "src" / "main" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "materials" / "JugcraftMaterials.java"

# Tags that Jugcraft reads but that vanilla/Fabric API define.
EXTERNAL_TAGS = {"c:ingots/copper", "minecraft:stone_ore_replaceables", "minecraft:deepslate_ore_replaceables"}

errors = []


def err(message):
    errors.append(message)


def load(path):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError) as exc:
        err(f"Unreadable JSON {path.relative_to(ROOT)}: {exc}")
        return None


def split(ref):
    ns, _, path = ref.partition(":")
    return ns, path


def tag_exists(registry, ref):
    ns, path = split(ref)
    return ref in EXTERNAL_TAGS or (DATA / ns / "tags" / registry / f"{path}.json").is_file()


def texture(ref):
    ns, path = split(ref)
    if ns == "minecraft":
        return
    png = RES / "assets" / ns / "textures" / f"{path}.png"
    if not png.is_file():
        err(f"Missing texture {ref}")
        return
    with Image.open(png) as img:
        if img.size != (16, 16):
            err(f"Texture {ref} is {img.size}, expected 16x16")


def model(ref):
    ns, path = split(ref)
    if ns == "minecraft":
        return
    data = load(RES / "assets" / ns / "models" / f"{path}.json")
    if data is None:
        return
    for tex in data.get("textures", {}).values():
        texture(tex)


def check_assets(registered):
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    for block in all_blocks():
        state = load(ASSETS / "blockstates" / f"{block}.json")
        if state:
            for variant in state["variants"].values():
                model(variant["model"])
        if f"block.{MOD}.{block}" not in lang:
            err(f"Missing name for block {block}")
        if not (DATA / MOD / "loot_table" / "blocks" / f"{block}.json").is_file():
            err(f"Missing loot table for {block}")
    for item in registered:
        definition = load(ASSETS / "items" / f"{item}.json")
        if definition:
            model(definition["model"]["model"])
        if item not in all_blocks() and f"item.{MOD}.{item}" not in lang:
            err(f"Missing name for item {item}")


def check_loot(registered):
    for path in sorted((DATA / MOD / "loot_table").rglob("*.json")):
        text = path.read_text(encoding="utf-8")
        for name in re.findall(r'"name": "jugcraft:([a-z_]+)"', text):
            if name not in registered:
                err(f"{path.name} drops unknown item {name}")


# Metal content in nugget units. Tags stand for the same forms from any mod.
UNITS = {"ingots": 9, "nuggets": 1, "raw_materials": 9, "ores": 9, "storage_blocks": 81}


def item_units(ref):
    """Returns {metal: units} for an item or tag reference."""
    ns, path = split(ref.lstrip("#"))
    if ref.startswith("#"):
        form, _, metal = path.partition("/")
        if form not in UNITS or not metal:
            err(f"Recipe uses unsupported tag {ref}")
            return {}
        if metal.startswith("raw_"):
            return {metal[4:]: 81}
        return {metal: UNITS[form]}
    if ns != MOD:
        return {}
    for metal in METALS:
        table = {f"{metal}_ingot": 9, f"{metal}_nugget": 1, f"{metal}_block": 81,
                 f"raw_{metal}": 9, f"raw_{metal}_block": 81, f"{metal}_ore": 9, f"deepslate_{metal}_ore": 9}
        if path in table:
            return {metal: table[path]}
    if path == "bronze_blend":
        return {"bronze": 9}
    err(f"No metal content known for {ref}")
    return {}


def check_recipes(registered):
    for path in sorted((DATA / MOD / "recipe").glob("*.json")):
        recipe = load(path)
        if recipe is None:
            continue
        name = path.stem
        conditions = recipe.get("fabric:load_conditions", [])
        if not any(c.get("condition") == f"{MOD}:feature_enabled" for c in conditions):
            err(f"{name}: missing feature switch condition")

        kind = recipe["type"]
        if kind == "minecraft:crafting_shaped":
            symbols = "".join(recipe["pattern"])
            inputs = [recipe["key"][ch] for ch in symbols if ch != " "]
        elif kind == "minecraft:crafting_shapeless":
            inputs = recipe["ingredients"]
        else:
            inputs = [recipe["ingredient"]]

        for ref in inputs:
            if ref.startswith("#"):
                if not tag_exists("item", ref[1:]):
                    err(f"{name}: unknown tag {ref}")
            elif split(ref)[0] == MOD and split(ref)[1] not in registered:
                err(f"{name}: unknown item {ref}")

        result = recipe["result"]["id"]
        if split(result)[1] not in registered:
            err(f"{name}: unknown result {result}")
        count = recipe["result"].get("count", 1)

        total_in = sum(sum(item_units(ref).values()) for ref in inputs)
        out = {metal: units * count for metal, units in item_units(result).items()}
        if sum(out.values()) > total_in:
            err(f"{name}: creates metal ({total_in} units in, {sum(out.values())} out)")
        if any(item_units(ref).keys() == {"bronze"} for ref in inputs) and set(out) - {"bronze"}:
            err(f"{name}: turns bronze back into its ingredients")


def check_tags():
    for path in sorted(DATA.rglob("tags/*/**/*.json")):
        registry = path.relative_to(DATA).parts[2]
        for value in (load(path) or {}).get("values", []):
            if value.startswith("#"):
                if not tag_exists(registry, value[1:]):
                    err(f"{path.relative_to(ROOT)}: unknown tag {value}")
            elif split(value)[0] == MOD and split(value)[1] not in all_blocks() + all_items():
                err(f"{path.relative_to(ROOT)}: unknown entry {value}")


def check_worldgen():
    for path in sorted((DATA / MOD / "worldgen" / "configured_feature").glob("*.json")):
        for target in (load(path) or {})["config"]["targets"]:
            block = split(target["state"]["Name"])[1]
            if block not in all_blocks():
                err(f"{path.name}: places unknown block {block}")
    for path in sorted((DATA / MOD / "worldgen" / "placed_feature").glob("*.json")):
        feature = split((load(path) or {})["feature"])[1]
        if not (DATA / MOD / "worldgen" / "configured_feature" / f"{feature}.json").is_file():
            err(f"{path.name}: unknown configured feature {feature}")


def check_java():
    source = JAVA.read_text(encoding="utf-8")
    declared = {}
    for name, chain in re.findall(r'MetalFamily\.builder\("([a-z_]+)"\)([^;]*)\.build\(\)', source):
        declared[name] = {"mined": ".mined()" in chain, "extras": re.findall(r'extraItem\("([a-z_]+)"\)', chain)}
    expected = {name: {"mined": info["mined"], "extras": info["extras"]} for name, info in METALS.items()}
    if declared != expected:
        err(f"JugcraftMaterials.java declares {declared}, tools/materials.py expects {expected}")


def main():
    registered = set(all_blocks()) | set(all_items())
    check_assets(sorted(registered))
    check_loot(registered)
    check_recipes(registered)
    check_tags()
    check_worldgen()
    check_java()
    for path in RES.rglob("*.json"):
        load(path)
    if errors:
        print("\n".join(errors), file=sys.stderr)
        sys.exit(1)
    print(f"PASS: {len(registered)} material IDs, data files and recipe audit. No Minecraft build or game test performed.")


if __name__ == "__main__":
    main()
