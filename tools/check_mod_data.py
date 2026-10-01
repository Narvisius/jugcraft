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

from materials import (MOD, METALS, MINERALS, ROCKS, ITEMS, FEATURES, COMPONENTS, PART_UNITS, CIRCUITS, WASHED_ORES,
                       all_blocks, all_items, feature_of)
import petro
from machines import (MACHINES, STATS, ORE_PROCESSING_MULTIPLIER, ORE_WASHING_MULTIPLIER, BYPRODUCT_SHARE,
                      RENEWABLE_UNITS, WOODS, machine_blocks, machine_items, machine_recipes)

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src" / "main" / "resources"
ASSETS = RES / "assets" / MOD
DATA = RES / "data"
JAVA_ROOT = ROOT / "src" / "main" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft"
JAVA = JAVA_ROOT / "materials" / "JugcraftMaterials.java"
CONFIG = JAVA_ROOT / "config" / "JugcraftConfig.java"
WORLDGEN = JAVA_ROOT / "materials" / "JugcraftWorldgen.java"
MACHINE_JAVA = JAVA_ROOT / "machine" / "MachineKind.java"
STYLE_PACK = RES / "resourcepacks" / "alternate_machines"

# Tags that Jugcraft reads but that vanilla/Fabric API define.
EXTERNAL_TAGS = ({"c:ingots/copper", "c:ingots/iron", "minecraft:stone_ore_replaceables",
                  "minecraft:deepslate_ore_replaceables", "minecraft:planks", "minecraft:mineable/axe",
                  "minecraft:mineable/shovel", "minecraft:leaves"}
                 | {f"minecraft:{tag}" for tag in WOODS.values()})

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
    animated = png.with_name(png.name + ".mcmeta").is_file()
    with Image.open(png) as img:
        # Animated textures are a vertical strip of 16x16 frames with an .mcmeta beside them.
        width, height = img.size
        if animated and not (width == 16 and height % 16 == 0 and height > 16):
            err(f"Animated texture {ref} is {img.size}, expected a 16-wide strip of 16x16 frames")
        elif not animated and img.size != (16, 16):
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


def check_petro():
    """Petroleum fluids: Java registers exactly tools/petro.py's fluids, each with its block, textures and names."""
    java = (JAVA_ROOT / "chemistry" / "PetroFluids.java").read_text(encoding="utf-8")
    declared = re.findall(r'= fluid\("([a-z_]+)", (\d+), (\d+), (\d+)', java)
    expected = [(f, str(i["tick_delay"]), str(i["slope"]), str(i["drop_off"])) for f, i in petro.FLUIDS.items()]
    if declared != expected:
        err(f"PetroFluids.java fluids {declared} != tools/petro.py {expected}")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    for fluid in petro.FLUIDS:
        if f"block.{MOD}.{fluid}" not in lang:
            err(f"Missing name for fluid {fluid}")
        for form in ("still", "flow"):
            texture(rid_of(f"block/{fluid}_{form}"))
            if not (ASSETS / "textures" / "block" / f"{fluid}_{form}.png.mcmeta").is_file():
                err(f"Fluid texture {fluid}_{form} is not animated")
        state = load(ASSETS / "blockstates" / f"{fluid}.json")
        if state:
            model(state["variants"][""]["model"])


def rid_of(path):
    return f"{MOD}:{path}"


def check_loot(registered):
    for path in sorted((DATA / MOD / "loot_table").rglob("*.json")):
        text = path.read_text(encoding="utf-8")
        for name in re.findall(r'"name": "jugcraft:([a-z_]+)"', text):
            if name not in registered:
                err(f"{path.name} drops unknown item {name}")


# Metal content in nugget units. Tags stand for the same forms from any mod.
UNITS = {"ingots": 9, "nuggets": 1, "raw_materials": 9, "ores": 9, "storage_blocks": 81,
         **{f"{form}s": units for form, units in PART_UNITS.items()}}


NON_METAL = {"sawdust"} | set(MINERALS) | set(ITEMS) | set(machine_blocks()) | set(machine_items()) | set(CIRCUITS) | {b for m in MINERALS for b in (f"{m}_ore", f"deepslate_{m}_ore", f"{m}_block")} | {"oil_sand"}


def item_units(ref):
    """Returns {metal: units} for an item or tag reference."""
    ns, path = split(ref.lstrip("#"))
    if ref.startswith("#") and ns == "minecraft":
        return {}  # vanilla tags used here (logs, planks) hold no metal
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
        vanilla = {}
        for metal in ("copper", "iron", "gold"):
            vanilla.update({f"{metal}_ingot": (metal, 9), f"raw_{metal}": (metal, 9), f"{metal}_ore": (metal, 9),
                            f"deepslate_{metal}_ore": (metal, 9), f"{metal}_nugget": (metal, 1)})
        if ns == "minecraft" and path in vanilla:
            metal, units = vanilla[path]
            return {metal: units}
        return {}
    for metal in METALS:
        table = {f"{metal}_ingot": 9, f"{metal}_nugget": 1, f"{metal}_block": 81,
                 f"raw_{metal}": 9, f"raw_{metal}_block": 81, f"{metal}_ore": 9, f"deepslate_{metal}_ore": 9}
        if path in table:
            return {metal: table[path]}
    if path.startswith("washed_") and path.endswith("_ore"):
        return {path[len("washed_"):-len("_ore")]: 9}
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
        if split(result)[0] == MOD and split(result)[1] not in registered:
            err(f"{name}: unknown result {result}")
        count = recipe["result"].get("count", 1)

        total_in = sum(sum(item_units(ref).values()) for ref in inputs)
        out = {metal: units * count for metal, units in item_units(result).items()}
        if sum(out.values()) > total_in:
            err(f"{name}: creates metal ({total_in} units in, {sum(out.values())} out)")
        if any(item_units(ref).keys() == {"bronze"} for ref in inputs) and set(out) - {"bronze"}:
            err(f"{name}: turns bronze back into its ingredients")


def check_machine_recipe_files(registered):
    """Machine recipes are data-driven files under recipe/<type>/; each must resolve and match its type."""
    from generate_material_data import RECIPE_TYPES
    kinds = MACHINE_JAVA.read_text(encoding="utf-8")
    for machine, kind in RECIPE_TYPES.items():
        if f'"{kind}"' not in kinds:
            err(f"MachineKind.recipeType() has no \"{kind}\" (tools say {machine} uses it)")
    expected = sum(len(recipes) for recipes in machine_recipes().values())
    files = sorted(path for path in (DATA / MOD / "recipe").glob("*/*.json") if path.parent.name in RECIPE_TYPES.values())
    if len(files) != expected:
        err(f"{len(files)} machine recipe files, but tools/machines.py defines {expected}")
    for path in files:
        recipe = load(path)
        if recipe is None:
            continue
        label = f"recipe/{path.parent.name}/{path.name}"
        if recipe.get("type") != f"{MOD}:{path.parent.name}":
            err(f"{label}: type {recipe.get('type')} does not match its folder")
        if not recipe.get("fabric:load_conditions"):
            err(f"{label}: missing feature switch condition")
        refs = [part["ingredient"] for part in recipe.get("ingredients", [])] or [recipe.get("ingredient", "")]
        refs += [entry["result"]["id"] for entry in recipe.get("byproducts", [])]
        for ref in refs + [recipe["result"]["id"]]:
            if ref.startswith("#"):
                if not tag_exists("item", ref[1:]):
                    err(f"{label}: unknown tag {ref}")
            elif split(ref)[0] == MOD and split(ref)[1] not in registered:
                err(f"{label}: unknown item {ref}")


def check_fluid_recipes(registered):
    """Fluid recipes (tools/petro.py): every item and fluid resolves, slots and tanks exist, and none makes fluid from
    nothing. Recipes with no item input must not give out more fluid than they take in; recipes with item input must
    say how much fluid they release from it ("source")."""
    from generate_material_data import RECIPE_TYPES
    java = MACHINE_JAVA.read_text(encoding="utf-8")
    fluids = {f"{MOD}:{f}" for f in petro.FLUIDS} | {"minecraft:water", "minecraft:lava"}
    for machine, spec in petro.FLUID_MACHINES.items():
        match = re.search(r"case " + machine.upper() + r" -> new FluidMachineSpec\(List\.of\(([^)]*)\), List\.of\(([^)]*)\), "
                          r"(\d+), (\d+)\)", java)
        if not match:
            err(f"MachineKind.fluidSpec() has no case for {machine}")
        else:
            def tanks(text):
                return [CONSTANTS.get(v.strip(), None) or int(v.strip().replace("_", "")) for v in text.split(",") if v.strip()]
            CONSTANTS = {name: int(value.replace("_", "")) for name, value in
                         re.findall(r"public static final int (\w+) = ([\d_]+);", java)}
            found = (tanks(match.group(1)), tanks(match.group(2)), int(match.group(3)), int(match.group(4)))
            if found != (spec["inputs"], spec["outputs"], spec["item_inputs"], spec["item_outputs"]):
                err(f"{machine}: fluid spec {found} in Java, {spec} in tools/petro.py")
        if spec["recipe_type"] is None:
            continue
        if f'"{spec["recipe_type"]}"' not in java:
            err(f"MachineKind.recipeType() has no \"{spec['recipe_type']}\" for {machine}")
        if spec["recipe_type"] in RECIPE_TYPES.values():
            err(f"Fluid recipe type {spec['recipe_type']} is also an item machine's")
    expected = sum(len(r) for r in petro.FLUID_RECIPES.values())
    types = {spec["recipe_type"] for spec in petro.FLUID_MACHINES.values() if spec["recipe_type"]}
    files = [p for p in (DATA / MOD / "recipe").glob("*/*.json") if p.parent.name in types]
    if len(files) != expected:
        err(f"{len(files)} fluid recipe files, but tools/petro.py defines {expected}")
    for machine, recipes in petro.FLUID_RECIPES.items():
        spec = petro.FLUID_MACHINES[machine]
        for recipe in recipes:
            label = f"{machine} recipe {recipe['name']}"
            if len(recipe.get("items", [])) > spec["item_inputs"] or len(recipe.get("results", [])) > spec["item_outputs"]:
                err(f"{label}: more items than the machine has slots")
            if len(recipe.get("fluids", [])) > len(spec["inputs"]) or len(recipe.get("fluid_results", [])) > len(spec["outputs"]):
                err(f"{label}: more fluids than the machine has tanks")
            for i, (fluid, mb) in enumerate(recipe.get("fluids", [])):
                if fluid not in fluids:
                    err(f"{label}: unknown fluid {fluid}")
                if mb > spec["inputs"][i]:
                    err(f"{label}: needs {mb} mB of {fluid} but its tank holds {spec['inputs'][i]}")
            for i, (fluid, mb) in enumerate(recipe.get("fluid_results", [])):
                if fluid not in fluids:
                    err(f"{label}: unknown fluid {fluid}")
                if mb > spec["outputs"][i]:
                    err(f"{label}: makes {mb} mB of {fluid} but its tank holds {spec['outputs'][i]}")
            for ref, _ in recipe.get("items", []) + recipe.get("results", []):
                if ref.startswith("#"):
                    if not tag_exists("item", ref[1:]):
                        err(f"{label}: unknown tag {ref}")
                elif split(ref)[0] == MOD and split(ref)[1] not in registered:
                    err(f"{label}: unknown item {ref}")
            fluid_in = sum(mb for _, mb in recipe.get("fluids", [])) + recipe.get("source", 0)
            fluid_out = sum(mb for _, mb in recipe.get("fluid_results", []))
            if recipe.get("items") and "source" not in recipe and fluid_out > sum(mb for _, mb in recipe.get("fluids", [])):
                err(f"{label}: makes fluid from items without saying how much (\"source\")")
            if fluid_out > fluid_in:
                err(f"{label}: {fluid_out} mB out from {fluid_in} mB in")


def check_tags():
    for path in sorted(DATA.rglob("tags/*/**/*.json")):
        registry = path.relative_to(DATA).parts[2]
        for value in (load(path) or {}).get("values", []):
            if value.startswith("#"):
                if not tag_exists(registry, value[1:]):
                    err(f"{path.relative_to(ROOT)}: unknown tag {value}")
            elif registry == "fluid":
                if split(value)[1] not in [f for fluid in petro.FLUIDS for f in (fluid, f"flowing_{fluid}")]:
                    err(f"{path.relative_to(ROOT)}: unknown fluid {value}")
            elif split(value)[0] == MOD and split(value)[1] not in all_blocks() + all_items() + machine_blocks():
                err(f"{path.relative_to(ROOT)}: unknown entry {value}")


def check_worldgen():
    if (DATA / MOD / "worldgen" / "configured_feature").exists():
        err("worldgen/configured_feature/ is the pre-26.x layout; Minecraft 26.3 reads worldgen/feature/")
    for path in sorted((DATA / MOD / "worldgen" / "feature").glob("*.json")):
        feature = load(path) or {}
        if "config" in feature:
            err(f"{path.name}: 26.x features have no \"config\" wrapper")
        for target in feature.get("targets", []):
            block = split(target["state"])[1]
            if block not in all_blocks():
                err(f"{path.name}: places unknown block {block}")
    for path in sorted((DATA / MOD / "worldgen" / "placed_feature").glob("*.json")):
        feature = split((load(path) or {})["feature"])[1]
        if not (DATA / MOD / "worldgen" / "feature" / f"{feature}.json").is_file():
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
                                ("WIRES", COMPONENTS["wire"]), ("CIRCUITS", list(CIRCUITS)),
                                ("DUSTS", COMPONENTS["dust"]), ("WASHED_ORES", WASHED_ORES)):
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
    """Audits machine recipes for metal and keeps Java machine stats in sync with tools/machines.py."""
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
            bonus = recipe.get("ore_bonus") or (ORE_PROCESSING_MULTIPLIER if recipe.get("ore") else 1)
            allowed = units_in * bonus
            is_ore = all(ref.endswith("_ore") and not split(ref)[1].startswith("washed_") for ref, _ in inputs)
            if bonus > 1 and not is_ore:
                err(f"{label}: only ores get the ore-processing bonus")
            if bonus > ORE_WASHING_MULTIPLIER:
                err(f"{label}: ore bonus {bonus} exceeds the washing route's {ORE_WASHING_MULTIPLIER}")
            if units_out > allowed:
                err(f"{label}: creates metal ({units_in} in, {units_out} out, {allowed} allowed)")
            # Byproducts: [item, count, chance, feature]. Expected metal stays a small share of the input,
            # or, for renewable recipes (sieve), under a nugget per operation.
            extra = 0.0
            for item, count, chance, feature in recipe.get("byproducts", []):
                if split(item)[0] == MOD and split(item)[1] not in registered:
                    err(f"{label}: unknown byproduct {item}")
                if not 0 < chance <= 1:
                    err(f"{label}: byproduct chance {chance} outside (0, 1]")
                if feature is not None and feature not in FEATURES:
                    err(f"{label}: byproduct gated by unknown feature {feature}")
                extra += sum(item_units(item).values()) * count * chance
            limit = RENEWABLE_UNITS if recipe.get("renewable") else units_in * BYPRODUCT_SHARE
            if extra > limit + 1e-9:
                err(f"{label}: byproducts add {extra:.2f} metal units on average, {limit:.2f} allowed")
            if recipe.get("renewable") and units_in:
                err(f"{label}: renewable recipes must not consume metal")

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
        match = re.search(r"case " + machine.upper() + r" -> Footprint\.(tall|of|cuboid)\((.*?)\);", kinds, re.S)
        if not match:
            err(f"MachineKind.footprint() has no case for {machine}")
            continue
        if match.group(1) == "tall":
            java = [(0, y, 0) for y in range(int(match.group(2)))]
        elif match.group(1) == "cuboid":
            from large_machines import cuboid
            java = cuboid(*(int(v) for v in match.group(2).split(",")))
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
    for path in sorted((ASSETS / "models").rglob("*.json")) + sorted(STYLE_PACK.rglob("models/**/*.json")):
        for element in (load(path) or {}).get("elements", []):
            if min(element["from"] + element["to"]) < -16 or max(element["from"] + element["to"]) > 32:
                err(f"{path.name}: element outside -16..32")


def check_handbook(registered):
    """Every Jugcraft item the Engineer's Handbook shows must exist."""
    book = load(ASSETS / "handbook" / "en_us.json")
    if book is None:
        err("assets/jugcraft/handbook/en_us.json is missing")
        return
    refs = []
    for chapter in book["chapters"]:
        refs.append(chapter["icon"])
        for page in chapter["pages"]:
            refs.append(page["icon"])
            craft = page.get("craft")
            if craft:
                refs += [ref for ref in craft["grid"] if ref] + [craft["result"]]
            for row in page.get("recipes", []):
                refs += [ref for ref, _ in row["in"]] + [row["out"][0]] + [ref for ref, _ in row.get("extra", [])]
    for ref in refs:
        if split(ref)[0] == MOD and split(ref)[1] not in registered:
            err(f"handbook: unknown item {ref}")


def check_advancements(registered):
    """Every advancement names real Jugcraft items, a translated title and a parent that exists."""
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    folder = DATA / MOD / "advancement"
    names = {path.stem for path in folder.glob("*.json")}
    for path in sorted(folder.glob("*.json")):
        data = load(path)
        text = path.read_text(encoding="utf-8")
        for name in re.findall(r'"jugcraft:([a-z_]+)"', text):
            if name not in registered and name not in names and not name.startswith("block/"):
                err(f"advancement {path.stem}: unknown item jugcraft:{name}")
        if data["display"]["title"]["translate"] not in lang:
            err(f"advancement {path.stem}: untranslated title")
        parent = data.get("parent")
        if parent and split(parent)[1] not in names:
            err(f"advancement {path.stem}: missing parent {parent}")


def check_style_pack():
    """Both machine styles must cover every machine completely, and everything they reference must exist."""
    from machines import PARTS, FLUID_BLOCKS
    pack_assets = STYLE_PACK / "assets" / MOD
    if not (STYLE_PACK / "pack.mcmeta").is_file():
        err("resourcepacks/alternate_machines has no pack.mcmeta")

    def resolve(ref, root):
        ns, path = split(ref)
        if ns == "minecraft":
            return
        file = root / "models" / f"{path}.json"
        data = load(file if file.is_file() else ASSETS / "models" / f"{path}.json")
        if data is None:
            return
        for value in data.get("textures", {}).values():
            if not value.startswith("#"):
                texture(value)
        if data.get("parent", "").startswith(MOD + ":"):
            resolve(data["parent"], root)

    for block in list(MACHINES) + list(PARTS) + list(FLUID_BLOCKS):
        keys = {}
        for root, label in ((ASSETS, "default style"), (pack_assets, "alternate style pack")):
            path = root / "blockstates" / f"{block}.json"
            if not path.is_file():
                err(f"The {label} has no blockstate for {block}")
                continue
            variants = (load(path) or {}).get("variants", {})
            keys[label] = set(variants)
            for variant in variants.values():
                resolve(variant["model"], root)
            item = root / "items" / f"{block}.json"
            if not item.is_file():
                err(f"The {label} has no item model for {block}")
            else:
                resolve((load(item) or {})["model"]["model"], root)
        if len(keys) == 2 and len(set(map(frozenset, keys.values()))) != 1:
            err(f"{block}: the two styles cover different block states")


def main():
    registered = (set(all_blocks()) | set(all_items()) | set(machine_blocks()) | set(machine_items())
                  | set(petro.petro_items()))
    check_assets(sorted(registered))
    check_petro()
    check_loot(registered)
    check_recipes(registered)
    check_machine_recipe_files(registered)
    check_fluid_recipes(registered)
    check_tags()
    check_worldgen()
    check_java()
    check_machines(registered)
    check_large_machines()
    check_style_pack()
    check_handbook(registered)
    check_advancements(registered)
    for path in RES.rglob("*.json"):
        load(path)
    if errors:
        print("\n".join(errors), file=sys.stderr)
        sys.exit(1)
    print(f"PASS: {len(registered)} material IDs, data files and recipe audit. No Minecraft build or game test performed.")


if __name__ == "__main__":
    main()
