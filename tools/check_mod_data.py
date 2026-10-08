"""Offline checks for Jugcraft's data files. This is NOT a Minecraft build or game test.

Verifies that every material ID has its blockstate/model/item definition/loot
table/texture/name, that every reference resolves, that Java registration matches
tools/materials.py, and that no recipe creates metal from nothing.
"""
import hashlib
import json
import re
import sys
from pathlib import Path

from PIL import Image

from party import PARTY_LANG
import drones
from materials import (MOD, METALS, MINERALS, ROCKS, ITEMS, FEATURES, COMPONENTS, PART_UNITS, CIRCUITS, WASHED_ORES,
                       all_blocks, all_items, feature_of, ore_gens, ore_gen_owners)
import agriculture as ag
import werewolf_model
import midway
import ferris_wheel
import pinata
import hot_air_balloon
import leaf_blower
import decor15
import decor16
import decor17
import decor18
import decor19
import decor20
import petro
import deposits
import seasons
import tank_display
import exosuit
import grapple
import field_chemistry
import construction
import hydroponics
import electroplating
import gas_storage
import control_electronics
import rocketry
import dieselworks
import kaiserworks
import trenchworks
import zeppelin
import mech
import landship
import artillery
import tower_guns
import fortifications
import bunkerworks
import fire_control
import raiders
import armoured_walker
import gear
import arms
import arms_variants
import check_icon_maps
import plastic
from machines import (CROPS, MACHINES, STATS, ORE_PROCESSING_MULTIPLIER, ORE_WASHING_MULTIPLIER, ORE_LEACHING_MULTIPLIER, BYPRODUCT_SHARE,
                      RENEWABLE_UNITS, WOODS, machine_blocks, machine_items, machine_recipes)
import pixel_hollows as ph
import alpine as al
import biomes as bm
import biomes_data
import trees as tr
import plants
import styx
import town_assets
import graveyard as gy
import diagonal_connections as dg

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src" / "main" / "resources"
ASSETS = RES / "assets" / MOD
DATA = RES / "data"
JAVA_ROOT = ROOT / "src" / "main" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft"
JAVA = JAVA_ROOT / "materials" / "JugcraftMaterials.java"
CONFIG = JAVA_ROOT / "config" / "JugcraftConfig.java"
SEASON_JAVA = JAVA_ROOT / "season"
WORLDGEN = JAVA_ROOT / "materials" / "JugcraftWorldgen.java"
MACHINE_JAVA = JAVA_ROOT / "machine" / "MachineKind.java"
AGRICULTURE_JAVA = JAVA_ROOT / "agriculture"
WORLD_JAVA = JAVA_ROOT / "world"
STYLE_PACK = RES / "resourcepacks" / "alternate_machines"

# Tags that Jugcraft reads but that vanilla/Fabric API define.
EXTERNAL_TAGS = ({"c:ingots/copper", "c:ingots/iron", "minecraft:stone_ore_replaceables",
                  "minecraft:deepslate_ore_replaceables", "minecraft:planks", "minecraft:campfires", "minecraft:mineable/axe",
                  "minecraft:mineable/shovel", "minecraft:leaves", "minecraft:eggs", "minecraft:dirt", "minecraft:mud",
                  "minecraft:grass_blocks", "minecraft:sand", "minecraft:wool", "minecraft:logs", "minecraft:candles",
                  "minecraft:stairs", "minecraft:slabs", "minecraft:walls", "minecraft:coals", "minecraft:dyes",
                  # The town's usable blocks (tools/town_assets.py USABLE): vanilla 26.3's own block tags.
                  "minecraft:wooden_doors", "minecraft:fence_gates", "minecraft:buttons", "minecraft:beds",
                  "minecraft:is_forest", "minecraft:is_taiga",
                  # Fabric's conventional biome tag (ConventionalBiomeTags.IS_SNOWY): the snow werewolf's haunts.
                  "c:is_snowy",
                  # Vanilla's Overworld stone (stone, granite, diorite, andesite, tuff, deepslate): thallite's natural
                  # ground (tools/gear.py EARTHEN_GROUND).
                  "minecraft:base_stone_overworld", "minecraft:base_stone_nether", "minecraft:nylium"}
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
        # Square: 16x16, or 32x32, 48x48 or 64x64 for high-resolution art, or 128x128 for a sculpted prop's packed texture
        # (docs/ART_DIRECTION.md, "High resolution"). New item icons are 16x16 (docs/ITEM_ICONS.md): 32, 48 and 64 remain
        # for the 122 older icons not yet redrawn (tools/legacy_item_icons.txt, which tools/check_icon_maps.py holds to),
        # and for block and model textures.
        # Animated textures are a vertical strip of square frames of 16x16, 32x32, 48x48 or 64x64 with an .mcmeta beside
        # them.
        width, height = img.size
        if animated and not (width in (16, 32, 48, 64) and height % width == 0 and height > width):
            err(f"Animated texture {ref} is {img.size}, expected a strip of 16x16, 32x32, 48x48 or 64x64 frames")
        elif not animated and img.size not in ((16, 16), (32, 32), (48, 48), (64, 64), (128, 128)) \
                and not (img.size in ((256, 256), (512, 512), (1024, 1024)) and _sharp(png.stem)):
            err(f"Texture {ref} is {img.size}, expected 16x16, 32x32, 48x48, 64x64 or 128x128")


# The special item renderers registered by client/StructureItemRenderers.java.
SPECIAL_ITEM_RENDERERS = {f"{MOD}:equipment", f"{MOD}:power_line", f"{MOD}:drone"}


def model(ref):
    ns, path = split(ref)
    if ns == "minecraft":
        return
    data = load(RES / "assets" / ns / "models" / f"{path}.json")
    if data is None:
        return
    for tex in data.get("textures", {}).values():
        # A texture is a reference, or {"sprite": reference, "force_translucent": ...}.
        texture(tex["sprite"] if isinstance(tex, dict) else tex)


def _sharp(name):
    """Plant kit textures with writing on them (paperwork, book spines) are kept at 128 or 256 so the text reads."""
    import plant_office3
    import plant_office4
    import plant_panels
    import nuclear
    import nuclear_storage
    return (name in plant_office3.SHARP or name in plant_office4.SHARP or name in plant_panels.SHARP or name in nuclear.SHARP
            or name in nuclear_storage.SHARP)


def item_models(definition):
    """Every model an item definition can show, through select (the blueprint's kinds), condition and range_dispatch
    (the power bow's draw). A special model (the miniatures of client/StructureItemRenderers.java) shows its base
    model's display and particle, and one of the special renderers the client registers."""
    if definition.get("type") == "minecraft:special":
        model(definition["base"])
        if definition["model"].get("type") not in SPECIAL_ITEM_RENDERERS:
            err(f"An item's special model is of {definition['model'].get('type')}, not one of {sorted(SPECIAL_ITEM_RENDERERS)}")
        return
    if "model" in definition:
        model(definition["model"])
    for case in definition.get("cases", []):
        item_models(case["model"])
    for key in ("on_true", "on_false", "fallback"):
        if key in definition:
            item_models(definition[key])
    for entry in definition.get("entries", []):
        item_models(entry["model"])


def check_assets(registered):
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    for block in (all_blocks() + machine_blocks() + ag.all_blocks() + petro.petro_blocks() + list(deposits.DEPOSITS)
                  + list(tank_display.BLOCKS) + plastic.blocks() + ph.blocks() + (town_assets.blocks() + styx.blocks()) + seasons.BLOCKS
                  + construction.blocks() + control_electronics.blocks() + rocketry.blocks() + dieselworks.blocks() + kaiserworks.blocks() + trenchworks.blocks() + fortifications.blocks() + bunkerworks.blocks() + fire_control.blocks()):
        state = load(ASSETS / "blockstates" / f"{block}.json")
        if state:
            for variant in state.get("variants", {}).values():
                for choice in variant if isinstance(variant, list) else [variant]:  # a list: weighted, picked by position
                    model(choice["model"])
            for part in state.get("multipart", []):
                model(part["apply"]["model"])
        if f"block.{MOD}.{block}" not in lang:
            err(f"Missing name for block {block}")
        if not (DATA / MOD / "loot_table" / "blocks" / f"{block}.json").is_file():
            err(f"Missing loot table for {block}")
    # Crops, stems, the cranberry bush and the chestnut sapling have no item of their own: what plants them stands in.
    for item in registered:
        if item in CROPS or item in ag.itemless_blocks():
            continue
        definition = load(ASSETS / "items" / f"{item}.json")
        if definition:
            item_models(definition["model"])
        if item not in (all_blocks() + machine_blocks() + ag.all_blocks() + petro.petro_blocks() + list(deposits.DEPOSITS)
                        + list(tank_display.BLOCKS) + plastic.blocks() + ph.blocks() + (town_assets.blocks() + styx.blocks())
                        + construction.blocks() + control_electronics.blocks() + rocketry.blocks() + dieselworks.blocks() + kaiserworks.blocks() + trenchworks.blocks() + fortifications.blocks() + bunkerworks.blocks() + fire_control.blocks()) and f"item.{MOD}.{item}" not in lang:
            err(f"Missing name for item {item}")


def check_petro():
    """Petroleum fluids: Java registers exactly tools/petro.py's fluids, each with its block, textures and names."""
    java = (JAVA_ROOT / "chemistry" / "PetroFluids.java").read_text(encoding="utf-8")
    declared = re.findall(r'= fluid\("([a-z_]+)", (\d+), (\d+), (\d+)', java)
    expected = [(f, str(i["tick_delay"]), str(i["slope"]), str(i["drop_off"])) for f, i in petro.FLUIDS.items()]
    if declared != expected:
        err(f"PetroFluids.java fluids {declared} != tools/petro.py {expected}")
    items_java = re.findall(r'JugcraftRegistry\.item\("([a-z_]+)"[,)]',
                            (JAVA_ROOT / "chemistry" / "PetroItems.java").read_text(encoding="utf-8"))
    if items_java != list(petro.ITEMS):
        err(f"PetroItems.java items {items_java} != tools/petro.py {list(petro.ITEMS)}")
    blocks_java = re.findall(r'= register\("([a-z_]+)"', (JAVA_ROOT / "chemistry" / "PetroBlocks.java").read_text(encoding="utf-8"))
    if blocks_java != list(petro.BLOCKS):
        err(f"PetroBlocks.java blocks {blocks_java} != tools/petro.py {list(petro.BLOCKS)}")
    gases = re.findall(r'= gas\("([a-z_]+)"', java)
    if gases != list(petro.GASES):
        err(f"PetroFluids.java gases {gases} != tools/petro.py {list(petro.GASES)}")
    fuels_java = (JAVA_ROOT / "chemistry" / "FluidFuels.java").read_text(encoding="utf-8")
    for machine, fuels in petro.FLUID_FUELS.items():
        if f"case {machine.upper()} ->" not in fuels_java:
            err(f"FluidFuels.java has no case for {machine}")
        for fuel, value in fuels.items():
            if fuel not in petro.FLUIDS and fuel not in petro.GASES:
                err(f"{machine}: unknown fuel {fuel}")
            accessor = "fluid()" if fuel in petro.GASES else "source()"
            # A machine may have its own constant for a fuel, such as ADVANCED_DIESEL.
            if not re.search(rf"int (\w+_)?{fuel.upper()} = {value};", fuels_java) or f"PetroFluids.{fuel.upper()}.{accessor}" not in fuels_java:
                err(f"{machine}: {fuel} at {value} JE/mB in tools/petro.py does not match FluidFuels.java")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    for gas in petro.GASES:
        if f"block.{MOD}.{gas}" not in lang:
            err(f"Missing name for gas {gas}")
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


# Keys of the pre-26.x loot format: Minecraft 26.x ignores them without a warning, so a table that used them would lose
# its conditions and functions (counts, Fortune, which half of a block drops) silently.
OLD_LOOT_KEYS = {"conditions", "functions", "function"}


def old_loot_keys(node):
    if isinstance(node, dict):
        return (OLD_LOOT_KEYS & set(node)) | {key for value in node.values() for key in old_loot_keys(value)}
    if isinstance(node, list):
        return {key for value in node for key in old_loot_keys(value)}
    return set()


def check_loot(registered):
    for path in sorted((DATA / MOD / "loot_table").rglob("*.json")):
        text = path.read_text(encoding="utf-8")
        stale = old_loot_keys(load(path))
        if stale:
            err(f"{path.name}: uses the pre-26.x loot keys {sorted(stale)}; use \"condition\" and \"modifier\"")
        if '"minecraft:block_state_property"' in text:
            err(f"{path.name}: 26.3 has no block_state_property loot condition (the server fails to load); use match_block")
        for name in re.findall(r'"name": "jugcraft:([a-z_]+)"', text):
            if name not in registered:
                err(f"{path.name} drops unknown item {name}")


# Metal content in nugget units. Tags stand for the same forms from any mod.
UNITS = {"ingots": 9, "nuggets": 1, "raw_materials": 9, "ores": 9, "storage_blocks": 81,
         **{f"{form}s": units for form, units in PART_UNITS.items()}}


import guide_books
NON_METAL = set(guide_books.BOOKS) | {"sawdust"} | set(MINERALS) | set(ITEMS) | set(machine_blocks()) | set(machine_items()) | set(CIRCUITS) | {b for m in MINERALS for b in (f"{m}_ore", f"deepslate_{m}_ore", f"{m}_block")} | {"oil_sand"} | set(petro.petro_items()) | set(ag.all_blocks()) | set(ag.all_items()) | set(petro.petro_blocks()) | set(tank_display.BLOCKS) | set(ph.blocks()) | set(ph.items()) | set((town_assets.blocks() + styx.blocks()))


def item_units(ref):
    """Returns {metal: units} for an item or tag reference."""
    ns, path = split(ref.lstrip("#"))
    if ref.startswith("#") and (ns == "minecraft" or ref[1:] in (*ag.WOOD_TAGS.values(), ag.HEIRLOOM_TAG)):
        return {}  # vanilla tags used here (logs, planks), Jugcraft logs and heirloom pumpkins hold no metal
    if ref.startswith("#"):
        form, _, metal = path.partition("/")
        if metal in MINERALS or path in {info["tag"] for info in ITEMS.values()} or path in ("fermentable", "grave_flowers"):
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
        # One bauxite holds two ingots of aluminum (it is about half alumina): the Bayer route (chemical reactor and
        # electrolytic cell) recovers all of it, the arc furnace stand-in half, the blast-furnace stand-in a nugget.
        return {"aluminum": 18}
    if path == "titanium_sponge":
        # Kroll-process sponge: one ingot of titanium each, melted in the arc furnace.
        return {"titanium": 9}
    if path == "alumina":
        # Bayer-process alumina: one ingot of aluminum each, smelted out in the electrolytic cell.
        return {"aluminum": 9}
    if path in NON_METAL:
        return {}
    if path in plastic.blocks() or path in exosuit.items() or path in grapple.items() or path in field_chemistry.items()\
            or path in construction.items() or path in construction.blocks() or path in gas_storage.items() or path in control_electronics.blocks() or path in control_electronics.items() or path in rocketry.items() or path in rocketry.blocks() or path in dieselworks.blocks() or path in kaiserworks.blocks() or path in trenchworks.blocks() or path in fortifications.blocks() or path in bunkerworks.blocks() or path in fire_control.blocks() or path in fire_control.items() or path in raiders.ITEMS or path in armoured_walker.ITEMS or path in zeppelin.ITEMS or path in mech.ITEMS or path in landship.ITEMS or path in artillery.ITEMS or path in tower_guns.items():
        return {}
    if path in arms.items():
        return arms.metal_content(path)
    if path in arms_variants.items():
        # A styled variant holds the steel arm it is smithed from and its style's addition; patterns and trophies none.
        if path in arms_variants.BY_ID and arms_variants.line(path) in arms_variants.STYLES:
            content = dict(arms.metal_content(f"steel_{arms_variants.kind(path)}"))
            for metal, units in item_units(arms_variants.STYLES[arms_variants.line(path)]["addition"]).items():
                content[metal] = content.get(metal, 0) + units
            return content
        return {}
    if path in gear.items():
        # Gear holds the ingots it is crafted from; a paxel holds its pickaxe, axe and shovel. Vanilla-tier paxels
        # hold nothing the audit tracks, like the vanilla tools they are made from. A styled armor piece counts as its
        # plain piece only (its smithing addition is lost metal), and its template holds none.
        if path in gear.style_items():
            return item_units(f"{MOD}:{gear.base_piece(path)}")
        if path in gear.style_templates():
            return {}
        tier, piece = path.rsplit("_", 1)
        if tier not in gear.GEAR_TIERS:
            return {}
        pieces = ("pickaxe", "axe", "shovel") if piece == "paxel" else (piece,)
        return {tier: 9 * sum("".join(gear.PATTERNS[p]).count("#") for p in pieces)}
    err(f"No metal content known for {ref}")
    return {}


# Special recipes, which make their result from what is in the grid: their type, registered by Java.
SPECIAL_RECIPES = {f"{MOD}:{decor18.KEY['recipe']}"}


def check_recipes(registered):
    for path in sorted((DATA / MOD / "recipe").glob("*.json")):
        recipe = load(path)
        if recipe is None:
            continue
        name = path.stem
        conditions = recipe.get("fabric:load_conditions", [])
        switches = [c for c in conditions if c.get("condition") == f"{MOD}:feature_enabled"]
        features = [c.get("feature") for c in switches]
        # Each condition is one switch, or several any one of which loads the recipe ("or": a wood's WOOD_SWITCHES).
        groups = [sorted([c.get("feature"), *c.get("or", [])]) for c in switches]
        if not features:
            err(f"{name}: missing feature switch condition")
        if "result" not in recipe:
            # A special recipe makes its result from what is in the grid (the Skeleton Key's copying).
            if recipe["type"] not in SPECIAL_RECIPES:
                err(f"{name}: no result, and {recipe['type']} is not a known special recipe")
            continue
        result = split(recipe["result"]["id"])[1]
        if features and result in registered:
            owner = feature_of(result)
            owner = sorted(owner) if isinstance(owner, list) else [owner]
            if owner not in groups:
                err(f"{name}: gated by {groups} but its result belongs to {owner} (any of them)")

        kind = recipe["type"]
        if kind == "minecraft:crafting_shaped":
            symbols = "".join(recipe["pattern"])
            inputs = [recipe["key"][ch] for ch in symbols if ch != " "]
        elif kind == "minecraft:crafting_shapeless":
            inputs = recipe["ingredients"]
        elif kind == "minecraft:crafting_dye":
            # 26.3's dyeing (as vanilla's leather_helmet_dyed): the item and a dye give the item back, dyed.
            inputs = [recipe["target"], recipe["dye"]]
            if recipe["result"]["id"] != recipe["target"]:
                err(f"{name}: a dyeing recipe must give back the item it dyes")
        elif kind == "minecraft:smithing_transform":
            inputs = [recipe["template"], recipe["base"], recipe["addition"]]
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
    from machines import FEATURE as MACHINE_SWITCH
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
        # A Jugcraft wood's sawmill recipe follows the machines switch and its wood's switches, any one of which loads it
        # (agriculture.WOOD_SWITCHES, TREES.md rule 4), as the wood's hand recipes do. The tree farm follows the machines
        # switch alone, by design.
        wood = path.stem.removesuffix("_logs")
        if path.parent.name == "sawing" and wood in ag.WOOD_SETS:
            groups = sorted(sorted([c.get("feature"), *c.get("or", [])]) for c in recipe.get("fabric:load_conditions", [])
                            if c.get("condition") == f"{MOD}:feature_enabled")
            wanted = sorted([[MACHINE_SWITCH], sorted(ag.WOOD_SWITCHES.get(wood, []))])
            if groups != wanted:
                err(f"{label}: gated by {groups}, not by the machines switch and the {wood} wood's switches "
                    f"{ag.WOOD_SWITCHES.get(wood)} (any of them)")
        refs = [part["ingredient"] for part in recipe.get("ingredients", [])] or [recipe.get("ingredient", "")]
        refs += [entry["result"]["id"] for entry in recipe.get("byproducts", [])]
        for ref in refs + [recipe["result"]["id"]]:
            if ref.startswith("#"):
                if not tag_exists("item", ref[1:]):
                    err(f"{label}: unknown tag {ref}")
            elif split(ref)[0] == MOD and split(ref)[1] not in registered:
                err(f"{label}: unknown item {ref}")


# Jugcraft entries of registries other than blocks and items that tags may name.
OTHER_ENTRIES = {"worldgen": {"pixel_hollows", al.BIOME, al.VILLAGE} | set(bm.BIOMES), "point_of_interest_type": {"arcade_cabinet"},
                 "villager_trade": {f"retro_trader/{name}" for name in ph.TRADES}}


def check_fluid_recipes(registered):
    """Fluid recipes (tools/petro.py): every item and fluid resolves, slots and tanks exist, and none makes fluid from
    nothing. Recipes with no item input must not give out more fluid than they take in; recipes with item input must
    say how much fluid they release from it ("source")."""
    from generate_material_data import RECIPE_TYPES
    java = MACHINE_JAVA.read_text(encoding="utf-8")
    fluids = {f"{MOD}:{f}" for f in list(petro.FLUIDS) + list(petro.GASES)} | {"minecraft:water", "minecraft:lava"}
    for machine, spec in petro.FLUID_MACHINES.items():
        match = re.search(r"case " + machine.upper() + r" -> new FluidMachineSpec\(List\.of\(([^)]*)\),\s*List\.of\(([^)]*)\),\s*"
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
    expected = sum(len(r) for r in petro.FLUID_RECIPES.values()) + sum(len(r) for r in drones.DRONE_FLUID_RECIPES.values())
    types = {spec["recipe_type"] for spec in petro.FLUID_MACHINES.values() if spec["recipe_type"]}
    files = [p for p in (DATA / MOD / "recipe").glob("*/*.json") if p.parent.name in types]
    if len(files) != expected:
        err(f"{len(files)} fluid recipe files, but tools/petro.py and tools/drones.py define {expected}")
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
            targets = [petro.result_tank(i, r) for i, r in enumerate(recipe.get("fluid_results", []))]
            if len(set(targets)) != len(targets):
                err(f"{label}: two fluid results share an output tank")
            for i, result in enumerate(recipe.get("fluid_results", [])):
                fluid, mb, tank = result[0], result[1], targets[i]
                if fluid not in fluids:
                    err(f"{label}: unknown fluid {fluid}")
                if tank >= len(spec["outputs"]):
                    err(f"{label}: sends {fluid} to output tank {tank}, which the machine does not have")
                elif mb > spec["outputs"][tank]:
                    err(f"{label}: makes {mb} mB of {fluid} but its tank holds {spec['outputs'][tank]}")
            # Metal is conserved like in the item machines: no recipe gives out more than its items hold.
            metal_in, metal_out = {}, {}
            for ref, count in recipe.get("items", []):
                for metal, units in item_units(ref).items():
                    metal_in[metal] = metal_in.get(metal, 0) + units * count
            for ref, count in recipe.get("results", []):
                for metal, units in item_units(ref).items():
                    metal_out[metal] = metal_out.get(metal, 0) + units * count
            # Ore routes (batch 26: acid leaching) may multiply an ore's metal, up to ORE_LEACHING_MULTIPLIER.
            bonus = recipe.get("ore_bonus", 1)
            if bonus > ORE_LEACHING_MULTIPLIER:
                err(f"{label}: ore bonus {bonus} exceeds the leaching route's {ORE_LEACHING_MULTIPLIER}")
            if bonus > 1 and not all(ref.split(":")[1].endswith("_ore") for ref, _ in recipe.get("items", [])):
                err(f"{label}: only ores may take an ore bonus")
            for metal, units in metal_out.items():
                if units > metal_in.get(metal, 0) * bonus:
                    err(f"{label}: gives {units} {metal} units from {metal_in.get(metal, 0)}")
            for ref, _ in recipe.get("items", []) + recipe.get("results", []):
                if ref.startswith("#"):
                    if not tag_exists("item", ref[1:]):
                        err(f"{label}: unknown tag {ref}")
                elif split(ref)[0] == MOD and split(ref)[1] not in registered:
                    err(f"{label}: unknown item {ref}")
            fluid_in = sum(mb for _, mb in recipe.get("fluids", [])) + recipe.get("source", 0)
            fluid_out = sum(r[1] for r in recipe.get("fluid_results", []))
            if recipe.get("items") and "source" not in recipe and fluid_out > sum(mb for _, mb in recipe.get("fluids", [])):
                err(f"{label}: makes fluid from items without saying how much (\"source\")")
            if fluid_out > fluid_in:
                err(f"{label}: {fluid_out} mB out from {fluid_in} mB in")


# The diagonal walls (tools/diagonal_connections.py), which join #minecraft:walls.
DIAGONAL_WALLS = {dg.DIAGONAL_WALL.format(name) for _, name in dg.all_walls()}


def check_tags():
    for path in sorted(DATA.rglob("tags/*/**/*.json")):
        registry = path.relative_to(DATA).parts[2]
        known = OTHER_ENTRIES.get(registry) or set(all_blocks() + all_items() + machine_blocks() + machine_items()
                                                    + petro.petro_blocks() + petro.petro_items() + list(deposits.DEPOSITS)
                                                    + list(tank_display.BLOCKS) + seasons.BLOCKS + ph.blocks() + ph.items()
                                                    + arms.items() + arms_variants.items()
                                                    + gear.items() + plastic.blocks() + exosuit.items() + grapple.items()
                                                    + field_chemistry.items() + construction.items() + construction.blocks() + gas_storage.items() + control_electronics.blocks() + control_electronics.items() + rocketry.items() + rocketry.blocks() + dieselworks.blocks() + kaiserworks.blocks() + trenchworks.blocks() + fortifications.blocks() + bunkerworks.blocks() + fire_control.blocks() + fire_control.items() + list(raiders.ITEMS) + list(raiders.BLOCKS) + list(armoured_walker.ITEMS) + list(zeppelin.ITEMS) + list(mech.ITEMS) + list(landship.ITEMS) + list(artillery.ITEMS) + tower_guns.items()
                                                    + ag.all_blocks() + ag.all_items() + (town_assets.blocks() + styx.blocks()))
        if registry == "entity_type":
            # These entity IDs have no same-named item. Derive them from actual registrations.
            scary = (JAVA_ROOT / "creatures" / "scary" / "ScaryMod.java").read_text(encoding="utf-8")
            known |= set(re.findall(r'Registry\.register\(BuiltInRegistries\.ENTITY_TYPE,\s*id\("([a-z_]+)"\)', scary))
        for value in (load(path) or {}).get("values", []):
            value = value["id"] if isinstance(value, dict) else value
            if value.startswith("#"):
                if not tag_exists(registry, value[1:]):
                    err(f"{path.relative_to(ROOT)}: unknown tag {value}")
            elif registry == "damage_type":
                ns, name = split(value)
                if not (DATA / ns / "damage_type" / f"{name}.json").is_file():
                    err(f"{path.relative_to(ROOT)}: unknown damage type {value}")
            elif registry == "fluid":
                if split(value)[1] not in petro.fluid_ids():
                    err(f"{path.relative_to(ROOT)}: unknown fluid {value}")
            elif registry == "villager_trade":
                namespace, trade = split(value)
                if namespace == MOD and not (DATA / MOD / "villager_trade" / f"{trade}.json").exists():
                    err(f"{path.relative_to(ROOT)}: unknown villager trade {value}")
            elif split(value)[0] == MOD and split(value)[1] not in known and split(value)[1] not in DIAGONAL_WALLS:
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
            if block not in all_blocks() + ph.blocks():
                err(f"{path.name}: places unknown block {block}")
    check_nested_features()
    for path in sorted((DATA / MOD / "worldgen" / "placed_feature").glob("*.json")):
        namespace, feature = split((load(path) or {})["feature"])
        # Vanilla configured features (the Pixel Hollows' bonus ores) are checked by the game tests, which load them.
        if namespace == MOD and not (DATA / MOD / "worldgen" / "feature" / f"{feature}.json").is_file():
            err(f"{path.name}: unknown configured feature {feature}")
    biome = load(DATA / MOD / "worldgen" / "biome" / "pixel_hollows.json") or {}
    if len(biome.get("features", [])) != len(ph.STEPS):
        err("pixel_hollows.json: needs one feature list per generation step")
    for step in biome.get("features", []):
        for ref in step:
            if split(ref)[0] == MOD and not (DATA / MOD / "worldgen" / "placed_feature" / f"{split(ref)[1]}.json").is_file():
                err(f"pixel_hollows.json: unknown placed feature {ref}")


def check_exosuit():
    """gear/JugcraftExosuit.java and gear/Exosuit.java against tools/exosuit.py: the liveries, pieces and numbers; and
    that every icon, worn layer and 3D part texture exists."""
    java = (JAVA_ROOT / "gear" / "JugcraftExosuit.java").read_text(encoding="utf-8")
    prefixes = dict(re.findall(r'ExosuitItem\.Style\.([A-Z]+), "([a-z_]+)"', java))
    expected = {style.upper(): info[0] for style, info in exosuit.STYLES.items()}
    if prefixes != expected:
        err(f"JugcraftExosuit.PREFIXES {prefixes} != tools/exosuit.py {expected}")
    pieces = [p.lower() for p in re.findall(r'ArmorType\.([A-Z]+)', re.search(r"PIECES = List\.of\(([^)]*)\)", java).group(1))]
    if pieces != exosuit.PIECES:
        err(f"JugcraftExosuit.PIECES {pieces} != tools/exosuit.py {exosuit.PIECES}")
    for name in ("ronin_katana", "ronin_livery", "vanguard_livery"):
        if f'item("{name}"' not in java:
            err(f"JugcraftExosuit does not register {name}")
    powers = (JAVA_ROOT / "gear" / "Exosuit.java").read_text(encoding="utf-8")
    for const in ("CAPACITY", "NIGHT_VISION_PER_TICK", "SHIELD_POINTS", "SHIELD_INTERVAL", "SHIELD_PER_POINT",
                  "SPEED_PER_TICK", "SPEED_BONUS", "BOOTS_PER_TICK", "STEP_BONUS"):
        value = getattr(exosuit, const)
        text = f"{value:_}" if isinstance(value, int) else str(value)
        if not re.search(rf"\b{const} = {re.escape(text)};", powers):
            err(f"Exosuit.{const} differs from tools/exosuit.py ({text})")
    textures = ASSETS / "textures"
    for style, (prefix, _, asset) in exosuit.STYLES.items():
        for layer in ("humanoid", "humanoid_leggings"):
            if not (textures / "entity" / "equipment" / layer / f"{asset}.png").exists():
                err(f"Missing worn exosuit texture {layer}/{asset}.png")
    for item in exosuit.items():
        if not (textures / "item" / f"{item}.png").exists():
            err(f"Missing item texture {item}.png")
    worn = load(ASSETS / "worn_models.json") or {}
    for style, parts in exosuit.PARTS.items():
        for name, boxes in parts.items():
            if f"{style}_{name}" not in worn:
                err(f"worn_models.json has no {style}_{name}")
            for _, _, tex in boxes:
                if not (textures / "block" / f"{tex}.png").exists():
                    err(f"Missing exosuit part texture block/{tex}.png")


def check_worn_armor():
    """The 3D armor sets (tools/armor_models.py, drawn by client/WornModelLayer). Every texture a quad in
    worn_models.json names exists (QuadModel's rule: a plain name is a block texture), and each set's atlas is there, is
    the size its layout gives the quads' UVs, and is exactly what tools/armor_paint.py paints. CI regenerates
    worn_models.json but not the textures, so a set changed without rerunning generate_textures.py stops here."""
    import armor_models
    import armor_paint
    textures = ASSETS / "textures"
    worn = load(ASSETS / "worn_models.json") or {}
    for name in sorted({quad["texture"] for quads in worn.values() for quad in quads}):
        if not (textures / (f"{name}.png" if "/" in name else f"block/{name}.png")).is_file():
            err(f"worn_models.json: missing texture {name}.png")
    for armor_set in armor_models.sets():
        path = textures / f"{armor_set.texture}.png"
        if not path.is_file():
            err(f"Missing 3D armor atlas {path.relative_to(ROOT)} (run tools/generate_textures.py)")
            continue
        image = Image.open(path).convert("RGBA")
        size = armor_models.layout(armor_set)[1]
        if image.size != size:
            err(f"{path.name} is {image.size[0]}x{image.size[1]}; set {armor_set.name}'s layout is {size[0]}x{size[1]}")
        elif image.tobytes() != armor_paint.paint_atlas(armor_set).convert("RGBA").tobytes():
            err(f"{path.name} is not what tools/armor_paint.py paints for {armor_set.name} (run tools/generate_textures.py)")


def check_hydroponics():
    """MachineKind's hydroponic bay numbers against tools/hydroponics.py."""
    java = MACHINE_JAVA.read_text(encoding="utf-8")
    for const, value in (("HYDROPONIC_TANK", hydroponics.TANK), ("HYDROPONIC_SOLUTION_PER_HARVEST", hydroponics.SOLUTION_PER_HARVEST)):
        if f"int {const} = {value:_};" not in java and f"int {const} = {value};" not in java:
            err(f"MachineKind.{const} differs from tools/hydroponics.py ({value})")


def check_electroplating():
    """machine/Electroplating.java against tools/electroplating.py: the numbers, metals and tooltips."""
    java = (JAVA_ROOT / "machine" / "Electroplating.java").read_text(encoding="utf-8")
    for const in ("TICKS", "ACID_PER_PLATING", "TANK", "NICKEL_DURABILITY_PERCENT", "SILVER_SMITE"):
        value = getattr(electroplating, const)
        if f"int {const} = {value:_};" not in java and f"int {const} = {value};" not in java:
            err(f"Electroplating.{const} differs from tools/electroplating.py ({value})")
    lang = json.loads((ASSETS / "lang" / "en_us.json").read_text(encoding="utf-8"))
    for metal, (tag, _, _) in electroplating.METALS.items():
        if f'"{metal}"' not in java or f'"{tag.split(":")[1]}"' not in java:
            err(f"Electroplating.java does not plate with {metal} ({tag})")
        if f"tooltip.jugcraft.plating.{metal}" not in lang:
            err(f"Missing tooltip for {metal} plating")


def check_gas_storage():
    """fluid/GasCylinderItem.java and MachineKind's ammonia chiller against tools/gas_storage.py."""
    java = (JAVA_ROOT / "fluid" / "GasCylinderItem.java").read_text(encoding="utf-8")
    if f"int CAPACITY = {gas_storage.CYLINDER_CAPACITY:_};" not in java:
        err(f"GasCylinderItem.CAPACITY differs from tools/gas_storage.py ({gas_storage.CYLINDER_CAPACITY})")
    kinds = MACHINE_JAVA.read_text(encoding="utf-8")
    for const, value in (("CHILLER_AMMONIA_TANK", gas_storage.AMMONIA_TANK), ("CHILLER_WATER_TANK", gas_storage.WATER_TANK)):
        if f"int {const} = {value:_};" not in kinds:
            err(f"MachineKind.{const} differs from tools/gas_storage.py ({value})")


def check_control_electronics():
    """control/ against tools/control_electronics.py: the numbers and the channel colours."""
    control = JAVA_ROOT / "control"
    for path, const, value in (("SensorBlock.java", "INTERVAL", control_electronics.SENSOR_INTERVAL),
                               ("LogicControllerBlockEntity.java", "INTERVAL", control_electronics.CONTROLLER_INTERVAL),
                               ("LogicControllerBlockEntity.java", "RULES", control_electronics.RULES),
                               ("LogicControllerBlockEntity.java", "STEP", control_electronics.THRESHOLD_STEP),
                               ("ControlNetwork.java", "MAX_CABLES", control_electronics.MAX_CABLES),
                               ("ControlMonitorBlockEntity.java", "RELINK_INTERVAL", control_electronics.MONITOR_RELINK),
                               ("LogicControllerBlockEntity.java", "HISTORY", control_electronics.HISTORY),
                               ("LogicControllerBlockEntity.java", "HISTORY_EVERY", control_electronics.HISTORY_EVERY),
                               ("AlarmBlock.java", "INTERVAL", control_electronics.ALARM_INTERVAL),
                               ("ControlRemoteItem.java", "RANGE", control_electronics.REMOTE_RANGE)):
        java = (control / path).read_text(encoding="utf-8")
        if f"int {const} = {value:_};" not in java and f"int {const} = {value};" not in java:
            err(f"{path} {const} differs from tools/control_electronics.py ({value})")
    screen = (ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client"
              / "LogicControllerScreen.java").read_text(encoding="utf-8")
    for rgb in control_electronics.CHANNEL_COLORS.values():
        if "0x%02X%02X%02X" % rgb not in screen:
            err(f"LogicControllerScreen.COLORS lacks channel colour {rgb}")


def check_zeppelin():
    """airship/Zeppelin.java against tools/zeppelin.py: the flight, fuel and cargo numbers."""
    java = (JAVA_ROOT / "airship" / "Zeppelin.java").read_text(encoding="utf-8")
    for const in ("MAX_SPEED", "ACCELERATION", "CLIMB", "TURN", "DRIFT_SINK", "FUEL_TANK", "FUEL_PER_BUCKET",
                  "FUEL_PER_SECOND", "SEATS", "CARGO_SLOTS", "HEALTH"):
        value = getattr(zeppelin, const)
        literal = f"{value}F" if const in ("TURN",) else str(value)
        if f" {const} = {literal};" not in java:
            err(f"Zeppelin.{const} differs from tools/zeppelin.py ({literal})")
    if not (ASSETS / "zeppelin_quads.json").is_file():
        err("assets/jugcraft/zeppelin_quads.json is missing: run tools/generate_material_data.py")


def check_walker():
    """walker/DieselWalker.java against tools/mech.py: the walking, drill, fist and fuel numbers."""
    java = (JAVA_ROOT / "walker" / "DieselWalker.java").read_text(encoding="utf-8")
    for const in ("WALK_SPEED", "TURN", "JUMP", "DRILL_REACH", "DRILL_TICKS_PER_HARDNESS", "DRILL_MIN_TICKS",
                  "DRILL_MAX_HARDNESS", "PUNCH_DAMAGE", "PUNCH_KNOCKBACK", "PUNCH_COOLDOWN", "FUEL_TANK",
                  "FUEL_PER_BUCKET", "FUEL_PER_SECOND", "HEALTH", "WIDTH", "HEIGHT"):
        value = getattr(mech, const)
        literal = f"{value}F" if const in ("TURN", "WIDTH", "HEIGHT") else str(value)
        if f" {const} = {literal};" not in java:
            err(f"DieselWalker.{const} differs from tools/mech.py ({literal})")
    renderer = (ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client"
                / "DieselWalkerRenderer.java")
    text = renderer.read_text(encoding="utf-8") if renderer.is_file() else ""
    joints = [*mech.HIPS.values(), mech.SHOULDERS["fist"], mech.SHOULDERS["drill"], mech.BIT]
    for joint in joints:
        if "{" + ", ".join(str(v) for v in joint) + "}" not in text:
            err(f"DieselWalkerRenderer lacks the joint {joint} from tools/mech.py")
    if not (ASSETS / "walker_quads.json").is_file():
        err("assets/jugcraft/walker_quads.json is missing: run tools/generate_material_data.py")


def check_armoured_walker():
    """walker/ArmouredWalker.java against tools/armoured_walker.py: the cannon, ram, toughness and size, the muzzle, and the
    joints its renderers draw the parts at."""
    java = (JAVA_ROOT / "walker" / "ArmouredWalker.java").read_text(encoding="utf-8")
    for const in ("CANNON_COOLDOWN", "CANNON_SPEED", "RAM_DAMAGE", "RAM_KNOCKBACK", "RAM_REACH", "RAM_COOLDOWN", "HEALTH",
                  "WIDTH", "HEIGHT"):
        value = getattr(armoured_walker, const)
        literal = f"{value}F" if const in ("WIDTH", "HEIGHT") else str(value)
        if f" {const} = {literal};" not in java:
            err(f"ArmouredWalker.{const} differs from tools/armoured_walker.py ({literal})")
    mx, my, mz = (v / 16 for v in armoured_walker.MUZZLE)
    if f"MUZZLE = new Vec3({mx:g}, {my:g}, {mz:g});" not in java.replace(".0,", ",").replace(".0)", ")"):
        err(f"ArmouredWalker.MUZZLE differs from tools/armoured_walker.py ({mx}, {my}, {mz})")
    client = ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client"
    joints = [*armoured_walker.HIPS.values(), armoured_walker.SHOULDER, armoured_walker.PISTON]
    for name in ("ArmouredWalkerParts.java",):
        text = (client / name).read_text(encoding="utf-8") if (client / name).is_file() else ""
        for joint in joints:
            if "{" + ", ".join(str(v) for v in joint) + "}" not in text:
                err(f"{name} lacks the joint {joint} from tools/armoured_walker.py")
        if f"PISTON_STROKE = {armoured_walker.PISTON_STROKE}" not in text:
            err(f"{name} lacks PISTON_STROKE = {armoured_walker.PISTON_STROKE} from tools/armoured_walker.py")
    quads = load(ASSETS / "armoured_walker_quads.json") or {}
    for part in armoured_walker.parts():
        if not quads.get(part):
            err(f"armoured_walker_quads.json lacks {part}: run tools/generate_material_data.py")


def check_artillery():
    """artillery/JugcraftArtillery.java against tools/artillery.py: the shells, guns, balloon and range finder, and the
    renderers' pivots and the howitzer's track."""
    java = (JAVA_ROOT / "artillery" / "JugcraftArtillery.java").read_text(encoding="utf-8")
    floats = ("HEAVY_DAMAGE", "FLAK_DAMAGE", "MORTAR_TRAVERSE", "HOWITZER_TRAVERSE", "FLAK_TRAVERSE", "HOWITZER_TURN")
    for const in ("HEAVY_SPEED", "HEAVY_GRAVITY", "HEAVY_RADIUS", "HEAVY_DAMAGE", "FLAK_SPEED", "FLAK_GRAVITY", "FLAK_RADIUS",
                  "FLAK_DAMAGE", "FLAK_FUSE", "FLAK_PROXIMITY", "MORTAR_COOLDOWN", "HOWITZER_COOLDOWN", "FLAK_COOLDOWN",
                  "MORTAR_TRAVERSE", "HOWITZER_TRAVERSE", "FLAK_TRAVERSE", "HOWITZER_ARC", "HOWITZER_SPEED", "HOWITZER_TURN",
                  "HOWITZER_FUEL_TANK", "FUEL_PER_BUCKET", "HOWITZER_FUEL_PER_SECOND", "BALLOON_HEIGHT", "BALLOON_CLIMB",
                  "MARK_RANGE", "MARK_TTL"):
        value = getattr(artillery, const)
        literal = f"{value}F" if const in floats else str(value)
        if f" {const} = {literal};" not in java:
            err(f"JugcraftArtillery.{const} differs from tools/artillery.py ({literal})")
    for name, java_name in (("siege_mortar", "MORTAR_HEALTH"), ("self_propelled_howitzer", "HOWITZER_HEALTH"),
                            ("flak_gun", "FLAK_HEALTH"), ("observation_balloon", "BALLOON_HEALTH")):
        if f" {java_name} = {artillery.HEALTH[name]};" not in java:
            err(f"JugcraftArtillery.{java_name} differs from tools/artillery.py ({artillery.HEALTH[name]})")
    for name, pivot in (("MORTAR_PIVOT_HEIGHT", artillery.MORTAR_TRUNNION), ("HOWITZER_PIVOT_HEIGHT", artillery.HOWITZER_GUN),
                        ("FLAK_PIVOT_HEIGHT", artillery.FLAK_HEAD)):
        if f" {name} = {pivot[1] / 16};" not in java:
            err(f"JugcraftArtillery.{name} should be {pivot[1] / 16} (tools/artillery.py)")
    renderer = (ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client"
                / "ArtilleryRenderers.java")
    text = renderer.read_text(encoding="utf-8") if renderer.is_file() else ""
    for name in ("MORTAR_TURNTABLE", "MORTAR_TRUNNION", "HOWITZER_GUN", "FLAK_HEAD"):
        pivot = getattr(artillery, name)
        if f"{name} = " + "{" + ", ".join(str(v) for v in pivot) + "}" not in text:
            err(f"ArtilleryRenderers.{name} differs from tools/artillery.py ({pivot})")
    for z, y in artillery.HOWITZER_TRACK:
        if "{" + f"{z}, {y}" + "}" not in text:
            err(f"ArtilleryRenderers.HOWITZER_TRACK lacks ({z}, {y}) from tools/artillery.py")
    x0, x1 = artillery.HOWITZER_TRACK_X
    if f"HOWITZER_TRACK_INNER = {x0};" not in text or f"HOWITZER_TRACK_OUTER = {x1};" not in text:
        err("ArtilleryRenderers' howitzer track span differs from tools/artillery.py")
    if not (ASSETS / "artillery_quads.json").is_file():
        err("assets/jugcraft/artillery_quads.json is missing: run tools/generate_material_data.py")


def check_tower_guns():
    """artillery/JugcraftTowerGuns.java and client/TowerGunRenderer.java against tools/tower_guns.py: the Great Shell,
    each gun's spec line and the renderer's pivots, and that every gun fits its footprint."""
    java = (JAVA_ROOT / "artillery" / "JugcraftTowerGuns.java").read_text(encoding="utf-8")
    for const in ("GREAT_SPEED", "GREAT_GRAVITY", "GREAT_RADIUS", "GREAT_DAMAGE"):
        value = getattr(tower_guns, const)
        literal = f"{value}F" if const == "GREAT_DAMAGE" else str(value)
        if f" {const} = {literal};" not in java:
            err(f"JugcraftTowerGuns.{const} differs from tools/tower_guns.py ({literal})")
    renderer = (ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client"
                / "TowerGunRenderer.java")
    text = renderer.read_text(encoding="utf-8") if renderer.is_file() else ""
    for gun, g in tower_guns.GUNS.items():
        if tower_guns.java_spec(gun) not in java:
            err(f"JugcraftTowerGuns' spec for {gun} differs from tools/tower_guns.py: {tower_guns.java_spec(gun)}")
        if tower_guns.java_pivots(gun) not in text:
            err(f"TowerGunRenderer's pivots for {gun} differ from tools/tower_guns.py: {tower_guns.java_pivots(gun)}")
        base, _, _ = tower_guns.PARTS[gun]
        reach = max(max(abs(v) for v in (f[0], t[0], f[2], t[2])) for f, t, *_ in base())
        if reach > g["size"] * 8:
            err(f"{gun}'s plinth reaches {reach} pixels from its centre, past its {g['size']}x{g['size']} footprint")
        if g["shell"] not in ("heavy", "flak", "great"):
            err(f"{gun} fires an unknown shell {g['shell']}")
    if not (ASSETS / "tower_gun_quads.json").is_file():
        err("assets/jugcraft/tower_gun_quads.json is missing: run tools/generate_material_data.py")


def _renderer(name):
    path = ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client" / f"{name}.java"
    return path.read_text(encoding="utf-8") if path.is_file() else ""


def check_gun_culling(reach):
    """Each big gun's, the Landship's and the Diesel Walker's culling box (its renderer's getBoundingBoxForCulling, the
    entity's box inflated) holds the whole model over its travel, or the game stops drawing a raised barrel whenever the
    gun's own box is out of view (5 October 2026: the owner found barrels "invisible")."""
    pattern = r"getBoundingBoxForCulling\(%s \w+, float partialTick\) \{[^}]*?inflate\(([\d.]+), ([\d.]+), ([\d.]+)\)"
    sizes = {gun: (g["size"] - 0.1, g["height"], "TowerGunRenderer", "TowerGun") for gun, g in tower_guns.GUNS.items()}
    registry = (JAVA_ROOT / "artillery" / "JugcraftArtillery.java").read_text(encoding="utf-8")
    for gun, cls in (("siege_mortar", "SiegeMortar"), ("self_propelled_howitzer", "SelfPropelledHowitzer"),
                     ("flak_gun", "FlakGun")):
        found = re.search(r'entity\("%s",[^;]*?\.sized\(([\d.]+)F, ([\d.]+)F\)' % gun, registry)
        if not found:
            err(f"JugcraftArtillery registers no size for {gun}")
            continue
        sizes[gun] = (float(found.group(1)), float(found.group(2)), "ArtilleryRenderers", cls)
    sizes["landship"] = (landship.WIDTH, landship.HEIGHT, "LandshipRenderer", "Landship")
    sizes["diesel_walker"] = (mech.WIDTH, mech.HEIGHT, "DieselWalkerRenderer", "DieselWalker")
    for name, (top, bottom, radius) in reach.items():
        width, height, renderer, cls = sizes[name]
        found = re.search(pattern % cls, _renderer(renderer))
        if not found:
            err(f"{renderer} has no culling box for {cls}")
            continue
        across, up = min(float(found.group(1)), float(found.group(3))), float(found.group(2))
        if top / 16 > height + up or -bottom / 16 > up or radius / 16 > width / 2 + across:
            err(f"{renderer}'s culling box for {cls} (inflate {found.group(1)}, {up}, {found.group(3)}) does not hold "
                f"{name} over its travel: it reaches {top / 16:.2f} blocks up and {radius / 16:.2f} out")


def check_gun_renderers():
    """The renderers pose the guns, the Landship and the Diesel Walker as tools/gun_poses.py checks them: the recoil,
    elevation and swing numbers match, and each cradle is drawn before the recoil and its barrel after."""
    text = _renderer("ArtilleryRenderers")
    for cls, recoil in (("SiegeMortar", artillery.MORTAR_RECOIL), ("SelfPropelledHowitzer", artillery.HOWITZER_RECOIL),
                        ("FlakGun", artillery.FLAK_RECOIL)):
        if not re.search(r"extractRenderState\(%s gun, GunState state, float partialTick\) \{[^}]*?extract\(gun, state, "
                         r"partialTick, %s\)" % (cls, f"{float(recoil)}F"), text):
            err(f"ArtilleryRenderers' recoil for {cls} differs from tools/artillery.py ({recoil})")
    for cls, (low, high) in (("SiegeMortar", artillery.MORTAR_PITCH), ("SelfPropelledHowitzer", artillery.HOWITZER_PITCH),
                             ("FlakGun", artillery.FLAK_PITCH)):
        java = (JAVA_ROOT / "artillery" / f"{cls}.java").read_text(encoding="utf-8")
        for limit, value in (("minPitch", low), ("maxPitch", high)):
            if not re.search(r"float %s\(\) \{\s*return %s;" % (limit, f"{float(value)}F"), java):
                err(f"{cls}.{limit} differs from tools/artillery.py ({value})")
    for cradle, barrel in (("mortar_cradle", "mortar_barrel"), ("howitzer_gun", "howitzer_barrel"), ("flak_head", "flak_barrels")):
        if not re.search(r'draw\("%s"[^;]*;\s*pose\.translate\(0, 0, -state\.recoil / 16\.0F\);\s*draw\("%s"' % (cradle, barrel), text):
            err(f"ArtilleryRenderers must draw {cradle} before the recoil and {barrel} after it")
    if not re.search(r'draw\(state\.id \+ "_cradle"[^;]*;\s*pose\.translate\(0, 0, -state\.recoil / 16\.0F\);\s*'
                     r'draw\(state\.id \+ "_barrel"', _renderer("TowerGunRenderer")):
        err("TowerGunRenderer must draw each gun's cradle before the recoil and its barrel after it")
    ship = (JAVA_ROOT / "landship" / "Landship.java").read_text(encoding="utf-8")
    for name, value in (("BARREL_UP", landship.BARREL_PITCH[0]), ("BARREL_DOWN", landship.BARREL_PITCH[1])):
        if f" {name} = {float(value)}F;" not in ship:
            err(f"Landship.{name} differs from tools/landship.py BARREL_PITCH ({value})")
    if f"getY() + {round((landship.STACK_TOP + 1) / 16, 1)}," not in ship:
        err(f"Landship's stack smoke should rise from getY() + {round((landship.STACK_TOP + 1) / 16, 1)} (tools/landship.py "
            f"STACK_TOP)")
    if f" RECOIL = {float(landship.RECOIL)}F;" not in _renderer("LandshipRenderer"):
        err(f"LandshipRenderer.RECOIL differs from tools/landship.py ({landship.RECOIL})")
    walker = _renderer("DieselWalkerRenderer")
    for line in (f" LEG_SWING = {float(mech.LEG_SWING)}F;", f" PUNCH_SWING = {float(mech.PUNCH_SWING)}F;",
                 f"legSwing * {mech.ARM_FOLLOW}F", f"state.drilling ? -{float(mech.DRILL_RAISE)}F"):
        if line not in walker:
            err(f"DieselWalkerRenderer lacks '{line.strip()}' from tools/mech.py")


def check_quad_names():
    """client/DecorQuads loads every quad file it lists into one table by part name, later files overwriting earlier
    ones, so a part name used in two files draws the wrong model (until 5 October 2026 the Observation Balloon's basket
    replaced the hot-air balloons'). Every listed file must exist and every part name must be unique across them."""
    java = ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client" / "DecorQuads.java"
    files = re.findall(r'Jugcraft\.id\("([a-z0-9_]+\.json)"\)', java.read_text(encoding="utf-8")) if java.is_file() else []
    if not files:
        err("client/DecorQuads.java lists no quad files")
    owner = {}
    for name in files:
        parts = load(ASSETS / name)
        if parts is None:
            err(f"assets/{MOD}/{name}, listed in DecorQuads, is missing: run tools/generate_material_data.py")
            continue
        for part in parts:
            if part in owner:
                err(f"Quad part '{part}' is in both {owner[part]} and {name}: DecorQuads would draw the second for both")
            owner.setdefault(part, name)


def check_gun_art():
    """The 5 October 2026 art fixes for the big guns, the Landship, the Diesel Walker and the balloons stay fixed: no
    moving gun part flickers against or cuts through its mount over its whole travel (tools/gun_poses.py); every quad
    texture exists and block ones are opaque; each decal (port cover, hazard sign, bore) is drawn whole, UV 0 to 1; the
    Observation Balloon's envelope is a closed, outward-facing mesh on its own opaque texture; nothing opaque is drawn
    through the translucent "nocull" path (opaque two-sided quads are "cutout", which 26.3 draws from both sides); and every
    war machine's icon is its 16x16 map in tools/item_icons/, committed as drawn (CI does not regenerate textures;
    tools/check_icon_maps.py checks the maps themselves against docs/ITEM_ICONS.md)."""
    import gun_poses
    import item_icons
    kinds = sorted(set(artillery.ITEMS) | set(tower_guns.items()) | set(landship.ITEMS) | set(mech.ITEMS)
                   | set(zeppelin.ITEMS))
    for kind in kinds:
        if not item_icons.has(kind):
            err(f"tools/item_icons/{kind}.txt is missing: every war machine's icon is a 16x16 map (docs/ITEM_ICONS.md)")
            continue
        path = ASSETS / "textures" / "item" / f"{kind}.png"
        if not path.is_file():
            err(f"textures/item/{kind}.png is missing: run tools/generate_textures.py")
            continue
        try:
            drawn = item_icons.draw(kind)
        except ValueError as problem:
            err(f"tools/item_icons.py cannot draw {kind}: {problem}")
            continue
        with Image.open(path) as img:
            saved = img.convert("RGBA")
        if saved.size != drawn.size or saved.tobytes() != drawn.tobytes():
            err(f"textures/item/{kind}.png differs from its map in tools/item_icons/: run tools/generate_textures.py")
    files = ("artillery_quads.json", "tower_gun_quads.json", "landship_quads.json", "walker_quads.json", "balloon_quads.json")
    quads = {name: load(ASSETS / name) or {} for name in files}
    opacity = {}

    def alpha(texture):
        if texture not in opacity:
            path = ASSETS / "textures" / (f"{texture}.png" if "/" in texture else f"block/{texture}.png")
            if not path.is_file():
                opacity[texture] = None
            else:
                with Image.open(path) as img:
                    histogram = img.convert("RGBA").getchannel("A").histogram()
                    lowest = next(v for v in range(256) if histogram[v])
                    opacity[texture] = (lowest, any(histogram[1:255]))
        return opacity[texture]

    for name, parts in quads.items():
        for part, qs in parts.items():
            for quad in qs:
                texture = quad["texture"]
                found = alpha(texture)
                if found is None:
                    err(f"{name}'s {part} draws missing texture {texture}")
                    break
                if quad.get("nocull") and not found[1]:
                    err(f"{name}'s {part} draws {texture}, which has no part-transparent pixels, through the translucent "
                        f"'nocull' path: flag it 'cutout' (26.3's entityCutout draws both sides)")
                    break
                if "/" not in texture and not quad.get("cutout") and not quad.get("nocull") and found[0] < 255:
                    err(f"{name}'s {part} draws {texture}, which has see-through pixels, as a solid quad")
                    break
                if texture in ("tg_port", "tg_warning", "tg_bore"):
                    us = [v[3] for v in quad["vertices"]]
                    vs = [v[4] for v in quad["vertices"]]
                    if min(us) != 0 or max(us) != 1 or min(vs) != 0 or max(vs) != 1:
                        err(f"{name}'s {part} cuts the {texture} decal (UV {min(us)}..{max(us)}, {min(vs)}..{max(vs)}): "
                            f"give it a '!' face")
                        break
    moving = {}
    for name in ("artillery_quads.json", "tower_gun_quads.json", "landship_quads.json", "walker_quads.json"):
        moving.update(quads[name])
    needed = [part for part in gun_poses.sources(artillery, tower_guns, landship, mech) if part not in moving]
    if needed:
        err(f"Quads missing for {', '.join(needed[:6])}: run tools/generate_material_data.py")
    else:
        found = gun_poses.problems(artillery, tower_guns, landship, mech, moving)
        for problem in found[:12]:
            err(problem)
        if len(found) > 12:
            err(f"... and {len(found) - 12} more poses where a gun, the Landship or the Diesel Walker flickers or cuts "
                f"through itself")
        check_gun_culling(gun_poses.reach(artillery, tower_guns, landship, mech, moving))
    check_gun_renderers()
    envelope = quads["artillery_quads.json"].get("balloon_envelope", [])
    path = ASSETS / "textures" / f"{artillery.ENVELOPE_TEXTURE}.png"
    if not path.is_file():
        err(f"The Observation Balloon's envelope texture {artillery.ENVELOPE_TEXTURE} is missing: run tools/generate_textures.py")
    elif Image.open(path).size != artillery.ENVELOPE_SIZE:
        err(f"{path.relative_to(ROOT)} must be {artillery.ENVELOPE_SIZE[0]} by {artillery.ENVELOPE_SIZE[1]}")
    edges = {}
    for quad in envelope:
        if quad["texture"] != artillery.ENVELOPE_TEXTURE or quad.get("nocull") or quad.get("cutout"):
            err("The Observation Balloon's envelope must be plain (culled) quads on its own texture")
            break
        v = [tuple(round(c, 2) for c in p[:3]) for p in quad["vertices"]]
        a = [v[2][k] - v[0][k] for k in range(3)]
        b = [v[3][k] - v[1][k] for k in range(3)]
        n = (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])
        if sum(n[k] * quad["normal"][k] for k in range(3)) <= 0:
            err("An Observation Balloon envelope quad is wound against its normal (it would be culled from outside)")
            break
        for k in range(4):
            if v[k] != v[(k + 1) % 4]:
                edges[(v[k], v[(k + 1) % 4])] = edges.get((v[k], v[(k + 1) % 4]), 0) + 1
    open_edges = sum(1 for (p, q), count in edges.items() if count != 1 or edges.get((q, p)) != 1)
    if envelope and open_edges:
        err(f"The Observation Balloon's envelope is not closed: {open_edges} edges lack a matching neighbour")
def check_raiders():
    """raiders/*.java against tools/raiders.py: every kind's stats, the weapons' and raids' numbers, the party table and
    the generated skins, paint and quads."""
    folder = JAVA_ROOT / "raiders"
    java = (folder / "JugcraftRaiders.java").read_text(encoding="utf-8")
    raids = (folder / "RaiderRaids.java").read_text(encoding="utf-8")
    for kind, (_, hp, dmg, armour, speed) in raiders.INFANTRY.items():
        if f'infantry("{kind}", {hp}, {dmg}, {armour}, {speed:.2f});' not in java:
            err(f"JugcraftRaiders.java does not give {kind} the stats tools/raiders.py does")
    for kind, (_, hp, dmg, armour, speed, width, height) in raiders.MACHINES.items():
        if f'machine("{kind}", {hp}, {dmg}, {armour}, {speed:.2f},' not in java or f".sized({width}F, {height}F)" not in java:
            err(f"JugcraftRaiders.java does not give {kind} the stats or size tools/raiders.py does")
    numbers = {"GRENADE_RADIUS": raiders.GRENADE_BLAST[0], "GRENADE_DAMAGE": f"{raiders.GRENADE_BLAST[1]}F",
               "BOMB_RADIUS": raiders.BOMB_BLAST[0], "BOMB_DAMAGE": f"{raiders.BOMB_BLAST[1]}F"}
    for const in ("GRENADE_COOLDOWN", "GRENADE_MIN_RANGE", "GRENADE_MAX_RANGE", "RALLY_TICKS", "RALLY_RADIUS", "RALLY_EFFECT",
                  "ROUT_TICKS", "WALKER_PUNCH_COOLDOWN", "WALKER_LAUNCH_COOLDOWN", "WALKER_LAUNCH_MIN", "WALKER_LAUNCH_MAX",
                  "BLIMP_CRUISE", "BLIMP_BOMB_COOLDOWN", "BLIMP_BOMB_REACH", "LADDER_STUCK", "LADDER_MAX", "LADDER_TTL"):
        numbers[const] = getattr(raiders, const)
    for const, value in numbers.items():
        if f" {const} = {value};" not in java:
            err(f"JugcraftRaiders.{const} differs from tools/raiders.py ({value})")
    for const in ("RAID_CHECK_TICKS", "SPAWN_MIN", "SPAWN_MAX", "RAID_TIMEOUT", "ABANDON_TICKS", "ABANDON_RANGE", "MAX_LEVEL"):
        if f" {const} = {getattr(raiders, const)};" not in raids:
            err(f"RaiderRaids.{const} differs from tools/raiders.py ({getattr(raiders, const)})")
    camps = (folder / "RaiderCamps.java").read_text(encoding="utf-8")
    if f" RARITY = {raiders.CAMP_RARITY};" not in camps:
        err(f"RaiderCamps.RARITY differs from tools/raiders.py CAMP_RARITY ({raiders.CAMP_RARITY})")
    if f" SPAWN_CLEARANCE = {raiders.CAMP_SPAWN_CLEARANCE};" not in camps:
        err(f"RaiderCamps.SPAWN_CLEARANCE differs from tools/raiders.py CAMP_SPAWN_CLEARANCE ({raiders.CAMP_SPAWN_CLEARANCE})")
    if f" RAID_CHANCE = {raiders.RAID_CHANCE}F;" not in raids:
        err(f"RaiderRaids.RAID_CHANCE differs from tools/raiders.py ({raiders.RAID_CHANCE})")
    order = ["raider_grunt", "raider_grenadier", "raider_officer", "raider_blimp", "raider_walker"]
    table = ", ".join("{" + ", ".join(str(raiders.party(level)[k]) for k in order) + "}" for level in range(1, raiders.MAX_LEVEL + 1))
    if f"PARTY = {{{table}}};" not in raids:
        err(f"RaiderRaids.PARTY differs from party() in tools/raiders.py ({table})")
    config = (JAVA_ROOT / "config" / "JugcraftConfig.java").read_text(encoding="utf-8")
    for key, default in raiders.OPTIONS.items():
        if f'Map.entry("{key}", "{default}")' not in config:
            err(f"JugcraftConfig.TEXT_OPTIONS lacks {key} (default {default})")
    for skin in raiders.SKINS:
        if not (ASSETS / "textures" / "entity" / "raider" / f"{skin}.png").is_file():
            err(f"textures/entity/raider/{skin}.png is missing: run tools/generate_textures.py")
    quads = load(ASSETS / "raider_quads.json") or {}
    for part in ("raider_walker_hull", "raider_walker_lamps", "raider_walker_leg", "raider_walker_tool_arm",
                 "raider_walker_piston_base", "raider_walker_piston_head", "raider_blimp_body", "raider_blimp_propeller"):
        if not quads.get(part):
            err(f"raider_quads.json lacks {part}: run tools/generate_material_data.py")
    for quad_list in quads.values():
        for quad in quad_list:
            if not (ASSETS / "textures" / "block" / f"{quad['texture']}.png").is_file() and "/" not in quad["texture"]:
                err(f"raider_quads.json uses a missing texture {quad['texture']}")
                return
    for entity in raiders.ENTITIES:
        if f'"{entity}"' not in java:
            err(f"JugcraftRaiders.java does not register {entity}")


def check_bunkerworks():
    """building/Bunkerworks.java against tools/bunkerworks.py: the list, kinds and strengths, the periscope's and the map
    table's numbers, the lights, Trench Stew's food, effect and pot recipe, and the field kitchen as a heat source."""
    java = (JAVA_ROOT / "building" / "Bunkerworks.java").read_text(encoding="utf-8")
    for block, (_, kind, hardness, blast) in bunkerworks.BLOCKS.items():
        if f'entry("{block}", "{kind}", {hardness}F, {blast}F);' not in java:
            err(f"Bunkerworks.java does not register {block} as tools/bunkerworks.py does ({kind}, {hardness}, {blast})")
    for const in ("PERISCOPE_INTERVAL", "PERISCOPE_RANGE", "PERISCOPE_CONE", "MAP_RANGE", "MAP_LINK", "MAP_LINES",
                  "KITCHEN_LIGHT", "LAMP_LIGHT"):
        if f" {const} = {getattr(bunkerworks, const)};" not in java:
            err(f"Bunkerworks.{const} differs from tools/bunkerworks.py ({getattr(bunkerworks, const)})")
    stew = bunkerworks.TRENCH_STEW
    food = ag.ITEMS.get("trench_stew", {})
    if food.get("food") != stew["food"] or food.get("stew_effect") != stew["effect"] or not food.get("stew"):
        err("tools/agriculture.py ITEMS trench_stew differs from tools/bunkerworks.py TRENCH_STEW")
    if ag.POT_RECIPES.get("trench_stew") != {"inputs": stew["inputs"], "time": stew["time"]}:
        err("tools/agriculture.py POT_RECIPES trench_stew differs from tools/bunkerworks.py TRENCH_STEW")
    main = (JAVA_ROOT / "agriculture" / "JugcraftAgriculture.java").read_text(encoding="utf-8")
    n, sat = stew["food"]
    effect, seconds = stew["effect"]
    if f'stew("trench_stew", {n}, {sat}F, MobEffects.{effect}, {seconds});' not in main:
        err("JugcraftAgriculture.java does not register trench_stew as tools/bunkerworks.py TRENCH_STEW says")
    heat = (load(DATA / MOD / "tags" / "block" / "heat_sources.json") or {}).get("values", [])
    if f"{MOD}:field_kitchen" not in heat:
        err("The Field Kitchen must be in the block tag jugcraft:heat_sources")
    for block in bunkerworks.TALL:
        table = load(DATA / MOD / "loot_table" / "blocks" / f"{block}.json") or {}
        if '"half": "lower"' not in json.dumps(table):
            err(f"{block} is two blocks tall: its loot table must drop it from the lower half only")
    # The gas curtain's line of sight is walked in steps no longer than a quarter block.
    curtain = (JAVA_ROOT / "building" / "GasCurtainBlock.java").read_text(encoding="utf-8")
    if "STEP = 0.25;" not in curtain:
        err("GasCurtainBlock.STEP must be 0.25 (blocks between the points it looks at)")


def check_fire_control():
    """building/FireControl.java against tools/fire_control.py: the table's strength, its modes and sectors, and every
    number it and the guns it lays work by."""
    java = (JAVA_ROOT / "building" / "FireControl.java").read_text(encoding="utf-8")
    table = (JAVA_ROOT / "building" / "FireControlTableBlock.java").read_text(encoding="utf-8")
    for block, (_, hardness, blast) in fire_control.BLOCKS.items():
        if f".strength({hardness}F, {blast}F)" not in java:
            err(f"FireControl.java does not give {block} the strength tools/fire_control.py does ({hardness}, {blast})")
    for const in ("MAX_GUNS", "LINK_RANGE", "SHEAF_SPACING", "SENTRY_RANGE", "SENTRY_MIN_RANGE", "CHECK_FIRE", "SENTRY_SCAN",
                  "TABLE_INTERVAL", "CREEP_STEP", "CREEP_STEPS"):
        if f" {const} = {getattr(fire_control, const)};" not in java:
            err(f"FireControl.{const} differs from tools/fire_control.py ({getattr(fire_control, const)})")
    sectors = ", ".join(str(s) for s in fire_control.SECTORS)
    if f"SECTORS = {{{sectors}}};" not in java:
        err(f"FireControl.SECTORS differs from tools/fire_control.py ({sectors})")
    modes = ", ".join(f'{m.upper()}("{m}")' for m in fire_control.MODES)
    if modes not in table:
        err(f"FireControlTableBlock.Mode differs from tools/fire_control.py ({modes})")


def check_fortifications():
    """building/Fortifications.java against tools/fortifications.py: the list, kinds and strengths, the hoist's and the
    rack's numbers, and that the ready rack's shell tag lists every shell a gun fires."""
    java = (JAVA_ROOT / "building" / "Fortifications.java").read_text(encoding="utf-8")
    for block, (_, kind, hardness, blast) in fortifications.BLOCKS.items():
        if f'entry("{block}", "{kind}", {hardness}F, {blast}F);' not in java:
            err(f"Fortifications.java does not register {block} as tools/fortifications.py does ({kind}, {hardness}, {blast})")
    for const in ("HOIST_INTERVAL", "HOIST_BATCH", "HOIST_BUFFER", "RACK_SLOTS", "RACK_REACH"):
        if f" {const} = {getattr(fortifications, const)};" not in java:
            err(f"Fortifications.{const} differs from tools/fortifications.py ({getattr(fortifications, const)})")
    tag = set((load(DATA / MOD / "tags" / "item" / "artillery_shells.json") or {}).get("values", []))
    for shell in ("heavy_shell", "flak_shell", "great_shell"):
        if f"{MOD}:{shell}" not in tag:
            err(f"#{MOD}:artillery_shells lacks {shell}, so ready racks would not hold it")


def check_landship():
    """landship/Landship.java against tools/landship.py: the driving, gun, fuel and size numbers, and the renderer's
    track path, link pitch and pivots."""
    java = (JAVA_ROOT / "landship" / "Landship.java").read_text(encoding="utf-8")
    for const in ("SPEED", "TURN", "CANNON_COOLDOWN", "SHELL_SPEED", "SHELL_RADIUS", "SHELL_DAMAGE", "GUN_INTERVAL",
                  "GUN_DAMAGE", "GUN_RANGE", "CRUSH_DAMAGE", "FUEL_TANK", "FUEL_PER_BUCKET", "FUEL_PER_SECOND", "HEALTH",
                  "SEATS", "WIDTH", "HEIGHT"):
        value = getattr(landship, const)
        literal = f"{value}F" if const in ("TURN", "WIDTH", "HEIGHT", "SHELL_DAMAGE", "GUN_DAMAGE", "CRUSH_DAMAGE") else str(value)
        if f" {const} = {literal};" not in java:
            err(f"Landship.{const} differs from tools/landship.py ({literal})")
    renderer = (ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client"
                / "LandshipRenderer.java")
    text = renderer.read_text(encoding="utf-8") if renderer.is_file() else ""
    for z, y in landship.TRACK_PATH:
        if "{" + f"{z}, {y}" + "}" not in text:
            err(f"LandshipRenderer.TRACK_PATH lacks ({z}, {y}) from tools/landship.py")
    for name, joint in (("TURRET", landship.TURRET), ("BARREL", landship.BARREL)):
        if f"{name} = " + "{" + ", ".join(str(v) for v in joint) + "}" not in text:
            err(f"LandshipRenderer.{name} differs from tools/landship.py ({joint})")
    for name, value in (("LINK_PITCH", landship.LINK_PITCH), ("TRACK_INNER", landship.TRACK_INNER),
                        ("TRACK_OUTER", landship.TRACK_OUTER)):
        if f" {name} = {value};" not in text:
            err(f"LandshipRenderer.{name} differs from tools/landship.py ({value})")
    if not (ASSETS / "landship_quads.json").is_file():
        err("assets/jugcraft/landship_quads.json is missing: run tools/generate_material_data.py")


def check_dieselworks():
    """building/Dieselworks.java against tools/dieselworks.py: every block, its kind and its strength."""
    java = (JAVA_ROOT / "building" / "Dieselworks.java").read_text(encoding="utf-8")
    for block, (_, kind, hardness, blast) in dieselworks.BLOCKS.items():
        if f'"{block}", "{kind}", {hardness}F, {blast}F' not in java:
            err(f"Dieselworks.java: {block} is not registered as {kind}, strength({hardness}F, {blast}F)")
    if f"int LAMP_LIGHT = {dieselworks.LAMP_LIGHT};" not in java:
        err(f"Dieselworks.LAMP_LIGHT differs from tools/dieselworks.py ({dieselworks.LAMP_LIGHT})")


def check_trenchworks():
    """building/Trenchworks.java and its blocks against tools/trenchworks.py: kinds, strengths and the numbers."""
    building = JAVA_ROOT / "building"
    java = (building / "Trenchworks.java").read_text(encoding="utf-8")
    for block, (_, kind, hardness, blast) in trenchworks.BLOCKS.items():
        if f'"{block}", "{kind}", {hardness}F, {blast}F' not in java:
            err(f"Trenchworks.java: {block} is not registered as {kind}, strength({hardness}F, {blast}F)")
    for const, literal in (("WIRE_SLOW", f"{trenchworks.WIRE_SLOW}"), ("WIRE_DAMAGE", f"{trenchworks.WIRE_DAMAGE}F"),
                           ("PHONE_RANGE", f"{trenchworks.PHONE_RANGE}"), ("SEARCHLIGHT_YAWS", f"{trenchworks.SEARCHLIGHT_YAWS}"),
                           ("SEARCHLIGHT_TILTS", f"{trenchworks.SEARCHLIGHT_TILTS}"),
                           ("SEARCHLIGHT_LIGHT", f"{trenchworks.SEARCHLIGHT_LIGHT}")):
        if f" {const} = {literal};" not in java:
            err(f"Trenchworks.{const} differs from tools/trenchworks.py ({literal})")
    renderer = (ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client"
                / "SearchlightRenderer.java")
    pivot = "PIVOT = {" + ", ".join(str(v) for v in trenchworks.PIVOT) + "}"
    if not renderer.is_file() or pivot not in renderer.read_text(encoding="utf-8"):
        err(f"SearchlightRenderer lacks {pivot} from tools/trenchworks.py")
    if not (ASSETS / "trench_quads.json").is_file():
        err("assets/jugcraft/trench_quads.json is missing: run tools/generate_material_data.py")


def check_kaiserworks():
    """building/Kaiserworks.java against tools/kaiserworks.py: every block, its kind and its strength."""
    java = (JAVA_ROOT / "building" / "Kaiserworks.java").read_text(encoding="utf-8")
    for block, (_, kind, hardness, blast) in kaiserworks.BLOCKS.items():
        if f'"{block}", "{kind}", {hardness}F, {blast}F' not in java:
            err(f"Kaiserworks.java: {block} is not registered as {kind}, strength({hardness}F, {blast}F)")
    if f"int LAMP_LIGHT = {kaiserworks.LAMP_LIGHT};" not in java:
        err(f"Kaiserworks.LAMP_LIGHT differs from tools/kaiserworks.py ({kaiserworks.LAMP_LIGHT})")


def check_rocketry():
    """rocketry/ against tools/rocketry.py: the numbers."""
    java = (JAVA_ROOT / "rocketry" / "JugcraftRocketry.java").read_text(encoding="utf-8")
    for const in ("LAUNCH_DELAY", "SURVEY_RADIUS", "SURVEY_STRIDE", "WEATHER_TICKS", "WEATHER_COOLDOWN", "FLARE_RADIUS",
                  "GLOW_TICKS", "SIGNAL_RANGE", "COOLDOWN"):
        value = getattr(rocketry, const)
        if f"int {const} = {value:_};" not in java and f"int {const} = {value};" not in java:
            err(f"JugcraftRocketry.{const} differs from tools/rocketry.py ({value})")
    for item in list(rocketry.ITEMS) + list(rocketry.BLOCKS):
        if f'"{item}"' not in java:
            err(f"JugcraftRocketry does not register {item}")
    post = (JAVA_ROOT / "rocketry" / "RocketPost.java").read_text(encoding="utf-8")
    for const, value in (("RANGE", rocketry.POST_RANGE), ("MIN_FLIGHT", rocketry.POST_MIN_FLIGHT),
                         ("BLOCKS_PER_TICK", rocketry.POST_BLOCKS_PER_TICK), ("CHECK_INTERVAL", rocketry.POST_CHECK_INTERVAL)):
        if f"int {const} = {value:_};" not in post and f"int {const} = {value};" not in post:
            err(f"RocketPost.{const} differs from tools/rocketry.py ({value})")
    booster = (JAVA_ROOT / "rocketry" / "BoosterRailBlockEntity.java").read_text(encoding="utf-8")
    for const in ("BOOST_TICKS", "CHARGES_PER_PROPELLANT", "MAX_CHARGES"):
        if f"int {const} = {getattr(rocketry, const)};" not in booster:
            err(f"BoosterRailBlockEntity.{const} differs from tools/rocketry.py ({getattr(rocketry, const)})")
    launcher = (JAVA_ROOT / "rocketry" / "RocketLauncherItem.java").read_text(encoding="utf-8")
    for const, value in (("COOLDOWN", rocketry.LAUNCHER_COOLDOWN), ("HOMING_RANGE", rocketry.HOMING_RANGE)):
        if f"int {const} = {value};" not in launcher:
            err(f"RocketLauncherItem.{const} differs from tools/rocketry.py ({value})")
    if f"int LIFETIME = {rocketry.ROCKET_LIFETIME};" not in (JAVA_ROOT / "rocketry" / "CombatRocket.java").read_text(encoding="utf-8"):
        err(f"CombatRocket.LIFETIME differs from tools/rocketry.py ({rocketry.ROCKET_LIFETIME})")
    anchor = (JAVA_ROOT / "rocketry" / "ZiplineAnchorBlockEntity.java").read_text(encoding="utf-8")
    for const, value in (("RANGE", rocketry.LINE_RANGE), ("REACH", rocketry.ANCHOR_REACH)):
        if f"int {const} = {value};" not in anchor:
            err(f"ZiplineAnchorBlockEntity.{const} differs from tools/rocketry.py ({value})")
    for result in rocketry.LINE_RESULTS:
        if result.upper() not in anchor:
            err(f"ZiplineAnchorBlockEntity.Result lacks {result.upper()}")
    pad = (JAVA_ROOT / "rocketry" / "RocketPadBlockEntity.java").read_text(encoding="utf-8")
    if f"int CARGO = {rocketry.PAD_CARGO};" not in pad:
        err(f"RocketPadBlockEntity.CARGO differs from tools/rocketry.py ({rocketry.PAD_CARGO})")
    for result in rocketry.PAD_RESULTS:
        if result.upper() not in pad:
            err(f"RocketPadBlockEntity.Result lacks {result.upper()}")


def check_construction():
    """chemistry/ConstructionChemistry.java against tools/construction.py: the numbers, blocks and items."""
    java = (JAVA_ROOT / "chemistry" / "ConstructionChemistry.java").read_text(encoding="utf-8")
    for const in ("SPRAY_RANGE", "SPRAY_BLOCKS", "SPRAY_RADIUS", "SPRAY_COOLDOWN", "CANISTER_FOAM"):
        value = getattr(construction, const)
        m = re.search(rf"\b{const} = ([0-9_.]+)F?;", java)
        if not m or float(m.group(1).replace("_", "")) != float(value):
            err(f"ConstructionChemistry.{const} differs from tools/construction.py ({value})")
    for block, (_, hardness, blast, variants) in construction.BLOCKS.items():
        if f'"{block}"' not in java:
            err(f"ConstructionChemistry does not register {block}")
        if f"strength({hardness}F, {blast}F)" not in java:
            err(f"ConstructionChemistry: {block} is not strength({hardness}F, {blast}F) as in tools/construction.py")
    for item in construction.items():
        if f'item("{item}"' not in java:
            err(f"ConstructionChemistry does not register {item}")


def check_field_chemistry():
    """weapons/FieldChemistry.java against tools/field_chemistry.py: the numbers, items, cloud entity and damage types."""
    java = (JAVA_ROOT / "weapons" / "FieldChemistry.java").read_text(encoding="utf-8")
    for const in ("CHLORINE_RADIUS", "CHLORINE_TICKS", "CHLORINE_DAMAGE", "SMOKE_RADIUS", "SMOKE_TICKS", "THERMITE_RADIUS",
                  "THERMITE_TICKS", "THERMITE_DAMAGE", "THERMITE_FIRE_SECONDS", "FLASH_RADIUS", "FLASH_BLIND_TICKS",
                  "FLASH_STUN_TICKS", "GAS_MASK_DURABILITY", "SCUBA_GAS_OXYGEN", "FIRST_AID_COOLDOWN", "STIMULANT_TICKS"):
        value = getattr(field_chemistry, const)
        m = re.search(rf"\b{const} = ([0-9_.]+)F?;", java)
        if not m or float(m.group(1).replace("_", "")) != float(value):
            err(f"FieldChemistry.{const} differs from tools/field_chemistry.py ({value})")
    for item in field_chemistry.items():
        if f'item("{item}"' not in java:
            err(f"FieldChemistry does not register {item}")
        if not (ASSETS / "textures" / "item" / f"{item}.png").is_file():
            err(f"missing texture item/{item}.png")
    if 'Jugcraft.id("chemical_cloud")' not in java:
        err("FieldChemistry does not register the chemical cloud")
    for name in field_chemistry.DAMAGE_TYPES:
        if f'Jugcraft.id("{name}")' not in java or not (DATA / MOD / "damage_type" / f"{name}.json").is_file():
            err(f"damage type {MOD}:{name} is not registered in Java and data")
    if not (ASSETS / "textures" / "entity" / "equipment" / "humanoid" / "gas_mask.png").is_file():
        err("missing the worn gas mask texture")


def check_grapple():
    """gear/JugcraftGrapple.java against tools/grapple.py: the numbers, the item and hook entity, their textures and names."""
    java = (JAVA_ROOT / "gear" / "JugcraftGrapple.java").read_text(encoding="utf-8")
    for const in ("CAPACITY", "SHOT_COST", "RANGE", "COOLDOWN"):
        value = getattr(grapple, const)
        if not re.search(rf"\bint {const} = {value:_};", java):
            err(f"JugcraftGrapple.{const} differs from tools/grapple.py ({value:_})")
    if 'item("pneumatic_grapple"' not in java or 'Jugcraft.id("grapple_hook")' not in java:
        err("JugcraftGrapple does not register the grapple and its hook")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    for key in (f"entity.{MOD}.grapple_hook", f"tooltip.{MOD}.nitrogen", f"message.{MOD}.pneumatic_grapple.empty"):
        if key not in lang:
            err(f"Missing name {key}")
    for path in ("item/pneumatic_grapple.png", "entity/grapple_hook.png"):
        if not (ASSETS / "textures" / path).exists():
            err(f"Missing texture {path}")


def check_plastic():
    """chemistry/PetroBlocks.PLASTIC_COLORS against tools/plastic.py COLORS."""
    java = (JAVA_ROOT / "chemistry" / "PetroBlocks.java").read_text(encoding="utf-8")
    found = re.findall(r'"([a-z_]+)"', re.search(r"PLASTIC_COLORS = List\.of\(([^)]*)\)", java).group(1))
    if found != list(plastic.COLORS):
        err(f"PetroBlocks.PLASTIC_COLORS {found} != tools/plastic.py {list(plastic.COLORS)}")


def check_gear():
    """gear/JugcraftGear.java against tools/gear.py: the tiers, pieces, paxel tiers and each tier's stats, and that
    every item and worn-armor layer has its texture."""
    java = (JAVA_ROOT / "gear" / "JugcraftGear.java").read_text(encoding="utf-8")
    for name, expected in (("TIERS", list(gear.GEAR_TIERS)), ("PIECES", gear.PIECES), ("PAXEL_TIERS", list(gear.PAXEL_TIERS))):
        found = re.findall(r'"([a-z_]+)"', re.search(name + r" = List\.of\(([^)]*)\)", java).group(1))
        if found != expected:
            err(f"JugcraftGear.{name} {found} != tools/gear.py {expected}")
    if f"PAXEL_DURABILITY = {gear.PAXEL_DURABILITY};" not in java:
        err("JugcraftGear.PAXEL_DURABILITY differs from tools/gear.py")
    for tier, info in gear.GEAR_TIERS.items():
        durability, speed, damage, enchant = info["tool"]
        drops = "INCORRECT_FOR_" + info["drops"].upper() + "_TOOL"
        tool = f"{tier.upper()} = new ToolMaterial(BlockTags.{drops}, {durability}, {speed}F, {damage}F, {enchant},"
        if tool not in java:
            err(f"JugcraftGear: {tier} tool material is not {tool}")
        mult, (boots, legs, chest, helmet), enchant, tough, knock = info["armor"]
        armor = (f"{tier.upper()}_ARMOR = new ArmorMaterial({mult}, defense({boots}, {legs}, {chest}, {helmet}), {enchant},")
        if armor not in java or f"{tough}F, {knock}F, repairs(\"{tier}\")" not in java:
            err(f"JugcraftGear: {tier} armor material differs from tools/gear.py")
        for layer in ("humanoid", "humanoid_leggings"):
            if not (ASSETS / "textures" / "entity" / "equipment" / layer / f"{tier}.png").exists():
                err(f"Missing worn armor texture {layer}/{tier}.png")
    extras = re.findall(r'^\t\t[A-Z_]+ = item\("([a-z_]+)"', java, re.M)
    if extras != list(gear.EXTRAS):
        err(f"JugcraftGear extras {extras} != tools/gear.py {list(gear.EXTRAS)}")
    scuba = (JAVA_ROOT / "gear" / "ScubaTankItem.java").read_text(encoding="utf-8")
    if (f"CAPACITY = {gear.SCUBA_OXYGEN:_};" not in scuba
            or f"OXYGEN_PER_TICK = {gear.SCUBA_OXYGEN_PER_TICK};" not in scuba):
        err("ScubaTankItem capacity or use differs from tools/gear.py")
    for asset in ("scuba", "free_runners"):
        if not (ASSETS / "textures" / "entity" / "equipment" / "humanoid" / f"{asset}.png").exists():
            err(f"Missing worn texture humanoid/{asset}.png")
    for item, info in gear.EXTRAS.items():
        frames = [item] + ([f"{item}_pulling_{step}" for step in range(3)] if info["model"] == "bow" else [])
        for frame in frames:
            if not (ASSETS / "textures" / "item" / f"{frame}.png").exists():
                err(f"Missing item texture {frame}.png")
    check_armor_styles(java)
    check_armor_looks()
    check_armor_tiers(java)


def check_armor_tiers(java):
    """The armor-only tiers (tools/gear.py ARMOR_TIERS, docs/features/bloodthorn-armor.md) against JugcraftGear: the
    list, each tier's material and its fire resistance; and, since they draw no flat layer, that every piece has a 3D
    model (tools/armor_models.py), an icon and no equipment asset file."""
    import armor_models
    match = re.search(r"ARMOR_TIERS = List\.of\(([^)]*)\)", java)
    found = re.findall(r'"([a-z_]+)"', match.group(1)) if match else None
    if found != list(gear.ARMOR_TIERS):
        err(f"JugcraftGear.ARMOR_TIERS {found} != tools/gear.py {list(gear.ARMOR_TIERS)}")
    worn = {item for armor_set in armor_models.sets() for item in armor_set.pieces}
    for tier, info in gear.ARMOR_TIERS.items():
        mult, (boots, legs, chest, helmet), enchant, tough, knock = info["armor"]
        armor = f"{tier.upper()}_ARMOR = new ArmorMaterial({mult}, defense({boots}, {legs}, {chest}, {helmet}), {enchant},"
        if armor not in java or f"{tough}F, {knock}F, repairs(\"{tier}\"), asset(\"{tier}\"))" not in java:
            err(f"JugcraftGear: the {tier} armor material differs from tools/gear.py")
        call = f'armorTier("{tier}", {tier.upper()}_ARMOR, {str(info["fire_resistant"]).lower()});'
        if call not in java:
            err(f"JugcraftGear must register the {tier} pieces as {call}")
        repairs = load(DATA / MOD / "tags" / "item" / f"repairs_{tier}_gear.json") or {}
        if repairs.get("values") != [info["repair"]]:
            err(f"{MOD}:repairs_{tier}_gear must hold {info['repair']}")
        for piece in gear.ARMOR:
            item = f"{tier}_{piece}"
            if item not in worn:
                err(f"{item} has no 3D model: an armor-only tier draws no flat layer (tools/armor_models.SET_MODULES)")
            if not (ASSETS / "textures" / "item" / f"{item}.png").is_file():
                err(f"Missing item texture {item}.png")
        if (ASSETS / "equipment" / f"{tier}.json").exists():
            err(f"equipment/{tier}.json should not exist: every {tier} piece is drawn as a 3D model")


def check_thallite_gear():
    """Thallite's traits (docs/features/thallite.md): gear/ThalliteGear.java's numbers against tools/gear.py, the trait
    and ground tags, each trait's name and description, and the thallite tools' and armor's icons."""
    java = (JAVA_ROOT / "gear" / "ThalliteGear.java").read_text(encoding="utf-8")
    for name in ("REGROWTH_SECONDS", "REGROWTH_CAP_PERCENT", "EARTHBOUND_FOR_STONE", "ROOTED_PER_PIECE", "ROOTED_TICKS"):
        if f"{name} = {getattr(gear, name)};" not in java:
            err(f"ThalliteGear.{name} differs from tools/gear.py ({getattr(gear, name)})")
    tags = {("item", "thallite_gear"): gear.thallite_gear(), ("item", "earthbound_armor"): gear.earthbound_armor(),
            ("block", "living_ground"): gear.LIVING_GROUND, ("block", "earthen_ground"): gear.EARTHEN_GROUND}
    for (kind, name), values in tags.items():
        found = (load(DATA / MOD / "tags" / kind / f"{name}.json") or {}).get("values")
        if found != values:
            err(f"{MOD}:{name} ({kind} tag) is {found}, expected {values} (tools/gear.py)")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    for trait in gear.TRAITS:
        for key in (f"tooltip.{MOD}.thallite.{trait}.trait", f"tooltip.{MOD}.thallite.{trait}"):
            if not lang.get(key):
                err(f"Missing thallite trait text {key}")
    for item in gear.thallite_gear() + [f"{MOD}:{gear.ARMOR_STYLES['earthbound_thallite']['template']}"]:
        if not (ASSETS / "textures" / "item" / f"{item.split(':')[1]}.png").is_file():
            err(f"Missing item texture {item.split(':')[1]}.png")


def check_armor_styles(java):
    """Steampunk and Kaiser Armor (docs/features/steampunk-and-kaiser-armor.md) against tools/gear.py ARMOR_STYLES:
    Java lists the styles and templates and derives each material from its metal's; every set has its equipment asset
    and 64x32 worn layers; each styled piece has its smithing recipes both ways, its slot tag and its lore; each
    template its recipe and tooltip."""
    for name, expected in (("ARMOR_STYLES", list(gear.ARMOR_STYLES)), ("STYLE_TEMPLATES", gear.style_templates())):
        match = re.search(name + r" = List\.of\(([^)]*)\)", java)
        found = re.findall(r'"([a-z_]+)"', match.group(1)) if match else None
        if found != expected:
            err(f"JugcraftGear.{name} {found} != tools/gear.py {expected}")
    for style, info in gear.ARMOR_STYLES.items():
        line = f'{style.upper()}_ARMOR = restyle({info["metal"].upper()}_ARMOR, "{style}");'
        if line not in java:
            err(f"JugcraftGear must derive the {style} armor material from its metal's: {line}")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    # A set whose every piece has a 3D model (tools/armor_models.py, docs/features/knight-armor.md) draws no flat layer
    # and has no equipment asset; its 64x32 layers are still generated, as a fallback.
    import armor_models
    worn = {item for armor_set in armor_models.sets() for item in armor_set.pieces}
    for asset in list(gear.GEAR_TIERS) + list(gear.ARMOR_STYLES):
        for layer in ("humanoid", "humanoid_leggings"):
            png = ASSETS / "textures" / "entity" / "equipment" / layer / f"{asset}.png"
            if png.is_file():
                with Image.open(png) as img:
                    if img.size != (64, 32):
                        err(f"Worn armor texture {layer}/{asset}.png is {img.size}, expected 64x32")
            else:
                err(f"Missing worn armor texture {layer}/{asset}.png")
        if all(f"{asset}_{piece}" in worn for piece in gear.ARMOR):
            if (ASSETS / "equipment" / f"{asset}.json").exists():
                err(f"equipment/{asset}.json should not exist: every {asset} piece is drawn as a 3D model")
            continue
        equipment = load(ASSETS / "equipment" / f"{asset}.json") or {}
        for layer in ("humanoid", "humanoid_leggings"):
            if equipment.get("layers", {}).get(layer) != [{"texture": f"{MOD}:{asset}"}]:
                err(f"equipment/{asset}.json must draw {MOD}:{asset} on its {layer} layer")
    recipes = DATA / MOD / "recipe"
    for style, info in gear.ARMOR_STYLES.items():
        metal, template = info["metal"], f"{MOD}:{info['template']}"
        for piece in gear.ARMOR:
            styled, plain = f"{style}_{piece}", f"{metal}_{piece}"
            routes = [(styled, plain, info["addition"], styled)]
            if info["reversible"]:
                routes.append((f"{plain}_from_{styled}", styled, gear.GEAR_TIERS[metal]["ingot"], plain))
            for name, base, addition, result in routes:
                recipe = load(recipes / f"{name}.json") if (recipes / f"{name}.json").is_file() else None
                if not recipe or recipe.get("type") != "minecraft:smithing_transform" \
                        or recipe.get("template") != template or recipe.get("base") != f"{MOD}:{base}" \
                        or recipe.get("addition") != addition or recipe.get("result", {}).get("id") != f"{MOD}:{result}":
                    err(f"recipe/{name}.json must smith {MOD}:{base} with {template} and {addition} into "
                        f"{MOD}:{result}")
            tag = load(DATA / "minecraft" / "tags" / "item" / f"{gear.ITEM_TAGS[piece]}.json") or {}
            if f"{MOD}:{styled}" not in tag.get("values", []):
                err(f"{styled} is missing from minecraft:{gear.ITEM_TAGS[piece]}")
            if f"tooltip.{MOD}.{styled}" not in lang:
                err(f"Missing tooltip for {styled}")
        recipe = load(recipes / f"{info['template']}.json") if (recipes / f"{info['template']}.json").is_file() else None
        if not recipe or recipe.get("result", {}).get("count") != info["template_count"]:
            err(f"recipe/{info['template']}.json must make {info['template_count']} {template}")
        if f"tooltip.{MOD}.{info['template']}" not in lang:
            err(f"Missing tooltip for {info['template']}")


# The stylized armor looks the owner kept (5 October 2026) as Steampunk and Kaiser Armor: the SHA-256 of
# f"{w}x{h}" + the RGBA pixels of each PNG, from efd85edc (#94), the last commit that changed the first bronze and steel
# armor art. Pixels, not file bytes, so another PNG encoder does not trip it.
OLD_ARMOR_LOOKS = {
    "item/steampunk_helmet": "7e9cd2f8152bd1aad1338c5df2781bd5f422f224e15ad8e44491b0f3886d5e41",
    "item/steampunk_chestplate": "e2b0b6174c816b6a46a347c1b4135a58f54a3561771d097a4300f04ee838d5b8",
    "item/steampunk_leggings": "165ba5565e635230aa78f74392c7ee25356330f7975b41b918bcc561d973f8da",
    "item/steampunk_boots": "13ea7f66a1b57b71819c317a964c00f6a7a6e843d00e99a9cbe774978f038dd5",
    "item/kaiser_helmet": "b57d95b7f99af00163934fe34b355d50b7446dfd93422e14e0de7dc5d7035025",
    "item/kaiser_chestplate": "2ba556aaac60b5bee50755bc7f6b67fe446b1ad87fa58c61ad9f6ba65779556d",
    "item/kaiser_leggings": "4b10f5f095766bedeb49c3eb7f7de0dfdee69eae5c944943b5dc6512abb5bd1e",
    "item/kaiser_boots": "f76f12f13130958584385e19ba0a643d5a75624af66f2ac0bab6a78bf144be53",
    "entity/equipment/humanoid/steampunk": "ba01b2d8c61954c71eefdf23945a1555828db11e33985fce18e15d60e97ed4fa",
    "entity/equipment/humanoid_leggings/steampunk": "ccee284ba4d0369cc0463b07dab2df64b7a9325595450e2841ec50f5d8042bd8",
    "entity/equipment/humanoid/kaiser": "0d05602d98d8cc01cdacb666186dcc7c9fdcbbbcd8240074d236e85ed06e4817",
    "entity/equipment/humanoid_leggings/kaiser": "e4528cbb4af9f2917f3d664dc60a6efaead6cad4caf673f946afb410d9188956",
}


def check_armor_looks():
    """Steampunk and Kaiser Armor keep the stylized looks pixel for pixel (OLD_ARMOR_LOOKS). Bronze and steel armor
    are not pinned: they get their own design later."""
    for path, expected in OLD_ARMOR_LOOKS.items():
        png = ASSETS / "textures" / f"{path}.png"
        if not png.is_file():
            err(f"Missing {path}.png, the stylized look the owner kept (5 October 2026)")
            continue
        with Image.open(png) as img:
            rgba = img.convert("RGBA")
            digest = hashlib.sha256(f"{rgba.width}x{rgba.height}".encode() + rgba.tobytes()).hexdigest()
        if digest != expected:
            err(f"{path}.png no longer matches the stylized look the owner kept (5 October 2026); a deliberate redraw "
                "needs the owner's OK and new hashes.")


def check_arms():
    """weapons/JugcraftArms.java against tools/arms.py (batch 42): the metals, each kind's numbers and traits in order,
    the charging kinds' numbers, the lance and parry constants, and that each arm's model has its held pose."""
    java = (JAVA_ROOT / "weapons" / "JugcraftArms.java").read_text(encoding="utf-8")

    def f(value):
        return f"{float(value)}F"
    metals = re.findall(r'"([a-z_]+)"', re.search(r"METALS = List\.of\(([^)]*)\)", java).group(1))
    if metals != arms.METALS:
        err(f"JugcraftArms.METALS {metals} != tools/arms.py {arms.METALS}")
    # Every kind, bow, crossbow and shield is a named trait with its description (docs/features/trait-details.md), and
    # every weapon art too; their tooltips also need the two-handed trait and the "hold Shift" line.
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    for kind in [*arms.KINDS, *arms.RANGED_KINDS, *arms.SHIELD_KINDS]:
        if kind not in arms.TRAIT_NAMES:
            err(f"tools/arms.py: {kind} has no TRAIT_NAMES entry, the name its tooltip shows")
        if not {f"tooltip.{MOD}.arms.{kind}", f"tooltip.{MOD}.arms.{kind}.trait"} <= lang.keys():
            err(f"lang: {kind} needs tooltip.{MOD}.arms.{kind} and its .trait name")
    for art in arms.ARTS.values():
        if not {f"tooltip.{MOD}.arms.art.{art['move']}", f"tooltip.{MOD}.arms.art.{art['move']}.trait"} <= lang.keys():
            err(f"lang: the {art['move']} art needs its description and its .trait name")
    for key in (f"tooltip.{MOD}.arms.two_handed", f"tooltip.{MOD}.arms.two_handed.trait", f"tooltip.{MOD}.hold_shift"):
        if key not in lang:
            err(f"lang: no {key}")
    swung = [kind for kind in arms.KINDS if kind not in arms.CHARGING]
    found = re.findall(r'new Kind\("([a-z_]+)"', java)
    if found != swung:
        err(f"JugcraftArms.KINDS {found} != tools/arms.py {swung}")
    for kind in swung:
        info = arms.KINDS[kind]
        swing, ticks = info["swing"]
        low, high = info["reach"]
        expected = (f'new Kind("{kind}", {f(info["damage"])}, {f(info["speed"])}, SwingAnimationType.{swing.upper()}, {ticks}, '
                    f'{f(low)}, {f(high)}, {f(info["margin"])}, {f(info["disable"])}, {info["wear"]}, {f(info["knockback"])}, '
                    f'{f(info["parry"])}, {str("swords" in info["tags"]).lower()}, {str(info.get("pierce", False)).lower()})')
        if expected not in java:
            err(f"JugcraftArms: {kind} is not {expected}")
    charges = re.findall(r'new Charge\("([a-z_]+)", "([a-z_]+)"', java)
    if sorted(charges) != sorted(arms.CHARGE):
        err(f"JugcraftArms.CHARGES {charges} != tools/arms.py {list(arms.CHARGE)}")
    for (kind, metal), values in arms.CHARGE.items():
        expected = f'new Charge("{kind}", "{metal}", ' + ", ".join(f(v) for v in values) + ")"
        if expected not in java:
            err(f"JugcraftArms: the {metal} {kind} is not {expected}")
    for name, value in (("LANCE_DAMAGE", arms.LANCE_DAMAGE), ("LANCE_MIN_REACH", arms.LANCE_REACH[0]),
                        ("LANCE_MAX_REACH", arms.LANCE_REACH[1]), ("PARRY_ANGLE", arms.PARRY_ANGLE),
                        ("PARRY_DELAY", arms.PARRY_DELAY), ("PARRY_WEAR_THRESHOLD", arms.PARRY_WEAR[0]),
                        ("PARRY_WEAR_BASE", arms.PARRY_WEAR[1]), ("PARRY_WEAR_FACTOR", arms.PARRY_WEAR[2])):
        if f"{name} = {f(value)};" not in java:
            err(f"JugcraftArms.{name} differs from tools/arms.py ({value})")
    # Arms II traits (batch 45): the same kinds and traits, and the same numbers.
    traits = dict(re.findall(r'"([a-z_]+)",\s*Trait\.([A-Z_]+)', re.search(r"TRAITS = Map\.of(?:Entries)?\((.*?)\);", java, re.S).group(1)))
    if traits != {kind: trait.upper() for kind, trait in arms.TRAITS.items()}:
        err(f"JugcraftArms.TRAITS {traits} != tools/arms.py {arms.TRAITS}")
    for name, value in (("BACKSTAB", arms.BACKSTAB), ("BACKSTAB_ANGLE", arms.BACKSTAB_ANGLE), ("SADDLE", arms.SADDLE),
                        ("ARMOR_PIERCE", arms.ARMOR_PIERCE), ("ARMOR_PIERCE_MAX", arms.ARMOR_PIERCE_MAX),
                        ("RIDERS", arms.RIDERS)):
        if f"{name} = {f(value)};" not in java:
            err(f"JugcraftArms.{name} differs from tools/arms.py ({value})")
    for name, value in (("DAZE_TICKS", arms.DAZE[0]), ("DAZE_AMPLIFIER", arms.DAZE[1]), ("REAP_RADIUS", arms.REAP_RADIUS),
                        ("REAP_WEAR", arms.REAP_WEAR)):
        if f"{name} = {value};" not in java:
            err(f"JugcraftArms.{name} differs from tools/arms.py ({value})")
    # Arms III (batch 46): its traits' numbers and the two-handed swings.
    for name, value in (("QUAKE_RADIUS", arms.QUAKE_RADIUS), ("QUAKE_SHARE", arms.QUAKE_SHARE), ("EXECUTE", arms.EXECUTE),
                        ("EXECUTE_HEALTH", arms.EXECUTE_HEALTH), ("HOOK", arms.HOOK),
                        ("TWO_HANDED_SLOW", arms.TWO_HANDED_SLOW), ("FINISHER", arms.FINISHER)):
        if f"{name} = {f(value)};" not in java:
            err(f"JugcraftArms.{name} differs from tools/arms.py ({value})")
    for name, value in (("QUAKE_TICKS", arms.QUAKE_DAZE[0]), ("QUAKE_AMPLIFIER", arms.QUAKE_DAZE[1]),
                        ("QUEUE_TICKS", arms.QUEUE_TICKS), ("COMBO_WINDOW", arms.COMBO_WINDOW)):
        if f"{name} = {value};" not in java:
            err(f"JugcraftArms.{name} differs from tools/arms.py ({value})")
    heavy = {kind: {"strike": int(strike), "arc": float(arc), "targets": int(targets), "combo": int(combo)}
             for kind, strike, arc, targets, combo in re.findall(
                 r'Map\.entry\("([a-z_]+)", new Heavy\((\d+), ([\d.]+)F, (\d+), (\d+)\)\)', java)}
    if heavy != {kind: {**info, "arc": float(info["arc"])} for kind, info in arms.TWO_HANDED.items()}:
        err(f"JugcraftArms.TWO_HANDED {heavy} != tools/arms.py {arms.TWO_HANDED}")
    import arms_moves
    for kind, info in arms.TWO_HANDED.items():
        if kind not in arms.KINDS or kind in arms.CHARGING or arms.KINDS[kind].get("pierce"):
            err(f"tools/arms.py: TWO_HANDED {kind} is not a swung arm")
            continue
        moves = arms_moves.MOVES.get(kind, {})
        ticks = arms.KINDS[kind]["swing"][1]
        blow = moves.get("blow")
        if not moves.get("two_handed") or blow is None:
            err(f"tools/arms_moves.py: the two-handed {kind} has no two-handed grip or blow")
            continue
        # The blow lands when the animation lands it: every attack has a key at the blow, half a tick from the strike.
        if abs(blow * ticks - info["strike"]) > 0.5:
            err(f"The {kind}'s strike, tick {info['strike']}, is not its animation's blow ({blow} of {ticks} ticks)")
        for clip in moves["attacks"]:
            if not any(abs(t - blow) < 1e-9 for t, _pose, _k in clip.keys):
                err(f"tools/arms_moves.py: the {kind}'s {clip.name} has no key at its blow, {blow}")
        if len(moves["attacks"]) != info["combo"]:
            err(f"The {kind}'s combo is {info['combo']} in tools/arms.py but {len(moves['attacks'])} attacks in its motion")
        if ticks > 20.0 / (4.0 + arms.KINDS[kind]["speed"]):
            err(f"The {kind}'s swing ({ticks} ticks) is longer than the time between its blows")
        if not 0 < info["strike"] < ticks or not 0 < info["arc"] <= 180 or info["targets"] < 1:
            err(f"tools/arms.py: the {kind}'s two-handed swing {info} is out of range")
    # Arms V (batch 48): the weapon arts, their numbers, their timing against their animations, and their balance.
    found_arts = {kind: {"move": move.lower(), "cooldown": int(cooldown), "ticks": int(ticks), "slow": float(slow)}
                  for kind, move, cooldown, ticks, slow in re.findall(
                      r'Map\.entry\("([a-z_]+)", new Art\(Move\.([A-Z_]+), (\d+), (\d+), ([\d.]+)F\)\)', java)}
    expected_arts = {kind: {"move": art["move"], "cooldown": art["cooldown"], "ticks": art["ticks"], "slow": float(art["slow"])}
                     for kind, art in arms.ARTS.items()}
    if found_arts != expected_arts:
        err(f"JugcraftArms.ARTS {found_arts} != tools/arms.py {expected_arts}")
    moves = re.search(r"enum Move \{\s*([A-Z_, ]+);", java)
    if not moves or sorted(m.strip().lower() for m in moves.group(1).split(",")) != sorted({a["move"] for a in arms.ARTS.values()}):
        err("JugcraftArms.Move does not name the arts' moves")
    for kind, art in arms.ARTS.items():
        if kind not in arms.KINDS or arms.KINDS[kind].get("art") != art["move"] or arms.KINDS[kind]["parry"] or "trait" in arms.KINDS[kind]:
            err(f"tools/arms.py: ARTS {kind} is not a kind with that art (and no parry or trait, which would take the use key)")
        if not 0 < art["ticks"] <= art["cooldown"] or not 0 <= art["slow"] < 1:
            err(f"tools/arms.py: the {kind}'s art {art} is out of range")
    for name in ("CYCLONE_FIRST", "CYCLONE_EVERY", "CYCLONE_HITS", "CYCLONE_TARGETS", "IAIDO_START", "IAIDO_DASH", "IAIDO_DELAY",
                 "IAIDO_TARGETS", "LEAP_MIN_AIR", "LEAP_STUCK", "LEAP_MAX_AIR", "LEAP_TARGETS", "FLURRY_FIRST", "FLURRY_EVERY",
                 "FLURRY_JABS", "CRESCENT_RELEASE", "CRESCENT_TICKS", "CRESCENT_TARGETS", "LASH_THROW", "LASH_REAP",
                 "CUTS_FIRST", "CUTS_EVERY", "CUTS_COUNT", "CUTS_TARGETS", "IGNITE_SECONDS", "IGNITE_WEAR"):
        if f"{name} = {getattr(arms, name)};" not in java:
            err(f"JugcraftArms.{name} differs from tools/arms.py ({getattr(arms, name)})")
    for name in ("CYCLONE_RADIUS", "CYCLONE_SHARE", "CYCLONE_PULL", "IAIDO_SPEED", "IAIDO_WIDTH", "IAIDO_SHARE", "IAIDO_REACH",
                 "LEAP_UP", "LEAP_FORWARD", "LEAP_RADIUS", "LEAP_SHARE", "LEAP_EDGE", "LEAP_PER_BLOCK", "LEAP_DROP_MAX",
                 "LEAP_LIFT", "FLURRY_SHARE", "FLURRY_FINISH", "FLURRY_ARC", "CRESCENT_SPEED", "CRESCENT_WIDTH",
                 "CRESCENT_SHARE", "CRESCENT_FADE", "LASH_RANGE", "LASH_SHARE", "LASH_PULL", "LASH_PULL_MAX",
                 "LASH_REAP_SHARE", "LASH_REAP_REACH", "CUTS_SHARE", "CUTS_ARC"):
        if f"{name} = {f(getattr(arms, name))};" not in java:
            err(f"JugcraftArms.{name} differs from tools/arms.py ({getattr(arms, name)})")
    # Each art's animation has a key at every tick the server lands a hit (to half a tick), so the blow is seen as it lands.
    hits = {"cyclone": [arms.CYCLONE_FIRST + i * arms.CYCLONE_EVERY for i in range(arms.CYCLONE_HITS)],
            "iaido": [arms.IAIDO_START + arms.IAIDO_DASH + arms.IAIDO_DELAY],
            "flurry": [arms.FLURRY_FIRST + i * arms.FLURRY_EVERY for i in range(arms.FLURRY_JABS + 1)],
            "crescent": [arms.CRESCENT_RELEASE], "chain_lash": [arms.LASH_THROW, arms.LASH_REAP], "leap_slam": [],
            "seven_cuts": [arms.CUTS_FIRST + i * arms.CUTS_EVERY for i in range(arms.CUTS_COUNT)]}
    for kind, art in arms.ARTS.items():
        clips = arms_moves.MOVES.get(kind, {}).get("arts", [])
        if not clips or clips[0].ticks != art["ticks"]:
            err(f"tools/arms_moves.py: the {kind}'s art has no clip, or its first lasts other than {art['ticks']} ticks")
            continue
        for tick in hits[art["move"]]:
            if tick > art["ticks"] or not any(abs(t * art["ticks"] - tick) <= 0.5 for t, _pose, _k in clips[0].keys):
                err(f"tools/arms_moves.py: the {kind}'s {clips[0].name} has no key at its hit on tick {tick}")
        if art["move"] == "leap_slam" and (len(clips) != 2 or not clips[0].hold or clips[1].keys[0][1] != clips[0].keys[-1][1]):
            err(f"tools/arms_moves.py: the {kind}'s leap must hold in the air and its slam start from there")
    if not 0 < arms.LEAP_MIN_AIR < arms.LEAP_STUCK < arms.LEAP_MAX_AIR or arms.LASH_THROW >= arms.LASH_REAP:
        err("tools/arms.py: the leap's or the chain's timing is out of order")
    # Against one foe an art is no better than plain blows: over its cooldown (busy for its ticks, plain blows for the
    # rest) an Arms V arm stays below its metal's sword, a second. Its worth is the shape: many foes, or getting there.
    for kind, art in arms.ARTS.items():
        info = arms.KINDS[kind]
        combo = arms.TWO_HANDED.get(kind, {}).get("combo", 1)
        finishing = (combo - 1 + arms.FINISHER) / combo if kind in arms.TWO_HANDED else 1.0
        cycle, busy = art["cooldown"] / 20.0, art["ticks"] / 20.0
        for metal, bonus in (("bronze", 2.0), ("steel", 2.5)):
            blow = 1.0 + bonus + info["damage"]
            plain = blow * (4.0 + info["speed"]) * finishing
            sword = (1.0 + bonus + 3.0) * 1.6
            average = (arms.art_share(kind) * blow + plain * (cycle - busy)) / cycle
            if average >= sword:
                err(f"The {metal} {kind} with its art deals {average:.2f} a second to one foe, not below the sword's {sword:.2f}")
    # Arms VI (batch 55): the bows, crossbows and shields as tools/arms.py has them, in registration order, and in range.
    found_ranged = [(name, metal, {"draw": int(draw), "speed": float(speed), "damage": float(damage)})
                    for name, metal, draw, speed, damage in re.findall(
                        r'new Ranged\("([a-z_]+)", "([a-z]+)", (\d+), ([\d.]+)F, ([\d.]+)F\)', java)]
    expected_ranged = [(name, metal, {"draw": info.get("draw", 0), "speed": float(info["speed"]), "damage": float(info["damage"])})
                       for metal in arms.METALS for (name, at), info in arms.RANGED.items() if at == metal]
    if found_ranged != expected_ranged:
        err(f"JugcraftArms.RANGED {found_ranged} != tools/arms.py {expected_ranged}")
    if f"CROSSBOW_SPEED = {f(arms.VANILLA_CROSSBOW['speed'])};" not in java:
        err(f"JugcraftArms.CROSSBOW_SPEED is not vanilla's crossbow's {arms.VANILLA_CROSSBOW['speed']}")
    bow = arms.VANILLA_BOW
    vanilla_rate = bow["speed"] * bow["damage"] / (bow["draw"] / 20)
    for (name, metal), info in arms.RANGED.items():
        kind_type = arms.RANGED_KINDS[name]["type"]
        cycle = (info["draw"] if kind_type == "bow" else arms.VANILLA_CROSSBOW["load"]) / 20
        if kind_type == "bow" and info["draw"] < bow["draw"]:
            err(f"The {metal} {name} draws faster than vanilla's bow")
        if info["speed"] * info["damage"] / cycle >= vanilla_rate:
            err(f"The {metal} {name} deals {info['speed'] * info['damage'] / cycle:.2f} a second, not below vanilla's bow's {vanilla_rate:.2f}")
    found_shields = [(name, metal, [float(v) for v in (delay, angle, disable)] + [int(durability)]
                      + [float(v) for v in (wear, brace, weight)])
                     for name, metal, delay, angle, disable, durability, wear, brace, weight in re.findall(
                         r'new Shield\("([a-z_]+)", "([a-z]+)", ([\d.]+)F, ([\d.]+)F, ([\d.]+)F, (\d+), ([\d.]+)F, ([\d.]+)F, ([\d.]+)F\)', java)]
    expected_shields = [(name, metal, [float(info[k]) for k in ("delay", "angle", "disable")] + [info["durability"]]
                         + [float(info[k]) for k in ("wear", "brace", "weight")])
                        for metal in arms.METALS for (name, at), info in arms.SHIELDS.items() if at == metal]
    if found_shields != expected_shields:
        err(f"JugcraftArms.SHIELDS {found_shields} != tools/arms.py {expected_shields}")
    for (name, metal), info in arms.SHIELDS.items():
        if not (0 < info["delay"] <= 1 and 0 < info["angle"] <= 180 and 0 < info["disable"] <= 1 and info["durability"] > 0
                and 0 < info["wear"] <= 1 and 0 <= info["brace"] <= 1 and 0 <= info["weight"] < 0.5):
            err(f"tools/arms.py: the {metal} {name} {info} is out of range")
        if info["angle"] > arms.VANILLA_SHIELD["angle"] and info["delay"] <= arms.VANILLA_SHIELD["delay"]:
            err(f"The {metal} {name} covers more than vanilla's shield without being slower to raise")
    # Arms VIII (batch 59): the thrown arms as tools/arms.py has them, in registration order, and their balance: no
    # throw hits harder than the trident's, and throwing one after another deals less a second than a netherite sword.
    found_thrown = [(name, metal, {"wind": int(wind), "speed": float(speed), "gravity": float(gravity), "damage": float(damage)})
                    for name, metal, wind, speed, gravity, damage in re.findall(
                        r'new Thrown\("([a-z_]+)", "([a-z]+)", (\d+), ([\d.]+)F, ([\d.]+)F, ([\d.]+)F\)', java)]
    expected_thrown = [(name, metal, {"wind": info["wind"], "speed": float(info["speed"]), "gravity": float(info["gravity"]),
                                      "damage": float(info["damage"])})
                       for metal in arms.METALS for (name, at), info in arms.THROWN.items() if at == metal]
    if found_thrown != expected_thrown:
        err(f"JugcraftArms.THROWN {found_thrown} != tools/arms.py {expected_thrown}")
    if sorted({name for name, _metal in arms.THROWN}) != sorted(arms.THROWN_KINDS) or any(
            kind not in arms.KINDS or kind in arms.CHARGING or kind in arms.ARTS or kind in arms.TWO_HANDED
            or arms.KINDS[kind]["parry"] for kind in arms.THROWN_KINDS):
        err("tools/arms.py: THROWN_KINDS must be swung arms with no art, two-handed swing or parry, each in THROWN")
    for name, value in (("FRANCISCA_DISABLE", arms.FRANCISCA_DISABLE), ("CHAKRAM_RANGE", arms.CHAKRAM_RANGE),
                        ("CHAKRAM_RETURN", arms.CHAKRAM_RETURN), ("CHAKRAM_CATCH", arms.CHAKRAM_CATCH),
                        ("HARPOON_WATER", arms.HARPOON_WATER), ("HARPOON_PULL", arms.HARPOON_PULL),
                        ("HARPOON_PULL_MAX", arms.HARPOON_PULL_MAX)):
        if f"{name} = {f(value)};" not in java:
            err(f"JugcraftArms.{name} differs from tools/arms.py ({value})")
    for name, value in (("THROW_WEAR", arms.THROW_WEAR), ("THROW_COOLDOWN", arms.THROW_COOLDOWN),
                        ("CHAKRAM_TARGETS", arms.CHAKRAM_TARGETS), ("CHAKRAM_MAX_TICKS", arms.CHAKRAM_MAX_TICKS)):
        if f"{name} = {value};" not in java:
            err(f"JugcraftArms.{name} differs from tools/arms.py ({value})")
    renderer = (ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client" / "arms"
                / "ThrownArmRenderer.java").read_text(encoding="utf-8")
    flight = {kind: float(size) for kind, size in re.findall(r'"([a-z_]+)", ([\d.]+)F', re.search(
        r"FLIGHT = Map\.of\((.*?)\);", renderer, re.S).group(1))}
    if flight != {kind: float(size) for kind, size in arms.FLIGHT.items()} or sorted(flight) != sorted(arms.THROWN_KINDS):
        err(f"ThrownArmRenderer.FLIGHT {flight} != tools/arms.py {arms.FLIGHT}")
    netherite = (1.0 + 4.0 + 3.0) * 1.6
    for (name, metal), info in arms.THROWN.items():
        if not (1 <= info["wind"] <= 40 and 0 < info["speed"] <= 4 and 0 <= info["gravity"] <= 0.1 and info["damage"] > 0):
            err(f"tools/arms.py: the thrown {metal} {name} {info} is out of range")
        if info["damage"] > arms.VANILLA_TRIDENT["damage"]:
            err(f"The thrown {metal} {name} hits for {info['damage']:g}, harder than the trident's {arms.VANILLA_TRIDENT['damage']:g}")
        if arms.throw_per_second(name, metal) >= netherite:
            err(f"Thrown one after another, the {metal} {name} deals {arms.throw_per_second(name, metal):.2f} a second, "
                f"not below a netherite sword's {netherite:.2f}")
    if arms.kit() != [item for item in arms.items() if arms.split(item)[1] not in arms.KINDS]:
        err("tools/arms.py: kit() is not the arms outside KINDS")
    # No arm may take an id another generator already registers (two items of one id stop the game at start).
    clash = set(arms.items()) & (set(ag.all_items()) | set(all_items()) | set(all_blocks()) | set(gear.items()))
    if clash:
        err(f"tools/arms.py: arms ids already registered elsewhere: {sorted(clash)}")
    mixins = load(RES / f"{MOD}.mixins.json") or {}
    if "AttackStrengthAccessor" not in mixins.get("mixins", []):
        err(f"{MOD}.mixins.json does not list AttackStrengthAccessor (the two-handed swings need it)")
    # Every arm swung as a sword is, a second, below its metal's sword: its trait is the reason to choose it. A
    # two-handed arm's finishing blow counts, over its combo.
    for kind, info in arms.KINDS.items():
        if kind in arms.CHARGING:
            continue
        combo = arms.TWO_HANDED.get(kind, {}).get("combo", 1)
        finishing = (combo - 1 + arms.FINISHER) / combo if kind in arms.TWO_HANDED else 1.0
        for metal, bonus in (("bronze", 2.0), ("steel", 2.5)):
            arm = (1.0 + bonus + info["damage"]) * (4.0 + info["speed"]) * finishing
            sword = (1.0 + bonus + 3.0) * 1.6
            if arm >= sword:
                err(f"The {metal} {kind} deals {arm:.2f} a second, not below the {metal} sword's {sword:.2f}")
    for kind, info in arms.KINDS.items():
        if kind in arms.CHARGING:
            continue
        held = load(ASSETS / "models" / "item" / f"arms_{kind}.json") or {}
        if held != arms.held_model(kind):
            err(f"models/item/arms_{kind}.json is not tools/arms.py's held pose")
    for item in arms.items():
        _metal, kind = arms.split(item)
        definition = load(ASSETS / "items" / f"{item}.json") or {}
        if not definition.get("swap_animation_scale"):
            err(f"items/{item}.json has no swap_animation_scale")


def check_nuclear_sounds():
    """The sound events nuclear/JugcraftNuclear.java registers by name (the Geiger counter, the diesel generators' start,
    run and stop) are in sounds.json, each sound with its .ogg: a sounds.json written on its own is replaced the next
    time the data is generated, and the game then plays nothing (Oct 8: the diesel generators were silent)."""
    java = (JAVA_ROOT / "nuclear" / "JugcraftNuclear.java").read_text(encoding="utf-8")
    events = re.findall(r'create(?:Variable|Fixed)RangeEvent\(Jugcraft\.id\("([a-z0-9_.]+)"\)', java)
    if not events:
        err("JugcraftNuclear.java registers no sound events by name (check_nuclear_sounds needs updating)")
    sounds = load(ASSETS / "sounds.json") or {}
    for event in events:
        if event not in sounds:
            err(f"sounds.json has no {event} (registered in JugcraftNuclear.java)")
        for sound in sounds.get(event, {}).get("sounds", []):
            ns, path = sound["name"].split(":")
            if ns == MOD and not (RES / "assets" / ns / "sounds" / f"{path}.ogg").is_file():
                err(f"The sound {sound['name']} has no .ogg")


def check_item_icons():
    """The 16x16 item icon maps, the materials they are coloured in and the item icons' sizes (tools/check_icon_maps.py,
    docs/ITEM_ICONS.md); and the checker's own self-test, so a later edit cannot quietly weaken it."""
    for message in check_icon_maps.errors() + check_icon_maps.self_test():
        err(f"Item icon maps: {message}")


def check_arms_variants():
    """weapons/ArmVariants.java against tools/arms_variants.py (Arms VII, batch 56): the variants in order with their kind,
    line and boon; the styles and patterns; every number; each style variant's smithing recipe and each boss's trophy loot
    table; the boons' and lines' tooltips; that every boon is bounded; and that no variant deals as much a second as a
    netherite sword, whatever its boon adds."""
    import arms_variants as av
    java = (JAVA_ROOT / "weapons" / "ArmVariants.java").read_text(encoding="utf-8")
    found = [(name, kind, line, None if boon == "null" else boon.split(".")[1].lower())
             for name, kind, line, boon in re.findall(r'new Variant\("([a-z_]+)", "([a-z_]+)", "([a-z_]+)", (null|Boon\.[A-Z]+)\)', java)]
    expected = [(name, kind, line, boon) for name, kind, line, boon, _display in av.VARIANTS]
    if found != expected:
        err(f"ArmVariants.VARIANTS {found} != tools/arms_variants.py {expected}")
    boons = re.search(r"enum Boon \{\s*([A-Z_, ]+?)\s*\}", java)
    if not boons or [b.strip().lower() for b in boons.group(1).split(",")] != list(av.BOONS):
        err(f"ArmVariants.Boon differs from tools/arms_variants.py BOONS {list(av.BOONS)}")
    if re.findall(r'"([a-z_]+)"', re.search(r"STYLES = List\.of\(([^)]*)\)", java).group(1)) != list(av.STYLES):
        err(f"ArmVariants.STYLES differs from tools/arms_variants.py {list(av.STYLES)}")
    if re.findall(r'"([a-z_]+)"', re.search(r"PATTERN_NAMES = List\.of\(([^)]*)\)", java, re.S).group(1)) != av.patterns():
        err(f"ArmVariants.PATTERN_NAMES differs from tools/arms_variants.py {av.patterns()}")
    ints = {"FROST_TICKS": av.FROST[0], "FROST_AMPLIFIER": av.FROST[1], "EMBER_SECONDS": av.EMBER_SECONDS,
            "VENOM_TICKS": av.VENOM[0], "VENOM_AMPLIFIER": av.VENOM[1], "WITHER_TICKS": av.WITHER[0],
            "WITHER_AMPLIFIER": av.WITHER[1], "HOWL_TICKS": av.HOWL[0], "HOWL_AMPLIFIER": av.HOWL[1],
            "MARK_TICKS": av.MARK_TICKS, "GILDED_ENCHANTABILITY": av.GILDED_ENCHANTABILITY,
            "IRONCLAD_DURABILITY": av.IRONCLAD_DURABILITY, "TROPHY_DURABILITY": av.TROPHY_DURABILITY}
    floats = {"DRAIN_HEAL": av.DRAIN_HEAL, "SHOCK_SHARE": av.SHOCK_SHARE, "SHOCK_RANGE": av.SHOCK_RANGE,
              "GALE_KNOCKBACK": av.GALE_KNOCKBACK, "GALE_LIFT": av.GALE_LIFT, "TIDE": av.TIDE, "GRAVEBANE": av.GRAVEBANE}
    for name, value in ints.items():
        if f"{name} = {value};" not in java:
            err(f"ArmVariants.{name} differs from tools/arms_variants.py ({value})")
    for name, value in floats.items():
        if f"{name} = {float(value)}F;" not in java:
            err(f"ArmVariants.{name} differs from tools/arms_variants.py ({value})")
    # Bounded: no effect longer than 5 s or above amplifier 1; no share above half a blow.
    for ticks, amplifier in (av.FROST, av.VENOM, av.WITHER, av.HOWL, (av.MARK_TICKS, 0), (av.EMBER_SECONDS * 20, 0)):
        if ticks > 100 or amplifier > 1:
            err(f"tools/arms_variants.py: a boon lasts {ticks} ticks at amplifier {amplifier}, over 5 s or amplifier 1")
    if av.SHOCK_SHARE > 0.5 or av.TIDE > 0.5 or av.GRAVEBANE > 0.5:
        err("tools/arms_variants.py: SHOCK_SHARE, TIDE and GRAVEBANE must be at most half a blow")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    for name, kind, line, boon, _display in av.VARIANTS:
        if kind not in arms.KINDS or kind in arms.CHARGING:
            err(f"tools/arms_variants.py: {name} is of {kind}, not a swung kind of tools/arms.py")
        if line not in av.LINES:
            err(f"tools/arms_variants.py: {name}'s line {line} is neither a style nor a boss")
        # Each trait has a name, and a description to show with Shift (docs/features/trait-details.md); a boss's line
        # is a name only.
        if boon is not None and not {f"tooltip.{MOD}.arms.boon.{boon}", f"tooltip.{MOD}.arms.boon.{boon}.trait"} <= lang.keys():
            err(f"lang: no trait name and description for the {boon} boon")
        if f"tooltip.{MOD}.arms.line.{line}.trait" not in lang:
            err(f"lang: no trait name for the {line} line")
        if line in av.STYLES and f"tooltip.{MOD}.arms.line.{line}" not in lang:
            err(f"lang: no description for the {line} line")
        recipe = load(DATA / MOD / "recipe" / f"{name}.json") if line in av.STYLES else None
        if line in av.STYLES and (not recipe or recipe.get("type") != "minecraft:smithing_transform"
                                  or recipe.get("base") != f"{MOD}:steel_{kind}"
                                  or recipe.get("template") != f"{MOD}:{av.STYLES[line]['pattern']}"):
            err(f"recipe/{name}.json must smith the steel {kind} with the {line} pattern")
        if line not in av.STYLES and (DATA / MOD / "recipe" / f"{name}.json").is_file():
            err(f"{name} is a trophy of {line}: it has no recipe")
        # A second at most: the steel arm's (its two-handed finisher too), with its boon at its best, below netherite's sword.
        info = arms.KINDS[kind]
        combo = arms.TWO_HANDED.get(kind, {}).get("combo", 1)
        finishing = (combo - 1 + arms.FINISHER) / combo if kind in arms.TWO_HANDED else 1.0
        blow = 1.0 + 2.5 + info["damage"]
        if boon == "tide":
            blow *= 1.0 + av.TIDE
        if boon == "gravebane":
            blow *= 1.0 + av.GRAVEBANE
        second = blow * (4.0 + info["speed"]) * finishing
        second += {"ember": 1.0, "venom": 0.8, "wither": 0.5}.get(boon, 0.0)   # fire, Poison and Wither, a second
        if second >= 12.8:
            err(f"{name} deals {second:.2f} a second at best, not below a netherite sword's 12.8")
    for boss in av.BOSSES:
        table = load(DATA / MOD / "loot_table" / "bosses" / f"{boss}.json") or {}
        dropped = re.findall(r'"name": "jugcraft:([a-z_]+)"', json.dumps(table, indent=0))
        if sorted(dropped) != sorted(av.trophies(boss)):
            err(f"loot_table/bosses/{boss}.json drops {dropped}, not its trophies {av.trophies(boss)}")
    for style, info in av.STYLES.items():
        if not (DATA / MOD / "recipe" / f"{info['pattern']}.json").is_file():
            err(f"recipe/{info['pattern']}.json is missing: the {style} pattern must be craftable")


def check_mesh_models():
    """The Runebound arms' mesh models (tools/arms_mesh.py, read in game by client/MeshItemModels.java): each in-hand
    model is an optional "jugcraft:mesh" model keeping its box model as "elements"; every quad has four corners of
    eight numbers (unit normals, UVs within the sprite, corners within -16..32 pixels) and a texture slot the model
    defines; only the glyph strip and the atlas's glowing regions glow; there are at most arms_mesh.MAX_QUADS; every
    part is closed (no edge belongs to one quad only: a see-through hole); no two surfaces within arms_mesh.PARALLEL
    degrees of parallel lie closer than arms_mesh.LIFT (0.1 pixel) where they overlap without crossing (at
    arms_mesh.CROSSING_ANGLE or more), compared triangle by triangle as they are drawn (drawn without culling they
    could flicker); the mesh lies along the diagonal from the butt to the point of the design it replaces, held at its
    grip, as the box model was; and its textures are solid (no see-through pixel)."""
    import math
    import arms_mesh as am
    import arms_variants_art
    glowing = [m for m in am.Mat.ALL if m.slot == "mesh" and m.glow]

    def inside(mat, u, v):
        x0, y0, x1, y1 = mat.region
        return x0 / mat.size - 1e-4 <= u <= x1 / mat.size + 1e-4 and y0 / mat.size - 1e-4 <= v <= y1 / mat.size + 1e-4
    for name in am.NAMES:
        ref = f"models/item/{name}_in_hand.json"
        model = load(ASSETS / "models" / "item" / f"{name}_in_hand.json") or {}
        kind = model.get("fabric:type")
        if not isinstance(kind, dict) or kind.get("id") != f"{MOD}:mesh" or kind.get("optional") is not True:
            err(f"{ref}: expected \"fabric:type\": {{\"id\": \"{MOD}:mesh\", \"optional\": true}}")
        if not model.get("elements"):
            err(f"{ref}: the box model (\"elements\") must stay as the mesh's fallback")
        textures = model.get("textures", {})
        quads = model.get("quads") or []
        if not quads or len(quads) > am.MAX_QUADS:
            err(f"{ref}: {len(quads)} quads, expected 1 to {am.MAX_QUADS}")
        problems = set()
        corners = []
        for q in quads:
            v = q.get("v")
            if q.get("t") not in textures:
                problems.add(f"a quad's texture slot {q.get('t')} is not defined")
            if not (isinstance(v, list) and len(v) == 4 and all(isinstance(c, list) and len(c) == 8 for c in v)):
                problems.add("a quad is not four corners of eight numbers")
                continue
            corners.append([c[:3] for c in v])
            for c in v:
                if not all(-16 <= x <= 32 for x in c[:3]):
                    problems.add("a corner lies outside -16..32")
                if not (0 <= c[3] <= 1 and 0 <= c[4] <= 1):
                    problems.add("a UV lies outside its sprite")
                if abs(math.sqrt(sum(n * n for n in c[5:])) - 1.0) > 0.03:
                    problems.add("a normal is not of unit length")
            if q.get("t") == "rune" and not q.get("e"):
                problems.add("a quad drawn in the glyph strip does not glow")
            if q.get("t") == "mesh" and q.get("e") and not any(all(inside(m, c[3], c[4]) for c in v) for m in glowing):
                problems.add("a glowing quad of the atlas lies outside its glowing regions")
        for problem in sorted(problems):
            err(f"{ref}: {problem}")
        overlaps = am.parallel_overlaps(corners)
        if overlaps:
            err(f"{ref}: {len(overlaps)} pairs of quads run within {am.PARALLEL:g} degrees of parallel closer than "
                f"{am.LIFT:g} px without crossing (they could flicker), e.g. quads {overlaps[0]}")
        holes = am.open_edges(corners)
        if holes:
            err(f"{ref}: {len(holes)} edges belong to one quad only (a part is not closed), e.g. {holes[0]}")
        # Held as the box model was: along the diagonal from the hand, from the design's butt to its point.
        grip, (gx, gy), unit = am.placement(name)
        length = arms_variants_art.design(name).length
        along = [((x - gx) + (y - gy)) * am.C45 for q in corners for x, y, _z in q]
        if corners and (abs(min(along) + grip * unit) > 0.6 or abs(max(along) - (length - grip) * unit) > 0.6):
            err(f"{ref}: the mesh runs {min(along):.2f} to {max(along):.2f} px along the arm from the hand, not "
                f"{-grip * unit:.2f} to {(length - grip) * unit:.2f} as its design")
    for texture in (am.ATLAS, am.RUNE):
        png = ASSETS / "textures" / "item" / f"{texture}.png"
        if png.is_file() and Image.open(png).convert("RGBA").getextrema()[3][0] < 255:
            err(f"textures/item/{texture}.png has see-through pixels: the mesh textures must be solid")


def check_arms_motion():
    """client/arms/ArmsMotion.java against tools/arms_motion.py and tools/arms_moves.py (batch 43): every kind of arm has
    its motion file as the generator writes it, with whole poses, keys in time order from 0 to 1 and tensions from 0 to
    1; the Java player reads the same kinds and bones; and its mixins are registered."""
    import arms_motion
    import arms_moves
    client = ROOT / "src" / "client"
    java = (client / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client" / "arms" / "ArmsMotion.java").read_text(encoding="utf-8")
    kinds = re.findall(r'"([a-z_]+)"', re.search(r"KINDS = List\.of\(([^)]*)\)", java).group(1))
    if kinds != list(arms.KINDS):
        err(f"ArmsMotion.KINDS {kinds} != tools/arms.py {list(arms.KINDS)}")
    if list(arms_moves.MOVES) != list(arms.KINDS):
        err(f"tools/arms_moves.py MOVES {list(arms_moves.MOVES)} != tools/arms.py {list(arms.KINDS)}")
    bones = re.findall(r'"([a-z_]+)"', re.search(r"BONE_NAMES = List\.of\(([^)]*)\)", java).group(1))
    if bones != arms_motion.BONES:
        err(f"ArmsMotion.BONE_NAMES {bones} != tools/arms_motion.py {arms_motion.BONES}")
    size = len(arms_motion.BONES) * len(arms_motion.CHANNELS)

    def track(label, times, tension, keys, width):
        if len(times) < 2 or times[0] != 0 or times[-1] != 1 or any(b <= a for a, b in zip(times, times[1:])):
            err(f"{label}: key times {times} do not rise from 0 to 1")
        if len(tension) != len(times) or any(not 0 <= k <= 1 for k in tension):
            err(f"{label}: tensions {tension} are not one per key, from 0 to 1")
        if len(keys) != len(times) or any(len(key) != width for key in keys):
            err(f"{label}: keys are not {len(times)} poses of {width} numbers")
    for kind in arms.KINDS:
        path = ASSETS / "arms_motion" / f"{kind}.json"
        motion = load(path)
        if motion is None:
            err(f"assets/{MOD}/arms_motion/{kind}.json is missing")
            continue
        if kind in arms_moves.MOVES and motion != json.loads(json.dumps(arms_motion.kind_json(arms_moves.MOVES[kind]))):
            err(f"arms_motion/{kind}.json is not what tools/arms_motion.py writes (run tools/generate_material_data.py)")
        if len(motion["hold"]) != size or (motion["use"] is not None and len(motion["use"]) != size) or len(motion["fp_hold"]) != 6:
            err(f"arms_motion/{kind}.json: a hold or use pose is the wrong size")
        if not motion["attacks"]:
            err(f"arms_motion/{kind}.json has no attacks")
        for attack in motion["attacks"] + motion.get("arts", []):
            label = f"arms_motion/{kind}.json {attack['name']}"
            track(label, attack["times"], attack["tension"], attack["keys"], size)
            track(label + " (first person)", attack["fp_times"], attack["fp_tension"], attack["fp_keys"], 6)
        for art in motion.get("arts", []):
            if not art["ticks"] or art["ticks"] <= 0 or (art["spin"] is not None and len(art["spin"]) != len(art["times"])):
                err(f"arms_motion/{kind}.json {art['name']}: an art clip needs its ticks, and a spin a turn for each key")
            if art["spin"] and art["spin"][-1] % 360 != 0:
                err(f"arms_motion/{kind}.json {art['name']}: a spin must end a whole number of turns round, or the body snaps")
        if bool(motion.get("arts")) != (kind in arms.ARTS):
            err(f"arms_motion/{kind}.json: has art clips if and only if the kind has an art")
    if "WeaponArtPayload.TYPE" not in (client / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client" / "JugcraftClient.java").read_text(encoding="utf-8"):
        err("JugcraftClient does not receive WeaponArtPayload (no client would see a weapon art)")
    mixins = load(client / "resources" / f"{MOD}.client.mixins.json") or {}
    for mixin in ("ArmsRenderStateMixin", "ArmsHumanoidModelMixin", "ArmsItemInHandLayerMixin", "ArmsFirstPersonMixin"):
        if mixin not in mixins.get("client", []):
            err(f"{MOD}.client.mixins.json does not list {mixin}")


def check_flail_heads():
    """The flail's swinging head (tools/arms_heads.py, client/arms/FlailHeads.java): arms_heads.json is what the generator
    writes; each head kind's link and ball models exist, have no parent (they are placed exactly, by Java) and no two
    faces facing one way in one plane; each flail's in-hand model is its handle alone, its item definition picks the
    head parts by FlailHeads' custom_model_data strings, and its _model texture stays one frame; and the Java and the
    mixins name the same strings and hooks."""
    import arms_art
    import arms_heads
    table = load(ASSETS / "arms_heads.json")
    if table != json.loads(json.dumps(arms.heads_table())):
        err("arms_heads.json is not what tools/arms.py writes (run tools/generate_material_data.py)")
        table = table or {}
    shared = [f"arms_{kind}" for kind in arms_heads.HEADS] + list(arms_heads.VARIANT_HEADS)
    for name in shared:
        for part in ("link", "ball"):
            model = load(ASSETS / "models" / "item" / f"{name}_{part}.json") or {}
            if "parent" in model or "display" in model or not model.get("elements"):
                err(f"models/item/{name}_{part}.json must be elements with no parent and no display transforms")
                continue
            bad = arms_heads.coplanar(model["elements"])
            if bad:
                err(f"models/item/{name}_{part}.json: faces sharing a plane (they would flicker): {bad[:4]}")
    for kind in arms_heads.HEADS:
        # The handle alone: no box of the in-hand model reaches past the eye the chain hangs from.
        held = load(ASSETS / "models" / "item" / f"arms_{kind}.json") or {}
        grip_model, unit, grip, eye = arms_art.head_layout(kind, arms.KINDS[kind]["held"])
        reach = (eye - grip + 1.5) * unit
        for element in held.get("elements", []):
            if element["to"][1] - grip_model[1] > reach + 1e-3:
                err(f"models/item/arms_{kind}.json reaches past the eye: its head should be drawn live, not modelled")
                break
    for item in arms.items():
        metal, kind = arms.split(item)
        if kind not in arms_heads.HEADS:
            continue
        if f"{MOD}:{item}" not in table:
            err(f"arms_heads.json has no entry for {item}")
        definition = (load(ASSETS / "items" / f"{item}.json") or {}).get("model", {})
        cases = {case.get("when"): case["model"].get("model") for case in definition.get("cases", [])}
        if (definition.get("property") != "minecraft:custom_model_data"
                or cases.get(arms_heads.LINK_CASE) != f"{MOD}:item/{item}_link"
                or cases.get(arms_heads.BALL_CASE) != f"{MOD}:item/{item}_ball"):
            err(f"items/{item}.json must pick {item}_link and {item}_ball by custom_model_data")
        for part in ("link", "ball"):
            model = load(ASSETS / "models" / "item" / f"{item}_{part}.json") or {}
            if model.get("parent") != f"{MOD}:item/arms_{kind}_{part}" or model.get("textures", {}).get("tex") != f"{MOD}:item/{item}_model":
                err(f"models/item/{item}_{part}.json must be arms_{kind}_{part} on {item}_model")
        tex = ASSETS / "textures" / "item"
        if (tex / f"{item}_model.png.mcmeta").is_file():
            err(f"textures/item/{item}_model.png must stay one frame (the head swings live, not in the texture)")
    # The Arms VII variants whose head swings: handle-only in the hand, their parts picked by FlailHeads' strings.
    import arms_variants_art
    for name in arms_heads.VARIANT_HEADS:
        if f"{MOD}:{name}" not in table:
            err(f"arms_heads.json has no entry for {name}")
        definition = (load(ASSETS / "items" / f"{name}.json") or {}).get("model", {})
        cases = {case.get("when"): case["model"].get("model") for case in definition.get("cases", [])}
        if (definition.get("property") != "minecraft:custom_model_data" or cases.get(arms_heads.LINK_CASE) != f"{MOD}:item/{name}_link"
                or cases.get(arms_heads.BALL_CASE) != f"{MOD}:item/{name}_ball"):
            err(f"items/{name}.json must pick {name}_link and {name}_ball by custom_model_data")
        held = load(ASSETS / "models" / "item" / f"{name}_in_hand.json") or {}
        grip_model, unit, grip, eye = arms_variants_art.head_layout(name, arms.KINDS[arms_variants.kind(name)]["held"])
        if any(e["to"][1] - grip_model[1] > (eye - grip + 1.5) * unit + 1e-3 for e in held.get("elements", [])):
            err(f"models/item/{name}_in_hand.json reaches past the eye: its head should be drawn live, not modelled")
        if (ASSETS / "textures" / "item" / f"{name}_model.png.mcmeta").is_file():
            err(f"textures/item/{name}_model.png must stay one frame")
    java = (ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client" / "arms"
            / "FlailHeads.java").read_text(encoding="utf-8")
    for name, value in (("LINK", arms_heads.LINK_CASE), ("BALL", arms_heads.BALL_CASE)):
        if f'{name} = "{value}";' not in java:
            err(f"FlailHeads.{name} differs from tools/arms_heads.py ({value})")
    mixins = ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "mixin" / "client"
    for mixin, calls in (("ArmsRenderStateMixin", ("FlailHeads.extract",)),
                         ("ArmsHumanoidModelMixin", ("FlailHeads.pose",)),
                         ("ArmsItemInHandLayerMixin", ("FlailHeads.root", "FlailHeads.submitThirdPerson")),
                         ("ArmsFirstPersonMixin", ("FlailHeads.beginFirstPerson", "FlailHeads.firstPersonFrame",
                                                   "FlailHeads.endFirstPerson"))):
        text = (mixins / f"{mixin}.java").read_text(encoding="utf-8")
        for call in calls:
            if call not in text:
                err(f"{mixin} does not call {call} (the flail's head would not be drawn, or not kept clear of the body)")
    client = (ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client" / "JugcraftClient.java").read_text(encoding="utf-8")
    if "FlailHeads.load()" not in client or "FlailHeads.register()" not in client:
        err("JugcraftClient does not load and register FlailHeads")


def check_end_shares():
    """End biomes give shares, which tools/end_noise.py turns into Fabric weights: highlands shares leave vanilla's End
    Highlands some, and one barrens biome at most, keyed by vanilla's End Highlands (the only case the weight maths
    covers). The noise quantiles rise from 0 to 1."""
    import end_noise
    quantiles = end_noise.QUANTILES
    if quantiles[0] != 0 or quantiles[-1] != 1 or any(b <= a for a, b in zip(quantiles, quantiles[1:])):
        err("tools/end_noise.py QUANTILES must rise from 0 to 1")
    ends = {name: info["end"] for name, info in bm.BIOMES.items() if info.get("dimension") == "end"}
    highlands = [end["share"] for end in ends.values() if end.get("zone") == "highlands"]
    barrens = [name for name, end in ends.items() if end.get("zone") == "barrens"]
    for name, end in ends.items():
        if end.get("zone") not in ("highlands", "barrens") or not 0 < end.get("share", 0) < 1 or "weight" in end:
            err(f"{name}: an End biome needs a zone (highlands or barrens) and a share between 0 and 1, not a weight")
    if not 0 < 1 - sum(highlands) < 1:
        err(f"End highlands shares {highlands} must leave vanilla's End Highlands a share")
    if len(barrens) > 1 or any(ends[name].get("highlands") != "minecraft:end_highlands" for name in barrens):
        err(f"End barrens {barrens}: at most one, keyed by minecraft:end_highlands")


NESTED_PLACED = ("default", "feature_true", "feature_false", "vegetation_feature")


def check_nested_features():
    """A placed feature inside another feature (a random selector's picks, a vegetation patch's plant) must not have a
    biome filter: only a biome's own top-level features know their biome, and a nested one with the filter throws while
    the chunk generates, which stops that chunk for good. Vanilla's top-level features (those its biomes list) have the
    filter, so they cannot be nested either; their checked forms (birch_bees_0002, super_birch_bees ...) can."""
    from biome_bases import BASES
    vanilla_top = {feature for base in BASES.values() for step in base["steps"] for feature in step}
    placed = DATA / MOD / "worldgen" / "placed_feature"

    def check(holder, where):
        if isinstance(holder, dict):
            if any(m.get("type") == "minecraft:biome" for m in holder.get("placement", [])):
                err(f"{where}: a nested placed feature has a biome filter")
            if isinstance(holder.get("feature"), dict):
                walk(holder["feature"], where)
            return
        ns, path = split(str(holder))
        if ns == MOD:
            nested = load(placed / f"{path}.json") or {}
            if any(m.get("type") == "minecraft:biome" for m in nested.get("placement", [])):
                err(f"{where}: nests {holder}, which has a biome filter")
        elif holder in vanilla_top:
            err(f"{where}: nests vanilla's top-level {holder}, which has a biome filter")

    def walk(feature, where):
        for key in NESTED_PLACED:
            if key in feature:
                check(feature[key], where)
        kind = feature.get("type")
        if kind == "minecraft:random_selector":
            for entry in feature.get("features", []):
                check(entry["feature"], where)
        elif kind == "minecraft:simple_random_selector" and isinstance(feature.get("features"), list):
            for entry in feature["features"]:
                check(entry, where)
        elif kind in ("minecraft:random_patch", "minecraft:root_system") and "feature" in feature:
            check(feature["feature"], where)

    for path in sorted((DATA / MOD / "worldgen" / "feature").glob("*.json")):
        walk(load(path) or {}, path.name)


def check_seasons():
    """The seasons biome tags match tools/seasons.py, seasonal snow is registered as data says, and the palette and
    calendar days are in the year."""
    java = (SEASON_JAVA / "JugcraftSeasons.java").read_text(encoding="utf-8")
    for tag_id, biomes in ((seasons.TAG, seasons.BIOMES), (seasons.WINTER_SNOW_TAG, seasons.WINTER_SNOW)):
        ns, path = split(tag_id)
        tag = load(DATA / ns / "tags" / "worldgen" / "biome" / f"{path}.json") or {}
        if tag.get("values") != biomes:
            err(f"#{tag_id} {tag.get('values')} != tools/seasons.py {biomes}")
        if f'Jugcraft.id("{path}")' not in java:
            err(f"JugcraftSeasons does not read #{tag_id}")
    if not set(seasons.WINTER_SNOW) - {"minecraft:pale_garden"} <= set(seasons.BIOMES):
        err("Every winter-snow biome but the pale garden must have seasons")
    snow = (SEASON_JAVA / "SeasonalSnow.java").read_text(encoding="utf-8")
    if f'ID = "{seasons.SNOW_BLOCK}"' not in snow:
        err(f"SeasonalSnow.ID is not {seasons.SNOW_BLOCK}")
    for registry, tag in (("block", "minecraft:snow"), ("block", "minecraft:mineable/shovel")):
        ns, path = split(tag)
        if f"{MOD}:{seasons.SNOW_BLOCK}" not in (load(DATA / ns / "tags" / registry / f"{path}.json") or {}).get("values", []):
            err(f"{seasons.SNOW_BLOCK} is not in #{tag}")
    days = [int(day) for day in re.findall(r"new Keyframe\((\d+),", (SEASON_JAVA / "SeasonPalette.java").read_text(encoding="utf-8"))]
    if not days or days != sorted(set(days)) or days[0] < 1 or days[-1] > 365:
        err(f"SeasonPalette keyframe days {days} must rise strictly within 1..365")
    modes = re.findall(r"([A-Z]+)\((-?\d+)\)", (SEASON_JAVA / "SeasonCalendar.java").read_text(encoding="utf-8"))
    for mode, day in modes:
        if not (int(day) == -1 if mode == "AUTO" else 0 <= int(day) <= 365):
            err(f"SeasonCalendar.Mode.{mode} day {day} is outside the year")
    options = CONFIG.read_text(encoding="utf-8")
    for option in ("seasons.mode", "seasons.hemisphere", "seasons.timezone", "seasons.snow", "seasons.snow_depth",
                   "harvest_feast", "harvest_feast.days", "december"):
        if f'"{option}"' not in options:
            err(f"JugcraftConfig.TEXT_OPTIONS has no {option}")


def check_region_rules():
    """Region rules: valid layouts, bands and biomes; no two rules can match the same entry in the same layout; and the
    generated /jugcraft/region_rules.json is current."""
    from biome_bases import BASES
    for rule in bm.RULES:
        where = f"rule {rule['replaces']} -> {rule['biome']}"
        if not rule["layouts"] or any(not 0 <= layout < bm.LAYOUTS for layout in rule["layouts"]):
            err(f"{where}: layouts {rule['layouts']} outside 0..{bm.LAYOUTS - 1}")
        for name in ("temperature", "humidity"):
            low, high = rule[name]
            if not 0 <= low <= high <= 4:
                err(f"{where}: {name} bands {rule[name]}")
        if rule["weirdness"] not in (-1, 0, 1):
            err(f"{where}: weirdness {rule['weirdness']}")
        if rule["replaces"].split(":")[1] not in BASES:
            err(f"{where}: {rule['replaces']} is not a vanilla biome")
        if rule["biome"].split(":")[1] not in bm.BIOMES:
            err(f"{where}: {rule['biome']} is not a biome in tools/biomes.py")
    for i, a in enumerate(bm.RULES):
        for b in bm.RULES[i + 1:]:
            if (a["replaces"] == b["replaces"] and set(a["layouts"]) & set(b["layouts"])
                    and a["temperature"][0] <= b["temperature"][1] and b["temperature"][0] <= a["temperature"][1]
                    and a["humidity"][0] <= b["humidity"][1] and b["humidity"][0] <= a["humidity"][1]
                    and (a["weirdness"] == 0 or b["weirdness"] == 0 or a["weirdness"] == b["weirdness"])):
                err(f"Region rules overlap: {a['replaces']} -> {a['biome']} and -> {b['biome']} in layouts "
                    f"{sorted(set(a['layouts']) & set(b['layouts']))}")
    if load(RES / MOD / "region_rules.json") != bm.rules_file():
        err("src/main/resources/jugcraft/region_rules.json is out of date (run tools/generate_material_data.py)")
    if load(RES / MOD / "dimension_biomes.json") != bm.dimension_file():
        err("src/main/resources/jugcraft/dimension_biomes.json is out of date (run tools/generate_material_data.py)")
    if "JugcraftDimensions.register();" not in (JAVA_ROOT / "Jugcraft.java").read_text(encoding="utf-8"):
        err("Jugcraft.java does not place the Nether and End biomes (JugcraftDimensions.register)")


def check_biomes():
    """The biomes branch: Java's region rules and options match tools/biomes.py, each biome's files are complete,
    every feature a biome or tree selector names exists, and the four-season biomes have seasons."""
    java = (JAVA_ROOT / "biome" / "JugcraftRegions.java").read_text(encoding="utf-8")
    check_region_rules()
    for expected in (f'FEATURE = "{bm.FEATURE}"', f"SIZE = {bm.REGIONS['size']};", f"SHARE = {bm.REGIONS['share']};",
                     f"LAYOUTS = {bm.LAYOUTS};",
                     "TEMPERATURE_BANDS = {" + ", ".join(f"{v}F" for v in bm.TEMPERATURE_BANDS) + "}",
                     "HUMIDITY_BANDS = {" + ", ".join(f"{v}F" for v in bm.HUMIDITY_BANDS) + "}"):
        if expected not in java:
            err(f"JugcraftRegions.java has no {expected} (tools/biomes.py)")
    config = CONFIG.read_text(encoding="utf-8")
    if (f'"biomes.region_size", "{bm.REGIONS["size"]}"' not in config or f'"biomes.region_share", "{bm.REGIONS["share"]}"' not in config
            or bm.FEATURE not in FEATURES):
        err("JugcraftConfig's biomes switch or region options differ from tools/biomes.py")
    check_end_shares()
    placed = DATA / MOD / "worldgen" / "placed_feature"
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    for name, info in bm.BIOMES.items():
        if info.get("dimension") in ("nether", "end"):
            if f"{MOD}:{name}" in {rule["biome"] for rule in bm.RULES} or "surface" in info or info["seasons"]:
                err(f"{name}: a {info['dimension']} biome has region rules, an Overworld surface or seasons")
        elif f"{MOD}:{name}" not in {rule["biome"] for rule in bm.RULES}:
            err(f"No region rule places the {name} biome")
        if lang.get(f"biome.{MOD}.{name}") != info["display"]:
            err(f"No name for the {name} biome")
        for step in biomes_data.steps(name):
            for feature in step:
                ns, path = split(feature)
                if ns == MOD and not (placed / f"{path}.json").is_file():
                    err(f"{name}: unknown placed feature {feature}")
        picks = [] if info["trees"] is None else [info["trees"]["default"]] + [feature for feature, _ in info["trees"]["picks"]]
        for feature in picks:
            ns, path = split(feature)
            if ns == MOD and not (placed / f"{path}.json").is_file():
                err(f"{name}'s trees: unknown placed feature {feature}")
        if info["seasons"] != (f"{MOD}:{name}" in seasons.BIOMES):
            err(f"{name}: seasons {info['seasons']} but seasons.BIOMES says otherwise")
    features = DATA / MOD / "worldgen" / "feature"
    for shape, info in tr.SHAPES.items():
        vanilla = info["wood"] in ("minecraft:oak", "minecraft:birch", "minecraft:spruce", "minecraft:jungle", "minecraft:acacia",
                                   "minecraft:dark_oak", "minecraft:cherry", "minecraft:mangrove", "minecraft:pale_oak")
        if not vanilla and (info["wood"] not in ag.WOOD_SETS or (info["foliage"] and info["wood"] not in ag.TREES)):
            err(f"Tree shape {shape} grows unknown wood or leaves ({info['wood']})")
            continue
        # Generated seasonal leaves need the decorator to start in today's look (world generation skips onPlace).
        seasonal = bool(info["foliage"]) and not vanilla and ag.TREES[info["wood"]]["season"] is not None
        decorators = [decorator.get("type") for decorator in (load(features / f"{shape}.json") or {}).get("decorators", [])]
        if seasonal != (f"{MOD}:{tr.DECORATOR}" in decorators):
            err(f"Tree shape {shape}: the {MOD}:{tr.DECORATOR} decorator belongs on exactly the trees with seasonal leaves")
    agriculture = (JAVA_ROOT / "agriculture" / "JugcraftAgriculture.java").read_text(encoding="utf-8")
    if f'TREE_DECORATOR_TYPE, Jugcraft.id("{tr.DECORATOR}")' not in agriculture:
        err(f"JugcraftAgriculture.java does not register the {tr.DECORATOR} tree decorator")
    # Wild plants: Java registers every kind tools/plants.py uses.
    for plant, info in plants.PLANTS.items():
        if info["kind"] not in plants.KINDS or f'case "{info["kind"]}" ->' not in agriculture:
            err(f"Wild plant {plant}: JugcraftAgriculture.registerWildPlants has no case for kind {info['kind']}")
    # Giant trees: four saplings in a square grow them (GiantSaplingBlock), so Java's growers match agriculture.TREES.
    giants = {tree: info["giant"] for tree, info in ag.TREES.items() if info.get("giant")}
    for tree, shape in giants.items():
        grower = f"{shape.upper()}_GROWER"
        if (f'{grower} = grower("{shape}")' not in agriculture or f'"{tree}", {grower}' not in agriculture
                or not tr.SHAPES.get(shape, {}).get("giant") or tr.SHAPES[shape]["wood"] != tree):
            err(f"The {tree} tree's giant ({shape}) is not a giant {tree} shape in tools/trees.py with its grower in GIANT_GROWERS")
    for shape, info in tr.SHAPES.items():
        if info.get("giant") and shape not in giants.values():
            err(f"Tree shape {shape} is giant but no tree's saplings grow it (agriculture.TREES \"giant\")")


def check_wood_switches():
    """TREES.md rule 4: a wood's hand and sawmill recipes, and its tree's leaves and sapling, are on whenever any feature
    switch that places its logs is on. Every switch that places a tree shape, fallen log or placed tree of a Jugcraft wood
    (a biome's trees or extras, Alpine Spawn's selector, agriculture's wild patches such as the chestnut trees) must be in
    that wood's agriculture.WOOD_SWITCHES."""
    if set(ag.WOOD_SWITCHES) != set(ag.WOOD_SETS):
        err(f"agriculture.WOOD_SWITCHES {sorted(ag.WOOD_SWITCHES)} should list exactly the wood sets {sorted(ag.WOOD_SETS)}")
    for wood, switches in ag.WOOD_SWITCHES.items():
        if (not isinstance(switches, list) or not switches or len(set(switches)) != len(switches)
                or any(switch not in FEATURES for switch in switches)):
            err(f"agriculture.WOOD_SWITCHES[{wood!r}] = {switches} is not a list of known feature switches")
    placers = {}  # a placed or configured feature's path, without "_checked": the switches that place it

    def placed_by(ref, switch):
        ns, path = split(ref)
        if ns == MOD:
            placers.setdefault(path.removesuffix("_checked"), set()).add(switch)
    for info in bm.BIOMES.values():
        if info["trees"] is not None:
            for ref in [info["trees"]["default"]] + [feature for feature, _ in info["trees"]["picks"]]:
                placed_by(ref, bm.FEATURE)
        for extra in info.get("extras", []):
            if "feature" in bm.EXTRAS.get(extra, {}):
                placed_by(bm.EXTRAS[extra]["feature"], bm.FEATURE)
    for key in ("larch", "spruce"):
        placed_by(al.TREES[key], al.FEATURE)
    # Agriculture's wild patches (JugcraftAgriculture.registerWorldgen, under the agriculture switch): the wild chestnut
    # trees, and anything else a patch_<name> placed feature places. A patch placed anywhere else has no known switch.
    agriculture = (AGRICULTURE_JAVA / "JugcraftAgriculture.java").read_text(encoding="utf-8")
    gated = re.search(r"private static void registerWorldgen\(\) \{\n\t\tif \(!JugcraftConfig\.isFeatureEnabled\(FEATURE\)\) \{"
                      r"(.*?)\n\t\}\n", agriculture, re.S)
    if not gated:
        err("JugcraftAgriculture.registerWorldgen() not found, or not gated by the agriculture switch first")
    inside = set(re.findall(r'wildPatch\("([a-z_]+)"', gated.group(1))) if gated else set()
    for name in re.findall(r'wildPatch\("([a-z_]+)"', agriculture):
        patch = load(DATA / MOD / "worldgen" / "placed_feature" / f"patch_{name}.json")
        if patch is not None:
            placed_by(patch["feature"], ag.FEATURE if name in inside else f"(patch_{name}, outside registerWorldgen)")
    woods = {shape: info["wood"] for shape, info in tr.SHAPES.items() if info["wood"] in ag.WOOD_SETS}
    woods.update({f"fallen_{wood}_tree": wood for wood in tr.FALLEN})
    for placed, (feature, _) in bm.PLACED_TREES.items():
        ns, path = split(feature)
        if ns == MOD and path in ag.WOOD_SETS:
            woods[placed.removesuffix("_checked")] = path
    for what, wood in woods.items():
        switches = ag.WOOD_SWITCHES.get(wood, [])
        missing = placers.get(what, set()) - (set(switches) if isinstance(switches, list) else {switches})
        if missing:
            err(f"{what} is placed by {sorted(missing)}, which the {wood} wood's switches {ag.WOOD_SWITCHES.get(wood)} "
                f"lack (agriculture.WOOD_SWITCHES, TREES.md rule 4)")


def check_alpine():
    """Alpine Spawn: Java's placement and spawn numbers match tools/alpine.py, and its data is all there."""
    java = (WORLD_JAVA / "AlpineSpawn.java").read_text(encoding="utf-8")
    for expected in (f'FEATURE = "{al.FEATURE}"', f'Jugcraft.id("{al.BIOME}")', f"PLATEAU_TEMPERATURE = {al.PLATEAU['temperature']};",
                     f"PLATEAU_HUMIDITY_MIN = {al.PLATEAU['humidity'][0]};", f"PLATEAU_HUMIDITY_MAX = {al.PLATEAU['humidity'][-1]};",
                     f"SEARCH_RADIUS = {al.SPAWN['radius']};", f"SEARCH_STEP = {al.SPAWN['step']};",
                     f"VILLAGE_CELLS = {al.SPAWN['village_cells']};", f'Jugcraft.id("{al.VILLAGE_STRUCTURES.split(":")[1]}")'):
        if expected not in java:
            err(f"AlpineSpawn.java has no {expected} (tools/alpine.py)")
    if al.FEATURE not in FEATURES:
        err(f"{al.FEATURE} is not a feature switch")
    if '"alpine_spawn.start"' not in CONFIG.read_text(encoding="utf-8"):
        err("JugcraftConfig has no alpine_spawn.start")
    folder = DATA / MOD / "worldgen"
    for path in sorted((folder / "biome").glob("*.json")):
        # 26.3 reads an attribute as either a plain value or {"modifier", "argument"}; an argument alone fails to load.
        for name, value in ((load(path) or {}).get("attributes") or {}).items():
            if isinstance(value, dict) and "argument" in value and "modifier" not in value:
                err(f"{path.name}: attribute {name} has an argument but no modifier")
    biome = load(folder / "biome" / f"{al.BIOME}.json") or {}
    if biome.get("temperature") != al.TEMPERATURE or biome.get("downfall") != al.DOWNFALL:
        err(f"{al.BIOME}.json climate differs from tools/alpine.py")
    for step in biome.get("features", []):
        for feature in step:
            ns, path = split(feature)
            if ns == MOD and not (folder / "placed_feature" / f"{path}.json").is_file():
                err(f"{al.BIOME}.json: unknown placed feature {feature}")
    structure = load(folder / "structure" / f"{al.VILLAGE}.json") or {}
    if structure.get("biomes") != f"#{al.VILLAGE_TAG}":
        err(f"{al.VILLAGE}.json should generate in #{al.VILLAGE_TAG}")
    placement = (load(folder / "structure_set" / f"{al.VILLAGE_SET}.json") or {}).get("placement", {})
    if [placement.get(k) for k in ("spacing", "separation", "salt")] != [al.VILLAGES[k] for k in ("spacing", "separation", "salt")]:
        err(f"{al.VILLAGE_SET}.json differs from VILLAGES in tools/alpine.py")
    if f"{MOD}:{al.BIOME}" not in seasons.BIOMES:
        err("Alpine Spawn has no seasons")
    if al.SPAWN["village_cells"] * al.VILLAGES["spacing"] * 16 < al.SPAWN["radius"]:
        err("The start search looks for alpine villages less far than for the biome (SPAWN in tools/alpine.py)")
    selector = load(folder / "feature" / f"{al.TREES['feature']}.json") or {}
    picks = {selector.get("default")} | {entry.get("feature") for entry in selector.get("features", [])}
    if picks != {al.TREES["larch"], al.TREES["spruce"]}:
        err(f"{al.TREES['feature']}.json should pick larches and spruces, found {sorted(map(str, picks))}")
    for placed in picks:
        ns, path = split(str(placed))
        if ns == MOD and not (folder / "placed_feature" / f"{path}.json").is_file():
            err(f"{al.TREES['feature']}.json: unknown placed feature {placed}")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    if lang.get(f"biome.{MOD}.{al.BIOME}") != al.DISPLAY:
        err(f"No name for the {al.BIOME} biome")


def check_java():
    source = JAVA.read_text(encoding="utf-8")
    declared = {}
    for name, chain in re.findall(r'MetalFamily\.builder\("([a-z_]+)"\)([^;]*)\.build\(\)', source):
        declared[name] = {"mined": ".mined()" in chain, "extras": re.findall(r'extraItem\("([a-z_]+)"\)', chain),
                          "lore": ".lore()" in chain}
    expected = {name: {"mined": info["mined"], "extras": info.get("extras", []), "lore": "lore" in info}
                for name, info in METALS.items()}
    if declared != expected:
        err(f"JugcraftMaterials.java metals {declared} != tools/materials.py {expected}")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    for metal, info in METALS.items():
        if "lore" in info and lang.get(f"tooltip.{MOD}.{metal}_ingot") != info["lore"]:
            err(f"No lore line tooltip.{MOD}.{metal}_ingot (run tools/generate_material_data.py)")

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

    # Every party action result needs a chat message (party/PartyCommands.java shows them).
    party_source = (JAVA_ROOT / "party" / "PartyManager.java").read_text(encoding="utf-8")
    results = re.findall(r"\b([A-Z_]+)\b", re.search(r"enum Result \{([^}]*)\}", party_source).group(1))
    for result in results:
        if f"error.{result.lower()}" not in PARTY_LANG:
            err(f"PartyManager.Result.{result} has no message in tools/party.py")

    worldgen = WORLDGEN.read_text(encoding="utf-8")
    placed = sorted(p.stem[4:] for p in (DATA / MOD / "worldgen" / "placed_feature").glob("ore_*.json"))
    in_java = sorted(set(re.findall(r'\{"([a-z_]+)", "[a-z_]+"\}', worldgen)) | set(re.findall(r'add\("([a-z_]+)"', worldgen)))
    if placed != in_java:
        err(f"JugcraftWorldgen adds {in_java}, data defines {placed}")
    owners = {placed: feature for placed, (_, feature) in ore_gen_owners().items()}
    owners.update({rock: info["feature"] for rock, info in ROCKS.items()})
    for name, feature in re.findall(r'\{"([a-z_]+)", "([a-z_]+)"\}', worldgen) + re.findall(r'add\("([a-z_]+)", "([a-z_]+)"', worldgen):
        owner = owners.get(name)
        if feature != owner:
            err(f"JugcraftWorldgen gates {name} by {feature}, expected {owner}")
    # Veins with "biomes" on their worldgen entry go in the biomeOres list, placed by the biome tag has_ore/<name>; every
    # other metal or mineral vein in the ores list, everywhere in the Overworld.
    def java_list(name):
        match = re.search(name + r" = \{([^;]*)\};", worldgen)
        return re.findall(r'\{"([a-z_]+)", "[a-z_]+"\}', match.group(1)) if match else []
    gens = {placed: gen for name, info in list(METALS.items()) + list(MINERALS.items()) for placed, gen in ore_gens(name, info)}
    limited = sorted(placed for placed, gen in gens.items() if gen.get("biomes"))
    if sorted(java_list("biomeOres")) != limited:
        err(f"JugcraftWorldgen.biomeOres {sorted(java_list('biomeOres'))} != the veins with biomes in tools/materials.py {limited}")
    if sorted(java_list("ores")) != sorted(set(gens) - set(limited)):
        err(f"JugcraftWorldgen.ores {sorted(java_list('ores'))} != the Overworld-wide veins in tools/materials.py")
    if limited and 'Jugcraft.id("has_ore/" + ore[0])' not in worldgen:
        err("JugcraftWorldgen does not place biomeOres by their biome tag jugcraft:has_ore/<name>")
    for placed in limited:
        tag = load(DATA / MOD / "tags" / "worldgen" / "biome" / "has_ore" / f"{placed}.json") or {}
        if tag.get("values") != gens[placed]["biomes"]:
            err(f"#{MOD}:has_ore/{placed} {tag.get('values')} != tools/materials.py {gens[placed]['biomes']}")


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
                # A list is a wood's WOOD_SWITCHES: any of them loads the recipe.
                for one in [feature] if isinstance(feature, str) else feature:
                    if one not in FEATURES:
                        err(f"{label}: unknown feature {one}")
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
        match = re.search(r'(\w+)\("' + machine + r'", ([\d_]+), ([\d_]+), ([\d_]+), ([\d_]+),', kinds)
        if not match:
            err(f"MachineKind.java has no entry for {machine}")
            continue
        # The enum constant can differ from the block id (ARC_FURNACE is "arc_furnace_controller").
        constant = match.group(1)
        capacity, max_in, max_out, use = (int(v.replace("_", "")) for v in match.groups()[1:])
        if capacity != stats["capacity"]:
            err(f"{machine}: capacity {capacity} in Java, {stats['capacity']} in machines.py")
        expected_use = stats.get("use_per_tick", 0)
        if use != expected_use:
            err(f"{machine}: use {use} in Java, {expected_use} in machines.py")
        if "boost" in stats:
            if f'case {constant} -> "{stats["boost"]}";' not in kinds:
                err(f"{machine}: MachineKind.boostGas() is not {stats['boost']}")
            if stats["boost"] not in petro.GASES:
                err(f"{machine}: boost gas {stats['boost']} is not a gas in tools/petro.py")
            per_tick = re.search(r"case " + constant + r" -> (\w+);\s*(?:case|default)", kinds.split("public int boostPerTick()")[1])
            constants = dict(re.findall(r"public static final int (\w+) = ([\d_]+);", kinds))
            if not per_tick or int(constants.get(per_tick.group(1), "-1").replace("_", "")) != stats["boost_per_tick"]:
                err(f"{machine}: MachineKind.boostPerTick() does not give {stats['boost_per_tick']}")
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


def check_machine_rotors():
    """The turning parts client/MachineRotors draws (tools/machine_rotors.py, giant_models.ROTORS): every rotor names a
    big machine and the parts it needs, its textures exist, are still and opaque (the solid render type ignores alpha)
    and every UV stays inside its texture; the renderer reads the file; the static models of both styles keep clear of
    the blade; the sawmill's and sieve's elements stay inside their footprints (an element kept whole outside them
    stretches its texture and escapes the per-part separation), no face of theirs drawn with a stretched texture
    ("name!") is cut by a block seam (each piece would show the whole texture again, squeezed), and their running lamps
    are on the master block, the only one whose lit state changes."""
    import steampunk_models  # noqa: F401  (loads giant_models after the helpers it builds on)
    from giant_models import MODELS as GIANTS, ROTORS
    from large_machines import ENLARGED, FOOTPRINTS, MODELS as CLASSIC
    from model_writer import FACE_AXES, unpack
    data = load(ASSETS / "machine_rotor_quads.json")
    if data is None:
        return
    if set(data) != set(ROTORS):
        err(f"machine_rotor_quads.json has {sorted(data)}, giant_models.ROTORS {sorted(ROTORS)}: run generate_material_data.py")
    opaque = {}
    for name, rotor in data.items():
        if rotor.get("block") not in ENLARGED or rotor.get("axis") not in ("x", "y", "z"):
            err(f"machine rotor {name}: block {rotor.get('block')} is not a big machine, or its axis is wrong")
        if rotor.get("when", {}).get("compact") != "false":
            err(f"machine rotor {name} must only draw on the big machine (when compact=false)")
        for key in ("center", "property", "speed", "quads"):
            if key not in rotor:
                err(f"machine rotor {name} has no {key}")
        for quad in rotor.get("quads", []):
            texture = quad["texture"]
            png = ASSETS / "textures" / "block" / f"{texture}.png"
            if texture not in opaque:
                if not png.is_file() or png.with_name(png.name + ".mcmeta").is_file():
                    err(f"machine rotor {name}: texture {texture} is missing or animated (the renderer draws the whole image)")
                    opaque[texture] = True
                else:
                    with Image.open(png) as img:
                        opaque[texture] = img.convert("RGBA").getextrema()[3][0] == 255
                    if not opaque[texture]:
                        err(f"machine rotor {name}: texture {texture} has see-through pixels (rotors are drawn solid)")
            if len(quad["vertices"]) != 4 or any(not (0 <= v[3] <= 1 and 0 <= v[4] <= 1) for v in quad["vertices"]):
                err(f"machine rotor {name}: a {texture} quad has a UV outside its texture (nothing may rely on wrapping)")
                break
    client = ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client"
    rotors_java = (client / "MachineRotors.java").read_text(encoding="utf-8") if (client / "MachineRotors.java").is_file() else ""
    renderer = (client / "WindTurbineRenderer.java").read_text(encoding="utf-8")
    if '"machine_rotor_quads.json"' not in rotors_java or "MachineRotors.extract" not in renderer or "MachineRotors.submit" not in renderer:
        err("client/MachineRotors must read machine_rotor_quads.json and the machine renderer must extract and submit it")
    for name, rotor in ROTORS.items():
        _, cy, cz = rotor["center"]
        for style, models in (("steampunk", GIANTS), ("classic", CLASSIC)):
            for item in models[rotor["block"]]:
                frm, to, _texture, options = unpack(item)
                if options.get("rotation"):
                    continue
                for x0, x1, radius in rotor.get("clear", []):
                    if frm[0] >= x1 or to[0] <= x0:
                        continue
                    dy = max(frm[1] - cy, 0, cy - to[1])
                    dz = max(frm[2] - cz, 0, cz - to[2])
                    if dy * dy + dz * dz < radius * radius:
                        err(f"{style} {rotor['block']}: element {frm}..{to} cuts into the turning {name} (x {x0}..{x1}, r {radius})")
    for machine in ("sawmill", "sieve"):
        footprint = FOOTPRINTS[machine]
        for item in GIANTS[machine]:
            frm, to, _texture, options = unpack(item)
            if options.get("rotation"):
                continue
            covered = 0.0
            for offset in footprint:
                low = [offset[axis] * 16 for axis in range(3)]
                a = [max(frm[axis], low[axis]) for axis in range(3)]
                b = [min(to[axis], low[axis] + 16) for axis in range(3)]
                if all(a[axis] < b[axis] for axis in range(3)):
                    covered += (b[0] - a[0]) * (b[1] - a[1]) * (b[2] - a[2])
            if abs(covered - (to[0] - frm[0]) * (to[1] - frm[1]) * (to[2] - frm[2])) > 1e-6:
                err(f"{machine}: element {frm}..{to} reaches outside its footprint")
        for style, models in (("steampunk", GIANTS), ("classic", CLASSIC)):
            for item in models[machine]:
                frm, to, texture, options = unpack(item)
                if options.get("rotation"):
                    continue  # kept whole on one part, never cut
                faces = texture if isinstance(texture, dict) else {"*": texture}
                for face, (u_axis, v_axis, _, _) in FACE_AXES.items():
                    name = faces.get(face, faces.get("*"))
                    if name and name.endswith("!") and any(
                            frm[axis] < seam < to[axis] for axis in (u_axis, v_axis) for seam in range(-64, 129, 16)):
                        err(f"{style} {machine}: the {face} face of {frm}..{to} stretches {name[:-1]} across a block seam "
                            "(each part would show the whole texture again): keep it inside one block")
        for index in range(len(footprint)):
            path = ASSETS / "models" / "block" / f"{machine}_part{index}.json"
            if index and '"#dr_amber"' in path.read_text(encoding="utf-8"):
                err(f"{machine}_part{index} has the amber running lamp: only the master block (part 0) lights up")


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
            refs += [step["item"] for step in page.get("steps", [])]
            for row in page.get("recipes", []):
                refs += [ref for ref, _ in row["in"]] + [row["out"][0]] + [ref for ref, _ in row.get("extra", [])]
    for ref in refs:
        if split(ref)[0] == MOD and split(ref)[1] not in registered:
            err(f"handbook: unknown item {ref}")


VANILLA_ADVANCEMENT_ROOTS = {"minecraft:story/root", "minecraft:nether/root", "minecraft:end/root", "minecraft:adventure/root",
                             "minecraft:husbandry/root"}


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
        # A vanilla tab's root may be the parent (the Halloween advancements live in Husbandry).
        if parent and parent not in VANILLA_ADVANCEMENT_ROOTS and split(parent)[1] not in names:
            err(f"advancement {path.stem}: missing parent {parent}")
def check_guide_books():
    """drone/GuideBooks.java's page counts match tools/guide_books.py; every page's screenshot exists at 512x288."""
    import guide_books
    java = (JAVA_ROOT / "drone" / "GuideBooks.java").read_text(encoding="utf-8")
    for item, const in (("drone_tower_manual", "MANUAL_PAGES"), ("creative_tower_guide", "CREATIVE_PAGES")):
        m = re.search(const + r" = (\d+);", java)
        if not m or int(m.group(1)) != guide_books.PAGE_COUNTS[item]:
            err(f"GuideBooks.{const} must be {guide_books.PAGE_COUNTS[item]} (pages in tools/guide_books.py)")
        for i, (heading, body, _) in enumerate(guide_books.BOOKS[item][1]):
            if len(body) > 300:
                err(f"{item} page {i + 1} is {len(body)} characters; keep it under 300 so it fits under its picture")
    for shot in guide_books.SCREENSHOTS:
        png = guide_books.SHOTS / f"{shot}.png"
        if not png.is_file():
            err(f"guide screenshot {png.relative_to(ROOT)} is missing")
        else:
            with Image.open(png) as img:
                if img.size != (512, 288):
                    err(f"guide screenshot {shot}.png is {img.size}, expected 512x288")


def check_tower():
    """tower/JugcraftTower.java registers what tools/tower.py describes, and the tower data is generated."""
    import tower
    import tower_costs
    tower_costs.check(err)
    java = (JAVA_ROOT / "tower" / "JugcraftTower.java").read_text(encoding="utf-8")
    building = re.findall(r'\{"([a-z_]+)", "([a-z:0-9]+)"\}', re.search(r"BUILDING = \{(.*?)\};", java, re.S).group(1))
    expected = [(b, i["kind"] if i["kind"] != "light" else f"light:{i['light']}") for b, i in tower.BUILDING.items()]
    if building != expected:
        err(f"JugcraftTower.BUILDING {building} != tools/tower.py {expected}")
    variants = re.findall(r'"([a-z_]+)"', re.search(r"VARIANTS = \{(.*?)\};", java, re.S).group(1))
    if variants != [b for b, i in tower.BUILDING.items() if i.get("variants")]:
        err("JugcraftTower.VARIANTS differs from tools/tower.py")
    modules = re.findall(r'"([a-z_]+)"', re.search(r"MODULES = \{(.*?)\};", java, re.S).group(1))
    if modules != list(tower.MODULES):
        err("JugcraftTower.MODULES differs from tools/tower.py")
    for block in tower.FURNITURE:
        if f'furniture("{block}"' not in java:
            err(f"JugcraftTower does not register furniture {block}")
    if not (ROOT / "src" / "main" / "resources" / "data" / "jugcraft" / "drone_tower" / "tower.json.gz").exists():
        err("missing tower data: run tools/drone_tower.py")


def check_power_lines():
    """powerline/LineKind.java and client/LineRenderer.java match tools/power_lines.py, and the frame block has its data."""
    import power_lines
    kinds = re.findall(r'^\s+[A-Z_]+\("([a-z_]+)", Family\.([A-Z]+), (\d+), (\d+), (\d+), ([\d_]+)\)',
                       (JAVA_ROOT / "powerline" / "LineKind.java").read_text(encoding="utf-8"), re.M)
    found = {k: (f.lower(), int(t), int(c.replace("_", "")), int(h), int(r)) for k, f, t, h, r, c in kinds}
    expected = {k: (fam, tier, cap, h, r) for k, (_, fam, tier, cap, h, r) in power_lines.KINDS.items()}
    if list(found) != list(expected):
        err(f"LineKind.java kinds {list(found)} != tools/power_lines.py {list(expected)}")
    for kind, values in expected.items():
        if kind in found and found[kind] != values:
            err(f"LineKind {kind}: {found[kind]} in Java, {values} in tools/power_lines.py")
    renderer = (ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client"
                / "LineRenderer.java").read_text(encoding="utf-8")
    for name in power_lines.MATERIALS:
        col, row = power_lines.slot(name)
        if f"static final float[] {name.upper()} = region({col}, {row});" not in renderer:
            err(f"LineRenderer.java: material {name.upper()} is not region({col}, {row})")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    frame = power_lines.FRAME
    if f"block.{MOD}.{frame}" not in lang:
        err(f"Missing name for block {frame}")
    state = load(ASSETS / "blockstates" / f"{frame}.json")
    if state:
        for variant in state.get("variants", {}).values():
            model(variant["model"])
    if not (DATA / MOD / "loot_table" / "blocks" / f"{frame}.json").is_file():
        err(f"Missing loot table for {frame}")
    if not (DATA / MOD / "damage_type" / "electrocution.json").is_file():
        err("Missing damage type jugcraft:electrocution")
    for key in power_lines.LANG:
        if key not in lang:
            err(f"Missing lang {key}")
    java = "".join(p.read_text(encoding="utf-8") for p in (JAVA_ROOT / "powerline").glob("*.java"))
    for key in set(re.findall(r'"((?:message|tooltip)\.jugcraft\.power_line\.[a-z_]+)"', java)):
        if key not in lang:
            err(f"powerline/ uses lang key {key} that is not generated")


def check_drones():
    """drone/DroneTier.java, JugcraftDrones.PARTS and PlatformLayout match tools/drones.py."""
    java = JAVA_ROOT / "drone"
    tiers = re.findall(r"^\s+[A-Z_]+\((\d+), (\d+), (\d+), DroneSize\.([A-Z]+), (true|false)\)",
                       (java / "DroneTier.java").read_text(encoding="utf-8"), re.M)
    if len(tiers) != len(drones.DRONE_TIERS):
        err(f"DroneTier.java has {len(tiers)} tiers, tools/drones.py {len(drones.DRONE_TIERS)}")
    for number, (capacity, speed, upkeep, size, available) in enumerate(tiers, start=1):
        info = drones.DRONE_TIERS.get(number, {})
        found = {"capacity": int(capacity), "speed": int(speed), "upkeep": int(upkeep), "size": size.lower(),
                 "available": available == "true"}
        for key, value in found.items():
            if info.get(key) != value:
                err(f"drone tier {number}: {key} is {value} in Java, {info.get(key)} in tools/drones.py")
    parts = re.findall(r'"([a-z_]+)"', re.search(r"PARTS = \{([^}]*)\}", (java / "JugcraftDrones.java").read_text(encoding="utf-8")).group(1))
    if parts != list(drones.DRONE_PARTS):
        err(f"JugcraftDrones.PARTS {parts} != tools/drones.py {list(drones.DRONE_PARTS)}")
    layout = (java / "PlatformLayout.java").read_text(encoding="utf-8")
    if f"MAX_DRONES = {drones.MAX_DRONES};" not in layout:
        err("PlatformLayout.MAX_DRONES differs from tools/drones.py")
    if f"PAD_SIZE = {drones.PAD_SIZE};" not in layout:
        err("PlatformLayout.PAD_SIZE differs from tools/drones.py")
    if f"HEIGHT = {drones.PAD_PLATE_HEIGHT};" not in (java / "LandingPadBlock.java").read_text(encoding="utf-8"):
        err("LandingPadBlock.HEIGHT differs from tools/drones.py PAD_PLATE_HEIGHT")
    if f"PICKUP_SIZE = {drones.PICKUP_SIZE};" not in layout:
        err("PlatformLayout.PICKUP_SIZE differs from tools/drones.py")
    # The drone models' 32x32 fleet regions sit where tools/drone_textures.py draws them.
    import drone_textures
    model = (ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client"
             / "DroneModel.java").read_text(encoding="utf-8")
    for index, name in enumerate(drone_textures.FLEET_ORDER):
        x, y = drone_textures.fleet_slot(index)
        if f"static final float[] {name.upper()} = fleet({x}, {y});" not in model:
            err(f"DroneModel.java: fleet region {name.upper()} is not fleet({x}, {y})")
    holo = (java / "HoloTableBlock.java").read_text(encoding="utf-8")
    if f"SIZE = {drones.HOLO_SIZE};" not in holo or f"HEIGHT = {drones.HOLO_HEIGHT};" not in holo:
        err("HoloTableBlock SIZE/HEIGHT differ from tools/drones.py")
    screen = (java / "ControlScreenBlock.java").read_text(encoding="utf-8")
    for name, value in (("WIDTH", drones.SCREEN_WIDTH), ("HEIGHT", drones.SCREEN_HEIGHT), ("THICKNESS", drones.SCREEN_THICKNESS)):
        if f"{name} = {value};" not in screen:
            err(f"ControlScreenBlock.{name} differs from tools/drones.py")
    for result, (pattern, key, count) in drones.DRONE_CRAFTING.items():
        used = set("".join(pattern)) - {" "}
        if used != set(key):
            err(f"drone recipe {result}: pattern letters {sorted(used)} != key {sorted(key)}")


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


def check_agriculture():
    """Agriculture: Java matches tools/agriculture.py, every crop state has a model, no recipe loop."""
    java = {path.stem: path.read_text(encoding="utf-8") for path in AGRICULTURE_JAVA.glob("*.java")}

    tall = {name.lower(): {"block": block, "seed": seed, "heights": [int(h) for h in heights.split(",")], "produce": produce,
                           "pick": [int(low), int(high)], "reset": int(reset), "growth": float(growth), "trellis": trellis == "true"}
            for name, block, seed, heights, produce, low, high, reset, growth, trellis in re.findall(
                r'(\w+)\("([a-z_]+)", "([a-z_]+)", new int\[\] \{([\d, ]+)\}, "([a-z_]+)", (\d+), (\d+), (\d+), ([\d.]+)F, (true|false)\)',
                java.get("TallCrop", ""))}
    expected = {name: {"block": info["block"], "seed": info["seed"], "heights": info["heights"], "produce": info["pick"]["item"],
                       "pick": [info["pick"]["min"], info["pick"]["max"]], "reset": info["pick_reset"],
                       "growth": info["growth_time"], "trellis": bool(info.get("trellis"))} for name, info in ag.TALL_CROPS.items()}
    if tall != expected:
        err(f"TallCrop.java {tall} != tools/agriculture.py {expected}")
    for name, info in ag.TALL_CROPS.items():
        if len(info["heights"]) != 8 or len(info["textures"]) != 8:
            err(f"{name}: needs 8 ages of heights and textures")
        elif any(len(textures) != info["heights"][age] for age, textures in enumerate(info["textures"])):
            err(f"{name}: each age needs one texture per block of height")
        elif info["heights"][info["pick_reset"]] != info["heights"][7] or max(info["heights"]) > ag.TALL_SECTIONS:
            err(f"{name}: picking must keep the plant's height, and it may be at most {ag.TALL_SECTIONS} tall")

    main = java.get("JugcraftAgriculture", "")
    crops = {block: {"seed": seed, "legume": legume == "true"} for block, seed, legume in
             re.findall(r'\bcrop\("([a-z_]+)", "([a-z_]+)", (true|false)\)', main)}
    expected = {info["block"]: {"seed": info["seed"], "legume": info["legume"]} for info in ag.CROPS.values()}
    if crops != expected:
        err(f"JugcraftAgriculture.java crops {crops} != tools/agriculture.py {expected}")

    items = {}
    for name, n, sat, compost in re.findall(r'\bfood\("([a-z_]+)", (\d+), ([\d.]+)F, COMPOST_(\w+)\)', main):
        items[name] = ("food", int(n), float(sat), compost.lower(), None)
    for name, crop, compost in re.findall(r'\bseeds\("([a-z_]+)", "([a-z_]+)", COMPOST_(\w+)\)', main):
        items[name] = ("seeds", None, None, compost.lower(), crop)
    for name, crop, compost in re.findall(r'\btrellisSeeds\("([a-z_]+)", TallCrop\.(\w+), COMPOST_(\w+)\)', main):
        items[name] = ("seeds", None, None, compost.lower(), ag.TALL_CROPS.get(crop.lower(), {}).get("block"))
    for name, n, sat in re.findall(r'\bmeal\("([a-z_]+)", (\d+), ([\d.]+)F\)', main):
        items[name] = ("food", int(n), float(sat), None, None)
    for name, crop, n, sat, compost in re.findall(r'\bedibleSeeds\("([a-z_]+)", "([a-z_]+)", (\d+), ([\d.]+)F, COMPOST_(\w+)\)', main):
        items[name] = ("seeds", int(n), float(sat), compost.lower(), crop)
    for name, compost in re.findall(r'\bplain\("([a-z_]+)", COMPOST_(\w+)\)', main):
        items[name] = ("plain", None, None, compost.lower(), None)
    for name, n, sat in re.findall(r'\bstew\("([a-z_]+)", (\d+), ([\d.]+)F\)', main):
        items[name] = ("stew", int(n), float(sat), None, None)
        if ag.ITEMS.get(name, {}).get("stew_effect"):
            err(f"JugcraftAgriculture.java stew {name} gives no effect, but tools/agriculture.py gives it {ag.ITEMS[name]['stew_effect']}")
    for name, n, sat, effect, seconds in re.findall(r'\bstew\("([a-z_]+)", (\d+), ([\d.]+)F, MobEffects\.(\w+), (\d+)\)', main):
        items[name] = ("stew", int(n), float(sat), None, None)
        if ag.ITEMS.get(name, {}).get("stew_effect") != [effect, int(seconds)]:
            err(f"JugcraftAgriculture.java stew {name} gives {effect} for {seconds} s, not as tools/agriculture.py says")
    for name, n, sat in re.findall(r'\btreat\("([a-z_]+)", (\d+), ([\d.]+)F\)', main):
        items[name] = ("treat", int(n), float(sat), None, None)
    for name, n, sat, effect, seconds in re.findall(r'\bsweet\("([a-z_]+)", (\d+), ([\d.]+)F, MobEffects\.(\w+), (\d+)\)', main):
        items[name] = ("sweet", int(n), float(sat), None, None)
        if ag.ITEMS.get(name, {}).get("sweet") != [effect, int(seconds)]:
            err(f"JugcraftAgriculture.java sweet {name} gives {effect} for {seconds} s, not as tools/agriculture.py says")
    for name, n, sat, effect, seconds, back in re.findall(
            r'\bdrink\("([a-z_]+)", (\d+), ([\d.]+)F, MobEffects\.(\w+), (\d+)(, true)?\)', main):
        items[name] = ("drink", int(n), float(sat), None, None)
        if ag.ITEMS.get(name, {}).get("drink") != [effect, int(seconds)]:
            err(f"JugcraftAgriculture.java drink {name} gives {effect} for {seconds} s, not as tools/agriculture.py says")
        if bool(back) != bool(ag.ITEMS.get(name, {}).get("bottle_back")):
            err(f"JugcraftAgriculture.java drink {name}: whether crafting gives its bottle back differs from tools/agriculture.py")
        # The Cooking Pot hands remainders back, so a drink that gives its bottle back must not cook into another drink.
        if back and any(f"jugcraft:{name}" in recipe["inputs"] and "drink" in ag.ITEMS.get(result, {})
                        for result, recipe in ag.POT_RECIPES.items()):
            err(f"{name} gives its bottle back, but a Cooking Pot recipe cooks it into another bottled drink (a bottle from nothing)")
    expected = {}
    for name, info in ag.ITEMS.items():
        food = info.get("food") or [None, None]
        kind = ("stew" if info.get("stew") else "treat" if info.get("treat") else "sweet" if info.get("sweet") else "drink" if info.get("drink")
                else "seeds" if "plants" in info
                else "food" if "food" in info else "plain")
        expected[name] = (kind, food[0], food[1], info.get("compost"), info.get("plants"))
    if items != expected:
        err(f"JugcraftAgriculture.java items {items} != tools/agriculture.py {expected}")

    sickles = {name: (int(radius), int(durability)) for name, radius, durability in
               re.findall(r'\bsickle\("([a-z_]+)", (\d+), (\d+)\)', main)}
    if sickles != {name: (info["radius"], info["durability"]) for name, info in ag.SICKLES.items()}:
        err(f"JugcraftAgriculture.java sickles {sickles} differ from tools/agriculture.py")
    if re.findall(r'\bwild\("([a-z_]+)"\)', main) != list(ag.WILD_CROPS):
        err("JugcraftAgriculture.java wild plants differ from tools/agriculture.py")
    patches = {name: re.findall(r"ConventionalBiomeTags\.(\w+)", biomes)
               for name, biomes in re.findall(r'wildPatch\("([a-z_]+)", ([^;]*)\);', main)}
    expected = {name: info["biomes"] for name, info in ag.WILD_CROPS.items()}
    expected.update({name: info["biomes"] for name, info in ag.FOUND_WILD.items()})
    expected["chestnut_tree"] = ag.CHESTNUT_TREES["biomes"]
    expected["apple_tree"] = ag.CIDER["tree"]["biomes"]
    expected["mums"] = ag.MUM_PATCH["biomes"]
    expected[ag.WOLFSBANE["block"]] = ag.WOLFSBANE["biomes"]
    expected.update({name: info["biomes"] for name, info in ag.FORAGING["mushrooms"].items()})
    if patches != expected:
        err(f"JugcraftAgriculture.java wild patch biomes {patches} differ from tools/agriculture.py")
    seeds = re.search(r'GRASS_SEEDS = List\.of\(([^)]*)\)', main)
    chance = re.search(r'GRASS_SEED_CHANCE = ([\d.]+)F', main)
    if not seeds or re.findall(r'"([a-z_]+)"', seeds.group(1)) != ag.GRASS_SEEDS or not chance or float(chance.group(1)) != ag.GRASS_SEED_CHANCE:
        err("JugcraftAgriculture.java grass seeds differ from tools/agriculture.py")
    # The grass seed game test counts every one of them, or its expected total is wrong.
    test = (ROOT / "src" / "gametest" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "test" / "AgricultureGameTests.java")
    listed = re.search(r'String\[\] seeds = \{([^}]*)\}', test.read_text(encoding="utf-8")) if test.is_file() else None
    if not listed or re.findall(r'"([a-z_]+)"', listed.group(1)) != ag.GRASS_SEEDS:
        err("AgricultureGameTests.grassDropsJugcraftSeeds must count every seed in tools/agriculture.py GRASS_SEEDS")
    for name, info in ag.ITEMS.items():
        plants = info.get("plants")
        if plants and bool(info.get("trellis_seed")) != (plants in ag.trellis_crops()):
            err(f"{name}: a seed is a trellis seed exactly when it plants a climbing crop")
        if plants and bool(info.get("bog_seed")) != (plants == ag.CRANBERRY["block"]):
            err(f"{name}: a seed is a bog seed exactly when it plants the cranberry bush")
    check_festival(java, main)
    check_carving(java, main)
    check_halloween(java, main)
    check_regatta(java, main)
    check_festivities(java, main)
    check_night(java, main)
    check_decor(java)
    check_decor2(java)
    check_decor3(java)
    check_decor4(java)
    check_decor5(java)
    check_decor6(java)
    check_decor7(java)
    check_decor8(java)
    check_decor9(java)
    check_decor10(java)
    check_decor11(java)
    check_decor12(java)
    check_decor13(java)
    check_decor14(java)
    check_chandlery(java)
    check_cider(java)
    check_pantry(java, main)
    check_crows(java, main)
    check_fireworks(java, main)
    check_lanterns(java, main)
    check_feast(java, main)
    check_maze(java, main)
    check_ghosts(java, main)
    check_face_paint(java, main)
    check_candy(java, main)
    check_foraging(java, main)
    check_bats(java, main)
    check_hay_golem(java, main)
    check_knitting(java, main)
    check_pies(java, main)
    check_spirit_board(java, main)
    check_turkeys(java, main)
    check_theremin(java, main)
    check_ofrenda(java, main)
    check_graveyard(java, main)
    pot = java.get("CookingPotBlockEntity", "")
    inputs = re.search(r'int INPUTS = (\d+);', pot)
    outputs = re.search(r'int OUTPUTS = (\d+);', pot)
    cooling = re.search(r'int COOLING = (\d+);', pot)
    if (not inputs or int(inputs.group(1)) != ag.POT_INPUTS or not outputs or int(outputs.group(1)) != ag.POT_OUTPUTS
            or not cooling or int(cooling.group(1)) != ag.POT_COOLING
            or f'Jugcraft.id("{ag.HEAT_TAG.split(":")[1]}")' not in pot):
        err("CookingPotBlockEntity.java differs from POT_INPUTS, POT_OUTPUTS, POT_COOLING or HEAT_TAG in tools/agriculture.py")
    growth = java.get("CropGrowth", "")
    bonus = re.search(r'LEGUME_BONUS = ([\d.]+)F', growth)
    if not bonus or float(bonus.group(1)) != ag.LEGUME_BONUS or f'Jugcraft.id("{ag.LEGUME_TAG.split(":")[1]}")' not in growth:
        err("CropGrowth.java legume rules differ from tools/agriculture.py")

    # Every crop state has a model, and every seed plants a real crop.
    for info in ag.TALL_CROPS.values():
        variants = set((load(ASSETS / "blockstates" / f"{info['block']}.json") or {}).get("variants", {}))
        if variants != {f"age={a},section={s}" for a in range(8) for s in range(ag.TALL_SECTIONS)}:
            err(f"{info['block']}: blockstate does not cover every age and section")
    for info in ag.CROPS.values():
        variants = set((load(ASSETS / "blockstates" / f"{info['block']}.json") or {}).get("variants", {}))
        if variants != {f"age={a}" for a in range(8)}:
            err(f"{info['block']}: blockstate does not cover every age")
    for name, info in ag.ITEMS.items():
        if "plants" in info and info["plants"] not in ag.planted_blocks():
            err(f"{name} plants unknown crop {info['plants']}")
    for texture in ag.textures():
        if not (ASSETS / "textures" / "block" / f"{texture}.png").is_file():
            err(f"Missing crop texture {texture}")

    # Cooking Pot recipes: one file each, known items, room in the pot, and no two with the same ingredients.
    registered = set(ag.all_items()) | set(all_items()) | set(all_blocks())
    folder = DATA / MOD / "recipe" / "pot_cooking"
    if sorted(path.stem for path in folder.glob("*.json")) != sorted(ag.POT_RECIPES):
        err("recipe/pot_cooking/ files differ from POT_RECIPES in tools/agriculture.py")
    seen = {}
    for result, info in ag.POT_RECIPES.items():
        refs = list(info["inputs"])
        for ref in refs + [f"{MOD}:{result}"]:
            if split(ref)[0] == MOD and split(ref)[1] not in registered:
                err(f"pot_cooking/{result}: unknown item {ref}")
        if len(refs) > ag.POT_INPUTS:
            err(f"pot_cooking/{result}: {len(refs)} ingredients do not fit in {ag.POT_INPUTS} slots")
        key = frozenset(refs)
        if key in seen:
            err(f"pot_cooking/{result} uses the same ingredients as {seen[key]}; the pot could not tell them apart")
        seen[key] = result
        recipe = load(folder / f"{result}.json") or {}
        if recipe.get("type") != f"{MOD}:pot_cooking" or not recipe.get("fabric:load_conditions"):
            err(f"pot_cooking/{result}: wrong type or missing feature switch condition")

    # No recipe loop among agriculture items: every conversion leads away from where it started.
    edges = {}

    def edge(ref, result):
        edges.setdefault(split(ref.lstrip("#"))[1], set()).add(split(result)[1] if ":" in result else result)
    for recipe in ag.SHAPELESS:
        for ref in recipe["inputs"]:
            edge(ref, recipe["result"])
    for recipe in ag.SHAPED:
        for ref in recipe["key"].values():
            edge(ref, recipe["result"])
    for result, info in ag.POT_RECIPES.items():
        for ref in info["inputs"]:
            edge(ref, result)
    for result, info in ag.COOKING.items():
        edges.setdefault(info["input"], set()).add(result)

    def reaches(start, target, seen):
        for nxt in edges.get(start, ()):
            if nxt == target or (nxt not in seen and reaches(nxt, target, seen | {nxt})):
                return True
        return False
    for start in edges:
        if reaches(start, start, {start}):
            err(f"Agriculture recipes form a loop through {start}")


def check_festival(java, main):
    """Festival crops: Java matches tools/agriculture.py, and every stage and fruit state has a model."""
    gourds = {name: (seed, float(growth)) for name, seed, growth in
              re.findall(r'\bgourd\("([a-z_]+)", "([a-z_]+)", ([\d.]+)F, MapColor\.\w+\)', main)}
    if gourds != {name: (info["seed"], info["growth_time"]) for name, info in ag.GOURDS.items()}:
        err(f"JugcraftAgriculture.java gourds {gourds} differ from GOURDS in tools/agriculture.py")
    for gourd, info in ag.GOURDS.items():
        if ag.ITEMS.get(info["seed"], {}).get("plants") != ag.stem(gourd):
            err(f"{info['seed']} must plant {ag.stem(gourd)}")
    bush = java.get("CranberryBushBlock", "")
    numbers = {name: int(value) for name, value in re.findall(r'int (GROWTH_CHANCE|PICK_MIN|PICK_MAX|PICK_RESET) = (\d+);', bush)}
    cranberry = ag.CRANBERRY
    if (numbers != {"GROWTH_CHANCE": cranberry["growth_chance"], "PICK_MIN": cranberry["pick"]["min"],
                    "PICK_MAX": cranberry["pick"]["max"], "PICK_RESET": cranberry["pick_reset"]}
            or f'Jugcraft.id("{ag.BOG_SOIL_TAG.split(":")[1]}")' not in bush):
        err("CranberryBushBlock.java differs from CRANBERRY or BOG_SOIL_TAG in tools/agriculture.py")
    leaves = java.get("ChestnutLeavesBlock", "")
    numbers = {name: int(value) for name, value in re.findall(r'int (FRUIT_CHANCE|PICK_MIN|PICK_MAX) = (\d+);', leaves)}
    chestnut = ag.CHESTNUT
    if numbers != {"FRUIT_CHANCE": chestnut["fruit_chance"], "PICK_MIN": chestnut["pick"]["min"], "PICK_MAX": chestnut["pick"]["max"]}:
        err("ChestnutLeavesBlock.java differs from CHESTNUT in tools/agriculture.py")
    tree_blocks = {block for tree in ag.TREES for block in (ag.sapling(tree), ag.TREES[tree]["leaves"])}
    for block in [b for b in ag.TREE_BLOCKS if b not in tree_blocks] + list(ag.DECOR) + [ag.CRANBERRY["block"]]:
        if f'registerBlock("{block}"' not in main:
            err(f"JugcraftAgriculture.java does not register {block}")
    for tree, info in ag.TREES.items():  # registerTree registers the sapling, the leaves and the wood set
        base = info["base"].upper()
        if not re.search(rf'registerTree\("{tree}", "{info["leaves"]}", {tree.upper()}_GROWER, '
                         rf'{tree.upper() + "_LEAVES" if info["season"] else "null"}, Blocks\.{base}_SAPLING, Blocks\.{base}_LEAVES,', main):
            err(f"JugcraftAgriculture.java does not register the {tree} tree as TREES in tools/agriculture.py says")
    for wood in ag.WOOD_SETS:  # registerWoodSet registers every block in agriculture.wood_blocks
        if f'registerWoodSet("{wood}",' not in main and wood not in ag.TREES:
            err(f"JugcraftAgriculture.java does not register the {wood} wood set")
    check_seasonal_trees(main, java.get("SeasonalLeavesBlock", ""))
    for block, info in ag.DECOR.items():
        if f"lightLevel(state -> {info['light']})" not in main:
            err(f"{block}: light level differs from DECOR in tools/agriculture.py")
    expected_states = {ag.stem(g): {f"age={a}" for a in range(8)} for g in ag.GOURDS}
    expected_states[ag.CRANBERRY["block"]] = {f"age={a}" for a in range(len(ag.CRANBERRY["stages"]))}
    expected_states[ag.CHESTNUT["leaves"]] = {f"fruit={f}" for f in range(3)}
    for tree, info in ag.TREES.items():
        expected_states[info["leaves"]] = {f"season={state}" for state in ag.SEASON_STATES} if info["season"] else {""}
    for block, variants in expected_states.items():
        if set((load(ASSETS / "blockstates" / f"{block}.json") or {}).get("variants", {})) != variants:
            err(f"{block}: blockstate does not cover every stage")


def check_seasonal_trees(main, leaves):
    """Each seasonal tree's leaf schedule in Java matches TREES, SeasonalLeavesBlock matches JITTER, SPREAD and the
    states, and every fixed season mode shows its own look on every block, whatever its jitter."""
    numbers = {name: int(value) for name, value in re.findall(r"int (JITTER|SPREAD) = (\d+);", leaves)}
    if numbers != {"JITTER": ag.JITTER, "SPREAD": ag.SPREAD}:
        err(f"SeasonalLeavesBlock.java {numbers} differs from JITTER and SPREAD in tools/agriculture.py")
    states = re.findall(r"^\t\t([A-Z, ]+);", leaves.partition("enum Foliage")[2], re.M)
    if not states or [state.strip().lower() for state in states[0].split(",")] != ag.SEASON_STATES:
        err(f"SeasonalLeavesBlock.Foliage differs from SEASON_STATES {ag.SEASON_STATES}")
    schedules = {name.lower(): [int(a), int(b), int(c)] for name, a, b, c in
                 re.findall(r"(\w+)_LEAVES = new SeasonalLeavesBlock\.Schedule\((\d+), (\d+), (\d+)\);", main)}
    expected = {tree: info["season"] for tree, info in ag.TREES.items() if info["season"]}
    if schedules != expected:
        err(f"JugcraftAgriculture.java leaf schedules {schedules} differ from TREES in tools/agriculture.py {expected}")
    calendar = (ROOT / "src/main/java/io/github/jimbozoomer/jugcraft/season/SeasonCalendar.java").read_text(encoding="utf-8")
    modes = {name.lower(): int(day) for name, day in re.findall(r"\b(SPRING|SUMMER|AUTUMN|WINTER)\((\d+)\)", calendar)}
    if len(modes) != 4:
        err(f"SeasonCalendar.Mode days not found ({modes})")
    wanted = {"spring": "green", "summer": "green", "autumn": "gold", "winter": "bare"}
    for tree, (green_from, gold_from, bare_from) in expected.items():
        def look(day):
            if bare_from <= day or day < green_from:
                return "bare"
            return "gold" if day >= gold_from else "green"
        for mode, day in modes.items():
            seen = {look((day - 1 + shift) % 365 + 1) for shift in range(-ag.JITTER, ag.JITTER + 1)}
            if seen != {wanted[mode]}:
                err(f"/jugcraft season set {mode} (day {day}) shows {tree} leaves {sorted(seen)}, not only {wanted[mode]}")


def check_carving(java, main):
    """Pumpkin carving: Java matches CARVING in tools/agriculture.py."""
    info = ag.CARVING
    carving = java.get("PumpkinCarving", "")
    numbers = {name: re.search(rf"int {name} = (\d+);", carving) for name in ("SIZE", "FACES")}
    if any(match is None for match in numbers.values()) or int(numbers["SIZE"].group(1)) != info["size"] \
            or int(numbers["FACES"].group(1)) != info["faces"]:
        err("PumpkinCarving.java SIZE or FACES differ from CARVING in tools/agriculture.py")
    glow = info["glow"]
    expected = f"Math.min({glow['max']}, {glow['base']} + count(CUT) / {glow['per_holes']} + count(SHAVED) / {glow['per_shaved']})"
    if expected not in carving:
        err(f"PumpkinCarving.glow() differs from CARVING['glow'] (expected {expected})")
    session = re.search(r"SESSION_TICKS = (\d+);", java.get("PumpkinCarvings", ""))
    if not session or int(session.group(1)) != info["session_ticks"]:
        err("PumpkinCarvings.SESSION_TICKS differs from CARVING['session_ticks']")
    durability = re.search(r"CARVING_KNIFE_DURABILITY = (\d+);", main)
    if not durability or int(durability.group(1)) != info["durability"]:
        err("JugcraftAgriculture.CARVING_KNIFE_DURABILITY differs from CARVING['durability']")
    for registered in (info["block"], info["knife"]):
        if f'"{registered}"' not in main:
            err(f"JugcraftAgriculture.java does not register {registered}")
    templates = re.findall(r'template\("([a-z_]+)",', java.get("CarvingTemplates", ""))
    if templates != info["templates"]:
        err(f"CarvingTemplates.java {templates} differ from CARVING['templates'] {info['templates']}")
    rows = re.findall(r'"([.s#]+)"[,)]', java.get("CarvingTemplates", ""))
    if len(rows) != info["size"] * len(info["templates"]) or any(len(row) != info["size"] for row in rows):
        err(f"CarvingTemplates.java: every starter face is {info['size']} rows of {info['size']} pixels")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    for template in info["templates"]:
        if f"carving.{MOD}.template.{template}" not in lang:
            err(f"Missing name for carving template {template}")
    results = java.get("PumpkinCarvings", "").partition("enum Result {")[2].partition("}")[0]
    if not results:
        err("PumpkinCarvings.java has no Result enum")
    for result in re.findall(r"\b([A-Z_]+)\b", results):
        if result not in ("CARVED", "UNCHANGED") and f"message.{MOD}.carving.{result.lower()}" not in lang:
            err(f"Missing carving message for {result}")


CRAFTING_CATEGORIES = {"building", "redstone", "equipment", "misc"}
COOKING_CATEGORIES = {"food", "blocks", "misc"}


def check_recipe_categories():
    """Minecraft 26.3 refuses a whole datapack over one recipe with a category its type doesn't have."""
    for path in sorted((DATA / MOD / "recipe").rglob("*.json")):
        recipe = load(path) or {}
        kind, category = recipe.get("type", ""), recipe.get("category")
        if category is None:
            continue
        allowed = CRAFTING_CATEGORIES if kind.startswith("minecraft:crafting") else COOKING_CATEGORIES \
            if kind in ("minecraft:smelting", "minecraft:smoking", "minecraft:blasting", "minecraft:campfire_cooking") else None
        if allowed is not None and category not in allowed:
            err(f"{path.relative_to(ROOT)}: category {category} is not one of {sorted(allowed)} for {kind}")


def check_halloween(java, main):
    """Halloween harvest: Java matches tools/agriculture.py, and every giant pumpkin and scarecrow state has a model."""
    def number(source, name, kind=r"\d+"):
        match = re.search(rf"\b{name} = ({kind})[FL]?;", java.get(source, ""))
        return match.group(1) if match else None

    giant = ag.GIANT_PUMPKIN
    entity = {name: number("GiantPumpkinBlockEntity", name) for name in
              ("GROW_TO_TWO", "GROW_TO_THREE", "BONE_MEAL_POINTS", "START_WEIGHT", "WEIGHT_PER_POINT", "MAX_WEIGHT", "WATERED_TICKS")}
    expected = {"GROW_TO_TWO": giant["grow_to_two"], "GROW_TO_THREE": giant["grow_to_three"], "BONE_MEAL_POINTS": giant["bone_meal_points"],
                "START_WEIGHT": giant["start_weight"], "WEIGHT_PER_POINT": giant["weight_per_point"], "MAX_WEIGHT": giant["max_weight"],
                "WATERED_TICKS": giant["watered_ticks"]}
    if {k: int(v) if v else None for k, v in entity.items()} != expected:
        err(f"GiantPumpkinBlockEntity.java {entity} differs from GIANT_PUMPKIN in tools/agriculture.py")
    if number("GiantPumpkinBlock", "MAX_SIZE") != str(giant["max_size"]) or giant["face_size"] != 16 * giant["max_size"]:
        err("GiantPumpkinBlock.MAX_SIZE differs from GIANT_PUMPKIN['max_size'], or face_size is not 16 per block")
    if number("GiantPumpkinVineBlock", "GROWTH_TIME", r"[\d.]+") is None or \
            float(number("GiantPumpkinVineBlock", "GROWTH_TIME", r"[\d.]+")) != giant["vine_growth_time"]:
        err("GiantPumpkinVineBlock.GROWTH_TIME differs from GIANT_PUMPKIN['vine_growth_time']")
    reach = number("PumpkinCarvings", "GIANT_REACH", r"[\d.]+")
    if reach is None or float(reach) != giant["reach"] or f'SCOOP_TABLE = "{ag.SCOOP["table"]}"' not in java.get("PumpkinCarvings", ""):
        err("PumpkinCarvings.GIANT_REACH or SCOOP_TABLE differs from tools/agriculture.py")
    glow = giant["glow"]
    expected_glow = f"Math.min({glow['max']}, {glow['base']} + cut / {glow['per_holes']} + shaved / {glow['per_shaved']})"
    if expected_glow not in java.get("GiantPumpkinBlockEntity", ""):
        err(f"GiantPumpkinBlockEntity.glow() differs from GIANT_PUMPKIN['glow'] (expected {expected_glow})")
    if sorted(giant["drops"]) != list(range(1, giant["max_size"] + 1)):
        err("GIANT_PUMPKIN['drops'] needs one entry per size")

    scale = ag.HARVEST_SCALE
    ribbons = re.search(r"RIBBONS = List\.of\(([^)]*)\)", java.get("HarvestScaleBlockEntity", ""))
    if (number("HarvestScaleBlockEntity", "BOARD") != str(scale["board"]) or number("HarvestScaleBlockEntity", "REMEMBERED") != str(scale["remembered"])
            or not ribbons or re.findall(r'"([a-z_]+)"', ribbons.group(1)) != list(scale["ribbons"]) or len(scale["ribbons"]) != scale["board"]):
        err("HarvestScaleBlockEntity.java differs from HARVEST_SCALE in tools/agriculture.py (one ribbon per place on the board)")
    if number("GourdCanteenItem", "CAPACITY") != str(ag.CANTEEN["capacity"]):
        err("GourdCanteenItem.CAPACITY differs from CANTEEN['capacity']")
    varieties = re.search(r'for \(String variety : List\.of\(([^)]*)\)\)', main)
    if (not varieties or re.findall(r'"([a-z_]+)"', varieties.group(1)) != list(ag.CARVED_VARIETIES)
            or any(carved != f"hand_carved_{variety}" for variety, carved in ag.CARVED_VARIETIES.items())
            or any(not ag.GOURDS.get(variety, {}).get("cube") for variety in ag.CARVED_VARIETIES)):
        err("JugcraftAgriculture.java carvable heirloom pumpkins differ from CARVED_VARIETIES (each a whole-block gourd)")
    mums = {name: (effect, float(seconds)) for name, effect, seconds in
            re.findall(r'\bmum\("([a-z_]+)", MobEffects\.(\w+), ([\d.]+)F\)', main)}
    if mums != {name: (info["effect"], info["seconds"]) for name, info in ag.MUMS.items()}:
        err(f"JugcraftAgriculture.java mums {mums} differ from MUMS in tools/agriculture.py")
    if f"DEFAULT_SHIRT = DyeColor.{ag.SCARECROW_SHIRT.upper()};" not in java.get("ScarecrowBlock", ""):
        err("ScarecrowBlock.DEFAULT_SHIRT differs from SCARECROW_SHIRT")
    for block in [giant["block"], scale["block"]] + list(ag.HALLOWEEN_DECOR):
        if f'"{block}"' not in main:
            err(f"JugcraftAgriculture.java does not register {block}")
    for crop in ag.STALKS["crops"]:
        if crop not in ag.TALL_CROPS:
            err(f"STALKS: {crop} is not a tall crop")
    for wild in ag.WILD_BONUS:
        if wild not in ag.WILD_CROPS:
            err(f"WILD_BONUS: {wild} is not a wild plant")

    # Minecraft 26.3 refuses a model element with no faces (the hidden middle of a giant pumpkin must have no element).
    for path in sorted((ASSETS / "models").rglob("*.json")):
        for element in (load(path) or {}).get("elements", []):
            if not element.get("faces"):
                err(f"{path.relative_to(ROOT)}: a model element needs at least one face")

    max_size = giant["max_size"]
    states = set((load(ASSETS / "blockstates" / f"{giant['block']}.json") or {}).get("variants", {}))
    if states != {f"part={p},size={size}" for size in range(1, max_size + 1) for p in range(max_size ** 3)}:
        err("giant_pumpkin: blockstate does not cover every size and part")
    states = set((load(ASSETS / "blockstates" / "scarecrow.json") or {}).get("variants", {}))
    expected = {f"facing={f},half=lower" for f in ("north", "east", "south", "west")}
    expected |= {f"facing={f},half=upper,shirt={c}" for f in ("north", "east", "south", "west") for c in ag.DYE_COLORS}
    if states != expected:
        err("scarecrow: blockstate does not cover every facing, half and shirt colour")


def check_regatta(java, main):
    """The pumpkin regatta and trick-or-treating: Java matches tools/agriculture.py, and every buoy number has a model."""
    def number(source, name, kind=r"[\d.]+"):
        match = re.search(rf"\b{name} = ({kind})[FLD]?;", java.get(source, ""))
        return float(match.group(1)) if match else None

    regatta, trick = ag.REGATTA, ag.TRICK_OR_TREAT
    expected = {("PumpkinBoat", "RACER_BASE_WEIGHT"): ag.RACER_BASE_WEIGHT, ("PumpkinBoat", "WATER_FRICTION"): ag.WATER_FRICTION,
                ("PumpkinBoat", "MARK_RADIUS"): regatta["mark_radius"], ("PumpkinBoat", "FINISH_RADIUS"): regatta["finish_radius"],
                ("PumpkinBoat", "COUNTDOWN_TICKS"): regatta["countdown_ticks"], ("PumpkinBoat", "MAX_RACE_TICKS"): regatta["max_race_ticks"],
                ("PumpkinBoat", "MAX_SPEED"): regatta["max_speed"], ("RegattaBuoyBlock", "MAX_NUMBER"): regatta["max_number"],
                ("RegattaFlagBlockEntity", "REMEMBERED"): regatta["remembered"], ("RegattaFlagBlockEntity", "COURSE_RANGE"): regatta["course_range"],
                ("RegattaFlagBlockEntity", "COURSE_HEIGHT"): regatta["course_height"], ("TrickOrTreat", "DUSK"): trick["dusk"],
                ("TrickOrTreat", "MIDNIGHT"): trick["midnight"], ("TrickOrTreat", "ANSWER_TICKS"): trick["answer_ticks"],
                ("TrickOrTreat", "KNOCK_COOLDOWN"): trick["knock_cooldown"], ("TrickOrTreat", "PORCH_RADIUS"): trick["porch_radius"],
                ("TrickOrTreat", "HOME_RADIUS"): trick["home_radius"], ("TrickOrTreat", "FULL_BAG"): trick["full_bag"],
                ("TrickOrTreat", "COSTUME_BONUS_CHANCE"): trick["costume_bonus"]}
    for (source, name), value in expected.items():
        if number(source, name) != value:
            err(f"{source}.{name} = {number(source, name)} differs from tools/agriculture.py ({value})")
    if regatta["board"] != ag.HARVEST_SCALE["board"] or "BOARD = HarvestScaleBlockEntity.BOARD;" not in java.get("RegattaFlagBlockEntity", ""):
        err("The regatta board must hand out the Harvest Scale's ribbons, one per place")
    for source, name, value in (("TrickOrTreat", "TREAT_TABLE", trick["table"]), ("PumpkinCarvings", "HOLLOW_TABLE", ag.HOLLOW["table"])):
        if f'{name} = "{value}"' not in java.get(source, ""):
            err(f"{source}.{name} differs from tools/agriculture.py ({value})")
    tags = {"COSTUMES": ag.COSTUME_TAG, "COSTUME_HATS": ag.COSTUME_HAT_TAG, "PORCH_LIGHTS": ag.PORCH_LIGHT_TAG}
    for field, tag in tags.items():
        if f'{field} = TagKey.create(Registries.{"BLOCK" if field == "PORCH_LIGHTS" else "ITEM"}, Jugcraft.id("{split(tag)[1]}"))' \
                not in java.get("TrickOrTreat", ""):
            err(f"TrickOrTreat.{field} is not the tag {tag}")

    # Each boat kind: Java's Kind constant and entity size match PUMPKIN_BOATS.
    kinds = {name: values for name, values in re.findall(r'^\t\t(BARGE|RACER)\("[a-z_]+", ([^;]*?)\)[,;]', java.get("PumpkinBoat", ""), re.M | re.S)}
    for boat, info in ag.PUMPKIN_BOATS.items():
        values = [v.strip().removesuffix("F") for v in re.sub(r"\s+", " ", kinds.get(info["kind"], "")).split(",")]
        width, height, face_top = info["shell"]
        want = [info["seats"], width, height, face_top, info["floor"], info["seat"], info["fastest"], info["slowest"]]
        try:
            got = [float(v) for v in values[:6] + values[-2:]]
        except ValueError:
            got = []
        if got != want:
            err(f"PumpkinBoat.Kind.{info['kind']} {values} differs from PUMPKIN_BOATS['{boat}'] in tools/agriculture.py")
        if f'"{boat}"' not in java.get("PumpkinBoat", "") or \
                f"boat(PumpkinBoat.Kind.{info['kind']}, {info['hitbox'][0]}F, {info['hitbox'][1]}F)" not in main:
            err(f"JugcraftAgriculture.java registers {boat} with a different id or hitbox than {info['hitbox']}")
    if 'List.of("' + '", "'.join(ag.COSTUMES) + '")' not in main:
        err("JugcraftAgriculture.COSTUMES differs from COSTUMES in tools/agriculture.py")

    # The Halloween window's defaults, and the treats: candy and sweets only, weights given.
    config = (JAVA_ROOT / "config" / "JugcraftConfig.java").read_text(encoding="utf-8")
    for key, value in trick["window"].items():
        if f'"{key}", "{value}"' not in config:
            err(f"JugcraftConfig.TEXT_OPTIONS {key} default differs from {value}")
    for item, weight, (low, high) in trick["treats"]:
        if weight <= 0 or not 1 <= low <= high:
            err(f"trick-or-treat: {item} needs a positive weight and count")
    states = set((load(ASSETS / "blockstates" / f"{regatta['buoy']}.json") or {}).get("variants", {}))
    if states != {f"number={n}" for n in range(1, regatta["max_number"] + 1)}:
        err("regatta_buoy: blockstate does not cover every number")


def check_festivities(java, main):
    """The Halloween festivities: Java matches tools/agriculture.py, the Peddler only sells, and the gravestones' shapes are their models."""
    def number(source, name):
        match = re.search(rf"\b{name} = ([\d.]+)[FLD]?;", java.get(source, ""))
        return float(match.group(1)) if match else None

    contest, mobs, peddler = ag.CONTEST, ag.COSTUMED_MOBS, ag.PEDDLER
    expected = {("CarvingContest", "CHECK_TICKS"): contest["check_ticks"], ("CarvingContest", "MAX_VOTERS"): contest["max_voters"],
                ("CarvingContest", "MAX_CONTESTS"): contest["max_contests"], ("CostumedMobs", "CHANCE"): mobs["chance"],
                ("GravestoneBlockEntity", "MAX_LENGTH"): ag.ENGRAVING["max_length"], ("CandleSkullBlock", "LIGHT"): ag.CANDLE_SKULL["light"]}
    for (source, name), value in expected.items():
        if number(source, name) != value:
            err(f"{source}.{name} = {number(source, name)} differs from tools/agriculture.py ({value})")
    if contest["places"] != ag.HARVEST_SCALE["board"] or "PLACES = HarvestScaleBlockEntity.BOARD;" not in java.get("CarvingContest", ""):
        err("The carving contest must hand out the Harvest Scale's ribbons, one per place")
    source = java.get("CostumedMobs", "")
    for field, values in (("MOBS", mobs["mobs"]), ("COSTUMES", mobs["costumes"])):
        found = re.search(rf"{field} = List\.of\(([^;]*)\);", source)
        if not found or re.findall(r'"([a-z_:]+)"', found.group(1)) != values:
            err(f"CostumedMobs.{field} differs from COSTUMED_MOBS in tools/agriculture.py")
    if f'COSTUMED = "{mobs["tag"]}"' not in source or f'Jugcraft.id("{mobs["table"]}")' not in source:
        err("CostumedMobs's tag or candy table differs from tools/agriculture.py")
    if f'Jugcraft.id("{peddler["trade_set"]}")' not in java.get("HalloweenPeddler", ""):
        err("HalloweenPeddler.TRADES differs from tools/agriculture.py")
    for item, weight, (low, high) in mobs["candy"]:
        if weight <= 0 or not 1 <= low <= high or (split(item)[0] == MOD and split(item)[1] not in ag.ITEMS):
            err(f"costumed mob candy: {item} needs to be a known food with a positive weight and count")
    for costume in mobs["costumes"]:
        if split(costume)[0] == MOD and costume.split(":")[1] not in {**ag.COSTUMES, **ag.OUTFITS}:
            err(f"costumed mobs: {costume} is not a costume")

    # The Peddler wants emeralds and gives Jugcraft goods that have another route; it never gives emeralds back.
    if not 1 <= peddler["amount"] <= len(peddler["trades"]):
        err("The Peddler must offer between one and all of its trades")
    test = ROOT / "src" / "gametest" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "test" / "FestivityGameTests.java"
    counted = re.search(r"set\.trades\(\)\.size\(\) == (\d+)", test.read_text(encoding="utf-8")) if test.is_file() else None
    if not counted or int(counted.group(1)) != len(peddler["trades"]):
        err(f"FestivityGameTests.festivityDataLoads must count the Peddler's {len(peddler['trades'])} trades")
    crafted = {r["result"] for r in ag.SHAPED + ag.SHAPELESS} | set(ag.POT_RECIPES)
    for trade, info in peddler["trades"].items():
        item, count = info["gives"]
        name = split(item)[1]
        if item == "minecraft:emerald" or count < 1 or info["wants"] < 1 or info["max_uses"] < 1:
            err(f"Peddler trade {trade}: it must take emeralds and give something else")
        elif name not in crafted and name not in ag.ITEMS:
            err(f"Peddler trade {trade}: {item} has no route besides the Peddler")

    # Gravestones: the Java styles' boxes and engraving places are tools/agriculture.py's, and so are the models.
    source = java.get("GravestoneBlock", "")
    for stone, info in ag.GRAVESTONES.items():
        found = re.search(rf'{info["style"]}\("{stone}", new double\[\]\[\] (\{{\{{.*?\}}\}}),\s*([^)]*)\)', source, re.S)
        boxes = [tuple(float(v) for v in b.split(",")) for b in re.findall(r"\{([^{}]*)\}", found.group(1))] if found else []
        engraving = [v.strip() for v in found.group(2).split(",")] if found else []
        want = [f"{v}F" if isinstance(v, float) else str(v) for v in info["engraving"]]
        if boxes != [tuple(float(v) for v in b) for b in info["boxes"]] or engraving != want:
            err(f"GravestoneBlock.Style.{info['style']} differs from GRAVESTONES['{stone}'] in tools/agriculture.py")
        model = load(ASSETS / "models" / "block" / f"{stone}.json") or {}
        if [tuple(e["from"] + e["to"]) for e in model.get("elements", [])] != [tuple(b) for b in info["boxes"]]:
            err(f"{stone}: model boxes differ from its shape")


def check_night(java, main):
    """Halloween nights: Java matches tools/agriculture.py, every trebuchet state has a model, and the Horseman's loot
    is tools/agriculture.py's (dropped only when a player defeats him)."""
    def number(source, name):
        match = re.search(rf"\b{name} = ([\d.]+)[FLD]?;", java.get(source, ""))
        return float(match.group(1)) if match else None

    wisps, moon, treb, horseman = ag.WISPS, ag.HARVEST_MOON, ag.TREBUCHET, ag.HORSEMAN
    expected = {("Wisps", "SPAWN_TICKS"): wisps["spawn_ticks"], ("Wisps", "SPAWN_CHANCE"): wisps["spawn_chance"],
                ("Wisps", "MIN_DISTANCE"): wisps["min"], ("Wisps", "MAX_DISTANCE"): wisps["max"],
                ("Wisps", "CORN_RADIUS"): wisps["corn_radius"], ("Wisps", "NEAR_CAP"): wisps["near_cap"],
                ("Wisps", "NEAR_RANGE"): wisps["near_range"], ("Wisps", "LEVEL_CAP"): wisps["level_cap"],
                ("WillOWisp", "FLEE_RADIUS"): wisps["flee"], ("WillOWisp", "SNEAK_FLEE_RADIUS"): wisps["sneak_flee"],
                ("JugcraftAgriculture", "WISP_JAR_LIGHT"): wisps["jar_light"],
                ("HarvestMoon", "GROWTH_BONUS"): moon["growth_bonus"], ("HarvestMoon", "DUSK"): moon["dusk"],
                ("HarvestMoon", "DAWN"): moon["dawn"], ("HarvestMoon", "CHECK_TICKS"): moon["check_ticks"],
                ("TrebuchetBlockEntity", "REMEMBERED"): treb["remembered"], ("TrebuchetBlockEntity", "BASE_SPEED"): treb["base_speed"],
                ("TrebuchetBlockEntity", "GUST"): treb["gust"], ("TrebuchetBlockEntity", "MIN_ANGLE"): treb["min_angle"],
                ("TrebuchetBlockEntity", "MAX_ANGLE"): treb["max_angle"], ("TrebuchetBlockEntity", "ANGLE_STEP"): treb["angle_step"],
                ("TrebuchetBlockEntity", "DEFAULT_ANGLE"): treb["default_angle"],
                ("TrebuchetBlockEntity", "ADVANCEMENT_DISTANCE"): treb["advancement_distance"],
                ("TrebuchetBlock", "RESET_TICKS"): treb["reset_ticks"], ("FlyingPumpkin", "MAX_FLIGHT"): treb["max_flight"],
                ("ThrowMarker", "LIFETIME"): treb["marker_ticks"],
                ("HeadlessHorseman", "MAX_HEALTH"): horseman["health"], ("HeadlessHorseman", "ARENA_RADIUS"): horseman["arena_radius"],
                ("HeadlessHorseman", "LEAVE_RANGE"): horseman["leave_range"], ("HeadlessHorseman", "LONELY_TICKS"): horseman["lonely_ticks"],
                ("HeadlessHorseman", "THROW_COOLDOWN"): horseman["throw_cooldown"],
                ("HeadlessHorseman", "ENRAGED_THROW_COOLDOWN"): horseman["enraged_throw_cooldown"],
                ("HeadlessHorseman", "THROW_RANGE"): horseman["throw_range"], ("HorsemanSummoning", "MIDNIGHT"): horseman["midnight"],
                ("HorsemanSummoning", "HOUR_WINDOW"): horseman["hour_window"], ("HorsemanSummoning", "ONE_AT_A_TIME"): horseman["one_at_a_time"],
                ("FlamingPumpkin", "DAMAGE"): horseman["pumpkin_damage"], ("FlamingPumpkin", "SPLASH_RADIUS"): horseman["splash"],
                ("FlamingPumpkin", "FIRE_SECONDS"): horseman["fire_seconds"]}
    for (source, name), value in expected.items():
        if number(source, name) != value:
            err(f"{source}.{name} = {number(source, name)} differs from tools/agriculture.py ({value})")
    if treb["board"] != ag.HARVEST_SCALE["board"] or "BOARD = HarvestScaleBlockEntity.BOARD;" not in java.get("TrebuchetBlockEntity", ""):
        err("The trebuchet's board must hand out the Harvest Scale's ribbons, one per place")
    if not treb["min_angle"] <= treb["default_angle"] <= treb["max_angle"] or (treb["default_angle"] - treb["min_angle"]) % treb["angle_step"]:
        err("The trebuchet's default angle must be one of its angles")

    # Throwing factors: Java's are the table's, each is a pumpkin that exists, and the ammunition tag lists exactly them.
    source = java.get("TrebuchetBlockEntity", "")
    factors = {item: float(factor) for item, factor in re.findall(r'Map\.entry\("([a-z_:]+)", ([\d.]+)\)', source)}
    if factors != treb["factors"]:
        err(f"TrebuchetBlockEntity.FACTORS {factors} differs from tools/agriculture.py {treb['factors']}")
    for item, factor in treb["factors"].items():
        if split(item)[0] == MOD and split(item)[1] not in ag.all_items():
            err(f"trebuchet: {item} is not a Jugcraft item")
        if not 0.8 <= factor <= 1.2:
            err(f"trebuchet: {item}'s factor {factor} is outside 0.8 to 1.2")
    ammo = load(DATA / MOD / "tags" / "item" / f"{split(treb['ammo_tag'])[1]}.json") or {}
    if sorted(ammo.get("values", [])) != sorted(treb["factors"]):
        err("The trebuchet's ammunition tag must list exactly the pumpkins that have a throwing factor")
    treats = load(DATA / MOD / "tags" / "item" / f"{split(ag.CANDY_BAG['treat_tag'])[1]}.json") or {}
    if treats.get("values") != ag.CANDY_BAG["treats"]:
        err("The candy bag's treat tag differs from tools/agriculture.py")

    # The trebuchet's blockstate covers every facing and arm position, with a model for each arm.
    states = (load(ASSETS / "blockstates" / f"{treb['block']}.json") or {}).get("variants", {})
    want = {f"arm={arm},facing={facing}" for arm in ("ready", "loaded", "released") for facing in ("north", "east", "south", "west")}
    if set(states) != want:
        err(f"{treb['block']}: blockstate variants {sorted(states)} differ from every arm position and facing")
    arms = re.search(r"enum Arm[^{]*\{\s*([^;]+);", java.get("TrebuchetBlock", ""))
    if not arms or re.findall(r'[A-Z_]+\("([a-z_]+)"\)', arms.group(1)) != ["ready", "loaded", "released"]:
        err("TrebuchetBlock.Arm differs from the arm positions the models draw")
    for arm in ("ready", "loaded", "released"):
        if load(ASSETS / "models" / "block" / f"{treb['block']}_{arm}.json") is None:
            err(f"{treb['block']}: no model for arm position {arm}")

    # The Harvest Moon's day is configured, defaulting to tools/agriculture.py's.
    config = (JAVA_ROOT / "config" / "JugcraftConfig.java").read_text(encoding="utf-8")
    if f'"halloween.harvest_moon", "{moon["day"]}"' not in config:
        err(f"JugcraftConfig's halloween.harvest_moon default differs from tools/agriculture.py ({moon['day']})")
    month, day = (int(v) for v in moon["day"].split("-"))
    if f"DEFAULT_HARVEST_MOON = MonthDay.of({month}, {day})" not in java.get("HalloweenSeason", ""):
        err("HalloweenSeason.DEFAULT_HARVEST_MOON differs from tools/agriculture.py")

    # The Horseman drops tools/agriculture.py's loot, and only to a player's kill.
    table = load(DATA / MOD / "loot_table" / f"{horseman['table']}.json") or {}
    drops = []
    for pool in table.get("pools", []):
        if pool.get("condition", {}).get("type") != "minecraft:killed_by_player":
            err(f"{horseman['table']}: every pool needs a player's kill")
        for entry in pool.get("entries", []):
            modifier = entry.get("modifier", {})
            count = modifier.get("count", 1) if modifier.get("type") == "minecraft:set_count" else 1
            low, high = (count["min"], count["max"]) if isinstance(count, dict) else (count, count)
            drops.append((entry.get("name"), [low, high]))
    if drops != [(item, list(counts)) for item, counts in horseman["loot"]]:
        err(f"{horseman['table']}: drops {drops} differ from tools/agriculture.py {horseman['loot']}")
    # Every way a summoning can fail has a message for the player.
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    results = re.search(r"enum Result \{([A-Z_,\s]+)\}", java.get("HorsemanSummoning", ""))
    for result in (re.findall(r"[A-Z_]+", results.group(1)) if results else []):
        if result != "SUMMONED" and f"message.jugcraft.horseman.{result.lower()}" not in lang:
            err(f"HorsemanSummoning.Result.{result} has no message.jugcraft.horseman.{result.lower()} text")
    if not results:
        err("HorsemanSummoning.Result not found")
    registration = re.search(r'HEADLESS_HORSEMAN = entity\("([a-z_]+)"[^;]*;', main)
    if not registration or f"entities/{registration.group(1)}" != horseman["table"] or "noLootTable" in registration.group(0):
        err(f"The Headless Horseman must be registered with his loot table {horseman['table']}")

def check_decor(java):
    """The first decorations batch: Java matches tools/agriculture.py, every block state has a model, every result has
    its message, and the portraits' eyes sit inside their frames where the textures paint them."""
    def number(source, name):
        match = re.search(rf"\b{name} = ([\d.]+)[FLD]?;", java.get(source, ""))
        return float(match.group(1)) if match else None

    def numbers(source, name):
        match = re.search(rf"\b{name} = \{{([\d, ]+)\}};", java.get(source, ""))
        return [int(v) for v in match.group(1).split(",")] if match else None

    lights, bowl, coffin, portrait, fog = ag.STRING_LIGHTS, ag.CANDY_BOWL, ag.COFFIN, ag.HAUNTED_PORTRAIT, ag.FOG_MACHINE
    expected = {("StringLightHookBlockEntity", "MAX_LENGTH"): lights["max_length"], ("StringLightHookBlockEntity", "USE"): lights["use"],
                ("StringLightHookBlockEntity", "CAPACITY"): lights["capacity"], ("StringLightHookBlockEntity", "INPUT"): lights["input"],
                ("StringLightHookBlockEntity", "CHECK_TICKS"): lights["check_ticks"], ("StringLightHookBlock", "LIGHT"): lights["light"],
                ("CandyBowlBlockEntity", "CAPACITY"): bowl["capacity"], ("CandyBowlBlockEntity", "VISITORS"): bowl["visitors"],
                ("CoffinBlockEntity", "SLOTS"): coffin["slots"], ("FogMachineBlockEntity", "USE"): fog["use"],
                ("FogMachineBlockEntity", "CAPACITY"): fog["capacity"], ("FogMachineBlockEntity", "INPUT"): fog["input"],
                ("FogMachineBlockEntity", "PARTICLES_PER_TICK"): fog["particles_per_tick"], ("FogMachineBlockEntity", "BUDGET"): fog["budget"],
                ("FogMachineBlockEntity", "VIEW"): fog["view"]}
    for (source, name), value in expected.items():
        if number(source, name) != value:
            err(f"{source}.{name} = {number(source, name)} differs from tools/agriculture.py ({value})")
    if numbers("CandyBowlBlockEntity", "FILL") != bowl["fill"] or numbers("FogMachineBlockEntity", "RADII") != fog["radii"]:
        err("CandyBowlBlockEntity.FILL or FogMachineBlockEntity.RADII differs from tools/agriculture.py")
    if fog["particles_per_tick"] > fog["budget"] or 2 + len(fog["radii"]) - 1 > fog["particles_per_tick"]:
        err("The fog machine's per-machine puffs must fit its per-tick budget")

    # The portraits: Java's eyes are the table's, inside the frame, and the textures paint a white where each eye is.
    found = {name: [tuple(int(v) for v in eye.split(",")) for eye in re.findall(r"new int\[\] \{([\d, ]+)\}", eyes)]
             for name, eyes in re.findall(r'[A-Z]+\("([a-z]+)", List\.of\(([^;]*?)\)\)', java.get("HauntedPortraitBlock", ""))}
    if found != {name: [tuple(e) for e in eyes] for name, eyes in portrait["portraits"].items()}:
        err(f"HauntedPortraitBlock.Portrait eyes {found} differ from tools/agriculture.py")
    for name, eyes in portrait["portraits"].items():
        image = ASSETS / "textures" / "block" / f"{portrait['block']}_{name}.png"
        pixels = Image.open(image).convert("RGBA") if image.exists() else None
        for x, y, w, h in eyes:
            if x < 1 or y < 1 or x + w > 15 or y + h > 15:
                err(f"{name}: an eye at {(x, y, w, h)} runs into the frame")
            elif pixels is not None and len({pixels.getpixel((x + i, y + j)) for i in range(w) for j in range(h)}) != 1:
                err(f"{name}: the eye at {(x, y, w, h)} is not one painted white")

    # Every combination of each block's state properties has a model.
    def variants(block):
        return set((load(ASSETS / "blockstates" / f"{block}.json") or {}).get("variants", {}))
    horizontal = ("north", "east", "south", "west")
    wanted = {
        lights["hook"]: {f"face={a},facing={f},lit={l}" for a in ("floor", "wall", "ceiling") for f in horizontal for l in ("false", "true")},
        bowl["block"]: {f"facing={f},fill={n}" for f in horizontal for n in range(4)},
        coffin["block"]: {f"facing={f},open={o},part={p}" for f in horizontal for o in ("false", "true") for p in ("head", "foot")},
        portrait["block"]: {f"facing={f},portrait={n}" for f in horizontal for n in portrait["portraits"]},
        fog["block"]: {f"facing={f},running={r}" for f in horizontal for r in ("false", "true")},
    }
    for block, keys in wanted.items():
        if variants(block) != keys:
            err(f"{block}: blockstate variants differ from its properties")

    # Every outcome a player can be told about has its text.
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    for source, enum, prefix in (("StringLightsItem", "Result", "message.jugcraft.string_lights."), ("CandyBowlBlockEntity", "Taken", "message.jugcraft.candy_bowl.")):
        values = re.search(rf"enum {enum} \{{([A-Z_,\s]+)\}}", java.get(source, ""))
        for value in re.findall(r"[A-Z_]+", values.group(1)) if values else []:
            if prefix + value.lower() not in lang:
                err(f"{source}.{enum}.{value} has no {prefix}{value.lower()} text")
        if not values:
            err(f"{source}.{enum} not found")
    for name in portrait["portraits"]:
        if f"message.jugcraft.haunted_portrait.{name}" not in lang:
            err(f"The {name} portrait has no name")

def check_decor2(java):
    """The second decorations batch: Java matches tools/agriculture.py (lights, the floating candles' places, the strand
    kinds), every block state has a model, the bag's walls have a face cut through them and its inside none (so the
    face shines with the candlelit far wall), and the soul-lit carving icon uses the client's soul colours."""
    def number(source, name):
        match = re.search(rf"\b{name} = ([\d.]+)[FLD]?;", java.get(source, ""))
        return float(match.group(1)) if match else None

    bag, candle, sconce, soul, bunting = ag.LUMINARIA, ag.FLOATING_CANDLE, ag.SCONCE, ag.SOUL_CARVING, ag.BAT_BUNTING
    expected = {("LuminariaBlock", "LIGHT"): bag["light"], ("FloatingCandleBlock", "MAX"): candle["max"],
                ("FloatingCandleBlock", "LIGHT_PER_CANDLE"): candle["light_per_candle"], ("FloatingCandleBlock", "BOB"): candle["bob"],
                ("FloatingCandleBlock", "BOB_TICKS"): candle["bob_ticks"], ("SkeletonHandSconceBlock", "LIGHT"): sconce["light"],
                ("CarvedPumpkinBlock", "SOUL_LIGHT"): soul["light"]}
    for (source, name), value in expected.items():
        if number(source, name) != value:
            err(f"{source}.{name} = {number(source, name)} differs from tools/agriculture.py ({value})")
    source = java.get("FloatingCandleBlock", "")
    table = re.search(r"CANDLE = \{(.*?)\};", source, re.S)
    places = [[tuple(float(v.rstrip("F")) for v in spot.split(",")) for spot in re.findall(r"\{([\d., F]+)\}", group)]
              for group in re.findall(r"\{(\{[^{}]*\}(?:, \{[^{}]*\})*)\}", table.group(1))] if table else []
    if places != [[tuple(float(v) for v in spot) for spot in group] for group in candle["candles"]]:
        err(f"FloatingCandleBlock.CANDLE {places} differs from tools/agriculture.py")
    heights = re.search(r"HEIGHT = \{([\d., F]+)\};", source)
    if not heights or [float(v.strip().rstrip("F")) for v in heights.group(1).split(",")] != [float(h) for h in candle["heights"]]:
        err("FloatingCandleBlock.HEIGHT differs from tools/agriculture.py")
    if any(len(group) != n + 1 for n, group in enumerate(candle["candles"])) or len(candle["heights"]) != candle["max"]:
        err("FLOATING_CANDLE needs one place per candle and a height for each place")
    if candle["max"] * candle["light_per_candle"] > 15:
        err("Floating candles would give more than light 15")
    strands = re.findall(r'[A-Z_]+\("([a-z_]+)", "([a-z_]+)"\)', java.get("StringLightHookBlockEntity", ""))
    if dict(strands) != {"lights": ag.STRING_LIGHTS["strand"], "bunting": bunting["item"],
                         **{strand: garland["item"] for strand, garland in decor20.GARLANDS.items()}}:
        err(f"StringLightHookBlockEntity.Strand {strands} differs from the strand items in tools/agriculture.py")

    def variants(block):
        return set((load(ASSETS / "blockstates" / f"{block}.json") or {}).get("variants", {}))
    horizontal = ("north", "east", "south", "west")
    booleans = ("false", "true")
    wanted = {
        bag["block"]: {f"color={c},lit={l}" for c in ag.DYE_COLORS for l in booleans},
        candle["block"]: {f"candles={n},lit={l}" for n in range(1, candle["max"] + 1) for l in booleans},
        sconce["block"]: {f"facing={f},lit={l}" for f in horizontal for l in booleans},
    }
    for block, keys in wanted.items():
        if variants(block) != keys:
            err(f"{block}: blockstate variants differ from its properties")

    # The bag's walls are cut with a face where the model shows them (x 4-11, y 6-15); the inside is whole.
    for color in ag.DYE_COLORS:
        for suffix in ("", "_lit"):
            image = ASSETS / "textures" / "block" / f"luminaria_{color}{suffix}.png"
            if not image.exists():
                err(f"Missing texture {image.name}")
                continue
            pixels = Image.open(image).convert("RGBA")
            if not any(pixels.getpixel((x, y))[3] == 0 for x in range(4, 12) for y in range(6, 16)):
                err(f"{image.name}: no face cut through the bag")
    for name in ("luminaria_inside", "luminaria_inside_lit"):
        image = ASSETS / "textures" / "block" / f"{name}.png"
        if not image.exists() or Image.open(image).convert("RGBA").getextrema()[3][0] < 255:
            err(f"{name}.png: missing, or see-through (the inside of the bag must be whole)")

    # The soul-lit icon and the client's soul colours agree.
    client = (ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client" / "CarvingTextures.java")
    colors = dict(re.findall(r"int (HOLE_SOUL|HOLE_WALL_SOUL) = 0xFF([0-9A-Fa-f]{6});", client.read_text(encoding="utf-8")))
    icon = ASSETS / "textures" / "item" / "hand_carved_pumpkin_soul.png"
    if icon.exists():
        pixels = Image.open(icon).convert("RGBA")
        used = {"%02X%02X%02X" % pixels.getpixel((x, y))[:3] for x in range(16) for y in range(16) if pixels.getpixel((x, y))[3]}
        if not {c.upper() for c in colors.values()} <= used or len(colors) != 2:
            err("The soul-lit carved pumpkin icon should use CarvingTextures' HOLE_SOUL and HOLE_WALL_SOUL")
    else:
        err("Missing texture hand_carved_pumpkin_soul.png")


def check_decor3(java):
    """The graveyard batch: the scare props' timings match tools/agriculture.py, every block state of the fence, gate,
    pillar, door, mound, angel and pop-up skeleton has a model, and the fence, gate and door are in vanilla's tags."""
    def number(source, name):
        match = re.search(rf"\b{name} = ([\d.]+)[FLD]?;", java.get(source, ""))
        return float(match.group(1)) if match else None

    expected = {("ScarePropBlockEntity", "PERIOD"): ag.SCARE_PERIOD}
    for source, info in (("GraveMoundBlock", ag.GRAVE_MOUND), ("PopUpSkeletonBlock", ag.POP_UP_SKELETON)):
        expected.update({(source, "REACH"): info["reach"], (source, "UP_TICKS"): info["up_ticks"],
                         (source, "COOLDOWN_TICKS"): info["cooldown_ticks"]})
        if f"implements ScareProp" not in java.get(source, ""):
            err(f"{source} is not a ScareProp")
        if info["up_ticks"] < ag.SCARE_PERIOD:
            err(f"{source}: it must stay up at least one look ({ag.SCARE_PERIOD} ticks)")
    for (source, name), value in expected.items():
        if number(source, name) != value:
            err(f"{source}.{name} = {number(source, name)} differs from tools/agriculture.py ({value})")

    def variants(block):
        return set((load(ASSETS / "blockstates" / f"{block}.json") or {}).get("variants", {}))
    horizontal = ("north", "east", "south", "west")
    booleans = ("false", "true")
    fence, gate, crypt = ag.CEMETERY_FENCE["fence"], ag.CEMETERY_FENCE["gate"], ag.CRYPT
    wanted = {
        gate: {f"facing={f},in_wall={w},open={o}" for f in horizontal for w in booleans for o in booleans},
        crypt["pillar"]: {"axis=x", "axis=y", "axis=z"},
        crypt["door"]: {f"facing={f},half={h},hinge={g},open={o}" for f in horizontal for h in ("lower", "upper") for g in ("left", "right")
                        for o in booleans},
        ag.GRAVE_MOUND["block"]: {f"facing={f},raised={r}" for f in horizontal for r in booleans},
        ag.POP_UP_SKELETON["block"]: {f"facing={f},raised={r}" for f in horizontal for r in booleans},
        ag.MOURNING_ANGEL["block"]: {f"facing={f},half={h}" for f in horizontal for h in ("lower", "upper")},
    }
    for block, keys in wanted.items():
        if variants(block) != keys:
            err(f"{block}: blockstate variants differ from its properties")
    parts = (load(ASSETS / "blockstates" / f"{fence}.json") or {}).get("multipart", [])
    straight = [p for p in parts if not set(p.get("when", {})) & set(dg.DIAGONAL_NAMES)]  # diagonals: check_diagonal_connections
    if sorted(next(iter(p.get("when", {"post": 1}))) for p in straight) != sorted(["post", "north", "east", "south", "west"]):
        err(f"{fence}: the multipart needs the post and a side for each direction")
    for tag, entry in (("fences", fence), ("fence_gates", gate), ("doors", crypt["door"])):
        for registry in ("block", "item"):
            values = (load(RES / "data" / "minecraft" / "tags" / registry / f"{tag}.json") or {}).get("values", [])
            if f"{MOD}:{entry}" not in values:
                err(f"{entry} is missing from the {registry} tag minecraft:{tag}")


def check_decor4(java):
    """The witch's cottage: Java matches tools/agriculture.py (lights, timings, the brews, arrangements, fortunes and
    spreads), every block state has a model, every fortune and spell has its text, and every brew its item tag."""
    def number(source, name):
        match = re.search(rf"\b{name} = ([\d.]+)[FLD]?;", java.get(source, ""))
        return float(match.group(1)) if match else None

    cauldron, shelf, ball, grimoire, broom = ag.CAULDRON, ag.APOTHECARY_SHELF, ag.CRYSTAL_BALL, ag.GRIMOIRE, ag.BROOM
    expected = {("BubblingCauldronBlock", "BREW_LIGHT"): cauldron["light"], ("ApothecaryShelfBlock", "ARRANGEMENTS"): shelf["arrangements"],
                ("CrystalBallBlock", "LIGHT"): ball["light"], ("CrystalBallBlock", "GAZING_LIGHT"): ball["gazing_light"],
                ("CrystalBallBlock", "FORTUNES"): len(ball["fortunes"]), ("CrystalBallBlock", "GAZE_TICKS"): ball["gaze_ticks"],
                ("GrimoireStandBlock", "LIGHT"): grimoire["light"]}
    for (source, name), value in expected.items():
        if number(source, name) != value:
            err(f"{source}.{name} = {number(source, name)} differs from tools/agriculture.py ({value})")

    def enum(source, name):
        match = re.search(rf"enum {name} implements StringRepresentable \{{\s*([A-Z_, ]+);", java.get(source, ""))
        return [v.strip().lower() for v in match.group(1).split(",")] if match else None
    if enum("BubblingCauldronBlock", "Brew") != ["empty", "water"] + list(cauldron["brews"]) + list(ag.HEX["brews"]):
        err(f"BubblingCauldronBlock.Brew {enum('BubblingCauldronBlock', 'Brew')} differs from CAULDRON's brews and HEX's")
    if enum("GrimoireStandBlock", "Spread") != list(grimoire["spreads"]):
        err(f"GrimoireStandBlock.Spread {enum('GrimoireStandBlock', 'Spread')} differs from GRIMOIRE's spreads")

    def variants(block):
        return set((load(ASSETS / "blockstates" / f"{block}.json") or {}).get("variants", {}))
    horizontal = ("north", "east", "south", "west")
    wanted = {
        cauldron["block"]: {f"contents={c}" for c in ["empty", "water"] + list(cauldron["brews"])}
        | {f"contents={h},doses={d}" for h in ag.HEX["brews"] for d in range(1, ag.HEX["doses"] + 1)},
        shelf["block"]: {f"arrangement={n},facing={f}" for f in horizontal for n in range(shelf["arrangements"])},
        ball["block"]: {"gazing=false", "gazing=true"},
        grimoire["block"]: {f"facing={f},page={p}" for f in horizontal for p in grimoire["spreads"]},
        broom["block"]: {f"facing={f}" for f in horizontal},
    }
    for block, keys in wanted.items():
        if variants(block) != keys:
            err(f"{block}: blockstate variants differ from its properties")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    for n in range(len(ball["fortunes"])):
        if f"message.{MOD}.crystal_ball.fortune.{n}" not in lang:
            err(f"Crystal ball fortune {n} has no text")
    for spread in grimoire["spreads"]:
        if f"message.{MOD}.grimoire.{spread}" not in lang:
            err(f"The {spread} spread has no name")
    for colour, items in cauldron["brews"].items():
        values = (load(DATA / MOD / "tags" / "item" / "brew" / f"{colour}.json") or {}).get("values", [])
        if sorted(values) != sorted(items):
            err(f"The item tag {MOD}:brew/{colour} differs from CAULDRON's brews")
    check_hexes(java, number, lang)
    check_broomstick(java, number, lang)
    check_werewolves(java, number, lang)
    check_squirrels(java, number, lang)
    check_pumpkling(java, number, lang)
    check_midway(java, number, lang)
    check_ferris_wheel(java, number, lang)
    check_pinata(java, number, lang)
    check_hot_air_balloon(java, number, lang)
    check_leaf_blower(java, number, lang)
    check_graveyard_flora(java, number, lang)
    check_churchyard_ornaments(java, number, lang)
    check_haunted_house_props(java, lang)
    check_witchs_workshop(java, lang)
    check_laboratory_larder_dining(java, lang)
    check_pumpkin_night(java, lang)


def check_churchyard_ornaments(java, number, lang):
    """Halloween decorations batch 15: Java's bone pile and lantern match tools/decor15.py; each ornament is registered,
    named, drawn on its 64 x 64 texture, drops and has its recipe; the Gargoyle is a headstone style."""
    bp = decor15.BONE_PILE
    for name, value in (("MAX_LAYERS", bp["layers"]), ("LAYER_PIXELS", bp["layer_pixels"]), ("RATTLE_CHANCE", bp["rattle_chance"])):
        found = number("BonePileBlock", name)
        if found is None or float(found) != value:
            err(f"BonePileBlock.{name} = {found} differs from tools/decor15.py ({value})")
    if number("JugcraftAgriculture", "WITCHS_LANTERN_LIGHT") != float(decor15.WITCHS_LANTERN["light"]):
        err("JugcraftAgriculture.WITCHS_LANTERN_LIGHT differs from tools/decor15.py")
    main = java.get("JugcraftAgriculture", "")
    for block in decor15.blocks():
        if f'"{block}"' not in main:
            err(f"JugcraftAgriculture.java does not register {block}")
        if f"block.{MOD}.{block}" not in lang:
            err(f"{block} has no name")
        if not (DATA / MOD / "loot_table" / "blocks" / f"{block}.json").is_file():
            err(f"{block} has no loot table")
        with Image.open(ASSETS / "textures" / "block" / f"{block}.png") as img:
            if img.size != (64, 64):
                err(f"textures/block/{block}.png is {img.size}, not 64 x 64")
    for recipe in decor15.SHAPED + decor15.SHAPELESS:
        if not (DATA / MOD / "recipe" / f"{recipe['id']}.json").is_file():
            err(f"The {recipe['id']} recipe is missing")
    if 'GARGOYLE("gargoyle"' not in java.get("HeadstoneBlock", "") or "gargoyle" not in gy.HEADSTONES:
        err("The Gargoyle must be a headstone style in HeadstoneBlock.java and tools/graveyard.py")


def check_haunted_house_props(java, lang):
    """Halloween decorations batch 16: Java's eyeball, candles and monster's head match tools/decor16.py; each prop is
    registered, named, drawn on its 64 x 64 texture, drops and has its recipe; the eyeball's renderer and quads are
    wired up; the pillar candles are vanilla candles (block tag minecraft:candles). The harvest plushes are checked with
    the midway's."""
    def number(source, name):
        match = re.search(rf"\b{name} = ([\d.]+)[FLD]?;", java.get(source, ""))
        return float(match.group(1)) if match else None

    eye, head = decor16.FLYING_EYEBALL, decor16.MONSTER_HEAD
    expected = {("FlyingEyeballBlock", "HOVER_PIXELS"): eye["hover_pixels"], ("FlyingEyeballBlock", "BOB_TICKS"): eye["bob_ticks"],
                ("FlyingEyeballBlock", "FLAP_TICKS"): eye["flap_ticks"], ("FlyingEyeballBlock", "WATCH_RANGE"): eye["watch_range"],
                ("FlyingEyeballBlock", "TURN_SPEED"): eye["turn_speed"], ("PillarCandleBlock", "FLAME_ABOVE"): decor16.PILLAR["flame_above"],
                ("MonsterHeadBlock", "LIGHT"): head["light"], ("MonsterHeadBlock", "SPARK_CHANCE"): head["spark_chance"]}
    for (source, name), value in expected.items():
        found = number(source, name)
        if found is None or abs(found - value) > 1e-9:
            err(f"{source}.{name} = {found} differs from tools/decor16.py ({value})")
    layout = ",\n".join("{" + ", ".join("{" + ", ".join(f"{v:.1f}" for v in c) + "}" for c in decor16.CANDLES[n]) + "}"
                         for n in sorted(decor16.CANDLES))
    flat = re.sub(r"\s+", "", java.get("PillarCandleBlock", ""))
    if re.sub(r"\s+", "", layout) not in flat:
        err("PillarCandleBlock.LAYOUT differs from tools/decor16.py CANDLES")
    main = java.get("JugcraftAgriculture", "")
    for call in ("registerHauntedHouseProps();", "FlyingEyeballBlock::new", "PillarCandleBlock::new", "MultifaceBlock::new", "MonsterHeadBlock::new"):
        if call not in main:
            err(f"JugcraftAgriculture.java must call {call}")
    for block in decor16.blocks():
        if f'"{block}"' not in main:
            err(f"JugcraftAgriculture.java does not register {block}")
        if f"block.{MOD}.{block}" not in lang:
            err(f"{block} has no name")
        if not (DATA / MOD / "loot_table" / "blocks" / f"{block}.json").is_file():
            err(f"{block} has no loot table")
        with Image.open(ASSETS / "textures" / "block" / f"{block}.png") as img:
            if img.size != (64, 64):
                err(f"textures/block/{block}.png is {img.size}, not 64 x 64")
    for name in decor16.PLUSHES:
        if name not in midway.PLUSHES or lang.get(f"block.{MOD}.{name}") != decor16.PLUSHES[name]:
            err(f"The harvest plush {name} must be a midway plush with its name")
        with Image.open(ASSETS / "textures" / "block" / f"{name}.png") as img:
            if img.size != (64, 64):
                err(f"textures/block/{name}.png is {img.size}, not 64 x 64")
    for recipe in decor16.SHAPED + decor16.SHAPELESS:
        if not (DATA / MOD / "recipe" / f"{recipe['id']}.json").is_file():
            err(f"The {recipe['id']} recipe is missing")
    candles = (load(DATA / "minecraft" / "tags" / "block" / "candles.json") or {}).get("values", [])
    for block in decor16.PILLAR_CANDLES:
        if f"{MOD}:{block}" not in candles:
            err(f"{block} must be in the block tag minecraft:candles, to be lit as vanilla's candles are")
    quads = load(ASSETS / "decor16_quads.json") or {}
    for model in ("flying_eyeball_body", "flying_eyeball_iris", "flying_eyeball_wing_left", "flying_eyeball_wing_right"):
        if not quads.get(model):
            err(f"decor16_quads.json has no {model}")
    client = ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client"
    if 'Jugcraft.id("decor16_quads.json")' not in (client / "DecorQuads.java").read_text(encoding="utf-8"):
        err("DecorQuads.java must read decor16_quads.json")
    if "FLYING_EYEBALL_ENTITY, FlyingEyeballRenderer::new" not in (client / "JugcraftClient.java").read_text(encoding="utf-8"):
        err("JugcraftClient.java must draw the flying eyeball")


def check_laboratory_larder_dining(java, lang):
    """Halloween decorations batch 19, the Laboratory, the Larder and the Dining Room: Java's numbers match
    tools/decor19.py; each block is registered, named, drawn on its texture, drops and has its recipe; the renderers'
    quads are generated."""
    def number(source, name):
        match = re.search(rf"\b{name} = (-?[\d.]+)[FLD]?;", java.get(source, ""))
        return float(match.group(1)) if match else None

    harness, chair, light, clock = decor19.HARNESS, decor19.CHAIR, decor19.WITCHLIGHT, decor19.CLOCK
    expected = {("LightningHarnessBlock", "PULSE"): harness["pulse"], ("LightningHarnessBlock", "COIL_RANGE"): harness["coil_range"],
                ("LightningHarnessBlock", "STRIKE_REACH"): harness["strike_reach"], ("LightningHarnessBlock", "TABLE_REACH"): harness["table_reach"],
                ("LightningHarnessBlock", "CHECK_TICKS"): harness["check_ticks"], ("LightningHarnessBlock", "ARC_TICKS"): harness["arc_ticks"],
                ("LightningHarnessBlock", "COOLDOWN_TICKS"): harness["cooldown_ticks"], ("LabTableBlock", "WAKE_TICKS"): harness["wake_ticks"],
                ("CrawlingHandBlock", "LAP_TICKS"): decor19.HAND["lap_ticks"], ("SilkCocoonBlockEntity", "SLOTS"): decor19.COCOON["slots"],
                ("SilkCocoonBlock", "WRIGGLE_TICKS"): decor19.COCOON["wriggle_ticks"], ("SilkSpoolStackBlock", "SPOOLS"): decor19.SPOOLS["spools"],
                ("HauntedDiningChairBlock", "SEAT"): chair["seat"], ("HauntedDiningChairBlock", "REACH"): chair["reach"],
                ("HauntedDiningChairBlock", "CHECK_TICKS"): chair["check_ticks"], ("HauntedDiningChairBlock", "OUT_TICKS"): chair["out_ticks"],
                ("HauntedDiningChairBlock", "SLIDE"): chair["slide"], ("FloatingTableSettingBlock", "LIGHT"): decor19.SETTING["light"],
                ("GrandfatherClockBlock", "HOUR_TICKS"): clock["hour_ticks"], ("GrandfatherClockBlock", "PULSE_TICKS"): clock["pulse_ticks"],
                ("GrandfatherClockBlock", "FACE_TICKS"): clock["face_ticks"], ("GrandfatherClockBlock", "STRIKE_GAP"): clock["strike_gap"],
                ("Witchlights", "ASLEEP"): light["asleep"], ("Witchlights", "AWAKE"): light["awake"], ("Witchlights", "RANGE"): light["range"],
                ("Witchlights", "LINGER_TICKS"): light["linger_ticks"], ("Witchlights", "FADE_TICKS"): light["fade_ticks"],
                ("Witchlights", "CHECK_TICKS"): light["check_ticks"], ("HarvestMoonLampBlock", "LIGHT"): decor19.MOON["light"]}
    for (source, name), value in expected.items():
        found = number(source, name)
        if found is None or abs(found - value) > 1e-9:
            err(f"{source}.{name} = {found} differs from tools/decor19.py ({value})")
    slow = re.search(r"SLOW = new Vec3\(([\d.]+), ([\d.]+), ([\d.]+)\)", java.get("WebDrapeBlock", ""))
    if not slow or [float(v) for v in slow.groups()] != decor19.DRAPE["slow"]:
        err("WebDrapeBlock.SLOW differs from tools/decor19.py DRAPE")
    for enum, names in (("FloatingTableSettingBlock", decor19.SETTINGS), ("YardSilhouetteBlock", decor19.FIGURES),
                        ("Witchlights", decor19.WITCHLIGHT_COLOURS)):
        for name in names:
            if not re.search(rf"\b{name.upper()}\b", java.get(enum, "")):
                err(f"{enum} has no {name.upper()} to match tools/decor19.py")
    main = java.get("JugcraftAgriculture", "")
    if "registerLaboratoryLarderDining();" not in main:
        err("JugcraftAgriculture.java must call registerLaboratoryLarderDining()")
    for block in decor19.blocks():
        if f'"{block}"' not in main:
            err(f"JugcraftAgriculture.java does not register {block}")
        if f"block.{MOD}.{block}" not in lang:
            err(f"{block} has no name")
        if not (DATA / MOD / "loot_table" / "blocks" / f"{block}.json").is_file():
            err(f"{block} has no loot table")
        if not (ASSETS / "textures" / "block" / f"{block}.png").is_file():
            err(f"{block} has no texture")
    for recipe in decor19.SHAPED + decor19.SHAPELESS:
        if not (DATA / MOD / "recipe" / f"{recipe['id']}.json").is_file():
            err(f"The {recipe['id']} recipe is missing")
    drawn = load(ASSETS / "decor19_quads.json") or {}
    wanted = (["crawling_hand", "lab_table_arms", "lab_table_eyes", "silk_cocoon", "spiderling", "haunted_dining_chair",
               "setting_flame", "clock_hour_hand", "clock_minute_hand", "clock_pendulum", "clock_ghost_face", "witchlight_wisp",
               "witchlight_glow"] + [f"crawling_hand_finger_{i}" for i in range(4)] + [f"silk_spool_silk_{i}" for i in range(decor19.SPOOLS["spools"])]
              + [f"clock_moon_{p}" for p in range(8)] + [f"harvest_moon_face_{p}" for p in range(8)]
              + [f"setting_{piece}" for piece in ("plate", "cutlery", "goblet", "candlestick", "saucer", "cup", "teapot", "platter")]
              + [f"silhouette_{f}" for f in decor19.FIGURES] + [f"silhouette_{f}_eyes" for f in decor19.FIGURES])
    for name in wanted:
        if not drawn.get(name):
            err(f"assets/{MOD}/decor19_quads.json has no {name}")
    client = ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client"
    if '"decor19_quads.json"' not in (client / "DecorQuads.java").read_text(encoding="utf-8"):
        err("DecorQuads.java must load decor19_quads.json")
    registered = (client / "JugcraftClient.java").read_text(encoding="utf-8")
    for renderer in ("LightningHarnessRenderer", "CrawlingHandRenderer", "SilkCocoonRenderer", "EggSacRenderer", "SilkSpoolRenderer",
                     "DiningChairRenderer", "TableSettingRenderer", "GrandfatherClockRenderer", "WitchlightRenderer",
                     "YardSilhouetteRenderer", "HarvestMoonLampRenderer"):
        if f"{renderer}::new" not in registered:
            err(f"JugcraftClient.java must register {renderer}")


def check_pumpkin_night(java, lang):
    """Halloween decorations batch 20, Pumpkin Night: Java's numbers match tools/decor20.py (the server's rules and where
    the client draws the faces, the goods, the chalk and the effigy's head); each block and item is registered, named,
    drawn, drops and has its recipe; the heirlooms glow and fly as listed; the choir's sounds and the cloak's quads are
    generated."""
    def number(source, name, text=None):
        match = re.search(rf"\b{name} = (-?[\d.]+)[FLD]?;", text if text is not None else java.get(source, ""))
        return float(match.group(1)) if match else None

    def floats(text, name):
        match = re.search(rf"\b{name} = (\{{.*?\}});", text, re.S)
        return [float(v.rstrip("FLD")) for v in re.findall(r"-?[\d.]+[FLD]?", match.group(1))] if match else None

    stand, effigy, choir = decor20.FARM_STAND, decor20.EFFIGY, decor20.CHOIR
    expected = {("SingingPumpkinBlock", "NOTES"): choir["notes"], ("SingingPumpkinBlock", "OPEN_TICKS"): choir["open_ticks"],
                ("SingingPumpkinBlock", "LIGHT"): choir["light"], ("HarvestEffigyBlock", "BURN_TICKS"): effigy["burn_ticks"],
                ("HarvestEffigyBlock", "CHEER_RADIUS"): effigy["cheer_radius"], ("HarvestEffigyBlock", "CROW_RADIUS"): effigy["crow_radius"],
                ("HarvestEffigyBlock", "CHECK_TICKS"): effigy["check_ticks"], ("HarvestEffigyBlock", "LIGHT"): effigy["light"],
                ("HarvestCheer", "REGENERATION_TICKS"): effigy["regeneration_ticks"], ("HarvestCheer", "LUCK_TICKS"): effigy["luck_ticks"],
                ("EffigyAshesBlock", "YIELD"): decor20.ASHES["ash_yield"], ("FarmStandBlockEntity", "CRATES"): stand["crates"],
                ("FarmStandMenu", "REACH"): stand["reach"], ("FarmStandMenu", "RATE_TICKS"): stand["rate_ticks"],
                ("JugcraftAgriculture", "HEARTH_ASH_RADIUS"): decor20.HEARTH_ASH["radius"],
                ("JugcraftAgriculture", "HEARTH_ASH_DOSES"): decor20.HEARTH_ASH["doses"]}
    for (source, name), value in expected.items():
        found = number(source, name)
        if found is None or abs(found - value) > 1e-9:
            err(f"{source}.{name} = {found} differs from tools/decor20.py ({value})")
    if floats(java.get("FarmStandMenu", ""), "PRICE_STEPS") != [float(v) for v in stand["price_steps"]]:
        err("FarmStandMenu.PRICE_STEPS differs from tools/decor20.py FARM_STAND")
    for voice in decor20.VOICES:
        if not re.search(rf"\b{voice.upper()}\b", java.get("SingingPumpkinBlock", "")):
            err(f"SingingPumpkinBlock.Voice has no {voice.upper()} to match tools/decor20.py")
    client = ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client"

    def client_source(name):
        path = client / f"{name}.java"
        return path.read_text(encoding="utf-8") if path.is_file() else ""

    faces = floats(client_source("SingingPumpkinRenderer"), "FACES")
    if faces != [v for spec in decor20.VOICES.values() for v in spec["face"]]:
        err("SingingPumpkinRenderer.FACES differs from tools/decor20.py VOICES")
    renderer = client_source("FarmStandRenderer")
    for name, value in (("CRATES", stand["crate_middles"]), ("TAGS", stand["tags"]), ("BOARD", [stand["board"]])):
        if floats(renderer, name) != [float(v) for row in value for v in row]:
            err(f"FarmStandRenderer.{name} differs from tools/decor20.py FARM_STAND")
    blaze = client_source("HarvestEffigyRenderer")
    for name, key in (("HEAD_PIXELS", "head"), ("NECK_TOP", "neck_top"), ("HAND_X", "hand_x"), ("HAND_Y", "hand_y")):
        found = number("", name, blaze)
        if found is None or abs(found - effigy[key]) > 1e-9:
            err(f"HarvestEffigyRenderer.{name} = {found} differs from tools/decor20.py EFFIGY")
    glows = client_source("CarvingTextures")
    for pumpkin, spec in decor20.HEIRLOOMS.items():
        if not re.search(rf'"hand_carved_{pumpkin}"\) \? {spec["glow"].upper()}\b', glows):
            err(f"CarvingTextures.Glow does not give hand_carved_{pumpkin} the {spec['glow']} glow")
        for item, factor in ((pumpkin, spec["throw"]), (f"hand_carved_{pumpkin}", spec["carved_throw"])):
            if not re.search(rf'"{MOD}:{item}", {factor}\)', java.get("TrebuchetBlockEntity", "")):
                err(f"TrebuchetBlockEntity.FACTORS does not throw {item} at {factor}")
    main = java.get("JugcraftAgriculture", "")
    if "registerPumpkinNight();" not in main:
        err("JugcraftAgriculture.java must call registerPumpkinNight()")
    for block in decor20.blocks():
        id_ok = f'"{block}"' in main or (block.startswith("singing_pumpkin_") and 'SINGING_PUMPKIN = "singing_pumpkin_"' in main)
        if not id_ok:
            err(f"JugcraftAgriculture.java does not register {block}")
        if f"block.{MOD}.{block}" not in lang:
            err(f"{block} has no name")
        if not (DATA / MOD / "loot_table" / "blocks" / f"{block}.json").is_file():
            err(f"{block} has no loot table")
        if not (ASSETS / "textures" / "block" / f"{block}.png").is_file():
            err(f"{block} has no texture")
    for item in [g["item"] for g in decor20.GARLANDS.values()] + [decor20.HEARTH_ASH["item"]]:
        if f'"{item}"' not in main:
            err(f"JugcraftAgriculture.java does not register {item}")
        if f"item.{MOD}.{item}" not in lang:
            err(f"{item} has no name")
        if not (ASSETS / "textures" / "item" / f"{item}.png").is_file():
            err(f"{item} has no texture")
    for key in decor20.MESSAGES:
        if key not in lang:
            err(f"The lang file has no {key}")
    for recipe in decor20.SHAPED + decor20.SHAPELESS:
        if not (DATA / MOD / "recipe" / f"{recipe['id']}.json").is_file():
            err(f"The {recipe['id']} recipe is missing")
    ashes = load(DATA / MOD / "loot_table" / "blocks" / f"{decor20.ASHES['block']}.json") or {}
    entries = [e for pool in ashes.get("pools", []) for e in pool.get("entries", [])]
    if [e.get("name") for e in entries] != [f"{MOD}:{decor20.HEARTH_ASH['item']}"] \
            or entries[0].get("modifier", [{}])[0].get("count") != decor20.ASHES["ash_yield"]:
        err(f"Effigy Ashes must drop {decor20.ASHES['ash_yield']} Hearth Ash")
    sounds = load(ASSETS / "sounds.json") or {}
    for event in [f"singing_pumpkin.{voice}" for voice in decor20.VOICES] + ["effigy.burn"]:
        if event not in sounds:
            err(f"sounds.json has no {event}")
        for sound in sounds.get(event, {}).get("sounds", []):
            ns, path = sound["name"].split(":")
            if not (RES / "assets" / ns / "sounds" / f"{path}.ogg").is_file():
                err(f"The sound {sound['name']} has no .ogg")
    drawn = load(ASSETS / "decor20_quads.json") or {}
    if not drawn.get("harvest_effigy_cloak"):
        err(f"assets/{MOD}/decor20_quads.json has no harvest_effigy_cloak")
    if '"decor20_quads.json"' not in client_source("DecorQuads"):
        err("DecorQuads.java must load decor20_quads.json")
    for texture in ("singing_pumpkin_face", "pumpkin_vine_garland", "autumn_leaf_garland", "harvest_effigy_cloak"):
        if not (ASSETS / "textures" / "entity" / f"{texture}.png").is_file():
            err(f"The client's texture entity/{texture} is missing")
    registered = client_source("JugcraftClient")
    for renderer_name in ("SingingPumpkinRenderer", "HarvestEffigyRenderer", "FarmStandRenderer", "FarmStandScreen"):
        if f"{renderer_name}::new" not in registered:
            err(f"JugcraftClient.java must register {renderer_name}")


def bat_reach(quads, bat):
    """How far the Bat in a Jar's wings reach from the jar's middle, across (pixels), and their lowest and highest points,
    over its flight awake and its sway asleep, turned and scaled as OddityJarRenderer.bat() draws them."""
    import math

    def mul(a, b):
        return [[sum(a[i][k] * b[k][j] for k in range(4)) for j in range(4)] for i in range(4)]

    def move(x, y, z):
        return [[1, 0, 0, x], [0, 1, 0, y], [0, 0, 1, z], [0, 0, 0, 1]]

    def turn(axis, degrees):
        c, s = math.cos(math.radians(degrees)), math.sin(math.radians(degrees))
        if axis == "y":
            return [[c, 0, s, 0], [0, 1, 0, 0], [-s, 0, c, 0], [0, 0, 0, 1]]
        return [[c, -s, 0, 0], [s, c, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]]

    k = bat["scale"]
    scale = [[k, 0, 0, 0], [0, k, 0, 0], [0, 0, k, 0], [0, 0, 0, 1]]
    reach, low, high = 0.0, 99.0, -99.0
    for step in range(480):
        t = step * 0.25
        for awake in (True, False):
            if awake:
                a = t * 0.35
                base = mul(mul(move(8 + bat["orbit"] * math.cos(a), bat["fly_y"] + bat["bob"] * math.sin(t * 0.5), 8 + bat["orbit"] * math.sin(a)),
                               turn("y", math.degrees(-a) + 180)), turn("z", 180))
                wing = 55 * math.sin(t * 1.6)
            else:
                base = mul(move(8, bat["hang_y"], 8), turn("y", 20 * math.sin(step * 0.37)))
                wing = 80
            base = mul(base, scale)
            for part, m in (("oddity_bat_wing_left", mul(mul(base, move(-1.2, 0, 0)), turn("y", -wing))),
                            ("oddity_bat_wing_right", mul(mul(base, move(1.2, 0, 0)), turn("y", wing)))):
                for quad in quads.get(part, []):
                    for v in quad["vertices"]:
                        p = [sum(m[i][j] * (v[j] if j < 3 else 1.0) for j in range(4)) for i in range(3)]
                        reach = max(reach, math.hypot(p[0] - 8, p[2] - 8))
                        low, high = min(low, p[1]), max(high, p[1])
    return reach, low, high


def coplanar_pairs(quads, gap=0.1):
    """How many pairs of a model's quads face the same way on planes closer than `gap` pixels and overlap: they would
    flicker against each other (z-fighting)."""
    faces = []
    for quad in quads:
        normal = quad["normal"]
        axis = max(range(3), key=lambda k: abs(normal[k]))
        if abs(abs(normal[axis]) - 1) > 1e-3:
            continue
        others = [k for k in range(3) if k != axis]
        points = quad["vertices"]
        faces.append((axis, normal[axis] > 0, sum(p[axis] for p in points) / len(points),
                      [min(p[k] for p in points) for k in others], [max(p[k] for p in points) for k in others]))
    pairs = 0
    for a in range(len(faces)):
        for b in range(a + 1, len(faces)):
            (axis, sign, plane, lo, hi), (axis2, sign2, plane2, lo2, hi2) = faces[a], faces[b]
            if axis == axis2 and sign == sign2 and abs(plane - plane2) < gap - 1e-6 \
                    and all(min(hi[k], hi2[k]) - max(lo[k], lo2[k]) > 1e-4 for k in range(2)):
                pairs += 1
    return pairs


def check_bigger_jars(java, quads, client):
    """The Witch's Workshop's bigger jars, each its small jar made bigger: Java's numbers match tools/decor17.py; each is
    its small jar's model `scale` times over on the small jar's textures (its parts, put back together, fill the small
    jar's box made as much bigger), and its shapes and renderer scale the small jar's; the heart stays inside its glass at
    its fullest swell; and every block of each has its model."""
    import math

    def number(text, name):
        match = re.search(rf"\b{name} = (-?[\d.]+)[FLD]?;", text)
        return float(match.group(1)) if match else None

    def floats(text, name):
        match = re.search(rf"\b{name} = \{{([^}}]*)\}};", text)
        return [float(v.strip().rstrip("FLD")) for v in match.group(1).split(",")] if match else None

    def source(name):
        path = client / f"{name}.java"
        return path.read_text(encoding="utf-8") if path.is_file() else ""

    g = decor17.GIANT_HEART
    block = java.get("GiantBeatingHeartBlock", "")
    if floats(block, "TEMPOS") != [float(t) for t in g["tempos"]] or number(block, "PULSE_TICKS") != g["pulse_ticks"] or number(block, "SIZE") != g["size"]:
        err("GiantBeatingHeartBlock's TEMPOS, PULSE_TICKS and SIZE differ from tools/decor17.py GIANT_HEART")
    if "grown(OddityJarBlock.BOX, SIZE, SIZE, SIZE)" not in block:
        err("GiantBeatingHeartBlock's shapes must be the Beating Heart Jar's box made SIZE times bigger")
    renderer = source("GiantBeatingHeartRenderer")
    if ("pose.scale(GiantBeatingHeartBlock.SIZE, GiantBeatingHeartBlock.SIZE, GiantBeatingHeartBlock.SIZE)" not in renderer
            or "OddityJarRenderer.heart(pose" not in renderer):
        err("GiantBeatingHeartRenderer must draw the Beating Heart Jar's heart (OddityJarRenderer.heart), SIZE times bigger")
    # The jar's heart, at its fullest swell and turned any way, stays inside the glass (and so does the giant's, the same
    # made bigger).
    jar = decor17.JARS["beating_heart_jar"]
    oddity = source("OddityJarRenderer")
    if floats(oddity, "HEART") != list(jar["heart"]) or number(oddity, "HEART_SWELL") != jar["swell"]:
        err("OddityJarRenderer.HEART and HEART_SWELL differ from tools/decor17.py JARS beating_heart_jar heart and swell")
    heart = quads.get("oddity_heart", [])
    if not heart:
        err("decor17_quads.json has no oddity_heart")
    else:
        k = 1 + jar["swell"]
        reach = max(math.hypot(v[0], v[2]) for q in heart for v in q["vertices"]) * k
        top = jar["heart"][1] + max(v[1] for q in heart for v in q["vertices"]) * (k * 0.96 + 0.04)
        shape = {**decor17.ODDITY_JAR, **jar.get("jar", {})}
        glass = 8 - shape["glass"]
        if reach > glass or top > shape["glass_top"]:
            err(f"The Beating Heart Jar's heart leaves its glass at its fullest: it reaches {reach:.2f} across (the glass is at {glass:.2f}) and up to {top:.2f}")
    vessel_renderer = source("SpecimenVesselRenderer")
    if "pose.scale(state.scale, state.scale, state.scale)" not in vessel_renderer or "SpecimenJarRenderer.draw(pose" not in vessel_renderer:
        err("SpecimenVesselRenderer must draw the Specimen Jar's specimen (SpecimenJarRenderer.draw), as many times bigger as the jar")
    if "grown(SpecimenJarBlock.BOX, scale, across, deep)" not in java.get("SpecimenVesselBlock", ""):
        err("SpecimenVesselBlock's shapes must be the Specimen Jar's box made `scale` times bigger")
    for vessel, info in decor17.SPECIMEN_VESSELS.items():
        source_ = java.get({"tall_specimen_jar": "TallSpecimenJarBlock", "specimen_tank": "SpecimenTankBlock"}[vessel], "")
        cells = re.search(r"CELLS = box\((\d+), (\d+), (\d+)\);", source_)
        if (number(source_, "LIGHT") != info["light"] or number(source_, "SCALE") != info["scale"]
                or not cells or tuple(int(c) for c in cells.groups()) != tuple(info["size"])):
            err(f"{vessel}'s LIGHT, SCALE and CELLS differ from tools/decor17.py SPECIMEN_VESSELS")
    # Each is its small jar's model made bigger: put back together, its parts fill the small jar's box `scale` times
    # over, and draw on the small jar's textures.
    for big, small, scale, size in [(g["block"], "beating_heart_jar", g["size"], (g["size"],) * 3)] + [
            (v, ag.SPECIMEN_JAR["block"], info["scale"], info["size"]) for v, info in decor17.SPECIMEN_VESSELS.items()]:
        whole = load(ASSETS / "models" / "block" / f"{small}.json") or {}
        lo = [min(e["from"][k] for e in whole.get("elements", [])) for k in range(3)] if whole.get("elements") else None
        hi = [max(e["to"][k] for e in whole.get("elements", [])) for k in range(3)] if whole.get("elements") else None
        cells = [(r, u, a) for a in range(size[2]) for u in range(size[1]) for r in range(size[0])]
        found_lo, found_hi, textures = [1e9] * 3, [-1e9] * 3, set()
        for part, (r, u, a) in enumerate(cells):
            model = load(ASSETS / "models" / "block" / f"{big}_{part}.json")
            if not model:
                continue
            origin = (16 * (size[0] - 1 - r), 16 * u, 16 * a)
            textures |= {t for key, t in model.get("textures", {}).items() if key != "particle"}
            for e in model.get("elements", []):
                for k in range(3):
                    found_lo[k] = min(found_lo[k], e["from"][k] + origin[k])
                    found_hi[k] = max(found_hi[k], e["to"][k] + origin[k])
        middle = (8 * size[0], 0, 8 * size[2])
        if lo is None or any(abs(found_lo[k] - (middle[k] + (lo[k] - (0 if k == 1 else 8)) * scale)) > 0.2
                             or abs(found_hi[k] - (middle[k] + (hi[k] - (0 if k == 1 else 8)) * scale)) > 0.2 for k in range(3)):
            err(f"{big}'s parts don't make up the {small}'s model {scale} times bigger: {found_lo} to {found_hi}")
        small_textures = {t for key, t in whole.get("textures", {}).items() if key != "particle"}
        if not textures or not textures <= small_textures:
            err(f"{big}'s models draw on {sorted(textures)}, not the {small}'s textures {sorted(small_textures)}")
    for big in decor17.big_jars():
        states = load(ASSETS / "blockstates" / f"{big}.json") or {}
        for entry in states.get("multipart", []):
            model = entry["apply"]["model"].split(":")[1]
            if not (ASSETS / "models" / f"{model}.json").is_file():
                err(f"blockstates/{big}.json uses {model}, which is missing")
        if not states.get("multipart"):
            err(f"blockstates/{big}.json has no parts")
    registered = (client / "JugcraftClient.java").read_text(encoding="utf-8")
    for entity, renderer_name in (("GIANT_HEART_ENTITY", "GiantBeatingHeartRenderer"), ("SPECIMEN_VESSEL_ENTITY", "SpecimenVesselRenderer")):
        if f"{entity}, {renderer_name}::new" not in registered:
            err(f"JugcraftClient.java must draw {entity} with {renderer_name}")


def check_witchs_workshop(java, lang):
    """Halloween decorations batch 17, the Witch's Workshop: Java's numbers match tools/decor17.py; each block is registered,
    named, drawn on its texture, drops and has its recipe; the candelabra's layout is generated for Java; the ember bed is a
    heat source; the renderers and their quads are wired up."""
    def number(source, name):
        match = re.search(rf"\b{name} = (-?[\d.]+)[FLD]?;", java.get(source, ""))
        return float(match.group(1)) if match else None

    cauldron, broom = decor17.CAULDRON, decor17.BROOM
    expected = {("HornedSkullCauldronBlock", "LEVELS"): cauldron["levels"], ("HornedSkullCauldronBlock", "FLOATERS"): cauldron["floaters"],
                ("HornedSkullCauldronBlock", "POTION_LIGHT"): cauldron["potion_light"], ("HornedSkullCauldronBlock", "HEAT_LIGHT"): cauldron["heat_light"],
                ("HornedSkullCauldronBlock", "WAFT_PLAYERS"): cauldron["waft_players"], ("HornedSkullCauldronBlock", "WAFT_RANGE"): cauldron["waft_range"],
                ("HornedSkullCauldronBlock", "WAFT_FRACTION"): cauldron["waft_fraction"], ("EmberBedBlock", "LIGHT"): decor17.EMBER_BED["light"],
                ("Candelabra", "DRIP_STAGES"): decor17.DRIPS["drip_stages"], ("Candelabra", "DRIP_CHANCE"): decor17.DRIPS["drip_chance"],
                ("Candelabra", "CANDLE_WIDTH"): decor17.CANDLE_WIDTH, ("EnchantedBroomBlock", "RANGE"): broom["range"],
                ("EnchantedBroomBlock", "SWEEP_TICKS"): broom["sweep_ticks"], ("EnchantedBroomBlock", "MAX_MOVES"): broom["max_moves"],
                ("EnchantedBroomBlock", "CHARGE_TICKS"): broom["charge_ticks"], ("EnchantedBroomBlock", "PAN_REACH"): broom["pan_reach"],
                ("EnchantedBroomBlock", "PUSH_SPEED"): broom["push_speed"], ("DustpanBlockEntity", "SLOTS"): decor17.DUSTPAN["slots"],
                ("BroomRackBlock", "PEGS"): decor17.BROOM_RACK["pegs"], ("CuriosityCabinetBlock", "PLACES"): decor17.CABINET["slots"],
                ("CuriosityCabinetBlock", "DOOR_TICKS"): decor17.CABINET["door_ticks"], ("BellJarBlock", "TURN_TICKS"): decor17.BELL_JAR["turn_ticks"],
                ("OddityJarBlock", "WATCH_RANGE"): decor17.JARS["jar_of_eyeballs"]["range"],
                ("BeatingHeartJarBlock", "PULSE_TICKS"): decor17.JARS["beating_heart_jar"]["pulse_ticks"],
                ("BatJarBlock", "WAKE_RANGE"): decor17.JARS["bat_in_a_jar"]["range"],
                ("BatJarBlock", "FLUTTER_TICKS"): decor17.JARS["bat_in_a_jar"]["flutter_ticks"]}
    for (source, name), value in expected.items():
        found = number(source, name)
        if found is None or abs(found - value) > 1e-9:
            err(f"{source}.{name} = {found} differs from tools/decor17.py ({value})")
    tempos = re.search(r"TEMPOS = \{([^}]*)\}", java.get("BeatingHeartJarBlock", ""))
    if not tempos or [int(t) for t in tempos.group(1).split(",")] != decor17.JARS["beating_heart_jar"]["tempos"]:
        err("BeatingHeartJarBlock.TEMPOS differs from tools/decor17.py")
    if (load(ROOT / "src" / "main" / "resources" / "jugcraft" / "candelabra.json") or {}) != decor17.layout():
        err("src/main/resources/jugcraft/candelabra.json differs from tools/decor17.py CANDELABRA (regenerate)")
    candelabra = java.get("Candelabra", "")
    for wax, info in decor17.WAXES.items():
        if f"{wax.upper()}(0x{''.join(f'{c:02X}' for c in info['rgb'])})" not in candelabra:
            err(f"Candelabra.Wax.{wax.upper()} must be the colour of tools/decor17.py WAXES")
    for flame, info in decor17.FLAMES.items():
        if f"{flame.upper()}(0x{''.join(f'{c:02X}' for c in info['rgb'])})" not in candelabra:
            err(f"Candelabra.Flame.{flame.upper()} must be the colour of tools/decor17.py FLAMES")
    main = java.get("JugcraftAgriculture", "")
    if "registerWitchsWorkshop();" not in main:
        err("JugcraftAgriculture.java must call registerWitchsWorkshop()")
    for kind, info in decor17.CANDELABRA.items():
        if f"Candelabra.light(state, {info['light']})" not in main:
            err(f"JugcraftAgriculture.java must light {kind} at {info['light']}")
    floor_kind = 'KIND = "floor_candelabrum"' in java.get("FloorCandelabrumBlock", "") and "FloorCandelabrumBlock.KIND" in main
    for block in decor17.blocks():
        if f'"{block}"' not in main and not (block == "floor_candelabrum" and floor_kind):
            err(f"JugcraftAgriculture.java does not register {block}")
        if f"block.{MOD}.{block}" not in lang:
            err(f"{block} has no name")
        if not (DATA / MOD / "loot_table" / "blocks" / f"{block}.json").is_file():
            err(f"{block} has no loot table")
        if block in decor17.big_jars():
            continue  # drawn on its small jar's textures (check_bigger_jars)
        texture = "candelabra" if block in decor17.CANDELABRA else block
        with Image.open(ASSETS / "textures" / "block" / f"{texture}.png") as img:
            if img.size not in ((64, 64), (128, 128)):
                err(f"textures/block/{texture}.png is {img.size}, not 64 x 64 or 128 x 128")
    if f"item.{MOD}.{decor17.LADLE['item']}" not in lang or f'"{decor17.LADLE["item"]}"' not in main:
        err("The Brew Ladle must be registered and named")
    for recipe in decor17.SHAPED + decor17.SHAPELESS:
        if not (DATA / MOD / "recipe" / f"{recipe['id']}.json").is_file():
            err(f"The {recipe['id']} recipe is missing")
    heat = (load(DATA / MOD / "tags" / "block" / "heat_sources.json") or {}).get("values", [])
    if f"{MOD}:{decor17.EMBER_BED['block']}" not in heat:
        err("The Ember Bed must be in the block tag jugcraft:heat_sources")
    for tag, values in (("brooms", decor17.BROOMS), ("cauldron_floaters", decor17.FLOATERS)):
        found = (load(DATA / MOD / "tags" / "item" / f"{tag}.json") or {}).get("values", [])
        if found != values:
            err(f"The item tag jugcraft:{tag} must list tools/decor17.py's")
    quads = load(ASSETS / "decor17_quads.json") or {}
    for model in ("enchanted_broom", "enchanted_broom_glow", "curiosity_cabinet_door_left", "curiosity_cabinet_door_right_pane", "oddity_eyeball",
                  "oddity_heart", "oddity_bat_body", "oddity_snake_head", "oddity_hand_finger",
                  *[f"moth_{m}_{part}" for m in decor17.MOTH_CASE["moths"] for part in ("body", "wing_left", "wing_right")]):
        if not quads.get(model):
            err(f"decor17_quads.json has no {model}")
    client = ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client"
    if 'Jugcraft.id("decor17_quads.json")' not in (client / "DecorQuads.java").read_text(encoding="utf-8"):
        err("DecorQuads.java must read decor17_quads.json")
    registered = (client / "JugcraftClient.java").read_text(encoding="utf-8")
    for entity, renderer in (("HORNED_SKULL_CAULDRON_ENTITY", "HornedSkullCauldronRenderer"), ("CANDELABRUM_ENTITY", "CandelabrumRenderer"),
                             ("ENCHANTED_BROOM_ENTITY", "EnchantedBroomRenderer"), ("DUSTPAN_ENTITY", "DustpanRenderer"),
                             ("SHOWCASE_ENTITY", "ShowcaseRenderer"), ("MOTH_CASE_ENTITY", "MothCaseRenderer"), ("ODDITY_JAR_ENTITY", "OddityJarRenderer")):
        if f"{entity}, {renderer}::new" not in registered:
            err(f"JugcraftClient.java must draw {entity} with {renderer}")
    for name in ("witchs_workshop_brew", "witchs_workshop_fume", "witchs_workshop_wax", "witchs_workshop_flame", "witchs_workshop_glow"):
        if not (ASSETS / "textures" / "entity" / f"{name}.png").is_file():
            err(f"textures/entity/{name}.png is missing")
    # The bat stays inside its jar: the renderer's numbers are tools/decor17.py's, and its wings never reach the glass.
    bat = decor17.JARS["bat_in_a_jar"]
    jar_renderer = (client / "OddityJarRenderer.java").read_text(encoding="utf-8")
    for name, key in (("BAT_SCALE", "scale"), ("BAT_ORBIT", "orbit"), ("BAT_FLY_Y", "fly_y"), ("BAT_BOB", "bob"), ("BAT_HANG_Y", "hang_y")):
        found = re.search(rf"\b{name} = (-?[\d.]+)F;", jar_renderer)
        if not found or abs(float(found.group(1)) - bat[key]) > 1e-9:
            err(f"OddityJarRenderer.{name} differs from tools/decor17.py JARS bat_in_a_jar {key}")
    reach, low, high = bat_reach(quads, bat)
    inside = 8 - bat["jar"]["glass"] - bat["margin"]
    if not quads.get("oddity_bat_wing_left") or reach > inside or low < 1.0 + bat["margin"] or high > bat["jar"]["glass_top"] - bat["margin"]:
        err(f"The Bat in a Jar's wings leave its glass: they reach {reach:.2f} across (at most {inside:.2f}) and {low:.2f} to {high:.2f} up")
    check_bigger_jars(java, quads, client)
    # The skull's glow lies exactly on its square sockets.
    cauldron_renderer = (client / "HornedSkullCauldronRenderer.java").read_text(encoding="utf-8")
    eyes = re.search(r"\bEYES = (\{.*?\});", cauldron_renderer, re.S)
    found = [float(v.rstrip("F")) for v in re.findall(r"-?[\d.]+F?", eyes.group(1))] if eyes else None
    if found != [v for eye in cauldron["skull_eyes"] for v in eye]:
        err("HornedSkullCauldronRenderer.EYES differs from tools/decor17.py CAULDRON skull_eyes")
    eye_z = re.search(r"\bEYE_Z = (-?[\d.]+)F;", cauldron_renderer)
    if not eye_z or abs(float(eye_z.group(1)) - cauldron["eye_z"]) > 1e-9 or cauldron["eye_z"] > cauldron["skull_face_z"] - 0.1 + 1e-9:
        err("HornedSkullCauldronRenderer.EYE_Z must be tools/decor17.py CAULDRON eye_z, at least 0.1 in front of the skull's face")
    # The Colossal Skull's night glow lies on its sockets, in front of the hollow behind them (behind it, it never shows).
    skull = decor18.SKULL
    skull_renderer = (client / "ColossalSkullRenderer.java").read_text(encoding="utf-8")
    sockets = re.search(r"\bSOCKETS = (\{.*?\});", skull_renderer, re.S)
    found = [float(v.rstrip("F")) for v in re.findall(r"-?[\d.]+F?", sockets.group(1))] if sockets else None
    socket_z = re.search(r"\bSOCKET_Z = (-?[\d.]+)F;", skull_renderer)
    if found != [v for socket in skull["sockets"] for v in socket]:
        err("ColossalSkullRenderer.SOCKETS differs from tools/decor18.py SKULL sockets")
    if not socket_z or abs(float(socket_z.group(1)) - skull["glow_z"]) > 1e-9 or skull["glow_z"] > skull["hollow_z"] - 0.1 + 1e-9:
        err("ColossalSkullRenderer.SOCKET_Z must be tools/decor18.py SKULL glow_z, at least 0.1 in front of the hollow behind the sockets")


def check_graveyard_flora(java, number, lang):
    """The graveyard flora: the mandrake's scream matches tools/agriculture.py MANDRAKE; every sculpted model turns its
    elements only as block models may (one axis, 22.5 or 45 degrees) and stays within -16..32; each flora plant's texture
    is the 64 x 64 one its models draw on; vanilla-biome patches have their placed features; the flying ointment takes a
    mandrake root."""
    md = ag.MANDRAKE
    expected = {"SCREAM_RADIUS": md["scream_radius"], "NAUSEA_SECONDS": md["nausea_seconds"]}
    for name, value in expected.items():
        found = number("Mandrakes", name)
        if found is None or abs(float(found) - value) > 1e-9:
            err(f"Mandrakes.{name} = {found} differs from tools/agriculture.py MANDRAKE ({value})")
    source = java.get("Mandrakes", "")
    for key in ("crop", "wild", "root", "advancement"):
        if f'"{md[key]}"' not in source:
            err(f"Mandrakes.java does not name the mandrake's {key} ({md[key]})")
    if "Mandrakes.register()" not in java.get("JugcraftAgriculture", ""):
        err("JugcraftAgriculture.java must register the mandrake's scream")
    if f"jugcraft:{md['root']}" not in ag.HEX["brews"]["flying"]["ingredients"]:
        err("Flying Ointment must take a mandrake root (tools/agriculture.py HEX)")
    sculpted = [p for p in plants.flora()] + ["mandrake"]
    models = ASSETS / "models" / "block"
    for name in sculpted:
        texture = ASSETS / "textures" / "block" / f"{name}.png"
        if not texture.is_file():
            err(f"The graveyard flora's {name} has no texture")
        else:
            with Image.open(texture) as img:
                if img.size != (64, 64):
                    err(f"textures/block/{name}.png is {img.size}, not 64 x 64")
    for path in sorted(models.glob("*.json")):
        model = load(path) or {}
        if model.get("textures", {}).get("p", "").split("/")[-1] not in sculpted:
            continue
        for e in model.get("elements", []):
            for c in e["from"] + e["to"]:
                if not -16 <= c <= 32:
                    err(f"{path.name}: an element reaches {c}, outside -16..32")
                    break
            r = e.get("rotation")
            if r and (r.get("axis") not in ("x", "y", "z") or r.get("angle") not in (-45, -22.5, 22.5, 45)):
                err(f"{path.name}: an element turns {r}, which block models cannot")
    for plant, info in plants.PLANTS.items():
        if "patch" in info and not (DATA / MOD / "worldgen" / "placed_feature" / f"patch_{plant}.json").is_file():
            err(f"{plant} has a vanilla-biome patch but no placed feature patch_{plant}")
        if info["kind"] == "grass" and plants.PLANTS.get(info.get("tall"), {}).get("kind") != "tall_grass":
            err(f"{plant}: bone meal grows it into {info.get('tall')}, which is not a tall_grass plant")
    for key in [f"advancements.{MOD}.{md['advancement']}.title", f"item.{MOD}.{md['root']}", f"block.{MOD}.{md['wild']}",
                f"block.{MOD}.{md['crop']}"]:
        if key not in lang:
            err(f"The graveyard flora has no words for {key}")


def check_midway(java, number, lang):
    """Fall addition 26: the High Striker, Midway, Ring Toss and the plushes match tools/midway.py (levels, timing,
    strength, the ringer's reach and distance, each plush's footprint); the blocks, items, block entity and thrown ring
    are registered and drawn; the strike is wired to left clicks; their words, loot (the prize table's plushes and
    weights), models, recipes and advancements exist."""
    hs, rt = midway.HIGH_STRIKER, midway.RING_TOSS
    expected = {("HighStrikerBlock", "LEVELS"): hs["levels"], ("HighStrikerBlock", "RISE_TICKS"): hs["rise_ticks"],
                ("HighStrikerBlock", "HOLD_TICKS"): hs["hold_ticks"], ("HighStrikerBlock", "LIGHT"): hs["light"],
                ("Midway", "FULL_LOW"): hs["full_low"], ("Midway", "FULL_HIGH"): hs["full_high"], ("Midway", "CRIT_BONUS"): hs["crit_bonus"],
                ("Midway", "RING_AT"): hs["ring_at"], ("RingTossBlock", "RINGER_RADIUS"): rt["ringer_radius"],
                ("RingTossBlock", "MIN_DISTANCE"): rt["min_distance"], ("RingTossBlock", "RINGER_TICKS"): rt["ringer_ticks"],
                ("RingTossBlock", "TOP"): rt["top"], ("TossRingItem", "SPEED"): rt["ring_speed"]}
    for (source, name), value in expected.items():
        found = number(source, name)
        if found is None or abs(float(found) - value) > 1e-9:
            err(f"{source}.{name} = {found} differs from tools/midway.py ({value})")
    necks = ", ".join(str(n) for n in rt["necks"])
    if f"NECKS = {{{necks}}}" not in java.get("RingTossBlock", ""):
        err("RingTossBlock.NECKS differs from RING_TOSS['necks']")
    mid = java.get("Midway", "")
    for name, plush in midway.PLUSHES.items():
        x0, z0, x1, z1, h = plush["shape"]
        if f'new Plush("{name}", {x0}, {z0}, {x1}, {z1}, {h})' not in mid:
            err(f"Midway.PLUSHES must have {name} with the footprint tools/midway.py gives")
    if 'AttackBlockCallback.EVENT.register' not in mid or 'player.resetAttackStrengthTicker()' not in mid:
        err("Midway must strike the High Striker from left clicks and spend the swing's charge")
    main = java.get("JugcraftAgriculture", "")
    for call in ('registerBlock("high_striker", HighStrikerBlock::new', "HighStrikerBlockEntity::new, striker",
                 f"props.sword(ToolMaterial.WOOD, {hs['mallet_damage']}F, {hs['mallet_speed']}F)",
                 'registerBlock("ring_toss", RingTossBlock::new', "registerItem(TossRingItem.ID, TossRingItem::new",
                 "EntityType.Builder.<TossRing>of(TossRing::new", "new PlushBlock(props, plush)", "Midway.register();"):
        if call not in main:
            err(f"JugcraftAgriculture.java must call {call}")
    client = (ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client" / "JugcraftClient.java").read_text(encoding="utf-8")
    if "JugcraftAgriculture.TOSS_RING, ThrownItemRenderer::new" not in client:
        err("JugcraftClient.java must draw the thrown Toss Ring")
    for item, display in midway.displays().items():
        kind = "item" if item in (hs["mallet"], rt["ring"]) else "block"
        if lang.get(f"{kind}.{MOD}.{item}") != display:
            err(f"The midway has no words for {kind}.{MOD}.{item}")
    prizes = load(DATA / MOD / "loot_table" / f"{midway.PRIZE_TABLE}.json") or {}
    entries = {e.get("name"): e.get("weight") for pool in prizes.get("pools", []) for e in pool.get("entries", [])}
    if entries != {f"{MOD}:{name}": plush["weight"] for name, plush in midway.PLUSHES.items()}:
        err("The midway's prize table must hold each plush at its weight")
    striker_loot = json.dumps(load(DATA / MOD / "loot_table" / "blocks" / f"{hs['block']}.json") or {})
    if '"part": "0"' not in striker_loot:
        err("The High Striker must drop only from its base")
    for path in [DATA / MOD / "advancement" / f"{a}.json" for a in midway.ADVANCEMENTS] + [
            DATA / MOD / "recipe" / f"{r['id']}.json" for r in midway.SHAPED + midway.SHAPELESS] + [
            ASSETS / "blockstates" / f"{b}.json" for b in midway.blocks()] + [
            ASSETS / "models" / "block" / f"{m}.json" for m in ("high_striker_base", "high_striker_4_rung", "ring_toss", "ring_toss_ringed_9")]:
        if not path.exists():
            err(f"The midway needs {path.relative_to(ROOT)}")


def check_ferris_wheel(java, number, lang):
    """Fall addition 27: the Ferris wheel matches tools/ferris_wheel.py (its size, its cars and seats, its power and
    speed, how far a player may board from); the booth, its block entity (a kinetic consumer) and the wheel are
    registered and drawn from the quads; its words, loot, recipe, advancements, models and quads exist."""
    fw = ferris_wheel.FERRIS_WHEEL
    expected = {("FerrisWheel", "CARS"): fw["cars"], ("FerrisWheel", "SEATS"): fw["seats"], ("FerrisWheel", "HUB"): fw["hub"],
                ("FerrisWheel", "RADIUS"): fw["radius"], ("FerrisWheel", "SEAT_ACROSS"): fw["seat_across"],
                ("FerrisWheel", "SEAT_DOWN"): fw["seat_down"], ("FerrisWheel", "SEAT_BACK"): fw["seat_back"],
                ("FerrisWheel", "NEED"): fw["need"], ("FerrisWheel", "TURN_TICKS"): fw["turn_ticks"],
                ("FerrisWheel", "BOARD_REACH"): fw["board_reach"], ("FerrisWheelBlock", "WIDTH"): fw["width"],
                ("FerrisWheelBlock", "HEIGHT"): fw["height"], ("FerrisWheelBlock", "DEPTH"): fw["depth"]}
    for (source, name), value in expected.items():
        found = number(source, name)
        if found is None or abs(float(found) - value) > 1e-9:
            err(f"{source}.{name} = {found} differs from tools/ferris_wheel.py ({value})")
    wheel = java.get("FerrisWheel", "")
    for rate, share in (("ACCEL", fw["accel"]), ("DECEL", fw["decel"])):
        if f"{rate} = FULL_SPEED / {round(1 / share)}.0F" not in wheel:
            err(f"FerrisWheel.{rate} must be full speed / {round(1 / share)} (tools/ferris_wheel.py)")
    if "implements KineticConsumer" not in java.get("FerrisWheelBlockEntity", ""):
        err("The Ferris Wheel's booth must take power through the shared KineticConsumer interface")
    main = java.get("JugcraftAgriculture", "")
    for call in ("registerBlock(FerrisWheelBlock.ID, FerrisWheelBlock::new", "FerrisWheelBlockEntity::new, booth",
                 "EntityType.Builder.<FerrisWheel>of(FerrisWheel::new"):
        if call not in main:
            err(f"JugcraftAgriculture.java must call {call}")
    client_dir = ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client"
    if "JugcraftAgriculture.FERRIS_WHEEL, FerrisWheelRenderer::new" not in (client_dir / "JugcraftClient.java").read_text(encoding="utf-8"):
        err("JugcraftClient.java must draw the Ferris wheel")
    if '"ferris_wheel_quads.json"' not in (client_dir / "DecorQuads.java").read_text(encoding="utf-8"):
        err("DecorQuads must read ferris_wheel_quads.json")
    quads = load(ASSETS / "ferris_wheel_quads.json") or {}
    for name in ["ferris_wheel_frame", "ferris_wheel_section", "ferris_wheel_hub", "ferris_wheel_pivot", "ferris_wheel_lights"] + [
            f"ferris_wheel_car_{c}" for c in ferris_wheel.CAR_COLOURS]:
        if not quads.get(name):
            err(f"ferris_wheel_quads.json has no quads for {name}")
        for quad in quads.get(name, []):
            if not (ASSETS / "textures" / "block" / f"{quad['texture']}.png").exists():
                err(f"ferris_wheel_quads.json's {name} draws missing texture {quad['texture']}")
                break
    if lang.get(f"block.{MOD}.{fw['block']}") != fw["display"] or lang.get(f"entity.{MOD}.{fw['entity']}") != fw["entity_display"]:
        err("The Ferris wheel has no words")
    for key in ("no_room", "full", "jammed"):
        if f"message.{MOD}.ferris_wheel.{key}" not in lang:
            err(f"The Ferris wheel has no words for message.{MOD}.ferris_wheel.{key}")
    for path in [DATA / MOD / "advancement" / f"{a}.json" for a in ferris_wheel.ADVANCEMENTS] + [
            DATA / MOD / "recipe" / f"{r['id']}.json" for r in ferris_wheel.SHAPED] + [
            DATA / MOD / "loot_table" / "blocks" / f"{fw['block']}.json", ASSETS / "blockstates" / f"{fw['block']}.json",
            ASSETS / "models" / "block" / f"{fw['block']}.json", ASSETS / "textures" / "item" / f"{fw['block']}.png"]:
        if not path.exists():
            err(f"The Ferris wheel needs {path.relative_to(ROOT)}")


def check_pinata(java, number, lang):
    """Fall addition 28: the piñatas match tools/pinata.py (slots, drop, a charged swing, the stick's hits, each kind's
    hits and item); the items, entity and Blindfold are registered and drawn from the quads; the Blindfold's view and
    worn band exist; their words, recipes and advancements exist."""
    pn = pinata.PINATA
    expected = {("Pinata", "SLOTS"): pn["slots"], ("Pinata", "DROP"): pn["drop"], ("Pinata", "CHARGED"): pn["charged"],
                ("Pinata", "STICK_HITS"): pn["stick_hits"]}
    for (source, name), value in expected.items():
        found = number(source, name)
        if found is None or abs(float(found) - value) > 1e-9:
            err(f"{source}.{name} = {found} differs from tools/pinata.py ({value})")
    source = java.get("Pinata", "")
    for kind, spec in pinata.KINDS.items():
        if f'("{kind}", "{spec["item"]}", {spec["hits"]},' not in source:
            err(f"Pinata.Kind must have {kind} with item {spec['item']} and {spec['hits']} hits (tools/pinata.py)")
    pinatas = java.get("Pinatas", "")
    if f'STICK = "{pn["stick"]}"' not in pinatas or f'BLINDFOLD = "{pn["blindfold"]}"' not in pinatas:
        err("Pinatas.STICK and BLINDFOLD must match tools/pinata.py")
    main = java.get("JugcraftAgriculture", "")
    for call in ("new PinataItem(props, kind)", "registerItem(Pinatas.STICK", 'setCameraOverlay(Jugcraft.id("misc/blindfold"))',
                 "EntityType.Builder.<Pinata>of(Pinata::new", "Pinatas.register();"):
        if call not in main:
            err(f"JugcraftAgriculture.java must call {call}")
    client_dir = ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client"
    if "JugcraftAgriculture.PINATA, PinataRenderer::new" not in (client_dir / "JugcraftClient.java").read_text(encoding="utf-8"):
        err("JugcraftClient.java must draw the piñatas")
    if '"pinata_quads.json"' not in (client_dir / "DecorQuads.java").read_text(encoding="utf-8"):
        err("DecorQuads must read pinata_quads.json")
    quads = load(ASSETS / "pinata_quads.json") or {}
    for name in [f"pinata_{k}{t}" for k in pinata.KINDS for t in ("", "_torn")] + ["pinata_rope", "pinata_rope_star"]:
        if not quads.get(name):
            err(f"pinata_quads.json has no quads for {name}")
        for quad in quads.get(name, []):
            if not (ASSETS / "textures" / "block" / f"{quad['texture']}.png").exists():
                err(f"pinata_quads.json's {name} draws missing texture {quad['texture']}")
                break
    for item, display in pinata.displays().items():
        if lang.get(f"item.{MOD}.{item}") != display:
            err(f"The piñata party has no words for item.{MOD}.{item}")
    for key in ("full", "no_room", "filled"):
        if f"message.{MOD}.pinata.{key}" not in lang:
            err(f"The piñata party has no words for message.{MOD}.pinata.{key}")
    for path in [DATA / MOD / "advancement" / f"{a}.json" for a in pinata.ADVANCEMENTS] + [
            DATA / MOD / "recipe" / f"{r['id']}.json" for r in pinata.SHAPED + pinata.SHAPELESS] + [
            ASSETS / "textures" / "misc" / "blindfold.png", ASSETS / "textures" / "entity" / "equipment" / "humanoid" / "blindfold.png",
            ASSETS / "equipment" / "blindfold.json"] + [ASSETS / "items" / f"{i}.json" for i in pinata.items()]:
        if not path.exists():
            err(f"The piñata party needs {path.relative_to(ROOT)}")


def check_hot_air_balloon(java, number, lang):
    """Fall addition 29: the balloons, winds, pibals and mooring posts match tools/hot_air_balloon.py; the items,
    entities, block, payload and renderers are registered; the quads draw from textures that exist (the envelopes'
    wraps 768 by 384); their words, recipes, advancements, loot and tags exist."""
    hb, wd, mo, pb, rules = (hot_air_balloon.HOT_AIR_BALLOON, hot_air_balloon.WINDS, hot_air_balloon.MOORING, hot_air_balloon.PIBAL,
                             hot_air_balloon.ADVANCEMENT_RULES)
    expected = {("HotAirBalloon", "RIDERS"): hb["riders"], ("HotAirBalloon", "BASKET"): hb["basket"],
                ("HotAirBalloon", "BASKET_HEIGHT"): hb["basket_height"], ("HotAirBalloon", "BURNER"): hb["burner"],
                ("HotAirBalloon", "THROAT"): hb["throat"], ("HotAirBalloon", "ENVELOPE_HEIGHT"): hb["envelope_height"],
                ("HotAirBalloon", "ENVELOPE_RADIUS"): hb["envelope_radius"], ("HotAirBalloon", "FIRE"): hb["fire"],
                ("HotAirBalloon", "COOL"): hb["cool"], ("HotAirBalloon", "VENT"): hb["vent"], ("HotAirBalloon", "NEUTRAL"): hb["neutral"],
                ("HotAirBalloon", "CLIMB"): hb["climb"], ("HotAirBalloon", "MAX_CLIMB"): hb["max_climb"],
                ("HotAirBalloon", "MAX_SINK"): hb["max_sink"], ("HotAirBalloon", "THIN"): hb["thin"],
                ("HotAirBalloon", "RESPONSE"): hb["response"], ("HotAirBalloon", "DRIFT"): hb["drift"],
                ("HotAirBalloon", "FUEL_PER_BURN_TICK"): hb["fuel_per_burn_tick"], ("HotAirBalloon", "MAX_FUEL"): hb["max_fuel"],
                ("HotAirBalloon", "GAUGE_TICKS"): hb["gauge_ticks"], ("HotAirBalloon", "MOOR_REACH"): mo["reach"],
                ("HotAirBalloon", "ROPE"): mo["rope"], ("HotAirBalloon", "TETHER_HEIGHT"): mo["height"],
                ("HotAirBalloon", "UP_HEIGHT"): rules["up_height"], ("HotAirBalloon", "BOX_HOME"): rules["box_home"],
                ("HotAirBalloon", "BOX_AWAY"): rules["box_away"], ("HotAirBalloon", "ALOFT"): rules["aloft"],
                ("HotAirBalloon", "CROWD"): rules["crowd"], ("HotAirBalloon", "CROWD_RANGE"): rules["crowd_range"],
                ("FiestaWinds", "LAYER"): wd["layer"], ("FiestaWinds", "LAYERS"): wd["layers"], ("FiestaWinds", "BASE"): wd["base"],
                ("FiestaWinds", "PER_LAYER"): wd["per_layer"], ("FiestaWinds", "BOX_SPREAD"): wd["box_spread"],
                ("FiestaWinds", "SWAY"): wd["sway"], ("FiestaWinds", "STORM"): wd["storm"],
                ("Pibal", "RISE"): pb["rise"], ("Pibal", "LIFE"): pb["life"]}
    for (source, name), value in expected.items():
        found = number(source, name)
        if found is None or abs(float(found) - value) > 1e-9:
            err(f"{source}.{name} = {found} differs from tools/hot_air_balloon.py ({value})")
    source = java.get("HotAirBalloon", "")
    for kind, spec in hot_air_balloon.KINDS.items():
        if f'("{kind}", "{spec["item"]}")' not in source:
            err(f"HotAirBalloon.Kind must have {kind} with item {spec['item']} (tools/hot_air_balloon.py)")
    if f'ID = "{mo["block"]}"' not in java.get("MooringPostBlock", ""):
        err("MooringPostBlock.ID must match tools/hot_air_balloon.py")
    main = java.get("JugcraftAgriculture", "")
    for call in ("new HotAirBalloonItem(props, kind)", 'registerItem("balloon_burner"', 'registerItem("pibal", PibalItem::new',
                 "registerBlock(MooringPostBlock.ID", "EntityType.Builder.<HotAirBalloon>of(HotAirBalloon::new",
                 "EntityType.Builder.<Pibal>of(Pibal::new", "Balloons.register();"):
        if call not in main:
            err(f"JugcraftAgriculture.java must call {call}")
    if "BalloonControlPayload.register();" not in java.get("Balloons", ""):
        err("Balloons must register the pilot's control payload")
    client_dir = ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client"
    client = (client_dir / "JugcraftClient.java").read_text(encoding="utf-8")
    for call in ("JugcraftAgriculture.HOT_AIR_BALLOON, HotAirBalloonRenderer::new", "JugcraftAgriculture.PIBAL, PibalRenderer::new",
                 "BalloonClient.register();"):
        if call not in client:
            err(f"JugcraftClient.java must call {call}")
    if '"balloon_quads.json"' not in (client_dir / "DecorQuads.java").read_text(encoding="utf-8"):
        err("DecorQuads must read balloon_quads.json")
    quads = load(ASSETS / "balloon_quads.json") or {}
    names = ["balloon_basket", "balloon_flame", "balloon_glow_pumpkin", "balloon_mooring_rope", "pibal"] + [
        f"balloon_envelope_{k}" for k in hot_air_balloon.KINDS]
    for name in names:
        if not quads.get(name):
            err(f"balloon_quads.json has no quads for {name}")
        for quad in quads.get(name, []):
            texture = quad["texture"]
            path = ASSETS / "textures" / (f"{texture}.png" if "/" in texture else f"block/{texture}.png")
            if not path.exists():
                err(f"balloon_quads.json's {name} draws missing texture {texture}")
                break
    for kind in list(hot_air_balloon.KINDS) + ["glow_pumpkin"]:
        name = f"envelope_{kind}" if kind in hot_air_balloon.KINDS else kind
        path = ASSETS / "textures" / "entity" / "hot_air_balloon" / f"{name}.png"
        if path.exists() and Image.open(path).size != (768, 384):
            err(f"{path.relative_to(ROOT)} must be 768 by 384 (24 gores of 32 pixels)")
    for item, display in hot_air_balloon.displays().items():
        key = f"block.{MOD}.{item}" if item in hot_air_balloon.blocks() else f"item.{MOD}.{item}"
        if lang.get(key) != display:
            err(f"The hot-air balloon fiesta has no words for {key}")
    for key in ("gauges", "moored", "controls", "fuelled", "fuel_full", "no_fuel", "no_room", "full"):
        if f"message.{MOD}.balloon.{key}" not in lang:
            err(f"The hot-air balloon fiesta has no words for message.{MOD}.balloon.{key}")
    for key in ("tied", "untied", "none"):
        if f"message.{MOD}.mooring.{key}" not in lang:
            err(f"The hot-air balloon fiesta has no words for message.{MOD}.mooring.{key}")
    for path in [DATA / MOD / "advancement" / f"{a}.json" for a in hot_air_balloon.ADVANCEMENTS] + [
            DATA / MOD / "recipe" / f"{r['id']}.json" for r in hot_air_balloon.SHAPED + hot_air_balloon.SHAPELESS] + [
            DATA / MOD / "loot_table" / "blocks" / f"{mo['block']}.json", ASSETS / "blockstates" / f"{mo['block']}.json"] + [
            ASSETS / "items" / f"{i}.json" for i in hot_air_balloon.items()]:
        if not path.exists():
            err(f"The hot-air balloon fiesta needs {path.relative_to(ROOT)}")


def check_leaf_blower(java, number, lang):
    """Fall addition 30: the Leaf Blower matches tools/leaf_blower.py (charge, costs, stream); it is a Chargeable item
    that starts empty, so the Charging Station charges it; its model draws from textures that exist; its words, recipe
    and advancement exist."""
    lb = leaf_blower.LEAF_BLOWER
    expected = {"CAPACITY": lb["capacity"], "BLOW_JE": lb["blow_je"], "VACUUM_JE": lb["vacuum_je"], "RANGE": lb["range"],
                "CONE": lb["cone"], "PUSH_ITEMS": lb["push_items"], "PUSH_MOBS": lb["push_mobs"], "PILE_RANGE": lb["pile_range"],
                "PILE_EVERY": lb["pile_every"], "VACUUM_RANGE": lb["vacuum_range"]}
    for name, value in expected.items():
        found = number("LeafBlowerItem", name)
        if found is None or abs(float(found) - value) > 1e-9:
            err(f"LeafBlowerItem.{name} = {found} differs from tools/leaf_blower.py ({value})")
    source = java.get("LeafBlowerItem", "")
    if f'ID = "{lb["item"]}"' not in source or "implements Chargeable" not in source:
        err("LeafBlowerItem must be the Chargeable item tools/leaf_blower.py names")
    if "registerItem(LeafBlowerItem.ID, LeafBlowerItem::new" not in java.get("JugcraftAgriculture", "") or \
            "component(JugcraftTools.ENERGY, 0L)" not in java.get("JugcraftAgriculture", ""):
        err("JugcraftAgriculture.java must register the Leaf Blower with an empty charge")
    model = load(ASSETS / "models" / "item" / f"{lb['item']}.json") or {}
    if not model.get("elements"):
        err(f"The Leaf Blower needs its model, models/item/{lb['item']}.json")
    for name, texture in model.get("textures", {}).items():
        path = ASSETS / "textures" / f"{texture.split(':', 1)[1]}.png"
        if not path.exists():
            err(f"The Leaf Blower's model draws missing texture {texture}")
    if lang.get(f"item.{MOD}.{lb['item']}") != lb["display"]:
        err(f"The Leaf Blower has no words for item.{MOD}.{lb['item']}")
    for key in (f"item.{MOD}.{lb['item']}.tooltip", f"message.{MOD}.leaf_blower.flat"):
        if key not in lang:
            err(f"The Leaf Blower has no words for {key}")
    for path in [DATA / MOD / "advancement" / f"{a}.json" for a in leaf_blower.ADVANCEMENTS] + [
            DATA / MOD / "recipe" / f"{r['id']}.json" for r in leaf_blower.SHAPED] + [ASSETS / "items" / f"{lb['item']}.json"]:
        if not path.exists():
            err(f"The Leaf Blower needs {path.relative_to(ROOT)}")


def check_pumpkling(java, number, lang):
    """Fall addition 25: Pumpkling matches PUMPKLING in tools/agriculture.py (health, speed, following, healing); it
    is registered and drawn, guards crops as a CropGuard, wakes from a carved pumpkin, and its words, tags and
    advancement exist."""
    pk = ag.PUMPKLING
    expected = {"MAX_HEALTH": pk["health"], "SPEED": pk["speed"], "TEMPT_RANGE": pk["tempt_range"], "FOLLOW_START": pk["follow_start"],
                "FOLLOW_STOP": pk["follow_stop"],
                "TELEPORT_DISTANCE": pk["teleport"], "TREAT_HEAL": pk["treat_heal"]}
    for name, value in expected.items():
        found = number("Pumpkling", name)
        if found is None or abs(float(found) - value) > 1e-9:
            err(f"Pumpkling.{name} = {found} differs from tools/agriculture.py ({value})")
    pumpkling = java.get("Pumpkling", "")
    if "implements CropGuard" not in pumpkling:
        err("Pumpkling must be a CropGuard, so crows keep away from it")
    if "Pumpkling.wake(" not in java.get("CarvedPumpkinBlock", ""):
        err("CarvedPumpkinBlock must wake a Pumpkling when given a spark")
    main = java.get("JugcraftAgriculture", "")
    for call in (f'entity("{pk["entity"]}"', f".sized({pk['size'][0]}F, {pk['size'][1]}F)",
                 "FabricDefaultAttributeRegistry.register(PUMPKLING, Pumpkling.createAttributes())"):
        if call not in main:
            err(f"JugcraftAgriculture.java must call {call}")
    client = (ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client" / "JugcraftClient.java").read_text(encoding="utf-8")
    if "JugcraftAgriculture.PUMPKLING, PumpklingRenderer::new" not in client or "PumpklingModel.LAYER, PumpklingModel::createLayer" not in client:
        err("JugcraftClient.java must register PumpklingRenderer and PumpklingModel's layer")
    if f"entity.{MOD}.{pk['entity']}" not in lang:
        err("The Pumpkling has no name")
    for path in (DATA / MOD / "advancement" / "little_jack.json", ASSETS / "textures" / "entity" / "pumpkling.png"):
        if not path.exists():
            err(f"The Pumpkling needs {path.relative_to(ROOT)}")
    for tag, key in (("pumpkling_sparks", "sparks"), ("pumpkling_treats", "treats")):
        values = (load(DATA / MOD / "tags" / "item" / f"{tag}.json") or {}).get("values", [])
        if sorted(values) != sorted(pk[key]):
            err(f"{MOD}:{tag} differs from PUMPKLING['{key}']")


def check_squirrels(java, number, lang):
    """Fall addition 24: Squirrel and Squirrels match SQUIRRELS in tools/agriculture.py (the squirrel, its gathering and
    burying, spawning, acorns from leaves); the entity, acorn and roasted acorns are registered and drawn; their words,
    tags, recipes, advancement and textures exist."""
    sq = ag.SQUIRRELS
    expected = {("Squirrel", "MAX_HEALTH"): sq["health"], ("Squirrel", "SPEED"): sq["speed"], ("Squirrel", "SEEK_RANGE"): sq["seek_range"],
                ("Squirrel", "BURY_TICKS"): sq["bury_ticks"], ("Squirrel", "SAPLING_CHANCE"): sq["sapling_chance"],
                ("Squirrel", "SAPLING_SPACE"): sq["sapling_space"], ("Squirrels", "SPAWN_TICKS"): sq["spawn_ticks"],
                ("Squirrels", "SPAWN_CHANCE"): sq["spawn_chance"], ("Squirrels", "MIN_DISTANCE"): sq["min_distance"],
                ("Squirrels", "MAX_DISTANCE"): sq["max_distance"], ("Squirrels", "NEAR_CAP"): sq["near_cap"],
                ("Squirrels", "LEVEL_CAP"): sq["level_cap"], ("Squirrels", "ACORN_CHANCE"): sq["acorn_chance"]}
    for (source, name), value in expected.items():
        found = number(source, name)
        if found is None or abs(float(found) - value) > 1e-9:
            err(f"{source}.{name} = {found} differs from tools/agriculture.py ({value})")
    squirrel, squirrels = java.get("Squirrel", ""), java.get("Squirrels", "")
    if f'ACORN = "{sq["acorn"]}"' not in squirrel:
        err("Squirrel.ACORN differs from SQUIRRELS['acorn']")
    leaves = "List.of(" + ", ".join(f'"{l}"' for l in sq["acorn_leaves"]) + ")"
    if f"ACORN_LEAVES = {leaves}" not in squirrels:
        err("Squirrels.ACORN_LEAVES differs from SQUIRRELS['acorn_leaves']")
    main = java.get("JugcraftAgriculture", "")
    for call in ('entity("squirrel"', f".sized({sq['size'][0]}F, {sq['size'][1]}F)", "Squirrels.register();",
                 "registerItem(Squirrel.ACORN, AcornItem::new", f'food("{sq["roasted"]}"'):
        if call not in main:
            err(f"JugcraftAgriculture.java must call {call}")
    client = (ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client" / "JugcraftClient.java").read_text(encoding="utf-8")
    if "JugcraftAgriculture.SQUIRREL, SquirrelRenderer::new" not in client or "SquirrelModel.LAYER, SquirrelModel::createLayer" not in client:
        err("JugcraftClient.java must register SquirrelRenderer and SquirrelModel's layer")
    for key in (f"entity.{MOD}.{sq['entity']}", f"item.{MOD}.{sq['acorn']}", f"item.{MOD}.{sq['roasted']}"):
        if key not in lang:
            err(f"Squirrels have no words for {key}")
    for path in (DATA / MOD / "advancement" / "nuts_about_squirrels.json", DATA / MOD / "recipe" / f"{sq['roasted']}.json",
                 ASSETS / "textures" / "entity" / "squirrel_red.png", ASSETS / "textures" / "entity" / "squirrel_grey.png",
                 ASSETS / "textures" / "item" / f"{sq['acorn']}.png", ASSETS / "textures" / "item" / f"{sq['roasted']}.png"):
        if not path.exists():
            err(f"Squirrels need {path.relative_to(ROOT)}")
    habitat = (load(DATA / MOD / "tags" / "worldgen" / "biome" / "squirrel_habitat.json") or {}).get("values", [])
    if sorted(habitat) != sorted(sq["habitat"]):
        err("jugcraft:squirrel_habitat differs from SQUIRRELS['habitat']")
    food = (load(DATA / MOD / "tags" / "item" / "squirrel_food.json") or {}).get("values", [])
    if sorted(food) != sorted(sq["food"]):
        err("jugcraft:squirrel_food differs from SQUIRRELS['food']")


def check_werewolves(java, number, lang):
    """Fall addition 23: Werewolf and Werewolves match WEREWOLF and WOLFSBANE in tools/agriculture.py (the hide and
    silver, healing, shunning, spawning and the ward, each kind's tier and attributes and abilities, the silver blade);
    every box of tools/werewolf_model.py is in WerewolfModel, which the texture is painted from; the entity, flower,
    potted flower, items, pelts and rugs are registered and drawn; their words, loot, tags, worldgen, recipes and
    advancements exist."""
    ww, wb = ag.WEREWOLF, ag.WOLFSBANE
    expected = {("Werewolf", "HIDE_FACTOR"): ww["hide_factor"], ("Werewolf", "SILVER_FACTOR"): ww["silver_factor"],
                ("Werewolf", "REGEN_TICKS"): ww["regen_ticks"], ("Werewolf", "SILVER_WOUND_TICKS"): ww["silver_wound_ticks"],
                ("Werewolf", "SHUN_TICKS"): ww["shun_ticks"], ("Werewolf", "PACK_REACH"): ww["pack_reach"],
                ("Werewolf", "FLEE_BELOW"): ww["flee_below"], ("Werewolf", "FLEE_UNTIL"): ww["flee_until"],
                ("Werewolf", "FROSTBITE_TICKS"): ww["frostbite_ticks"], ("Werewolf", "FROSTBITE_CHILL"): ww["frostbite_chill"],
                ("Werewolf", "SNOW_STRIDE"): ww["snow_stride"], ("Werewolf", "SHADOW_STEP_MIN"): ww["shadow_step_min"],
                ("Werewolf", "SHADOW_STEP_TICKS"): ww["shadow_step_ticks"], ("Werewolf", "ALPHA_REACH"): ww["alpha_reach"],
                ("Werewolf", "DARKNESS_TICKS"): ww["darkness_ticks"], ("Werewolf", "FRENZY_REACH"): ww["frenzy_reach"],
                ("Werewolf", "FRENZY_TICKS"): ww["frenzy_ticks"], ("Werewolf", "HOWL_COOLDOWN"): ww["howl_cooldown"],
                ("Werewolves", "SPAWN_TICKS"): ww["spawn_ticks"], ("Werewolves", "SPAWN_CHANCE"): ww["spawn_chance"],
                ("Werewolves", "MIN_DISTANCE"): ww["min_distance"], ("Werewolves", "MAX_DISTANCE"): ww["max_distance"],
                ("Werewolves", "NEAR_CAP"): ww["near_cap"], ("Werewolves", "LEVEL_CAP"): ww["level_cap"],
                ("Werewolves", "WARD_REACH"): ww["ward_reach"], ("Werewolves", "STEW_SECONDS"): wb["seconds"],
                ("Werewolves", "SHADOW_CHANCE"): ww["shadow_chance"], ("Werewolves", "SHADOW_HAUNT_CHANCE"): ww["shadow_haunt_chance"]}
    for (source, name), value in expected.items():
        found = number(source, name)
        if found is None or abs(float(found) - value) > 1e-9:
            err(f"{source}.{name} = {found} differs from tools/agriculture.py ({value})")
    werewolf = java.get("Werewolf", "")
    for kind, k in ww["kinds"].items():
        entry = (f'{kind.upper()}("{kind}", {k["tier"]}, {k["health"]}, {k["damage"]}, {k["armor"]}, {k["speed"]}, {k["scale"]}, '
                 f'{k["knockback"]}, {k["xp"]}, "{k["pelt"]}", "{k["rug"]}")')
        if entry not in werewolf:
            err(f"Werewolf.Kind must have {entry}, as WEREWOLF['kinds'] says")
    for attribute in ("MAX_HEALTH", "MOVEMENT_SPEED", "ATTACK_DAMAGE", "ARMOR", "KNOCKBACK_RESISTANCE", "SCALE"):
        if f"Attributes.{attribute}, kind." not in werewolf:
            err(f"Werewolf.setKind must set {attribute} from its kind")
    blade = ww["dagger_material"]
    if (f"INCORRECT_FOR_IRON_TOOL, {blade['durability']}, {blade['speed']}F, {blade['bonus']}F, {blade['enchantability']}," not in werewolf):
        err("Werewolf.SILVER differs from WEREWOLF['dagger_material']")
    for call in ("Werewolves.warded(level, target, kind() != Kind.SHADOW)", "kind() == Kind.SNOW && target instanceof LivingEntity",
                 '"leader_of_the_pack"', "kind().peltTable()", "GameRules.MOB_DROPS"):
        if call not in werewolf:
            err(f"Werewolf.java must have {call}")
    if "werewolf.setKind(kindFor(level, spot, random));" not in java.get("Werewolves", ""):
        err("Werewolves.trySpawn must choose the kind by kindFor")
    main = java.get("JugcraftAgriculture", "")
    for call in ('entity("werewolf"', f".sized({ww['size'][0]}F, {ww['size'][1]}F)", "Werewolves.register();",
                 f"props.sword(Werewolf.SILVER, {blade['damage']}F, {blade['attack_speed']}F)", "registerItem(Werewolves.SILVER_ARROW, ArrowItem::new",
                 "for (Werewolf.Kind kind : Werewolf.Kind.values())", "registerItem(kind.pelt, Item::new", "registerBlock(kind.rug, WerewolfRugBlock::new",
                 f"MobEffects.{wb['effect']}"):
        if call not in main:
            err(f"JugcraftAgriculture.java must call {call}")
    client_root = ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client"
    client = (client_root / "JugcraftClient.java").read_text(encoding="utf-8")
    if "JugcraftAgriculture.WEREWOLF, WerewolfRenderer::new" not in client or "WerewolfModel.LAYER, WerewolfModel::createLayer" not in client:
        err("JugcraftClient.java must register WerewolfRenderer and WerewolfModel's layer")
    renderer = (client_root / "WerewolfRenderer.java").read_text(encoding="utf-8")
    for call in ('"textures/entity/werewolf_" + kind.id + ".png"', '"textures/entity/werewolf_" + kind.id + "_eyes.png"', "state.kind == kind"):
        if call not in renderer:
            err(f"WerewolfRenderer.java must have {call}")
    check_werewolf_model((client_root / "WerewolfModel.java").read_text(encoding="utf-8"))
    for key in [f"entity.{MOD}.werewolf", f"block.{MOD}.{wb['block']}", f"block.{MOD}.potted_{wb['block']}", f"item.{MOD}.{ww['dagger']}",
                f"item.{MOD}.{ww['arrow']}"] + [key for kind, k in ww["kinds"].items() for key in (
                f"entity.{MOD}.werewolf.{kind}", f"block.{MOD}.{k['rug']}", f"item.{MOD}.{k['pelt']}")]:
        if key not in lang:
            err(f"Werewolves have no words for {key}")
    paths = [DATA / MOD / "loot_table" / "entities" / "werewolf.json", DATA / MOD / "loot_table" / "blocks" / f"{wb['block']}.json",
             DATA / MOD / "worldgen" / "placed_feature" / f"patch_{wb['block']}.json", DATA / MOD / "advancement" / "silver_lining.json",
             DATA / MOD / "advancement" / "wolfsbane_ward.json", DATA / MOD / "advancement" / "leader_of_the_pack.json",
             ASSETS / "textures" / "block" / f"{wb['block']}.png", DATA / MOD / "recipe" / f"{ww['dagger']}.json"]
    for kind, k in ww["kinds"].items():
        paths += [DATA / MOD / "loot_table" / "entities" / "werewolf" / f"{kind}.json", DATA / MOD / "loot_table" / "blocks" / f"{k['rug']}.json",
                  ASSETS / "textures" / "entity" / f"werewolf_{kind}.png", ASSETS / "textures" / "entity" / f"werewolf_{kind}_eyes.png",
                  ASSETS / "textures" / "block" / f"{k['rug']}.png", ASSETS / "textures" / "item" / f"{k['pelt']}.png",
                  DATA / MOD / "recipe" / f"{k['rug']}.json", DATA / MOD / "recipe" / f"leather_from_{k['pelt']}.json"]
        pelts = (load(DATA / MOD / "loot_table" / "entities" / "werewolf" / f"{kind}.json") or {}).get("pools", [])
        if [e.get("name") for pool in pelts for e in pool.get("entries", [])] != [f"{MOD}:{k['pelt']}"]:
            err(f"The {kind} werewolf's pelt table must drop {MOD}:{k['pelt']}")
    for path in paths:
        if not path.exists():
            err(f"Werewolves need {path.relative_to(ROOT)}")
    painted = tuple(n * werewolf_model.TEXTURE_SCALE for n in werewolf_model.TEXTURE_SIZE)
    for kind in ww["kinds"]:
        for suffix in ("", "_eyes"):
            texture = ASSETS / "textures" / "entity" / f"werewolf_{kind}{suffix}.png"
            if texture.exists() and Image.open(texture).size != painted:
                err(f"werewolf_{kind}{suffix}.png must be {painted}: tools/werewolf_model.py's texture size times its scale")
    for tag, values in (("werewolf_haunts", ww["haunts"]), ("snow_werewolf_haunts", ww["snow_haunts"]),
                        ("shadow_werewolf_haunts", ww["shadow_haunts"])):
        found = (load(DATA / MOD / "tags" / "worldgen" / "biome" / f"{tag}.json") or {}).get("values", [])
        if sorted(found) != sorted(values):
            err(f"{MOD}:{tag} differs from tools/agriculture.py")
    prey = (load(DATA / MOD / "tags" / "entity_type" / "werewolf_prey.json") or {}).get("values", [])
    if sorted(prey) != sorted(ww["prey"]):
        err(f"{MOD}:werewolf_prey differs from WEREWOLF['prey']")
    arrows = (load(DATA / "minecraft" / "tags" / "item" / "arrows.json") or {}).get("values", [])
    weapons = (load(DATA / MOD / "tags" / "item" / "silver_weapons.json") or {}).get("values", [])
    if f"{MOD}:{ww['arrow']}" not in arrows or f"{MOD}:{ww['dagger']}" not in weapons:
        err("The silver arrow must be in minecraft:arrows and the dagger in jugcraft:silver_weapons")
    # The dagger joins minecraft:swords alongside every other Jugcraft sword; a writer that replaced the file would
    # drop the others.
    swords = (load(DATA / "minecraft" / "tags" / "item" / "swords.json") or {}).get("values", [])
    for sword in [f"{MOD}:{ww['dagger']}", f"{MOD}:power_katana"] + [f"{MOD}:{tier}_sword" for tier in gear.GEAR_TIERS]:
        if sword not in swords:
            err(f"minecraft:swords must hold {sword}")


def check_werewolf_model(model):
    """Every box of tools/werewolf_model.py (texture offset, corner and size) is in WerewolfModel.java, so the texture
    painted from it lines up with the model, and the texture is the size the model declares."""
    def f(value):
        return f"{float(value)}F"

    flat = re.sub(r"\s+", "", model).replace(".mirror()", "").replace(".mirror(false)", "")
    for part in werewolf_model.PARTS:
        for u, v, x, y, z, w, h, d, _mirrored in part["boxes"]:
            box = f".texOffs({u},{v}).addBox({f(x)},{f(y)},{f(z)},{f(w)},{f(h)},{f(d)})"
            if box not in flat:
                err(f"WerewolfModel.java lacks {part['name']}'s box {box} from tools/werewolf_model.py")
    width, height = werewolf_model.TEXTURE_SIZE
    if f"LayerDefinition.create(mesh,{width},{height})" not in flat:
        err(f"WerewolfModel's texture must be {width} by {height}, as tools/werewolf_model.py says")


def check_broomstick(java, number, lang):
    """Fall addition 22: Broomstick matches BROOMSTICK in tools/agriculture.py (charge, flight, the server's checks);
    the item, entity, component and use callback are registered, the client steers and draws it, and its words,
    recipe, advancements and textures exist."""
    br = ag.BROOMSTICK
    names = {"CHARGE_PER_OINTMENT": "charge_per_ointment", "MAX_CHARGE": "max_charge", "ACCEL": "accel", "STRAFE": "strafe",
             "CLIMB": "climb", "DRAG": "drag", "BRAKE": "brake", "MAX_SPEED": "max_speed", "HAT_BONUS": "hat_bonus", "SINK": "sink",
             "CHECK_TICKS": "check_ticks", "TOLERANCE": "tolerance", "DRY_CLIMB": "dry_climb", "LOW_CHARGE": "low_charge",
             "SLOW_FALL_TICKS": "slow_fall_ticks", "MOON_HEIGHT": "moon_height"}
    for name, key in names.items():
        value = number("Broomstick", name)
        if value is None or abs(value - br[key]) > 1e-9:
            err(f"Broomstick.{name} = {value} differs from BROOMSTICK['{key}'] in tools/agriculture.py ({br[key]})")
    if f'ITEM = "{br["item"]}"' not in java.get("Broomstick", ""):
        err("Broomstick.ITEM differs from BROOMSTICK['item']")
    main = java.get("JugcraftAgriculture", "")
    for call in ("registerItem(Broomstick.ITEM, FlyingBroomstickItem::new", "FLYING_BROOMSTICK = entity(Broomstick.ITEM",
                 'Jugcraft.id("broom_charge")', "Broomstick.use(player, level, hand, entity)"):
        if call not in main:
            err(f"JugcraftAgriculture.java must call {call}")
    client = ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client"
    registry = (client / "JugcraftClient.java").read_text(encoding="utf-8")
    if "BroomstickClient.register();" not in registry or "FLYING_BROOMSTICK, BroomstickRenderer::new" not in registry:
        err("JugcraftClient.java must register BroomstickClient and BroomstickRenderer")
    for key in (f"item.{MOD}.{br['item']}", f"entity.{MOD}.{br['item']}", "message.jugcraft.broom.needs_ointment",
                "message.jugcraft.broom.anointed", "message.jugcraft.broom.full", "message.jugcraft.broom.thin", "message.jugcraft.broom.dry",
                "message.jugcraft.broom.bucked", "tooltip.jugcraft.broom.charge", "tooltip.jugcraft.broom.empty", "tooltip.jugcraft.broom.how"):
        if key not in lang:
            err(f"The flying broomstick has no words for {key}")
    recipe = load(DATA / MOD / "recipe" / f"{br['item']}.json") or {}
    if sorted(recipe.get("ingredients", [])) != sorted(br["inputs"]):
        err("The flying broomstick's recipe differs from BROOMSTICK['inputs']")
    for advancement in ("up_and_away", "over_the_moon"):
        if not (DATA / MOD / "advancement" / f"{advancement}.json").exists():
            err(f"The advancement {advancement} is missing")
    for path in (ASSETS / "textures" / "item" / f"{br['item']}.png", ASSETS / "textures" / "entity" / f"{br['item']}.png"):
        if not path.exists():
            err(f"The flying broomstick needs {path.relative_to(ROOT)}")


def check_hexes(java, number, lang):
    """Fall addition 21: Hexes and the cauldron match HEX in tools/agriculture.py (doses, the room check's extension,
    each draught's scale, steps, reach and time, and which brew each hex comes from); the hex tags, items, effects,
    words and advancements exist."""
    hx = ag.HEX
    brews = hx["brews"]
    expected = {("BubblingCauldronBlock", "DOSES"): hx["doses"], ("Hexes", "ROOM_EXTEND_TICKS"): hx["room_extend_ticks"],
                ("Hexes", "SHRUNK_SCALE"): abs(brews["shrinking"]["scale"]), ("Hexes", "GIANT_SCALE"): brews["giant"]["scale"],
                ("Hexes", "GIANT_STEP"): brews["giant"]["step"], ("Hexes", "GIANT_REACH"): brews["giant"]["reach"],
                ("Hexes", "SHRINKING_SECONDS"): brews["shrinking"]["seconds"], ("Hexes", "GIANT_SECONDS"): brews["giant"]["seconds"],
                ("Hexes", "FLYING_SECONDS"): brews["flying"]["seconds"]}
    for (source, name), value in expected.items():
        if number(source, name) is None or abs(number(source, name) - value) > 1e-6:
            err(f"{source}.{name} = {number(source, name)} differs from HEX in tools/agriculture.py ({value})")
    source = java.get("BubblingCauldronBlock", "")
    for hex_name, info in brews.items():
        if f"{hex_name.upper()}(Brew.{info['brew'].upper()}" not in source and f"case {hex_name.upper()} -> {info['brew'].upper()}" not in source:
            err(f"BubblingCauldronBlock must make the {hex_name} hex from the {info['brew']} brew")
        values = (load(DATA / MOD / "tags" / "item" / "hex" / f"{hex_name}.json") or {}).get("values", [])
        if sorted(values) != sorted(info["ingredients"]):
            err(f"The item tag {MOD}:hex/{hex_name} differs from HEX in tools/agriculture.py")
        if f"item.{MOD}.{info['item']}" not in lang or not (ASSETS / "textures" / "item" / f"{info['item']}.png").exists():
            err(f"{info['item']} needs its words and texture")
        if "effect" in info and (f"effect.{MOD}.{info['effect']}" not in lang
                                 or not (ASSETS / "textures" / "mob_effect" / f"{info['effect']}.png").exists()):
            err(f"The {info['effect']} effect needs its words and icon")
    for key in ("drink_me", "fee_fi_fo_fum"):
        if not (DATA / MOD / "advancement" / f"{key}.json").exists():
            err(f"Missing advancement {key}")


def check_decor5(java):
    """The harvest party: Java matches tools/agriculture.py (the tub's apples, odds and splash, the crate's capacity
    and tag, the bale's seat height and softening, the wreath's flowers, the piles' layers, softening and colours), every
    block state has a model, and the tub's messages have their text."""
    def number(source, name):
        match = re.search(rf"\b{name} = ([\d.]+)[FLD]?;", java.get(source, ""))
        return float(match.group(1)) if match else None

    tub, crate, bale, wreath, piles = ag.BOBBING_TUB, ag.PUMPKIN_CRATE, ag.HAY_BALE_SEAT, ag.AUTUMN_WREATH, ag.LEAF_PILES
    expected = {("BobbingTubBlock", "MAX_APPLES"): tub["max_apples"], ("BobbingTubBlock", "CHANCE"): tub["chance"],
                ("BobbingTubBlock", "SPLASH_TICKS"): tub["splash_ticks"], ("PumpkinCrateBlockEntity", "CAPACITY"): crate["capacity"],
                ("HayBaleSeatBlock", "FALL_SOFTENING"): bale["fall_softening"], ("LeafPileBlock", "MAX_LAYERS"): piles["max_layers"],
                ("LeafPileBlock", "LAYER_PIXELS"): piles["layer_pixels"], ("LeafPileBlock", "SOFTENING_PER_LAYER"): piles["softening_per_layer"],
                ("LeafPileBlock", "SCATTER_CHANCE"): piles["scatter_chance"]}
    for (source, name), value in expected.items():
        if number(source, name) is None or abs(number(source, name) - value) > 1e-9:
            err(f"{source}.{name} = {number(source, name)} differs from tools/agriculture.py ({value})")
    height = re.search(r"HEIGHT = ([\d.]+) / ([\d.]+);", java.get("HayBaleSeatBlock", ""))
    if not height or abs(float(height.group(1)) / float(height.group(2)) - bale["height"]) > 1e-9:
        err(f"HayBaleSeatBlock.HEIGHT differs from HAY_BALE_SEAT's height ({bale['height']})")
    if f'Jugcraft.id("{crate["produce_tag"].split(":")[1]}")' not in java.get("PumpkinCrateBlock", ""):
        err(f"PumpkinCrateBlock.PRODUCE is not {crate['produce_tag']}")
    mums = re.search(r"enum Mums implements StringRepresentable \{\s*([A-Z_, ]+);", java.get("AutumnWreathBlock", ""))
    if not mums or [v.strip().lower() for v in mums.group(1).split(",")] != wreath["flowers"]:
        err("AutumnWreathBlock.Mums differs from AUTUMN_WREATH's flowers")
    if f"Mums.{wreath['default'].upper()}" not in java.get("AutumnWreathBlock", ""):
        err("AutumnWreathBlock's default flowers differ from AUTUMN_WREATH's default")
    colours = re.search(r"LEAF_PILE_COLOURS = List\.of\(([^)]*)\)", java.get("JugcraftAgriculture", ""))
    if not colours or re.findall(r'"(\w+)"', colours.group(1)) != list(piles["colours"]):
        err("JugcraftAgriculture.LEAF_PILE_COLOURS differs from LEAF_PILES' colours")

    def variants(block):
        return set((load(ASSETS / "blockstates" / f"{block}.json") or {}).get("variants", {}))
    horizontal = ("north", "east", "south", "west")
    wanted = {
        tub["block"]: {f"apples={n},splashing={s}" for n in range(tub["max_apples"] + 1) for s in ("false", "true")},
        crate["block"]: {f"facing={f}" for f in horizontal},
        bale["block"]: {f"facing={f}" for f in horizontal},
        wreath["block"]: {f"facing={f},flowers={c}" for f in horizontal for c in wreath["flowers"]},
        **{pile: {f"layers={n}" for n in range(1, piles["max_layers"] + 1)} for pile in ag.leaf_piles()},
    }
    for block, keys in wanted.items():
        if variants(block) != keys:
            err(f"{block}: blockstate variants differ from its properties")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    for key in tub["messages"]:
        if f"message.{MOD}.{tub['block']}.{key}" not in lang:
            err(f"The tub's {key} message has no text")
    values = (load(DATA / MOD / "tags" / "item" / f"{crate['produce_tag'].split(':')[1]}.json") or {}).get("values", [])
    if sorted(values) != sorted(crate["produce"]):
        err(f"The item tag {crate['produce_tag']} differs from PUMPKIN_CRATE's produce")

def check_decor6(java):
    """The haunted house and yard: Java matches tools/agriculture.py (the chair's seat and rocking, the eyes' distance
    and blinking, the window's designs and light, the music box's tune length and every note in range, the spider's
    drop and sway), every block state has a model, and the quads, glowing papers and eye sprite the client draws exist."""
    def number(source, name):
        match = re.search(rf"\b{name} = ([\d.]+)[FLD]?;", java.get(source, ""))
        return float(match.group(1)) if match else None

    chair, eyes, window, box_, spider = ag.ROCKING_CHAIR, ag.LURKING_EYES, ag.SILHOUETTE_WINDOW, ag.MUSIC_BOX, ag.GIANT_FAKE_SPIDER
    expected = {("RockingChairBlock", "HAUNTED_ROCK"): chair["haunted_rock"], ("RockingChairBlock", "SITTER_ROCK"): chair["sitter_rock"],
                ("RockingChairBlock", "ROCK_PERIOD"): chair["rock_period"], ("LurkingEyesBlock", "HIDE_DISTANCE"): eyes["hide_distance"],
                ("LurkingEyesBlock", "BLINK_PERIOD"): eyes["blink_period"], ("LurkingEyesBlock", "BLINK_TICKS"): eyes["blink_ticks"],
                ("SilhouetteWindowBlock", "GLOW_LIGHT"): window["glow_light"], ("MusicBoxBlockEntity", "TICKS_PER_BEAT"): box_["ticks_per_beat"],
                ("MusicBoxBlockEntity", "BEATS"): box_["beats"], ("GiantFakeSpiderBlock", "MAX_DROP"): spider["max_drop"],
                ("GiantFakeSpiderBlock", "SWAY_PERIOD"): spider["sway_period"], ("GiantFakeSpiderBlock", "SWAY_DEGREES"): spider["sway_degrees"]}
    for (source, name), value in expected.items():
        if number(source, name) is None or abs(number(source, name) - value) > 1e-9:
            err(f"{source}.{name} = {number(source, name)} differs from tools/agriculture.py ({value})")
    seat = re.search(r"SEAT_HEIGHT = ([\d.]+) / ([\d.]+);", java.get("RockingChairBlock", ""))
    if not seat or abs(float(seat.group(1)) / float(seat.group(2)) - chair["seat_height"]) > 1e-9:
        err(f"RockingChairBlock.SEAT_HEIGHT differs from ROCKING_CHAIR's seat_height ({chair['seat_height']})")
    designs = re.search(r"enum Design implements StringRepresentable \{\s*([A-Z_, ]+);", java.get("SilhouetteWindowBlock", ""))
    if not designs or [v.strip().lower() for v in designs.group(1).split(",")] != window["designs"]:
        err("SilhouetteWindowBlock.Design differs from SILHOUETTE_WINDOW's designs")
    tune = re.search(r"TUNE = \{(.*?)\};", java.get("MusicBoxBlockEntity", ""), re.S)
    notes = re.findall(r"\{(\d+), (\d+), (BELL|HARP)\}", tune.group(1)) if tune else []
    if not notes:
        err("MusicBoxBlockEntity.TUNE has no notes")
    for beat, note, _ in notes:
        if not 0 <= int(beat) < box_["beats"] or not 0 <= int(note) <= 24:
            err(f"Music box note {{{beat}, {note}}} is outside the tune or a note block's range")

    def variants(block):
        return set((load(ASSETS / "blockstates" / f"{block}.json") or {}).get("variants", {}))
    horizontal = ("north", "east", "south", "west")
    wanted = {
        chair["block"]: {f"facing={f}" for f in horizontal},
        eyes["block"]: {f"facing={f}" for f in horizontal + ("up", "down")},
        window["block"]: {f"design={d},facing={f}" for f in horizontal for d in window["designs"]},
        box_["block"]: {f"facing={f},open={o},powered={p}" for f in horizontal for o in ("false", "true") for p in ("false", "true")},
        spider["block"]: {f"drop={n}" for n in range(1, spider["max_drop"] + 1)},
    }
    for block, keys in wanted.items():
        if variants(block) != keys:
            err(f"{block}: blockstate variants differ from its properties")
    quads = load(ASSETS / "decor_quads.json") or {}
    for name in ("rocking_chair", "giant_fake_spider"):
        if not quads.get(name):
            err(f"decor_quads.json has no quads for {name}")
        for quad in quads.get(name, []):
            if not (ASSETS / "textures" / "block" / f"{quad['texture']}.png").exists():
                err(f"decor_quads.json: {name} uses a missing texture {quad['texture']}")
                break
    for texture in [f"silhouette_window_{d}_lit" for d in window["designs"]] + ["lurking_eyes"]:
        if not (ASSETS / "textures" / "entity" / f"{texture}.png").exists():
            err(f"Missing entity texture {texture}")

def check_decor7(java):
    """The haunted house inside: Java matches tools/agriculture.py (the chandelier's candles, gusts and sway, the
    organ's size, tune length and phantom chance, the suit's watching, the sheet's tag and breathing, the mirror's face,
    the curtains' drape and sway, the doll's glances), every note of the organ's tune is in range, every block state
    has a model, and the quads and textures the client draws exist."""
    import decor7_data

    def number(source, name):
        match = re.search(rf"\b{name} = ([\d.]+)[FLD]?;", java.get(source, ""))
        return float(match.group(1)) if match else None

    chandelier, organ, armor, sheet = ag.HAUNTED_CHANDELIER, ag.PIPE_ORGAN, ag.SUIT_OF_ARMOR, ag.DUST_SHEET
    mirror, curtains, doll = ag.SPIRIT_MIRROR, ag.TATTERED_CURTAINS, ag.CREEPY_DOLL
    expected = {("HauntedChandelierBlock", "CANDLES"): chandelier["candles"], ("HauntedChandelierBlock", "GUST_CHANCE"): chandelier["gust_chance"],
                ("HauntedChandelierBlock", "RELIGHT_TICKS"): chandelier["relight_ticks"],
                ("HauntedChandelierBlock", "SWAY_DEGREES"): chandelier["sway_degrees"], ("HauntedChandelierBlock", "SWAY_PERIOD"): chandelier["sway_period"],
                ("HauntedChandelierBlock", "RING_RADIUS"): decor7_data.RING,
                ("PipeOrganBlock", "WIDTH"): organ["width"], ("PipeOrganBlock", "HEIGHT"): organ["height"],
                ("PipeOrganBlock", "PHANTOM_CHANCE"): organ["phantom_chance"], ("PipeOrganBlockEntity", "TUNE_TICKS"): organ["tune_ticks"],
                ("SuitOfArmorBlock", "WATCH_RANGE"): armor["watch_range"], ("SuitOfArmorBlock", "TURN_SPEED"): armor["turn_speed"],
                ("SuitOfArmorBlock", "MAX_TURN"): armor["max_turn"], ("DustSheetBlock", "BREATHE_CHANCE"): sheet["breathe_chance"],
                ("SpiritMirrorBlock", "PERIOD"): mirror["period"], ("SpiritMirrorBlock", "VISIBLE"): mirror["visible"],
                ("SpiritMirrorBlock", "FADE"): mirror["fade"], ("SpiritMirrorBlock", "RANGE"): mirror["range"],
                ("TatteredCurtainsBlock", "MAX_DROP"): curtains["max_drop"], ("TatteredCurtainsBlock", "SWAY"): curtains["sway"],
                ("TatteredCurtainsBlock", "NIGHT_SWAY"): curtains["night_sway"], ("TatteredCurtainsBlock", "SWAY_PERIOD"): curtains["sway_period"],
                ("CreepyDollBlock", "UNSEEN_TICKS"): doll["unseen_ticks"], ("CreepyDollBlock", "ELSEWHERE_CHANCE"): doll["elsewhere_chance"]}
    for (source, name), value in expected.items():
        if number(source, name) is None or abs(number(source, name) - value) > 1e-9:
            err(f"{source}.{name} = {number(source, name)} differs from tools/agriculture.py ({value})")
    tune = re.search(r"TUNE = \{(.*?)\};", java.get("PipeOrganBlockEntity", ""), re.S)
    notes = re.findall(r"\{(\d+), (\d+), (FLUTE|HARP|BASS)\}", tune.group(1)) if tune else []
    if not notes:
        err("PipeOrganBlockEntity.TUNE has no notes")
    for tick, note, _ in notes:
        if not 0 <= int(tick) < organ["tune_ticks"] or not 0 <= int(note) <= 24:
            err(f"Organ note {{{tick}, {note}}} is outside the tune or a note block's range")
    if f'Jugcraft.id("{sheet["tag"].split(":")[1]}")' not in java.get("JugcraftAgriculture", ""):
        err(f"JugcraftAgriculture.DUST_SHEET_COVERABLE is not {sheet['tag']}")
    values = (load(DATA / MOD / "tags" / "block" / f"{sheet['tag'].split(':')[1]}.json") or {}).get("values", [])
    if sorted(values) != sorted(sheet["coverable"]):
        err(f"The block tag {sheet['tag']} differs from DUST_SHEET's coverable blocks")

    def variants(block):
        return set((load(ASSETS / "blockstates" / f"{block}.json") or {}).get("variants", {}))
    horizontal = ("north", "east", "south", "west")
    booleans = ("false", "true")
    wanted = {
        chandelier["block"]: {f"burning={n},lit={b}" for n in range(chandelier["candles"] + 1) for b in booleans},
        organ["block"]: {f"facing={f},part={p},playing={a},powered={b}" for f in horizontal for p in range(organ["width"] * organ["height"])
                         for a in booleans for b in booleans},
        armor["block"]: {f"facing={f},half={h}" for f in horizontal for h in ("lower", "upper")},
        sheet["block"]: {""},
        mirror["block"]: {f"facing={f}" for f in horizontal},
        curtains["block"]: {f"facing={f},open={o},part={p}" for f in horizontal for o in booleans for p in ("single", "top", "middle", "bottom")},
        doll["block"]: {f"facing={f}" for f in horizontal},
    }
    for block, keys in wanted.items():
        if variants(block) != keys:
            err(f"{block}: blockstate variants differ from its properties")
    quads = load(ASSETS / "decor7_quads.json") or {}
    for name in ("haunted_chandelier", "suit_of_armor_helmet", "creepy_doll_head"):
        if not quads.get(name):
            err(f"decor7_quads.json has no quads for {name}")
        for quad in quads.get(name, []):
            if not (ASSETS / "textures" / "block" / f"{quad['texture']}.png").exists():
                err(f"decor7_quads.json: {name} uses a missing texture {quad['texture']}")
                break
    for kind, texture in (("entity", "haunted_chandelier_flame"), ("entity", "suit_of_armor_glow"), ("entity", "dust_sheet_side"),
                          ("entity", "spirit_mirror_face"), ("entity", "tattered_curtains"), ("entity", "tattered_curtains_hem"),
                          ("block", "phantom_pipe_organ_ivory"), ("block", "phantom_pipe_organ_ebony"), ("block", "tattered_curtains_rod"),
                          ("block", "dust_sheet")):
        if not (ASSETS / "textures" / kind / f"{texture}.png").exists():
            err(f"Missing {kind} texture {texture}")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    for key in ("day", "night"):
        if f"message.{MOD}.{mirror['block']}.{key}" not in lang:
            err(f"The spirit mirror's {key} message has no text")


def check_decor8(java):
    """The mad scientist and monsters: Java matches tools/agriculture.py (the coil's power, range and arcs, the table's
    sitting and twitching, the jar's specimens, light and bob, the sarcophagus's timing and swing, the raven's watching
    and ruffling, the cat's reach, hiss and swish), every block state has a model, and the quads and textures the client
    draws exist."""
    def number(source, name):
        match = re.search(rf"\b{name} = ([\d.]+)[FLD]?;", java.get(source, ""))
        return float(match.group(1)) if match else None

    coil, table, jar, sarcophagus, raven, cat = ag.TESLA_COIL, ag.LAB_TABLE, ag.SPECIMEN_JAR, ag.SARCOPHAGUS, ag.RAVEN, ag.BLACK_CAT
    expected = {("TeslaCoilBlockEntity", "USE"): coil["use"], ("TeslaCoilBlockEntity", "CAPACITY"): coil["capacity"],
                ("TeslaCoilBlockEntity", "INPUT"): coil["input"], ("TeslaCoilBlockEntity", "RANGE"): coil["range"],
                ("TeslaCoilBlockEntity", "ARC_MIN"): coil["arc_min"], ("TeslaCoilBlockEntity", "ARC_SPREAD"): coil["arc_spread"],
                ("TeslaCoilBlockEntity", "ARC_TICKS"): coil["arc_ticks"], ("TeslaCoilBlock", "LIGHT"): coil["light"],
                ("LabTableBlock", "SIT_DEGREES"): table["sit_degrees"], ("LabTableBlock", "SIT_SPEED"): table["sit_speed"],
                ("LabTableBlock", "TWITCH_PERIOD"): table["twitch_period"], ("LabTableBlock", "TWITCH_TICKS"): table["twitch_ticks"],
                ("SpecimenJarBlock", "LIGHT"): jar["light"], ("SpecimenJarBlock", "BOB"): jar["bob"], ("SpecimenJarBlock", "BOB_TICKS"): jar["bob_ticks"],
                ("MummySarcophagusBlock", "OPEN_TICKS"): sarcophagus["open_ticks"], ("MummySarcophagusBlock", "LID_DEGREES"): sarcophagus["lid_degrees"],
                ("MummySarcophagusBlock", "LURCH"): sarcophagus["lurch"], ("RavenPerchBlock", "WATCH_RANGE"): raven["watch_range"],
                ("RavenPerchBlock", "MAX_TURN"): raven["max_turn"], ("RavenPerchBlock", "RUFFLE_PERIOD"): raven["ruffle_period"],
                ("RavenPerchBlock", "RUFFLE_TICKS"): raven["ruffle_ticks"], ("BlackCatBlock", "REACH"): cat["reach"],
                ("BlackCatBlock", "HISS_TICKS"): cat["hiss_ticks"], ("BlackCatBlock", "COOLDOWN_TICKS"): cat["cooldown_ticks"],
                ("BlackCatBlock", "SWISH_PERIOD"): cat["swish_period"], ("BlackCatBlock", "SWISH_DEGREES"): cat["swish_degrees"]}
    for (source, name), value in expected.items():
        if number(source, name) is None or abs(number(source, name) - value) > 1e-9:
            err(f"{source}.{name} = {number(source, name)} differs from tools/agriculture.py ({value})")
    specimens = re.search(r"enum Specimen implements StringRepresentable \{\s*([A-Z_, ]+);", java.get("SpecimenJarBlock", ""))
    if not specimens or [v.strip().lower() for v in specimens.group(1).split(",")] != jar["specimens"]:
        err("SpecimenJarBlock.Specimen differs from SPECIMEN_JAR's specimens")

    def variants(block):
        return set((load(ASSETS / "blockstates" / f"{block}.json") or {}).get("variants", {}))
    horizontal = ("north", "east", "south", "west")
    booleans = ("false", "true")
    wanted = {
        coil["block"]: {f"active={a},enabled={e},facing={f},half={h}" for a in booleans for e in booleans for f in horizontal
                        for h in ("lower", "upper")},
        table["block"]: {f"facing={f},part={p},powered={b}" for f in horizontal for p in ("foot", "head") for b in booleans},
        jar["block"]: {f"specimen={s}" for s in jar["specimens"]},
        sarcophagus["block"]: {f"facing={f},half={h},open={o},powered={p}" for f in horizontal for h in ("lower", "upper") for o in booleans
                               for p in booleans},
        raven["block"]: {f"facing={f}" for f in horizontal},
        cat["block"]: {f"facing={f},hissing={h}" for f in horizontal for h in booleans},
    }
    for block, keys in wanted.items():
        if variants(block) != keys:
            err(f"{block}: blockstate variants differ from its properties")
    quads = load(ASSETS / "decor8_quads.json") or {}
    names = (["lab_table_legs", "lab_table_torso", "mummy_sarcophagus_lid", "mummy_sarcophagus_mummy", "mummy_sarcophagus_arms", "raven_body",
              "raven_head", "raven_left_wing", "raven_right_wing", "black_cat_tail", "black_cat_tail_up"]
             + [f"specimen_{s}" for s in jar["specimens"]])
    for name in names:
        if not quads.get(name):
            err(f"decor8_quads.json has no quads for {name}")
        for quad in quads.get(name, []):
            if not (ASSETS / "textures" / "block" / f"{quad['texture']}.png").exists():
                err(f"decor8_quads.json: {name} uses a missing texture {quad['texture']}")
                break
    for texture in ("tesla_coil_arc", "specimen_jar_bubble", "black_cat_eyes"):
        if not (ASSETS / "textures" / "entity" / f"{texture}.png").exists():
            err(f"Missing entity texture {texture}")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    for key in [f"tesla_coil.{k}" for k in ("on", "off")] + [f"specimen_jar.{s}" for s in jar["specimens"]]:
        if f"message.{MOD}.{key}" not in lang:
            err(f"Message {key} has no text")


def check_decor9(java):
    """The yard and porch: Java matches tools/agriculture.py (the inflatables' filling, light and wobble, the witch's
    reach, cackle, rest and stirring, the hands' grab and rest, the chimes' swing and clacking, the vanes' turning, the
    archway's and tree's sizes and light), the designs, poses and words agree, every block state has a model, the
    block models turn only by 22.5 or 45 degrees, and the quads and textures the client draws exist."""
    def number(source, name):
        match = re.search(rf"\b{name} = ([\d.]+)[FLD]?;", java.get(source, ""))
        return float(match.group(1)) if match else None

    inf, witch, hands, chimes, vanes = ag.INFLATABLES, ag.PORCH_WITCH, ag.GRASPING_HANDS, ag.WIND_CHIMES, ag.WEATHERVANES
    arch, tree = ag.HAUNTED_ARCHWAY, ag.DEAD_TREE
    expected = {("InflatableBlock", "INFLATE_TICKS"): inf["inflate_ticks"], ("InflatableBlock", "DEFLATE_TICKS"): inf["deflate_ticks"],
                ("InflatableBlock", "LIGHT"): inf["light"], ("InflatableBlock", "WOBBLE_DEGREES"): inf["wobble_degrees"],
                ("InflatableBlock", "WOBBLE_PERIOD"): inf["wobble_period"],
                ("PorchWitchBlock", "REACH"): witch["reach"], ("PorchWitchBlock", "CACKLE_TICKS"): witch["cackle_ticks"],
                ("PorchWitchBlock", "COOLDOWN_TICKS"): witch["cooldown_ticks"], ("PorchWitchBlock", "STIR_PERIOD"): witch["stir_period"],
                ("PorchWitchBlock", "FAST_STIR_PERIOD"): witch["fast_stir_period"], ("PorchWitchBlock", "WATCH_RANGE"): witch["watch_range"],
                ("GraspingHandsBlock", "SLOW_TICKS"): hands["slow_ticks"], ("GraspingHandsBlock", "SLOWNESS_LEVEL"): hands["slowness_level"],
                ("GraspingHandsBlock", "GRAB_TICKS"): hands["grab_ticks"], ("GraspingHandsBlock", "REST_TICKS"): hands["rest_ticks"],
                ("BoneWindChimesBlock", "BONES"): chimes["bones"], ("BoneWindChimesBlock", "CALM_SWING"): chimes["calm_swing"],
                ("BoneWindChimesBlock", "RAIN_SWING"): chimes["rain_swing"], ("BoneWindChimesBlock", "STORM_SWING"): chimes["storm_swing"],
                ("BoneWindChimesBlock", "CALM_CHANCE"): chimes["calm_chance"], ("BoneWindChimesBlock", "RAIN_CHANCE"): chimes["rain_chance"],
                ("BoneWindChimesBlock", "STORM_CHANCE"): chimes["storm_chance"], ("WeathervaneBlock", "TURN_SPEED"): vanes["turn_speed"],
                ("HauntedArchwayBlock", "WIDTH"): arch["width"], ("HauntedArchwayBlock", "HEIGHT"): arch["height"],
                ("HauntedArchwayBlock", "LIGHT"): arch["light"], ("DeadHollowTreeBlock", "HEIGHT"): tree["height"],
                ("DeadHollowTreeBlock", "LIGHT"): tree["light"]}
    for (source, name), value in expected.items():
        if number(source, name) is None or abs(number(source, name) - value) > 1e-9:
            err(f"{source}.{name} = {number(source, name)} differs from tools/agriculture.py ({value})")
    main = java.get("JugcraftAgriculture", "")
    for name, designs in (("INFLATABLE_DESIGNS", inf["designs"]), ("WEATHERVANE_DESIGNS", vanes["designs"])):
        match = re.search(rf"{name} = List\.of\(([^)]*)\);", main)
        if not match or re.findall(r'"([a-z_]+)"', match.group(1)) != designs:
            err(f"JugcraftAgriculture.{name} differs from tools/agriculture.py")
    for source, enum, values in (("PoseableSkeletonBlock", "Pose", ag.POSEABLE_SKELETON["poses"]), ("SpookySignBlock", "Words", ag.SPOOKY_SIGN["words"]),
                                 ("GraspingHandsBlock", "Phase", ["rest", "grab", "recover"])):
        match = re.search(rf"enum {enum} implements StringRepresentable \{{\s*([A-Z_, ]+);", java.get(source, ""))
        if not match or [v.strip().lower() for v in match.group(1).split(",")] != values:
            err(f"{source}.{enum} differs from tools/agriculture.py")
    if number("GravestoneBlockEntity", "MAX_LENGTH") != ag.SPOOKY_SIGN["max_length"]:
        err("SPOOKY_SIGN's max_length differs from GravestoneBlockEntity.MAX_LENGTH")

    def variants(block):
        return set((load(ASSETS / "blockstates" / f"{block}.json") or {}).get("variants", {}))
    horizontal = ("north", "east", "south", "west")
    booleans = ("false", "true")
    halves = ("lower", "upper")
    wanted = {ag.PORCH_WITCH["block"]: {f"cackling={c},facing={f},half={h}" for c in booleans for f in horizontal for h in halves},
              ag.GRASPING_HANDS["block"]: {f"facing={f},phase={p}" for f in horizontal for p in ("rest", "grab", "recover")},
              ag.POSEABLE_SKELETON["block"]: {f"facing={f},half={h},pose={p}" for f in horizontal for h in halves for p in ag.POSEABLE_SKELETON["poses"]},
              ag.WIND_CHIMES["block"]: {""},
              ag.SPOOKY_SIGN["block"]: {f"facing={f},words={w}" for f in horizontal for w in ag.SPOOKY_SIGN["words"]},
              arch["block"]: {f"facing={f},lit={l},part={p}" for f in horizontal for l in booleans for p in range(7)},
              tree["block"]: {f"facing={f},lit={l},part={p}" for f in horizontal for l in booleans for p in range(tree["height"])}}
    for design in inf["designs"]:
        wanted[ag.inflatable(design)] = {f"facing={f},half={h},on={o},powered={p}" for f in horizontal for h in halves for o in booleans for p in booleans}
    for design in vanes["designs"]:
        wanted[ag.weathervane(design)] = {""}
    for block, keys in wanted.items():
        if variants(block) != keys:
            err(f"{block}: blockstate variants differ from its properties")
        for variant in ((load(ASSETS / "blockstates" / f"{block}.json") or {}).get("variants", {})).values():
            model = load(ASSETS / "models" / f"{variant['model'].split(':')[1]}.json") or {}
            for element in model.get("elements", []):
                angle = element.get("rotation", {}).get("angle", 0)
                if angle not in (-45, -22.5, 0, 22.5, 45):
                    err(f"{variant['model']}: a box turns by {angle} degrees; block models only turn by 22.5 or 45")
    quads = load(ASSETS / "decor9_quads.json") or {}
    names = ([f"inflatable_{d}" for d in inf["designs"]] + ["porch_witch_arm", "porch_witch_head", "wind_chime_bone", "wind_chime_skull"]
             + [f"weathervane_{d}" for d in vanes["designs"]])
    for name in names:
        if not quads.get(name):
            err(f"decor9_quads.json has no quads for {name}")
        for quad in quads.get(name, []):
            if not (ASSETS / "textures" / "block" / f"{quad['texture']}.png").exists():
                err(f"decor9_quads.json: {name} uses a missing texture {quad['texture']}")
                break
    if not (ASSETS / "textures" / "entity" / "porch_witch_eyes.png").exists():
        err("Missing entity texture porch_witch_eyes")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    for key in [f"spooky_sign.{w}" for w in ag.SPOOKY_SIGN["words"]] + ["spooky_sign.painted", "spooky_sign.unnamed_tag"]:
        if f"message.{MOD}.{key}" not in lang:
            err(f"Message {key} has no text")


def check_decor10(java):
    """Lighting and glow: Java matches tools/agriculture.py (the black light's light and range, the brazier's light and
    flames, the lamp's light, turning and reach, the pumpkins' and hat's light, the hat's bob and turning), the glow
    paint's designs agree, every block state has a model, and the textures and quads the client draws exist."""
    def number(source, name):
        match = re.search(rf"\b{name} = ([\d.]+)[FLD]?;", java.get(source, ""))
        return float(match.group(1)) if match else None

    light, paint, brazier, lamp, pumpkins, hat = ag.BLACK_LIGHT, ag.GLOW_PAINT, ag.BRAZIER, ag.SHADOW_LAMP, ag.MINI_PUMPKINS, ag.FLOATING_HAT
    expected = {("BlackLightBlock", "LIGHT"): light["light"], ("BlackLightBlock", "RANGE"): light["range"],
                ("WitchFireBrazierBlock", "LIGHT"): brazier["light"], ("ShadowPuppetLampBlock", "LIGHT"): lamp["light"],
                ("ShadowPuppetLampBlock", "TURN_TICKS"): lamp["turn_ticks"], ("ShadowPuppetLampBlock", "RANGE"): lamp["range"],
                ("MiniPumpkinStackBlock", "LIGHT"): pumpkins["light"], ("FloatingWitchHatBlock", "LIGHT"): hat["light"],
                ("FloatingWitchHatBlock", "BOB"): hat["bob"], ("FloatingWitchHatBlock", "BOB_TICKS"): hat["bob_ticks"],
                ("FloatingWitchHatBlock", "TURN_TICKS"): hat["turn_ticks"]}
    for (source, name), value in expected.items():
        if number(source, name) is None or abs(number(source, name) - value) > 1e-9:
            err(f"{source}.{name} = {number(source, name)} differs from tools/agriculture.py ({value})")
    designs = re.search(r"enum Design implements StringRepresentable \{\s*([A-Z_, ]+);", java.get("GlowPaintBlock", ""))
    if not designs or [v.strip().lower() for v in designs.group(1).split(",")] != paint["designs"]:
        err("GlowPaintBlock.Design differs from GLOW_PAINT's designs")
    flames = re.findall(r"\b([A-Z]+)\(0x[0-9A-F]+, DyeColor\.([A-Z_]+)\)", java.get("WitchFireBrazierBlock", ""))
    if [(f.lower(), f"{d.lower()}_dye") for f, d in flames] != list(brazier["flames"].items()):
        err("WitchFireBrazierBlock.Flame differs from BRAZIER's flames and dyes")

    def variants(block):
        return set((load(ASSETS / "blockstates" / f"{block}.json") or {}).get("variants", {}))
    horizontal = ("north", "east", "south", "west")
    booleans = ("false", "true")
    wanted = {light["block"]: {f"facing={f},lit={l},powered={p}" for f in horizontal for l in booleans for p in booleans},
              paint["block"]: {f"design={d},facing={f}" for d in paint["designs"] for f in horizontal + ("up", "down")},
              brazier["block"]: {f"flame={c},lit={l}" for c in brazier["flames"] for l in booleans},
              lamp["block"]: {f"lit={l}" for l in booleans},
              pumpkins["block"]: {f"facing={f},lit={l}" for f in horizontal for l in booleans},
              hat["block"]: {f"lit={l}" for l in booleans}}
    for block, keys in wanted.items():
        if variants(block) != keys:
            err(f"{block}: blockstate variants differ from its properties")
    entity = ASSETS / "textures" / "entity"
    for texture in ([f"glow_paint_{d}_glow" for d in paint["designs"]] + ["witch_fire_flame"]
                    + [f"shadow_puppet_lamp_{d}_shadow" for d in ("bat", "cat", "witch")]):
        if not (entity / f"{texture}.png").exists():
            err(f"Missing entity texture {texture}")
    for design in ("bat", "cat", "witch"):
        if not (ASSETS / "textures" / "block" / f"shadow_puppet_lamp_paper_{design}.png").exists():
            err(f"Missing lamp paper {design}")
    quads = load(ASSETS / "decor10_quads.json") or {}
    for name in ("floating_witch_hat", "floating_witch_hat_flame"):
        if not quads.get(name):
            err(f"decor10_quads.json has no quads for {name}")


def check_decor11(java):
    """Party games: Java matches tools/agriculture.py (the trap's reach and timing, the contest's round, range and
    entries, the bowling pumpkin's roll and the lane's reach, the dance floor's reach and light, ghost tag's round,
    range, players and tag-backs, the fortune table's fortunes, cards and cooldown), every block state has a model, every
    message the games show has its words (each fortune, each vote, each candy cache answer), and the textures and quads
    the client draws exist."""
    def number(source, name):
        match = re.search(rf"\b{name} = ([\d.]+)[FLD]?;", java.get(source, ""))
        return float(match.group(1)) if match else None

    scare, contest, bowling, dance, tag, fortune = ag.JUMP_SCARE, ag.COSTUME_CONTEST, ag.BOWLING, ag.DANCE_FLOOR, ag.GHOST_TAG, ag.FORTUNE_TABLE
    expected = {("JumpScareTrapBlock", "REACH"): scare["reach"], ("JumpScareTrapBlock", "POP_TICKS"): scare["pop_ticks"],
                ("JumpScareTrapBlock", "RESET_TICKS"): scare["reset_ticks"], ("JudgesTableBlockEntity", "ROUND_TICKS"): contest["round_ticks"],
                ("JudgesTableBlockEntity", "RANGE"): contest["range"], ("JudgesTableBlockEntity", "MAX_CONTESTANTS"): contest["max_contestants"],
                ("BowlingPumpkin", "SPEED"): bowling["speed"], ("BowlingPumpkin", "FRICTION"): bowling["friction"],
                ("BowlingPumpkin", "DOMINO_CHANCE"): bowling["domino_chance"], ("BowlingScoreboardBlock", "LANE_REACH"): bowling["lane_reach"],
                ("BowlingScore", "FRAMES"): bowling["frames"], ("DanceFloorBlock", "REACH"): dance["reach"], ("DanceFloorBlock", "LIGHT"): dance["light"],
                ("GhostBellBlockEntity", "ROUND_TICKS"): tag["round_ticks"], ("GhostBellBlockEntity", "RANGE"): tag["range"],
                ("GhostBellBlockEntity", "TAG_BACK_TICKS"): tag["tag_back_ticks"], ("GhostBellBlockEntity", "MAX_PLAYERS"): tag["max_players"],
                ("FortuneTellerTableBlock", "FORTUNES"): fortune["fortunes"], ("FortuneTellerTableBlock", "CARDS"): fortune["cards"],
                ("FortuneTellerTableBlock", "COOLDOWN_TICKS"): fortune["cooldown_ticks"]}
    for (source, name), value in expected.items():
        if number(source, name) is None or abs(number(source, name) - value) > 1e-9:
            err(f"{source}.{name} = {number(source, name)} differs from tools/agriculture.py ({value})")
    if "extends CandyBowlBlock" not in java.get("CandyCacheBlock", "") or "cache).build()" not in java.get("JugcraftAgriculture", ""):
        err("The Candy Cache must be a Candy Bowl with the same block entity (no second candy store)")

    def variants(block):
        return set((load(ASSETS / "blockstates" / f"{block}.json") or {}).get("variants", {}))
    horizontal = ("north", "east", "south", "west")
    booleans = ("false", "true")
    wanted = {scare["block"]: {f"facing={f},phase={p},powered={w}" for f in horizontal for p in ("ready", "popped", "resetting") for w in booleans},
              contest["runway"]: {"axis=x", "axis=z"},
              contest["table"]: {f"facing={f},open={o}" for f in horizontal for o in booleans},
              bowling["pin"]: {f"down={d},facing={f}" for d in booleans for f in horizontal},
              bowling["scoreboard"]: {f"facing={f}" for f in horizontal},
              ag.CANDY_CACHE["block"]: {f"facing={f},fill={n}" for f in horizontal for n in range(4)},
              dance["block"]: {f"distance={d}" for d in range(dance["reach"] + 1)},
              tag["block"]: {f"facing={f},ringing={r}" for f in horizontal for r in booleans},
              fortune["block"]: {f"facing={f}" for f in horizontal}}
    for block, keys in wanted.items():
        if variants(block) != keys:
            err(f"{block}: blockstate variants differ from its properties")

    lang = load(ASSETS / "lang" / "en_us.json") or {}
    votes = re.search(r"enum Vote \{\s*([A-Z_, ]+?)\s*\}", java.get("JudgesTableBlockEntity", ""))
    taken = re.search(r"enum Taken \{\s*([A-Z_, ]+?)\s*\}", java.get("CandyBowlBlockEntity", ""))
    keys = [f"message.jugcraft.fortune.{n}" for n in range(1, fortune["fortunes"] + 1)]
    keys += [f"message.jugcraft.judges_table.{v.strip().lower()}" for v in (votes.group(1).split(",") if votes else [])]
    keys += [f"message.jugcraft.candy_cache.{t.strip().lower()}" for t in (taken.group(1).split(",") if taken else [])]
    keys += [f"message.jugcraft.candy_cache.{k}" for k in ("filled", "full", "count")]
    for source in ("JudgesTableBlockEntity", "GhostBellBlockEntity", "BowlingScoreboardBlock"):
        keys += re.findall(r'"(message\.jugcraft\.[a-z_.]+[a-z_])"', java.get(source, ""))
    if not votes or not taken:
        err("JudgesTableBlockEntity.Vote or CandyBowlBlockEntity.Taken not found")
    for key in keys:
        if key not in lang:
            err(f"Missing words for {key}")
    for key in ("frame", "score", "game_over", "ready"):
        if f"message.jugcraft.bowling_scoreboard.{key}" not in lang:
            err(f"Missing words for the scoreboard's {key}")

    if not (ASSETS / "textures" / "entity" / "dance_floor_glow.png").exists():
        err("Missing entity texture dance_floor_glow")
    for card in range(fortune["cards"]):
        if not (ASSETS / "textures" / "block" / f"fortune_card_{card}.png").exists():
            err(f"Missing tarot card {card}")
    quads = load(ASSETS / "decor11_quads.json") or {}
    for name in ("jump_scare_lid", "jump_scare_ghost", "jump_scare_spring", "bowling_pumpkin", "ghost_bell", "ghost_bell_clapper",
                 "fortune_planchette"):
        if not quads.get(name):
            err(f"decor11_quads.json has no quads for {name}")


def check_decor12(java):
    """Night events: Java matches tools/agriculture.py (the trick-or-treaters' timing, chances, group sizes, distances,
    prank and costumes and their gift table, the toilet paper's reach and strands, the hayride's seats and spooks, the
    bonfire's light, skewers and speed, and toasting's reach and times), every block state has a model, the gift table
    holds the gifts, every message has its words, and the textures and quads the client draws exist."""
    def number(source, name):
        match = re.search(rf"\b{name} = ([\d.]+)[FLD]?;", java.get(source, ""))
        return float(match.group(1)) if match else None

    kids, paper, ride, fire = ag.TRICK_OR_TREATERS, ag.TOILET_PAPER, ag.HAYRIDE, ag.BONFIRE
    expected = {("TrickOrTreaters", "CHECK_TICKS"): kids["check_ticks"], ("TrickOrTreaters", "CHANCE"): kids["chance"],
                ("TrickOrTreaters", "MAX_GROUPS"): kids["max_groups"], ("TrickOrTreaters", "MIN_KIDS"): kids["kids"][0],
                ("TrickOrTreaters", "MAX_KIDS"): kids["kids"][1], ("TrickOrTreaters", "MIN_DISTANCE"): kids["spawn_distance"][0],
                ("TrickOrTreaters", "MAX_DISTANCE"): kids["spawn_distance"][1], ("TrickOrTreaters", "PLAYER_RANGE"): kids["player_range"],
                ("TrickOrTreaters", "GIVE_UP_TICKS"): kids["give_up_ticks"], ("TrickOrTreaters", "WAIT_TICKS"): kids["wait_ticks"],
                ("TrickOrTreaters", "LEAVE_TICKS"): kids["leave_ticks"], ("TrickOrTreaters", "PRANK_REACH"): kids["prank_reach"],
                ("TrickOrTreaters", "STREAMERS"): kids["streamers"], ("ToiletPaperRoll", "STREAMERS"): paper["streamers"],
                ("ToiletPaperRoll", "REACH"): paper["reach"], ("ToiletPaperStreamerBlock", "MAX_LENGTH"): paper["max_length"],
                ("HauntedHayride", "SEATS"): ride["seats"], ("HauntedHayride", "SPOOK_MIN"): ride["spook_ticks"][0],
                ("HauntedHayride", "SPOOK_MAX"): ride["spook_ticks"][1], ("HalloweenBonfireBlock", "LIGHT"): fire["light"],
                ("HalloweenBonfireBlockEntity", "SLOTS"): fire["slots"], ("HalloweenBonfireBlockEntity", "SPEED"): fire["speed"],
                ("MarshmallowStickItem", "BONFIRE_REACH"): fire["reach"], ("MarshmallowStickItem", "CAMPFIRE_REACH"): fire["campfire_reach"],
                ("MarshmallowStickItem", "TOAST_TICKS"): fire["toast_ticks"], ("MarshmallowStickItem", "BURN_TICKS"): fire["burn_ticks"]}
    for (source, name), value in expected.items():
        if number(source, name) is None or abs(number(source, name) - value) > 1e-9:
            err(f"{source}.{name} = {number(source, name)} differs from tools/agriculture.py ({value})")
    source = java.get("TrickOrTreaters", "")
    if f'GIFT_TABLE = "{kids["gift_table"]}"' not in source:
        err("TrickOrTreaters.GIFT_TABLE differs from TRICK_OR_TREATERS' gift table")
    costumes = re.search(r"COSTUMES = List\.of\(([^)]*)\)", source)
    if not costumes or re.findall(r'"([a-z_:]+)"', costumes.group(1)) != kids["costumes"]:
        err("TrickOrTreaters.COSTUMES differs from TRICK_OR_TREATERS' costumes")

    def variants(block):
        return set((load(ASSETS / "blockstates" / f"{block}.json") or {}).get("variants", {}))
    if variants(paper["block"]) != {"draped=false", "draped=true"}:
        err(f"{paper['block']}: blockstate variants differ from its properties")
    if variants(fire["block"]) != {"lit=false", "lit=true"}:
        err(f"{fire['block']}: blockstate variants differ from its properties")

    table = load(DATA / "jugcraft" / "loot_table" / f"{kids['gift_table']}.json") or {}
    gifts = [(entry.get("name"), entry.get("weight")) for pool in table.get("pools", []) for entry in pool.get("entries", [])]
    if gifts != [tuple(gift) for gift in kids["gifts"]]:
        err(f"The trick-or-treaters' gift table differs from TRICK_OR_TREATERS' gifts: {gifts}")

    lang = load(ASSETS / "lang" / "en_us.json") or {}
    for source in ("TrickOrTreaters", "MarshmallowStickItem"):
        for key in re.findall(r'"(message\.jugcraft\.[a-z_.]+[a-z_])"', java.get(source, "")):
            if key not in lang:
                err(f"Missing words for {key}")
    if not (ASSETS / "textures" / "entity" / "bonfire_flame.png").exists():
        err("Missing entity texture bonfire_flame")
    quads = load(ASSETS / "decor12_quads.json") or {}
    for name in ("hayride_wagon", "hayride_lantern"):
        if not quads.get(name):
            err(f"decor12_quads.json has no quads for {name}")


def check_decor13(java):
    """Treats: Java matches tools/agriculture.py (the punch bowl's servings, brewing and light, the barmbrack's slices,
    food and fortunes, the giant candy's designs), every block state has a model, every fortune has its words, and
    the punch's ingredients are tagged."""
    def number(source, name):
        match = re.search(rf"\b{name} = ([\d.]+)[FLD]?;", java.get(source, ""))
        return float(match.group(1)) if match else None

    bowl, brack, candy = ag.PUNCH_BOWL, ag.BARMBRACK, ag.GIANT_CANDY
    expected = {("PunchBowlBlock", "SERVINGS"): bowl["servings"], ("PunchBowlBlock", "PER_BERRY"): bowl["per_berry"],
                ("PunchBowlBlock", "LIGHT"): bowl["light"], ("BarmbrackBlock", "SLICES"): brack["slices"],
                ("BarmbrackBlock", "NUTRITION"): brack["slice_food"][0], ("BarmbrackBlock", "SATURATION"): brack["slice_food"][1]}
    for (source, name), value in expected.items():
        if number(source, name) is None or abs(number(source, name) - value) > 1e-9:
            err(f"{source}.{name} = {number(source, name)} differs from tools/agriculture.py ({value})")
    fortunes = re.search(r"FORTUNES = List\.of\(([^)]*)\)", java.get("BarmbrackBlock", ""))
    if not fortunes or re.findall(r'"([a-z_]+)"', fortunes.group(1)) != brack["fortunes"]:
        err("BarmbrackBlock.FORTUNES differs from BARMBRACK's fortunes")
    designs = re.search(r"enum Design implements StringRepresentable \{\s*([A-Z_, ]+?);", java.get("GiantCandyBlock", ""))
    if not designs or [d.strip().lower() for d in designs.group(1).split(",")] != candy["designs"]:
        err("GiantCandyBlock.Design differs from GIANT_CANDY's designs")
    if f'item("{bowl["punch"]}")' not in java.get("PunchBowlBlock", "") or f'item("{brack["ring"]}")' not in java.get("BarmbrackBlock", ""):
        err("The punch bowl or the barmbrack hands out an item other than tools/agriculture.py's")

    def variants(block):
        return set((load(ASSETS / "blockstates" / f"{block}.json") or {}).get("variants", {}))
    horizontal = ("north", "east", "south", "west")
    wanted = {bowl["block"]: {f"servings={n}" for n in range(bowl["servings"] + 1)},
              brack["block"]: {f"bites={b},facing={f}" for b in range(brack["slices"]) for f in horizontal},
              candy["block"]: {f"design={d},facing={f}" for d in candy["designs"] for f in horizontal}}
    for block, keys in wanted.items():
        if variants(block) != keys:
            err(f"{block}: blockstate variants differ from its properties")

    lang = load(ASSETS / "lang" / "en_us.json") or {}
    for key in ["ring"] + brack["fortunes"]:
        if f"message.jugcraft.{brack['block']}.{key}" not in lang:
            err(f"Missing words for the barmbrack's {key}")
    tag = load(DATA / "jugcraft" / "tags" / "item" / "witchs_brew_ingredients.json") or {}
    if tag.get("values") != bowl["ingredients"]:
        err("Item tag jugcraft:witchs_brew_ingredients differs from PUNCH_BOWL's ingredients")


def check_decor14(java):
    """Costumes: Java matches tools/agriculture.py (the outfits, the trunk's slots and how long its lid stays open), every
    outfit has an equipment asset without layers, boxes in costumes.json on body parts and motions the client knows,
    inside textures that exist, and is a trick-or-treat costume and a costume hat; the trunk's block states have models
    and its messages their words."""
    import decor14_data
    main = java.get("JugcraftAgriculture", "")
    outfits = re.search(r"OUTFITS = List\.of\(([^)]*)\)", main)
    if not outfits or re.findall(r'"([a-z_]+)"', outfits.group(1)) != list(ag.OUTFITS):
        err("JugcraftAgriculture.OUTFITS differs from tools/agriculture.py")
    trunk = ag.COSTUME_TRUNK
    for source, name, value in (("CostumeTrunkBlockEntity", "SLOTS", trunk["slots"]), ("CostumeTrunkBlock", "OPEN_TICKS", trunk["open_ticks"])):
        match = re.search(rf"\b{name} = (\d+);", java.get(source, ""))
        if not match or int(match.group(1)) != value:
            err(f"{source}.{name} differs from tools/agriculture.py ({value})")

    layer = (ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client" / "CostumeLayer.java")
    motions = set(re.findall(r'case "([a-z_]+)" -> ', layer.read_text(encoding="utf-8") if layer.exists() else ""))
    if not set(decor14_data.MOTIONS) <= motions:
        err(f"CostumeLayer doesn't know the motions {sorted(set(decor14_data.MOTIONS) - motions)}")
    costumes = load(ASSETS / "costumes.json") or {}
    if set(costumes) != set(ag.OUTFITS):
        err("costumes.json doesn't hold exactly the outfits")
    tags = {tag: set((load(DATA / "jugcraft" / "tags" / "item" / f"{tag.split(':')[1]}.json") or {}).get("values", []))
            for tag in (ag.COSTUME_TAG, ag.COSTUME_HAT_TAG)}
    for name, info in ag.OUTFITS.items():
        entry = costumes.get(name, {})
        width, height = entry.get("size", [0, 0])
        for key in ("texture", "glow") if info.get("glow") else ("texture",):
            namespace, path = split(entry.get(key, ":"))
            if not (ASSETS.parent / namespace / path).exists():
                err(f"{name}: missing {key} texture {entry.get(key)}")
        for piece in entry.get("pieces", []):
            if piece["part"] not in decor14_data.PARTS or any(j[4] not in decor14_data.MOTIONS or j[3] not in "xyz" for j in piece["joints"]):
                err(f"{name}: a piece on an unknown part, axis or motion")
            for b in piece["boxes"]:
                u, v, w, h, d = b[6:11]
                if u + 2 * (d + w) > width or v + d + h > height:
                    err(f"{name}: a box's faces run off its texture")
        if (load(ASSETS / "equipment" / f"{name}.json") or {}).get("layers") != {}:
            err(f"{name}: its equipment asset must have no layers (CostumeLayer draws it)")
        for tag, values in tags.items():
            if rid_of(name) not in values:
                err(f"{name} is not in {tag}")

    states = set((load(ASSETS / "blockstates" / f"{trunk['block']}.json") or {}).get("variants", {}))
    if states != {f"facing={f},open={o}" for f in ("north", "east", "south", "west") for o in ("false", "true")}:
        err(f"{trunk['block']}: blockstate variants differ from its properties")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    for key in re.findall(r'MESSAGES \+ "([a-z_]+)"', java.get("CostumeTrunkBlock", "")):
        if f"message.jugcraft.{trunk['block']}.{key}" not in lang:
            err(f"Missing words for the trunk's {key}")


def check_chandlery(java):
    """The chandlery: Java matches tools/agriculture.py (the pot's capacity, melting, setting and cooling times, scents
    and burn factors; the candle's layers, pulse, effect time, radii, light and harvest; each wax's measures, burn and
    colour; each scent's effect and colour), the tags hold the items that melt, scent, brighten and extend, every block
    state has its blockstate entry, and every message and tooltip has its words."""
    ch = ag.CHANDLERY

    def number(source, name):
        match = re.search(rf"\b{name} = ([\d.]+)[FLD]?;", java.get(source, ""))
        return float(match.group(1)) if match else None

    expected = {("WaxPotBlockEntity", "CAPACITY"): ch["capacity"], ("WaxPotBlockEntity", "MELT_TICKS"): ch["melt_ticks"],
                ("WaxPotBlockEntity", "SET_TICKS"): ch["set_ticks"], ("WaxPotBlockEntity", "COOL_TICKS"): ch["cool_ticks"],
                ("WaxPotBlockEntity", "MAX_SCENTS"): ch["max_scents"], ("WaxPotBlockEntity", "BRIGHT_BURN"): ch["bright_burn"],
                ("WaxPotBlockEntity", "LONG_BURN"): ch["long_burn"], ("AuraCandleBlock", "MAX_DIPS"): ch["max_dips"],
                ("AuraCandleBlock", "PULSE_TICKS"): ch["pulse_ticks"], ("AuraCandleBlock", "EFFECT_TICKS"): ch["effect_ticks"],
                ("AuraCandleBlock", "HARVEST_DIVISOR"): ch["harvest_divisor"]}
    for (source, name), value in expected.items():
        if number(source, name) is None or abs(number(source, name) - value) > 1e-9:
            err(f"{source}.{name} = {number(source, name)} differs from tools/agriculture.py ({value})")
    candle = java.get("AuraCandleBlock", "")
    for name, key in (("RADIUS", "radius"), ("LIGHT", "light")):
        match = re.search(rf"\b{name} = \{{([\d, ]+)\}};", candle)
        if not match or [int(v) for v in match.group(1).split(",")] != ch[key]:
            err(f"AuraCandleBlock.{name} differs from tools/agriculture.py {ch[key]}")
    waxes = {name.lower(): (int(m), int(b), int(c, 16)) for name, m, b, c in
             re.findall(r"\b([A-Z]+)\((\d+), (\d+), 0x([0-9A-F]{6})\)", java.get("CandleWax", ""))}
    if waxes != {name: (info["measures"], info["burn_per_dip"], info["color"]) for name, info in ch["waxes"].items()}:
        err(f"CandleWax {waxes} differs from tools/agriculture.py")
    scents = {name.lower(): (None if effect == "null" else effect.split(".")[1], int(c, 16)) for name, effect, c in
              re.findall(r"\b([A-Z]+)\((null|MobEffects\.[A-Z_]+), 0x([0-9A-F]{6})\)", java.get("CandleScent", ""))}
    if scents != {name: (info["effect"], info["color"]) for name, info in ch["scents"].items()}:
        err(f"CandleScent {scents} differs from tools/agriculture.py")

    def tag(path):
        return (load(DATA / "jugcraft" / "tags" / "item" / f"{path}.json") or {}).get("values")
    for name, info in ch["waxes"].items():
        if tag(f"candle_wax/{name}") != info["items"]:
            err(f"Item tag jugcraft:candle_wax/{name} differs from tools/agriculture.py")
    for name, info in ch["scents"].items():
        if tag(f"candle_scents/{name}") != info["items"]:
            err(f"Item tag jugcraft:candle_scents/{name} differs from tools/agriculture.py")
    for kind in ("brightener", "extender"):
        if tag(ch[kind]["tag"].split(":")[1]) != ch[kind]["items"]:
            err(f"Item tag {ch[kind]['tag']} differs from tools/agriculture.py")

    states = set((load(ASSETS / "blockstates" / f"{ch['candle']}.json") or {}).get("variants", {}))
    if states != {f"dips={d},lit={lit}" for d in range(1, ch["max_dips"] + 1) for lit in ("false", "true")}:
        err(f"{ch['candle']}: blockstate variants differ from its properties")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    keys = [f"message.jugcraft.{ch['pot']}.{k}" for k in re.findall(r'MESSAGES \+ "([a-z_]+)"', java.get("WaxPotBlock", ""))]
    keys += [f"tooltip.jugcraft.{ch['candle']}.{k}" for k in re.findall(r'TOOLTIP \+ "([a-z_]+)"', java.get("AuraCandleItem", ""))]
    keys += [f"item.jugcraft.{ch['candle']}.{k}" for k in ("plain", "one", "two", "muddled")]
    keys += [f"candle_wax.jugcraft.{w}" for w in ch["waxes"]] + [f"candle_scent.jugcraft.{s}" for s in ch["scents"]]
    for key in keys:
        if key not in lang:
            err(f"Missing words for {key}")



def check_cider(java):
    """The cider mill: Java matches CIDER in tools/agriculture.py (the apple leaves' fruiting and picking, the press's
    capacity, trough, turns, pacing and pomace, the barrel's capacity, ageing times and stages), the apple tag holds its
    items, every block state has its blockstate entry, every message has its words, and the press and barrel loot keep
    what they should."""
    tree, press, barrel = ag.CIDER["tree"], ag.CIDER["press"], ag.CIDER["barrel"]

    def numbers(source, names):
        return {name: int(value) for name, value in re.findall(rf"int ({names}) = (\d+);", java.get(source, ""))}

    if numbers("AppleLeavesBlock", "FRUIT_CHANCE|PICK_MIN|PICK_MAX") != {"FRUIT_CHANCE": tree["fruit_chance"],
                                                                         "PICK_MIN": tree["pick"]["min"], "PICK_MAX": tree["pick"]["max"]}:
        err("AppleLeavesBlock.java differs from CIDER['tree'] in tools/agriculture.py")
    if "Items.APPLE" not in java.get("AppleLeavesBlock", "") or tree["pick"]["item"] != "minecraft:apple":
        err("AppleLeavesBlock.java must pick the apple CIDER['tree'] names")
    expected = {"CAPACITY": press["capacity"], "TROUGH": press["trough"], "TURNS": press["turns"], "WORK_TICKS": press["work_ticks"],
                "APPLES_PER_POMACE": press["apples_per_pomace"]}
    if numbers("CiderPressBlockEntity", "|".join(expected)) != expected:
        err("CiderPressBlockEntity.java differs from CIDER['press'] in tools/agriculture.py")
    if f'Jugcraft.id("{press["apples"].split(":")[1]}")' not in java.get("CiderPressBlock", ""):
        err("CiderPressBlock.java does not read the apple tag CIDER['press'] names")
    expected = {"CAPACITY": barrel["capacity"], "SPARKLING_TICKS": barrel["sparkling_ticks"], "AGED_TICKS": barrel["aged_ticks"]}
    entity = java.get("CiderBarrelBlockEntity", "")
    if numbers("CiderBarrelBlockEntity", "|".join(expected)) != expected:
        err("CiderBarrelBlockEntity.java differs from CIDER['barrel'] in tools/agriculture.py")
    stages = re.search(r"STAGES = List\.of\(([^)]*)\)", entity)
    if not stages or re.findall(r'"([a-z_]+)"', stages.group(1)) != barrel["stages"]:
        err("CiderBarrelBlockEntity.STAGES differs from CIDER['barrel'] in tools/agriculture.py")
    for stage in barrel["stages"] + ["mulled_cider"]:
        if "drink" not in ag.ITEMS.get(stage, {}):
            err(f"{stage}: every cider is a drink in ITEMS")
    if (load(DATA / "jugcraft" / "tags" / "item" / f"{press['apples'].split(':')[1]}.json") or {}).get("values") != press["apple_items"]:
        err(f"Item tag {press['apples']} differs from tools/agriculture.py")

    def variants(block):
        return set((load(ASSETS / "blockstates" / f"{block}.json") or {}).get("variants", {}))
    facings = ("north", "south", "east", "west")
    if variants(press["block"]) != {f"facing={f}" for f in facings}:
        err(f"{press['block']}: blockstate does not cover every facing")
    if variants(barrel["block"]) != {f"cider={c},facing={f}" for c in range(len(barrel["stages"]) + 1) for f in facings}:
        err(f"{barrel['block']}: blockstate does not cover every stage and facing")
    if variants(tree["leaves"]) != {f"fruit={f}" for f in range(3)}:
        err(f"{tree['leaves']}: blockstate does not cover every fruit stage")
    cider = re.search(r'IntegerProperty\.create\("cider", 0, (\d+)\)', java.get("CiderBarrelBlock", ""))
    if not cider or int(cider.group(1)) != len(barrel["stages"]):
        err("CiderBarrelBlock.CIDER must run from 0 (empty) to the number of stages")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    keys = [f"message.jugcraft.{press['block']}.{k}" for k in re.findall(r'MESSAGES \+ (?:\([^"]*)?"([a-z_]+)"', java.get("CiderPressBlock", ""))]
    keys += [f"message.jugcraft.{press['block']}.{k}" for k in ("full", "pressing")]
    keys += [f"message.jugcraft.{barrel['block']}.{k}" for k in re.findall(r'MESSAGES \+ (?:\([^"]*)?"([a-z_]+)"', java.get("CiderBarrelBlock", ""))]
    keys += [f"message.jugcraft.{barrel['block']}.{k}" for k in ("full", "fermenting")]
    keys += ["tooltip.jugcraft.cider_barrel.servings"]
    for key in keys:
        if key not in lang:
            err(f"Missing words for {key}")
    table = load(DATA / "jugcraft" / "loot_table" / "blocks" / f"{barrel['block']}.json") or {}
    if "jugcraft:barrel_cider" not in json.dumps(table):
        err(f"{barrel['block']}: its loot must keep its cider (copy jugcraft:barrel_cider)")


def check_pantry(java, main):
    """The preserves pantry: Java matches PANTRY in tools/agriculture.py (servings, spoiling time, the kettle's jars and
    timings, the shelf's slots, and every preserve's food, effect and colour), every preserve is cooked into a Mason Jar in
    the Cooking Pot, its item model shows the cloth cap once sealed, and every message and tooltip has its words."""
    pantry = ag.PANTRY

    def numbers(source, names):
        return {name: int(value) for name, value in re.findall(rf"int ({names}) = (\d+);", java.get(source, ""))}

    for source, expected in (("PreserveJarItem", {"SERVINGS": pantry["servings"], "SPOIL_TICKS": pantry["spoil_ticks"]}),
                             ("CanningKettleBlockEntity", {"JARS": pantry["kettle_jars"], "BOIL_TICKS": pantry["boil_ticks"],
                                                           "PROCESS_TICKS": pantry["process_ticks"]}),
                             ("PantryShelfBlockEntity", {"SLOTS": pantry["shelf_slots"]})):
        if numbers(source, "|".join(expected)) != expected:
            err(f"{source}.java differs from PANTRY in tools/agriculture.py")
    registered = {name: (int(n), float(sat), None if effect == "null" else [effect.split(".")[1], int(seconds)], int(color, 16))
                  for name, n, sat, effect, seconds, color in re.findall(
                      r'\bpreserve\("([a-z_]+)", (\d+), ([\d.]+)F, (null|MobEffects\.\w+), (\d+), 0x([0-9A-F]{6})\)', main)}
    expected = {name: (info["food"][0], info["food"][1], info["effect"], info["color"]) for name, info in pantry["preserves"].items()}
    if registered != expected:
        err(f"JugcraftAgriculture.java preserves {registered} differ from PANTRY in tools/agriculture.py")
    jar = f"jugcraft:{pantry['jar']}"
    for preserve in pantry["preserves"]:
        recipe = ag.POT_RECIPES.get(preserve)
        if not recipe or recipe["inputs"].get(jar) != 1 or recipe.get("count", 1) != 1:
            err(f"{preserve}: must be cooked into one Mason Jar in the Cooking Pot")
        model = (load(ASSETS / "items" / f"{preserve}.json") or {}).get("model", {})
        if model.get("type") != "minecraft:condition" or model.get("component") != "jugcraft:sealed":
            err(f"{preserve}: its item model must show the sealed jar (a condition on jugcraft:sealed)")
    if pantry["vinegar"] not in ag.POT_RECIPES:
        err("Cider vinegar must be cooked in the Cooking Pot")
    if f'registerItem("{pantry["vinegar"]}", Item::new, new Item.Properties().craftRemainder(Items.GLASS_BOTTLE)' not in main:
        err("Cider vinegar must give its bottle back when cooked into preserves")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    keys = [f"message.jugcraft.{pantry['kettle']}.{k}" for k in re.findall(r'MESSAGES \+ (?:\([^"]*)?"([a-z_]+)"', java.get("CanningKettleBlock", ""))]
    keys += [f"message.jugcraft.{pantry['kettle']}.{k}" for k in ("no_water", "already_sealed", "opened", "full")]
    keys += [f"message.jugcraft.{pantry['shelf']}.{k}" for k in re.findall(r'MESSAGES \+ "([a-z_]+)"', java.get("PantryShelfBlock", ""))]
    keys += [f"tooltip.jugcraft.preserves.{k}" for k in re.findall(r'TOOLTIP \+ "([a-z_]+)"', java.get("PreserveJarItem", ""))]
    for key in keys:
        if key not in lang:
            err(f"Missing words for {key}")


def check_crows(java, main):
    """Crows and working scarecrows: Crow.java, Crows.java and Scarecrows.java match CROWS in tools/agriculture.py; the
    crow is registered with its attributes, named, and drops feathers."""
    crows = ag.CROWS

    def numbers(source):
        return {name: float(value) for name, value in re.findall(r"static final (?:int|double|float) ([A-Z_]+) = ([\d.]+)[FD]?;",
                                                                    java.get(source, ""))}

    expected = {"Crow": {"FLEE_RADIUS": crows["flee_radius"], "SNEAK_FLEE_RADIUS": crows["sneak_flee_radius"], "RAID_RADIUS": crows["raid_radius"],
                         "SEARCH_TRIES": crows["search_tries"], "PECK_TICKS": crows["peck_ticks"], "SETBACK": crows["setback"],
                         "RAID_COOLDOWN": crows["raid_cooldown"], "RAID_COOLDOWN_SPREAD": crows["raid_cooldown_spread"],
                         "LEAVE_HEIGHT": crows["leave_height"], "LEAVE_TICKS": crows["leave_ticks"],
                         "LOOK_TICKS": crows["look_ticks"]},
                "Crows": {"SPAWN_TICKS": crows["spawn_ticks"], "SPAWN_CHANCE": crows["spawn_chance"], "MIN_DISTANCE": crows["min_distance"],
                          "MAX_DISTANCE": crows["max_distance"], "FIELD_RADIUS": crows["field_radius"], "FIELD_TRIES": crows["field_tries"],
                          "FLOCK_MIN": crows["flock"][0], "FLOCK_MAX": crows["flock"][1], "NEAR_CAP": crows["near_cap"],
                          "NEAR_RANGE": crows["near_range"], "LEVEL_CAP": crows["level_cap"], "DAY_END": crows["day_end"]},
                "Scarecrows": {"BARE": crows["guard"]["bare"], "HEADED": crows["guard"]["headed"], "LIT": crows["guard"]["lit"],
                               "HEIGHT": crows["guard_height"]}}
    for source, values in expected.items():
        found = numbers(source)
        for name, value in values.items():
            if name not in found or abs(found[name] - value) > 1e-6:
                err(f"{source}.{name} = {found.get(name)} differs from CROWS in tools/agriculture.py ({value})")
    if f"Attributes.MAX_HEALTH, {crows['health']})" not in java.get("Crow", ""):
        err("Crow.java health differs from CROWS in tools/agriculture.py")
    if f'entity("{crows["entity"]}"' not in main or "FabricDefaultAttributeRegistry.register(CROW, Crow.createAttributes())" not in main:
        err("JugcraftAgriculture.java must register the crow and its attributes")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    if lang.get(f"entity.jugcraft.{crows['entity']}") != crows["display"]:
        err("The crow has no name")
    table = load(DATA / "jugcraft" / "loot_table" / f"{crows['table']}.json") or {}
    if "minecraft:feather" not in json.dumps(table):
        err("A crow must drop feathers")

def check_fireworks(java, main):
    """Spooky fireworks: SpookyRocket.java, ShowLauncherBlockEntity.java and FireworkShape.java match FIREWORKS in
    tools/agriculture.py; every picture has a firework item, named and drawn, with a recipe for each flight, plain and
    twinkling, giving `per_craft` rockets of that flight; the launcher has a model for every facing and mode, its words,
    loot and recipe; the spark particle, the burst payload and the dispensing are registered."""
    fw = ag.FIREWORKS

    def numbers(source):
        return {name: float(value) for name, value in re.findall(r"static final (?:int|double|float) ([A-Z_]+) = ([\d.]+)[FD]?;",
                                                                    java.get(source, ""))}

    expected = {"SpookyRocket": {"LIFETIME_BASE": fw["lifetime_base"], "LIFETIME_SPREAD": fw["lifetime_spread"], "CLIMB": fw["climb"]},
                "ShowLauncherBlockEntity": {"TUBES": fw["tubes"], "TUBE_CAPACITY": fw["tube_capacity"], "SEQUENCE_TICKS": fw["sequence_ticks"],
                                            "VOLLEY_TICKS": fw["volley_ticks"], "LEAN": fw["lean"], "VANILLA_LEAN": fw["vanilla_lean"]}}
    for source, values in expected.items():
        found = numbers(source)
        for name, value in values.items():
            if name not in found or abs(found[name] - value) > 1e-9:
                err(f"{source}.{name} = {found.get(name)} differs from FIREWORKS in tools/agriculture.py ({value})")
    shapes_java = re.findall(r'\n\t([A-Z]+)\("([a-z]+)", 0x([0-9A-Fa-f]{6})', java.get("FireworkShape", ""))
    colours = {sid: int(colour, 16) for constant, sid, colour in shapes_java if constant.lower() == sid}
    if set(colours) != set(fw["shapes"]) or len(shapes_java) != len(colours):
        err(f"FireworkShape's pictures {sorted(colours)} differ from FIREWORKS in tools/agriculture.py {sorted(fw['shapes'])}")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    for shape, info in fw["shapes"].items():
        if colours.get(shape) != info["colour"]:
            err(f"FireworkShape {shape}'s colour differs from FIREWORKS in tools/agriculture.py")
        if info["item"] != f"{shape}_firework":
            err(f"The {shape} firework must be {shape}_firework (FireworkShape.item())")
        if lang.get(f"item.jugcraft.{info['item']}") != info["display"] or f"tooltip.jugcraft.spooky_firework.{shape}" not in lang:
            err(f"The {shape} firework has no name or tooltip")
        if not (ASSETS / "textures" / "item" / f"{info['item']}.png").exists():
            err(f"The {shape} firework has no texture")
        for flight in fw["flights"]:
            for twinkle in (False, True):
                name = f"{info['item']}_{flight}" + ("_twinkle" if twinkle else "")
                recipe = load(DATA / "jugcraft" / "recipe" / f"{name}.json") or {}
                result = recipe.get("result", {})
                components = result.get("components", {})
                inputs = recipe.get("ingredients", [])
                if (result.get("id") != f"jugcraft:{info['item']}" or result.get("count") != fw["per_craft"]
                        or components.get("minecraft:fireworks", {}).get("flight_duration") != flight
                        or inputs.count("minecraft:gunpowder") != flight or (fw["twinkle"] in inputs) != twinkle
                        or bool(components.get(f"jugcraft:{fw['component']}")) != twinkle
                        or any(ingredient not in inputs for ingredient in info["ingredients"])):
                    err(f"Recipe {name} must make {fw['per_craft']} {info['item']} of flight {flight}" + (", twinkling" if twinkle else ""))
    launcher = fw["launcher"]
    states = (load(ASSETS / "blockstates" / f"{launcher}.json") or {}).get("variants", {})
    for facing in ("north", "south", "east", "west"):
        for mode in ("sequence", "volley", "finale"):
            if f"facing={facing},mode={mode}" not in states:
                err(f"The Show Launcher's blockstate has no variant for facing={facing},mode={mode}")
    for key in ("full", "started", "stopped", "empty", "mode.sequence", "mode.volley", "mode.finale"):
        if f"message.jugcraft.show_launcher.{key}" not in lang:
            err(f"The Show Launcher has no words for {key}")
    if lang.get(f"block.jugcraft.{launcher}") != fw["launcher_display"] or f"entity.jugcraft.{fw['entity']}" not in lang:
        err("The Show Launcher or the spooky rocket has no name")
    if not (DATA / "jugcraft" / "loot_table" / "blocks" / f"{launcher}.json").exists() or not (DATA / "jugcraft" / "recipe" / f"{launcher}.json").exists():
        err("The Show Launcher needs its loot table and recipe")
    if not (ASSETS / "particles" / f"{fw['particle']}.json").exists() or not (ASSETS / "textures" / "particle" / f"{fw['particle']}.png").exists():
        err("The spooky spark particle needs its definition and texture")
    for needed in (f'Jugcraft.id("{fw["particle"]}"), SPOOKY_SPARK', "SpookyBurstPayload.TYPE, SpookyBurstPayload.CODEC",
                   f'entity("{fw["entity"]}"', "SpookyFireworkItem.registerDispensing()", f'Jugcraft.id("{fw["component"]}")'):
        if needed not in main:
            err(f"JugcraftAgriculture.java must register {needed}")

def check_lanterns(java, main):
    """The sky lantern festival: SkyLantern.java, SkyLanterns.java and MooncakeItem.java match LANTERNS in
    tools/agriculture.py; the lantern is registered, named and crafted, dyed by a dyeing recipe as leather is and washed
    in a cauldron; every mooncake is registered with its
    food, named, drawn and baked in the Cooking Pot; the festival has its message and advancement."""
    lt = ag.LANTERNS

    def numbers(source):
        return {name: float(int(value, 16)) if value.startswith("0x") else float(value) for name, value in
                re.findall(r"static final (?:int|double|float) ([A-Z_]+) = (0x[0-9A-Fa-f]+|[\d.]+)[FD]?;", java.get(source, ""))}

    expected = {"SkyLantern": {"RISE": lt["rise"], "WIND": lt["wind"], "WIND_PERIOD": lt["wind_period"], "LIFETIME": lt["lifetime"],
                               "LIFETIME_SPREAD": lt["lifetime_spread"], "FADE_TICKS": lt["fade_ticks"], "DEFAULT_COLOUR": lt["default_colour"]},
                "SkyLanterns": {"FESTIVAL_LANTERNS": lt["festival_lanterns"], "FESTIVAL_RADIUS": lt["festival_radius"],
                                "FESTIVAL_WINDOW": lt["festival_window"], "FESTIVAL_COOLDOWN": lt["festival_cooldown"],
                                "LUCK_TICKS": lt["luck_ticks"], "MEMORY": lt["memory"]},
                "MooncakeItem": {"LUCK_TICKS": lt["mooncake_luck_ticks"], "NIGHT_START": lt["night"][0], "NIGHT_END": lt["night"][1]}}
    for source, values in expected.items():
        found = numbers(source)
        for name, value in values.items():
            if name not in found or abs(found[name] - value) > 1e-9:
                err(f"{source}.{name} = {found.get(name)} differs from LANTERNS in tools/agriculture.py ({value})")
    listed = re.search(r'MOONCAKES = List\.of\(([^)]*)\)', main)
    if not listed or re.findall(r'"([a-z_]+)"', listed.group(1)) != list(lt["mooncakes"]):
        err("JugcraftAgriculture.MOONCAKES differs from LANTERNS in tools/agriculture.py")
    food = lt["mooncake_food"]
    if f"MooncakeItem::new, new Item.Properties().food(nourishment({food[0]}, {food[1]}F))" not in main:
        err("The mooncakes' food differs from LANTERNS in tools/agriculture.py")
    for needed in (f'registerItem("{lt["item"]}", SkyLanternItem::new', f'entity("{lt["entity"]}"', "SkyLanterns.register()"):
        if needed not in main:
            err(f"JugcraftAgriculture.java must register {needed}")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    if lang.get(f"item.jugcraft.{lt['item']}") != lt["display"] or "message.jugcraft.sky_lantern.festival" not in lang:
        err("The sky lantern has no name, or the festival no message")
    # Minecraft 26.3 dyes leather by a recipe per item, not by the minecraft:dyeable tag; the lantern is dyed the same way.
    dyed = load(DATA / "jugcraft" / "recipe" / f"{lt['item']}_dyed.json") or {}
    if (dyed.get("type") != lt["dye_recipe"] or dyed.get("target") != f"jugcraft:{lt['item']}"
            or dyed.get("dye") != "#minecraft:dyes" or dyed.get("result", {}).get("id") != f"jugcraft:{lt['item']}"):
        err(f"The sky lantern needs its dyeing recipe ({lt['dye_recipe']}: the lantern and any dye give it back)")
    wash_tag = lt["wash_tag"].split(":")
    washes = (load(DATA / wash_tag[0] / "tags" / "item" / f"{wash_tag[1]}.json") or {}).get("values", [])
    if f"jugcraft:{lt['item']}" not in washes:
        err(f"The sky lantern must be in {lt['wash_tag']}, so a cauldron washes its dye out")
    dyeable = DATA / "minecraft" / "tags" / "item" / "dyeable.json"
    if dyeable.exists() and f"jugcraft:{lt['item']}" in (load(dyeable) or {}).get("values", []):
        err("The sky lantern is dyed by its recipe; minecraft:dyeable doesn't dye in 26.3")
    recipe = load(DATA / "jugcraft" / "recipe" / f"{lt['item']}.json") or {}
    if recipe.get("result", {}).get("count") != lt["per_craft"]:
        err(f"The sky lantern's recipe must make {lt['per_craft']}")
    for cake, info in lt["mooncakes"].items():
        if lang.get(f"item.jugcraft.{cake}") != info["display"] or not (ASSETS / "textures" / "item" / f"{cake}.png").exists():
            err(f"{cake} has no name or texture")
        baked = load(DATA / "jugcraft" / "recipe" / "pot_cooking" / f"{cake}.json") or {}
        if baked.get("result", {}).get("count") != lt["mooncake_count"]:
            err(f"{cake} must bake {lt['mooncake_count']} at a time in the Cooking Pot")
    if not (DATA / "jugcraft" / "advancement" / "lantern_festival.json").exists():
        err("The lantern festival needs its advancement")

def check_feast(java, main):
    """The harvest feast: FeastTableBlockEntity.java and Feasts.java match FEAST in tools/agriculture.py; the table is
    registered, has a model for every axis and part, its words for every tier, loot, recipe and advancement."""
    fe = ag.FEAST

    def numbers(source):
        return {name: float(value) for name, value in re.findall(r"static final (?:int|double|float) ([A-Z_]+) = ([\d.]+)[FD]?;",
                                                                    java.get(source, ""))}

    expected = {"FeastTableBlockEntity": {"DISHES": fe["dishes"], "SERVINGS": fe["servings"], "WINDOW": fe["window"], "MAX_LENGTH": fe["max_length"]},
                "Feasts": {"GOOD_MEAL": fe["tiers"][0], "FEAST": fe["tiers"][1], "HARVEST_FEAST": fe["tiers"][2], "GRAND_FEAST": fe["tiers"][3],
                           "REGENERATION_TICKS": fe["regeneration_ticks"], "ABSORPTION_TICKS": fe["absorption_ticks"],
                           "LONG_TICKS": fe["long_ticks"], "REACH": fe["reach"]}}
    for source, values in expected.items():
        found = numbers(source)
        for name, value in values.items():
            if name not in found or abs(found[name] - value) > 1e-9:
                err(f"{source}.{name} = {found.get(name)} differs from FEAST in tools/agriculture.py ({value})")
    if f'registerBlock("{fe["block"]}", FeastTableBlock::new' not in main:
        err("JugcraftAgriculture.java must register the feast table")
    states = (load(ASSETS / "blockstates" / f"{fe['block']}.json") or {}).get("variants", {})
    for axis in ("x", "z"):
        for part in ("single", "start", "middle", "end"):
            if f"axis={axis},part={part}" not in states:
                err(f"The feast table's blockstate has no variant for axis={axis},part={part}")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    for key in ["dish_taken", "empty"] + [f"tier.{tier}" for tier in range(5)]:
        if f"message.jugcraft.feast_table.{key}" not in lang:
            err(f"The feast table has no words for {key}")
    if lang.get(f"block.jugcraft.{fe['block']}") != fe["display"]:
        err("The feast table has no name")
    recipe = load(DATA / "jugcraft" / "recipe" / f"{fe['block']}.json") or {}
    if recipe.get("result", {}).get("count") != fe["per_craft"] or not (DATA / "jugcraft" / "loot_table" / "blocks" / f"{fe['block']}.json").exists():
        err(f"The feast table needs its loot table and a recipe making {fe['per_craft']}")
    if not (DATA / "jugcraft" / "advancement" / "harvest_home.json").exists():
        err("A grand feast needs its advancement")

def check_maze(java, main):
    """The corn maze: CornMaze.java and CornMazeGateBlockEntity.java match MAZE in tools/agriculture.py; the gate, finish
    post and maze corn are registered with models for every state, the words for every message and size, loot (maze corn
    giving back its kernel from the bottom only), the gate's recipe and the advancement."""
    mz = ag.MAZE
    maze = java.get("CornMaze", "")
    cells = re.search(r"CELLS = \{([^}]*)\}", maze)
    sizes = re.search(r"SIZES = \{([^}]*)\}", maze)
    if not cells or [int(v) for v in cells.group(1).split(",")] != mz["cells"] or not sizes or re.findall(r'"([a-z]+)"', sizes.group(1)) != mz["sizes"]:
        err("CornMaze's sizes differ from MAZE in tools/agriculture.py")
    found = {name: float(value) for name, value in re.findall(r"static final (?:int|double) ([A-Z_]+) = ([\d.]+);", java.get("CornMazeGateBlockEntity", ""))}
    for name, value in {"PLANT_PER_TICK": mz["plant_per_tick"], "MAX_RUN": mz["max_run"], "SHORTCUT": mz["shortcut"],
                        "MAX_RUNNERS": mz["max_runners"]}.items():
        if name not in found or abs(found[name] - value) > 1e-9:
            err(f"CornMazeGateBlockEntity.{name} = {found.get(name)} differs from MAZE in tools/agriculture.py ({value})")
    for needed in (f'registerBlock("{mz["corn"]}", MazeCornBlock::new', f'registerBlock("{mz["gate"]}", CornMazeGateBlock::new',
                   f'registerBlock("{mz["finish"]}", CornMazeFinishBlock::new'):
        if needed not in main:
            err(f"JugcraftAgriculture.java must register {needed}")
    corn_states = (load(ASSETS / "blockstates" / f"{mz['corn']}.json") or {}).get("variants", {})
    if sorted(corn_states) != [f"section={s}" for s in range(3)]:
        err("Maze corn needs a model for each of its three sections")
    for block in (mz["gate"], mz["finish"]):
        states = (load(ASSETS / "blockstates" / f"{block}.json") or {}).get("variants", {})
        if sorted(states) != sorted(f"facing={f}" for f in ("north", "south", "east", "west")):
            err(f"{block} needs a model for every facing")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    keys = ["go", "shortcut", "finished", "planted", "plan", "size_set", "needs", "blocked", "already", "planting"]
    keys += [f"void.{why}" for why in ("flew", "climbed", "left", "slow")] + [f"size.{size}" for size in mz["sizes"]]
    for key in keys:
        if f"message.jugcraft.corn_maze.{key}" not in lang:
            err(f"The corn maze has no words for {key}")
    corn_loot = json.dumps(load(DATA / "jugcraft" / "loot_table" / "blocks" / f"{mz['corn']}.json") or {})
    if f"jugcraft:{mz['kernel']}" not in corn_loot or '"section": "0"' not in corn_loot:
        err("Maze corn must give back its kernel, from its bottom section only")
    if not (DATA / "jugcraft" / "recipe" / f"{mz['gate']}.json").exists() or not (DATA / "jugcraft" / "advancement" / "amazing.json").exists():
        err("The corn maze gate needs its recipe, and finishing a maze its advancement")

def check_ghosts(java, main):
    """Ghost hunting: Spirits.java and RestlessSpirit.java match GHOSTS in tools/agriculture.py; the spirit is registered
    with its attributes and no loot, named; the Spirit Lantern and Ectoplasm are registered, named and drawn, Ectoplasm
    giving its bottle back and scenting wax (the Ghostly scent); the lantern has its recipe and tooltip; every grave takes
    random ticks and stirs; catching a spirit has its advancement."""
    gh = ag.GHOSTS

    def numbers(source):
        return {name: float(value) for name, value in re.findall(r"static final (?:int|double|float) ([A-Z_]+) = ([\d.]+)[FD]?;",
                                                                    java.get(source, ""))}

    expected = {"Spirits": {"STIR_CHANCE": gh["stir_chance"], "NEAR_CAP": gh["near_cap"], "NEAR_RANGE": gh["near_range"],
                            "REVEAL_RADIUS": gh["reveal_radius"]},
                "RestlessSpirit": {"HAUNT_RADIUS": gh["haunt_radius"], "HAUNT_HEIGHT": gh["haunt_height"], "SHY_RADIUS": gh["shy_radius"],
                                   "SNEAK_SHY_RADIUS": gh["sneak_shy_radius"], "DRIFT_SPEED": gh["drift_speed"], "SHY_SPEED": gh["shy_speed"],
                                   "REVEAL_TICKS": gh["reveal_ticks"], "LOOK_TICKS": gh["look_ticks"], "FADE_TICKS": gh["fade_ticks"]}}
    for source, values in expected.items():
        found = numbers(source)
        for name, value in values.items():
            if name not in found or abs(found[name] - value) > 1e-9:
                err(f"{source}.{name} = {found.get(name)} differs from GHOSTS in tools/agriculture.py ({value})")
    if f'entity("{gh["entity"]}", EntityType.Builder.<RestlessSpirit>of(RestlessSpirit::new' not in main \
            or "FabricDefaultAttributeRegistry.register(RESTLESS_SPIRIT" not in main:
        err("JugcraftAgriculture.java must register the restless spirit and its attributes")
    if f'registerItem("{gh["lantern"]}", SpiritLanternItem::new' not in main \
            or f'registerItem("{gh["ectoplasm"]}", Item::new, new Item.Properties().craftRemainder(Items.GLASS_BOTTLE)' not in main:
        err("JugcraftAgriculture.java must register the Spirit Lantern, and Ectoplasm giving back its glass bottle")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    for key, display in ((f"entity.jugcraft.{gh['entity']}", gh["display"]), (f"item.jugcraft.{gh['lantern']}", gh["lantern_display"]),
                         (f"item.jugcraft.{gh['ectoplasm']}", gh["ectoplasm_display"])):
        if lang.get(key) != display:
            err(f"{key} must be named {display!r}")
    if "item.jugcraft.spirit_lantern.tooltip" not in lang:
        err("The Spirit Lantern has no tooltip")
    for item in (gh["lantern"], gh["ectoplasm"]):
        if not (ASSETS / "textures" / "item" / f"{item}.png").exists() or not (ASSETS / "items" / f"{item}.json").exists():
            err(f"{item} needs its texture and item model")
    if not (ASSETS / "textures" / "entity" / f"{gh['entity']}.png").exists():
        err("The restless spirit needs its texture")
    if f"jugcraft:{gh['ectoplasm']}" not in ag.CHANDLERY["scents"].get("ghostly", {}).get("items", []):
        err("Ectoplasm must be the Ghostly candle scent")
    if not (DATA / "jugcraft" / "recipe" / f"{gh['lantern']}.json").exists() or not (DATA / "jugcraft" / "advancement" / "ghost_hunter.json").exists():
        err("The Spirit Lantern needs its recipe, and catching a spirit its advancement")
    if main.count(".noOcclusion().randomTicks());") < 2:
        err("The gravestones and the grave mound must take random ticks, so graves stir")
    for source in ("GravestoneBlock", "GraveMoundBlock"):
        if "Spirits.stir(level, pos, random)" not in java.get(source, ""):
            err(f"{source} must stir at night (Spirits.stir in randomTick)")

def check_face_paint(java, main):
    """Face paint: FacePaint.java and FacePaintKitItem.java match FACE_PAINT in tools/agriculture.py (the designs in order,
    the uses, the time to paint your own face, how often paint washes off); the kit, its design component and the paint
    attachment are registered; every design has its name and its texture; the kit has its texture, words, recipe and
    advancement; and a painted face counts as a costume for trick-or-treating and the costume contest."""
    fp = ag.FACE_PAINT
    paint = java.get("FacePaint", "")
    designs = re.search(r"enum Design implements StringRepresentable \{\s*([A-Z_,\s]+);", paint)
    if not designs or [d.strip().lower() for d in designs.group(1).split(",")] != list(fp["designs"]):
        err("FacePaint.Design differs from FACE_PAINT['designs'] in tools/agriculture.py")
    found = {name: float(value) for source in ("FacePaint", "FacePaintKitItem")
             for name, value in re.findall(r"static final int ([A-Z_]+) = (\d+);", java.get(source, ""))}
    for name, value in {"WASH_TICKS": fp["wash_ticks"], "USES": fp["uses"], "USE_TICKS": fp["use_ticks"]}.items():
        if found.get(name) != value:
            err(f"{name} = {found.get(name)} differs from FACE_PAINT in tools/agriculture.py ({value})")
    if f'registerItem("{fp["kit"]}", FacePaintKitItem::new, new Item.Properties().durability(FacePaintKitItem.USES)' not in main \
            or f'Jugcraft.id("{fp["component"]}")' not in main:
        err("JugcraftAgriculture.java must register the Face Paint Kit (worn by use) and its design component")
    if f'buildAndRegister(Jugcraft.id("{fp["attachment"]}"))' not in paint or "syncWith(" not in paint:
        err("FacePaint must register the face paint attachment, sent to the clients that see the player")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    keys = [f"item.jugcraft.{fp['kit']}", "item.jugcraft.face_paint_kit.design", "item.jugcraft.face_paint_kit.hint",
            "message.jugcraft.face_paint.design", "message.jugcraft.face_paint.painted", "message.jugcraft.face_paint.washed"]
    for key in keys + [f"face_paint.jugcraft.{design}" for design in fp["designs"]]:
        if key not in lang:
            err(f"Face paint has no words for {key}")
    for design in fp["designs"]:
        if not (ASSETS / "textures" / "entity" / "face_paint" / f"{design}.png").exists():
            err(f"The {design} face paint needs its texture")
    if not (ASSETS / "textures" / "item" / f"{fp['kit']}.png").exists() or not (DATA / "jugcraft" / "recipe" / f"{fp['kit']}.json").exists() \
            or not (DATA / "jugcraft" / "advancement" / "face_painter.json").exists():
        err("The Face Paint Kit needs its texture, recipe and advancement")
    for source in ("TrickOrTreat", "JudgesTableBlockEntity"):
        if "FacePaint.inCostume(player)" not in java.get(source, ""):
            err(f"{source} must count a painted face as a costume (FacePaint.inCostume)")

def check_candy(java, main):
    """The candy kitchen: the kettle's, tray's and candies' Java matches CANDY in tools/agriculture.py (the batch limits,
    temperatures and heating rates; the tray's setting, pulling, crystal and layer times; the eating time and candy corn's
    bands; the stages and where each starts; which candy each base sets into at each stage; each kind's colour; each
    flavour's effect, time and colour; the candies' food), the kettle, tray, candies and batch component are registered,
    and every candy, stage, base and flavour has its words, textures and tags; the kettle and tray have recipes, and the
    two advancements exist."""
    cd = ag.CANDY

    def number(source, name):
        match = re.search(rf"\b{name} = (-?[\d.]+)[FLD]?;", java.get(source, ""))
        return float(match.group(1)) if match else None

    expected = {("CandyKettleBlockEntity", "MAX_SUGAR"): cd["max_sugar"], ("CandyKettleBlockEntity", "PIECES_PER_SUGAR"): cd["pieces_per_sugar"],
                ("CandyKettleBlockEntity", "MAX_FLAVOURS"): cd["max_flavours"], ("CandyKettleBlockEntity", "ROOM"): cd["room"],
                ("CandyKettleBlockEntity", "BOIL"): cd["boil"], ("CandyKettleBlockEntity", "BOILED"): cd["boiled"],
                ("CandyKettleBlockEntity", "MAX_TEMP"): cd["max_temp"], ("CandyKettleBlockEntity", "ADD_BELOW"): cd["add_below"],
                ("CandyKettleBlockEntity", "HEAT_TICKS"): cd["heat_ticks"], ("CandyKettleBlockEntity", "BOIL_TICKS"): cd["boil_ticks"],
                ("CandyKettleBlockEntity", "COOK_TICKS"): cd["cook_ticks"], ("CandyKettleBlockEntity", "COOL_TICKS"): cd["cool_ticks"],
                ("CandyTrayItem", "SET_TICKS"): cd["set_ticks"], ("CandyTrayItem", "WARM_TICKS"): cd["warm_ticks"],
                ("CandyTrayItem", "PULL_TICKS"): cd["pull_ticks"], ("CandyTrayItem", "PULLS"): cd["pulls"],
                ("CandyTrayItem", "CRYSTAL_TICKS"): cd["crystal_ticks"], ("CandyTrayItem", "MAX_LAYERS"): cd["max_layers"],
                ("Candies", "EAT_SECONDS"): cd["eat_seconds"]}
    for (source, name), value in expected.items():
        if number(source, name) is None or abs(number(source, name) - value) > 1e-9:
            err(f"{source}.{name} = {number(source, name)} differs from CANDY in tools/agriculture.py ({value})")
    bands = re.search(r"CORN_BANDS = \{([^}]+)\};", java.get("Candies", ""))
    if not bands or [int(v.strip(), 16) for v in bands.group(1).replace("0x", "").split(",")] != cd["corn_bands"]:
        err("Candies.CORN_BANDS differs from CANDY['corn_bands'] in tools/agriculture.py")
    stages = {name.lower(): int(start) for name, start in re.findall(r"\b([A-Z_]+)\((\d+)\)", java.get("CandyStage", ""))}
    if stages != {name: start for name, (_, start) in cd["stages"].items()}:
        err(f"CandyStage {stages} differs from CANDY['stages'] in tools/agriculture.py")
    makes = re.search(r"MAKES = \{\s*\{([^}]*)\},\s*\{([^}]*)\}\};", java.get("CandyBase", ""))
    if not makes:
        err("CandyBase.MAKES not found")
    else:
        for base, row in zip(("syrup", "cream"), makes.groups()):
            found = [None if v.strip() == "null" else v.strip().split(".")[1].lower() for v in row.split(",")]
            if found != cd["makes"][base]:
                err(f"CandyBase.MAKES for {base} {found} differs from CANDY['makes'] in tools/agriculture.py")
    kinds = {name.lower(): int(c, 16) for name, c in re.findall(r"\b([A-Z_]+)\(0x([0-9A-F]{6})\)", java.get("CandyKind", ""))}
    if kinds != cd["kinds"]:
        err(f"CandyKind {kinds} differs from CANDY['kinds'] in tools/agriculture.py")
    flavours = {name.lower(): (effect, int(sec), int(c, 16)) for name, effect, sec, c in
                re.findall(r"\b([A-Z_]+)\(MobEffects\.([A-Z_]+), (\d+), 0x([0-9A-F]{6})\)", java.get("CandyFlavour", ""))}
    if flavours != {name: (info["effect"], info["seconds"], info["color"]) for name, info in cd["flavours"].items()}:
        err(f"CandyFlavour {flavours} differs from CANDY['flavours'] in tools/agriculture.py")
    foods = {name: [int(n), float(sat)] for name, n, sat in re.findall(r'\bcandy\("([a-z_]+)", (\d+), ([\d.]+)F\);', main)}
    if foods != {name: info["food"] for name, info in cd["candies"].items()}:
        err(f"The candies registered in JugcraftAgriculture.java {foods} differ from CANDY['candies'] in tools/agriculture.py")
    listed = re.search(r"CANDIES = List\.of\(([^)]*)\);", main)
    if not listed or [v.strip().strip('"') for v in listed.group(1).split(",")] != list(cd["candies"]):
        err("JugcraftAgriculture.CANDIES differs from CANDY['candies'] in tools/agriculture.py")
    for needle in (f'registerBlock("{cd["kettle"]}", CandyKettleBlock::new', f'registerItem("{cd["tray"]}", CandyTrayItem::new',
                   f'Jugcraft.id("{cd["component"]}")', f'Jugcraft.id("{cd["kettle"]}")'):
        if needle not in main:
            err(f"JugcraftAgriculture.java must register {needle}")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    keys = [f"block.jugcraft.{cd['kettle']}", f"item.jugcraft.{cd['tray']}"] + [f"item.jugcraft.{c}" for c in cd["candies"]]
    keys += [f"candy_stage.jugcraft.{s}" for s in cd["stages"]] + [f"candy_base.jugcraft.{b}" for b in cd["bases"]]
    keys += [f"candy_flavour.jugcraft.{f}" for f in cd["flavours"]]
    keys += [f"tooltip.jugcraft.candy_tray.{k}" for k in cd["kinds"] if k != "lollipop"]
    for source in ("CandyKettleBlock", "CandyTrayItem", "Candies"):
        keys += [f"message.jugcraft.candy_kettle.{k}" for k in re.findall(r'MESSAGES \+ "([a-z_]+)"', java.get(source, ""))]
        keys += re.findall(r'"((?:message|tooltip|item)\.jugcraft\.candy[a-z_]*\.[a-z_]+)"', java.get(source, ""))
    for key in keys:
        if key not in lang:
            err(f"The candy kitchen has no words for {key}")
    textures = ["block/candy_kettle", "block/candy_kettle_inside", "block/candy_dial", "entity/candy_syrup", "entity/candy_needle",
                "item/candy_tray", "item/candy_tray_candy", "item/candy_corn_tip", "item/candy_corn_middle", "item/candy_corn_base",
                "item/candy_corn_outline"] + [f"item/{c}" for c in cd["candies"]]
    for texture in textures:
        if not (ASSETS / "textures" / f"{texture}.png").exists():
            err(f"The candy kitchen needs its texture {texture}")
    for recipe in (cd["kettle"], cd["tray"]):
        if not (DATA / "jugcraft" / "recipe" / f"{recipe}.json").exists():
            err(f"The {recipe} needs its recipe")
    for advancement in ("candy_maker", "taffy_puller"):
        if not (DATA / "jugcraft" / "advancement" / f"{advancement}.json").exists():
            err(f"The candy kitchen needs its advancement {advancement}")
    candy_tag = (load(DATA / "c" / "tags" / "item" / "foods" / "candy.json") or {}).get("values", [])
    for candy in cd["candies"]:
        if f"jugcraft:{candy}" not in candy_tag:
            err(f"{candy} must count as candy (c:foods/candy)")
    for name, info in cd["flavours"].items():
        if (load(DATA / "jugcraft" / "tags" / "item" / "candy_flavours" / f"{name}.json") or {}).get("values") != info["items"]:
            err(f"Item tag jugcraft:candy_flavours/{name} differs from tools/agriculture.py")


def check_foraging(java, main):
    """Autumn foraging: WildMushroomBlock and FairyRings match FORAGING in tools/agriculture.py (spreading, the fairy ring's
    size, chance, check interval and blessing), JugcraftAgriculture registers the mushrooms (the jack o'lantern
    mushroom's light), the basket and the foods; every mushroom has its texture, words, loot and worldgen, the tags hold
    the mushrooms and the forage, the cooked foods have their recipes, and the two advancements exist."""
    fg = ag.FORAGING

    def number(source, name):
        match = re.search(rf"\b{name} = (-?[\d.]+)[FLD]?;", java.get(source, ""))
        return float(match.group(1)) if match else None

    expected = {("WildMushroomBlock", "SPREAD_CHANCE"): fg["spread_chance"], ("WildMushroomBlock", "SPREAD_CAP"): fg["spread_cap"],
                ("WildMushroomBlock", "SPREAD_LIGHT"): fg["spread_light"], ("WildMushroomBlock", "RING_CHANCE"): fg["ring_chance"],
                ("FairyRings", "RING_MUSHROOMS"): fg["ring_mushrooms"], ("FairyRings", "INNER"): fg["inner"],
                ("FairyRings", "OUTER"): fg["outer"], ("FairyRings", "CHECK_TICKS"): fg["check_ticks"],
                ("FairyRings", "LUCK_TICKS"): fg["luck_ticks"]}
    for (source, name), value in expected.items():
        if number(source, name) is None or abs(number(source, name) - value) > 1e-9:
            err(f"{source}.{name} = {number(source, name)} differs from FORAGING in tools/agriculture.py ({value})")
    listed = re.search(r"WILD_MUSHROOMS = List\.of\(([^)]*)\);", main)
    if not listed or [v.strip().strip('"') for v in listed.group(1).split(",")] != list(fg["mushrooms"]):
        err("JugcraftAgriculture.WILD_MUSHROOMS differs from FORAGING['mushrooms'] in tools/agriculture.py")
    glowing = [name for name, info in fg["mushrooms"].items() if info["light"]]
    for name in glowing:
        if f'id.equals("{name}") ? {fg["mushrooms"][name]["light"]} : 0' not in main:
            err(f"The {name} must give light {fg['mushrooms'][name]['light']} (JugcraftAgriculture.java)")
    if f'registerItem("{fg["basket"]}", ForagingBasketItem::new' not in main or "FairyRings.register();" not in main:
        err("JugcraftAgriculture.java must register the Foraging Basket and the fairy rings")
    for food in fg["foods"]:
        if f'"{food}"' not in main:
            err(f"JugcraftAgriculture.java must register {food}")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    keys = [f"block.jugcraft.{m}" for m in fg["mushrooms"]] + [f"item.jugcraft.{fg['basket']}", "item.jugcraft.foraging_basket.hint",
                                                             "message.jugcraft.fairy_ring.blessed"]
    for key in keys:
        if key not in lang:
            err(f"Autumn foraging has no words for {key}")
    for mushroom in fg["mushrooms"]:
        for path in (ASSETS / "textures" / "block" / f"{mushroom}.png", DATA / "jugcraft" / "loot_table" / "blocks" / f"{mushroom}.json",
                     DATA / "jugcraft" / "worldgen" / "placed_feature" / f"patch_{mushroom}.json"):
            if not path.exists():
                err(f"The {mushroom} needs {path.relative_to(ROOT)}")
    for item in [fg["basket"]] + fg["foods"]:
        if not (ASSETS / "textures" / "item" / f"{item}.json".replace(".json", ".png")).exists():
            err(f"{item} needs its texture")
    mushrooms = (load(DATA / "jugcraft" / "tags" / "item" / "wild_mushrooms.json") or {}).get("values")
    if mushrooms != [f"jugcraft:{m}" for m in fg["mushrooms"]]:
        err("Item tag jugcraft:wild_mushrooms differs from FORAGING['mushrooms'] in tools/agriculture.py")
    soil = (load(DATA / "jugcraft" / "tags" / "block" / "mushroom_soil.json") or {}).get("values")
    if soil != fg["soil"] or 'Jugcraft.id("mushroom_soil")' not in java.get("WildMushroomBlock", ""):
        err("Block tag jugcraft:mushroom_soil differs from FORAGING['soil'], or WildMushroomBlock doesn't use it")
    forage = (load(DATA / "jugcraft" / "tags" / "item" / "forage.json") or {}).get("values")
    if forage != fg["forage"]:
        err("Item tag jugcraft:forage differs from FORAGING['forage'] in tools/agriculture.py")
    for recipe in [fg["basket"]] + fg["foods"]:
        if not (DATA / "jugcraft" / "recipe" / f"{recipe}.json").exists() \
                and not (DATA / "jugcraft" / "recipe" / "pot_cooking" / f"{recipe}.json").exists():
            err(f"{recipe} needs its recipe")
    for advancement in ("fairy_ring", "forager"):
        if not (DATA / "jugcraft" / "advancement" / f"{advancement}.json").exists():
            err(f"Autumn foraging needs its advancement {advancement}")


def check_bats(java, main):
    """The Bat House: BatHouseBlockEntity and JugcraftAgriculture match BATS in tools/agriculture.py (room, guano, range,
    move-in chance, check interval, the bats' tag, guano's area and doses); the house and guano are registered (guano as
    a FertilizerItem); and the house has its models (one for each guano level), words, loot, recipe and advancement, and
    guano its texture and its phosphate recipe."""
    bt = ag.BATS

    def number(source, name):
        match = re.search(rf"\b{name} = (-?[\d.]+)[FLD]?;", java.get(source, ""))
        return float(match.group(1)) if match else None

    expected = {("BatHouseBlockEntity", "CAPACITY"): bt["capacity"], ("BatHouseBlockEntity", "GUANO_CAP"): bt["guano_cap"],
                ("BatHouseBlockEntity", "RETURN_RANGE"): bt["return_range"], ("BatHouseBlockEntity", "MOVE_IN_CHANCE"): bt["move_in_chance"],
                ("BatHouseBlockEntity", "CHECK_TICKS"): bt["check_ticks"], ("JugcraftAgriculture", "GUANO_RADIUS"): bt["guano_radius"],
                ("JugcraftAgriculture", "GUANO_DOSES"): bt["guano_doses"]}
    for (source, name), value in expected.items():
        if number(source, name) is None or abs(number(source, name) - value) > 1e-9:
            err(f"{source}.{name} = {number(source, name)} differs from BATS in tools/agriculture.py ({value})")
    if f'TAG = "{bt["tag"]}"' not in java.get("BatHouseBlockEntity", ""):
        err(f"BatHouseBlockEntity.TAG must be {bt['tag']}")
    if f'registerBlock("{bt["house"]}", BatHouseBlock::new' not in main or f'registerItem("{bt["guano"]}", props -> new FertilizerItem(' not in main:
        err("JugcraftAgriculture.java must register the Bat House and guano (a FertilizerItem)")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    for key in (f"block.jugcraft.{bt['house']}", f"item.jugcraft.{bt['guano']}", "message.jugcraft.bat_house.status",
                "message.jugcraft.bat_house.scooped"):
        if key not in lang:
            err(f"The Bat House has no words for {key}")
    paths = [ASSETS / "models" / "block" / f"{bt['house']}.json"] + [ASSETS / "models" / "block" / f"{bt['house']}_guano_{g}.json" for g in (1, 2, 3)]
    paths += [ASSETS / "textures" / "item" / f"{bt['guano']}.png", DATA / "jugcraft" / "loot_table" / "blocks" / f"{bt['house']}.json",
              DATA / "jugcraft" / "recipe" / f"{bt['house']}.json", DATA / "jugcraft" / "recipe" / "phosphate_from_bat_guano.json",
              DATA / "jugcraft" / "advancement" / "night_shift.json"]
    for path in paths:
        if not path.exists():
            err(f"The Bat House needs {path.relative_to(ROOT)}")
    guano = load(DATA / "jugcraft" / "recipe" / "phosphate_from_bat_guano.json") or {}
    if guano.get("ingredients") != [f"jugcraft:{bt['guano']}"] * bt["guano_per_phosphate"]:
        err(f"Phosphate takes {bt['guano_per_phosphate']} guano (tools/agriculture.py)")


def check_hay_golem(java, main):
    """The Hay Golem: HayGolem.java matches HAY_GOLEM in tools/agriculture.py (health, speed, post radius, search height,
    tending, work, give-up and idle times, pouch, carry, wheat's healing, fire, hay bales, reach, leading range); it is
    registered with its size and attributes and its building callback; it guards as a scarecrow (Scarecrows looks for
    golems); it is named, drops wheat, has its head tag, texture and advancement; and its model lays its boxes where the
    texture paints them."""
    hg = ag.HAY_GOLEM
    source = java.get("HayGolem", "")
    found = {name: float(value) for name, value in re.findall(r"static final (?:int|double|float) ([A-Z_]+) = ([\d.]+)[FD]?;", source)}
    expected = {"MAX_HEALTH": hg["health"], "SPEED": hg["speed"], "POST_RADIUS": hg["post_radius"], "SEARCH_HEIGHT": hg["search_height"],
                "TEND_TICKS": hg["tend_ticks"], "WORK_TICKS": hg["work_ticks"], "GIVE_UP_TICKS": hg["give_up_ticks"],
                "POUCH_SLOTS": hg["pouch_slots"], "CARRY": hg["carry"], "IDLE_TICKS": hg["idle_ticks"], "WHEAT_HEAL": hg["wheat_heal"],
                "FIRE_FACTOR": hg["fire_factor"], "HAY_BALES": hg["hay_bales"], "REACH": hg["reach"], "LEAD_RANGE": hg["lead_range"]}
    for name, value in expected.items():
        if name not in found or abs(found[name] - value) > 1e-6:
            err(f"HayGolem.{name} = {found.get(name)} differs from HAY_GOLEM in tools/agriculture.py ({value})")
    if f'Jugcraft.id("{hg["heads_tag"].split(":", 1)[1]}")' not in source:
        err(f"HayGolem.HEADS must be {hg['heads_tag']}")
    width, height = hg["size"]
    if (f'entity("{hg["entity"]}", EntityType.Builder.<HayGolem>of(HayGolem::new, MobCategory.MISC).sized({width}F, {height}F)' not in main
            or "FabricDefaultAttributeRegistry.register(HAY_GOLEM, HayGolem.createAttributes())" not in main
            or "UseBlockCallback.EVENT.register(HayGolem::onUseBlock)" not in main):
        err("JugcraftAgriculture.java must register the Hay Golem with its size, attributes and building callback")
    if "instanceof CropGuard" not in java.get("Scarecrows", "") or "implements CropGuard" not in java.get("HayGolem", ""):
        err("Scarecrows.guarded must count every CropGuard, the Hay Golem among them")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    if lang.get(f"entity.jugcraft.{hg['entity']}") != hg["display"]:
        err("The Hay Golem has no name")
    table = load(DATA / "jugcraft" / "loot_table" / f"{hg['table']}.json") or {}
    if "minecraft:wheat" not in json.dumps(table):
        err("A Hay Golem must drop wheat")
    heads = load(DATA / "jugcraft" / "tags" / "item" / f"{hg['heads_tag'].split(':', 1)[1]}.json") or {}
    if sorted(heads.get("values", [])) != sorted(hg["heads"]):
        err(f"The tag {hg['heads_tag']} must hold the heads in HAY_GOLEM")
    for path in (ASSETS / "textures" / "entity" / f"{hg['entity']}.png", DATA / "jugcraft" / "advancement" / "man_of_straw.json"):
        if not path.exists():
            err(f"The Hay Golem needs {path.relative_to(ROOT)}")
    import hay_golem_textures
    model_path = ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client" / "HayGolemModel.java"
    model = model_path.read_text(encoding="utf-8") if model_path.exists() else ""
    for name, (u, v, *_size) in hay_golem_textures.BOXES.items():
        if f"texOffs({u}, {v})" not in model:
            err(f"HayGolemModel has no box at ({u}, {v}) for the texture's {name}")


def check_knitting(java, main):
    """Knitting: the Java matches KNITTING in tools/agriculture.py (the wheel's turns, spin time and unravelling loss; yarn
    per wool, cosiness and undyed colour; the needles' row time and durability; each garment's slot, rows and look, in
    the needles' order); the wheel, yarn, needles and garments are registered; every garment has its words, item model
    and equipment asset (the dyeable knit, and its motif); and the tags, recipes and advancements exist."""
    kn = ag.KNITTING

    def number(source, name):
        match = re.search(rf"\b{name} = (-?(?:0x)?[\dA-Fa-f.]+)[FLD]?;", java.get(source, ""))
        if not match:
            return None
        value = match.group(1)
        return float(int(value, 16)) if value.startswith("0x") else float(value)

    expected = {("SpinningWheelBlockEntity", "TURNS"): kn["turns"], ("SpinningWheelBlockEntity", "SPIN_TICKS"): kn["spin_ticks"],
                ("SpinningWheelBlockEntity", "UNRAVEL_LOSS"): kn["unravel_loss"], ("Knitting", "YARN_PER_WOOL"): kn["yarn_per_wool"],
                ("Knitting", "COZY_TICKS"): kn["cozy"]["ticks"], ("Knitting", "COZY_PIECES"): kn["cozy"]["pieces"],
                ("Knitting", "COZY_RANGE"): kn["cozy"]["range"], ("Knitting", "COZY_EFFECT_TICKS"): kn["cozy"]["effect_ticks"],
                ("Knitting", "UNDYED"): kn["undyed"], ("KnittingNeedlesItem", "ROW_TICKS"): kn["row_ticks"],
                ("JugcraftAgriculture", "NEEDLES_DURABILITY"): kn["needles_durability"]}
    for (source, name), value in expected.items():
        found = number(source, name)
        if found is None or abs(found - value) > 1e-9:
            err(f"{source}.{name} = {found} differs from KNITTING in tools/agriculture.py ({value})")
    enum = java.get("Knitwear", "")
    declared = re.findall(r'^\t([A-Z_]+)\("([a-z_]+)", EquipmentSlot\.([A-Z]+), (\d+), "([a-z_]+)"\)', enum, re.M)
    wanted = [(garment, info["slot"], str(info["rows"]), info["asset"]) for garment, info in kn["garments"].items()]
    if [(item, slot, rows, asset) for _, item, slot, rows, asset in declared] != wanted:
        err("Knitwear.java's garments (item, slot, rows, look, in order) differ from KNITTING in tools/agriculture.py")
    for call in ('registerBlock("spinning_wheel", SpinningWheelBlock::new', 'registerItem("yarn", Item::new',
                 'registerItem("knitting_needles", KnittingNeedlesItem::new', "for (Knitwear knit : Knitwear.values())", "Knitting.register()"):
        if call not in main:
            err(f"JugcraftAgriculture.java must call {call}")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    for key in [f"block.jugcraft.{kn['wheel']}", f"item.jugcraft.{kn['yarn']}", f"item.jugcraft.{kn['needles']}"] + [
            f"item.jugcraft.{g}" for g in kn["garments"]]:
        if key not in lang:
            err(f"Knitting has no words for {key}")
    for garment, info in kn["garments"].items():
        model = load(ASSETS / "items" / f"{garment}.json") or {}
        if (model.get("model", {}).get("tints") or [{}])[0].get("type") != "minecraft:dye":
            err(f"{garment}'s item model must be tinted by its dyed colour")
        asset = load(ASSETS / "equipment" / f"{info['asset']}.json") or {}
        layers = asset.get("layers", {}).get("humanoid", [])
        if not layers or "dyeable" not in layers[0] or ("motif" in info) != (len(layers) == 2):
            err(f"The equipment asset {info['asset']} must be the dyeable knit (and the motif over it for a motif sweater)")
    for path in [ASSETS / "textures" / "entity" / "equipment" / "humanoid" / "knit.png", ASSETS / "textures" / "item" / "yarn.png",
                 DATA / "jugcraft" / "recipe" / "spinning_wheel.json", DATA / "jugcraft" / "recipe" / "knitting_needles.json",
                 DATA / "jugcraft" / "loot_table" / "blocks" / "spinning_wheel.json",
                 DATA / "jugcraft" / "advancement" / "knit_one_purl_two.json", DATA / "jugcraft" / "advancement" / "snug_as_a_bug.json"]:
        if not path.exists():
            err(f"Knitting needs {path.relative_to(ROOT)}")
    knitwear = (load(DATA / "jugcraft" / "tags" / "item" / "knitwear.json") or {}).get("values", [])
    frozen = (load(DATA / "minecraft" / "tags" / "item" / "freeze_immune_wearables.json") or {}).get("values", [])
    washed = (load(DATA / "minecraft" / "tags" / "item" / f"{kn['wash_tag'].split(':')[1]}.json") or {}).get("values", [])
    for garment in kn["garments"]:
        if f"jugcraft:{garment}" not in knitwear or f"jugcraft:{garment}" not in frozen:
            err(f"{garment} must be knitwear and freeze-immune")
    for item in [kn["yarn"]] + list(kn["garments"]):
        recipe = load(DATA / "jugcraft" / "recipe" / f"{item}_dyed.json") or {}
        if recipe.get("type") != kn["dye_recipe"] or recipe.get("target") != f"jugcraft:{item}" or f"jugcraft:{item}" not in washed:
            err(f"{item} must have its dyeing recipe ({item}_dyed) and wash clean in a cauldron")


def check_pies(java, main):
    """Pie baking: the Java matches PIES in tools/agriculture.py (the oven's fuel bank, heat, heating and cooling, baking
    heat, baked and burnt points and light; the pie's slices and a burnt slice's food and chance of Hunger; each filling's
    slice food and colour, in order); the oven, pies, raw pies, slices and dough are registered; every pie has a model for
    each slice gone and its words, textures and loot (only while whole); the raw pies have recipes; the advancement exists."""
    pies = ag.PIES

    def number(source, name):
        match = re.search(rf"\b{name} = (-?[\d.]+)[FLD]?;", java.get(source, ""))
        return float(match.group(1)) if match else None

    expected = {("HearthOvenBlockEntity", "MAX_BURN"): pies["max_burn"], ("HearthOvenBlockEntity", "WOOD_BURN"): pies["wood_burn"],
                ("HearthOvenBlockEntity", "MAX_HEAT"): pies["max_heat"],
                ("HearthOvenBlockEntity", "HEAT_TICKS"): pies["heat_ticks"], ("HearthOvenBlockEntity", "COOL_TICKS"): pies["cool_ticks"],
                ("HearthOvenBlockEntity", "BAKE_HEAT"): pies["bake_heat"], ("HearthOvenBlockEntity", "BAKED"): pies["baked"],
                ("HearthOvenBlockEntity", "BURNT"): pies["burnt_points"], ("HearthOvenBlock", "LIGHT"): pies["light"],
                ("PieBlock", "SLICES"): pies["slices"], ("PieBlock", "BURNT_NUTRITION"): pies["burnt_nutrition"],
                ("PieBlock", "BURNT_SICK_CHANCE"): pies["burnt_sick_chance"]}
    for (source, name), value in expected.items():
        found = number(source, name)
        if found is None or abs(found - value) > 1e-9:
            err(f"{source}.{name} = {found} differs from PIES in tools/agriculture.py ({value})")
    declared = re.findall(r'^\t([A-Z_]+)\("([a-z_]+)", (\d+), ([\d.]+)F, 0x([0-9A-Fa-f]{6})\)', java.get("PieFilling", ""), re.M)
    wanted = [(f, str(i["food"][0]), str(i["food"][1]), f"{i['color']:06X}") for f, i in pies["fillings"].items()]
    if [(name, food, sat, color.upper()) for _, name, food, sat, color in declared] != wanted:
        err("PieFilling.java's fillings (slice food, colour, in order) differ from PIES in tools/agriculture.py")
    for call in ('registerBlock("hearth_oven", HearthOvenBlock::new', 'registerItem("pastry_dough"', "for (PieFilling filling : PieFilling.values())",
                 'registerBlock("burnt_pie", props -> new PieBlock(null, props)'):
        if call not in main:
            err(f"JugcraftAgriculture.java must call {call}")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    pies_blocks = [f"{f}_pie" for f in pies["fillings"]] + [pies["burnt"]]
    for block in pies_blocks:
        if f"block.jugcraft.{block}" not in lang:
            err(f"Pie baking has no words for {block}")
        for bites in range(pies["slices"]):
            name = f"{block}.json" if bites == 0 else f"{block}_slice{bites}.json"
            if not (ASSETS / "models" / "block" / name).exists():
                err(f"{block} needs its model {name}")
        loot = json.dumps(load(DATA / "jugcraft" / "loot_table" / "blocks" / f"{block}.json") or {})
        if '"bites": "0"' not in loot:
            err(f"{block} must drop only while whole")
        for texture in (f"{block}_top", f"{block}_inside"):
            if not (ASSETS / "textures" / "block" / f"{texture}.png").exists():
                err(f"{block} needs its texture {texture}")
    for filling in pies["fillings"]:
        for item in (f"raw_{filling}_pie", f"{filling}_pie_slice"):
            if f"item.jugcraft.{item}" not in lang or not (ASSETS / "textures" / "item" / f"{item}.png").exists():
                err(f"Pie baking needs the words and texture of {item}")
        if not (DATA / "jugcraft" / "recipe" / f"raw_{filling}_pie.json").exists():
            err(f"raw_{filling}_pie needs its recipe")
    if f'"{pies["wood_tag"].split(":")[1]}"' not in java.get("HearthOvenBlockEntity", ""):
        err("HearthOvenBlockEntity.WOOD must be the tag PIES['wood_tag'] in tools/agriculture.py")
    for path in (DATA / "jugcraft" / "recipe" / "hearth_oven.json", DATA / "jugcraft" / "recipe" / "pastry_dough.json",
                 DATA / "jugcraft" / "tags" / "item" / "hearth_oven_wood.json",
                 DATA / "jugcraft" / "loot_table" / "blocks" / "hearth_oven.json", ASSETS / "models" / "block" / "hearth_oven_lit.json",
                 DATA / "jugcraft" / "advancement" / "as_easy_as_pie.json"):
        if not path.exists():
            err(f"Pie baking needs {path.relative_to(ROOT)}")


def check_spirit_board(java, main):
    """The Spirit Board: the Java matches SPIRIT_BOARD in tools/agriculture.py (the séance's ranges, hands, letter times,
    rest, watch range and a spirit's reward; the names; the wishes, in order); its words, YES, NO and GOODBYE sit on the
    face where tools/spirit_board_textures.py draws them; the block is registered; its model, textures (the face at its
    size), words, tags, recipe, loot and advancements exist."""
    import spirit_board_textures as sbt
    board = ag.SPIRIT_BOARD
    source = java.get("SpiritBoard", "")

    def number(name):
        match = re.search(rf"\b{name} = (-?[\d.]+)[FLD]?;", source)
        return float(match.group(1)) if match else None

    expected = {"CANDLE_RANGE": board["candle_range"], "SPIRIT_RANGE": board["spirit_range"], "HAND_RANGE": board["hand_range"],
                "MAX_HANDS": board["max_hands"], "LETTER_TICKS": board["letter_ticks"], "FAST_LETTER_TICKS": board["fast_letter_ticks"],
                "COOLDOWN_TICKS": board["cooldown_ticks"], "WATCH_RANGE": board["watch_range"], "REST_XP": board["rest_xp"],
                "REST_LUCK_TICKS": board["rest_luck_ticks"], "FACE_WIDTH": sbt.FACE[0], "FACE_HEIGHT": sbt.FACE[1]}
    for name, value in expected.items():
        found = number(name)
        if found is None or abs(found - value) > 1e-9:
            err(f"SpiritBoard.{name} = {found} differs from SPIRIT_BOARD in tools/agriculture.py ({value})")
    names = re.search(r"NAMES = \{([^}]*)\}", source)
    if not names or re.findall(r'"([A-Z]+)"', names.group(1)) != board["names"]:
        err("SpiritBoard.NAMES differs from SPIRIT_BOARD['names'] in tools/agriculture.py")
    wishes = re.search(r"enum Wish \{\s*([A-Z_, ]+);", source)
    if not wishes or [w.strip().lower() for w in wishes.group(1).split(",")] != list(board["wishes"]):
        err("SpiritBoard.Wish differs from SPIRIT_BOARD['wishes'] in tools/agriculture.py (in order)")
    if f'"{board["candles_tag"].split(":")[1]}"' not in source:
        err("SpiritBoard.CANDLES must be the tag SPIRIT_BOARD['candles_tag']")
    # Each word's middle on the face, as SpiritBoard.place() has it, is where the texture draws it.
    stops = {"YES": "YES", "NO": "NO", "GOODBYE": "GOODBYE"}
    for word, stop in stops.items():
        x, y = sbt.WORDS[word]
        middle = (x + (4 * len(word) - 1) / 2, y + 2.5)
        found = re.search(rf"case {stop} -> new float\[\] \{{([\d.]+)F, ([\d.]+)F\}}", source)
        if not found or (float(found.group(1)), float(found.group(2))) != middle:
            err(f"SpiritBoard.place({word}) must be {middle}, the middle of the word on the face")
    for formula in ("7.5F + 4 * i, 15.5F - arc(i)", "7.5F + 4 * i, 24.5F - arc(i)", "13.5F + 4 * i, 33.5F"):
        if formula not in source:
            err(f"SpiritBoard.place() must put the letters where spirit_board_textures.letter_places() draws them ({formula})")
    if 'registerBlock("spirit_board", SpiritBoardBlock::new' not in main:
        err('JugcraftAgriculture.java must call registerBlock("spirit_board", SpiritBoardBlock::new')
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    keys = [f"block.jugcraft.{board['block']}"] + [f"spirit_wish.jugcraft.{w}" for w in board["wishes"]]
    for key in keys:
        if key not in lang:
            err(f"The Spirit Board has no words for {key}")
    for texture, size in (("entity/spirit_board", sbt.FACE), ("entity/planchette", (32, 32)), ("entity/planchette_wood", (16, 16))):
        png = ASSETS / "textures" / f"{texture}.png"
        if not png.is_file():
            err(f"The Spirit Board needs its texture {texture}")
            continue
        with Image.open(png) as img:
            if img.size != size:
                err(f"{texture} is {img.size}, expected {size}")
    tags = DATA / "jugcraft" / "tags"
    paths = [tags / "block" / f"{board['candles_tag'].split(':')[1]}.json", DATA / "jugcraft" / "recipe" / "spirit_board.json",
             DATA / "jugcraft" / "loot_table" / "blocks" / "spirit_board.json", DATA / "jugcraft" / "advancement" / "is_anybody_there.json",
             DATA / "jugcraft" / "advancement" / "unfinished_business.json"] + [tags / "item" / "spirit_wishes" / f"{w}.json" for w in board["wishes"]]
    for path in paths:
        if not path.exists():
            err(f"The Spirit Board needs {path.relative_to(ROOT)}")


def check_turkeys(java, main):
    """Wild turkeys: Turkey.java, Turkeys.java and RoastTurkeyBlock.java match TURKEYS in tools/agriculture.py (health,
    speed, strutting, eggs; spawning; servings and their food); the turkey is registered with its size and attributes
    and its spawning; it is named and drops a raw turkey and feathers; its food and habitat tags, textures, roast models,
    cooking and advancements exist; and its model lays its boxes where the textures paint them."""
    import turkey_textures
    tk = ag.TURKEYS

    def numbers(source):
        text = java.get(source, "")
        return {name: float(value) for name, value in re.findall(r"static final (?:int|double|float) ([A-Z_]+) = ([\d.]+)[FD]?;", text)}

    expected = {("Turkey", "MAX_HEALTH"): tk["health"], ("Turkey", "SPEED"): tk["speed"], ("Turkey", "STRUT_RANGE"): tk["strut_range"],
                ("Turkey", "STRUT_TICKS"): tk["strut_ticks"], ("Turkey", "STRUT_CHANCE"): tk["strut_chance"], ("Turkey", "EGG_MIN"): tk["egg_min"],
                ("Turkey", "EGG_MAX"): tk["egg_max"], ("Turkeys", "SPAWN_TICKS"): tk["spawn_ticks"],
                ("Turkeys", "SPAWN_CHANCE"): tk["spawn_chance"], ("Turkeys", "MIN_DISTANCE"): tk["min_distance"],
                ("Turkeys", "MAX_DISTANCE"): tk["max_distance"], ("Turkeys", "FLOCK_MIN"): tk["flock"][0],
                ("Turkeys", "FLOCK_MAX"): tk["flock"][1], ("Turkeys", "NEAR_CAP"): tk["near_cap"], ("Turkeys", "NEAR_RANGE"): tk["near_range"],
                ("Turkeys", "LEVEL_CAP"): tk["level_cap"], ("RoastTurkeyBlock", "SERVINGS"): tk["servings"],
                ("RoastTurkeyBlock", "NUTRITION"): tk["serving"][0], ("RoastTurkeyBlock", "SATURATION"): tk["serving"][1]}
    for (source, name), value in expected.items():
        found = numbers(source).get(name)
        if found is None or abs(found - value) > 1e-6:
            err(f"{source}.{name} = {found} differs from TURKEYS in tools/agriculture.py ({value})")
    for source, tag in (("Turkey", tk["food_tag"]), ("Turkeys", tk["habitat_tag"])):
        if f'Jugcraft.id("{tag.split(":", 1)[1]}")' not in java.get(source, ""):
            err(f"{source} must use the tag {tag}")
    if ag.ITEMS.get(tk["slice"], {}).get("food") != tk["serving"]:
        err("A slice of roast turkey must be worth a serving at the table")
    width, height = tk["size"]
    if (f'entity("{tk["entity"]}", EntityType.Builder.<Turkey>of(Turkey::new, MobCategory.CREATURE).sized({width}F, {height}F)' not in main
            or "FabricDefaultAttributeRegistry.register(TURKEY, Turkey.createAttributes())" not in main or "Turkeys.register()" not in main):
        err("JugcraftAgriculture.java must register the turkey with its size, attributes and spawning")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    if lang.get(f"entity.jugcraft.{tk['entity']}") != tk["display"] or lang.get(f"block.jugcraft.{tk['roast']}") != tk["roast_display"]:
        err("The turkey and the roast turkey need their names")
    table = json.dumps(load(DATA / "jugcraft" / "loot_table" / f"{tk['table']}.json") or {})
    if f"jugcraft:{tk['raw']}" not in table or "minecraft:feather" not in table:
        err("A turkey must drop a raw turkey and feathers")
    if ag.COOKING.get(tk["roast"], {}).get("input") != tk["raw"]:
        err("A raw turkey must cook into a roast turkey")
    paths = [ASSETS / "textures" / "entity" / f"turkey_{kind}.png" for kind in turkey_textures.BIRDS]
    paths += [ASSETS / "models" / "block" / f"{tk['roast']}{stage}.json" for stage in ("", "_carved", "_breast", "_carcass")]
    paths += [DATA / "jugcraft" / "tags" / "item" / f"{tk['food_tag'].split(':')[1]}.json",
              DATA / "jugcraft" / "tags" / "worldgen" / "biome" / f"{tk['habitat_tag'].split(':')[1]}.json",
              DATA / "jugcraft" / "advancement" / "gobble_gobble.json", DATA / "jugcraft" / "advancement" / "carving_the_bird.json",
              DATA / "jugcraft" / "recipe" / f"{tk['roast']}.json"]
    for path in paths:
        if not path.exists():
            err(f"Wild turkeys need {path.relative_to(ROOT)}")
    model_path = ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client" / "TurkeyModel.java"
    model = model_path.read_text(encoding="utf-8") if model_path.exists() else ""
    for name, (u, v, *_size) in turkey_textures.BOXES.items():
        if f"texOffs({u}, {v})" not in model:
            err(f"TurkeyModel has no box at ({u}, {v}) for the texture's {name}")


def check_theremin(java, main):
    """The Theremin: ThereminBlockEntity and ThereminBlock match THEREMIN in tools/agriculture.py (how often it looks, its
    range, the range that earns Good Vibrations, its pitch, vibrato and light); it is registered; its models (silent and
    playing), words, recipe (gated on machines, for its copper wire), loot and advancement exist."""
    th = ag.THEREMIN
    source = java.get("ThereminBlockEntity", "") + java.get("ThereminBlock", "")
    found = {name: float(value) for name, value in re.findall(r"static final (?:int|double|float) ([A-Z_]+) = ([\d.]+)[FD]?;", source)}
    expected = {"SENSE_TICKS": th["sense_ticks"], "RANGE": th["range"], "PLAYER_RANGE": th["player_range"], "LOW": th["low"],
                "HIGH": th["high"], "VIBRATO": th["vibrato"], "VIBRATO_SPEED": th["vibrato_speed"], "LIGHT": th["light"]}
    for name, value in expected.items():
        if name not in found or abs(found[name] - value) > 1e-6:
            err(f"Theremin {name} = {found.get(name)} differs from THEREMIN in tools/agriculture.py ({value})")
    if 'registerBlock("theremin", ThereminBlock::new' not in main:
        err('JugcraftAgriculture.java must call registerBlock("theremin", ThereminBlock::new')
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    for key in (f"block.jugcraft.{th['block']}", "message.jugcraft.theremin.on", "message.jugcraft.theremin.off"):
        if key not in lang:
            err(f"The theremin has no words for {key}")
    recipe = load(DATA / "jugcraft" / "recipe" / f"{th['block']}.json") or {}
    if "machines" not in json.dumps(recipe.get("fabric:load_conditions", [])):
        err("The theremin's recipe must load only with the machines feature (it takes copper wire)")
    for path in (ASSETS / "models" / "block" / f"{th['block']}.json", ASSETS / "models" / "block" / f"{th['block']}_on.json",
                 DATA / "jugcraft" / "loot_table" / "blocks" / f"{th['block']}.json", DATA / "jugcraft" / "advancement" / "good_vibrations.json"):
        if not path.exists():
            err(f"The theremin needs {path.relative_to(ROOT)}")



def check_graveyard(java, main):
    """The graveyard pack: HeadstoneBlock's styles (id, stone, cells, boxes per part, where the epitaph goes), its
    weathering numbers, the stones' ink, Epitaph's limits, the chisel and the session match tools/graveyard.py; the
    renderer fades ink as graveyard.py says; every part and stage has its model, every state a variant; each headstone
    has its loot (part 0 only, keeping the epitaph), recipe and words; the textures and advancements exist."""
    source = java.get("HeadstoneBlock", "")
    if f"IntegerProperty.create(\"part\", 0, {gy.PARTS - 1})" not in source:
        err("HeadstoneBlock.PART must run 0 to PARTS - 1 (tools/graveyard.py)")
    styles = re.findall(r'[A-Z_]+\("([a-z_]+)", Stone\.([A-Z]+), ([A-Z0-9]+),\s*new double\[\]\[\]\[\] \{(.*?)\},\s*'
                        r'new Text\((true|false), ([^)]*)\)\)', source, re.S)
    found = {}
    for sid, stone, cells, boxes, top, text in styles:
        parts = [[[float(v) for v in box.split(",")] for box in re.findall(r"\{([\d., ]+)\}", part)]
                 for part in re.findall(r"\{(\{[\d., ]+\}(?:, \{[\d., ]+\})*)\}", boxes)]
        values = [float(eval(v.replace("F", ""))) for v in text.split(",")]
        found[sid] = {"stone": stone.lower(), "cells": cells, "boxes": parts, "top": top == "true", "text": values}
    cell_names = {tuple(map(tuple, gy.SINGLE)): "SINGLE", tuple(map(tuple, gy.TALL2)): "TALL2", tuple(map(tuple, gy.TALL3)): "TALL3",
                  tuple(map(tuple, gy.LONG)): "LONG", tuple(map(tuple, gy.TALL4)): "TALL4", tuple(map(tuple, gy.WIDE)): "WIDE",
                  tuple(map(tuple, gy.WIDE2)): "WIDE2"}
    if set(found) != set(gy.HEADSTONES):
        err(f"HeadstoneBlock.Style {sorted(found)} differs from HEADSTONES in tools/graveyard.py {sorted(gy.HEADSTONES)}")
    for sid, info in gy.HEADSTONES.items():
        got = found.get(sid)
        if not got:
            continue
        text = info["text"]
        want_text = [text["x"], text["y"], text["z"], text["width"], text["height"], text["max_scale"]]
        want_boxes = [[list(map(float, box)) for box in part] for part in info["shapes"]]
        if (got["stone"] != info["stone"] or got["cells"] != cell_names.get(tuple(map(tuple, info["cells"])))
                or got["top"] != (text["face"] == "TOP") or any(abs(a - b) > 1e-4 for a, b in zip(got["text"], want_text))
                or got["boxes"] != want_boxes):
            err(f"HeadstoneBlock.Style for {sid} differs from tools/graveyard.py")
    numbers = {name: float(value) for name, value in re.findall(r"static final (?:int|float|long) ([A-Z_]+) = ([\d.]+)[FL]?;", source)}
    weather = gy.WEATHERING
    if numbers.get("AGE_CHANCE") != weather["age_chance"] or numbers.get("SKY_FACTOR") != weather["sky_factor"]:
        err("HeadstoneBlock AGE_CHANCE / SKY_FACTOR differ from WEATHERING in tools/graveyard.py")
    stir = re.search(r"STIR = \{([\d.F, ]+)\}", source)
    if not stir or [float(v.strip().rstrip("F")) for v in stir.group(1).split(",")] != weather["stir"]:
        err("HeadstoneBlock.STIR differs from WEATHERING['stir'] in tools/graveyard.py")
    inks = {name.lower(): (int(ink, 16), int(fade, 16)) for name, ink, fade in re.findall(r"([A-Z]+)\(0x([0-9A-F]{8}), 0x([0-9A-F]{8})\)", source)}
    if inks != {stone: (info["ink"], info["fade_to"]) for stone, info in gy.STONES.items()}:
        err("HeadstoneBlock.Stone inks differ from STONES in tools/graveyard.py")
    renderer = (ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client" / "HeadstoneRenderer.java")
    fade = re.search(r"INK_FADE = \{([\d.F, ]+)\}", renderer.read_text(encoding="utf-8") if renderer.exists() else "")
    if not fade or [float(v.strip().rstrip("F")) for v in fade.group(1).split(",")] != weather["ink_fade"]:
        err("HeadstoneRenderer.INK_FADE differs from WEATHERING['ink_fade'] in tools/graveyard.py")
    epitaph = java.get("Epitaph", "")
    limits = {name: int(value) for name, value in re.findall(r"static final int ([A-Z_]+) = (\d+);", epitaph)}
    if limits.get("LINES") != gy.EPITAPH["lines"] or limits.get("LINE_LENGTH") != gy.EPITAPH["line_length"]:
        err("Epitaph LINES / LINE_LENGTH differ from EPITAPH in tools/graveyard.py")
    epitaphs = java.get("Epitaphs", "")
    if f'CHISEL = "{gy.EPITAPH["chisel"]}"' not in epitaphs or f"SESSION_TICKS = {gy.EPITAPH['session_ticks']};" not in epitaphs:
        err("Epitaphs CHISEL / SESSION_TICKS differ from EPITAPH in tools/graveyard.py")
    for call in ('Jugcraft.id("epitaph")', 'Jugcraft.id("headstone")', "registerItem(Epitaphs.CHISEL", "Epitaphs.register()"):
        if call not in main:
            err(f"JugcraftAgriculture.java must call {call}")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    for sid, info in gy.HEADSTONES.items():
        parts = len(info["cells"])
        for part in range(parts):
            for stage in gy.STAGES:
                name = f"{sid}_{stage}" if parts == 1 else f"{sid}_{part}_{stage}"
                if not (ASSETS / "models" / "block" / f"{name}.json").exists():
                    err(f"{sid} needs model {name}")
        variants = set((load(ASSETS / "blockstates" / f"{sid}.json") or {}).get("variants", {}))
        want = {f"facing={f},part={p},waxed={w},weathering={s}" for f in ("north", "east", "south", "west") for p in range(gy.PARTS)
                for w in ("false", "true") for s in range(4)}
        if variants != want:
            err(f"{sid}: its blockstate must cover every facing, part, wax and stage")
        table = load(DATA / "jugcraft" / "loot_table" / "blocks" / f"{sid}.json") or {}
        if "jugcraft:epitaph" not in json.dumps(table) or '"part": "0"' not in json.dumps(table):
            err(f"{sid}: its loot must drop from part 0 only and keep the epitaph")
        recipe = DATA / "jugcraft" / "recipe" / (f"{sid}_from_stonecutting.json" if "stonecutting" in info["recipe"] else f"{sid}.json")
        if not recipe.exists() or f"block.jugcraft.{sid}" not in lang:
            err(f"{sid} needs its recipe and words")
    for texture in gy.textures():
        if not (ASSETS / "textures" / "block" / f"{texture}.png").is_file():
            err(f"Missing graveyard texture {texture}")
    for key in gy.ADVANCEMENTS:
        if not (DATA / "jugcraft" / "advancement" / f"{key}.json").exists():
            err(f"Missing advancement {key}")
    check_graveyard_buildings(java, main, lang)
    check_graveyard_grounds(java, main, lang)


# Vanilla models a Jugcraft model may inherit its textures from: the concrete side models of vanilla's walls, fences,
# panes and bars (each defines its own texture, as `minecraft:block/tuff_wall_side` does), which the diagonal
# connections build on. Vanilla templates (`block`, `cube_all`, `fence_side`, `template_*`, `door_*`...) define none.
VANILLA_TEXTURED_PARENTS = ("_wall_side", "_wall_side_tall", "_fence_side", "_fence_side_north", "_pane_side", "_bars_side")


def check_model_textures():
    """Every model a blockstate or item draws directly defines each texture variable its faces use, itself or through
    its Jugcraft parents (a template only used as a parent may leave them to its children), unless it builds on one of
    vanilla's concrete side models, which define their own. An undefined one draws the missing-texture pattern in game."""
    models = ASSETS / "models"

    def model(ref):
        if not isinstance(ref, str) or not ref.startswith(f"{MOD}:"):
            return None
        return load(models / f"{ref.split(':', 1)[1]}.json")

    def textures(m):
        out = dict(textures(model(m["parent"]))) if model(m.get("parent")) else {}
        out.update(m.get("textures") or {})
        return out

    def refs(node):
        if isinstance(node, dict):
            for key, value in node.items():
                if key == "model" and isinstance(value, str):
                    yield value
                else:
                    yield from refs(value)
        elif isinstance(node, list):
            for value in node:
                yield from refs(value)

    used = set()
    for folder in ("blockstates", "items"):
        for path in (ASSETS / folder).glob("*.json"):
            used.update(refs(load(path) or {}))
    for ref in sorted(used):
        m = model(ref)
        if not m:
            continue
        chain, elements = m, m.get("elements")
        while elements is None and model(chain.get("parent")):
            chain = model(chain["parent"])
            elements = chain.get("elements")
        root = m
        while model(root.get("parent")):
            root = model(root["parent"])
        parent = root.get("parent", "")
        if parent.startswith("minecraft:block/") and parent.rsplit("/", 1)[1].endswith(VANILLA_TEXTURED_PARENTS):
            continue
        defined = textures(m)
        for element in elements or []:
            for face in element.get("faces", {}).values():
                variable = face.get("texture", "")
                if variable.startswith("#") and variable[1:] not in defined:
                    err(f"{ref}: texture {variable} is not defined (it would draw as missing)")
                    break


def check_graveyard_grounds(java, main, lang):
    """Pack 4: the grave vase's wilting and calming, the lamp post's light and check, the open grave's stirring and the
    bench's seat match tools/graveyard.py; the vase and lamp post are registered with their models for every state,
    loot, recipe, words and tags; every bouquet's flowers are tagged."""
    def numbers(name):
        return {k: float(v) for k, v in re.findall(r"static final (?:int|float|double) ([A-Z_]+) = ([\d.]+)[FD]?;", java.get(name, ""))}
    vase, post = gy.GRAVE_VASE, gy.LAMP_POST
    got = numbers("GraveVaseBlock")
    if got.get("WILT_CHANCE") != vase["wilt_chance"] or got.get("CALM_REACH") != vase["calm_reach"] or got.get("CALM") != vase["calm"]:
        err("GraveVaseBlock WILT_CHANCE / CALM_REACH / CALM differ from GRAVE_VASE in tools/graveyard.py")
    bouquets = re.search(r"enum Bouquet implements StringRepresentable \{\s*([A-Z_, ]+);", java.get("GraveVaseBlock", ""))
    if not bouquets or [b.strip().lower() for b in bouquets.group(1).split(",")] != ["none"] + vase["colours"]:
        err("GraveVaseBlock.Bouquet must be NONE then GRAVE_VASE['colours'] of tools/graveyard.py, in order")
    got = numbers("LampPostBlock")
    if got.get("LIGHT") != post["light"] or got.get("CHECK_TICKS") != post["check_ticks"]:
        err("LampPostBlock LIGHT / CHECK_TICKS differ from LAMP_POST in tools/graveyard.py")
    if numbers("HeadstoneBlock").get("OPEN_GRAVE_STIR") != gy.STIR_BY_KIND["open_grave"] or numbers("MemorialBenchBlock").get("SEAT") != gy.BENCH_SEAT:
        err("HeadstoneBlock.OPEN_GRAVE_STIR / MemorialBenchBlock.SEAT differ from tools/graveyard.py")
    for call, const in (("GraveVaseBlock::new", f'GRAVE_VASE = "{vase["block"]}"'), ("LampPostBlock::new", f'LAMP_POST = "{post["block"]}"'),
                        ("new MemorialBenchBlock(props, style)", "registerGraveyardGrounds();")):
        if call not in main or const not in main:
            err(f"JugcraftAgriculture.java must register {call} ({const})")
    states = set((load(ASSETS / "blockstates" / f"{vase['block']}.json") or {}).get("variants", {}))
    if states != {f"flowers={c},wilted={w}" for c in ["none"] + vase["colours"] for w in ("false", "true")}:
        err("The grave vase's blockstate must cover every bouquet, fresh and wilted")
    states = set((load(ASSETS / "blockstates" / f"{post['block']}.json") or {}).get("variants", {}))
    if states != {f"facing={f},lit={l},part={p}" for f in ("north", "east", "south", "west") for l in ("false", "true") for p in range(3)}:
        err("The lamp post's blockstate must cover every part, lit and not, facing every way")
    for block in (vase["block"], post["block"]):
        if f"block.jugcraft.{block}" not in lang or not (DATA / "jugcraft" / "recipe" / f"{block}.json").exists() \
                or not (DATA / "jugcraft" / "loot_table" / "blocks" / f"{block}.json").exists():
            err(f"{block} needs its words, recipe and loot")
    for colour, flowers in vase["flowers"].items():
        tag = load(DATA / "jugcraft" / "tags" / "item" / "grave_flowers" / f"{colour}.json") or {}
        if sorted(tag.get("values", [])) != sorted(flowers):
            err(f"jugcraft:grave_flowers/{colour} must list GRAVE_VASE['flowers']['{colour}'] of tools/graveyard.py")
    every = (load(DATA / "jugcraft" / "tags" / "item" / "grave_flowers.json") or {}).get("values", [])
    want = [f"#jugcraft:grave_flowers/{c}" for c in vase["flowers"]] + vase["others"] + [{"id": "#minecraft:small_flowers", "required": False}]
    if sorted(map(json.dumps, every)) != sorted(map(json.dumps, want)):
        err("jugcraft:grave_flowers must hold every colour's tag, GRAVE_VASE['others'] and vanilla's small flowers (optional)")
    if 'TagKey.create(Registries.ITEM, Jugcraft.id("grave_flowers"))' not in java.get("GraveVaseBlock", ""):
        err("GraveVaseBlock must take the flowers of jugcraft:grave_flowers")


def check_graveyard_buildings(java, main, lang):
    """Pack 3: the buildings' layout file holds every building of tools/graveyard.py with as many slots and lights as
    parts, every part's cell, shape and inscription where graveyard_data.py puts them, and no more inscriptions than a
    block entity keeps; Java reads it and registers the inscriptions component, the buildings' block entity and the
    door; each building's blockstate draws every part at every stage facing every way; models, loot (part 0, keeping
    every inscription), recipes, words and tags exist."""
    import graveyard_data as gyd
    layouts = load(ROOT / "src" / "main" / "resources" / "jugcraft" / "graveyard_buildings.json") or []
    if [entry.get("id") for entry in layouts] != list(gy.BUILDINGS):
        err("src/main/resources/jugcraft/graveyard_buildings.json must list BUILDINGS of tools/graveyard.py, in order")
    entity = java.get("HeadstoneBlockEntity", "")
    more = re.search(r"static final int MORE = (\d+);", entity)
    for entry in layouts:
        bid = entry.get("id")
        if bid not in gy.BUILDINGS:
            continue
        want = gyd.building_layout(bid)
        if entry != json.loads(json.dumps(want)):
            err(f"{bid}: graveyard_buildings.json differs from tools/graveyard_data.py (regenerate)")
        parts = len(entry["cells"])
        if len(entry["slots"]) != parts or len(entry["light"]) != parts or len(entry["shapes"]) != parts or entry["cells"][0] != [0, 0, 0]:
            err(f"{bid}: every part needs its slot, light and shape, and part 0 its own cell")
        if not more or len(entry["texts"]) > 1 + int(more.group(1)) or max(entry["slots"]) >= len(entry["texts"]):
            err(f"{bid}: more inscriptions than HeadstoneBlockEntity.MORE keeps, or a part cutting one it lacks")
        state = load(ASSETS / "blockstates" / f"{bid}.json") or {}
        drawn = {(c["when"]["facing"], c["when"]["part"], c["when"]["weathering"]) for c in state.get("multipart", [])}
        need = {(f, str(p), str(w)) for f in ("north", "east", "south", "west") for p in range(parts) for w in range(4)}
        if not need <= drawn:
            err(f"{bid}: its blockstate must draw every part at every stage facing every way")
        for c in state.get("multipart", []):
            if not (ASSETS / "models" / "block" / (c["apply"]["model"].split("/", 1)[1] + ".json")).exists():
                err(f"{bid}: missing model {c['apply']['model']}")
                break
        table = json.dumps(load(DATA / "jugcraft" / "loot_table" / "blocks" / f"{bid}.json") or {})
        if '"part": "0"' not in table or "jugcraft:epitaph" not in table or "jugcraft:inscriptions" not in table:
            err(f"{bid}: its loot must drop from part 0 only and keep every inscription")
        if not (DATA / "jugcraft" / "recipe" / f"{bid}.json").exists() or f"block.jugcraft.{bid}" not in lang:
            err(f"{bid} needs its recipe and words")
        tool = gy.BUILDINGS[bid]["tool"]
        if bid not in json.dumps(load(DATA / "minecraft" / "tags" / "block" / "mineable" / f"{tool}.json") or {}):
            err(f"{bid} must be mineable with a {tool}")
    for call in ('Jugcraft.id("inscriptions")', 'Jugcraft.id("graveyard_building")', '"/jugcraft/graveyard_buildings.json"',
                 "GraveyardBuildingBlock.create(props, building)", "registerGraveyardBuildings();"):
        if call not in main:
            err(f"JugcraftAgriculture.java must call {call}")
    door = gy.MAUSOLEUM_DOOR["id"]
    if f'MAUSOLEUM_DOOR = "{door}"' not in main or "new DoorBlock(BlockSetType.COPPER" not in main:
        err(f"JugcraftAgriculture.MAUSOLEUM_DOOR must be {door}, a door opened by hand")
    for path in (ASSETS / "blockstates" / f"{door}.json", ASSETS / "models" / "block" / f"{door}_bottom.json",
                 ASSETS / "models" / "block" / f"{door}_top.json", ASSETS / "textures" / "item" / f"{door}.png",
                 DATA / "jugcraft" / "loot_table" / "blocks" / f"{door}.json", DATA / "jugcraft" / "recipe" / f"{door}.json"):
        if not path.exists():
            err(f"The Bronze Mausoleum Door needs {path.relative_to(ROOT)}")

def check_ofrenda(java, main):
    """The ofrenda: OfrendaBlockEntity and OfrendaBlock match OFRENDA in tools/agriculture.py (slots, how often it looks,
    its ranges and light; the kinds of offering, in order); the ofrenda, petals, papel picado and sugar skull are
    registered and the marigold is one of MUMS; their models, words, loot, the offering tags, pan de muerto's cooking and
    the advancement exist."""
    of = ag.OFRENDA
    source = java.get("OfrendaBlockEntity", "") + java.get("OfrendaBlock", "")
    found = {name: float(value) for name, value in re.findall(r"static final (?:int|double|float) ([A-Z_]+) = ([\d.]+)[FD]?;", source)}
    expected = {"SLOTS": of["slots"], "CHECK_TICKS": of["check_ticks"], "WELCOME_RANGE": of["welcome_range"], "ARRIVED": of["arrived"],
                "WITNESS_RANGE": of["witness_range"], "LIGHT": of["light"]}
    for name, value in expected.items():
        if name not in found or abs(found[name] - value) > 1e-6:
            err(f"Ofrenda {name} = {found.get(name)} differs from OFRENDA in tools/agriculture.py ({value})")
    kinds = re.search(r"enum Kind \{\s*([A-Z_, ]+);", source)
    if not kinds or [k.strip().lower() for k in kinds.group(1).split(",")] != list(of["kinds"]):
        err("OfrendaBlockEntity.Kind differs from OFRENDA['kinds'] in tools/agriculture.py (in order)")
    for call in ('registerBlock("ofrenda", OfrendaBlock::new', 'registerBlock("marigold_petals", CarpetBlock::new', 'registerBlock("papel_picado"',
                 'registerBlock("sugar_skull", SugarSkullBlock::new'):
        if call not in main:
            err(f"JugcraftAgriculture.java must call {call}")
    if "marigold" not in ag.MUMS or ag.COOKING.get("pan_de_muerto", {}).get("input") != "pan_de_muerto_dough":
        err("The marigold must be one of MUMS, and pan de muerto must bake from its dough")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    for block in [of["block"]] + list(of["decor"]):
        if f"block.jugcraft.{block}" not in lang or not (ASSETS / "models" / "block" / f"{block}.json").exists():
            err(f"The ofrenda set needs the words and model of {block}")
        if not (DATA / "jugcraft" / "loot_table" / "blocks" / f"{block}.json").exists():
            err(f"{block} needs its loot table")
    tags = DATA / "jugcraft" / "tags" / "item" / "ofrenda"
    for path in [tags / f"{kind}.json" for kind in of["kinds"]] + [tags / "offerings.json", DATA / "jugcraft" / "advancement" / "remembered.json"]:
        if not path.exists():
            err(f"The ofrenda needs {path.relative_to(ROOT)}")


def check_model_uvs():
    """Minecraft 26.3 refuses to bake a block model face that reads outside its texture when the texture has
    see-through pixels ("Cannot compute translucency out of bounds"). Faces without a "uv" take theirs from the
    element's position, so a box reaching outside the block must pin its UVs."""
    def default_uv(side, lo, hi):
        fx, fy, fz = lo
        tx, ty, tz = hi
        return {"down": (fx, 16 - tz, tx, 16 - fz), "up": (fx, fz, tx, tz), "north": (16 - tx, 16 - ty, 16 - fx, 16 - fy),
                "south": (fx, 16 - ty, tx, 16 - fy), "west": (fz, 16 - ty, tz, 16 - fy), "east": (16 - tz, 16 - ty, 16 - fz, 16 - fy)}[side]

    see_through = {}

    def translucent(texture):
        if texture not in see_through:
            namespace, path = split(texture) if ":" in texture else (MOD, texture)
            image = ASSETS.parent / namespace / "textures" / f"{path}.png"
            see_through[texture] = image.exists() and Image.open(image).convert("RGBA").getextrema()[3][0] < 255
        return see_through[texture]

    for path in sorted((ASSETS / "models" / "block").glob("*.json")):
        model = load(path) or {}
        textures = model.get("textures", {})
        for element in model.get("elements", []):
            for side, face in element.get("faces", {}).items():
                texture = face.get("texture", "")
                for _ in range(4):
                    if texture.startswith("#"):
                        texture = textures.get(texture[1:], "")
                if not texture or not translucent(texture):
                    continue
                uv = face.get("uv") or default_uv(side, element["from"], element["to"])
                if min(uv) < 0 or max(uv) > 16:
                    err(f"{path.relative_to(ROOT)}: the {side} face reads {list(uv)} outside its see-through texture {texture}; pin its uv")


def check_material_sets():
    """The material sets (tools/material_icons.py, docs/MATERIAL_SETS.md): every map loads, every texture they draw is the
    committed PNG (CI does not re-run tools/generate_textures.py), every ore overlay is a clean cut-out, every ore model
    layers its overlay on vanilla's own stone or deepslate through the template, every metal's ramp steps from dark
    to light, and no two metals (vanilla's iron, gold and copper included) look alike."""
    import material_icons as mi
    from generate_textures import COPPER_METAL, GOLD_METAL, IRON_METAL
    for map_path in sorted(Path(mi.FOLDER).glob("*.txt")):
        try:
            mi.load(map_path.stem)
        except ValueError as exc:
            err(f"Material sets: {exc}")
    # The owner's ingot (6 October 2026): its pixels are kept exactly and only recoloured, for every metal. Its 16x12
    # rows sit in rows 2-13 of the map; edit its colours in material_icons.ingot_palette, never the map.
    owner_ingot = [
                    "..........33....",
                    ".......333553...",
                    "....3335666653..",
                    ".33356666666653.",
                    "3866666666668873",
                    "3586666668886351",
                    "3558668886433451",
                    "3555886433335551",
                    "345564333355411.",
                    ".345643344111...",
                    "..34542111......",
                    "...3311.........",
    ]
    try:
        ingot = mi.load("ingot")
        if ingot[2:14] != owner_ingot or any(set(row) != {"."} for row in ingot[:2] + ingot[14:]):
            err("Material sets: tools/material_icons/ingot.txt must be the owner's ingot exactly; recolour it in "
                "material_icons.ingot_palette, never redraw it")
    except ValueError:
        pass
    for (kind, name), image in mi.textures().items():
        path = ASSETS / "textures" / kind / f"{name}.png"
        if not path.is_file():
            err(f"Material sets: missing {kind}/{name}.png")
            continue
        with Image.open(path) as committed:
            if committed.convert("RGBA").tobytes() != image.tobytes():
                err(f"Material sets: {kind}/{name}.png differs from its map; run tools/generate_textures.py")
    if mi.OVERLAY:
        template = load(ASSETS / "models" / "block" / f"{mi.TEMPLATE}.json") or {}
        if template != mi.template():
            err(f"Material sets: models/block/{mi.TEMPLATE}.json is not the template; run tools/generate_material_data.py")
        elements = template.get("elements", [])
        if [(e.get("from"), e.get("to")) for e in elements] != [([0, 0, 0], [16, 16, 16])] * 2:
            err(f"Material sets: {mi.TEMPLATE} must be the stone cube then the overlay cube, both exactly 0..16 (as vanilla's grass_block)")
        for element in elements:
            for face, info in element.get("faces", {}).items():
                if info.get("uv") != [0, 0, 16, 16] or info.get("cullface") != face:
                    err(f"Material sets: {mi.TEMPLATE}'s {face} face needs uv [0, 0, 16, 16] and cullface {face}")
    for block, _ore, rock in mi.ore_blocks():
        model = load(ASSETS / "models" / "block" / f"{block}.json") or {}
        expected = mi.block_model(block) or {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MOD}:block/{block}"}}
        if model != expected:
            err(f"Material sets: models/block/{block}.json is not the {rock} ore model; run tools/generate_material_data.py")
        if mi.OVERLAY:
            with Image.open(ASSETS / "textures" / "block" / f"{block}.png") as overlay:
                alphas = set(overlay.convert("RGBA").getchannel("A").tobytes())
            if alphas != {0, 255}:
                err(f"Material sets: block/{block}.png must be cut out (alpha 0 or 255 only, some of each), not {sorted(alphas)[:4]}")
    for metal, tones in mi.METAL_RAMPS.items():
        lumas = [mi.luma(tone) for tone in tones]
        if any(b <= a for a, b in zip(lumas, lumas[1:])) or lumas[1] - lumas[0] < 30:
            err(f"Material sets: {metal}'s ramp must step from dark to light, D at least 30 luma above O")
    vanilla = {"vanilla iron": IRON_METAL, "vanilla gold": GOLD_METAL, "vanilla copper": COPPER_METAL}
    names = list(mi.METAL_RAMPS)
    for i, first in enumerate(names):
        for second, tones in [(n, mi.METAL_RAMPS[n]) for n in names[i + 1:]] + list(vanilla.items()):
            distance = mi.ramp_distance(mi.METAL_RAMPS[first], tones)
            if distance < mi.PAIR_FLOOR:
                err(f"Material sets: {first} and {second} look alike (CIEDE2000 {distance:.1f} over D, M and L, under "
                    f"{mi.PAIR_FLOOR:g}); move one ramp in tools/material_icons.py")


def check_art():
    """The art's geometry and texture rules for every block/item model and quad part (tools/art_check.py; the rules are
    in docs/ART_DIRECTION.md, Rules for everything): no shared or nearly shared face planes, UVs inside their sprites,
    closed models, and textures that suit their render type."""
    import art_check
    problems, summary = art_check.run(machine_blocks())
    for problem in problems:
        err(problem)
    print(summary)


def check_town():
    """The walled town: its data is written, its shops have no profit loop, every townsperson's skin and every line they
    can say exists, and the Java side names the same feature, shops screen ids and decor kinds."""
    import gzip
    import town_shops
    import town_lang
    import town_decor
    town_dir = DATA / MOD / "town"
    if not (town_dir / "town.json.gz").exists() or not (town_dir / "shops.json").exists():
        err("missing town data: run tools/generate_material_data.py (tools/town.py)")
        return
    for problem in town_shops.loop_errors():
        err(f"town shops: {problem}")
    shops = load(town_dir / "shops.json") or {}
    if shops.get("shops") != town_shops.data():
        err("data/jugcraft/town/shops.json differs from tools/town_shops.py: regenerate")
    data = json.loads(gzip.decompress((town_dir / "town.json.gz").read_bytes()))
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    skins = ASSETS / "textures" / "entity" / "townsfolk"
    for spot in data["spots"]:
        if not (skins / f"{spot['skin']}.png").exists():
            err(f"town: no skin texture {spot['skin']}")
        if spot["role"] == "shopkeeper" and spot.get("shop") not in town_shops.SHOPS:
            err(f"town: shopkeeper {spot['name']} keeps no known shop {spot.get('shop')}")
    for role in town_lang.LINES:
        for theme in town_decor.THEMES:
            for i in range(3):
                if f"message.jugcraft.townsfolk.{role}.{theme}.{i}" not in lang:
                    err(f"lang: missing townsfolk line {role}.{theme}.{i}")
    for theme in town_decor.THEMES:
        if f"theme.jugcraft.{theme}" not in lang:
            err(f"lang: missing theme name {theme}")
        for kind, themes in town_decor.KINDS.items():
            if theme not in themes:
                err(f"town decor: {kind} has nothing for {theme}")
    for shop in town_shops.SHOPS:
        if f"shop.jugcraft.{shop}" not in lang:
            err(f"lang: missing shop name {shop}")
    check_town_water(data)
    kinds = {site["kind"] for site in data["sites"]}
    unknown = kinds - set(town_decor.KINDS) - {"centerpiece"}
    if unknown:
        err(f"town: decor sites of unknown kinds {sorted(unknown)}")
    java = (JAVA_ROOT / "town" / "JugcraftTown.java").read_text(encoding="utf-8")
    if 'FEATURE = "town";' not in java or "town" not in FEATURES:
        err("JugcraftTown.FEATURE must be the town feature switch")
    menu = (JAVA_ROOT / "town" / "ShopMenu.java").read_text(encoding="utf-8")
    most = max(len(info["sells"]) for info in town_shops.SHOPS.values())
    base = int(re.search(r"SELL_BASE = (\d+);", menu).group(1))
    if most > base or max(len(info["buys"]) for info in town_shops.SHOPS.values()) + base > 127:
        err(f"ShopMenu.SELL_BASE {base} cannot number every offer as a menu button")


# Blocks that can hold water (waterloggable) or let it through: beside the town's water they would spill it.
TOWN_WATER_LEAKY = re.compile(r"(wall|slab|stairs|fence|pane|bars|lantern|chain|sign|trapdoor|door|leaves|ladder|campfire|candle|"
                              r"coral|scaffolding|lightning|chest|rail|flower_pot|amethyst|dripleaf|sea_pickle|grate|conduit|"
                              r"button|lever|torch|carpet|banner|plate|hopper)")


def check_town_water(data):
    """The town's water stays put: no water block has air beside or under it, and no block that can hold water (a
    wall, a slab, stairs ...) touches two water blocks, or the game's infinite-water rule fills it and it spills over
    whatever is beyond (the fountain's rim did in CI)."""
    import base64
    size, height, ymin = data["size"], data["height"], data["y_min"]
    palette = data["palette"]
    raw = base64.b64decode(data["blocks"])
    vol = [int.from_bytes(raw[i:i + 2], "little") for i in range(0, len(raw), 2)]

    def at(x, y, z):
        if not (0 <= x < size and 0 <= z < size and ymin <= y < ymin + height):
            return None
        return vol[((y - ymin) * size + z) * size + x]

    def water(i):
        return i is not None and i > 1 and palette[i].startswith("minecraft:water")
    cells = [(x, y, z) for y in range(ymin, ymin + height) for z in range(size) for x in range(size) if water(at(x, y, z))]
    leaky = set()
    for x, y, z in cells:
        below = at(x, y - 1, z)
        if below == 1:
            err(f"town water at {x} {y} {z} has air under it")
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = at(x + dx, y, z + dz)
            if n == 1:
                err(f"town water at {x} {y} {z} has air beside it")
            elif n is not None and n > 1 and not water(n) and TOWN_WATER_LEAKY.search(palette[n]):
                wet = sum(water(at(x + dx + ex, y, z + dz + ez)) for ex, ez in ((1, 0), (-1, 0), (0, 1), (0, -1)))
                if wet >= 2 and (x + dx, y, z + dz) not in leaky:
                    leaky.add((x + dx, y, z + dz))
                    err(f"town: {palette[n]} at {x + dx} {y} {z + dz} touches {wet} water blocks and would fill with water")


def check_pixel_hollows():
    """The cave's shards stay finite, and the Retro Trader's Java numbers match tools/pixel_hollows.py without a
    profit loop between his shard sale and buyback."""
    for path in sorted((DATA / MOD / "recipe").rglob("*.json")):
        if f'"{MOD}:{ph.SHARD}"' in json_result(path):
            err(f"{path.name}: makes pixel shards (they must only come from clusters and trade)")

    trader = (WORLD_JAVA / "RetroTrader.java").read_text(encoding="utf-8")
    if f"SHOP_WEIGHT = {ph.SHOP_WEIGHT};" not in trader:
        err("RetroTrader.SHOP_WEIGHT differs from tools/pixel_hollows.py")
    listed = re.search(r'VILLAGE_HOUSES =\s*List\.of\(([^;]*)\);', trader)
    names = {const: village for const, village in re.findall(r'([A-Z_]+) = houses\("([a-z]+)"\)', trader)}
    villages = [names.get(item.strip(), item.strip()) if not item.strip().startswith("houses(") else
                re.match(r'houses\("([a-z]+)"\)', item.strip()).group(1)
                for item in (listed.group(1).split(",") if listed else [])]
    if villages != ph.SHOP_VILLAGES:
        err(f"RetroTrader.VILLAGE_HOUSES {villages} differs from SHOP_VILLAGES {ph.SHOP_VILLAGES} in tools/pixel_hollows.py")
    for level in ph.TRADE_LEVELS:
        if f'"retro_trader/level_{level}"' not in trader:
            err(f"RetroTrader's profession does not name the level {level} trade set")
    maps = (WORLD_JAVA / "PixelHollowsMaps.java").read_text(encoding="utf-8")
    for name, java in (("radius", "RADIUS"), ("step", "STEP"), ("vertical_step", "VERTICAL_STEP"), ("start_y", "START_Y")):
        if f"{java} = {ph.MAP_SEARCH[name]};" not in maps:
            err(f"PixelHollowsMaps.{java} differs from MAP_SEARCH in tools/pixel_hollows.py")
    for path in sorted((DATA / MOD / "villager_trade").rglob("*.json")):
        trade = load(path) or {}
        for key in ("wants", "additional_wants", "gives"):
            ref = trade.get(key, {}).get("id", "")
            if split(ref)[0] == MOD and split(ref)[1] not in (set(all_items()) | set(ph.blocks()) | set(ph.items())
                                                   | set(ag.all_blocks()) | set(ag.all_items())):
                err(f"villager_trade {path.stem}: unknown item {ref}")

    # Cheapest shard sale: a discount can bring the price down to 1 emerald. Best buyback: price multiplier 0 means no
    # reputation discount; Hero of the Village V takes floor(0.55 * count) (at least 1) off.
    shard = f"{MOD}:{ph.SHARD}"
    sale = min(1 / t["gives"][1] for t in ph.TRADES.values() if t["wants"][0] == "minecraft:emerald" and t["gives"][0] == shard)
    for name, t in ph.TRADES.items():
        if t["wants"][0] == shard:
            if t["reputation_discount"] != 0:
                err(f"{name}: the shard buyback needs reputation_discount 0, or discounts make a profit loop")
            best = t["gives"][1] / max(1, t["wants"][1] - max(1, int(0.55 * t["wants"][1])))
            if best >= sale:
                err(f"{name} pays {best:.3f} emeralds per shard, but a shard can be bought for {sale:.3f}: a profit loop")


def json_result(path):
    """A recipe's outputs as text: its "result", or the "results" of machine recipes."""
    data = load(path) or {}
    return json.dumps([data.get("result", {}), data.get("results", [])])


def check_deposits():
    """Surface deposits: Java registration, worldgen and capacity match tools/deposits.py."""
    java = (JAVA_ROOT / "deposit" / "JugcraftDeposits.java").read_text(encoding="utf-8")
    found = dict(re.findall(r'DEPOSITS\.put\("([a-z_]+)", [^;]*?"([a-z_]+)"\)\);', java))
    expected = {name: info["yield"].split(":")[1] for name, info in deposits.DEPOSITS.items()}
    if found != expected:
        err(f"JugcraftDeposits.java deposits {found} != tools/deposits.py {expected}")
    if f"CAPACITY = {deposits.CAPACITY:_}" not in (JAVA_ROOT / "deposit" / "Deposits.java").read_text(encoding="utf-8"):
        err(f"Deposits.CAPACITY is not {deposits.CAPACITY:_} as in tools/deposits.py")
    worldgen = WORLDGEN.read_text(encoding="utf-8")
    for name, info in deposits.DEPOSITS.items():
        call = "addDeposit(stonyHills, " + ", ".join(f'"{v}"' for v in [name] + info["features"]) + ");"
        if call not in worldgen:
            err(f"JugcraftWorldgen does not add {name} with features {info['features']}")
        for feature in info["features"]:
            if feature not in FEATURES:
                err(f"{name}: unknown feature {feature}")
        if not (DATA / MOD / "worldgen" / "placed_feature" / f"{name}.json").is_file():
            err(f"{name}: no placed feature")
    for item in (info["yield"] for info in deposits.DEPOSITS.values()):
        if split(item)[0] == MOD and split(item)[1] not in all_items():
            err(f"Deposit yield {item} is not a Jugcraft item")
    stats = STATS["deposit_drill"]
    kinds = MACHINE_JAVA.read_text(encoding="utf-8")
    for key, constant in (("ticks", "DEPOSIT_TICKS"), ("units", "DEPOSIT_UNITS"), ("reach", "DEPOSIT_REACH"),
                          ("depth", "DEPOSIT_DEPTH")):
        if f"int {constant} = {stats[key]};" not in kinds:
            err(f"MachineKind.{constant} is not {stats[key]} as in tools/machines.py")


def check_nuclear():
    """client/HazmatLayer.java reads the suit sheet from tools/nuclear.py's slots, and the Java registers its items."""
    import nuclear
    layer = (ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client" / "HazmatLayer.java").read_text(encoding="utf-8")
    found = {name.lower(): (int(c), int(r)) for name, c, r in re.findall(r"static final float\[\] (\w+) = region\((\d), (\d)\);", layer)}
    for name in nuclear.HAZMAT_MATERIALS:
        if found.get(name) != nuclear.slot(name):
            err(f"HazmatLayer.java region for {name} is {found.get(name)}, tools/nuclear.py has {nuclear.slot(name)}")
    java = (JAVA_ROOT / "nuclear" / "JugcraftNuclear.java").read_text(encoding="utf-8")
    for item in nuclear.ITEMS + ["shielding_concrete"]:
        if f'"{item}"' not in java:
            err(f"JugcraftNuclear.java does not register {item}")
    kinds = (JAVA_ROOT / "nuclear" / "EquipmentKind.java").read_text(encoding="utf-8")
    found = [(i, int(w), int(d), int(h)) for i, w, d, h in re.findall(r'[A-Z_]+\("([a-z_]+)", (\d+), (\d+), (\d+)\)', kinds)]
    expected = [(k, w, d, h) for k, (_, w, d, h) in nuclear.EQUIPMENT.items()]
    if found != expected:
        err(f"EquipmentKind.java {found} != tools/nuclear.py EQUIPMENT {expected}")
    pipes = re.findall(r'[A-Z_]+\("([a-z_]+)", (\d+)\)', (JAVA_ROOT / "nuclear" / "PipeKind.java").read_text(encoding="utf-8"))
    if [(p, int(n)) for p, n in pipes] != [(k, v[1]) for k, v in nuclear.PIPES.items()]:
        err(f"PipeKind.java {pipes} != tools/nuclear.py PIPES")
    # The cooling tower controller's kinds name the tower blueprints it checks itself against, and the plume heights
    # match the towers tools/nuclear_plants.py draws (top of the shell above the controller).
    towers = re.findall(r'[A-Z]+\("(\w+)", "(\w+)", [\d.]+, (\d+), \d+, new int', (JAVA_ROOT / "nuclear" / "CoolingTowerBlock.java").read_text(encoding="utf-8"))
    large = (ROOT / "src" / "main" / "resources" / "data" / MOD / "blueprint" / "large.txt").read_text(encoding="utf-8").split()
    if [t[0] for t in towers] != nuclear.TOWERS:
        err(f"CoolingTowerBlock.Tower {[t[0] for t in towers]} != tools/nuclear.py TOWERS {nuclear.TOWERS}")
    for kind, blueprint, height in towers:
        if blueprint not in large:
            err(f"CoolingTowerBlock.Tower {kind} checks blueprint {blueprint}, which is not a built-in")
            continue
        data = json.loads((ROOT / "src" / "main" / "resources" / "data" / MOD / "blueprint" / f"{blueprint}.jugbp.json").read_text(encoding="utf-8"))
        tops = [y for y, rows in enumerate(data["layers"]) if any(rows)]
        controller_y = 0  # the controller is in the bottom layer, under the basin floor
        if int(height) != max(tops) - controller_y:
            err(f"CoolingTowerBlock.Tower {kind} plume height {height} != tower top {max(tops)} - controller {controller_y}")
    renderer = (ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client" / "NuclearRenderer.java").read_text(encoding="utf-8")
    regions = {name.lower(): (int(c), int(r)) for name, c, r in re.findall(r"static final float\[\] (\w+) = region\((\d), (\d)\);", renderer)}
    for name in nuclear.EQ_MATERIALS:
        if regions.get(name) != nuclear.eq_slot(name):
            err(f"NuclearRenderer.java region for {name} is {regions.get(name)}, tools/nuclear.py has {nuclear.eq_slot(name)}")


def check_diagonal_connections():
    """Diagonal connections (tools/diagonal_connections.py): every block in #jugcraft:connects_diagonally has a
    blockstate with one arm part for each diagonal, turned toward it, and an arm model of turned elements; every
    Jugcraft blockstate shaped like a fence, pane or bars (a part for each straight direction) or like a wall (a low or
    tall part for each) is in the tag; vanilla's rebuilt blockstates keep vanilla's own parts; vanilla's walls are left
    as they are and each has a diagonal wall (its arms, post and low sides) in #minecraft:walls; and the Java property
    names match."""
    tag = set((load(RES / "data" / MOD / "tags" / "block" / "connects_diagonally.json") or {}).get("values", []))
    if tag != set(dg.blocks()):
        err(f"#{dg.TAG} differs from tools/diagonal_connections.py: {sorted(tag ^ set(dg.blocks()))}")
    for block in sorted(tag):
        name = block.split(":")[1]
        namespace, state_name = dg.arm_blockstate(block)
        parts = (load(RES / "assets" / namespace / "blockstates" / f"{state_name}.json") or {}).get("multipart", [])
        models = dg.arm_models(block)
        arms = {}
        for part in parts:
            diagonals = set(part.get("when", {})) & set(dg.DIAGONAL_NAMES)
            if diagonals:
                arms.setdefault(diagonals.pop(), []).append(part["apply"])
        if sorted(arms) != sorted(dg.DIAGONAL_NAMES) or any(sorted(a["model"] for a in arms[d]) != sorted(models) for d in arms):
            err(f"{block}: needs one diagonal arm part for each of {dg.DIAGONAL_NAMES} and each of {list(models)}, has {arms}")
            continue
        for diagonal, y in dg.DIAGONALS:
            if any(apply.get("y", 0) != y for apply in arms[diagonal]):
                err(f"{block}: its {diagonal} arms should be turned y={y}")
        for arm in models:
            model = load(ASSETS / "models" / "block" / "diagonal" / f"{arm.split('/')[-1]}.json") or {}
            elements = model.get("elements", [])
            if not elements or any(e.get("rotation", {}).get("axis") != "y" or e["rotation"].get("angle") != dg.ANGLE
                                   or not e["rotation"].get("rescale") for e in elements):
                err(f"{block}: its diagonal arm model {arm} needs elements turned {dg.ANGLE} degrees about y, with rescale")
            if not model.get("parent"):
                err(f"{block}: its diagonal arm model {arm} needs the block's side model as parent (for its textures)")
    vanilla = dg.vanilla_blockstates()
    for name, (own, _side, _kind) in vanilla.items():
        parts = (load(RES / "assets" / "minecraft" / "blockstates" / f"{name}.json") or {}).get("multipart", [])
        if [p for p in parts if not set(p.get("when", {})) & set(dg.DIAGONAL_NAMES)] != own:
            err(f"minecraft:{name}: the rebuilt blockstate lost vanilla's own parts")
    walls = set((load(RES / "data" / "minecraft" / "tags" / "block" / "walls.json") or {}).get("values", []))
    for namespace, name in dg.all_walls():
        diagonal = dg.DIAGONAL_WALL.format(name)
        if (RES / "assets" / "minecraft" / "blockstates" / f"{name}.json").exists():
            err(f"minecraft:{name}: its blockstate is overridden; vanilla's walls are left as they are")
        parts = (load(ASSETS / "blockstates" / f"{diagonal}.json") or {}).get("multipart", [])
        if [p for p in parts if not set(p.get("when", {})) & set(dg.DIAGONAL_NAMES)] != dg.diagonal_wall(name, namespace):
            err(f"{MOD}:{diagonal}: needs the wall's post and low sides, as tools/diagonal_connections.py writes them")
        if f"{MOD}:{diagonal}" not in walls:
            err(f"{MOD}:{diagonal} is not in #minecraft:walls, so walls, gates and bars would not join it")
    for path in sorted((ASSETS / "blockstates").glob("*.json")):
        if path.stem.startswith(dg.DIAGONAL_WALL.format("")):
            continue
        parts = (load(path) or {}).get("multipart", [])
        sides = {next(iter(p["when"])) for p in parts if len(p.get("when", {})) == 1 and list(p["when"].values()) == ["true"]}
        if {"north", "east", "south", "west"} <= sides and not sides & {"up", "down"} and f"{MOD}:{path.stem}" not in tag:
            err(f"{MOD}:{path.stem} joins like a fence but is not in #{dg.TAG}; add it to tools/diagonal_connections.py")
        heights = {next(iter(p["when"])) for p in parts
                   if len(p.get("when", {})) == 1 and list(p["when"].values())[0] in ("low", "tall")}
        if {"north", "east", "south", "west"} <= heights and f"{MOD}:{path.stem}" not in tag:
            err(f"{MOD}:{path.stem} joins like a wall but is not in #{dg.TAG}; add it to tools/diagonal_connections.py")
    java = (ROOT / "src" / "main" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "diagonal" / "DiagonalConnections.java")
    source = java.read_text(encoding="utf-8") if java.exists() else ""
    for diagonal, _y in dg.DIAGONALS:
        if f'"{diagonal}"' not in source:
            err(f"diagonal/DiagonalConnections.java does not name the property {diagonal}")
    if f'"{dg.TAG.split(":")[1]}"' not in source:
        err(f"diagonal/DiagonalConnections.java does not name the tag {dg.TAG}")
    walls_java = java.with_name("DiagonalWalls.java")
    if f'"{dg.DIAGONAL_WALL.format("")}"' not in (walls_java.read_text(encoding="utf-8") if walls_java.exists() else ""):
        err(f"diagonal/DiagonalWalls.java does not register the diagonal walls as {dg.DIAGONAL_WALL}")


def main():
    registered = (set(all_blocks()) | set(all_items()) | set(machine_blocks()) | set(machine_items())
                  | set(ag.all_blocks()) | set(ag.all_items()) | set(petro.petro_items()) | set(petro.petro_blocks())
                  | set(deposits.DEPOSITS) | set(guide_books.BOOKS) | set(tank_display.BLOCKS)
                  | set(arms.items()) | set(arms_variants.items())
                  | set(gear.items()) | set(plastic.blocks()) | set(exosuit.items()) | set(grapple.items())
                  | set(field_chemistry.items()) | set(construction.items()) | set(construction.blocks()) | set(gas_storage.items()) | set(control_electronics.blocks()) | set(control_electronics.items()) | set(rocketry.items()) | set(rocketry.blocks()) | set(dieselworks.blocks()) | set(kaiserworks.blocks()) | set(trenchworks.blocks()) | set(fortifications.blocks()) | set(bunkerworks.blocks()) | set(fire_control.blocks()) | set(fire_control.items()) | set(raiders.ITEMS) | set(armoured_walker.ITEMS) | set(zeppelin.ITEMS) | set(mech.ITEMS) | set(landship.ITEMS) | set(artillery.ITEMS) | set(tower_guns.items())
                  | set(ph.blocks()) | set(ph.items()) | set((town_assets.blocks() + styx.blocks())))
    check_assets(sorted(registered))
    check_model_textures()
    check_petro()
    check_loot(registered)
    check_recipes(registered)
    check_machine_recipe_files(registered)
    check_fluid_recipes(registered)
    check_tags()
    check_worldgen()
    check_java()
    check_deposits()
    check_gear()
    check_thallite_gear()
    check_arms()
    check_item_icons()
    check_nuclear_sounds()
    check_arms_variants()
    check_mesh_models()
    check_arms_motion()
    check_flail_heads()
    check_exosuit()
    check_worn_armor()
    check_grapple()
    check_field_chemistry()
    check_construction()
    check_hydroponics()
    check_electroplating()
    check_gas_storage()
    check_control_electronics()
    check_rocketry()
    check_dieselworks()
    check_kaiserworks()
    check_trenchworks()
    check_zeppelin()
    check_walker()
    check_armoured_walker()
    check_landship()
    check_artillery()
    check_tower_guns()
    check_gun_art()
    check_quad_names()
    check_fortifications()
    check_bunkerworks()
    check_fire_control()
    check_raiders()
    check_plastic()
    check_seasons()
    check_alpine()
    check_biomes()
    check_wood_switches()
    check_machines(registered)
    check_large_machines()
    check_style_pack()
    check_machine_rotors()
    check_drones()
    check_tower()
    check_power_lines()
    check_nuclear()
    check_guide_books()
    check_handbook(registered)
    check_agriculture()
    check_recipe_categories()
    check_advancements(registered)
    check_model_uvs()
    check_material_sets()
    check_art()
    check_pixel_hollows()
    check_town()
    check_diagonal_connections()
    for path in RES.rglob("*.json"):
        load(path)
    if errors:
        print("\n".join(errors), file=sys.stderr)
        sys.exit(1)
    print(f"PASS: {len(registered)} material IDs, data files and recipe audit. No Minecraft build or game test performed.")


if __name__ == "__main__":
    main()
