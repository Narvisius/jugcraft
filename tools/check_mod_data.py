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

from materials import (MOD, METALS, MINERALS, ROCKS, ITEMS, FEATURES, COMPONENTS, PART_UNITS, CIRCUITS,
                       all_blocks, all_items, feature_of)
from machines import MACHINES, STATS, ORE_PROCESSING_MULTIPLIER, machine_blocks, machine_recipes

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src" / "main" / "resources"
ASSETS = RES / "assets" / MOD
DATA = RES / "data"
JAVA_ROOT = ROOT / "src" / "main" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft"
JAVA = JAVA_ROOT / "materials" / "JugcraftMaterials.java"
CONFIG = JAVA_ROOT / "config" / "JugcraftConfig.java"
WORLDGEN = JAVA_ROOT / "materials" / "JugcraftWorldgen.java"
MACHINE_JAVA = JAVA_ROOT / "machine" / "MachineKind.java"

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
    for block in all_blocks() + machine_blocks():
        state = load(ASSETS / "blockstates" / f"{block}.json")
        if state:
            for variant in state.get("variants", {}).values():
                model(variant["model"])
            for part in state.get("multipart", []):
                model(part["apply"]["model"])
        if f"block.{MOD}.{block}" not in lang:
            err(f"Missing name for block {block}")
        if not (DATA / MOD / "loot_table" / "blocks" / f"{block}.json").is_file():
            err(f"Missing loot table for {block}")
    for item in registered:
        definition = load(ASSETS / "items" / f"{item}.json")
        if definition:
            model(definition["model"]["model"])
        if item not in all_blocks() + machine_blocks() and f"item.{MOD}.{item}" not in lang:
            err(f"Missing name for item {item}")


def check_loot(registered):
    for path in sorted((DATA / MOD / "loot_table").rglob("*.json")):
        text = path.read_text(encoding="utf-8")
        for name in re.findall(r'"name": "jugcraft:([a-z_]+)"', text):
            if name not in registered:
                err(f"{path.name} drops unknown item {name}")


# Metal content in nugget units. Tags stand for the same forms from any mod.
UNITS = {"ingots": 9, "nuggets": 1, "raw_materials": 9, "ores": 9, "storage_blocks": 81,
         **{f"{form}s": units for form, units in PART_UNITS.items()}}


NON_METAL = set(MINERALS) | set(ITEMS) | set(machine_blocks()) | set(CIRCUITS) | {b for m in MINERALS for b in (f"{m}_ore", f"deepslate_{m}_ore", f"{m}_block")} | {"oil_sand"}


def item_units(ref):
    """Returns {metal: units} for an item or tag reference."""
    ns, path = split(ref.lstrip("#"))
    if ref.startswith("#"):
        form, _, metal = path.partition("/")
        if metal in MINERALS or path in {info["tag"] for info in ITEMS.values()}:
            return {}
        if form not in UNITS or not metal:
            err(f"Recipe uses unsupported tag {ref}")
            return {}
        if metal.startswith("raw_"):
            return {metal[4:]: 81}
        return {metal: UNITS[form]}
    if ns != MOD:
        vanilla = {"copper_ingot": ("copper", 9), "iron_ingot": ("iron", 9), "gold_ingot": ("gold", 9)}
        if ns == "minecraft" and path in vanilla:
            metal, units = vanilla[path]
            return {metal: units}
        return {}
    for metal in METALS:
        table = {f"{metal}_ingot": 9, f"{metal}_nugget": 1, f"{metal}_block": 81,
                 f"raw_{metal}": 9, f"raw_{metal}_block": 81, f"{metal}_ore": 9, f"deepslate_{metal}_ore": 9}
        if path in table:
            return {metal: table[path]}
    if path == "bronze_blend":
        return {"bronze": 9}
    for form, metals in COMPONENTS.items():
        for metal in metals:
            if path == f"{metal}_{form}":
                return {metal: PART_UNITS[form]}
    if path == "bauxite":
        # One bauxite holds one ingot of aluminum: the arc furnace recovers all of it,
        # the blast-furnace stand-in only a nugget.
        return {"aluminum": 9}
    if path in NON_METAL:
        return {}
    err(f"No metal content known for {ref}")
    return {}


def check_recipes(registered):
    for path in sorted((DATA / MOD / "recipe").glob("*.json")):
        recipe = load(path)
        if recipe is None:
            continue
        name = path.stem
        conditions = recipe.get("fabric:load_conditions", [])
        features = [c.get("feature") for c in conditions if c.get("condition") == f"{MOD}:feature_enabled"]
        if not features:
            err(f"{name}: missing feature switch condition")
        elif split(recipe["result"]["id"])[1] in registered and feature_of(split(recipe["result"]["id"])[1]) not in features:
            err(f"{name}: gated by {features} but its result belongs to {feature_of(split(recipe['result']['id'])[1])}")

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
            elif split(value)[0] == MOD and split(value)[1] not in all_blocks() + all_items() + machine_blocks():
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
    expected = {name: {"mined": info["mined"], "extras": info.get("extras", [])} for name, info in METALS.items()}
    if declared != expected:
        err(f"JugcraftMaterials.java metals {declared} != tools/materials.py {expected}")

    minerals = re.findall(r'MineralFamily\.register\("([a-z_]+)"\)', source)
    if minerals != list(MINERALS):
        err(f"JugcraftMaterials.java minerals {minerals} != {list(MINERALS)}")
    rocks = re.findall(r'JugcraftRegistry\.block\("([a-z_]+)"', source)
    if rocks != list(ROCKS):
        err(f"JugcraftMaterials.java rocks {rocks} != {list(ROCKS)}")
    items = re.findall(r'JugcraftRegistry\.item\("([a-z_]+)"\)', source)
    if items != list(ITEMS):
        err(f"JugcraftMaterials.java items {items} != {list(ITEMS)}")

    components = (JAVA_ROOT / "materials" / "JugcraftComponents.java").read_text(encoding="utf-8")
    for name, expected_list in (("PLATES", COMPONENTS["plate"]), ("GEARS", COMPONENTS["gear"]),
                                ("WIRES", COMPONENTS["wire"]), ("CIRCUITS", list(CIRCUITS))):
        found = re.findall(r'"([a-z_]+)"', re.search(name + r' = \{([^}]*)\}', components).group(1))
        if found != expected_list:
            err(f"JugcraftComponents.{name} {found} != tools/materials.py {expected_list}")

    features = re.findall(r'"([a-z_]+)"', CONFIG.read_text(encoding="utf-8").split("List.of(")[1].split(");")[0])
    if features != FEATURES:
        err(f"JugcraftConfig.FEATURES {features} != {FEATURES}")

    worldgen = WORLDGEN.read_text(encoding="utf-8")
    placed = sorted(p.stem[4:] for p in (DATA / MOD / "worldgen" / "placed_feature").glob("ore_*.json"))
    in_java = sorted(set(re.findall(r'\{"([a-z_]+)", "[a-z_]+"\}', worldgen)) | set(re.findall(r'add\("([a-z_]+)"', worldgen)))
    if placed != in_java:
        err(f"JugcraftWorldgen adds {in_java}, data defines {placed}")
    for name, feature in re.findall(r'\{"([a-z_]+)", "([a-z_]+)"\}', worldgen) + re.findall(r'add\("([a-z_]+)", "([a-z_]+)"', worldgen):
        owner = feature_of(name if name in ROCKS else f"{name}_ore")
        if feature != owner:
            err(f"JugcraftWorldgen gates {name} by {feature}, expected {owner}")


def check_machines(registered):
    """Audits crusher/arc furnace recipes and keeps Java machine stats in sync with tools/machines.py."""
    generated = load(RES / MOD / "machine_recipes.json") or {}
    if generated != machine_recipes():
        err("jugcraft/machine_recipes.json is stale; run tools/generate_material_data.py")
    for machine, recipes in machine_recipes().items():
        for recipe in recipes:
            inputs = recipe.get("inputs") or [[recipe["input"], 1]]
            label = f"{machine} {' + '.join(ref for ref, _ in inputs)}"
            for ref in [ref for ref, _ in inputs] + [recipe["output"]]:
                if split(ref)[0] == MOD and split(ref)[1] not in registered:
                    err(f"{label}: unknown item {ref}")
            for feature in recipe["features"]:
                if feature not in FEATURES:
                    err(f"{label}: unknown feature {feature}")
            units_in = sum(sum(item_units(ref).values()) * count for ref, count in inputs)
            units_out = sum(item_units(recipe["output"]).values()) * recipe["count"]
            allowed = units_in * (ORE_PROCESSING_MULTIPLIER if recipe.get("ore") else 1)
            if recipe.get("ore") and not all(ref.endswith("_ore") for ref, _ in inputs):
                err(f"{label}: only ores get the ore-processing bonus")
            if units_out > allowed:
                err(f"{label}: creates metal ({units_in} in, {units_out} out, {allowed} allowed)")

    kinds = MACHINE_JAVA.read_text(encoding="utf-8")
    for machine, stats in STATS.items():
        match = re.search(r'\("' + machine + r'", ([\d_]+), ([\d_]+), ([\d_]+), ([\d_]+),', kinds)
        if not match:
            err(f"MachineKind.java has no entry for {machine}")
            continue
        capacity, max_in, max_out, use = (int(v.replace("_", "")) for v in match.groups())
        if capacity != stats["capacity"]:
            err(f"{machine}: capacity {capacity} in Java, {stats['capacity']} in machines.py")
        expected_use = stats.get("use_per_tick", 0)
        if use != expected_use:
            err(f"{machine}: use {use} in Java, {expected_use} in machines.py")
    if set(MACHINES) != set(re.findall(r'\("([a-z_]+)", [\d_]+,', kinds)):
        err("MachineKind.java and tools/machines.py list different machines")


def check_large_machines():
    """Footprints must match MachineKind.footprint(); model elements must stay in Minecraft's -16..32 range."""
    from large_machines import FOOTPRINTS
    kinds = MACHINE_JAVA.read_text(encoding="utf-8")
    for machine, footprint in FOOTPRINTS.items():
        match = re.search(r"case " + machine.upper() + r" -> Footprint\.(tall|of)\((.*?)\);", kinds)
        if not match:
            err(f"MachineKind.footprint() has no case for {machine}")
            continue
        if match.group(1) == "tall":
            java = [(0, y, 0) for y in range(int(match.group(2)))]
        else:
            java = [(0, 0, 0) if part.strip() == "Vec3i.ZERO"
                    else tuple(int(v) for v in re.fullmatch(r"\s*new Vec3i\((-?\d+), (-?\d+), (-?\d+)\)\s*", part).groups())
                    for part in re.split(r",\s*(?=new|Vec3i)", match.group(2))]
        if java != [tuple(offset) for offset in footprint]:
            err(f"{machine}: footprint {java} in Java, {footprint} in large_machines.py")
    from large_machines import POWER_PORTS
    java_ports = {kind.lower(): (int(part), face.lower()) for kind, part, face in
                  re.findall(r"(\w+) \? new PowerPort\((\d+), Direction\.(\w+)\)", kinds)}
    if java_ports != POWER_PORTS:
        err(f"Power ports differ: {java_ports} in MachineKind.java, {POWER_PORTS} in large_machines.py")
    for path in sorted((ASSETS / "models").rglob("*.json")):
        for element in (load(path) or {}).get("elements", []):
            if min(element["from"] + element["to"]) < -16 or max(element["from"] + element["to"]) > 32:
                err(f"{path.name}: element outside -16..32")


def main():
    registered = set(all_blocks()) | set(all_items()) | set(machine_blocks())
    check_assets(sorted(registered))
    check_loot(registered)
    check_recipes(registered)
    check_tags()
    check_worldgen()
    check_java()
    check_machines(registered)
    check_large_machines()
    for path in RES.rglob("*.json"):
        load(path)
    if errors:
        print("\n".join(errors), file=sys.stderr)
        sys.exit(1)
    print(f"PASS: {len(registered)} material IDs, data files and recipe audit. No Minecraft build or game test performed.")


if __name__ == "__main__":
    main()
