"""Consolidate walnut supply while keeping every recovered registry ID loadable.

Old blocks keep their states and inventories. Their drops and recipes converge on
the approved timber; extra furniture/boat IDs remain canonical for those forms.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"
ASSET = RES / "assets/jugcraft"
DATA = RES / "data/jugcraft"
FORMS = ("walnut_log", "walnut_wood", "stripped_walnut_log", "stripped_walnut_wood",
         "walnut_planks", "walnut_stairs", "walnut_slab", "walnut_fence", "walnut_fence_gate")
ALIASES = {"jugcraft:ecology_"+name: "jugcraft:"+name for name in FORMS}


def read(path):
    return json.loads(path.read_text(encoding="utf-8"))


def transform(value, mapping):
    if isinstance(value, str): return mapping.get(value, value)
    if isinstance(value, list): return [transform(v, mapping) for v in value]
    if isinstance(value, dict): return {k: transform(v, mapping) for k, v in value.items()}
    return value


def reconcile(write):
    # Shared ingredients let existing inventories pay for all new and recovered forms.
    ingredients = {}
    for old, new in ALIASES.items():
        name = new.split(":")[1]
        ingredients[old] = ingredients[new] = "#jugcraft:walnut_compat/"+name
        write(DATA/f"tags/item/walnut_compat/{name}.json", {"values": [old, new]})
        # Only loot item names change; block predicates must still match the saved ID.
        path = DATA/f"loot_table/blocks/ecology_{name}.json"
        loot = read(path)
        def drops(v):
            if isinstance(v, list): return [drops(x) for x in v]
            if isinstance(v, dict):
                return {k: ALIASES.get(x, x) if k == "name" and isinstance(x, str) else drops(x) for k, x in v.items()}
            return v
        write(path, drops(loot))
    paths = list((DATA/"recipe/ecology").rglob("*.json"))
    paths += [DATA/f"recipe/{form}.json" for form in FORMS if (DATA/f"recipe/{form}.json").exists()]
    def ingredient(value):
        if isinstance(value, list):
            # Native ingredient alternatives are item IDs; tags cannot be nested
            # inside that list. Expand only these exact two-member aliases.
            expanded = []
            for member in value:
                ref = ingredients.get(member, member)
                if ref.startswith("#jugcraft:walnut_compat/"):
                    form = ref.rsplit("/", 1)[1]
                    expanded.extend(("jugcraft:"+form, "jugcraft:ecology_"+form))
                else:
                    expanded.append(ref)
            return list(dict.fromkeys(expanded))
        return transform(value, ingredients)
    for path in paths:
        if "walnut" not in path.name: continue
        recipe = read(path)
        if "ingredients" in recipe: recipe["ingredients"] = [ingredient(v) for v in recipe["ingredients"]]
        if "key" in recipe: recipe["key"] = {k: ingredient(v) for k, v in recipe["key"].items()}
        for field in ("ingredient", "input"):
            if field in recipe: recipe[field] = ingredient(recipe[field])
        for field in ("result", "results"):
            if field in recipe: recipe[field] = transform(recipe[field], ALIASES)
        # Timber and furniture stay craftable wherever walnut grows.
        if "ecology" in path.parts:
            recipe["fabric:load_conditions"] = [{"condition": "jugcraft:feature_enabled", "feature": "biomes", "or": ["agriculture"]}]
        write(path, recipe)
    for name in ("walnut_logs", "ecology_walnut_logs"):
        for registry in ("block", "item"):
            path = DATA/f"tags/{registry}/{name}.json"
            tag = read(path) if path.exists() else {"values": []}
            for form in FORMS[:4]:
                for prefix in ("", "ecology_"):
                    value = "jugcraft:"+prefix+form
                    if value not in tag["values"]: tag["values"].append(value)
            write(path, tag)
    # Existing wooden blocks retain vanilla crafting, mining and decay behavior.
    tag_aliases = ALIASES | {"jugcraft:ecology_"+name: "jugcraft:"+name
                             for name in ("walnut_leaves", "walnut_sapling")}
    for registry in ("block", "item"):
        for path in (RES / f"data/minecraft/tags/{registry}").glob("*.json"):
            tag = read(path)
            values = tag.get("values", [])
            added = [old for old, new in tag_aliases.items() if new in values and old not in values]
            if added:
                tag["values"] += added
                write(path, tag)
    # Legacy saplings, potted plants and foliage now show the approved family.
    for name in ("walnut_leaves", "walnut_sapling"):
        write(ASSET/f"blockstates/ecology_{name}.json", read(ASSET/f"blockstates/{name}.json"))
        write(ASSET/f"items/ecology_{name}.json", read(ASSET/f"items/{name}.json"))
        loot = read(DATA/f"loot_table/blocks/{name}.json")
        # Match the legacy block while yielding the canonical sapling/leaves.
        def predicate(v):
            if isinstance(v, list): return [predicate(x) for x in v]
            if isinstance(v, dict):
                return {k: "jugcraft:ecology_"+name if k == "blocks" and x == "jugcraft:"+name else predicate(x) for k, x in v.items()}
            return v
        write(DATA/f"loot_table/blocks/ecology_{name}.json", predicate(loot))
    # Core block models and potted saplings use the exact approved shared textures.
    textures = {"jugcraft:menagerie/ecology/block/"+name: "jugcraft:block/"+name for name in (
        "walnut_log", "walnut_log_top", "stripped_walnut_log", "stripped_walnut_log_top",
        "walnut_planks", "walnut_sapling", "walnut_leaves")}
    for path in (ASSET/"models/menagerie/ecology").rglob("*.json"):
        if "walnut" in path.name: write(path, transform(read(path), textures))
    write(DATA/"recipe/walnut_husk_dye.json", {"type": "minecraft:crafting_shapeless",
        "fabric:load_conditions": [{"condition": "jugcraft:feature_enabled", "feature": "biomes", "or": ["agriculture"]}],
        "ingredients": ["jugcraft:walnut_husk"], "result": {"id": "minecraft:brown_dye"}})
    from walnut_furniture import write_all
    write_all()


if __name__ == "__main__":
    from generate_material_data import write
    reconcile(write)
