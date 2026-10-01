"""Regenerate Jugcraft's material JSON resources from tools/materials.py.

Run from the repository root:  python3 tools/generate_material_data.py
The output is deterministic; CI re-runs it and fails if anything changes.
"""
import json
import shutil
from pathlib import Path

from materials import (MOD, METALS, MINERALS, ROCKS, ITEMS, EXTRA_NAMES, MINERAL_TAGS, PROCESSING, COMPONENTS, CIRCUITS,
                       metal_blocks, metal_items, mineral_blocks, all_blocks, all_items, feature_of, ingot_id)

from machines import MACHINES, PARTS, CABLES, PIPES, FLUID_BLOCKS, ITEM_PIPES, LOGISTICS_BLOCKS, STORAGE_BLOCKS, KINETIC_BLOCKS, TOOLS, UPGRADES, POWERED_TOOLS, TOOL_BLOCKS, UPGRADE_MODULES, SLOPE_BLOCKS, CRAFTING, FEATURE as MACHINE_FEATURE, machine_blocks, machine_recipes
import model_writer

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src" / "main" / "resources"
ASSETS = RES / "assets" / MOD
DATA = RES / "data"
PACKS = RES / "resourcepacks"

GENERATED_DIRS = [
    DATA / MOD / "advancement", ASSETS / "blockstates", ASSETS / "items", ASSETS / "models", ASSETS / "lang", ASSETS / "handbook",
    DATA / MOD / "loot_table", DATA / MOD / "recipe", DATA / MOD / "worldgen",
    DATA / "c" / "tags", DATA / "minecraft" / "tags", RES / MOD, PACKS,
]

CABLE_ROTATION = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270},
                  "up": {"x": 270}, "down": {"x": 90}}


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
    if item in CIRCUITS:
        return CIRCUITS[item]
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
    machine_assets(lang)
    import advancements
    lang.update(advancements.generate(MOD)[1])
    write(ASSETS / "lang" / "en_us.json", dict(sorted(lang.items())))


def write_alternate_pack(lang):
    """resourcepacks/alternate_machines: the non-default style, switchable in Options > Resource Packs."""
    style = model_writer.alternate_style()
    pack = PACKS / model_writer.PACK_ID
    model_writer.WRITERS[style](pack / "assets" / MOD, MACHINES, PARTS, FLUID_BLOCKS)
    write(pack / "pack.mcmeta", model_writer.pack_metadata(style))
    lang[f"pack.{MOD}.{model_writer.PACK_ID}"] = f"Jugcraft: {model_writer.STYLE_NAMES[style]}"


def machine_assets(lang):
    """Machine models in the default style, plus the other style as a built-in resource pack."""
    for machine, info in MACHINES.items():
        lang[f"block.{MOD}.{machine}"] = info["display"]
        lang[f"container.{MOD}.{machine}"] = info["display"]
    for part, display in PARTS.items():
        lang[f"block.{MOD}.{part}"] = display
    for block, info in FLUID_BLOCKS.items():
        lang[f"block.{MOD}.{block}"] = info["display"]
    model_writer.WRITERS[model_writer.DEFAULT_STYLE](ASSETS, MACHINES, PARTS, FLUID_BLOCKS)
    write_alternate_pack(lang)

    for cable, info in {**CABLES, **PIPES, **ITEM_PIPES}.items():
        lang[f"block.{MOD}.{cable}"] = info["display"]
        texture = rid(f"block/{cable}")
        # Transmitters are `size` pixels thick (4 for cables and fluid pipes, 6 for item pipes).
        lo = 8 - info.get("size", 4) // 2
        hi = 16 - lo
        write(ASSETS / "models" / "block" / f"{cable}_core.json", {
            "textures": {"cable": texture, "particle": texture},
            "elements": [{"from": [lo, lo, lo], "to": [hi, hi, hi], "faces": {
                face: {"uv": [lo, lo, hi, hi], "texture": "#cable"}
                for face in ("north", "east", "south", "west", "up", "down")}}],
        })
        write(ASSETS / "models" / "block" / f"{cable}_arm.json", {
            "textures": {"cable": texture, "particle": texture},
            "elements": [{"from": [lo, lo, 0], "to": [hi, hi, lo], "faces": {
                "north": {"uv": [lo, lo, hi, hi], "texture": "#cable"},
                "east": {"uv": [0, lo, lo, hi], "texture": "#cable"},
                "west": {"uv": [0, lo, lo, hi], "texture": "#cable"},
                "up": {"uv": [lo, 0, hi, lo], "texture": "#cable"},
                "down": {"uv": [lo, 0, hi, lo], "texture": "#cable"},
            }}],
        })
        parts = [{"apply": {"model": rid(f"block/{cable}_core")}}]
        for direction, rotation in CABLE_ROTATION.items():
            parts.append({"when": {direction: "true"}, "apply": {"model": rid(f"block/{cable}_arm"), **rotation}})
        write(ASSETS / "blockstates" / f"{cable}.json", {"multipart": parts})
        # A 3D straight segment in hand and inventory, like other tech mods' transmitters.
        write(ASSETS / "models" / "item" / f"{cable}.json", {
            "parent": "minecraft:block/block",
            "textures": {"cable": texture, "particle": texture},
            "elements": [{"from": [lo, lo, 0], "to": [hi, hi, 16], "faces": {
                "north": {"uv": [lo, lo, hi, hi], "texture": "#cable"},
                "south": {"uv": [lo, lo, hi, hi], "texture": "#cable"},
                "east": {"uv": [0, lo, 16, hi], "texture": "#cable"},
                "west": {"uv": [0, lo, 16, hi], "texture": "#cable"},
                "up": {"uv": [lo, 0, hi, 16], "texture": "#cable"},
                "down": {"uv": [lo, 0, hi, 16], "texture": "#cable"},
            }}],
        })
        write(ASSETS / "items" / f"{cable}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{cable}")}})

    # Item logistics blocks: one model each (same in both styles), turned to all six directions.
    from logistics_models import MODELS as LOGISTICS_MODELS, FACING_ROTATION
    for block, info in LOGISTICS_BLOCKS.items():
        lang[f"block.{MOD}.{block}"] = info["display"]
        elements = LOGISTICS_MODELS[block]
        textures = {name: rid(f"block/{name}") for name in model_writer.texture_names(elements)}
        textures["particle"] = rid("block/sp_brass")
        write(ASSETS / "models" / "block" / f"{block}.json", {
            "parent": "minecraft:block/block", "textures": textures,
            "elements": model_writer.slice_model(block, elements, [(0, 0, 0)])[0]})
        write(ASSETS / "blockstates" / f"{block}.json", {"variants": {
            f"facing={facing}": {"model": rid(f"block/{block}"), **rotation} for facing, rotation in FACING_ROTATION.items()}})
        write(ASSETS / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{block}")}})
    # Storage blocks: one model each, no rotation.
    from storage_models import MODELS as STORAGE_MODELS
    for block, info in STORAGE_BLOCKS.items():
        lang[f"block.{MOD}.{block}"] = info["display"]
        elements = STORAGE_MODELS[block]
        textures = {name: rid(f"block/{name}") for name in model_writer.texture_names(elements)}
        textures["particle"] = rid("block/sp_wood")
        write(ASSETS / "models" / "block" / f"{block}.json", {
            "parent": "minecraft:block/block", "textures": textures,
            "elements": model_writer.slice_model(block, elements, [(0, 0, 0)])[0]})
        write(ASSETS / "blockstates" / f"{block}.json", {"variants": {"": {"model": rid(f"block/{block}")}}})
        write(ASSETS / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{block}")}})
    # Kinetic blocks: one model each, a turning or lit variant where they have one, and rotations.
    import kinetic_models
    import kinetic_rotors
    for block, info in KINETIC_BLOCKS.items():
        lang[f"block.{MOD}.{block}"] = info["display"]
        elements = kinetic_models.MODELS[block]
        textures = {name: rid(f"block/{name}") for name in model_writer.texture_names(elements)}
        textures["particle"] = rid("block/sp_iron")
        write(ASSETS / "models" / "block" / f"{block}.json", {
            "parent": "minecraft:block/block", "textures": textures,
            "elements": model_writer.slice_model(block, elements, [(0, 0, 0)])[0]})
        swap = kinetic_models.TURNING.get(block) or kinetic_models.LIT.get(block)
        if block in kinetic_models.ROTORS:
            # Spinning: only the static parts; client/KineticRotorRenderer draws the rotor.
            static = kinetic_models.STATIC[block]
            active_textures = {name: rid(f"block/{name}") for name in model_writer.texture_names(static)}
            active_textures["particle"] = rid("block/sp_iron")
            if swap:
                active_textures[swap[0]] = rid(f"block/{swap[1]}")
            write(ASSETS / "models" / "block" / f"{block}_active.json", {
                "parent": "minecraft:block/block", "textures": active_textures,
                "elements": model_writer.slice_model(block, static, [(0, 0, 0)])[0] if static else []})
            swap = True
        elif swap:
            write(ASSETS / "models" / "block" / f"{block}_active.json", {
                "parent": rid(f"block/{block}"), "textures": {swap[0]: rid(f"block/{swap[1]}")}})
        # The block state property the variants depend on (the Java blocks define the same ones).
        prop = "lit" if block in kinetic_models.LIT else "turning" if block in kinetic_models.STATES_TURNING else None

        def model(active):
            return rid(f"block/{block}_active") if active and swap else rid(f"block/{block}")
        variants = {}
        for active in ((False, True) if prop else (None,)):
            suffix = f"{prop}={str(active).lower()}" if prop else ""
            if info["states"] == "axis":
                for axis, rotation in (("x", {"y": 90}), ("y", {"x": 90}), ("z", {})):
                    variants[f"axis={axis}," + suffix] = {"model": model(active), **rotation}
            elif info["states"] in ("facing", "horizontal"):
                for facing, rotation in FACING_ROTATION.items():
                    if info["states"] == "horizontal" and facing in ("up", "down"):
                        continue
                    variants[f"facing={facing}," + suffix if suffix else f"facing={facing}"] = {"model": model(active), **rotation}
            else:
                variants[suffix] = {"model": model(active)}
        write(ASSETS / "blockstates" / f"{block}.json", {"variants": {k.rstrip(","): v for k, v in variants.items()}})
        write(ASSETS / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{block}")}})
    write(ASSETS / "kinetic_rotors.json", kinetic_rotors.export(KINETIC_BLOCKS))
    # Conveyor slopes: an ascending and a descending model, each with a moving-belt version, turned to face the way
    # items travel.
    for block, info in SLOPE_BLOCKS.items():
        lang[f"block.{MOD}.{block}"] = info["display"]
        variants = {}
        for ascending, name in ((True, block), (False, f"{block}_down")):
            elements = kinetic_models.SLOPES[name]
            textures = {tex: rid(f"block/{tex}") for tex in model_writer.texture_names(elements)}
            textures["particle"] = rid("block/sp_iron")
            write(ASSETS / "models" / "block" / f"{name}.json", {
                "parent": "minecraft:block/block", "textures": textures,
                "elements": model_writer.slice_model(name, elements, [(0, 0, 0)])[0]})
            write(ASSETS / "models" / "block" / f"{name}_active.json", {
                "parent": rid(f"block/{name}"), "textures": {"conveyor_belt": rid("block/conveyor_belt_moving")}})
            for facing, rotation in FACING_ROTATION.items():
                if facing in ("up", "down"):
                    continue
                for turning in (False, True):
                    model = rid(f"block/{name}_active" if turning else f"block/{name}")
                    key = f"ascending={str(ascending).lower()},facing={facing},turning={str(turning).lower()}"
                    variants[key] = {"model": model, **rotation}
        write(ASSETS / "blockstates" / f"{block}.json", {"variants": variants})
        write(ASSETS / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{block}")}})
    lang[f"message.{MOD}.conveyor_slope"] = "Conveyor slope: %s"
    lang[f"message.{MOD}.conveyor_slope.up"] = "up"
    lang[f"message.{MOD}.conveyor_slope.down"] = "down"
    powered_tools(lang)
    petro_assets(lang)
    lang[f"message.{MOD}.hand_crank"] = "Turning for %s more seconds"
    lang[f"message.{MOD}.steam_engine"] = "Steam engine: %s fuel, %s / %s mB water"
    lang[f"message.{MOD}.dynamo"] = "Dynamo: %s / %s JE"
    lang[f"message.{MOD}.electric_motor"] = "Electric motor: %s / %s JE"
    lang[f"message.{MOD}.belt.first"] = "Now use the belt on the second pulley"
    lang[f"message.{MOD}.belt.linked"] = "Belt fitted"
    lang[f"message.{MOD}.belt.same"] = "Pick a different pulley"
    lang[f"message.{MOD}.belt.not_pulley"] = "Both ends of a belt need a belt pulley"
    lang[f"message.{MOD}.belt.taken"] = "That pulley already has a belt"
    lang[f"message.{MOD}.belt.axis"] = "The pulleys must share an axis and be level with each other along it"
    lang[f"message.{MOD}.belt.far"] = "Too far: a belt reaches 16 blocks"
    lang[f"message.{MOD}.crate"] = "%s × %s (holds up to %s)"
    lang[f"message.{MOD}.crate.empty"] = "Empty crate: holds %s stacks of one item"
    for tool, display in TOOLS.items():
        lang[f"item.{MOD}.{tool}"] = display
        write(ASSETS / "models" / "item" / f"{tool}.json",
              {"parent": "minecraft:item/handheld", "textures": {"layer0": rid(f"item/{tool}")}})
        write(ASSETS / "items" / f"{tool}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{tool}")}})

    for upgrade, display in UPGRADES.items():
        lang[f"item.{MOD}.{upgrade}"] = display
        write(ASSETS / "models" / "item" / f"{upgrade}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{upgrade}")}})
        write(ASSETS / "items" / f"{upgrade}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{upgrade}")}})
    lang[f"tooltip.{MOD}.speed_upgrade"] = "Each: faster, uses more energy per item (up to 4 count)"
    lang[f"tooltip.{MOD}.efficiency_upgrade"] = "Each: 20% less energy (up to 4 count)"
    lang[f"container.{MOD}.tank.empty"] = "Empty"
    lang[f"prospector.{MOD}.oil"] = "Oil"
    lang[f"container.{MOD}.pumpjack.oil"] = "Pumping oil"
    lang[f"container.{MOD}.pumpjack.dry"] = "No pumpable oil here"
    lang[f"prospector.{MOD}.shale_oil"] = "Shale oil"
    lang[f"container.{MOD}.redstone"] = "Redstone: %s"
    lang[f"container.{MOD}.redstone.ignored"] = "ignored (always runs)"
    lang[f"container.{MOD}.redstone.high"] = "runs only with a signal"
    lang[f"container.{MOD}.redstone.low"] = "runs only without a signal"
    lang[f"container.{MOD}.upgrades"] = "Upgrades: %s× speed, %s energy per tick"
    lang[f"tooltip.{MOD}.energy"] = "%s / %s JE"
    lang[f"message.{MOD}.tank"] = "%s: %s / %s mB"
    lang[f"message.{MOD}.tank.empty"] = "Empty (0 / %s mB)"
    lang[f"message.{MOD}.pump"] = "Energy %s / %s JE, holding %s mB"
    lang[f"container.{MOD}.arc_furnace.incomplete"] = "Structure incomplete"
    lang[f"container.{MOD}.arc_furnace.formed"] = "Arc furnace formed"
    lang[f"container.{MOD}.item_sorter"] = "Item Sorter"
    lang[f"message.{MOD}.wrench.large"] = "Multi-block machines can't be turned; sneak to dismantle"
    lang[f"container.{MOD}.side"] = "%s: %s"
    for face, name in (("front", "Front"), ("back", "Back"), ("left", "Left"), ("right", "Right"),
                       ("top", "Top"), ("bottom", "Bottom")):
        lang[f"container.{MOD}.side.{face}"] = name
    for mode, name in (("input", "input"), ("output", "output"), ("both", "input and output"), ("none", "closed")):
        lang[f"container.{MOD}.mode.{mode}"] = name
    lang[f"container.{MOD}.eject"] = "Eject"
    lang[f"container.{MOD}.eject.on"] = "Eject: on"
    lang[f"container.{MOD}.eject.off"] = "Eject: off"
    lang[f"container.{MOD}.eject.tooltip"] = "Push results out of output faces into pipes and inventories"
    lang[f"container.{MOD}.wind_turbine.clear"] = "Rotor turning"
    lang[f"container.{MOD}.wind_turbine.blocked"] = "Rotor blocked: clear the blocks beside and above the top"
    lang[f"container.{MOD}.water_wheel.turning"] = "Wheel turning"
    lang[f"container.{MOD}.water_wheel.still"] = "Needs flowing water on its right side"


# Machine recipe types (Java: machine/MachineRecipes.java). Each machine's list in tools/machines.py
# becomes data/jugcraft/recipe/<type>/<name>.json, so data packs can add, replace or remove them.
RECIPE_TYPES = {"crusher": "crushing", "arc_furnace": "arc_smelting", "alloy_smelter": "alloying",
                "metal_press": "pressing", "wire_drawer": "wire_drawing", "circuit_assembler": "circuit_assembly",
                "pulverizer": "pulverizing", "ore_washer": "ore_washing", "sieve": "sifting", "sawmill": "sawing",
                "coke_oven": "coking", "steel_foundry": "steelmaking",
                "tree_farm": "tree_growing"}


def machine_recipe_files(out):
    for machine, recipes in machine_recipes().items():
        kind = RECIPE_TYPES[machine]
        names = set()
        for recipe in recipes:
            data = {"fabric:load_conditions": [c for f in recipe["features"] for c in condition(f)],
                    "type": rid(kind)}
            if "inputs" in recipe:
                name = recipe["output"].split(":")[1]
                data["ingredients"] = [{"ingredient": item, "count": count} for item, count in recipe["inputs"]]
            else:
                name = recipe["input"].split(":")[1]
                data["ingredient"] = recipe["input"]
            data["result"] = {"id": recipe["output"], "count": recipe["count"]}
            data["time"] = recipe["ticks"]
            if recipe.get("byproducts"):
                data["byproducts"] = []
                for item, count, chance, feature in recipe["byproducts"]:
                    entry = {"result": {"id": item, "count": count}, "chance": chance}
                    if feature:
                        entry["feature"] = feature
                    data["byproducts"].append(entry)
            if name in names:
                raise ValueError(f"Two {kind} recipes would both be named {name}")
            names.add(name)
            write(out / kind / f"{name}.json", data)


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


def powered_tools(lang):
    """Dieselpunk powered tools (3D item models) and the 2-tall charging station (tools/tool_models.py)."""
    import tool_models
    for item, display in POWERED_TOOLS.items():
        lang[f"item.{MOD}.{item}"] = display
        elements = tool_models.ITEMS[item]
        textures = {name: rid(f"block/{name}") for name in model_writer.texture_names(elements)}
        textures["particle"] = rid("block/dp_olive")
        write(ASSETS / "models" / "item" / f"{item}.json", {
            "textures": textures, "elements": model_writer.slice_model(item, elements, [(0, 0, 0)])[0],
            "display": tool_models.DISPLAY[item]})
        write(ASSETS / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{item}")}})
    for block, info in TOOL_BLOCKS.items():
        lang[f"block.{MOD}.{block}"] = info["display"]
        elements = tool_models.BLOCKS[block]
        textures = {name: rid(f"block/{name}") for name in model_writer.texture_names(elements)}
        textures["particle"] = rid("block/dp_olive")
        halves = model_writer.slice_model(block, elements, [(0, 0, 0), (0, 1, 0)])
        lit_from, lit_to = tool_models.LIT[block]
        for half, part in zip(("lower", "upper"), halves):
            write(ASSETS / "models" / "block" / f"{block}_{half}.json",
                  {"parent": "minecraft:block/block", "textures": textures, "elements": part})
            write(ASSETS / "models" / "block" / f"{block}_{half}_lit.json",
                  {"parent": rid(f"block/{block}_{half}"), "textures": {lit_from: rid(f"block/{lit_to}")}})
        variants = {}
        for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
            for half in ("lower", "upper"):
                for lit in (False, True):
                    variant = {"model": rid(f"block/{block}_{half}{'_lit' if lit else ''}")}
                    if y:
                        variant["y"] = y
                    variants[f"facing={facing},half={half},lit={str(lit).lower()}"] = variant
        write(ASSETS / "blockstates" / f"{block}.json", {"variants": variants})
        write(ASSETS / "models" / "item" / f"{block}.json", {
            "parent": "minecraft:block/block", "textures": textures,
            "elements": model_writer.scaled_elements(elements)})
        write(ASSETS / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{block}")}})
    # How the rocket pack looks when worn: harness straps as an armor layer (textures/entity/equipment/humanoid/
    # rocket_pack.png), and the pack itself in 3D on the back (client/RocketPackLayer draws these quads).
    write(ASSETS / "equipment" / "rocket_pack.json", {"layers": {"humanoid": [{"texture": rid("rocket_pack")}]}})
    import kinetic_rotors
    write(ASSETS / "worn_models.json", {"rocket_pack": kinetic_rotors.quads(tool_models.ITEMS["rocket_pack"])})
    for module, (display, short, about) in UPGRADE_MODULES.items():
        lang[f"item.{MOD}.{module}"] = display
        lang[f"item.{MOD}.{module}.short"] = short
        lang[f"tooltip.{MOD}.{module}"] = about
        write(ASSETS / "models" / "item" / f"{module}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{module}")}})
        write(ASSETS / "items" / f"{module}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{module}")}})
    lang[f"tooltip.{MOD}.module_fitting"] = "Use it on a charging station holding the tool"
    lang[f"tooltip.{MOD}.upgrades"] = "Upgrades:"
    lang[f"message.{MOD}.module.fitted"] = "%s fitted to the %s"
    lang[f"message.{MOD}.module.wrong_tool"] = "The %s does not fit the %s"
    lang[f"message.{MOD}.module.full"] = "The %2$s has no room for another %1$s"
    lang[f"message.{MOD}.module.conflict"] = "The %2$s cannot take a %1$s alongside its other enchantment"
    lang[f"message.{MOD}.drill_mode"] = "Drill mode: %s"
    lang[f"message.{MOD}.drill_mode.single"] = "one block"
    lang[f"message.{MOD}.drill_mode.area"] = "3×3"
    lang[f"message.{MOD}.drill_mode.vein"] = "whole ore vein"
    lang[f"tooltip.{MOD}.energy"] = "%s / %s JE"
    lang[f"tooltip.{MOD}.drill_mode"] = "Mode: %s (sneak + use to change)"
    lang[f"tooltip.{MOD}.chainsaw"] = "Fells whole trees (sneak to cut one log)"
    lang[f"tooltip.{MOD}.rocket_pack"] = "Hold jump in the air to fly"
    lang[f"message.{MOD}.charging_station"] = "Charging station: %s / %s JE"
    lang[f"message.{MOD}.charging_station.tool"] = "%s: %s / %s JE"


def petro_assets(lang):
    """Petroleum fluids (tools/petro.py): the liquid block (particles only; the fluid renderer draws the liquid) and
    the bucket."""
    import petro
    for fluid, info in petro.FLUIDS.items():
        lang[f"block.{MOD}.{fluid}"] = info["display"]
        write(ASSETS / "blockstates" / f"{fluid}.json", {"variants": {"": {"model": rid(f"block/{fluid}")}}})
        write(ASSETS / "models" / "block" / f"{fluid}.json", {"textures": {"particle": rid(f"block/{fluid}_still")}})
        bucket = f"{fluid}_bucket"
        lang[f"item.{MOD}.{bucket}"] = f"{info['display']} Bucket"
        write(ASSETS / "models" / "item" / f"{bucket}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{bucket}")}})
        write(ASSETS / "items" / f"{bucket}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{bucket}")}})


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
    for block in machine_blocks():
        write(out / f"{block}.json", self_drop(block))
    # The 2-tall charging station drops once, from its lower half.
    for block in TOOL_BLOCKS:
        table = self_drop(block)
        table["pools"][0]["conditions"].append({"condition": "minecraft:block_state_property", "block": rid(block),
                                                "properties": {"half": "lower"}})
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

    for result, (pattern, key, count) in CRAFTING.items():
        features = [MACHINE_FEATURE] + sorted({
            feature_of(f"{ref.split('/')[-1]}_ingot") for ref in key.values() if ref.startswith("#c:ingots/")
            and ref.split("/")[-1] not in ("copper", "iron", "gold")} | ({"silicon"} if "#c:silicon" in key.values() else set()))
        recipe = shaped(MACHINE_FEATURE, pattern, key, result, count)
        recipe["fabric:load_conditions"] = [c for f in features for c in condition(f)]
        write(out / f"{result}.json", recipe)
    machine_recipe_files(out)
    import petro
    for kind, name, data in petro.fluid_recipe_files(condition):
        write(out / kind / f"{name}.json", data)

    # Dusts smelt back into ingots wherever the metal's ore could be smelted; the others use the arc furnace.
    for metal in COMPONENTS["dust"]:
        kinds = METALS[metal]["cook"] if metal in METALS else ["smelting", "blasting"]
        feature = METALS[metal]["feature"] if metal in METALS else MACHINE_FEATURE
        for kind in kinds:
            recipe = cooking(feature, kind, f"#c:dusts/{metal}", f"{metal}_ingot", 0.1)
            recipe["result"]["id"] = ingot_id(metal)
            recipe["fabric:load_conditions"] = [c for f in sorted({MACHINE_FEATURE, feature}) for c in condition(f)]
            write(out / f"{metal}_ingot_from_{kind}_{metal}_dust.json", recipe)
    # Four sawdust press into a sheet of paper.
    paper = shaped(MACHINE_FEATURE, ["SS", "SS"], {"S": rid("sawdust")}, "sawdust")
    paper["result"] = {"id": "minecraft:paper", "count": 1}
    write(out / "paper_from_sawdust.json", paper)

    # Gears: four plates of one metal (36 nugget units in, 36 out).
    for metal in COMPONENTS["gear"]:
        write(out / f"{metal}_gear.json", shaped(MACHINE_FEATURE, [" P ", "P P", " P "],
                                                 {"P": f"#c:plates/{metal}"}, f"{metal}_gear"))

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

    for block in machine_blocks():
        tags.add("block", "minecraft:mineable/pickaxe", rid(block))

    # What the powered tools mine fast (tools/JugcraftTools): the drill is a pickaxe and shovel, the chainsaw an axe
    # that also cuts leaves.
    for tag in ("#minecraft:mineable/pickaxe", "#minecraft:mineable/shovel"):
        tags.add("block", "jugcraft:mineable/drill", tag)
    for tag in ("#minecraft:mineable/axe", "#minecraft:leaves"):
        tags.add("block", "jugcraft:mineable/chainsaw", tag)

    for form, metals in COMPONENTS.items():
        for metal in metals:
            tags.add("item", f"c:{form}s/{metal}", rid(f"{metal}_{form}"))
            tags.add("item", f"c:{form}s", f"#c:{form}s/{metal}")

    for item, info in ITEMS.items():
        if info["tag"]:
            tags.add("item", f"c:{info['tag']}", rid(item))
            if info["tag"].startswith("dusts/"):
                tags.add("item", "c:dusts", f"#c:{info['tag']}")

    # Petroleum fluids, so other mods' machines can recognise them (c:crude_oil and so on).
    import petro
    for fluid in petro.FLUIDS:
        tags.add("fluid", f"c:{fluid}", rid(fluid))
        tags.add("fluid", f"c:{fluid}", rid(f"flowing_{fluid}"))
    tags.write()


# ---------------------------------------------------------------- worldgen

def ore_feature(name, size, targets):
    # Minecraft 26.x: configured features live in worldgen/feature/ and have no "config" wrapper.
    write(DATA / MOD / "worldgen" / "feature" / f"ore_{name}.json", {
        "type": "minecraft:ore",
        "size": size, "discard_chance_on_air_exposure": 0.0, "targets": targets,
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
         "state": rid(ore)},
        {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables"},
         "state": rid(deep)},
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
                                         "state": rid(rock)}])
        placed_feature(rock, gen)


def main():
    for directory in GENERATED_DIRS:
        if directory.exists():
            shutil.rmtree(directory)
    assets()
    import handbook
    write(ASSETS / "handbook" / "en_us.json", handbook.build())
    import recipe_view
    write(ASSETS / "recipe_view.json", recipe_view.build())
    loot_tables()
    recipes()
    tags()
    worldgen()
    import advancements
    for key, advancement in advancements.generate(MOD)[0].items():
        write(DATA / MOD / "advancement" / f"{key}.json", advancement)


if __name__ == "__main__":
    main()
