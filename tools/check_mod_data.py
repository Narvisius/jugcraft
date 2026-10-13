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
import kitchen
import feasts
import menu
import rice
import soil
import orchard
import cakes
import cake_data
import pie_tart_data
import pies_and_tarts
import milkshake_data
import milkshakes
import garden
import vegetables
import herbs
import spices
import owner_art
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
import industrial_forms
import concordance
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
import guns
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
CLIENT_JAVA_ROOT = ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client"
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
                  # The Concordance's interaction and harvesting lists (tools/concordance.py), as optional entries.
                  "minecraft:wooden_trapdoors", "minecraft:small_flowers",
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
        elif not animated and img.size not in ((16, 16), (32, 32), (48, 48), (64, 64), (128, 128)):
            err(f"Texture {ref} is {img.size}, expected 16x16, 32x32, 48x48, 64x64 or 128x128")


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


def item_models(definition):
    """Every model an item definition can show, through select (the blueprint's kinds), condition and range_dispatch
    (the power bow's draw)."""
    if "model" in definition:
        if isinstance(definition["model"], str):
            model(definition["model"])
        else:
            item_models(definition["model"])  # a special model's renderer (GeckoLib's guns) names no model of its own
    if definition.get("type") == "minecraft:special":
        model(definition["base"])  # the base model gives a special model its display transforms and particle
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
                  + construction.blocks() + control_electronics.blocks() + rocketry.blocks() + dieselworks.blocks() + kaiserworks.blocks() + trenchworks.blocks() + fortifications.blocks() + bunkerworks.blocks() + fire_control.blocks()
                  + concordance.blocks() + guns.blocks() + industrial_forms.blocks()):
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
                        + construction.blocks() + control_electronics.blocks() + rocketry.blocks() + dieselworks.blocks() + kaiserworks.blocks() + trenchworks.blocks() + fortifications.blocks() + bunkerworks.blocks() + fire_control.blocks()
                        + concordance.blocks() + guns.blocks() + industrial_forms.blocks()) and f"item.{MOD}.{item}" not in lang:
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
        if metal in MINERALS or path in {info["tag"] for info in ITEMS.values()} or path in ("fermentable", "grave_flowers", "mourning_flowers") \
                or ns == MOD and form == "concordance":
            return {}  # ... and the Concordance's own item tags (luminous matter) hold no metal
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
            or path in construction.items() or path in construction.blocks() or path in gas_storage.items() or path in control_electronics.blocks() or path in control_electronics.items() or path in rocketry.items() or path in rocketry.blocks() or path in dieselworks.blocks() or path in kaiserworks.blocks() or path in trenchworks.blocks() or path in fortifications.blocks() or path in bunkerworks.blocks() or path in fire_control.blocks() or path in fire_control.items() or path in raiders.ITEMS or path in armoured_walker.ITEMS or path in zeppelin.ITEMS or path in mech.ITEMS or path in landship.ITEMS or path in artillery.ITEMS or path in tower_guns.items()\
            or path in guns.items() or path in guns.blocks() or path in concordance.items() or path in concordance.blocks() or path in industrial_forms.blocks():
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
SPECIAL_RECIPES = {f"{MOD}:{decor18.KEY['recipe']}", f"{MOD}:gun_attachment", f"{MOD}:gun_attachment_removal"}


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
                 "villager_trade": {f"retro_trader/{name}" for name in ph.TRADES},
                 "spell": set(concordance.INVOCATIONS) | {"composed"}}


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
    expected = (sum(len(r) for r in petro.FLUID_RECIPES.values()) + sum(len(r) for r in drones.DRONE_FLUID_RECIPES.values())
                + sum(len(r) for r in industrial_forms.FORM_RECIPES.values()))
    types = {spec["recipe_type"] for spec in petro.FLUID_MACHINES.values() if spec["recipe_type"]}
    files = [p for p in (DATA / MOD / "recipe").glob("*/*.json") if p.parent.name in types]
    if len(files) != expected:
        err(f"{len(files)} fluid recipe files, but tools/petro.py, tools/drones.py and tools/industrial_forms.py define {expected}")
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
    # Shared generation must retain both kitchen tools and combat equipment.
    from kitchen import KNIVES
    for tag in ("enchantable/melee_weapon", "enchantable/durability"):
        actual = set((load(DATA / "minecraft" / "tags" / "item" / f"{tag}.json") or {}).get("values", []))
        expected = set(arms.item_tags().get(tag, [])) | set(arms_variants.item_tags().get(tag, []))
        expected.update(f"{MOD}:{knife}" for knife in KNIVES)
        if missing := expected - actual:
            err(f"{tag}: shared equipment entries were overwritten: {sorted(missing)}")
    for path in sorted(DATA.rglob("tags/*/**/*.json")):
        registry = path.relative_to(DATA).parts[2]
        known = OTHER_ENTRIES.get(registry) or set(all_blocks() + all_items() + machine_blocks() + machine_items()
                                                    + petro.petro_blocks() + petro.petro_items() + list(deposits.DEPOSITS)
                                                    + list(tank_display.BLOCKS) + seasons.BLOCKS + ph.blocks() + ph.items()
                                                    + arms.items() + arms_variants.items()
                                                    + gear.items() + plastic.blocks() + exosuit.items() + grapple.items()
                                                    + field_chemistry.items() + construction.items() + construction.blocks() + gas_storage.items() + control_electronics.blocks() + control_electronics.items() + rocketry.items() + rocketry.blocks() + dieselworks.blocks() + kaiserworks.blocks() + trenchworks.blocks() + fortifications.blocks() + bunkerworks.blocks() + fire_control.blocks() + fire_control.items() + list(raiders.ITEMS) + list(raiders.BLOCKS) + list(armoured_walker.ITEMS) + list(zeppelin.ITEMS) + list(mech.ITEMS) + list(landship.ITEMS) + list(artillery.ITEMS) + tower_guns.items()
                                                    + ag.all_blocks() + ag.all_items() + (town_assets.blocks() + styx.blocks())
                                                    + guns.items() + guns.blocks() + concordance.items() + concordance.blocks() + concordance.itemless_blocks())
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
    order = ["raider_grunt", "raider_grenadier", "raider_officer", "raider_blimp", "raider_walker", "raider_gunner"]
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


def check_guns():
    """guns/JugcraftGuns.java and client/guns/GunAnimations.java against tools/guns.py: each gun's numbers, the rounds,
    the sound events and the animation sounds' aliases; then tools/guns.py's own check that every GeckoLib model gives
    back the owner's parts face for face, that the animations and sounds are the library's files unchanged, and that
    every sound an animation plays exists."""
    java = (JAVA_ROOT / "guns" / "JugcraftGuns.java").read_text(encoding="utf-8")
    for gun, spec in guns.GUNS.items():
        reload, start, each, finish = (spec["reload"], 0, 0, 0) if isinstance(spec["reload"], int) else (0, *spec["reload"])
        line = (f'SPECS.put("{gun}", new GunSpec({spec["damage"]}F, {spec["pellets"]}, {spec["interval"]}, '
                f'{str(spec["auto"]).lower()}, {spec["capacity"]}, {reload}, {start}, {each}, {finish}, {spec["spread"][0]}F, '
                f'{spec["spread"][1]}F, {spec["range"]}, "{spec["ammo"]}"));')
        if line not in java:
            err(f"JugcraftGuns.SPECS differs from tools/guns.py for {gun}: expected {line}")
    for kind, att in guns.ATTACHMENTS.items():
        effects = ", ".join(f"{float(att['effects'].get(e, 1.0))}F" for e in guns.EFFECTS)
        line = (f'ATTACHMENTS.put("{kind}", new GunAttachment("{att["slot"]}", {str(att["replaces"]).lower()}, {effects}, '
                f'{float(att.get("stab", 0.0))}F));')
        if line not in java:
            err(f"JugcraftGuns.ATTACHMENTS differs from tools/guns.py for {kind}: expected {line}")
    accepts = {gun: re.findall(r'"([a-z_]+)"', listed) for gun, listed in re.findall(r'ACCEPTS\.put\("([a-z_]+)", List\.of\(([^)]*)\)\)', java)}
    expected = {gun: guns.fits(gun) for gun in guns.GUNS if guns.fits(gun)}
    if accepts != expected:
        err(f"JugcraftGuns.ACCEPTS differs from tools/guns.py fits(): {accepts} != {expected}")
    slots = re.search(r"SLOTS = List\.of\(([^)]*)\)", java)
    if not slots or re.findall(r'"([a-z_]+)"', slots.group(1)) != list(guns.SLOTS):
        err(f"JugcraftGuns.SLOTS differs from tools/guns.py {list(guns.SLOTS)}")
    listed = re.search(r"AMMO = List\.of\(([^)]*)\)", java)
    if not listed or re.findall(r'"([a-z_]+)"', listed.group(1)) != list(guns.AMMO):
        err(f"JugcraftGuns.AMMO differs from tools/guns.py {list(guns.AMMO)}")
    # Slice 8C: what the heavy weapons fire, their spin-up and the ammunition an item of which loads several rounds
    # (more than Map.of's ten pairs since slice 10A).
    shots = ", ".join(f'Map.entry("{gun}", "{spec["shot"]}")' for gun, spec in guns.GUNS.items()
                      if spec.get("shot", "bullet") != "bullet")
    if f"SHOTS = Map.ofEntries({shots});" not in java:
        err(f"JugcraftGuns.SHOTS differs from tools/guns.py GUNS shot: expected Map.ofEntries({shots})")
    # Slice 10A: the rocket guns' rockets' speed.
    speeds = ", ".join(f'"{gun}", {spec["rocket_speed"]}F' for gun, spec in guns.GUNS.items() if "rocket_speed" in spec)
    if f"ROCKET_SPEED = Map.of({speeds});" not in java:
        err(f"JugcraftGuns.ROCKET_SPEED differs from tools/guns.py GUNS rocket_speed: expected Map.of({speeds})")
    spins = ", ".join(f'"{gun}", {spec["spin_up"]}' for gun, spec in guns.GUNS.items() if spec.get("spin_up"))
    if f"SPIN_UP = Map.of({spins});" not in java:
        err(f"JugcraftGuns.SPIN_UP differs from tools/guns.py GUNS spin_up: expected Map.of({spins})")
    per_item = ", ".join(f'"{ammo}", {n}' for ammo, n in guns.OTHER_AMMO.items() if n != 1)
    if f"PER_ITEM = Map.of({per_item});" not in java:
        err(f"JugcraftGuns.PER_ITEM differs from tools/guns.py OTHER_AMMO: expected Map.of({per_item})")
    # Slice 10G: the guns a player may hold two of, one in each hand, and how much further each strays.
    one_handed = ", ".join(f'"{gun}"' for gun in guns.one_handed())
    if f"ONE_HANDED = Set.of({one_handed});" not in java:
        err(f"JugcraftGuns.ONE_HANDED differs from tools/guns.py one_handed(): expected Set.of({one_handed})")
    if f"DUAL_SPREAD = {guns.DUAL_SPREAD}F;" not in java:
        err(f"JugcraftGuns.DUAL_SPREAD differs from tools/guns.py: expected {guns.DUAL_SPREAD}F")
    # Slice 11A: the Nether guns, whose bullets set what they hurt alight, and for how long.
    incendiary = ", ".join(f'"{gun}"' for gun in guns.INCENDIARY)
    if f"INCENDIARY = Set.of({incendiary});" not in java:
        err(f"JugcraftGuns.INCENDIARY differs from tools/guns.py: expected Set.of({incendiary})")
    for gun in guns.INCENDIARY:
        if gun not in guns.GUNS or guns.GUNS[gun].get("shot", "bullet") != "bullet":
            err(f"tools/guns.py INCENDIARY names {gun}, which is not a gun that fires bullets")
    shots_java = (JAVA_ROOT / "guns" / "GunShots.java").read_text(encoding="utf-8")
    if f"BURN_SECONDS = {guns.BURN_SECONDS};" not in shots_java:
        err(f"GunShots.BURN_SECONDS differs from tools/guns.py: expected {guns.BURN_SECONDS}")
    # Slice 8D: the energy weapons' charge a round, the arcs' leaps and the Energy Cell's capacity.
    charges = ", ".join(f'"{gun}", {spec["charge"]}' for gun, spec in guns.GUNS.items() if spec.get("charge"))
    if f"CHARGE = Map.of({charges});" not in java:
        err(f"JugcraftGuns.CHARGE differs from tools/guns.py GUNS charge: expected Map.of({charges})")
    for line in (f"ARC_HOPS = {guns.ARC_HOPS};", f"ARC_REACH = {guns.ARC_REACH};", f"ARC_SHARE = {guns.ARC_SHARE}F;"):
        if line not in java:
            err(f"JugcraftGuns differs from tools/guns.py: expected {line}")
    cell = (JAVA_ROOT / "guns" / "EnergyCellItem.java").read_text(encoding="utf-8")
    if f"CAPACITY = {guns.CELL_CAPACITY:_};" not in cell or "implements Chargeable" not in cell:
        err(f"EnergyCellItem must be Chargeable, holding tools/guns.py CELL_CAPACITY ({guns.CELL_CAPACITY:_} JE)")
    # Slice 10F: guns in mobs' hands (guns/MobGuns), the raider gunners' arms and how they fire them.
    mob = (JAVA_ROOT / "guns" / "MobGuns.java").read_text(encoding="utf-8")
    arms = ", ".join(f'new Arm("{gun}", {weight}, {reach})' for gun, weight, reach in guns.MOB_ARMS)
    arms_java = re.search(r"ARMS = List\.of\(([^;]*)\);", mob)
    if not arms_java or " ".join(arms_java.group(1).split()) != arms:
        err(f"MobGuns.ARMS differs from tools/guns.py MOB_ARMS: expected {arms}")
    for line in (f"DAMAGE = {guns.MOB_DAMAGE}F;", f"BURST = {guns.MOB_BURST};", f"PAUSE = {guns.MOB_PAUSE};",
                 f"DROP = {guns.MOB_DROP}F;", f"REACH = {guns.MOB_REACH};"):
        if line not in mob:
            err(f"MobGuns differs from tools/guns.py: expected {line}")
    for gun, _, _ in guns.MOB_ARMS:
        if gun not in guns.GUNS or guns.GUNS[gun].get("shot", "bullet") != "bullet":
            err(f"tools/guns.py MOB_ARMS names {gun}, which is not a gun that fires bullets")
    # Slice 10E: the Cell Rack's cradles, where tools/guns.py builds them, as the block picks them and as the renderer
    # stands the cells in them.
    columns = ", ".join(f"{x}" for x in guns.RACK_COLUMNS)
    if f"COLUMNS = {{{columns}}};" not in (JAVA_ROOT / "guns" / "CellRackBlock.java").read_text(encoding="utf-8"):
        err(f"CellRackBlock.COLUMNS differs from tools/guns.py RACK_COLUMNS: expected {{{columns}}}")
    rack_renderer = (CLIENT_JAVA_ROOT / "guns" / "CellRackRenderer.java").read_text(encoding="utf-8")
    rows = ", ".join(f"{top + guns.RACK_CELL_MIDDLE}F" for top in guns.RACK_SHELVES)
    for line in ("COLUMNS = {" + ", ".join(f"{x}F" for x in guns.RACK_COLUMNS) + "};", f"ROWS = {{{rows}}};",
                 f"DEPTH = {guns.RACK_DEPTH}F;"):
        if line not in rack_renderer:
            err(f"CellRackRenderer differs from tools/guns.py RACK_COLUMNS, RACK_SHELVES and RACK_DEPTH: expected {line}")
    events = re.search(r"SOUND_EVENTS = List\.of\(([^)]*)\)", java)
    shared = [name.removeprefix("guns.") for name in guns.sound_events() if name.count(".") == 1]
    if not events or re.findall(r'"([a-z_]+)"', events.group(1)) != shared:
        err(f"JugcraftGuns.SOUND_EVENTS differs from tools/guns.py {shared}")
    sounds = load(ASSETS / "sounds.json") or {}
    for name in guns.sound_events():
        if name not in sounds:
            err(f"sounds.json has no {name}")
    animations = (ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client" / "guns"
                  / "GunAnimations.java").read_text(encoding="utf-8")
    for alias, path in guns.EVENT_SOUNDS.items():
        if alias != "rustle" or guns.EVENT_SOUNDS["gun_rustle"] != path:
            continue
        if 'Map.of("rustle", "gun_rustle")' not in animations:
            err("GunAnimations.SOUND_ALIASES does not play gun_rustle for rustle (tools/guns.py EVENT_SOUNDS)")
    for gun, overrides in guns.EVENT_OVERRIDES.items():
        pairs = ", ".join(f'"{event}", "{sound}"' for event, sound in overrides.items())
        if f'"{gun}", Map.of({pairs})' not in animations:
            err(f"GunAnimations.GUN_SOUND_ALIASES does not play {overrides} for {gun}'s events (tools/guns.py): "
                f'expected "{gun}", Map.of({pairs})')
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    for key in [f"key.{MOD}.reload", f"key.{MOD}.inspect", f"hud.{MOD}.guns.ammo", f"hud.{MOD}.guns.reloading",
                f"message.{MOD}.guns.no_ammo", f"death.attack.{MOD}.bullet", f"death.attack.{MOD}.flame", f"tooltip.{MOD}.guns.fits",
                f"message.{MOD}.guns.no_charge", f"hud.{MOD}.guns.in_cells", f"death.attack.{MOD}.zap", f"tooltip.{MOD}.guns.charge",
                f"tooltip.{MOD}.guns.fitting", f"tooltip.{MOD}.guns.fitted", f"tooltip.{MOD}.guns.stab", f"key.{MOD}.stab",
                f"message.{MOD}.guns.no_grenades", f"tooltip.{MOD}.guns.loaded_grenade", f"tooltip.{MOD}.guns.one_handed"] + [f"tooltip.{MOD}.guns.{i}" for i in guns.items()] + [
                f"tooltip.{MOD}.guns.slot.{slot}" for slot in guns.SLOTS] + [f"tooltip.{MOD}.guns.effect.{e}" for e in guns.EFFECTS]:
        if key not in lang:
            err(f"Missing name {key}")
    for gun in guns.GUNS:
        for path in (f"geckolib/models/item/{gun}.geo.json", f"geckolib/animations/item/{gun}.animation.json",
                     f"textures/item/guns/{gun}.png"):
            if not (ASSETS / path).exists():
                err(f"Missing {path}")
    # Slice 6, the guns in use: the client's looks, the casings and the hooks that show them.
    client_guns = ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client" / "guns"
    looks = (client_guns / "GunLooks.java").read_text(encoding="utf-8")
    for gun in guns.GUNS:
        line = f'LOOKS.put("{gun}", new Look({str(guns.two_handed(gun)).lower()}, {guns.ZOOM[gun]}F));'
        if line not in looks:
            err(f"GunLooks.LOOKS differs from tools/guns.py for {gun}: expected {line}")
    for ammo, size in guns.FLASH_SIZE.items():
        if f'"{ammo}", {size}F' not in looks:
            err(f"GunLooks.FLASH_SIZES differs from tools/guns.py FLASH_SIZE for {ammo} ({size})")
    # Slice 8B: the guns held further out aimed (more than Map.of's ten pairs since slice 9D).
    reliefs = ", ".join(f'Map.entry("{gun}", {build["eye_relief"]}F)' for gun, build in guns.BUILDS.items()
                        if "eye_relief" in build)
    if f"EYE_RELIEF = Map.ofEntries({reliefs});" not in looks:
        err(f"GunLooks.EYE_RELIEF differs from tools/guns.py BUILDS eye_relief: expected Map.ofEntries({reliefs})")
    # Slice 8C: the guns carried lower seen from outside, by their own third-person tilt.
    tints = ", ".join(f'"{ammo}", 0x{tint:06X}' for ammo, tint in guns.FLASH_TINT.items())
    if f"FLASH_TINTS = Map.of({tints});" not in looks:
        err(f"GunLooks.FLASH_TINTS differs from tools/guns.py FLASH_TINT: expected Map.of({tints})")
    tilts = ", ".join(f'"{gun}", {guns.tilt(gun)}F' for gun in guns.GUNS if guns.tilt(gun))
    if f"TILT = Map.of({tilts});" not in looks:
        err(f"GunLooks.TILT differs from tools/guns.py tilt(): expected Map.of({tilts})")
    # Slice 7: each scope's zoom and view.
    quoted = lambda name: f'"{name}"' if name else "null"
    for kind, att in guns.ATTACHMENTS.items():
        if att.get("mount"):
            view = att["view"]
            line = (f'OPTICS.put("{kind}", new Optic({att["zoom"]}F, {quoted(view.get("reticle"))}, '
                    f'{quoted(view.get("vignette"))}, {quoted(view.get("dot"))}));')
            if line not in looks:
                err(f"GunLooks.OPTICS differs from tools/guns.py for {kind}: expected {line}")
    if (f"Codec.STRING.listOf(0, {len(guns.SLOTS)})" not in java
            or f"ByteBufCodecs.list({len(guns.SLOTS)})" not in java):
        err(f"JugcraftGuns.FITTED must hold one attachment a slot: {len(guns.SLOTS)} at most")
    hide = re.search(r"HIDE_FLASH = List\.of\(([^)]*)\)", looks)
    hiding = [kind for kind, att in guns.ATTACHMENTS.items() if att.get("hides_flash")]
    if not hide or re.findall(r'"([a-z_]+)"', hide.group(1)) != hiding:
        err(f"GunLooks.HIDE_FLASH differs from tools/guns.py's hides_flash attachments {hiding}")
    casings = re.search(r"CASING_AMMO = List\.of\(([^)]*)\)", java)
    if not casings or re.findall(r'"([a-z_]+)"', casings.group(1)) != list(guns.CASINGS):
        err(f"JugcraftGuns.CASING_AMMO differs from tools/guns.py CASINGS {list(guns.CASINGS)}")
    for ammo in guns.CASINGS:
        if load(ASSETS / "particles" / f"{ammo}_casing.json") != {"textures": [f"{MOD}:{ammo}_casing"]}:
            err(f"particles/{ammo}_casing.json does not draw textures/particle/{ammo}_casing.png")
    # A prop on a bone of its own name shows only while an animation moves it (GunRenderer.PROPS).
    renderer = (client_guns / "GunRenderer.java").read_text(encoding="utf-8")
    shown = re.search(r"PROPS = List\.of\(([^)]*)\)", renderer)
    own_bones = {name for gun in guns.PROPS for name, _, parts, _ in guns.BUILDS[gun]["bones"] if f"@{name}" in parts}
    if not shown or not own_bones <= set(re.findall(r'"([a-z0-9_]+)"', shown.group(1))):
        err(f"GunRenderer.PROPS does not list every prop bone of tools/guns.py PROPS: {sorted(own_bones)}")
    if f'EJECT_CUE = "{guns.EJECT_CUE}";' not in animations:
        err(f"GunAnimations.EJECT_CUE differs from tools/guns.py ({guns.EJECT_CUE})")
    client_mixins = load(ROOT / "src" / "client" / "resources" / f"{MOD}.client.mixins.json") or {}
    if "GunFovMixin" not in client_mixins.get("client", []):
        err(f"{MOD}.client.mixins.json does not list GunFovMixin (no zoom aimed down the sights)")
    if "GunMouseMixin" not in client_mixins.get("client", []):
        err(f"{MOD}.client.mixins.json does not list GunMouseMixin (the mouse would not slow through a scope)")
    mixin_dir = ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "mixin" / "client"
    for mixin, call in (("ArmsRenderStateMixin", "GunPose.extract"), ("ArmsHumanoidModelMixin", "GunPose.apply")):
        if call not in (mixin_dir / f"{mixin}.java").read_text(encoding="utf-8"):
            err(f"{mixin} does not call {call} (a held gun would hang at the player's side, pointing down)")
    for problem in guns.check():
        err(f"guns: {problem}")


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


def check_item_icons():
    """The 16x16 item icon maps, the materials they are coloured in and the item icons' sizes (tools/check_icon_maps.py,
    docs/ITEM_ICONS.md); and the checker's own self-test, so a later edit cannot quietly weaken it."""
    for message in check_icon_maps.errors() + check_icon_maps.self_test():
        err(f"Item icon maps: {message}")


def check_arms_variants():
    """weapons/ArmVariants.java against tools/arms_variants.py (Arms VII, batch 56): the variants in order with their kind,
    line and boon; the styles, the armor sets and the patterns; every number; each style variant's smithing recipe and
    each boss's trophy loot table, and that an armor set's arm has neither a recipe nor any loot table yet (how a set is
    won is the owner's to decide); the boons' and lines' tooltips, a boss's line naming its boss and a set's its set;
    that every boon is bounded; that no variant deals as much a second as a netherite sword, whatever its boon adds;
    and the armor sets' shields (SET_SHIELDS), each of a set and blocking as a kit shield, with no recipe or drop yet."""
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
    sets = re.search(r"\bSETS = List\.of\(([^)]*)\)", java)
    if not sets or re.findall(r'"([a-z_]+)"', sets.group(1)) != list(av.SETS):
        err(f"ArmVariants.SETS differs from tools/arms_variants.py {list(av.SETS)}")
    if len(set(av.LINES)) != len(av.STYLES) + len(av.BOSSES) + len(av.SETS):
        err("tools/arms_variants.py: a line is in more than one of STYLES, BOSSES and SETS")
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
            err(f"tools/arms_variants.py: {name}'s line {line} is neither a style, a boss nor an armor set")
        # Each trait has a name, and a description to show with Shift (docs/features/trait-details.md); a boss's or an
        # armor set's line is a name only.
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
            what = f"the {av.SETS[line]['display']} set's arm" if line in av.SETS else f"a trophy of {line}"
            err(f"{name} is {what}: it has no recipe")
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
    # A boss's line names its boss ("Trophy of the Yeti King"), an armor set's its set ("Of the Hades Armor set").
    for line, info in list(av.BOSSES.items()) + list(av.SETS.items()):
        if info["display"] not in lang.get(f"tooltip.{MOD}.arms.line.{line}.trait", info["display"]):
            err(f"lang: the {line} line's name does not name {info['display']}")
    # An armor set's arm is creative only for now: no recipe (above), no boss table of its own, and nothing drops it.
    tables = {path: path.read_text(encoding="utf-8")
              for folder in DATA.glob("*/loot_table") for path in folder.rglob("*.json")}
    for armor_set, info in av.SETS.items():
        if not av.set_arms(armor_set):
            err(f"tools/arms_variants.py: the {info['display']} set has no arm")
        if (DATA / MOD / "loot_table" / "bosses" / f"{armor_set}.json").is_file():
            err(f"loot_table/bosses/{armor_set}.json: the {info['display']} set is no boss, and has no loot table yet")
        for path, text in sorted(tables.items()):
            for name in av.set_arms(armor_set):
                if f'"{MOD}:{name}"' in text:
                    err(f"{path.relative_to(DATA)} drops {name}, the {info['display']} set's arm, which nothing drops "
                        "until the owner settles how the set is won")
    for style, info in av.STYLES.items():
        if not (DATA / MOD / "recipe" / f"{info['pattern']}.json").is_file():
            err(f"recipe/{info['pattern']}.json is missing: the {style} pattern must be craftable")
    # An armor set's shield: as tools/arms_variants.py has it, of an armor set, in a shape with its own model and tooltip,
    # blocking as a kit shield in SET_SHIELD_METAL; creative only, as a set's arm is (no recipe, nothing drops it).
    import arms_kit
    found = re.findall(r'new SetShield\("([a-z_]+)", "([a-z_]+)", "([a-z_]+)", "([a-z_]+)"\)', java)
    expected = [(name, info["set"], info["shape"], info["base"]) for name, info in av.SET_SHIELDS.items()]
    if found != expected:
        err(f"ArmVariants.SET_SHIELDS {found} != tools/arms_variants.py {expected}")
    if f'SET_SHIELD_METAL = "{av.SET_SHIELD_METAL}";' not in java:
        err(f"ArmVariants.SET_SHIELD_METAL differs from tools/arms_variants.py ({av.SET_SHIELD_METAL})")
    for name, info in av.SET_SHIELDS.items():
        if info["set"] not in av.SETS:
            err(f"tools/arms_variants.py: {name} is of {info['set']}, not an armor set of SETS")
        if info["shape"] not in av.SHIELD_SHAPES or info["shape"] not in arms_kit.PLATE:
            err(f"tools/arms_variants.py: {name}'s shape {info['shape']} has no SHIELD_SHAPES entry or no model (arms_kit.PLATE)")
        if (info["base"], av.SET_SHIELD_METAL) not in arms.SHIELDS:
            err(f"tools/arms_variants.py: {name} blocks as a {av.SET_SHIELD_METAL} {info['base']}, which tools/arms.py SHIELDS lacks")
        if not {f"tooltip.{MOD}.arms.{info['shape']}", f"tooltip.{MOD}.arms.{info['shape']}.trait"} <= lang.keys():
            err(f"lang: {name}'s shape {info['shape']} needs tooltip.{MOD}.arms.{info['shape']} and its .trait name")
        if (DATA / MOD / "recipe" / f"{name}.json").is_file():
            err(f"{name} is the {av.SETS[info['set']]['display']} set's shield: it has no recipe")
        for path, text in sorted(tables.items()):
            if f'"{MOD}:{name}"' in text:
                err(f"{path.relative_to(DATA)} drops {name}, the {av.SETS[info['set']]['display']} set's shield, which "
                    "nothing drops until the owner settles how the set is won")


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


def machines_stats(family):
    """A machine kind's energy figures (tools/machines.py STATS)."""
    import machines
    return machines.STATS.get(family, {})


def cheapest_electrolysis(fluid):
    """The least JE any recipe spends per mB of `fluid` it makes, with the most efficiency cards its machine takes (the
    legacy fluid machines take none), or None when no recipe makes it. All the recipe's energy counts against `fluid`,
    so a fuel burning for less than this cannot pay back the electricity that made it, whatever else came out too."""
    import machines
    costs = []
    for machine, recipes in petro.FLUID_RECIPES.items():
        use = machines.STATS.get(machine, {}).get("use_per_tick", 0)
        for recipe in recipes:
            for result in recipe.get("fluid_results", []):
                if result[0] == fluid and use:
                    costs.append(recipe["ticks"] * use / result[1])
    for form, recipes in industrial_forms.FORM_RECIPES.items():
        info = industrial_forms.FORMS[form]
        use = machines.STATS[info["family"]]["use_per_tick"]
        # MachineUpgrades.Effect.use with four efficiency cards, the most that count.
        cheapest = max(1, round(use * 0.8 ** 4)) if info["upgrades"] else use
        for recipe in recipes:
            for result in recipe.get("fluid_results", []):
                if result[0] == fluid:
                    costs.append(recipe["ticks"] * cheapest / result[1])
    return min(costs) if costs else None


def check_industrial_forms():
    """Industrial forms (tools/industrial_forms.py, IndustrialForms.java): Java builds each form exactly as the tools
    describe it, its fuels included; its blockstate covers every facing, light and part; its recipes name a capability
    it offers, fit its tanks and make no fluid from nothing; a generator burns only from one fuel tank, gives its power
    out and never burns a fuel for as much as electrolysis spends making it; and the Separator's construction costs the
    owner's 20 steel plates."""
    java = (JAVA_ROOT / "machine" / "form" / "IndustrialForms.java").read_text(encoding="utf-8")
    fuel_values = {name: int(value.replace("_", "")) for name, value in re.findall(
        r"public static final int (\w+) = ([\d_]+);", (JAVA_ROOT / "chemistry" / "FluidFuels.java").read_text(encoding="utf-8"))}

    def value(text):
        return int(text) if text.isdigit() else fuel_values.get(text.removeprefix("FluidFuels."), -1)
    profiles = {name: int(mb.replace("_", "")) for name, mb in re.findall(
        r"(\w+)\(\d+, ([\d_]+), \d+\)", (JAVA_ROOT / "machine" / "form" / "OperatingProfile.java").read_text(encoding="utf-8"))}
    constants = dict(re.findall(r'public static final String (\w+) = "([^"]+)";', java))
    view = load(ASSETS / "recipe_view.json") or {}
    viewed = {machine["block"] for machine in view.get("fluid_machines", [])}
    for form, info in industrial_forms.FORMS.items():
        match = re.search(r'MachineForm\.builder\(Jugcraft\.id\("' + form + r'"\),\s*MachineKind\.(\w+)\)(.*?)\.build\(\);', java, re.S)
        if not match:
            err(f"IndustrialForms.java does not build the form {form}")
            continue
        family, body = match.group(1), match.group(2)
        found = {
            "family": family.lower(),
            "layers": [re.findall(r'"([^"]*)"', layer) for layer in re.findall(r"\.layer\(([^)]*)\)", body)],
            "profile": (re.findall(r"\.profile\(OperatingProfile\.(\w+)\)", body) or ["ENTRY"])[0],
            "input_tanks": re.findall(r'\.inputTank\("(\w+)"\)', body),
            "output_tanks": re.findall(r'\.outputTank\("(\w+)"\)', body),
            "capabilities": [constants.get(c, c.strip('"')) for c in re.findall(r"\.capability\(([^)]+)\)", body)],
            "upgrades": ".upgrades()" in body,
            "warmup": int((re.findall(r"\.warmup\((\d+)\)", body) or ["0"])[0]),
            "ports": [(name, kind, int(target) if target.lstrip("-").isdigit() else target, int(column), int(row), int(layer), side)
                      for name, kind, target, column, row, layer, side in re.findall(
                          r'\.port\("(\w+)", FormPort\.Kind\.(\w+), ([\w.-]+), (\d+), (\d+), (\d+), FormSide\.(\w+)\)', body)],
            "fuels": [(f"{MOD}:{fluid}", value(per_mb), value(per_tick)) for fluid, per_mb, per_tick in re.findall(
                r'\.fuel\(Jugcraft\.id\("(\w+)"\), ([\w.]+), ([\w.]+)\)', body)],
        }
        for key, built in found.items():
            described = info.get(key, []) if key == "fuels" else info[key]
            if [tuple(item) for item in described] != [tuple(item) for item in built] if key == "fuels" else described != built:
                err(f"{form}: {key} is {built} in IndustrialForms.java but {described} in tools/industrial_forms.py")
        if profiles.get(info["profile"]) != info["tank_mb"]:
            err(f"{form}: the {info['profile']} profile's tanks hold {profiles.get(info['profile'])} mB, not {info['tank_mb']}")
        parts = len(industrial_forms.footprint(info["layers"]))
        state = load(ASSETS / "blockstates" / f"{form}.json") or {}
        expected = {f"facing={facing},lit={lit},part={part}" for facing in ("north", "east", "south", "west")
                    for lit in ("false", "true") for part in range(parts)}
        if set(state.get("variants", {})) != expected:
            err(f"{form}: its blockstate must cover exactly 4 facings x 2 lights x {parts} parts")
        if bool(industrial_forms.FORM_RECIPES.get(form)) != (f"{MOD}:{form}" in viewed):
            err(f"{form}: a JEI category in recipe_view.json exactly when it runs recipes")
        if info.get("fuels"):
            kinds = [port[1] for port in info["ports"]]
            if (len(info["input_tanks"]) != 1 or info["output_tanks"] or info["capabilities"] or info["upgrades"]
                    or industrial_forms.FORM_RECIPES.get(form) or "FLUID_IN" not in kinds or "ENERGY_OUT" not in kinds
                    or "ENERGY_IN" in kinds):
                err(f"{form}: a generator burns from one fuel tank, runs no recipes and only gives power out")
            for fluid, per_mb, per_tick in info["fuels"]:
                if fluid not in {f"{MOD}:{f}" for f in list(petro.FLUIDS) + list(petro.GASES)}:
                    err(f"{form}: burns unknown fluid {fluid}")
                if per_mb <= 0 or per_tick <= 0 or per_tick > machines_stats(info["family"]).get("capacity", 0):
                    err(f"{form}: burns {fluid} for nothing or makes more a tick than it stores")
                cost = cheapest_electrolysis(fluid)
                if cost is not None and per_mb >= cost:
                    err(f"{form}: {fluid} burns for {per_mb} JE/mB, at least the {cost:.1f} JE/mB its cheapest making costs: a power loop")
        tanks = info["input_tanks"] + info["output_tanks"]
        fluids = {f"{MOD}:{f}" for f in list(petro.FLUIDS) + list(petro.GASES)} | {"minecraft:water", "minecraft:lava"}
        for recipe in industrial_forms.FORM_RECIPES.get(form, []):
            label = f"{form} recipe {recipe['name']}"
            if recipe["capability"] not in info["capabilities"]:
                err(f"{label}: needs {recipe['capability']}, which the form does not offer")
            if not (DATA / MOD / "recipe" / petro.FLUID_MACHINES[info["family"]]["recipe_type"] / f"{recipe['name']}.json").is_file():
                err(f"{label}: no recipe file")
            if len(recipe.get("fluids", [])) > len(info["input_tanks"]) or len(recipe.get("fluid_results", [])) > len(info["output_tanks"]):
                err(f"{label}: more fluids than the form has tanks")
            for fluid, mb in recipe.get("fluids", []) + [(r[0], r[1]) for r in recipe.get("fluid_results", [])]:
                if fluid not in fluids:
                    err(f"{label}: unknown fluid {fluid}")
                if mb > info["tank_mb"]:
                    err(f"{label}: {mb} mB of {fluid} is more than a {info['tank_mb']} mB tank")
            targets = [petro.result_tank(i, r) for i, r in enumerate(recipe.get("fluid_results", []))]
            if len(set(targets)) != len(targets) or any(t >= len(info["output_tanks"]) for t in targets):
                err(f"{label}: its results need distinct output tanks the form has")
            if sum(r[1] for r in recipe.get("fluid_results", [])) > sum(mb for _, mb in recipe.get("fluids", [])):
                err(f"{label}: makes more fluid than it takes")
            if recipe["ticks"] <= 0:
                err(f"{label}: takes no time")
        if len(tanks) != len(set(tanks)):
            err(f"{form}: two tanks share a role")
    # The Separator's construction: the owner's selected 20 steel plates, counting the Steel Tanks' own.
    pattern, key = industrial_forms.FORMS["electrolytic_separator"]["recipe"]
    tank = load(DATA / MOD / "recipe" / "steel_tank.json") or {}
    plates_per_tank = sum(row.count(ch) for row in tank.get("pattern", []) for ch, ref in tank.get("key", {}).items()
                          if ref == "#c:plates/steel")
    plates = (sum(row.count(ch) for row in pattern for ch, ref in key.items() if ref == "#c:plates/steel")
              + plates_per_tank * sum(row.count(ch) for row in pattern for ch, ref in key.items() if ref == f"{MOD}:steel_tank"))
    if plates != 20:
        err(f"The Electrolytic Separator costs {plates} steel plates, not the owner's selected 20")


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
    if set(data) != set(ROTORS) | set(industrial_forms.ROTORS):
        err(f"machine_rotor_quads.json has {sorted(data)}, giant_models.ROTORS and industrial_forms.ROTORS "
            f"{sorted(set(ROTORS) | set(industrial_forms.ROTORS))}: run generate_material_data.py")
    opaque = {}
    for name, rotor in data.items():
        form = rotor.get("block") in industrial_forms.FORMS
        if not (form or rotor.get("block") in ENLARGED) or rotor.get("axis") not in ("x", "y", "z"):
            err(f"machine rotor {name}: block {rotor.get('block')} is not a big machine or a form, or its axis is wrong")
        if not form and rotor.get("when", {}).get("compact") != "false":
            err(f"machine rotor {name} must only draw on the big machine (when compact=false)")
        if form and (rotor.get("property") != "lit" or not rotor.get("always")):
            err(f"machine rotor {name}: a form's turning parts turn while it is lit and stand still while idle (always)")
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
    for renderer_file in ("WindTurbineRenderer.java", "FormMachineRenderer.java"):
        renderer = (client / renderer_file).read_text(encoding="utf-8") if (client / renderer_file).is_file() else ""
        if '"machine_rotor_quads.json"' not in rotors_java or "MachineRotors.extract" not in renderer or "MachineRotors.submit" not in renderer:
            err(f"client/MachineRotors must read machine_rotor_quads.json and {renderer_file} must extract and submit it")
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
                           "pick": [int(low), int(high)], "reset": int(reset), "growth": float(growth), "trellis": trellis == "true", "paddy": paddy == "true"}
            for name, block, seed, heights, produce, low, high, reset, growth, trellis, paddy in re.findall(
                r'(\w+)\("([a-z_]+)", "([a-z_]+)", new int\[\] \{([\d, ]+)\}, "([a-z_]+)", (\d+), (\d+), (\d+), ([\d.]+)F, (true|false), (true|false)\)',
                java.get("TallCrop", ""))}
    expected = {name: {"block": info["block"], "seed": info["seed"], "heights": info["heights"], "produce": info["pick"]["item"],
                       "pick": [info["pick"]["min"], info["pick"]["max"]], "reset": info["pick_reset"],
                       "growth": info["growth_time"], "trellis": bool(info.get("trellis")), "paddy": bool(info.get("paddy"))}
                for name, info in ag.TALL_CROPS.items()}
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
    for name, n, sat, compost in re.findall(r'\bcob\("([a-z_]+)", (\d+), ([\d.]+)F, COMPOST_(\w+)\)', main):
        items[name] = ("cob", int(n), float(sat), compost.lower(), None)
    for name, compost in re.findall(r'\bthrown\("([a-z_]+)", COMPOST_(\w+)\)', main):
        items[name] = ("thrown", None, None, compost.lower(), None)
    for name in re.findall(r'\bmilkBottle\("([a-z_]+)"\)', main):
        items[name] = ("milk", None, None, None, None)
    for name in re.findall(r'\bpetFood\("([a-z_]+)", EntityTypes\.', main):
        items[name] = ("pet", None, None, None, None)
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
                else "cob" if info.get("cob") else "milk" if info.get("milk") else "pet" if info.get("pet")
                else "thrown" if info.get("thrown")
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
    for wild in ag.WILD_CROPS:
        feature = DATA / MOD / "worldgen" / "feature" / f"{wild}.json"
        patch = DATA / MOD / "worldgen" / "placed_feature" / f"patch_{wild}.json"
        if not feature.is_file() or not patch.is_file():
            err(f"{wild}: registered wild crop must retain its generated feature and placed patch")
    patches = {name: re.findall(r"ConventionalBiomeTags\.(\w+)", biomes)
               for name, biomes in re.findall(r'wildPatch\("([a-z_]+)", ([^;]*)\);', main)}
    expected = {name: info["biomes"] for name, info in ag.WILD_CROPS.items()}
    expected.update({name: info["biomes"] for name, info in ag.FOUND_WILD.items()})
    expected["chestnut_tree"] = ag.CHESTNUT_TREES["biomes"]
    expected["apple_tree"] = ag.CIDER["tree"]["biomes"]
    expected["mums"] = ag.MUM_PATCH["biomes"]
    expected[ag.WOLFSBANE["block"]] = ag.WOLFSBANE["biomes"]
    expected[rice.WILD_RICE["block"]] = rice.WILD_RICE["biomes"]
    expected.update({orchard.feature(tree): info["biomes"] for tree, info in orchard.TREES.items()})
    expected[f"{spices.CINNAMON['tree']}_tree"] = spices.CINNAMON["biomes"]
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
        bog = plants == ag.CRANBERRY["block"] or any(crop["block"] == plants and crop.get("paddy") for crop in ag.TALL_CROPS.values())
        if plants and bool(info.get("bog_seed")) != bog:
            err(f"{name}: a seed is a bog seed exactly when it plants the cranberry bush or a paddy crop")
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
        # A vine that goes over (tools/garden.py OVERRIPE) has its over-ripe state too.
        gone = info.get("overripe")
        expected = ({f"age={a},{gone['property']}={v},section={s}" for a in range(8) for v in ("false", "true")
                     for s in range(ag.TALL_SECTIONS)} if gone else {f"age={a},section={s}" for a in range(8) for s in range(ag.TALL_SECTIONS)})
        if variants != expected:
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

    # No recipe loop among agriculture items: every conversion leads away from where it started. Unpacking a storage block
    # (tools/agriculture.py UNPACKING: tools/rice.py's and tools/soil.py's) only gives back what packed it, so it is left out.
    edges = {}

    def edge(ref, result):
        edges.setdefault(split(ref.lstrip("#"))[1], set()).add(split(result)[1] if ":" in result else result)
    for recipe in ag.SHAPELESS:
        if recipe.get("id") in ag.UNPACKING:
            continue
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
    for info in kitchen.CUTTING.values():
        for result, _count in info["results"]:
            edge(info["input"], result)

    def reaches(start, target, seen):
        for nxt in edges.get(start, ()):
            if nxt == target or (nxt not in seen and reaches(nxt, target, seen | {nxt})):
                return True
        return False
    for start in edges:
        if reaches(start, start, {start}):
            err(f"Agriculture recipes form a loop through {start}")


# Vanilla's foods that the kitchen cuts: [hunger, saturation modifier] (the cuts must not outweigh them).
VANILLA_FOOD = {"porkchop": [3, 0.3], "cooked_porkchop": [8, 0.8], "beef": [3, 0.3], "cooked_beef": [8, 0.8],
                "chicken": [2, 0.3], "cooked_chicken": [6, 0.6], "mutton": [2, 0.3], "cooked_mutton": [6, 0.8],
                "cod": [2, 0.1], "cooked_cod": [5, 0.6], "salmon": [2, 0.1], "cooked_salmon": [6, 0.8],
                "cake": [14, 0.1],  # a cake is seven bites of 2 / 0.1
                "baked_potato": [5, 0.6], "carrot": [3, 0.6], "bread": [5, 0.6], "honey_bottle": [6, 0.1], "sweet_berries": [2, 0.1],
                "glow_berries": [2, 0.1], "melon_slice": [2, 0.3], "pumpkin_pie": [8, 0.3]}
# Vanilla foods the menu cooks with that the kitchen does not cut: [hunger, saturation modifier].
MENU_VANILLA_FOOD = {"apple": [4, 0.3], "beetroot": [1, 0.6], "potato": [1, 0.3], "rotten_flesh": [4, 0.1], "dried_kelp": [1, 0.3]}
# A grain counts as the bread vanilla bakes from it: three wheat make a loaf of 5 hunger, and rice counts as wheat does.
GRAIN = {"minecraft:wheat": 5 / 3, "jugcraft:rice": 5 / 3}
# A whole that is not food (a pumpkin, an egg) has nothing to outweigh; a cooked cut is held to the cooked whole.
COOKED_WHOLE = {"porkchop": "cooked_porkchop", "beef": "cooked_beef", "chicken": "cooked_chicken", "mutton": "cooked_mutton",
                "cod": "cooked_cod", "salmon": "cooked_salmon"}


def check_feasts():
    """Feasts and food displays (tools/feasts.py): FeastDish, FeastBlock, FoodDisplay and the placed pumpkin pie match it;
    a feast's servings give no more than COOK_BONUS hunger over its ingredients, and a placed pie's slices add up to the
    vanilla pie; each feast has a model for every serving left, a blockstate for every facing, its recipe, its words, and
    loot that gives it back only whole and its leftovers only once eaten; each display has its model, blockstate, recipe,
    words and loot, and the client draws it."""
    java = {path.stem: path.read_text(encoding="utf-8") for path in AGRICULTURE_JAVA.glob("*.java")}
    main = java.get("JugcraftAgriculture", "")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    dishes = re.findall(r'^\t([A-Z_]+)\("([a-z_]+)", (\d+), ([\d.]+)F, (\d+), \d+, \d+, \d+\)', java.get("FeastDish", ""), re.M)
    wanted = [(name, str(info["food"][0]), str(info["food"][1]), str(info["light"])) for name, info in feasts.FEASTS.items()]
    if [(name, food, sat, light) for _, name, food, sat, light in dishes] != wanted:
        err("FeastDish.java's feasts (food, light, in order) differ from FEASTS in tools/feasts.py")
    servings = re.search(r"\bSERVINGS = (\d+);", java.get("FeastBlock", ""))
    if not servings or any(int(servings.group(1)) != info["servings"] for info in feasts.FEASTS.values()):
        err("FeastBlock.SERVINGS differs from a feast's servings in tools/feasts.py")
    for name, info in feasts.FEASTS.items():
        if f'stew("{info["serving"]}", {info["food"][0]}, {info["food"][1]}F)' not in main:
            err(f"JugcraftAgriculture must register {info['serving']} as a bowl food of {info['food']}")
    displays = re.findall(r'^\t([A-Z_]+)\("([a-z_]+)", new float\[\]\[\] \{(.*?)\}, ([\d.]+)F, ([\d.]+)F, \d+, \d+\)',
                          java.get("FoodDisplay", ""), re.M)
    wanted = [(name, [[float(x), float(z)] for x, z in info["layout"]], info["height"], info["scale"]) for name, info in feasts.DISPLAYS.items()]
    found = [(name, [[float(v) for v in re.findall(r"[\d.]+", place)] for place in re.findall(r"\{([^{}]*)\}", layout)], float(height),
              float(scale)) for _, name, layout, height, scale in displays]
    if found != wanted:
        err("FoodDisplay.java's displays (layout, height, scale, in order) differ from DISPLAYS in tools/feasts.py")
    if 'Jugcraft.id("food_display")' not in main:
        err("JugcraftAgriculture must register the food displays' block entity as jugcraft:food_display")
    renderer = (ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client" / "ShowcaseRenderer.java")
    if "FoodDisplayBlock" not in (renderer.read_text(encoding="utf-8") if renderer.exists() else ""):
        err("ShowcaseRenderer must draw what is on the food displays")

    # Balance: what a feast's servings give against what goes into it (a pumpkin counted as the slices a knife cuts).
    foods = {name: info["food"] for name, info in ag.ITEMS.items() if "food" in info}

    def hunger(ref):
        namespace, name = split(ref)
        if namespace == "minecraft" and name in VANILLA_FOOD:
            return VANILLA_FOOD[name][0]
        if namespace == MOD and name in foods:
            return foods[name][0]
        cut = next((info for info in kitchen.CUTTING.values() if info["input"] == ref), None)
        return sum(hunger(f"{MOD}:{part}") * count for part, count in cut["results"]) if cut else 0

    for name, info in feasts.FEASTS.items():
        given = info["servings"] * info["food"][0]
        taken = sum(hunger(item) for item in info["inputs"])
        if given > taken + feasts.COOK_BONUS:
            err(f"{name}: its servings give {given} hunger from {taken} in ingredients (at most {feasts.COOK_BONUS} more)")
    for name, info in feasts.PLACED_PIES.items():
        slices = ag.PIES["slices"]
        if slices * info["food"][0] != info["whole"][0] or info["food"][1] != info["whole"][1] \
                or VANILLA_FOOD.get(split(info["item"])[1]) != info["whole"]:
            err(f"{name}: {slices} slices of {info['food']} must add up to the vanilla pie {info['whole']}")
        call = rf'new PlacedPieBlock\(Items\.{split(info["item"])[1].upper()},\s*"{info["slice"]}", {info["food"][0]}, {info["food"][1]}F'
        if not re.search(call, main) or f'food("{info["slice"]}", {info["food"][0]}, {info["food"][1]}F' not in main:
            err(f"JugcraftAgriculture must register {name} as a PlacedPieBlock of {info['item']} and its slice {info['slice']}")
        if f"block.{MOD}.{name}" not in lang:
            err(f"{name} has no words")

    for name, info in feasts.FEASTS.items():
        for count in range(info["servings"] + 1):
            if not (ASSETS / "models" / "block" / f"{name}_{count}.json").exists():
                err(f"{name} needs its model {name}_{count}.json")
        state = load(ASSETS / "blockstates" / f"{name}.json") or {}
        if len(state.get("variants", {})) != 4 * (info["servings"] + 1):
            err(f"{name}'s blockstate needs a variant for every facing and serving left")
        loot = load(DATA / MOD / "loot_table" / "blocks" / f"{name}.json") or {}
        text = json.dumps(loot)
        if f'"servings": "{info["servings"]}"' not in text or (info["leftovers"] and '"servings": "0"' not in text):
            err(f"{name} must drop itself only whole, and its leftovers once eaten")
        for path in (DATA / MOD / "recipe" / f"{name}.json", ASSETS / "items" / f"{name}.json", ASSETS / "items" / f"{info['serving']}.json"):
            if not path.exists():
                err(f"{name} needs {path.relative_to(ROOT)}")
        if f"block.{MOD}.{name}" not in lang or f"item.{MOD}.{info['serving']}" not in lang:
            err(f"{name} and its serving need their words")
    for name in feasts.DISPLAYS:
        for path in (ASSETS / "models" / "block" / f"{name}.json", ASSETS / "blockstates" / f"{name}.json", ASSETS / "items" / f"{name}.json",
                     DATA / MOD / "recipe" / f"{name}.json", DATA / MOD / "loot_table" / "blocks" / f"{name}.json"):
            if not path.exists():
                err(f"{name} needs {path.relative_to(ROOT)}")
        if f"block.{MOD}.{name}" not in lang:
            err(f"{name} has no words")


def check_menu():
    """The menu (tools/menu.py): MenuDishes, PlacedDishBlock and the pet food match it; no dish gives more than
    COOK_BONUS hunger over its ingredients; every dish set down has its model, a blockstate for each facing, loot that
    gives its food back, and words; the corn foods give their cob back; the Cooking Pot wears the owner's pot."""
    java = {path.stem: path.read_text(encoding="utf-8") for path in AGRICULTURE_JAVA.glob("*.java")}
    main = java.get("JugcraftAgriculture", "")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    placed = re.findall(r'new Dish\("([a-z_]+)", DishShape\.([A-Z]+)\)', java.get("MenuDishes", ""))
    if placed != [(name, model[0].upper()) for name, model in menu.all_placed().items()]:
        err("MenuDishes.java's dishes (and their shapes, in order) differ from tools/menu.py all_placed()")
    shapes = set(re.findall(r"^\t\t([A-Z]+)\(Block\.box\(", java.get("PlacedDishBlock", ""), re.M))
    if shapes != {model[0].upper() for model in menu.all_placed().values()}:
        err(f"PlacedDishBlock.DishShape {sorted(shapes)} must have exactly the templates tools/menu.py uses")
    pets = {name: {"animal": animal.lower(), "heal": int(heal), "effects": [[e, int(t)] for e, t in re.findall(r"MobEffects\.(\w+), (\d+)", treats)],
                   "returns": "minecraft:bowl" if bowl == "true" else None}
            for name, animal, heal, bowl, treats in re.findall(
                r'\bpetFood\("([a-z_]+)", EntityTypes\.(\w+), (\d+), (true|false), List\.of\((.*?)\)\);', main, re.S)}
    if pets != menu.PETS:
        err(f"JugcraftAgriculture pet food {pets} differs from tools/menu.py PETS")
    if sorted(name for name, info in ag.ITEMS.items() if info.get("cob")) != sorted(menu.COB_FOODS) or menu.COB not in ag.ITEMS \
            or f'usingConvertsTo(item("{menu.COB}"))' not in main:
        err("The corn on the cob foods (tools/menu.py COB_FOODS) must each give the corncob back")
    if "PlacedDishBlock.setDown" not in main or "PetFoodItem.feed" not in main:
        err("JugcraftAgriculture must set dishes down and feed pets through the use events")

    # Balance: what a dish gives against its ingredients, each counted as the most it would give: as itself, as what a
    # furnace makes of it (raw meat, an egg, corn, sugar), as what a board cuts it into (a pumpkin as its slices), as the
    # bread it would bake (a grain, GRAIN), or, for an ingredient that is not food (a dough, a batter), as what went into it.
    foods = {name: info["food"][0] for name, info in ag.ITEMS.items() if "food" in info}
    made = {}
    for recipe in ag.SHAPELESS:
        made.setdefault(f"{MOD}:{split(recipe['result'])[1]}", ([*recipe["inputs"]], recipe.get("count", 1)))
    for result, info in ag.POT_RECIPES.items():
        made.setdefault(f"{MOD}:{result}", ([ref for ref, n in info["inputs"].items() for _ in range(n)], info.get("count", 1)))

    def full(ref):
        return ref if ":" in ref else f"{MOD}:{ref}"

    def value(ref, seen=()):
        ref = full(ref)
        namespace, name = split(ref)
        own = (VANILLA_FOOD.get(name, MENU_VANILLA_FOOD.get(name, [0]))[0] if namespace == "minecraft" else foods.get(name, 0))
        best = max(own, GRAIN.get(ref, 0))
        if namespace == "minecraft" and name in COOKED_WHOLE:
            best = max(best, VANILLA_FOOD[COOKED_WHOLE[name]][0])
        for result, info in ag.COOKING.items():
            if full(info["input"]) == ref:
                best = max(best, value(result, seen + (ref,)))
        cut = next((info for info in kitchen.CUTTING.values() if full(info["input"]) == ref), None)
        if cut and ref not in seen:
            best = max(best, sum(value(part, seen + (ref,)) * count for part, count in cut["results"]))
        if not own and ref not in GRAIN and ref in made and ref not in seen:
            inputs, count = made[ref]
            best = max(best, sum(value(i, seen + (ref,)) for i in inputs) / count)
        return best

    dishes = {**menu.DISHES, **rice.DISHES, **orchard.DISHES, **milkshakes.DISHES}
    # The vegetables', herbs' and spices' dishes (slice 7b): whatever their tables cook, craft or roast into food.
    garden = (vegetables, herbs, spices)
    garden_items = {name: info for module in garden for name, info in module.ITEMS.items()}
    pot = {**menu.POT_RECIPES, **rice.POT_RECIPES, **{name: info for module in garden for name, info in module.POT_RECIPES.items()}}
    recipes = [(name, [ref for ref, n in info["inputs"].items() for _ in range(n)], info.get("count", 1)) for name, info in pot.items()]
    recipes += [(name, inputs, count) for name, (inputs, count) in menu.SHAPELESS.items()]
    recipes += [(recipe["result"], recipe["inputs"], recipe["count"]) for recipe in rice.SHAPELESS + orchard.SHAPELESS + milkshakes.SHAPELESS
                if recipe["result"] in dishes]
    recipes += [(recipe["result"], recipe["inputs"], recipe["count"]) for module in garden for recipe in module.SHAPELESS
                if "food" in garden_items.get(recipe["result"], {})]
    recipes += [(name, [full(info["input"])], 1) for name, info in menu.COOKING.items()]
    # A roasted vegetable against the raw one (value() would count the raw one as what it roasts into).
    for module in garden:
        for name, info in getattr(module, "COOKING", {}).items():
            raw = foods.get(split(full(info["input"]))[1], 0)
            if "food" in garden_items.get(name, {}) and foods.get(name, 0) > raw + menu.COOK_BONUS:
                err(f"{name}: gives {foods[name]} hunger, roasted from {info['input']} worth {raw} (at most {menu.COOK_BONUS} more)")
    recipes += [(result, [info["input"]], count) for info in kitchen.CUTTING.values() for result, count in info["results"] if result in dishes]
    for name, inputs, count in recipes:
        given = foods.get(name, 0) * count
        taken = sum(value(ref) for ref in inputs)
        if given > taken + menu.COOK_BONUS:
            err(f"{name}: {count} give {given} hunger from {taken:g} in ingredients (at most {menu.COOK_BONUS} more)")
    for name, info in dishes.items():
        if "food" in info and not any(name == recipe[0] for recipe in recipes):
            err(f"{name} has no recipe in tools/menu.py, tools/rice.py, tools/orchard.py or tools/milkshakes.py")

    for name in menu.all_placed():
        model = load(ASSETS / "models" / "block" / f"{name}.json") or {}
        if not model.get("elements") or model.get("textures", {}).get("dish") != f"{MOD}:block/menu/{name}":
            err(f"{name} needs its model, wearing block/menu/{name}")
        state = load(ASSETS / "blockstates" / f"{name}.json") or {}
        if set(state.get("variants", {})) != {f"facing={f}" for f in ("north", "south", "east", "west")}:
            err(f"{name}'s blockstate needs a variant for every facing")
        loot = load(DATA / MOD / "loot_table" / "blocks" / f"{name}.json") or {}
        if f'"name": "{MOD}:{name}"' not in json.dumps(loot):
            err(f"{name} must drop its food when broken")
        if f"block.{MOD}.{name}" not in lang or f"item.{MOD}.{name}" not in lang:
            err(f"{name} needs its words, set down and in hand")
        if name not in ag.ITEMS:
            err(f"{name} is set down but is no food in tools/agriculture.py ITEMS")
    pot = load(ASSETS / "models" / "block" / "cooking_pot.json") or {}
    if any(pot.get("textures", {}).get(key) != f"{MOD}:block/cooking_pot_{key}" for key in ("side", "top", "bottom", "handle", "parts")):
        err("The Cooking Pot must wear the owner's pot (tools/menu.py COOKING_POT_TEXTURES)")


def check_rice():
    """Rice and wet farming (tools/rice.py): the rice plant is a paddy crop; wild rice, the storage blocks, the tatami, its
    mats and the roll medley are registered as tools/rice.py says, each with its blockstate, item, loot and words; the
    storage blocks give back exactly what packed them; the medley serves exactly the rolls it is made of; the board's rice
    cuts are the kitchen's."""
    java = {path.stem: path.read_text(encoding="utf-8") for path in AGRICULTURE_JAVA.glob("*.java")}
    main = java.get("JugcraftAgriculture", "")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    if ag.TALL_CROPS.get("rice") is not rice.CROP or not rice.CROP.get("paddy"):
        err("tools/agriculture.py TALL_CROPS must hold the rice plant (tools/rice.py CROP) as a paddy crop")
    paddy = java.get("PaddyCropBlock", "")
    if "crop.paddy ? new PaddyCropBlock(props, crop)" not in main or "extends TallCropBlock" not in paddy \
            or "CropGrowth.paddySpeed" not in paddy:
        err("JugcraftAgriculture must register a paddy crop as a PaddyCropBlock, a tall crop that grows by CropGrowth.paddySpeed")
    for name, info in rice.CUTTING.items():
        if kitchen.CUTTING.get(name) != info:
            err(f"tools/kitchen.py CUTTING[{name}] must be tools/rice.py's cut {info}")

    # Registrations: each block by its class, each with an item.
    blocks = {rice.WILD_RICE["block"]: "WildRiceBlock::new", "rice_bag": "RiceBagBlock::new", rice.TATAMI["block"]: "TatamiBlock::new",
              rice.MEDLEY["block"]: "RollMedleyBlock::new"}
    blocks.update({name: "FullTatamiMatBlock::new" if info["length"] == 2 else "TatamiMatBlock::new"
                   for name, info in rice.TATAMI_MATS.items()})
    for name, constructor in blocks.items():
        if f'registerBlock("{name}", {constructor}' not in main:
            err(f"JugcraftAgriculture must register {name} with {constructor}")
    bales = [n for n, i in rice.STORAGE.items() if i["kind"] == "bale"]
    found = re.search(r'for \(String bale : List\.of\(([^)]*)\)\) \{\s*Block block = registerBlock\(bale, HayBlock::new[^;]*;'
                      r'\s*registerItem\(bale, ', main)
    if not found or re.findall(r'"([a-z_]+)"', found.group(1)) != bales:
        err("JugcraftAgriculture must register the bales of tools/rice.py STORAGE as hay bales, each with its item")
    if [n for n, i in rice.STORAGE.items() if i["kind"] == "bag"] != ["rice_bag"]:
        err("tools/rice.py STORAGE has one bag, the rice_bag")
    for name in rice.items():
        if name not in bales and f'registerItem("{name}"' not in main:
            err(f"JugcraftAgriculture must register {name}'s item")
        for path in (ASSETS / "blockstates" / f"{name}.json", ASSETS / "items" / f"{name}.json",
                     DATA / MOD / "loot_table" / "blocks" / f"{name}.json"):
            if not path.exists():
                err(f"{name} needs {path.relative_to(ROOT)}")
        if f"block.{MOD}.{name}" not in lang:
            err(f"{name} has no words")
    for name in ("rice_panicle", "straw"):
        if f'plain("{name}", COMPOST_{rice.ITEMS[name]["compost"].upper()})' not in main:
            err(f"JugcraftAgriculture must register {name} as a plain item")

    # Storage: nine in, the same nine out.
    for block, info in rice.STORAGE.items():
        pack = next((r for r in rice.SHAPED if r["id"] == block), None)
        unpack = next((r for r in rice.SHAPELESS if r["id"] == info["unpack"]), None)
        item = f"{MOD}:{info['item']}"
        if (not pack or "".join(pack["pattern"]).count("#") != rice.PACK or pack["key"] != {"#": item} or pack["result"] != block
                or not unpack or unpack["inputs"] != [f"{MOD}:{block}"] or unpack["result"] != info["item"] or unpack["count"] != rice.PACK):
            err(f"{block} must pack {rice.PACK} {item} and unpack into the same {rice.PACK}")
    if sorted(rice.UNPACKING) != sorted(info["unpack"] for info in rice.STORAGE.values()):
        err("tools/rice.py UNPACKING must name exactly the storage blocks' unpacking recipes")
    for recipe in rice.SHAPELESS + rice.SHAPED:
        if not (DATA / MOD / "recipe" / f"{recipe['id']}.json").exists():
            err(f"recipe/{recipe['id']}.json is missing")

    # The tatami pairs, the full mat has its two halves and drops from its foot.
    sides = ("north", "south", "east", "west")
    state = load(ASSETS / "blockstates" / f"{rice.TATAMI['block']}.json") or {}
    if set(state.get("variants", {})) != {f"facing={f},paired={p}" for f in sides for p in ("false", "true")}:
        err("The tatami's blockstate needs every facing, alone and paired")
    for name, info in rice.TATAMI_MATS.items():
        state = load(ASSETS / "blockstates" / f"{name}.json") or {}
        wanted = ({f"facing={f},part={p}" for f in sides for p in ("foot", "head")} if info["length"] == 2
                  else {f"facing={f}" for f in sides})
        if set(state.get("variants", {})) != wanted:
            err(f"{name}'s blockstate needs a variant for every facing{' and part' if info['length'] == 2 else ''}")
        loot = json.dumps(load(DATA / MOD / "loot_table" / "blocks" / f"{name}.json") or {})
        if info["length"] == 2 and '"part": "foot"' not in loot:
            err(f"{name} must drop once, from its foot")

    # Wild rice: drops from its lower half only, and a patch places it.
    loot = json.dumps(load(DATA / MOD / "loot_table" / "blocks" / f"{rice.WILD_RICE['block']}.json") or {})
    if '"half": "lower"' not in loot or f'"{MOD}:{rice.WILD_RICE["drops"]["item"]}"' not in loot:
        err("Wild rice must drop its rice once, from its lower half")
    if not (DATA / MOD / "worldgen" / "placed_feature" / f"patch_{rice.WILD_RICE['block']}.json").exists():
        err("Wild rice needs its placed feature patch_wild_rice")

    # The medley: Java's pieces are tools/rice.py's, and its recipe holds exactly them (a kelp roll as its slices) on a platter.
    medley = java.get("RollMedleyBlock", "")
    pieces = re.search(r"PIECES = List\.of\((.*?)\);", medley, re.S)
    most = re.search(r"\bMAX = (\d+);", medley)
    if not pieces or re.findall(r'"([a-z_]+)"', pieces.group(1)) != rice.MEDLEY["pieces"] or not most \
            or int(most.group(1)) != len(rice.MEDLEY["pieces"]):
        err("RollMedleyBlock.PIECES and MAX differ from tools/rice.py MEDLEY")
    recipe = next((r for r in rice.SHAPELESS if r["result"] == rice.MEDLEY["block"]), {"inputs": []})
    served = []
    for ref in recipe["inputs"]:
        cut = next((info for info in rice.CUTTING.values() if info["input"] == ref), None)
        if split(ref)[1] != rice.MEDLEY["platter"]:
            served += [part for part, count in cut["results"] for _ in range(count)] if cut else [split(ref)[1]]
    if sorted(served) != sorted(rice.MEDLEY["pieces"]) or recipe["inputs"].count(f"{MOD}:{rice.MEDLEY['platter']}") != 1:
        err("The rice roll medley must be made of exactly the rolls it serves, on one platter")
    name, count = rice.MEDLEY["block"], len(rice.MEDLEY["pieces"])
    for rolls in range(count + 1):
        if not (ASSETS / "models" / "block" / f"{name}_{rolls}.json").exists():
            err(f"{name} needs its model {name}_{rolls}.json")
    state = load(ASSETS / "blockstates" / f"{name}.json") or {}
    if set(state.get("variants", {})) != {f"facing={f},rolls={r}" for f in sides for r in range(count + 1)}:
        err(f"{name}'s blockstate needs a variant for every facing and roll left")
    loot = json.dumps(load(DATA / MOD / "loot_table" / "blocks" / f"{name}.json") or {})
    if f'"rolls": "{count}"' not in loot or f'"{MOD}:{rice.MEDLEY["platter"]}"' not in loot or '"minecraft:inverted"' not in loot:
        err(f"{name} must drop itself only whole, and its platter otherwise")


def check_soil():
    """Soil, compost and storage (tools/soil.py): Java's numbers and registrations match it; Rich Soil counts as dirt and
    its farmland as farmland; each crate and bag packs and unpacks nine; each block has its blockstate, item (but the
    farmland), loot and words; the farmland drops Rich Soil."""
    java = {path.stem: path.read_text(encoding="utf-8") for path in AGRICULTURE_JAVA.glob("*.java")}
    main = java.get("JugcraftAgriculture", "")
    lang = load(ASSETS / "lang" / "en_us.json") or {}

    def number(source, name):
        match = re.search(rf"\b{name} = (\d+);", java.get(source, ""))
        return int(match.group(1)) if match else None

    expected = {("RichSoilBlock", "BOOST"): soil.BOOST, ("RichFarmlandBlock", "WATER_REACH"): soil.WATER_REACH,
                ("RichFarmlandBlock", "MAX_MOISTURE"): 7, ("OrganicCompostBlock", "TURN_CHANCE"): soil.COMPOST["turn_chance"],
                ("OrganicCompostBlock", "LAST_STAGE"): soil.COMPOST["stages"] - 1, ("BasketBlockEntity", "SLOTS"): soil.BASKET_SLOTS,
                ("BasketBlockEntity", "PICKUP_TICKS"): soil.PICKUP_TICKS}
    for (source, name), value in expected.items():
        if number(source, name) != value:
            err(f"{source}.{name} must be {value} (tools/soil.py)")
    if len(soil.COMPOST["textures"]) != soil.COMPOST["stages"]:
        err("tools/soil.py COMPOST needs a texture for each stage")

    constructors = {soil.RICH_SOIL["block"]: "RichSoilBlock::new", soil.RICH_FARMLAND["block"]: "RichFarmlandBlock::new",
                    soil.COMPOST["block"]: "OrganicCompostBlock::new"}
    constructors.update({name: "RiceBagBlock::new" for name in soil.SACKS})
    for name, constructor in constructors.items():
        if f'registerBlock("{name}", {constructor}' not in main:
            err(f"JugcraftAgriculture must register {name} with {constructor}")
    crates = re.search(r'for \(String crate : List\.of\(([^)]*)\)\) \{\s*Block block = registerBlock\(crate, Block::new', main)
    if not crates or re.findall(r'"([a-z_]+)"', crates.group(1)) != list(soil.CRATES):
        err("JugcraftAgriculture must register tools/soil.py CRATES, in order, as plain blocks")
    baskets = re.search(r'for \(String name : List\.of\(([^)]*)\)\) \{\s*Block basket = registerBlock\(name, BasketBlock::new', main)
    if not baskets or re.findall(r'"([a-z_]+)"', baskets.group(1)) != list(soil.BASKETS) or 'Jugcraft.id("basket")' not in main:
        err("JugcraftAgriculture must register tools/soil.py BASKETS as BasketBlocks, with the jugcraft:basket block entity")
    if f'registerBlock("{soil.RICH_FARMLAND["block"]}"' in main and f'registerItem("{soil.RICH_FARMLAND["block"]}"' in main:
        err("Rich Soil Farmland has no item: a hoe makes it")

    # Packing: nine in, the same nine out.
    for block, info in list(soil.CRATES.items()) + list(soil.SACKS.items()):
        pack = next((r for r in soil.SHAPED if r["id"] == block), None)
        unpack = next((r for r in soil.SHAPELESS if r["id"] == info["unpack"]), None)
        if (not pack or "".join(pack["pattern"]).count("#") != soil.PACK or pack["key"] != {"#": info["item"]}
                or not unpack or unpack["inputs"] != [f"{MOD}:{block}"] or unpack["result"] != info["item"] or unpack["count"] != soil.PACK):
            err(f"{block} must pack {soil.PACK} {info['item']} and unpack into the same {soil.PACK}")
    if sorted(soil.UNPACKING) != sorted(info["unpack"] for info in list(soil.CRATES.values()) + list(soil.SACKS.values())):
        err("tools/soil.py UNPACKING must name exactly the crates' and bags' unpacking recipes")
    for recipe in soil.SHAPELESS + soil.SHAPED:
        if not (DATA / MOD / "recipe" / f"{recipe['id']}.json").exists():
            err(f"recipe/{recipe['id']}.json is missing")

    # Tags: rich soil is dirt, its farmland takes crops; tools reach them.
    def tagged(path, entry):
        return rid_of(entry) in (load(DATA / "minecraft" / "tags" / "block" / f"{path}.json") or {}).get("values", [])
    if not tagged("dirt", soil.RICH_SOIL["block"]):
        err("Rich Soil must be in minecraft:dirt")
    for tag in ("supports_crops", "grows_crops"):
        if not tagged(tag, soil.RICH_FARMLAND["block"]):
            err(f"Rich Soil Farmland must be in minecraft:{tag}")
    for name in (soil.RICH_SOIL["block"], soil.RICH_FARMLAND["block"], soil.COMPOST["block"]):
        if not tagged("mineable/shovel", name):
            err(f"{name} must be mineable with a shovel")
    for name in (*soil.CRATES, *soil.BASKETS):
        if not tagged("mineable/axe", name):
            err(f"{name} must be mineable with an axe")

    for name in soil.blocks():
        for path in (ASSETS / "blockstates" / f"{name}.json", DATA / MOD / "loot_table" / "blocks" / f"{name}.json"):
            if not path.exists():
                err(f"{name} needs {path.relative_to(ROOT)}")
        if name in soil.items() and not (ASSETS / "items" / f"{name}.json").exists():
            err(f"{name} needs its item model")
        if f"block.{MOD}.{name}" not in lang:
            err(f"{name} has no words")
    if f"container.{MOD}.basket" not in lang:
        err("The basket's screen has no title")
    state = load(ASSETS / "blockstates" / f"{soil.RICH_FARMLAND['block']}.json") or {}
    if set(state.get("variants", {})) != {f"moisture={m}" for m in range(8)}:
        err("Rich Soil Farmland's blockstate needs every moisture")
    state = load(ASSETS / "blockstates" / f"{soil.COMPOST['block']}.json") or {}
    if set(state.get("variants", {})) != {f"composting={s}" for s in range(soil.COMPOST["stages"])}:
        err("Organic Compost's blockstate needs every stage")
    loot = json.dumps(load(DATA / MOD / "loot_table" / "blocks" / f"{soil.RICH_FARMLAND['block']}.json") or {})
    if f'"{MOD}:{soil.RICH_SOIL["block"]}"' not in loot:
        err("Rich Soil Farmland must drop Rich Soil")


def check_vegetables_herbs_spices():
    """Garden crops, herbs and spices, part b (tools/vegetables.py, herbs.py, spices.py): the planter box, herb bundles,
    potted herbs, cinnamon tree, spice rack and spice grinding match the Java; their blocks have models, blockstates, loot,
    tags and words; Ratatouille is cooked from tools/vegetables.py's ingredients."""
    java = {path.stem: path.read_text(encoding="utf-8") for path in AGRICULTURE_JAVA.glob("*.java")}
    main = java.get("JugcraftAgriculture", "")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    client = (CLIENT_JAVA_ROOT / "JugcraftClient.java").read_text(encoding="utf-8")

    def number(source, name):
        match = re.search(rf"\b{name} = ([\d.]+)F?;", java.get(source, ""))
        return float(match.group(1)) if match else None

    def string(source, name):
        match = re.search(rf'\b{name} = "([a-z_]+)";', java.get(source, ""))
        return match.group(1) if match else None

    def tagged(registry, tag, entry):
        namespace, path = split(tag)
        return f"{MOD}:{entry}" in ((load(DATA / namespace / "tags" / registry / f"{path}.json") or {}).get("values", []))

    # The herbs: Java's list, each a pot, a bundle and words.
    listed = re.search(r"List<String> HERBS = List\.of\(([^)]*)\)", main)
    if not listed or re.findall(r'"([a-z_]+)"', listed.group(1)) != list(herbs.HERBS):
        err("JugcraftAgriculture.HERBS differs from tools/herbs.py HERBS")
    bundle_loot_ok = True
    for herb in herbs.HERBS:
        pot, bundle = herbs.potted(herb), herbs.bundle(herb)
        if (load(ASSETS / "models" / "block" / f"{pot}.json") or {}).get("textures", {}).get("plant") != \
                f"{MOD}:block/{herbs.stage_texture(herb, 3)}":
            err(f"{pot} must show the grown herb")
        loot = json.dumps(load(DATA / MOD / "loot_table" / "blocks" / f"{pot}.json") or {})
        if '"minecraft:flower_pot"' not in loot or f'"{MOD}:{herb}"' not in loot:
            err(f"{pot} must give back its pot and a sprig of {herb}")
        state = load(ASSETS / "blockstates" / f"{bundle}.json") or {}
        if set(state.get("variants", {})) != {"dried=false", "dried=true"}:
            err(f"{bundle}'s blockstate needs a fresh and a dried look")
        loot = load(DATA / MOD / "loot_table" / "blocks" / f"{bundle}.json") or {}
        text = json.dumps(loot)
        if (f'"{MOD}:{herbs.BUNDLE["dried"]}"' not in text or f'"count": {herbs.BUNDLE["sprigs"]}' not in text
                or f'"{MOD}:{bundle}"' not in text):
            bundle_loot_ok = False
        for key in (f"block.{MOD}.{pot}", f"block.{MOD}.{bundle}"):
            if key not in lang:
                err(f"{key} has no words")
        recipe = next((r for r in herbs.SHAPELESS if r["result"] == bundle), None)
        if not recipe or recipe["inputs"].count(f"{MOD}:{herb}") != herbs.BUNDLE["sprigs"]:
            err(f"{bundle} must be tied from {herbs.BUNDLE['sprigs']} sprigs, as many as it dries into")
    if not bundle_loot_ok:
        err(f"A herb bundle must give itself fresh and {herbs.BUNDLE['sprigs']} {herbs.BUNDLE['dried']} dried")
    if number("HerbBundleBlock", "DRY_CHANCE") != herbs.BUNDLE["dry_chance"]:
        err("HerbBundleBlock.DRY_CHANCE differs from tools/herbs.py BUNDLE")

    # The planter box: always moist, taken by crops as farmland.
    planter = herbs.PLANTER["block"]
    if number("PlanterBoxBlock", "MOISTURE_LEVEL") != 7:
        err("PlanterBoxBlock must always be moist (moisture 7), as watered farmland")
    for tag in ("minecraft:supports_crops", "minecraft:grows_crops"):
        if not tagged("block", tag, planter):
            err(f"{planter} must be in {tag}")
    state = load(ASSETS / "blockstates" / f"{planter}.json") or {}
    if set(state.get("variants", {})) != {f"moisture={m}" for m in range(8)}:
        err(f"{planter}'s blockstate needs every moisture")

    # The cinnamon tree.
    cinnamon = spices.CINNAMON
    if (number("CinnamonLogBlock", "BARK_MIN"), number("CinnamonLogBlock", "BARK_MAX")) != tuple(cinnamon["bark"]) \
            or string("CinnamonLogBlock", "BARK") != cinnamon["bark_item"]:
        err("CinnamonLogBlock's bark differs from tools/spices.py CINNAMON")
    if f'new CinnamonLogBlock(props, "{cinnamon["stripped"]}")' not in main:
        err(f"{cinnamon['log']} must strip into {cinnamon['stripped']}")
    feature = load(DATA / MOD / "worldgen" / "feature" / f"{cinnamon['tree']}_tree.json") or {}
    trunk = feature.get("trunk_placer", {})
    if (feature.get("trunk_provider", {}).get("id") != f"{MOD}:{cinnamon['log']}"
            or feature.get("foliage_provider", {}).get("id") != f"{MOD}:{cinnamon['leaves']}"
            or (trunk.get("base_height"), trunk.get("height_rand_a")) != (cinnamon["trunk"], cinnamon["trunk_extra"])):
        err("worldgen/feature/cinnamon_tree.json differs from tools/spices.py CINNAMON")
    for log in (cinnamon["log"], cinnamon["stripped"]):
        if not tagged("block", "minecraft:logs_that_burn", log):
            err(f"{log} must be a log (minecraft:logs_that_burn), or its tree's leaves decay under it")
        recipe = next((r for r in spices.SHAPELESS if r["inputs"] == [f"{MOD}:{log}"]), None)
        if not recipe or recipe["result"] != "minecraft:jungle_planks" or recipe["count"] != cinnamon["planks"]:
            err(f"{log} must saw into {cinnamon['planks']} jungle planks")
    for name in cinnamon["displays"]:
        if f"block.{MOD}.{name}" not in lang:
            err(f"block.{MOD}.{name} has no words")

    # The spice rack.
    rack = spices.RACK
    if number("SpiceRackBlockEntity", "SLOTS") != rack["slots"] or \
            f'TagKey.create(Registries.ITEM, Jugcraft.id("{split(rack["tag"])[1]}"))' not in java.get("SpiceRackBlockEntity", ""):
        err("SpiceRackBlockEntity differs from tools/spices.py RACK")
    spots = re.search(r"SPOTS = \{(.*?)\};", (CLIENT_JAVA_ROOT / "SpiceRackRenderer.java").read_text(encoding="utf-8"), re.S)
    if not spots or len(re.findall(r"\{[^{}]*\}", spots.group(1))) != rack["slots"]:
        err(f"SpiceRackRenderer needs a spot for each of the rack's {rack['slots']} slots")
    if "SPICE_RACK_ENTITY, SpiceRackRenderer::new" not in client:
        err("JugcraftClient must draw the spice rack's spices")
    for key in (f"block.{MOD}.{rack['block']}", f"message.{MOD}.{rack['block']}.full", f"message.{MOD}.{rack['block']}.empty"):
        if key not in lang:
            err(f"{key} has no words")
    spice_tag = (load(DATA / MOD / "tags" / "item" / f"{split(rack['tag'])[1]}.json") or {}).get("values", [])
    expected = sorted(f"{MOD}:{name}" for module in (vegetables, herbs, spices) for name, info in module.ITEMS.items()
                      if rack["tag"] in info.get("tags", []))
    if sorted(spice_tag) != expected:
        err(f"{rack['tag']} {sorted(spice_tag)} differs from the spices tools/herbs.py and spices.py tag {expected}")

    # Paprika, ground with the Concordance's mortar.
    grind = spices.GRIND
    if (string("SpiceGrinding", "INPUT"), string("SpiceGrinding", "RESULT"), number("SpiceGrinding", "COUNT")) != \
            (grind["input"], grind["result"], grind["count"]):
        err("SpiceGrinding differs from tools/spices.py GRIND")
    mortar = (ROOT / "src" / "main" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "concordance" / "MortarItem.java")
    if "SpiceGrinding.grinds(other)" not in (mortar.read_text(encoding="utf-8") if mortar.is_file() else ""):
        err(f"The Mortar and Pestle ({MOD}:{grind['tool']}) must grind {grind['input']} into {grind['result']}")

    # Ratatouille is cooked from what tools/vegetables.py says.
    ratatouille = load(DATA / MOD / "recipe" / "pot_cooking" / "ratatouille.json") or {}
    cooked = {entry["ingredient"]: entry.get("count", 1) for entry in ratatouille.get("ingredients", [])}
    if cooked != vegetables.RATATOUILLE:
        err(f"pot_cooking/ratatouille.json {cooked} differs from tools/vegetables.py RATATOUILLE")


def check_garden():
    """Garden crops in the owner's art (tools/garden.py): the tomato vine that goes over, the Rotten Tomato, the mushroom
    colonies and the wild carrots, potatoes and beetroots match the Java; the vine's over-ripe state and the colonies have
    their models, blockstates, loot and words; the wild plants give their vanilla seed; ornamental corn's ears change colour."""
    java = {path.stem: path.read_text(encoding="utf-8") for path in AGRICULTURE_JAVA.glob("*.java")}
    main = java.get("JugcraftAgriculture", "")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    client = (CLIENT_JAVA_ROOT / "JugcraftClient.java").read_text(encoding="utf-8")

    def number(source, name):
        match = re.search(rf"\b{name} = ([\d.]+)F?;", java.get(source, ""))
        return float(match.group(1)) if match else None

    # The tomato vine that goes over.
    over, tomato = garden.OVERRIPE, ag.TALL_CROPS["tomato"]
    vine = java.get("TomatoVineBlock", "")
    if (number("TomatoVineBlock", "OVERRIPE_CHANCE") != over["chance"] or f'BooleanProperty.create("{over["property"]}")' not in vine
            or f'ROTTEN = "{over["item"]}"' not in vine):
        err("TomatoVineBlock differs from tools/garden.py OVERRIPE")
    if "crop == TallCrop.TOMATO ? new TomatoVineBlock(props, crop)" not in main or tomato.get("overripe") != over:
        err("The tomato must be registered as a TomatoVineBlock, going over as tools/garden.py OVERRIPE says")
    variants = (load(ASSETS / "blockstates" / f"{tomato['block']}.json") or {}).get("variants", {})
    for section in range(ag.TALL_SECTIONS):
        withered = over["textures"][min(section, len(over["textures"]) - 1)]
        if variants.get(f"age=7,{over['property']}=true,section={section}", {}).get("model") != rid_of(f"block/{withered}"):
            err(f"The over-ripe tomato vine's section {section} must show {withered}")
    loot = load(DATA / MOD / "loot_table" / "blocks" / f"{tomato['block']}.json") or {}
    pools = json.dumps(loot.get("pools", []))
    if rid_of(over["item"]) not in pools or rid_of(tomato["pick"]["item"]) not in pools:
        err(f"{tomato['block']} must drop tomatoes when ripe and rotten tomatoes when gone over")

    # The Rotten Tomato: thrown as a snowball, rendered as its item, named.
    rotten, item = garden.ROTTEN_TOMATO, java.get("RottenTomatoItem", "")
    thrown = re.search(r"void thrown\(([^}]*)\}", main)
    if (number("RottenTomatoItem", "SPEED") != rotten["speed"] or "ID = TomatoVineBlock.ROTTEN;" not in item
            or not thrown or f"stacksTo({rotten['stack']})" not in thrown.group(1)):
        err("RottenTomatoItem or JugcraftAgriculture.thrown differs from tools/garden.py ROTTEN_TOMATO")
    if "ROTTEN_TOMATO = entity(RottenTomatoItem.ID" not in main or "JugcraftAgriculture.ROTTEN_TOMATO, ThrownItemRenderer::new" not in client:
        err("The thrown Rotten Tomato must be registered as an entity and rendered as its item")
    for key, text in garden.TEXT.items():
        if lang.get(key) != text:
            err(f"en_us.json {key} must be {text!r} (tools/garden.py TEXT)")

    # The mushroom colonies.
    colony = garden.COLONY
    expected = {"MAX_AGE": colony["stages"] - 1, "MAX_LIGHT": colony["max_light"], "GROWTH": colony["growth"],
                "PICK_MIN": colony["pick"]["min"], "PICK_MAX": colony["pick"]["max"], "PICK_RESET": colony["pick_reset"]}
    for name, value in expected.items():
        if number("MushroomColonyBlock", name) != value:
            err(f"MushroomColonyBlock.{name} must be {value} (tools/garden.py COLONY)")
    registered = re.findall(r'\bcolony\("([a-z_]+)", Blocks\.(\w+), Items\.(\w+)\)', main)
    wanted = [(name, info["mushroom"].split(":")[1].upper(), info["mushroom"].split(":")[1].upper()) for name, info in garden.COLONIES.items()]
    if registered != wanted:
        err(f"JugcraftAgriculture colonies {registered} differ from tools/garden.py COLONIES")
    if "JugcraftAgriculture.colony(stack.getItem())" not in java.get("RichSoilBlock", ""):
        err("A mushroom used on Rich Soil must plant its colony")
    for name, info in garden.COLONIES.items():
        state = (load(ASSETS / "blockstates" / f"{name}.json") or {}).get("variants", {})
        if set(state) != {f"age={a}" for a in range(colony["stages"])}:
            err(f"{name}: blockstate needs each of its {colony['stages']} stages")
        for stage in range(colony["stages"]):
            texture = garden.colony_texture(name, stage)
            model = load(ASSETS / "models" / "block" / f"{texture}.json") or {}
            if model.get("parent") != "minecraft:block/cross" or model.get("textures", {}).get("cross") != rid_of(f"block/{texture}"):
                err(f"{texture}: must be a cross of the owner's stage")
        drops = json.dumps(load(DATA / MOD / "loot_table" / "blocks" / f"{name}.json") or {})
        if drops.count(f'"{info["mushroom"]}"') != 2 or f'"age": "{colony["stages"] - 1}"' not in drops:
            err(f"{name} must give back its mushroom, and grown, {colony['pick']['min']}-{colony['pick']['max']} more")
        if lang.get(f"block.{MOD}.{name}") != info["display"]:
            err(f"{name} has no words")
        if name in ag.all_items():
            err(f"{name} has no item: a mushroom plants it")

    # The wild carrots, potatoes and beetroots give their vanilla crop's planting item.
    for name, info in garden.WILD.items():
        if ag.WILD_CROPS.get(name) != info:
            err(f"tools/agriculture.py WILD_CROPS must hold tools/garden.py WILD {name}")
        drops = json.dumps(load(DATA / MOD / "loot_table" / "blocks" / f"{name}.json") or {})
        if f'"{info["seed"]}"' not in drops:
            err(f"{name} must give {info['seed']}")
    for name, texture in garden.WILD_ART.items():
        if ag.WILD_CROPS[name]["texture"] != texture:
            err(f"{name} must wear the owner's {texture}")

    # Ornamental corn's ears: the owner's ripe corn has the golden tones the recolouring swaps.
    for source in garden.ORNAMENTAL_EARS.values():
        image = Image.open(owner_art._source(f"{owner_art.FOOD}/{source}")).convert("RGBA")
        pixels = (image.getpixel((x, y)) for x in range(image.width) for y in range(image.height))
        if not any(pixel[3] and pixel[:3] in garden.EAR_TONES for pixel in pixels):
            err(f"The owner's {source} has none of tools/garden.py EAR_TONES: ornamental corn's ears would not change colour")


def check_cakes():
    """The cakes (tools/cakes.py): Java's PieFilling holds them after the pies, as CAKES has them (ID, slice food and sponge
    colour, in order); CakeBlock's height and quarters match tools/cakes.py and tools/cake_data.py; the cakes, the Burnt
    Cake and Cake Batter are registered; every cake (and the Burnt Cake) has a model for each slice gone, a blockstate for
    each slice and facing (turned as the model faces north), its textures (the toppings' too where it has them), words and
    loot (only while whole); each topping stands inside one quarter but the jam the quarters share; the raw cakes and slices
    have their words and textures, the raw cakes and the batter their recipes; the drawing they were rebuilt from is kept."""
    java = {path.stem: path.read_text(encoding="utf-8") for path in AGRICULTURE_JAVA.glob("*.java")}
    main = java.get("JugcraftAgriculture", "")
    filling = java.get("PieFilling", "")
    declared = re.findall(r'^\t([A-Z_]+)\("([a-z_]+)", (\d+), ([\d.]+)F, 0x([0-9A-Fa-f]{6})\)[,;]', filling, re.M)
    wanted = [(cake.upper(), cake, str(info["food"][0]), str(info["food"][1]), f"{info['color']:06X}") for cake, info in cakes.CAKES.items()]
    if [(c, i, f, s_, col.upper()) for c, i, f, s_, col in declared] != wanted:
        err(f"PieFilling.java's cakes {declared} differ from tools/cakes.py CAKES (ID, slice food, colour, in order)")
    last_pie = max((filling.find(f'"{ag.pie_name(f)}"') for f in ag.PIES["fillings"]), default=-1)
    if declared and filling.find(f'"{next(iter(cakes.CAKES))}"') < last_pie:
        err("PieFilling.java must list the cakes after the pies: the Hearth Oven saves a filling by its place")
    block = java.get("CakeBlock", "")
    height = re.search(r"\bint HEIGHT = (\d+);", block)
    if not height or int(height.group(1)) != cakes.HEIGHT:
        err("CakeBlock.HEIGHT differs from tools/cakes.py HEIGHT")
    quarters = [tuple(map(int, q)) for q in re.findall(r"\{(\d+), (\d+), (\d+), (\d+)\}", block.split("QUARTERS =", 1)[-1].split(";", 1)[0])]
    if quarters != [(x0, z0, x1, z1) for (x0, z0), (x1, z1) in cake_data.QUARTERS]:
        err("CakeBlock.QUARTERS differs from tools/cake_data.py QUARTERS (the order slices are taken)")
    for call in ("filling.height > 0 ? new CakeBlock(filling, filling.height, props)", 'registerBlock("burnt_cake", props -> new CakeBlock(null, props)',
                 f'plain("{cakes.BATTER}"'):
        if call not in main:
            err(f"JugcraftAgriculture.java must call {call}")
    if 'cake ? "burnt_cake" : "burnt_pie"' not in filling or "pie.burnt()" not in java.get("HearthOvenBlockEntity", ""):
        err("A cake left in the Hearth Oven must burn into a Burnt Cake (PieFilling.burnt, HearthOvenBlockEntity)")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    for cake in cakes.blocks():
        if f"block.{MOD}.{cake}" not in lang:
            err(f"{cake} has no words")
        for bites in range(len(cake_data.QUARTERS)):
            if not (ASSETS / "models" / "block" / f"{cake_data.model_name(cake, bites)}.json").exists():
                err(f"{cake} needs its model {cake_data.model_name(cake, bites)}")
        variants = (load(ASSETS / "blockstates" / f"{cake}.json") or {}).get("variants", {})
        for bites in range(len(cake_data.QUARTERS)):
            for facing, turn in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
                variant = variants.get(f"bites={bites},facing={facing}", {})
                if variant.get("model") != f"{MOD}:block/{cake_data.model_name(cake, bites)}" or variant.get("y", 0) != turn:
                    err(f"{cake}'s blockstate must show {cake_data.model_name(cake, bites)} turned {turn} for facing={facing}")
        if '"bites": "0"' not in json.dumps(load(DATA / MOD / "loot_table" / "blocks" / f"{cake}.json") or {}):
            err(f"{cake} must drop only while whole")
        textures = ["top", "front", "side", "inside"] + (["toppings"] if cake in cakes.BARS or cake in cakes.CANDLES or cake in cakes.JAM else [])
        for texture in textures:
            path = ASSETS / "textures" / "block" / f"{cake}_{texture}.png"
            if not path.exists():
                err(f"{cake} needs its texture {cake}_{texture}")
            elif Image.open(path).size != (16, 16):
                err(f"{cake}_{texture}.png must be 16 x 16")
    for cake in cakes.CAKES:
        for box in cakes.topping_boxes(cake):
            if cake in cakes.JAM and box == cakes.jam_box(cake):
                continue
            lo, hi = cakes.turn(*box)
            quarter = cake_data.quarter_of(lo, hi)
            (x0, z0), (x1, z1) = cake_data.QUARTERS[quarter]
            if not (x0 <= lo[0] and hi[0] <= x1 and z0 <= lo[2] and hi[2] <= z1):
                err(f"{cake}'s topping at {lo}-{hi} crosses a cut: each topping must stand inside one quarter")
        for item in (cakes.raw(cake), cakes.slice_item(cake)):
            if f"item.{MOD}.{item}" not in lang or not (ASSETS / "textures" / "item" / f"{item}.png").exists():
                err(f"The cakes need the words and texture of {item}")
        if not (DATA / MOD / "recipe" / f"{cakes.raw(cake)}.json").exists():
            err(f"{cakes.raw(cake)} needs its recipe")
    if not (DATA / MOD / "recipe" / f"{cakes.BATTER}.json").exists():
        err("Cake Batter needs its recipe")
    if not (ROOT / "art" / "owner-library" / "drawings" / "cakes_and_bakes.png").exists():
        err("The owner's drawing the cakes are rebuilt from (art/owner-library/drawings/cakes_and_bakes.png) is missing")


def check_pies_and_tarts():
    """The owner's pies and tarts (tools/pies_and_tarts.py): Java's PieFilling holds them after the cakes, as BAKES has
    them (ID, slice food, filling colour and height, in order), and they are set down as CakeBlocks of that height; every
    bake has a model for each slice gone, a blockstate for each slice and facing (turned as the model faces north), its
    textures (the toppings' too where it has them), words and loot (only while whole); its toppings stand on its top
    within its crust (a tart's within its rim); the raw bakes and the slices have their words and textures, the raw bakes
    their recipes (Pastry Dough, sugar but in the pork pie, and their own ingredients); the drawing they were rebuilt from
    is kept."""
    java = {path.stem: path.read_text(encoding="utf-8") for path in AGRICULTURE_JAVA.glob("*.java")}
    main = java.get("JugcraftAgriculture", "")
    filling = java.get("PieFilling", "")
    declared = re.findall(r'^\t([A-Z_]+)\("([a-z_]+)", (\d+), ([\d.]+)F, 0x([0-9A-Fa-f]{6}), (\d+)\)[,;]', filling, re.M)
    declared = [(c, i, f, s_, col.upper(), h) for c, i, f, s_, col, h in declared]
    wanted = [(bake.upper(), bake, str(info["food"][0]), str(info["food"][1]), f"{info['color']:06X}", str(pies_and_tarts.height(bake)))
              for bake, info in pies_and_tarts.BAKES.items()]
    if declared != wanted:
        err(f"PieFilling.java's pies and tarts {declared} differ from tools/pies_and_tarts.py BAKES (ID, slice food, colour, height, in order)")
    last_cake = max((filling.find(f'"{c}"') for c in cakes.CAKES), default=-1)
    if filling.find(f'"{next(iter(pies_and_tarts.BAKES))}"') < last_cake:
        err("PieFilling.java must list the pies and tarts after the cakes: the Hearth Oven saves a filling by its place")
    if "public CakeBlock(@Nullable PieFilling filling, int height, Properties properties)" not in java.get("CakeBlock", ""):
        err("CakeBlock must take its height, for the pies and tarts")
    if "filling.height > 0 ? new CakeBlock(filling, filling.height, props)" not in main:
        err("JugcraftAgriculture.java must set the pies and tarts down as CakeBlocks of their height")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    for bake, info in pies_and_tarts.BAKES.items():
        if lang.get(f"block.{MOD}.{bake}") != info["display"]:
            err(f"{bake} has no words, or not \"{info['display']}\"")
        for bites in range(len(pie_tart_data.QUARTERS)):
            if not (ASSETS / "models" / "block" / f"{pie_tart_data.model_name(bake, bites)}.json").exists():
                err(f"{bake} needs its model {pie_tart_data.model_name(bake, bites)}")
        variants = (load(ASSETS / "blockstates" / f"{bake}.json") or {}).get("variants", {})
        for bites in range(len(pie_tart_data.QUARTERS)):
            for facing, turn in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
                variant = variants.get(f"bites={bites},facing={facing}", {})
                if variant.get("model") != f"{MOD}:block/{pie_tart_data.model_name(bake, bites)}" or variant.get("y", 0) != turn:
                    err(f"{bake}'s blockstate must show {pie_tart_data.model_name(bake, bites)} turned {turn} for facing={facing}")
        if '"bites": "0"' not in json.dumps(load(DATA / MOD / "loot_table" / "blocks" / f"{bake}.json") or {}):
            err(f"{bake} must drop only while whole")
        for texture in ["top", "front", "side", "inside"] + (["toppings"] if info.get("toppings") else []):
            path = ASSETS / "textures" / "block" / f"{bake}_{texture}.png"
            if not path.exists():
                err(f"{bake} needs its texture {bake}_{texture}")
            elif Image.open(path).size != (16, 16):
                err(f"{bake}_{texture}.png must be 16 x 16")
        lo_edge, hi_edge = (0, 16) if info["shape"] == "pie" else (pies_and_tarts.INSET, 16 - pies_and_tarts.INSET)
        for lo, hi in pies_and_tarts.topping_boxes(bake):
            if lo[1] != pies_and_tarts.top_of(bake) or not (lo_edge <= lo[0] and hi[0] <= hi_edge and lo_edge <= lo[2] and hi[2] <= hi_edge):
                err(f"{bake}'s topping at {lo}-{hi} must stand on its top, within its crust")
        for item in (pies_and_tarts.raw(bake), pies_and_tarts.slice_item(bake)):
            if f"item.{MOD}.{item}" not in lang or not (ASSETS / "textures" / "item" / f"{item}.png").exists():
                err(f"The pies and tarts need the words and texture of {item}")
        recipe = load(DATA / MOD / "recipe" / f"{pies_and_tarts.raw(bake)}.json") or {}
        inputs = [f"{MOD}:{pies_and_tarts.DOUGH}"] + ([] if info.get("savory") else ["minecraft:sugar"]) + info["with"]
        if sorted(json.dumps(i, sort_keys=True) for i in recipe.get("ingredients", [])) != sorted(json.dumps(i) for i in inputs):
            err(f"{pies_and_tarts.raw(bake)}'s recipe must take {inputs}")
    if not (ROOT / "art" / "owner-library" / "drawings" / "pies_and_tarts.png").exists():
        err("The owner's drawing the pies and tarts are rebuilt from (art/owner-library/drawings/pies_and_tarts.png) is missing")


def check_milkshakes():
    """The owner's milkshakes (tools/milkshakes.py): each a drink of tools/menu.py's kind (Java's drink() registrations
    are checked with the other foods, its set-down dish and balance by check_menu), set down as the glass the page draws:
    its model is tools/milkshake_data.py's (the foot's four bars, the base, the glass and its band, the cream, the fruit
    and the straw leaning back), wearing its one 64 x 64 opaque texture; PlacedDishBlock's MILKSHAKE outline is
    milkshakes.OUTLINE; its item is its 3D glass, shown as milkshake_data.DISPLAY says; its recipe is a Milk Bottle, a
    snowball, a sugar and its flavour; every part fits the block; the page they are read off is kept."""
    java = AGRICULTURE_JAVA.joinpath("PlacedDishBlock.java").read_text(encoding="utf-8") if AGRICULTURE_JAVA.joinpath("PlacedDishBlock.java").exists() else ""
    outline = re.search(r"MILKSHAKE\(Block\.box\(([^)]*)\)\)", java)
    if not outline or [float(v) for v in outline.group(1).split(",")] != [float(v) for v in milkshakes.OUTLINE]:
        err("PlacedDishBlock.DishShape.MILKSHAKE must be the box tools/milkshakes.py OUTLINE gives")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    boxes = [milkshakes.PLATE, milkshakes.BODY, milkshakes.BAND, milkshakes.CAP, milkshakes.FRUIT] + milkshakes.FOOT["bars"]
    for lo, hi in boxes + [(milkshakes.STRAW["from"], milkshakes.STRAW["to"])]:
        if not all(0 <= lo[i] < hi[i] <= 16 for i in range(3)):
            err(f"The milkshake glass's part {lo}-{hi} must fit inside the block")
    x0, y0, z0, x1, y1, z1 = milkshakes.OUTLINE
    if any(lo[0] < x0 or lo[1] < y0 or lo[2] < z0 or hi[0] > x1 or hi[1] > y1 or hi[2] > z1 for lo, hi in boxes):
        err("tools/milkshakes.py OUTLINE must hold every part of the glass")
    for key, (u0, v0, u1, v1) in milkshakes.LAYOUT.items():
        if not (0 <= u0 < u1 <= 16 and 0 <= v0 < v1 <= 16) or any(round(c * milkshakes.PIXELS) != c * milkshakes.PIXELS for c in (u0, v0, u1, v1)):
            err(f"tools/milkshakes.py LAYOUT {key} must lie on whole pixels of the 64 x 64 texture")
    for name, info in milkshakes.SHAKES.items():
        model = load(ASSETS / "models" / "block" / f"{name}.json") or {}
        if model.get("elements") != milkshake_data.elements(name):
            err(f"{name}'s model must be the glass tools/milkshake_data.py builds")
        straw = (model.get("elements") or [{}])[-1].get("rotation", {})
        if (straw.get("axis"), straw.get("angle")) != (milkshakes.STRAW["axis"], milkshakes.STRAW["angle"]) or straw.get("angle") not in (-45, -22.5, 0, 22.5, 45):
            err(f"{name}'s straw must lean as tools/milkshakes.py STRAW says, by a turn the model format allows")
        texture = ASSETS / "textures" / "block" / "menu" / f"{name}.png"
        if not texture.exists():
            err(f"{name} needs its texture block/menu/{name}")
        else:
            image = Image.open(texture).convert("RGBA")
            if image.size != (16 * milkshakes.PIXELS, 16 * milkshakes.PIXELS) or image.getextrema()[3][0] < 255:
                err(f"block/menu/{name}.png must be an opaque {16 * milkshakes.PIXELS} x {16 * milkshakes.PIXELS} texture")
        item = load(ASSETS / "items" / f"{name}.json") or {}
        held = load(ASSETS / "models" / "item" / f"{name}.json") or {}
        if item.get("model", {}).get("model") != f"{MOD}:item/{name}" or held.get("parent") != f"{MOD}:block/{name}" \
                or held.get("display") != milkshake_data.DISPLAY or set(held) != {"parent", "display"}:
            err(f"{name}'s item must be its 3D glass (block/{name}), shown as tools/milkshake_data.py DISPLAY says")
        if lang.get(f"item.{MOD}.{name}") != info["display"] or lang.get(f"block.{MOD}.{name}") != info["display"]:
            err(f"{name} needs its words, as an item and set down")
        recipe = load(DATA / MOD / "recipe" / f"{name}.json") or {}
        inputs = [milkshakes.MILK, "minecraft:snowball", "minecraft:sugar"] + info["with"]
        if sorted(json.dumps(i, sort_keys=True) for i in recipe.get("ingredients", [])) != sorted(json.dumps(i) for i in inputs) \
                or recipe.get("result", {}).get("id") != f"{MOD}:{name}":
            err(f"{name}'s recipe must be {inputs}")
        if menu.all_placed().get(name) != ["milkshake"] or ag.ITEMS.get(name, {}).get("drink") != milkshakes.EFFECT:
            err(f"{name} must be a drink set down as the milkshake glass")
    if not (ROOT / "art" / "owner-library" / "drawings" / "milkshakes.png").exists():
        err("The owner's drawing the milkshakes are read off (art/owner-library/drawings/milkshakes.png) is missing")


def check_orchard():
    """Orchards (tools/orchard.py): Java's OrchardTree, OrchardLeavesBlock and registrations match it; each tree's sapling
    (planted by its seed, which a fruit crafts into), its leaves (a model for each fruit stage), their loot, tags and words;
    each tree as a feature of its trunk (oak, or the banana's own stem) and its own leaves in the shape TREES gives, a wild
    patch where its sapling could stand, and the Jugcraft biomes that pick it; every texture Jugcraft draws for it; the
    banana's stem; the juices' recipes and textures. (The pies, preserves, foods and set-down juices are checked with the
    others: check_pies, check_pantry, the item check, check_menu.)"""
    java = {path.stem: path.read_text(encoding="utf-8") for path in AGRICULTURE_JAVA.glob("*.java")}
    main = java.get("JugcraftAgriculture", "")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    declared = re.findall(r'^\t([A-Z]+)\("([a-z_]+)", "([a-z_]+)", (\d+), (\d+)\)[,;]', java.get("OrchardTree", ""), re.M)
    wanted = [(tree.upper(), tree, info["seed"], str(info["pick"][0]), str(info["pick"][1])) for tree, info in orchard.TREES.items()]
    if declared != wanted:
        err(f"OrchardTree.java's trees {declared} differ from tools/orchard.py TREES (id, seed, pick, in order)")
    chance = re.search(r"\bint FRUIT_CHANCE = (\d+);", java.get("OrchardLeavesBlock", ""))
    if not chance or int(chance.group(1)) != orchard.FRUIT_CHANCE:
        err("OrchardLeavesBlock.FRUIT_CHANCE differs from tools/orchard.py FRUIT_CHANCE")
    for call in ("registerOrchardTrees();", "registerOrchards();", "props -> new OrchardLeavesBlock(tree, props)",
                 "props -> new SaplingBlock(tree.grower, props)", 'Jugcraft.id(id + "_tree")'):
        if call not in main + java.get("OrchardTree", ""):
            err(f"The orchards' Java must call {call}")

    def tagged(registry, path, entry):
        return f"{MOD}:{entry}" in (load(DATA / "minecraft" / "tags" / registry / f"{path}.json") or {}).get("values", [])
    for tree, info in orchard.TREES.items():
        sapling, leaves, seed = orchard.sapling(tree), orchard.leaves(tree), info["seed"]
        if ag.ITEMS.get(seed, {}).get("plants") != sapling or sapling not in ag.planted_blocks():
            err(f"{seed} must plant {sapling}")
        recipe = next((r for r in orchard.SHAPELESS if r["id"] == seed), None)
        if not recipe or recipe["inputs"] != [f"{MOD}:{tree}"] or not (DATA / MOD / "recipe" / f"{seed}.json").exists():
            err(f"A {tree} must craft into its {seed}")
        state = load(ASSETS / "blockstates" / f"{leaves}.json") or {}
        if set(state.get("variants", {})) != {f"fruit={f}" for f in range(len(orchard.LEAF_STAGES))}:
            err(f"{leaves}: blockstate does not cover every fruit stage")
        for stage in orchard.LEAF_STAGES:
            model = load(ASSETS / "models" / "block" / f"{leaves}{stage}.json") or {}
            if model.get("textures", {}).get("all") != f"{MOD}:block/{leaves}{stage}":
                err(f"{leaves}{stage} needs its model, wearing block/{leaves}{stage}")
        for kind, name in (("block", f"{leaves}{stage}") for stage in orchard.LEAF_STAGES):
            if not (ASSETS / "textures" / kind / f"{name}.png").exists():
                err(f"Missing texture {kind}/{name}")
        for kind, name in (("block", sapling), ("item", tree), ("item", seed)):
            if not (ASSETS / "textures" / kind / f"{name}.png").exists():
                err(f"Missing texture {kind}/{name}")
        for path in (ASSETS / "blockstates" / f"{sapling}.json", ASSETS / "models" / "block" / f"{sapling}.json",
                     ASSETS / "items" / f"{leaves}.json", ASSETS / "items" / f"{tree}.json", ASSETS / "items" / f"{seed}.json"):
            if not path.exists():
                err(f"{tree} tree needs {path.relative_to(ROOT)}")
        for key in (f"block.{MOD}.{sapling}", f"block.{MOD}.{leaves}", f"item.{MOD}.{tree}", f"item.{MOD}.{seed}"):
            if key not in lang:
                err(f"Missing words for {key}")
        loot = json.dumps(load(DATA / MOD / "loot_table" / "blocks" / f"{leaves}.json") or {})
        if f'"{MOD}:{seed}"' not in loot or f'"{MOD}:{tree}"' not in loot or '"fruit": "2"' not in loot:
            err(f"{leaves} must drop its seed now and then, and its {tree}s when ripe")
        if f'"{MOD}:{seed}"' not in json.dumps(load(DATA / MOD / "loot_table" / "blocks" / f"{sapling}.json") or {}):
            err(f"{sapling} must give its {seed} back")
        if not (tagged("block", "leaves", leaves) and tagged("item", "leaves", leaves) and tagged("block", "saplings", sapling)):
            err(f"The {tree} tree's leaves and sapling must be in minecraft:leaves and minecraft:saplings")
        # The tree: its trunk under its own leaves, in its shape; wild where its sapling could stand; picked by its biomes.
        feature = load(DATA / MOD / "worldgen" / "feature" / f"{orchard.feature(tree)}.json") or {}
        placer = f"minecraft:{info['foliage'].get('type', 'blob')}_foliage_placer"
        if (feature.get("trunk_provider", {}).get("id") != orchard.log(tree)
                or feature.get("foliage_placer", {}).get("type") != placer
                or feature.get("foliage_provider", {}).get("id") != f"{MOD}:{leaves}"
                or feature.get("trunk_placer", {}).get("base_height") != info["trunk"]["base_height"]
                or feature.get("trunk_placer", {}).get("height_rand_a") != info["trunk"]["height_rand_a"]
                or feature.get("foliage_placer", {}).get("radius") != info["foliage"]["radius"]
                or feature.get("foliage_placer", {}).get("height") != info["foliage"]["height"]):
            err(f"worldgen/feature/{orchard.feature(tree)}.json differs from tools/orchard.py TREES[{tree!r}]")
        patch = load(DATA / MOD / "worldgen" / "placed_feature" / f"patch_{orchard.feature(tree)}.json") or {}
        text = json.dumps(patch)
        if (patch.get("feature") != f"{MOD}:{orchard.feature(tree)}" or f'"chance": {info["rarity"]}' not in text
                or f'"state": "{MOD}:{sapling}"' not in text):
            err(f"patch_{orchard.feature(tree)} must place the {tree} tree, one in {info['rarity']} chunks, where its sapling could stand")
        if bm.PLACED_TREES.get(orchard.checked(tree)) != (f"{MOD}:{orchard.feature(tree)}", f"{MOD}:{sapling}"):
            err(f"tools/biomes.py PLACED_TREES must place {orchard.checked(tree)} where the {tree} sapling could stand")
        for biome, share in info["regions"].items():
            if [f"{MOD}:{orchard.checked(tree)}", share] not in bm.BIOMES[biome]["trees"]["picks"]:
                err(f"tools/biomes.py {biome} must pick {orchard.checked(tree)} ({share})")
    # The banana's stem: a pillar block (registered, with its item) that is a log to its fronds and to an axe, but no fuel.
    stem = orchard.BANANA_STEM["block"]
    if f'registerBlock("{stem}", RotatedPillarBlock::new' not in main:
        err(f"JugcraftAgriculture must register {stem} as a RotatedPillarBlock")
    if any(tree_info.get("log", f"{MOD}:{stem}") != f"{MOD}:{stem}" for tree_info in orchard.TREES.values()):
        err("tools/orchard.py: a tree's own log must be the banana's stem, the only one registered")
    state = load(ASSETS / "blockstates" / f"{stem}.json") or {}
    if set(state.get("variants", {})) != {"axis=x", "axis=y", "axis=z"}:
        err(f"{stem}: blockstate does not cover every axis")
    for name in (stem, f"{stem}_top"):
        if not (ASSETS / "textures" / "block" / f"{name}.png").exists():
            err(f"Missing texture block/{name}")
    if (not (ASSETS / "items" / f"{stem}.json").exists() or f"block.{MOD}.{stem}" not in lang
            or f'"{MOD}:{stem}"' not in json.dumps(load(DATA / MOD / "loot_table" / "blocks" / f"{stem}.json") or {})):
        err(f"{stem} needs its item, words and loot")
    if (not tagged("block", "logs", stem) or not tagged("item", "logs", stem) or not tagged("block", "mineable/axe", stem)
            or tagged("item", "logs_that_burn", stem)):
        err(f"{stem} must be in minecraft:logs and minecraft:mineable/axe, and not in minecraft:logs_that_burn")
    for name in orchard.DISHES:
        for kind, path in (("item", name), ("block", f"menu/{name}")):
            if not (ASSETS / "textures" / kind / f"{path}.png").exists():
                err(f"Missing texture {kind}/{path}")
        if not (DATA / MOD / "recipe" / f"{name}.json").exists():
            err(f"recipe/{name}.json is missing")
    for preserve in orchard.PRESERVES:
        if ag.PANTRY["preserves"].get(preserve) != orchard.PRESERVES[preserve] or preserve not in ag.POT_RECIPES:
            err(f"{preserve} must be a preserve (tools/agriculture.py PANTRY) cooked in the Cooking Pot")
    for filling, info in orchard.PIES.items():
        if ag.PIES["fillings"].get(filling) != info:
            err(f"The {filling} pie must be one of tools/agriculture.py PIES' fillings")


def check_kitchen():
    """The Farmhouse Kitchen: Java's numbers, knives and cabinet woods match tools/kitchen.py; every cutting recipe is
    written and never a gain, raw or cooked; the knife tag holds every knife; the owner's textures are imported unchanged."""
    java = {path.stem: path.read_text(encoding="utf-8") for path in AGRICULTURE_JAVA.glob("*.java")}

    def number(source, name):
        match = re.search(rf"\b{name} = (-?[\d.]+)[FLD]?;", java.get(source, ""))
        return float(match.group(1)) if match else None

    expected = {("KitchenStoveBlock", "LIGHT"): kitchen.STOVE["light"], ("KitchenStoveBlock", "BURN"): kitchen.STOVE["burn"],
                ("KitchenStoveBlockEntity", "SLOTS"): kitchen.STOVE["slots"], ("KitchenStoveBlockEntity", "SPEED"): kitchen.STOVE["speed"],
                ("SkilletBlockEntity", "CAPACITY"): kitchen.SKILLET["capacity"], ("SkilletBlockEntity", "SPEED"): kitchen.SKILLET["speed"],
                ("KitchenCabinetBlockEntity", "SLOTS"): kitchen.CABINET_SLOTS, ("JugcraftAgriculture", "KNIFE_DAMAGE"): kitchen.KNIFE_DAMAGE,
                ("JugcraftAgriculture", "KNIFE_SPEED"): kitchen.KNIFE_SPEED}
    for (source, name), value in expected.items():
        if number(source, name) != value:
            err(f"{source}.{name} is {number(source, name)}, tools/kitchen.py says {value}")
    main = java.get("JugcraftAgriculture", "")
    knives = dict(re.findall(r'\bknife\("([a-z_]+)", (?:ToolMaterial|io\.github\.jimbozoomer\.jugcraft\.gear\.JugcraftGear)\.([A-Z]+)\)', main))
    if knives != {knife: info["tier"] for knife, info in kitchen.KNIVES.items()}:
        err(f"JugcraftAgriculture knives {knives} differ from tools/kitchen.py KNIVES")
    woods = re.search(r"CABINET_WOODS = List\.of\(([^)]*)\)", main)
    if not woods or re.findall(r'"([a-z_]+)"', woods.group(1)) != list(kitchen.CABINET_WOODS):
        err("JugcraftAgriculture.CABINET_WOODS differs from tools/kitchen.py CABINET_WOODS")
    for block in (kitchen.STOVE["block"], kitchen.SKILLET["block"], kitchen.BOARD["block"]):
        if f'registerBlock("{block}"' not in main:
            err(f"{block} is not registered in JugcraftAgriculture")
    if f'Jugcraft.id("{kitchen.CUTTING_TYPE.split(":")[1]}")' not in main or f'"{kitchen.KNIFE_TAG.split(":")[1]}"' not in main:
        err("JugcraftAgriculture does not register the cutting recipe type or the knives tag of tools/kitchen.py")

    # The knives tag: every kitchen knife and the Carving Knife.
    tag = load(DATA / MOD / "tags" / "item" / f"{kitchen.KNIFE_TAG.split(':')[1]}.json") or {}
    if set(tag.get("values", [])) != {f"{MOD}:{knife}" for knife in list(kitchen.KNIVES) + ["carving_knife"]}:
        err(f"The item tag {kitchen.KNIFE_TAG} should hold every kitchen knife and the Carving Knife")

    # Cutting recipes: one file each, the right type, tool and results, and never more food than the whole.
    folder = DATA / MOD / "recipe" / "cutting"
    if sorted(path.stem for path in folder.glob("*.json")) != sorted(kitchen.CUTTING):
        err("recipe/cutting/ files differ from CUTTING in tools/kitchen.py")
    foods = {name: info["food"] for name, info in ag.ITEMS.items() if "food" in info}

    def food(ref):
        namespace, name = split(ref) if ":" in ref else (MOD, ref)
        return VANILLA_FOOD.get(name) if namespace == "minecraft" else foods.get(name)

    def worth(value, count=1):
        return count * value[0], count * value[0] * value[1] * 2

    for name, info in kitchen.CUTTING.items():
        recipe = load(folder / f"{name}.json") or {}
        results = [{"id": result if ":" in result else f"{MOD}:{result}", **({"count": count} if count != 1 else {})}
                   for result, count in info["results"]]
        if (recipe.get("type") != kitchen.CUTTING_TYPE or recipe.get("ingredient") != info["input"]
                or recipe.get("tool") != f"#{kitchen.KNIFE_TAG}" or recipe.get("results") != results):
            err(f"cutting/{name}: the file differs from tools/kitchen.py")
        whole = food(info["input"])
        if whole is None:
            continue
        nutrition = saturation = 0.0
        cooked_nutrition = cooked_saturation = 0.0
        for result, count in info["results"]:
            part = food(result)
            if part is None:
                continue
            nutrition, saturation = nutrition + worth(part, count)[0], saturation + worth(part, count)[1]
            cooked = next((out for out, cook in ag.COOKING.items() if cook["input"] == result), None)
            if cooked:
                cooked_nutrition += worth(food(cooked), count)[0]
                cooked_saturation += worth(food(cooked), count)[1]
        if nutrition > worth(whole)[0] or saturation > worth(whole)[1] + 1e-6:
            err(f"cutting/{name}: the parts ({nutrition:g} hunger, {saturation:.2f} saturation) outweigh the whole {info['input']}")
        cooked_whole = COOKED_WHOLE.get(split(info["input"])[1])
        if cooked_nutrition and (not cooked_whole or cooked_nutrition > worth(VANILLA_FOOD[cooked_whole])[0]
                                 or cooked_saturation > worth(VANILLA_FOOD[cooked_whole])[1] + 1e-6):
            err(f"cutting/{name}: the cooked parts outweigh {cooked_whole or 'a cooked whole'}")

    # The owner's textures: byte-for-byte copies of the library (and the two recoloured knives).
    for problem in owner_art.errors():
        err(problem)
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    for key in kitchen.TEXT:
        if key not in lang:
            err(f"Missing lang key {key}")
    used = "\n".join(java.get(source, "") for source in ("KitchenStoveBlock", "SkilletBlock", "CuttingBoardBlock", "KitchenCabinetBlockEntity"))
    for key in kitchen.TEXT:
        if f'"{key}"' not in used:
            err(f"tools/kitchen.py TEXT {key} is not used by the kitchen's Java")


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
    outfit omits its flat equipment asset, has boxes in costumes.json on body parts and motions the client knows,
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
    if (ASSETS / "equipment" / "ghost_sheet.json").exists():
        err("ghost_sheet: omit the flat equipment asset; GhostSheetLayer draws it")
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
        if (ASSETS / "equipment" / f"{name}.json").exists():
            err(f"{name}: omit the flat equipment asset; CostumeLayer draws it and an empty layer map is invalid")
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
    declared = re.findall(r'^\t([A-Z_]+)\("([a-z_]+)", "([a-z_]+)", (\d+), ([\d.]+)F, 0x([0-9A-Fa-f]{6})\)', java.get("PieFilling", ""),
                          re.M)
    wanted = [(f, ag.pie_name(f), str(i["food"][0]), str(i["food"][1]), f"{i['color']:06X}") for f, i in pies["fillings"].items()]
    if [(name, pie, food, sat, color.upper()) for _, name, pie, food, sat, color in declared] != wanted:
        err("PieFilling.java's fillings (pie, slice food, colour, in order) differ from PIES in tools/agriculture.py")
    for call in ('registerBlock("hearth_oven", HearthOvenBlock::new', 'registerItem("pastry_dough"', "for (PieFilling filling : PieFilling.values())",
                 'registerBlock("burnt_pie", props -> new PieBlock(null, props)'):
        if call not in main:
            err(f"JugcraftAgriculture.java must call {call}")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    pies_blocks = [ag.pie_name(f) for f in pies["fillings"]] + [pies["burnt"]]
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
        pie = ag.pie_name(filling)
        for item in (f"raw_{pie}", f"{pie}_slice"):
            if f"item.jugcraft.{item}" not in lang or not (ASSETS / "textures" / "item" / f"{item}.png").exists():
                err(f"Pie baking needs the words and texture of {item}")
        if not (DATA / "jugcraft" / "recipe" / f"raw_{pie}.json").exists():
            err(f"raw_{pie} needs its recipe")
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
                    if isinstance(texture, dict):  # {"sprite": ..., "force_translucent": true}
                        texture = texture.get("sprite", "")
                    if texture.startswith("#"):
                        texture = textures.get(texture[1:], "")
                if isinstance(texture, dict):
                    texture = texture.get("sprite", "")
                if not texture or not translucent(texture):
                    continue
                uv = face.get("uv") or default_uv(side, element["from"], element["to"])
                if min(uv) < 0 or max(uv) > 16:
                    err(f"{path.relative_to(ROOT)}: the {side} face reads {list(uv)} outside its see-through texture {texture}; pin its uv")


def check_lairs():
    """The lairs (tools/lairs.py, tools/hollow_acre.py, tools/spindle_loft.py, tools/glacier_hall.py,
    tools/cinder_kiln.py, docs/features/hollow-acre.md, docs/features/spindle-loft.md, docs/features/glacier-hall.md,
    docs/features/cinder-kiln.md): the Java's numbers are the tables' and the layouts', every lair has its dimension,
    dimension type, biome (no spawns), template and words for coming in, the rituals' blocks, items, recipes and tags are
    there, the Spindle Loft's lace patterns and thread colours, the Glacier Hall's icicle parts and snow heights and the
    Cinder Kiln's slag, troughs and sluices are the tables', every server option has its default, every message the Java
    sends has its words, and the Glacier Hall and the Cinder Kiln are laid out as their records say (check_glacier_hall,
    check_cinder_kiln)."""
    import cinder_kiln as ck
    import glacier_hall as gh
    import hollow_acre as ha
    import lair_data as ld
    import lairs as la
    import spindle_loft as sl
    folder = JAVA_ROOT / "lair"
    lair = (folder / "Lair.java").read_text(encoding="utf-8")
    # Each lair's line in Lair.java, from its layout: size, arrival and its facing, centre, bounds, floor and moon.
    for name, layout, centre, moon in (("hollow_acre", ha, ha.ISLAND["centre"], (ha.MOON, ha.MOON_RADIUS)),
                                       ("spindle_loft", sl, sl.CENTRE, None), ("glacier_hall", gh, gh.CENTRE, None),
                                       ("cinder_kiln", ck, ck.CENTRE, None)):
        ax, ay, az, yaw = layout.ARRIVAL
        sky = "null, 0" if moon is None else "new BlockPos({}, {}, {}), {}".format(*moon[0], moon[1])
        expected = (f'{name.upper()}("{name}", {layout.SIZE[0]}, {layout.SIZE[1]}, {layout.SIZE[2]}, new Vec3({float(ax)}, '
                    f'{float(ay)}, {float(az)}), {yaw}F, {float(centre[0])}, {float(centre[1])}, {layout.BOUNDS}, '
                    f'{layout.FLOOR}, {sky})')
        if expected not in lair:
            err(f"Lair.java's {name.upper()} differs from its layout: expected {expected}")
    for name, value in (("SPACING", la.SPACING), ("BASE_Y", la.BASE_Y)):
        if f"int {name} = {value};" not in lair:
            err(f"Lair.java's {name} is not tools/lairs.py's {value}")
    lairs_java = (folder / "Lairs.java").read_text(encoding="utf-8")
    for name, value in (("CHECK_TICKS", la.CHECK_TICKS), ("EMPTY_SECONDS", la.EMPTY_SECONDS)):
        if f"int {name} = {value};" not in lairs_java:
            err(f"Lairs.java's {name} is not tools/lairs.py's {value}")
    if f"float EDGE_DAMAGE = {la.EDGE_DAMAGE}F;" not in lairs_java:
        err(f"Lairs.java's EDGE_DAMAGE is not tools/lairs.py's {la.EDGE_DAMAGE}")
    for option, (low, high) in la.LIMITS.items():
        if f'number("{option}", {la.OPTIONS[option]}, {low}, {high})' not in lairs_java:
            err(f"Lairs.java does not read {option} with default {la.OPTIONS[option]} kept within {low} to {high}")
    rites = (folder / "LastRites.java").read_text(encoding="utf-8")
    for name, key in (("GRAVE_RANGE", "grave_range"), ("CANDLE_RANGE", "candle_range"), ("CANDLES", "candles"),
                      ("WREATH_RANGE", "wreath_range"), ("KNELL_COOLDOWN", "knell_cooldown")):
        if f"int {name} = {la.RITE[key]};" not in rites:
            err(f"LastRites.java's {name} is not tools/lairs.py RITE {key} ({la.RITE[key]})")
    config = CONFIG.read_text(encoding="utf-8")
    for option, default in la.OPTIONS.items():
        if f'Map.entry("{option}", "{default}")' not in config:
            err(f"JugcraftConfig.TEXT_OPTIONS lacks {option} (default {default})")
    registry = (folder / "JugcraftLairs.java").read_text(encoding="utf-8")
    for block in la.LAIR_BLOCKS:
        if f'fixture("{block}",' not in registry:
            err(f"JugcraftLairs does not register the lair block {block}")
    for name in ("mourning_wreath", "death_knell", "cursed_spindle", "frost_horn", "kiln_seal", la.GATE["entity"]):
        if f'Jugcraft.id("{name}")' not in registry:
            err(f"JugcraftLairs does not register {name}")
    if f"float GLARE_FRICTION = {la.GLARE_FRICTION}F;" not in registry:
        err(f"JugcraftLairs.GLARE_FRICTION is not tools/lairs.py GLARE_FRICTION ({la.GLARE_FRICTION})")
    horn = (folder / "FrostHornRite.java").read_text(encoding="utf-8")
    for name, key in (("COOLDOWN", "cooldown"), ("FROST_TICKS", "frost_ticks")):
        if f"int {name} = {la.HORN[key]};" not in horn:
            err(f"FrostHornRite.{name} is not tools/lairs.py HORN {key} ({la.HORN[key]})")
    parts = re.search(r"enum Part implements StringRepresentable \{\s*([A-Z_, ]+);",
                      (folder / "GiantIcicleBlock.java").read_text(encoding="utf-8"))
    if not parts or tuple(p.strip().lower() for p in parts.group(1).split(",")) != la.ICICLE_PARTS:
        err(f"GiantIcicleBlock.Part is not tools/lairs.py ICICLE_PARTS {la.ICICLE_PARTS}")
    low, high = la.TRAMPLED_HEIGHTS[0], la.TRAMPLED_HEIGHTS[-1]
    if f'IntegerProperty.create("height", {low}, {high})' not in (folder / "TrampledSnowBlock.java").read_text(encoding="utf-8"):
        err(f"TrampledSnowBlock.HEIGHT is not tools/lairs.py TRAMPLED_HEIGHTS {la.TRAMPLED_HEIGHTS}")
    ground = (load(DATA / MOD / "tags" / "block" / "frost_horn_ground.json") or {}).get("values", [])
    if sorted(ground) != sorted(la.HORN_GROUND):
        err(f"#jugcraft:frost_horn_ground is not tools/lairs.py HORN_GROUND: {ground}")
    # The Cinder Kiln's: the Kiln Seal's rest and reach, the slag's burn, the sluices' timings, walks and parts, and the
    # trough's height (its model's, tools/lair_data.py TROUGH_TOP).
    seal = (folder / "KilnSealRite.java").read_text(encoding="utf-8")
    for name, key in (("COOLDOWN", "cooldown"), ("VENT_RANGE", "vent_range")):
        if not java_number(seal, name, la.SEAL[key]):
            err(f"KilnSealRite.{name} is not tools/lairs.py SEAL {key} ({la.SEAL[key]})")
    slag = (folder / "MoltenSlagBlock.java").read_text(encoding="utf-8")
    for name, key in (("DAMAGE", "damage"), ("BURN_SECONDS", "burn_seconds")):
        if not java_number(slag, name, la.SLAG[key]):
            err(f"MoltenSlagBlock.{name} is not tools/lairs.py SLAG {key} ({la.SLAG[key]})")
    sluice = (folder / "SluiceGateBlock.java").read_text(encoding="utf-8")
    for name, key in (("FLOOD_TICKS", "flood_ticks"), ("REFILL_TICKS", "refill_ticks"), ("GATE_BLOCKS", "gate_blocks"),
                      ("TROUGH_BLOCKS", "trough_blocks")):
        if not java_number(sluice, name, la.SLUICE[key]):
            err(f"SluiceGateBlock.{name} is not tools/lairs.py SLUICE {key} ({la.SLUICE[key]})")
    for enum, table in (("Part", la.SLUICE_PARTS), ("Flow", la.SLUICE_FLOWS)):
        found = re.search(rf"enum {enum} implements StringRepresentable \{{\s*([A-Z_, ]+);", sluice)
        if not found or tuple(v.strip().lower() for v in found.group(1).split(",")) != table:
            err(f"SluiceGateBlock.{enum} is not tools/lairs.py SLUICE_{enum.upper()}S {table}")
    if not java_number((folder / "TroughStoneBlock.java").read_text(encoding="utf-8"), "TOP", ld.TROUGH_TOP):
        err(f"TroughStoneBlock.TOP is not the trough's model height (tools/lair_data.py TROUGH_TOP, {ld.TROUGH_TOP})")
    seal_ground = (load(DATA / MOD / "tags" / "block" / "kiln_seal_ground.json") or {}).get("values", [])
    if sorted(seal_ground) != sorted(la.SEAL_GROUND):
        err(f"#jugcraft:kiln_seal_ground is not tools/lairs.py SEAL_GROUND: {seal_ground}")
    lands = (load(DATA / MOD / "tags" / "worldgen" / "biome" / "kiln_seal_lands.json") or {}).get("values", [])
    if sorted(lands) != sorted(la.SEAL_LANDS):
        err(f"#jugcraft:kiln_seal_lands is not tools/lairs.py SEAL_LANDS: {lands}")
    for land in la.SEAL_LANDS:
        if not (DATA / MOD / "worldgen" / "biome" / f"{land.split(':')[1]}.json").is_file():
            err(f"tools/lairs.py SEAL_LANDS names {land}, which is no biome of Jugcraft's")
    spindle = (folder / "SpindleRite.java").read_text(encoding="utf-8")
    if f"int WAKING_TICKS = {la.SPINDLE['waking_ticks']};" not in spindle:
        err(f"SpindleRite.WAKING_TICKS is not tools/lairs.py SPINDLE waking_ticks ({la.SPINDLE['waking_ticks']})")
    if f"int PATTERNS = {la.LACE_PATTERNS};" not in (folder / "DoilyLaceBlock.java").read_text(encoding="utf-8"):
        err(f"DoilyLaceBlock.PATTERNS is not tools/lairs.py LACE_PATTERNS ({la.LACE_PATTERNS})")
    colours = re.search(r"enum ThreadColour[^{]*\{\s*([A-Z_, ]+);", (folder / "ThreadColour.java").read_text(encoding="utf-8"))
    if not colours or [c.strip().lower() for c in colours.group(1).split(",")] != list(la.THREAD_COLOURS):
        err(f"ThreadColour's values are not tools/lairs.py THREAD_COLOURS {la.THREAD_COLOURS}")
    for name in la.FIXTURES:
        values = (load(DATA / MOD / "tags" / "block" / "lair_fixtures.json") or {}).get("values", [])
        if f"{MOD}:{name}" not in values:
            err(f"#jugcraft:lair_fixtures lacks {name}")
    for item in la.RECIPES:
        if not (DATA / MOD / "recipe" / f"{item}.json").is_file():
            err(f"No recipe for {item}")
    for tag, values in (("block/wither_immune", la.LAIR_BLOCKS), ("block/dragon_immune", la.LAIR_BLOCKS)):
        listed = (load(DATA / "minecraft" / "tags" / f"{tag}.json") or {}).get("values", [])
        for block in values:
            if f"{MOD}:{block}" not in listed:
                err(f"#minecraft:{tag.split('/')[1]} lacks the lair block {block}")
    for name in la.LAIRS:
        stem = load(DATA / MOD / "dimension" / f"{name}.json") or {}
        if stem.get("type") != f"{MOD}:{name}" or stem.get("generator", {}).get("settings", {}).get("biome") != f"{MOD}:{name}":
            err(f"data/jugcraft/dimension/{name}.json does not use its own dimension type and biome")
        kind = load(DATA / MOD / "dimension_type" / f"{name}.json") or {}
        rules = kind.get("attributes", {})
        if not kind.get("has_fixed_time") or rules.get("minecraft:gameplay/bed_rule", {}).get("can_sleep") != "never" \
                or rules.get("minecraft:gameplay/respawn_anchor_works") is not False:
            err(f"The {name} dimension type must have a fixed time, no sleeping and no respawn anchors")
        biome = load(DATA / MOD / "worldgen" / "biome" / f"{name}.json") or {}
        spawns = biome.get("attributes", {}).get("minecraft:gameplay/natural_mob_spawns", {}).get("argument", {})
        if spawns.get("spawns_by_category"):
            err(f"The {name} biome must spawn nothing")
        if not (DATA / MOD / "structure" / "lair" / f"{name}.nbt").is_file():
            err(f"No structure template for {name}")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    for path in sorted(folder.glob("*.java")):
        for key in re.findall(r'"((?:message|commands|tooltip|lair)\.jugcraft\.[a-z_.]+)"', path.read_text(encoding="utf-8")):
            if key.endswith("."):
                continue
            if key not in lang:
                err(f"{path.name} sends {key}, which has no words in en_us.json")
    for name in la.LAIRS:
        if f"lair.{MOD}.{name}" not in lang:
            err(f"No name for the lair {name}")
        if f"message.{MOD}.lair.enter.{name}" not in lang:
            err(f"No words for coming into the lair {name} (message.{MOD}.lair.enter.{name})")
    check_glacier_hall(gh)
    check_cinder_kiln(ck)


def check_glacier_hall(gh):
    """The Glacier Hall as tools/glacier_hall.py builds it is the hall its record describes (docs/features/glacier-hall.md):
    the arrival stands on trampled snow with room overhead; the Grey Mist hangs in its arch; the snow ramp falls half a
    block a block from the ledge to the lake, with headroom all the way; the lake is drift snow, glare ice and trampled
    snow, trampled round every column, and nothing a fight needs is missing from it; no icicle hangs lower than its
    clearance; and nothing in it melts (no vanilla ice or snow layers, which the hidden lights would melt)."""
    import math
    blocks = gh.build()
    name = lambda pos: blocks[pos][0] if pos in blocks else None
    solid = lambda pos: name(pos) not in (None, "minecraft:light")
    ax, ay, az, _ = gh.ARRIVAL
    feet = (int(ax), int(ay), int(az))
    if name((feet[0], feet[1] - 1, feet[2])) != "jugcraft:trampled_snow" or solid(feet) \
            or solid((feet[0], feet[1] + 1, feet[2])):
        err(f"The Glacier Hall's arrival at {feet} does not stand on trampled snow with room overhead")
    for pos in gh.exit_mist():
        if name(pos) != "jugcraft:lair_exit":
            err(f"No Grey Mist in the Glacier Hall's arch at {pos}")
    last = None
    for z in range(gh.RAMP["foot"], gh.RAMP["top"] + 1):
        h = gh.ramp_height(z)
        for x in range(gh.RAMP["x"][0], gh.RAMP["x"][1] + 1):
            y = int(h)
            under = (x, y - 1, z) if h == y else (x, y, z)
            if h > gh.LAKE + 1 and name(under) != "jugcraft:trampled_snow":
                err(f"The Glacier Hall's snow ramp has no trampled snow at {under} (row {z}, height {h})")
            if any(solid((x, y + dy + (0 if h == y else 1), z)) for dy in range(2)):
                err(f"The Glacier Hall's snow ramp has no headroom over row {z}, column {x}")
        if last is not None and abs(h - last) > 0.5:
            err(f"The Glacier Hall's snow ramp steps {abs(h - last)} between rows {z - 1} and {z}")
        last = h
    lake = {}
    for x in range(gh.SIZE[0]):
        for z in range(gh.SIZE[2]):
            if gh.lake_radius(x, z) <= gh.LAKE_RADIUS:
                lake[(x, z)] = name((x, gh.LAKE, z))
    allowed = {"jugcraft:drift_snow", "jugcraft:glare_ice", "jugcraft:trampled_snow", "minecraft:packed_ice", "minecraft:blue_ice"}
    stray = sorted({kind for kind in lake.values() if kind not in allowed}, key=str)
    if stray:
        err(f"The Glacier Hall's lake has {stray} in it")
    if list(lake.values()).count("jugcraft:drift_snow") < len(lake) // 2:
        err("Less than half the Glacier Hall's lake is drift snow, which the King bares")
    for sx, sz in gh.COLUMNS:
        ring = gh.COLUMN_RADIUS + gh.COLUMN_FOOT + gh.COLUMN_RING
        for (x, z), kind in lake.items():
            if kind == "jugcraft:drift_snow" and math.hypot(x + 0.5 - sx, z + 0.5 - sz) <= ring:
                err(f"Drift snow at {(x, z)} inside the trampled ring of the column at {(sx, sz)}")
                break
    icicles = [pos for pos, state in blocks.items() if state[0] == "jugcraft:giant_icicle"]
    if not icicles or min(y for _, y, _ in icicles) < gh.ICICLE_CLEARANCE:
        err(f"The Glacier Hall's icicles hang lower than {gh.ICICLE_CLEARANCE}, or there are none")
    melts = {"minecraft:ice", "minecraft:snow", "minecraft:frosted_ice"} & {state[0] for state in blocks.values()}
    if melts:
        err(f"The Glacier Hall has {sorted(melts)}, which its hidden lights would melt")


def check_cinder_kiln(ck):
    """The Cinder Kiln as tools/cinder_kiln.py builds it is the kiln its record describes (docs/features/cinder-kiln.md):
    the arrival stands on the ledge with room overhead, and the Grey Mist hangs in its arch; no two neighbouring blocks of
    the ledge and the stair differ by more than half a block, and a walk from the arrival reaches the bowl's middle, and
    so back, with no step more than half a block and without treading the slag; the bowl is cracked basalt with nothing else in its floor but the troughs, the channel and the
    crucible; the crucible is slag CRUCIBLE["depth"] deep on basalt, rimmed with kiln brick but where the channel runs
    in, and the channel is slag from the slag fall over the forge's lip down to it; the lip has room over it; each
    sluice's gate is whole and faces into the kiln, the walk SluiceGateBlock makes from any of its blocks finds all of it
    and nothing of another's, and the walk from under it finds all of its trough and no other; each shelf is a raised
    step inside the bowl with room over it, more than SPILL blocks from the slag; no magma lies where anyone stands; and
    nothing in it burns."""
    import math
    from collections import deque
    import lairs as la
    blocks = ck.build()
    cavern = ck.cavern()
    name = lambda pos: blocks[pos][0] if pos in blocks else None
    # Open air inside the kiln (outside its shell is the void, where nobody is).
    free = lambda pos: pos in cavern and name(pos) in (None, "minecraft:light", "jugcraft:lair_exit")
    props = lambda pos: dict(blocks[pos][1]) if pos in blocks else {}
    ax, ay, az, _ = ck.ARRIVAL
    feet = (int(ax), int(ay), int(az))
    if name((feet[0], feet[1] - 1, feet[2])) != "minecraft:polished_blackstone" or not free(feet) \
            or not free((feet[0], feet[1] + 1, feet[2])):
        err(f"The Cinder Kiln's arrival at {feet} does not stand on the ledge's blackstone with room overhead")
    for pos in ck.exit_mist():
        if name(pos) != "jugcraft:lair_exit":
            err(f"No Grey Mist in the Cinder Kiln's arch at {pos}")

    # Where anyone can stand, column by column: the surface (in blocks) of a block with two free cells over it.
    tops = {"minecraft:polished_blackstone_slab": 0.5, "jugcraft:trough_stone": 13 / 16, "jugcraft:molten_slag": 14 / 16}
    stands = {}
    for (x, y, z), state in blocks.items():
        if state[0] in ("minecraft:light", "jugcraft:lair_exit") or not free((x, y + 1, z)) or not free((x, y + 2, z)):
            continue
        stands.setdefault((x, z), []).append((y + tops.get(state[0], 1.0), state[0]))
    start = (feet[0], feet[2], float(ay))
    goal = (int(ck.BOWL_CENTRE[0]), int(ck.BOWL_CENTRE[1]), float(ck.BOWL + 1))
    seen, queue = {start}, deque([start])
    while queue:
        x, z, h = queue.popleft()
        for nx, nz in ((x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1)):
            for nh, kind in stands.get((nx, nz), ()):
                node = (nx, nz, nh)
                if abs(nh - h) <= 0.5 + 1e-9 and kind != "jugcraft:molten_slag" and node not in seen:
                    seen.add(node)
                    queue.append(node)
    if goal not in seen:
        err("No walk from the Cinder Kiln's arrival to the bowl's middle without a step of more than half a block "
            "(the ledge and the stair) or treading the slag")
    # And every step of the ledge and the stair, to each neighbour (across a corner too), is at most half a block.
    steps = {(x, z): ck.stair_height(x, z) for x in range(ck.SIZE[0]) for z in range(ck.SIZE[2])
             if ck.stair_height(x, z) is not None}
    steep = [((x, z), (x + dx, z + dz)) for (x, z), h in steps.items() for dx in (-1, 0, 1) for dz in (-1, 0, 1)
             if steps.get((x + dx, z + dz)) is not None and abs(steps[(x + dx, z + dz)] - h) > 0.5]
    if steep:
        a, b = steep[0]
        err(f"The Cinder Kiln's stair steps {abs(steps[a] - steps[b])} blocks between {a} and {b} "
            f"({len(steep) // 2} such steps): at most half a block")

    bowl = {}
    for x in range(ck.SIZE[0]):
        for z in range(ck.SIZE[2]):
            if ck.in_bowl(x, z) and free((x, ck.BOWL + 1, z)):  # not under a shelf or the crucible's rim
                bowl[(x, z)] = name((x, ck.BOWL, z))
    stray = sorted({kind for kind in bowl.values()
                    if kind not in ("jugcraft:cracked_basalt", "jugcraft:trough_stone", "jugcraft:molten_slag")}, key=str)
    if stray:
        err(f"The Cinder Kiln's bowl has {stray} in its floor")
    if list(bowl.values()).count("jugcraft:cracked_basalt") < len(bowl) * 2 // 3:
        err("Less than two thirds of the Cinder Kiln's bowl is cracked basalt")

    depth = ck.CRUCIBLE["depth"]
    slag = set()
    for x in range(ck.SIZE[0]):
        for z in range(ck.SIZE[2]):
            if ck.in_crucible(x, z):
                if any(name((x, y, z)) != "jugcraft:molten_slag" for y in range(ck.BOWL - depth + 1, ck.BOWL + 1)) \
                        or name((x, ck.BOWL - depth, z)) != "minecraft:basalt":
                    err(f"The Cinder Kiln's crucible is not slag {depth} deep on basalt at {(x, z)}")
                slag.add((x, z))
            elif ck.on_rim(x, z) and name((x, ck.BOWL + 1, z)) != "jugcraft:kiln_brick":
                err(f"The Cinder Kiln's crucible has no rim of kiln brick at {(x, z)}")
    l0, l1 = ck.FORGE["lip"]
    for x in range(ck.RUN[0], ck.RUN[1] + 1):
        if not free((x, ck.BOWL + 1, int(ck.CRUCIBLE["centre"][1] - ck.CRUCIBLE["radius"] - 0.5))):
            err(f"The Cinder Kiln's crucible rim closes the channel's mouth at column {x}")
        for z in range(ck.CHANNEL[0], ck.CHANNEL[1] + 1):
            if not ck.in_crucible(x, z):
                if name((x, ck.BOWL, z)) != "jugcraft:molten_slag":
                    err(f"The Cinder Kiln's heat channel has no slag at {(x, z)}")
                slag.add((x, z))
        if any(name((x, y, l1 + 1)) != "jugcraft:molten_slag" for y in range(ck.BOWL + 1, ck.LIP + 1)) \
                or any(name((x, ck.LIP, z)) != "jugcraft:molten_slag" for z in range(l0, l1 + 1)):
            err(f"The slag does not run over the forge's lip and fall into the channel in column {x}")
    x0, x1 = ck.FORGE["x"]
    for x in range(x0, x1 + 1):
        for z in range(l0, l1 + 1):
            if name((x, ck.LIP, z)) not in ("jugcraft:kiln_brick", "jugcraft:molten_slag"):
                err(f"The Cinder Kiln's forge lip has {name((x, ck.LIP, z))} at {(x, z)}")
            if x0 < x < x1 and not all(free((x, y, z)) for y in range(ck.LIP + 1, ck.LIP + 5)):
                err(f"The Cinder Kiln's forge lip has no room over it at {(x, z)}")

    gates = ck.sluice_blocks()
    troughs = ck.trough_cells()

    def walk(seeds, ok, steps, limit):
        found, queue = [], deque(seeds)
        while queue and len(found) < limit:
            at = queue.popleft()
            if at in found or not ok(at):
                continue
            found.append(at)
            queue.extend((at[0] + dx, at[1] + dy, at[2] + dz) for dx, dy, dz in steps)
        return set(found)

    six = [(1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)]
    four = [(1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1)]
    for sluice, s in ck.SLUICES.items():
        own = {pos for pos, (which, _) in gates.items() if which == sluice}
        for pos in own:
            part = gates[pos][1]
            if name(pos) != "jugcraft:sluice_gate" or props(pos).get("part") != part \
                    or props(pos).get("facing") != s["facing"] or props(pos).get("flow") != "ready":
                err(f"The Cinder Kiln's {sluice} sluice has no ready {part} facing {s['facing']} at {pos}")
        for pos in own:
            found = walk([pos], lambda at: name(at) == "jugcraft:sluice_gate", six, la.SLUICE["gate_blocks"])
            if found != own:
                err(f"From {pos}, SluiceGateBlock's walk finds {len(found)} blocks, not the {sluice} sluice's {len(own)} "
                    f"(at most {la.SLUICE['gate_blocks']})")
                break
        trough = {(x, ck.BOWL, z) for (x, z), which in troughs.items() if which == sluice}
        found = walk([(x, y - 1, z) for x, y, z in own], lambda at: name(at) == "jugcraft:trough_stone", four,
                     la.SLUICE["trough_blocks"])
        if found != trough:
            err(f"The walk from under the {sluice} sluice finds {len(found)} trough stones, not its trough's {len(trough)} "
                f"(at most {la.SLUICE['trough_blocks']})")
        for x, y, z in trough:
            if props((x, y, z)).get("flooded") != "false":
                err(f"The {sluice} sluice's trough is not dry at {(x, z)}")
                break
            if (x, y + 1, z) not in own and not (free((x, y + 1, z)) and free((x, y + 2, z))):
                err(f"The {sluice} sluice's trough has no room over it at {(x, z)}")
                break
        wheel = next(pos for pos in own if gates[pos][1] == "wheel")
        front = ck.at(s, s["plane"] + 2 * s["toward"], ck.right_post(s), ck.WHEEL_HEIGHT)
        if not free(front):
            err(f"Nobody can reach the {sluice} sluice's wheel at {wheel}: {front} in front of it is {name(front)}")

    spill = 3.0
    for sx, sz in ck.SHELVES:
        cells = [(x, z) for x in range(int(sx) - 5, int(sx) + 6) for z in range(int(sz) - 5, int(sz) + 6) if ck.shelf(x, z)]
        for x, z in cells:
            if name((x, ck.BOWL + 1, z)) not in ("minecraft:smooth_basalt", "minecraft:polished_basalt") \
                    or not free((x, ck.BOWL + 2, z)) or not free((x, ck.BOWL + 3, z)) or not ck.in_bowl(x, z):
                err(f"The Cinder Kiln's shelf at {(sx, sz)} is not a raised step in the bowl with room over it at {(x, z)}")
                break
        near = min(math.hypot(x - a, z - b) for x, z in cells for a, b in slag)
        if near <= spill:
            err(f"The Cinder Kiln's shelf at {(sx, sz)} is only {near:.1f} blocks from the slag (more than {spill})")

    for (x, y, z), state in blocks.items():
        if state[0] == "minecraft:magma_block" and free((x, y + 1, z)) and free((x, y + 2, z)):
            err(f"Magma lies where someone could stand in the Cinder Kiln, at {(x, y, z)}")
            break
    allowed = {"jugcraft:kiln_brick", "jugcraft:cracked_basalt", "jugcraft:molten_slag", "jugcraft:trough_stone",
               "jugcraft:sluice_gate", "jugcraft:lair_exit", "minecraft:basalt", "minecraft:blackstone",
               "minecraft:magma_block", "minecraft:smooth_basalt", "minecraft:polished_basalt",
               "minecraft:polished_blackstone", "minecraft:polished_blackstone_slab",
               "minecraft:polished_blackstone_bricks", "minecraft:light"}
    burns = sorted({state[0] for state in blocks.values()} - allowed)
    if burns:
        err(f"The Cinder Kiln has {burns}: only its stone, slag, troughs, gates and mist (nothing that burns)")


def java_number(source, name, value):
    """Whether `source` declares `name` = `value` as an int, float (F) or double constant."""
    if isinstance(value, int) and not isinstance(value, bool):
        return re.search(rf"\bint {name} = {value};", source) is not None
    return re.search(rf"\b(?:float|double) {name} = {float(value)}F?;", source) is not None


def check_vesperine():
    """Vesperine, the Last Reaper (tools/vesperine.py, docs/features/vesperine.md): every number the Java uses is the
    table's; her attacks are the table's; where her fight looks for the Hollow Acre's parts (HollowAcre.java) is where
    tools/hollow_acre.py builds them; her settings are read with their defaults and ranges; every entity is
    registered, named and drawn; each GeckoLib body has its model, clips (every clip the Java names) and sheet, its
    box-UV regions inside the sheet without overlapping, its sheet and glowmask TEXTURE_SCALE times the size the model
    declares and cut out (no half-transparent pixel); no bone is animated by two of her controllers; her loot table is
    the table's; her recipes, tags and advancement are there; every message she sends has its words; and nothing of
    hers loads a chunk or leaves its dimension."""
    import vesperine as vs
    import vesperine_models as vm
    folder = JAVA_ROOT / "lair" / "vesperine"
    sources = {path.stem: path.read_text(encoding="utf-8") for path in folder.glob("*.java")}
    if not sources:
        err("lair/vesperine: no Java")
        return
    for cls, table in (("VesperineEntity", vs.VESPERINE), ("ReaperSkullEntity", vs.SKULL), ("GriefBoltEntity", vs.BOLT),
                       ("ThrownScytheEntity", vs.SCYTHE), ("GraveThrallEntity", vs.THRALL),
                       ("HarvestSoulEntity", vs.SOUL), ("ReapingCrescentEntity", vs.CRESCENT)):
        for name, value in table.items():
            if not java_number(sources.get(cls, ""), name, value):
                err(f"{cls}.{name} is not tools/vesperine.py's {value}")
    boon = (JAVA_ROOT / "weapons" / "HarvestBoon.java").read_text(encoding="utf-8")
    for name, value in vs.HARVEST.items():
        if not java_number(boon, name, value):
            err(f"HarvestBoon.{name} is not tools/vesperine.py's {value}")
    boss = sources.get("VesperineEntity", "")
    for attack, (windup, active, recovery, cooldown, near, far, reaping, moon) in vs.ATTACKS.items():
        expected = (f"{attack}({windup}, {active}, {recovery}, {cooldown}, {float(near)}, {float(far)}, "
                    f"{str(reaping).lower()}, {str(moon).lower()})")
        if expected not in boss:
            err(f"VesperineEntity.Attack: expected {expected}")
    # Where her fight looks for the Hollow Acre's parts (HollowAcre.java) is where tools/hollow_acre.py builds them.
    import hollow_acre as ha
    acre = re.sub(r"\s+", " ", sources.get("HollowAcre", ""))
    for name, value in (("ARENA_X", ha.ARENA_CENTRE[0]), ("ARENA_Z", ha.ARENA_CENTRE[1]), ("FLOOR", ha.SURFACE + 1),
                        ("ARENA_RADIUS", ha.ARENA_RADIUS), ("FIELD_NORTH", ha.FIELD[0]), ("FIELD_SOUTH", ha.FIELD[1]),
                        ("PATH_WEST", ha.PATH[0]), ("PATH_EAST", ha.PATH[1])):
        if not java_number(acre, name, value):
            err(f"HollowAcre.{name} is not tools/hollow_acre.py's {value}")
    if "THRONE = new BlockPos({}, {}, {});".format(*ha.THRONE) not in acre:
        err(f"HollowAcre.THRONE is not tools/hollow_acre.py's {ha.THRONE}")
    if "WARDS = List.of({});".format(", ".join("new BlockPos({}, {}, {})".format(*ward) for ward in ha.WARDS)) not in acre:
        err(f"HollowAcre.WARDS are not tools/hollow_acre.py's {ha.WARDS}")
    loot_java = sources.get("VesperineLoot", "")
    if f'ADVANCEMENT = "{vs.ADVANCEMENT["key"]}";' not in loot_java or \
            f"EVENT_HOOD_CHANCE = {vs.EVENT_HOOD_CHANCE}F;" not in loot_java:
        err("VesperineLoot's advancement or event hood chance differs from tools/vesperine.py")
    config = (JAVA_ROOT / "config" / "JugcraftConfig.java").read_text(encoding="utf-8")
    for option, default in vs.OPTIONS.items():
        if f'Map.entry("{option}", "{default}")' not in config:
            err(f"JugcraftConfig.TEXT_OPTIONS lacks {option} (default {default})")
    shared = (JAVA_ROOT / "lair" / "LairBosses.java").read_text(encoding="utf-8")
    for option, (low, high) in vs.LIMITS.items():
        if f'Lairs.decimal("{option}", {float(vs.OPTIONS[option])}, {low}, {high})' not in shared:
            err(f"LairBosses does not read {option} with default {vs.OPTIONS[option]} kept within {low} to {high}")
    for call in ("LairBosses.damage(base)", "LairBosses.partyScale(players, PARTY_STEP, PARTY_MAX)", "LairBosses.eligible(player)"):
        if call not in boss:
            err(f"VesperineEntity does not scale and choose foes as every lair boss does ({call})")
    registry = sources.get("JugcraftVesperine", "")
    client = (CLIENT_JAVA_ROOT / "VesperineClient.java").read_text(encoding="utf-8")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    for entity in vs.ENTITIES:
        if f'entity("{entity}"' not in registry:
            err(f"JugcraftVesperine does not register the {entity} entity")
        if f"entity.{MOD}.{entity}" not in lang:
            err(f"No name for the {entity} entity")
        if f"JugcraftVesperine.{entity.upper()}," not in client:
            err(f"VesperineClient draws no {entity}")
    for item in vs.items():
        if f'"{item}"' not in registry:
            err(f"JugcraftVesperine does not register {item}")
    # The GeckoLib bodies.
    clips_named = {}
    for cls, text in sources.items():
        for clip in re.findall(r'"(animation\.[a-z_]+\.)"|"(animation\.[a-z_]+\.[a-z_]+)"', text):
            name = clip[1] or None
            if name:
                clips_named.setdefault(name.split(".")[1], set()).add(name)
    for clip in vm.CLIPS["vesperine"]:
        clips_named.setdefault("vesperine", set()).add(f"animation.vesperine.{clip}")
    for entity, body in vs.GECKO.items():
        geo = load(ASSETS / "geckolib" / "models" / "entity" / f"{entity}.geo.json") or {}
        animations = load(ASSETS / "geckolib" / "animations" / "entity" / f"{entity}.animation.json") or {}
        definition = (geo.get("minecraft:geometry") or [{}])[0]
        if not definition:
            err(f"{entity}: no GeckoLib model")
            continue
        clips = animations.get("animations", {})
        for clip in clips_named.get(body, set()):
            if clip not in clips:
                err(f"{entity}: the Java plays {clip}, which is not in {entity}.animation.json")
        for clip in vm.CLIPS[body]:
            if f"animation.{body}.{clip}" not in clips:
                err(f"{entity}.animation.json: missing animation.{body}.{clip}")
        width = definition.get("description", {}).get("texture_width", 0)
        height = definition.get("description", {}).get("texture_height", 0)
        bones = {bone["name"] for bone in definition.get("bones", [])}
        for clip in clips.values():
            for bone in clip.get("bones", {}):
                if bone not in bones:
                    err(f"{entity}.animation.json: animates unknown bone {bone}")
        regions = set()
        for bone in definition.get("bones", []):
            for cube in bone.get("cubes", []):
                w, h, d = cube["size"]
                u, v = cube["uv"]
                region = (u, v, u + 2 * (w + d), v + d + h)
                if region[2] > width or region[3] > height:
                    err(f"{entity}.geo.json: a cube in {bone['name']} maps outside the {width}x{height} sheet")
                regions.add(region)
        regions = sorted(regions)
        for i, a in enumerate(regions):
            for b in regions[i + 1:]:
                if a[0] < b[2] and b[0] < a[2] and a[1] < b[3] and b[1] < a[3]:
                    err(f"{entity}.geo.json: UV regions {a} and {b} overlap")
        for name in (entity, f"{entity}_glowmask"):
            sheet = ASSETS / "textures" / "entity" / f"{name}.png"
            if not sheet.is_file():
                err(f"{entity}: missing textures/entity/{name}.png")
                continue
            with Image.open(sheet) as image:
                if image.size != (width * vm.TEXTURE_SCALE, height * vm.TEXTURE_SCALE):
                    err(f"textures/entity/{name}.png must be {width * vm.TEXTURE_SCALE}x{height * vm.TEXTURE_SCALE}")
                if any(image.convert("RGBA").getchannel("A").histogram()[1:255]):
                    err(f"textures/entity/{name}.png has half-transparent pixels (it is drawn cut out)")
    # Her controllers own their bones: the body's clips never key the halo's or the scythe's, nor theirs the body's.
    clips = vm.ANIMATIONS["vesperine"]()["animations"]
    owned = {bone: controller for controller, bones in vm.CONTROLLERS.items() for bone in bones}
    for name, clip in clips.items():
        short = name.split(".")[-1]
        controller = "halo" if short.startswith("halo_") else "scythe" if short in ("armed", "unarmed") else "body"
        for bone in clip.get("bones", {}):
            if owned.get(bone, "body") != controller:
                err(f"vesperine: {name} ({controller}) animates {bone}, which the {owned.get(bone, 'body')} controller owns")
    # Her loot.
    table = load(DATA / MOD / "loot_table" / "entities" / "vesperine.json") or {}
    if table.get("type") != "minecraft:gift":
        err("loot_table/entities/vesperine.json must be a gift table (rolled for each participant)")
    found = {}
    for pool in table.get("pools", []):
        for entry in pool.get("entries", []):
            item = entry.get("name", "").split(":")[-1]
            chance = pool.get("condition", {}).get("chance")
            count = entry.get("modifier", {}).get("count", {})
            found[item] = chance if chance is not None else (count.get("min"), count.get("max"))
    expected = {"reaper_shade": vs.SHADE, **vs.CHANCES}
    if found != expected:
        err(f"loot_table/entities/vesperine.json gives {found}, not tools/vesperine.py's {expected}")
    if f'Jugcraft.id("entities/vesperine")' not in loot_java:
        err("VesperineLoot must roll loot_table/entities/vesperine.json")
    for key in vs.RECIPES:
        if not (DATA / MOD / "recipe" / f"{key}.json").is_file():
            err(f"recipe/{key}.json is missing")
    costumes = json.dumps(load(DATA / MOD / "tags" / "item" / "trick_or_treat_costumes.json") or {})
    for item in ("reaper_hood", "dirge_skull", "requiem_skull"):
        if f"{MOD}:{item}" not in costumes:
            err(f"{item} must count as a costume (tags/item/trick_or_treat_costumes.json)")
    advancement = load(DATA / MOD / "advancement" / f"{vs.ADVANCEMENT['key']}.json") or {}
    if advancement.get("criteria", {}).get("done", {}).get("trigger") != "minecraft:impossible":
        err(f"advancement/{vs.ADVANCEMENT['key']}.json must be granted from code (an impossible \"done\")")
    for cls, text in sources.items():
        for key in re.findall(r'"((?:message|tooltip|entity)\.jugcraft\.[a-z_.]+[a-z_])"', text):
            if key not in lang:
                err(f"{cls}.java sends {key}, which has no words in en_us.json")
        for banned, why in (("setChunkForced", "loads a chunk"), ("addRegionTicket", "loads a chunk"),
                            ("TicketType", "loads a chunk"), ("changeDimension", "crosses dimensions"),
                            ("TeleportTransition", "crosses dimensions")):
            if banned in text:
                err(f"{cls}.java {why} ({banned})")



def check_tatterlace():
    """Madame Tatterlace (tools/tatterlace.py, docs/features/tatterlace.md): every number the Java uses is the table's; her
    attacks are the table's; where her fight looks for the Spindle Loft's parts (SpindleLoft.java) is where
    tools/spindle_loft.py builds them, and its lace rule finds every lace cell of the template with its pattern; her heights
    meet the white silk (her perch on its thread, her dragline up to it); she spits as many egg sacs as there are places
    for them; every entity is registered, named and drawn; each GeckoLib body has its model, clips (every clip the Java
    names) and sheet, its box-UV regions inside the sheet without overlapping, its sheet (and its glowmask if it is drawn
    with a glow layer, and none if not) TEXTURE_SCALE times the size the model declares and cut out; no bone is animated by both of her controllers; her loot table is the
    table's; her recipes, tags and advancement are there; the Stitch boon is bounded; every message she sends has its
    words; and nothing of hers loads a chunk or leaves its dimension."""
    import math
    import spindle_loft as sl
    import tatterlace as tt
    import tatterlace_models as tm
    folder = JAVA_ROOT / "lair" / "tatterlace"
    sources = {path.stem: path.read_text(encoding="utf-8") for path in folder.glob("*.java")}
    if not sources:
        err("lair/tatterlace: no Java")
        return
    for cls, table in (("TatterlaceEntity", tt.TATTERLACE), ("TossedThimbleEntity", tt.THIMBLE),
                       ("BindingThreadEntity", tt.THREAD), ("LaceSnareEntity", tt.SNARE), ("RollingSpoolEntity", tt.SPOOL),
                       ("SpiderlingEntity", tt.SPIDERLING), ("TatterEggSacEntity", tt.EGG_SAC),
                       ("GoldenThimble", tt.GOLDEN_THIMBLE)):
        for name, value in table.items():
            if not java_number(sources.get(cls, ""), name, value):
                err(f"{cls}.{name} is not tools/tatterlace.py's {value}")
    boon = (JAVA_ROOT / "weapons" / "StitchBoon.java").read_text(encoding="utf-8")
    for name, value in tt.STITCH.items():
        if not java_number(boon, name, value):
            err(f"StitchBoon.{name} is not tools/tatterlace.py's {value}")
    if tt.STITCH["SLOW_TICKS"] > 100 or tt.STITCH["SLOW_AMPLIFIER"] > 1:
        err("tools/tatterlace.py: the Stitch boon lasts over 5 s or above amplifier 1, as no boon may")
    boss = sources.get("TatterlaceEntity", "")
    for attack, (windup, active, recovery, cooldown, near, far, fitting, final, frenzy) in tt.ATTACKS.items():
        expected = (f"{attack}({windup}, {active}, {recovery}, {cooldown}, {float(near)}, {float(far)}, "
                    f"{str(fitting).lower()}, {str(final).lower()}, {str(frenzy).lower()})")
        if expected not in boss:
            err(f"TatterlaceEntity.Attack: expected {expected}")
    for call in ("LairBosses.damage(base)", "LairBosses.partyScale(players, PARTY_STEP, PARTY_MAX)"):
        if call not in boss:
            err(f"TatterlaceEntity does not scale as every lair boss does ({call})")
    # Where her fight looks for the Spindle Loft's parts is where tools/spindle_loft.py builds them.
    loft = re.sub(r"\s+", " ", sources.get("SpindleLoft", ""))
    for name, value in (("LACE", sl.LACE), ("DOILY_X", sl.DOILY[0]), ("DOILY_Z", sl.DOILY[1]), ("DOILY_RADIUS", sl.DOILY_RADIUS),
                        ("BARREL", sl.BARREL), ("SAFE_RING", sl.SAFE_RING), ("WEB", sl.SPOOL_TOP + 4),
                        ("TAPE_WEST", sl.TAPE["x"][0]), ("TAPE_EAST", sl.TAPE["x"][1]), ("TAPE_FOOT", sl.TAPE["foot"]),
                        ("SAC_RADIUS", tt.SAC_RADIUS)):
        if not java_number(loft, name, value):
            err(f"SpindleLoft.{name} is not {value} (tools/spindle_loft.py, tools/tatterlace.py)")
    spools = ", ".join("new double[] {{{}, {}}}".format(*sl.SPOOLS[c]) for c in ("green", "blue", "beige", "red"))
    if f"SPOOLS = List.of({spools});" not in loft:
        err(f"SpindleLoft.SPOOLS are not tools/spindle_loft.py's spools (green, blue, beige, red): {spools}")
    rings = ", ".join(str(limit) for limit, _ in sl.RINGS)
    patterns = ", ".join(str(pattern) for _, pattern in sl.RINGS)
    if f"RINGS = {{{rings}}};" not in loft or f"PATTERNS = {{{patterns}}};" not in loft:
        err(f"SpindleLoft.RINGS and PATTERNS are not tools/spindle_loft.py's RINGS {sl.RINGS}")
    if "SAC_ANGLES = List.of({});".format(", ".join(str(a) for a in tt.SAC_ANGLES)) not in loft:
        err(f"SpindleLoft.SAC_ANGLES are not tools/tatterlace.py's {tt.SAC_ANGLES}")
    # SpindleLoft.lace, as tools/spindle_loft.py builds the doily: every lace cell of the template has the pattern the rule
    # gives it, and every cell the rule calls lace is lace in the template or holds another of its blocks (the thimble).
    blocks = sl.build()
    def rule(x, z):
        r = math.hypot(x + 0.5 - sl.DOILY[0], z + 0.5 - sl.DOILY[1])
        near = min(math.hypot(x + 0.5 - sx, z + 0.5 - sz) for sx, sz in sl.SPOOLS.values())
        if r > sl.DOILY_RADIUS or near <= sl.BARREL:
            return -1
        return 0 if near <= sl.BARREL + sl.SAFE_RING else sl.lace_pattern(r)
    reach = int(sl.DOILY_RADIUS) + 2
    for x in range(int(sl.DOILY[0]) - reach, int(sl.DOILY[0]) + reach + 1):
        for z in range(int(sl.DOILY[1]) - reach, int(sl.DOILY[1]) + reach + 1):
            state = blocks.get((x, sl.LACE, z))
            lace = state is not None and state[0] == f"{MOD}:doily_lace"
            pattern = rule(x, z)
            if lace and pattern != int(dict(state[1]).get("pattern", -1)):
                err(f"SpindleLoft.lace: the template's lace at ({x}, {z}) is pattern {dict(state[1]).get('pattern')}, the rule says {pattern}")
            elif not lace and pattern >= 0 and state is None:
                err(f"SpindleLoft.lace: the rule puts lace at ({x}, {z}), where the template has none")
    # Her heights meet the white silk: her perch on its thread's top, her dragline up to its middle.
    web = sl.SPOOL_TOP + 4 - sl.LACE
    if tt.TATTERLACE["PERCH"] != web + 9 / 16:
        err(f"tools/tatterlace.py PERCH must be the silk's top, {web + 9 / 16} over the lace")
    if tt.TATTERLACE["HANG"] + (tm.DRAGLINE_Y + 48) / 16 != web + 0.5:
        err("tools/tatterlace.py HANG: her dragline (tools/tatterlace_models.py) must reach the white silk's middle")
    if tt.ATTACKS["UNRAVEL"][0] != tt.TATTERLACE["FRAY_TICKS"] or tt.ATTACKS["DROP_STRIKE"][2] != tt.TATTERLACE["DROP_OPEN_TICKS"]:
        err("tools/tatterlace.py: Unravel's wind-up must be FRAY_TICKS, and Drop Strike's recovery DROP_OPEN_TICKS")
    if tt.TATTERLACE["EGG_SACS"] != len(tt.SAC_ANGLES):
        err(f"tools/tatterlace.py: EGG_SACS is {tt.TATTERLACE['EGG_SACS']}, but there are {len(tt.SAC_ANGLES)} places for them")
    loot_java = sources.get("TatterlaceLoot", "")
    if f'ADVANCEMENT = "{tt.ADVANCEMENT["key"]}";' not in loot_java or \
            f"EVENT_HEADDRESS_CHANCE = {tt.EVENT_HEADDRESS_CHANCE}F;" not in loot_java:
        err("TatterlaceLoot's advancement or event headdress chance differs from tools/tatterlace.py")
    registry = sources.get("JugcraftTatterlace", "")
    client = (CLIENT_JAVA_ROOT / "TatterlaceClient.java").read_text(encoding="utf-8")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    for entity in tt.ENTITIES:
        if f'entity("{entity}"' not in registry:
            err(f"JugcraftTatterlace does not register the {entity} entity")
        if f"entity.{MOD}.{entity}" not in lang:
            err(f"No name for the {entity} entity")
        if f"JugcraftTatterlace.{entity.upper()}," not in client:
            err(f"TatterlaceClient draws no {entity}")
    for item in tt.items():
        if f'"{item}"' not in registry:
            err(f"JugcraftTatterlace does not register {item}")
    # The GeckoLib bodies.
    clips_named = {}
    for text in sources.values():
        for name in re.findall(r'"(animation\.[a-z_]+\.[a-z_]+)"', text):
            clips_named.setdefault(name.split(".")[1], set()).add(name)
    for clip in tm.CLIPS["tatterlace"]:
        clips_named.setdefault("tatterlace", set()).add(f"animation.tatterlace.{clip}")
    for entity, body in tt.GECKO.items():
        geo = load(ASSETS / "geckolib" / "models" / "entity" / f"{entity}.geo.json") or {}
        animations = load(ASSETS / "geckolib" / "animations" / "entity" / f"{entity}.animation.json") or {}
        definition = (geo.get("minecraft:geometry") or [{}])[0]
        if not definition:
            err(f"{entity}: no GeckoLib model")
            continue
        clips = animations.get("animations", {})
        for clip in clips_named.get(body, set()):
            if clip not in clips:
                err(f"{entity}: the Java plays {clip}, which is not in {entity}.animation.json")
        for clip in tm.CLIPS[body]:
            if f"animation.{body}.{clip}" not in clips:
                err(f"{entity}.animation.json: missing animation.{body}.{clip}")
        width = definition.get("description", {}).get("texture_width", 0)
        height = definition.get("description", {}).get("texture_height", 0)
        bones = {bone["name"] for bone in definition.get("bones", [])}
        for clip in clips.values():
            for bone in clip.get("bones", {}):
                if bone not in bones:
                    err(f"{entity}.animation.json: animates unknown bone {bone}")
        regions = set()
        for bone in definition.get("bones", []):
            for cube in bone.get("cubes", []):
                w, h, d = cube["size"]
                u, v = cube["uv"]
                region = (u, v, u + 2 * (w + d), v + d + h)
                if region[2] > width or region[3] > height:
                    err(f"{entity}.geo.json: a cube in {bone['name']} maps outside the {width}x{height} sheet")
                regions.add(region)
        regions = sorted(regions)
        for i, a in enumerate(regions):
            for b in regions[i + 1:]:
                if a[0] < b[2] and b[0] < a[2] and a[1] < b[3] and b[1] < a[3]:
                    err(f"{entity}.geo.json: UV regions {a} and {b} overlap")
        # A body with a glow layer has a glowmask; one without has none (it would be a texture nothing draws).
        glows = entity in tt.GLOWING
        start = client.find(f"register(JugcraftTatterlace.{entity.upper()},")
        end = client.find("EntityRendererRegistry.register(", start + 1)
        if start >= 0 and ("AutoGlowingGeoLayer" in client[start:end if end >= 0 else len(client)]) != glows:
            err(f"TatterlaceClient: the {entity}'s glow layer must match tools/tatterlace.py GLOWING")
        if not glows and (ASSETS / "textures" / "entity" / f"{entity}_glowmask.png").is_file():
            err(f"textures/entity/{entity}_glowmask.png: the {entity} has no glow layer, so no glowmask")
        for name in (entity, f"{entity}_glowmask") if glows else (entity,):
            sheet = ASSETS / "textures" / "entity" / f"{name}.png"
            if not sheet.is_file():
                err(f"{entity}: missing textures/entity/{name}.png")
                continue
            with Image.open(sheet) as image:
                if image.size != (width * tm.TEXTURE_SCALE, height * tm.TEXTURE_SCALE):
                    err(f"textures/entity/{name}.png must be {width * tm.TEXTURE_SCALE}x{height * tm.TEXTURE_SCALE}")
                if any(image.convert("RGBA").getchannel("A").histogram()[1:255]):
                    err(f"textures/entity/{name}.png has half-transparent pixels (it is drawn cut out)")
    # Her controllers own their bones: the body's clips never key a cuff, nor the cuffs' a body bone.
    owned = {bone: controller for controller, bones in tm.CONTROLLERS.items() for bone in bones}
    for name, clip in tm.ANIMATIONS["tatterlace"]()["animations"].items():
        controller = "cuffs" if name.split(".")[-1].startswith("cuffs_") else "body"
        for bone in clip.get("bones", {}):
            if owned.get(bone, "body") != controller:
                err(f"tatterlace: {name} ({controller}) animates {bone}, which the {owned.get(bone, 'body')} controller owns")
    # Her loot.
    table = load(DATA / MOD / "loot_table" / "entities" / "tatterlace.json") or {}
    if table.get("type") != "minecraft:gift":
        err("loot_table/entities/tatterlace.json must be a gift table (rolled for each participant)")
    found = {}
    for pool in table.get("pools", []):
        for entry in pool.get("entries", []):
            item = entry.get("name", "").split(":")[-1]
            chance = pool.get("condition", {}).get("chance")
            count = entry.get("modifier", {}).get("count", {})
            found[item] = chance if chance is not None else (count.get("min"), count.get("max"))
    expected = {"gossamer_silk": tt.SILK, **tt.CHANCES}
    if found != expected:
        err(f"loot_table/entities/tatterlace.json gives {found}, not tools/tatterlace.py's {expected}")
    if 'Jugcraft.id("entities/tatterlace")' not in loot_java:
        err("TatterlaceLoot must roll loot_table/entities/tatterlace.json")
    for key in tt.RECIPES:
        if not (DATA / MOD / "recipe" / f"{key}.json").is_file():
            err(f"recipe/{key}.json is missing")
    costumes = json.dumps(load(DATA / MOD / "tags" / "item" / "trick_or_treat_costumes.json") or {})
    for item in tt.COSTUMES:
        if f"{MOD}:{item}" not in costumes:
            err(f"{item} must count as a costume (tags/item/trick_or_treat_costumes.json)")
    advancement = load(DATA / MOD / "advancement" / f"{tt.ADVANCEMENT['key']}.json") or {}
    if advancement.get("criteria", {}).get("done", {}).get("trigger") != "minecraft:impossible":
        err(f"advancement/{tt.ADVANCEMENT['key']}.json must be granted from code (an impossible \"done\")")
    for cls, text in list(sources.items()) + [("StitchBoon", boon)]:
        for key in re.findall(r'"((?:message|tooltip|entity)\.jugcraft\.[a-z_.]+[a-z_])"', text):
            if key not in lang:
                err(f"{cls}.java sends {key}, which has no words in en_us.json")
        for banned, why in (("setChunkForced", "loads a chunk"), ("addRegionTicket", "loads a chunk"),
                            ("TicketType", "loads a chunk"), ("changeDimension", "crosses dimensions"),
                            ("TeleportTransition", "crosses dimensions")):
            if banned in text:
                err(f"{cls}.java {why} ({banned})")

def check_yeti():
    """The Yeti King (tools/yeti_king.py, docs/features/yeti-king.md): every number the Java uses is the table's; his
    attacks are the table's; where his fight looks for the Glacier Hall's parts (GlacierHall.java) is where
    tools/glacier_hall.py builds them, and the places he leaps to, roars from and calls his kin out of are clear in the
    template, on its floor; the block he hurls leaves from where his model holds it; every entity is registered, named
    and drawn; each GeckoLib body has its model, clips (every clip the Java names) and sheet, its box-UV regions inside
    the sheet without overlapping, its sheet (and its glowmask if it is drawn with a glow layer, and none if not)
    TEXTURE_SCALE times the size the model declares and cut out; no bone is animated by both of his controllers; his loot
    table is the table's and his trophies are Arms VII's; his recipes, tags and advancement are there; every message he
    sends has its words; and nothing of his loads a chunk or leaves its dimension."""
    import arms_variants
    import glacier_hall as gh
    import yeti_king as yk
    import yeti_king_models as ym
    folder = JAVA_ROOT / "lair" / "yeti"
    sources = {path.stem: path.read_text(encoding="utf-8") for path in folder.glob("*.java")}
    if not sources:
        err("lair/yeti: no Java")
        return
    for cls, table in (("YetiKingEntity", yk.YETI_KING), ("YetiWhelpEntity", yk.WHELP), ("HurledBoulderEntity", yk.BOULDER),
                       ("FallingIcicleEntity", yk.ICICLE), ("GlacialSpikeEntity", yk.SPIKE), ("YetiMitten", yk.MITTEN)):
        for name, value in table.items():
            if not java_number(sources.get(cls, ""), name, value):
                err(f"{cls}.{name} is not tools/yeti_king.py's {value}")
    boss = sources.get("YetiKingEntity", "")
    for attack, (windup, active, recovery, cooldown, near, far, hunt, blizzard, fury) in yk.ATTACKS.items():
        expected = (f"{attack}({windup}, {active}, {recovery}, {cooldown}, {float(near)}, {float(far)}, "
                    f"{str(hunt).lower()}, {str(blizzard).lower()}, {str(fury).lower()})")
        if expected not in boss:
            err(f"YetiKingEntity.Attack: expected {expected}")
    for call in ("LairBosses.damage(base)", "LairBosses.partyScale(players, PARTY_STEP, PARTY_MAX)"):
        if call not in boss:
            err(f"YetiKingEntity does not scale as every lair boss does ({call})")
    if yk.ATTACKS["FROST_BREATH"][6:] != (True, False, False):
        err("tools/yeti_king.py: Frost Breath is the Hunt's alone (the King's Roar ends it)")
    # Where his fight looks for the Glacier Hall's parts is where tools/glacier_hall.py builds them.
    hall = re.sub(r"\s+", " ", sources.get("GlacierHall", ""))
    for name, value in (("LAKE", gh.LAKE), ("LAKE_X", gh.LAKE_CENTRE[0]), ("LAKE_Z", gh.LAKE_CENTRE[1]),
                        ("LAKE_RADIUS", gh.LAKE_RADIUS), ("COLUMN_RADIUS", gh.COLUMN_RADIUS),
                        ("COLUMN_FOOT", gh.COLUMN_FOOT), ("STEP_TOP", gh.STEP_TOP), ("THRONE_X", gh.DAIS_CENTRE[0]),
                        ("THRONE_Z", gh.DAIS_CENTRE[1]), ("SEAT_TOP", gh.STEP_TOP + 1)):
        if not java_number(hall, name, value):
            err(f"GlacierHall.{name} is not {value} (tools/glacier_hall.py)")
    columns = ", ".join("new double[] {{{}, {}}}".format(*c) for c in gh.COLUMNS)
    if f"COLUMNS = List.of({columns});" not in hall:
        err(f"GlacierHall.COLUMNS are not tools/glacier_hall.py's: {columns}")
    found = re.search(r"\bROAR_Z = ([0-9.]+);", hall)
    dens = re.findall(r"new double\[\] \{([0-9.]+), ([0-9.]+)\}", hall.split("DENS = ", 1)[-1].split(";", 1)[0])
    blocks = gh.build()
    name = lambda pos: blocks[pos][0] if pos in blocks else None
    solid = lambda pos: name(pos) not in (None, "minecraft:light")
    def clear(x, y, z, label, floor=True, height=4, half=1):
        """Room for him (or a whelp) standing at (x, y, z) in the template, on its floor (a pelt, a carpet, is no
        obstacle)."""
        cx, cz = int(x // 1), int(z // 1)
        for dx in range(-half, half + 1):
            for dz in range(-half, half + 1):
                for dy in range(height):
                    pos = (cx + dx, int(y) + dy, cz + dz)
                    if solid(pos) and not name(pos).endswith("_carpet"):
                        err(f"GlacierHall: {label} at ({x}, {y}, {z}) is not clear: {name((cx + dx, int(y) + dy, cz + dz))} "
                            f"at {(cx + dx, int(y) + dy, cz + dz)}")
                        return
        if floor and not solid((cx, int(y) - 1, cz)):
            err(f"GlacierHall: {label} at ({x}, {y}, {z}) stands on nothing")
    seat = (int(gh.DAIS_CENTRE[0]), gh.STEP_TOP, int(gh.DAIS_CENTRE[1]))
    if not solid(seat):
        err(f"GlacierHall: no throne seat under him at {seat}")
    if found is None:
        err("GlacierHall.ROAR_Z is missing")
    else:
        clear(gh.DAIS_CENTRE[0], gh.STEP_TOP, float(found.group(1)), "where he roars from")
    clear(gh.LAKE_CENTRE[0], gh.LAKE + 1, gh.LAKE_CENTRE[1] - gh.LAKE_RADIUS + 4.0, "where he lands from his dais")
    if len(dens) != len(gh.DENS):
        err(f"GlacierHall.DENS: {len(dens)} dens, tools/glacier_hall.py builds {len(gh.DENS)}")
    for x, z in dens:
        if not any(abs(float(x) - cx) <= 1.0 and abs(float(z) - cz) <= 1.0 for (cx, _, cz), _ in gh.DENS.values()):
            err(f"GlacierHall.DENS: ({x}, {z}) is not at a den of tools/glacier_hall.py DENS")
        clear(float(x), gh.LAKE + 1, float(z), "a den his kin come out of", height=2, half=0)
    # The block he hurls leaves from where his model holds it over his head.
    if yk.BOULDER["HELD"] != (ym.HELD_Y - 8) / 16:
        err(f"tools/yeti_king.py BOULDER HELD must be where tools/yeti_king_models.py holds it, {(ym.HELD_Y - 8) / 16}")
    loot_java = sources.get("YetiKingLoot", "")
    if f'ADVANCEMENT = "{yk.ADVANCEMENT["key"]}";' not in loot_java:
        err("YetiKingLoot's advancement differs from tools/yeti_king.py")
    registry = sources.get("JugcraftYeti", "")
    client = (CLIENT_JAVA_ROOT / "YetiKingClient.java").read_text(encoding="utf-8")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    for entity in yk.ENTITIES:
        if f'entity("{entity}"' not in registry:
            err(f"JugcraftYeti does not register the {entity} entity")
        if f"entity.{MOD}.{entity}" not in lang:
            err(f"No name for the {entity} entity")
        if f"JugcraftYeti.{entity.upper()}," not in client:
            err(f"YetiKingClient draws no {entity}")
    for item in yk.items():
        if f'"{item}"' not in registry:
            err(f"JugcraftYeti does not register {item}")
    # The GeckoLib bodies.
    clips_named = {}
    for text in sources.values():
        for clip in re.findall(r'"(animation\.[a-z_]+\.[a-z_]+)"', text):
            clips_named.setdefault(clip.split(".")[1], set()).add(clip)
    # His body's clips are named from a prefix (YetiKingEntity's BODY table).
    for clip in re.findall(r'prefix \+ "([a-z_]+)"', boss):
        clips_named.setdefault("yeti_king", set()).add(f"animation.yeti_king.{clip}")
    for clip in ym.CLIPS["yeti_king"]:
        clips_named.setdefault("yeti_king", set()).add(f"animation.yeti_king.{clip}")
    for entity, body in yk.GECKO.items():
        geo = load(ASSETS / "geckolib" / "models" / "entity" / f"{entity}.geo.json") or {}
        animations = load(ASSETS / "geckolib" / "animations" / "entity" / f"{entity}.animation.json") or {}
        definition = (geo.get("minecraft:geometry") or [{}])[0]
        if not definition:
            err(f"{entity}: no GeckoLib model")
            continue
        clips = animations.get("animations", {})
        for clip in clips_named.get(body, set()):
            if clip not in clips:
                err(f"{entity}: the Java plays {clip}, which is not in {entity}.animation.json")
        for clip in ym.CLIPS[body]:
            if f"animation.{body}.{clip}" not in clips:
                err(f"{entity}.animation.json: missing animation.{body}.{clip}")
        width = definition.get("description", {}).get("texture_width", 0)
        height = definition.get("description", {}).get("texture_height", 0)
        bones = {bone["name"] for bone in definition.get("bones", [])}
        for clip in clips.values():
            for bone in clip.get("bones", {}):
                if bone not in bones:
                    err(f"{entity}.animation.json: animates unknown bone {bone}")
        regions = set()
        for bone in definition.get("bones", []):
            for cube in bone.get("cubes", []):
                w, h, d = cube["size"]
                u, v = cube["uv"]
                region = (u, v, u + 2 * (w + d), v + d + h)
                if region[2] > width or region[3] > height:
                    err(f"{entity}.geo.json: a cube in {bone['name']} maps outside the {width}x{height} sheet")
                regions.add(region)
        regions = sorted(regions)
        for i, a in enumerate(regions):
            for b in regions[i + 1:]:
                if a[0] < b[2] and b[0] < a[2] and a[1] < b[3] and b[1] < a[3]:
                    err(f"{entity}.geo.json: UV regions {a} and {b} overlap")
        # A body with a glow layer has a glowmask; one without has none (it would be a texture nothing draws).
        glows = entity in yk.GLOWING
        start = client.find(f"register(JugcraftYeti.{entity.upper()},")
        end = client.find("EntityRendererRegistry.register(", start + 1)
        if start >= 0 and ("AutoGlowingGeoLayer" in client[start:end if end >= 0 else len(client)]) != glows:
            err(f"YetiKingClient: the {entity}'s glow layer must match tools/yeti_king.py GLOWING")
        if not glows and (ASSETS / "textures" / "entity" / f"{entity}_glowmask.png").is_file():
            err(f"textures/entity/{entity}_glowmask.png: the {entity} has no glow layer, so no glowmask")
        for name_ in (entity, f"{entity}_glowmask") if glows else (entity,):
            sheet = ASSETS / "textures" / "entity" / f"{name_}.png"
            if not sheet.is_file():
                err(f"{entity}: missing textures/entity/{name_}.png")
                continue
            with Image.open(sheet) as image:
                if image.size != (width * ym.TEXTURE_SCALE, height * ym.TEXTURE_SCALE):
                    err(f"textures/entity/{name_}.png must be {width * ym.TEXTURE_SCALE}x{height * ym.TEXTURE_SCALE}")
                if any(image.convert("RGBA").getchannel("A").histogram()[1:255]):
                    err(f"textures/entity/{name_}.png has half-transparent pixels (it is drawn cut out)")
    # His controllers own their bones: the body's clips never key the crown's fire, nor the crown's a body bone.
    owned = {bone: controller for controller, bones in ym.CONTROLLERS.items() for bone in bones}
    for clip_name, clip in ym.ANIMATIONS["yeti_king"]()["animations"].items():
        controller = "crown" if clip_name.split(".")[-1].startswith("crown_") else "body"
        for bone in clip.get("bones", {}):
            if owned.get(bone, "body") != controller:
                err(f"yeti_king: {clip_name} ({controller}) animates {bone}, which the {owned.get(bone, 'body')} controller owns")
    # His loot, and his trophies: Arms VII's, as his bosses/yeti_king table holds them.
    table = load(DATA / MOD / "loot_table" / "entities" / "yeti_king.json") or {}
    if table.get("type") != "minecraft:gift":
        err("loot_table/entities/yeti_king.json must be a gift table (rolled for each participant)")
    found_loot = {}
    for pool in table.get("pools", []):
        for entry in pool.get("entries", []):
            item = entry.get("name", "").split(":")[-1]
            chance = pool.get("condition", {}).get("chance")
            count = entry.get("modifier", {}).get("count", {})
            found_loot[item] = chance if chance is not None else (count.get("min"), count.get("max"))
    expected = {"yeti_fur": yk.FUR, **{item: yk.TROPHY_CHANCE for item in yk.TROPHIES}, **yk.CHANCES}
    if found_loot != expected:
        err(f"loot_table/entities/yeti_king.json gives {found_loot}, not tools/yeti_king.py's {expected}")
    if tuple(arms_variants.trophies("yeti_king")) != tuple(yk.TROPHIES):
        err(f"tools/yeti_king.py TROPHIES {yk.TROPHIES} are not Arms VII's: {arms_variants.trophies('yeti_king')}")
    trophies = load(DATA / MOD / "loot_table" / "bosses" / "yeti_king.json") or {}
    held = [entry.get("name") for pool in trophies.get("pools", []) for entry in pool.get("entries", [])]
    if held != [f"{MOD}:{item}" for item in yk.TROPHIES]:
        err(f"loot_table/bosses/yeti_king.json holds {held}, not his trophies {yk.TROPHIES}")
    for table_id in ("entities/yeti_king", "bosses/yeti_king"):
        if f'Jugcraft.id("{table_id}")' not in loot_java:
            err(f"YetiKingLoot must roll loot_table/{table_id}.json")
    for key in yk.RECIPES:
        if not (DATA / MOD / "recipe" / f"{key}.json").is_file():
            err(f"recipe/{key}.json is missing")
    costumes = json.dumps(load(DATA / MOD / "tags" / "item" / "trick_or_treat_costumes.json") or {})
    for item in yk.COSTUMES:
        if f"{MOD}:{item}" not in costumes:
            err(f"{item} must count as a costume (tags/item/trick_or_treat_costumes.json)")
    advancement = load(DATA / MOD / "advancement" / f"{yk.ADVANCEMENT['key']}.json") or {}
    if advancement.get("criteria", {}).get("done", {}).get("trigger") != "minecraft:impossible":
        err(f"advancement/{yk.ADVANCEMENT['key']}.json must be granted from code (an impossible \"done\")")
    for cls, text in sources.items():
        for key in re.findall(r'"((?:message|tooltip|entity)\.jugcraft\.[a-z_.]+[a-z_])"', text):
            if key not in lang:
                err(f"{cls}.java sends {key}, which has no words in en_us.json")
        for banned, why in (("setChunkForced", "loads a chunk"), ("addRegionTicket", "loads a chunk"),
                            ("TicketType", "loads a chunk"), ("changeDimension", "crosses dimensions"),
                            ("TeleportTransition", "crosses dimensions")):
            if banned in text:
                err(f"{cls}.java {why} ({banned})")

def check_cinder_tyrant():
    """The Cinder Tyrant (tools/cinder_tyrant.py, docs/features/cinder-tyrant.md): every number the Java uses is the
    table's; his attacks are the table's, and his rule holds (blows on him do less than their damage while he is hot,
    more while quenched; a sluice's flood outlasts a Body Slam's wind-up and flight, so a turned sluice can catch one);
    where his fight looks for the Cinder Kiln's parts (CinderKiln.java) is where tools/cinder_kiln.py builds them, the
    sluices' wheels are the gates' own, and the places he waits sunk, crawls out to, roars from and sets his Cinderlings
    and the Grey Mist on are as the kiln is built (sunk in the crucible's slag with only his crest over it, the rest clear
    and on the floor); his gobs leave from where his model's jaws meet; every entity is registered, named and drawn;
    each GeckoLib body has its model, clips (every clip the Java names) and sheet, its box-UV regions inside the sheet
    without overlapping, its sheet and glowmask TEXTURE_SCALE times the size the model declares and cut out; no bone is
    animated by both of his controllers; his loot table is the table's and his trophies are Arms VII's; his recipes,
    tags and advancement are there; every message he sends has its words; and nothing of his loads a chunk or leaves
    its dimension."""
    import math
    import arms_variants
    import cinder_kiln as ck
    import cinder_tyrant as ct
    import cinder_tyrant_models as cm
    import lairs as la
    folder = JAVA_ROOT / "lair" / "tyrant"
    sources = {path.stem: path.read_text(encoding="utf-8") for path in folder.glob("*.java")}
    if not sources:
        err("lair/tyrant: no Java")
        return
    for cls, table in (("CinderTyrantEntity", ct.CINDER_TYRANT), ("CinderlingEntity", ct.CINDERLING),
                       ("MagmaGobEntity", ct.GOB), ("FallingCinderEntity", ct.CINDER)):
        for name, value in table.items():
            if not java_number(sources.get(cls, ""), name, value):
                err(f"{cls}.{name} is not tools/cinder_tyrant.py's {value}")
    boss = sources.get("CinderTyrantEntity", "")
    for attack, (windup, active, recovery, cooldown, near, far, kiln, eruption, heart) in ct.ATTACKS.items():
        expected = (f"{attack}({windup}, {active}, {recovery}, {cooldown}, {float(near)}, {float(far)}, "
                    f"{str(kiln).lower()}, {str(eruption).lower()}, {str(heart).lower()})")
        if expected not in boss:
            err(f"CinderTyrantEntity.Attack: expected {expected}")
    for call in ("LairBosses.damage(base)", "LairBosses.partyScale(players, PARTY_STEP, PARTY_MAX)"):
        if call not in boss:
            err(f"CinderTyrantEntity does not scale as every lair boss does ({call})")
    t = ct.CINDER_TYRANT
    if not t["HOT_TAKEN"] < 1.0 < t["QUENCH_TAKEN"]:
        err("tools/cinder_tyrant.py: blows must do less than their damage on him hot (HOT_TAKEN), more quenched (QUENCH_TAKEN)")
    if not 0.0 < t["HEART_AT"] < t["ERUPT_AT"] < 1.0:
        err("tools/cinder_tyrant.py: the Molten Heart (HEART_AT) must come after the Eruption (ERUPT_AT)")
    slam = ct.ATTACKS["BODY_SLAM"]
    if la.SLUICE["flood_ticks"] <= slam[0] + slam[1]:
        err("A sluice's flood must outlast a Body Slam's wind-up and flight, or no slam can be caught in a trough")
    for attack, phases in ((name, row[6:]) for name, row in ct.ATTACKS.items()):
        kiln_only_not = attack in ("CINDER_RAIN", "LAVA_WAVE")
        if phases != ((not kiln_only_not), True, True):
            err(f"tools/cinder_tyrant.py: {attack} is used in "
                f"{'the Eruption and the Molten Heart' if kiln_only_not else 'every phase'}, not {phases}")
    if cm.MOUTH_Z != -16 * ct.GOB["MOUTH_AHEAD"] or cm.MOUTH_Y != 16 * ct.GOB["MOUTH_UP"]:
        err("tools/cinder_tyrant.py GOB MOUTH_AHEAD and MOUTH_UP must be where tools/cinder_tyrant_models.py's jaws "
            f"meet: {-cm.MOUTH_Z / 16} and {cm.MOUTH_Y / 16}")
    # Where his fight looks for the Cinder Kiln's parts is where tools/cinder_kiln.py builds them.
    kiln = re.sub(r"\s+", " ", sources.get("CinderKiln", ""))
    for name, value in (("BOWL", ck.BOWL), ("BOWL_X", ck.BOWL_CENTRE[0]), ("BOWL_Z", ck.BOWL_CENTRE[1]),
                        ("BOWL_RADIUS", ck.BOWL_RADIUS), ("CRUCIBLE_X", ck.CRUCIBLE["centre"][0]),
                        ("CRUCIBLE_Z", ck.CRUCIBLE["centre"][1]), ("CRUCIBLE_RADIUS", ck.CRUCIBLE["radius"]),
                        ("CRUCIBLE_DEPTH", ck.CRUCIBLE["depth"]), ("RUN_WEST", ck.RUN[0]), ("RUN_EAST", ck.RUN[1]),
                        ("CHANNEL_NORTH", ck.CHANNEL[0]), ("CHANNEL_SOUTH", ck.CHANNEL[1]), ("LIP", ck.LIP),
                        ("SHELF_HALF", ck.SHELF_HALF)):
        if not java_number(kiln, name, value):
            err(f"CinderKiln.{name} is not {value} (tools/cinder_kiln.py)")
    shelves = ", ".join("new double[] {{{}, {}}}".format(*s) for s in ck.SHELVES)
    if f"SHELVES = List.of({shelves});" not in kiln:
        err(f"CinderKiln.SHELVES are not tools/cinder_kiln.py's: {shelves}")
    wheels = {sluice: pos for pos, (sluice, part) in ck.sluice_blocks().items() if part == "wheel"}
    expected_wheels = ", ".join("new BlockPos({}, {}, {})".format(*wheels[s]) for s in ("west", "east", "south"))
    if f"WHEELS = List.of({expected_wheels});" not in kiln:
        err(f"CinderKiln.WHEELS must be the sluices' wheels, west, east and south: {expected_wheels}")
    numbers = {}
    for name in ("LIP_Z", "SUNK_Y", "SUNK_Z", "OUT_Z"):
        found = re.search(rf"\bdouble {name} = (-?[0-9.]+);", kiln)
        if found is None:
            err(f"CinderKiln.{name} is missing")
            return
        numbers[name] = float(found.group(1))
    blocks = ck.build()
    cavern = ck.cavern()
    name = lambda pos: blocks[pos][0] if pos in blocks else None
    free = lambda pos: pos in cavern and name(pos) in (None, "minecraft:light")
    def clear(x, y, z, label, half=1, height=3, floors=("jugcraft:cracked_basalt", "minecraft:basalt")):
        """Room for him (half 1, three high) or a Cinderling (half 0, one high) standing at (x, y, z) in the template,
        on a floor of `floors`."""
        cx, cz = math.floor(x), math.floor(z)
        for dx in range(-half, half + 1):
            for dz in range(-half, half + 1):
                for dy in range(height):
                    pos = (cx + dx, int(y) + dy, cz + dz)
                    if not free(pos):
                        err(f"CinderKiln: {label} at ({x}, {y}, {z}) is not clear: {name(pos)} at {pos}")
                        return
                under = (cx + dx, int(y) - 1, cz + dz)
                if name(under) not in floors:
                    err(f"CinderKiln: {label} at ({x}, {y}, {z}) does not stand on {floors}: {name(under)} at {under}")
                    return
    lip_rows = range(ck.FORGE["lip"][0] + 1, ck.FORGE["lip"][1])
    if math.floor(numbers["LIP_Z"]) not in lip_rows:
        err(f"CinderKiln.LIP_Z {numbers['LIP_Z']} is not on the forge's lip, inside rows {ck.FORGE['lip']}")
    clear(ck.CRUCIBLE["centre"][0], ck.LIP + 1, numbers["LIP_Z"], "where he roars from on the lip",
          floors=("jugcraft:molten_slag", "jugcraft:kiln_brick"))
    clear(ck.CRUCIBLE["centre"][0], ck.BOWL + 1, numbers["OUT_Z"], "where he crawls out to")
    for west in (True, False):
        x = ck.RUN[0] - 1.5 if west else ck.RUN[1] + 2.5
        clear(x, ck.BOWL + 1, (ck.CHANNEL[0] + ck.CHANNEL[1]) / 2.0, "a bank a Cinderling crawls out on", half=0, height=1)
    if f"RUN_WEST - 1.5 : RUN_EAST + 2.5, BOWL + 1, (CHANNEL_NORTH + CHANNEL_SOUTH) / 2.0" not in kiln:
        err("CinderKiln.bank: the banks are not where the audit looks for them (a block and a half from either side of the "
            "channel, halfway along)")
    for x, z in ((ck.BOWL_CENTRE[0] - 1, ck.BOWL_CENTRE[1]), ck.BOWL_CENTRE):
        clear(x, ck.BOWL + 1, z, "the Grey Mist he leaves", half=0, height=2, floors=("jugcraft:cracked_basalt",))
    # Sunk, his feet and body are in the crucible's slag (its pool, never its rim or the basalt under), his back under its
    # surface (it is drawn a whole block high) and his crest over it.
    sx, sz = ck.CRUCIBLE["centre"][0], numbers["SUNK_Z"]
    geo = load(ASSETS / "geckolib" / "models" / "entity" / "cinder_tyrant.geo.json") or {}
    cubes = [cube for bone in (geo.get("minecraft:geometry") or [{}])[0].get("bones", []) for cube in bone.get("cubes", [])]
    if cubes:
        lo_x = min(c["origin"][0] for c in cubes) / 16
        hi_x = max(c["origin"][0] + c["size"][0] for c in cubes) / 16
        lo_z = min(c["origin"][2] for c in cubes) / 16
        hi_z = max(c["origin"][2] + c["size"][2] for c in cubes) / 16
        # Sunk he faces south: his model's -z (his jaws) is the world's +z.
        for x, z in ((sx + lo_x, sz - hi_z), (sx + hi_x, sz - hi_z), (sx + lo_x, sz - lo_z), (sx + hi_x, sz - lo_z)):
            if math.hypot(x - ck.CRUCIBLE["centre"][0], z - ck.CRUCIBLE["centre"][1]) > ck.CRUCIBLE["radius"]:
                err(f"Sunk, the Cinder Tyrant reaches ({x:.2f}, {z:.2f}), outside the crucible's pool")
    feet = (math.floor(sx), math.floor(numbers["SUNK_Y"]), math.floor(sz))
    if name(feet) != "jugcraft:molten_slag" or not ck.BOWL - ck.CRUCIBLE["depth"] < numbers["SUNK_Y"] < ck.BOWL:
        err(f"Sunk at y {numbers['SUNK_Y']}, the Cinder Tyrant's feet are not in the crucible's slag ({name(feet)})")
    if not numbers["SUNK_Y"] + cm.BACK_TOP / 16 < ck.BOWL + 14 / 16 < ck.BOWL + 1 < numbers["SUNK_Y"] + cm.CREST_TOP / 16:
        err("Sunk, the Cinder Tyrant's back must be under the slag and his crest over it "
            f"(back {numbers['SUNK_Y'] + cm.BACK_TOP / 16}, crest {numbers['SUNK_Y'] + cm.CREST_TOP / 16}, "
            f"slag {ck.BOWL + 1})")
    loot_java = sources.get("CinderTyrantLoot", "")
    if f'ADVANCEMENT = "{ct.ADVANCEMENT["key"]}";' not in loot_java:
        err("CinderTyrantLoot's advancement differs from tools/cinder_tyrant.py")
    registry = sources.get("JugcraftTyrant", "")
    client = (CLIENT_JAVA_ROOT / "CinderTyrantClient.java").read_text(encoding="utf-8")
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    for entity in ct.ENTITIES:
        if f'entity("{entity}"' not in registry:
            err(f"JugcraftTyrant does not register the {entity} entity")
        if f"entity.{MOD}.{entity}" not in lang:
            err(f"No name for the {entity} entity")
        if f"JugcraftTyrant.{entity.upper()}," not in client:
            err(f"CinderTyrantClient draws no {entity}")
    for item in ct.items():
        if f'"{item}"' not in registry:
            err(f"JugcraftTyrant does not register {item}")
    if "Lairs.onPlaced(Lair.CINDER_KILN, CinderTyrantEntity::summon)" not in registry:
        err("JugcraftTyrant must sink the Cinder Tyrant in every Cinder Kiln as it is placed")
    # The GeckoLib bodies.
    clips_named = {}
    for text in sources.values():
        for clip in re.findall(r'"(animation\.[a-z_]+\.[a-z_]+)"', text):
            clips_named.setdefault(clip.split(".")[1], set()).add(clip)
    # His body's clips are named from a prefix (CinderTyrantEntity's BODY table).
    for clip in re.findall(r'prefix \+ "([a-z_]+)"', boss):
        clips_named.setdefault("cinder_tyrant", set()).add(f"animation.cinder_tyrant.{clip}")
    for entity, body in ct.GECKO.items():
        geo = load(ASSETS / "geckolib" / "models" / "entity" / f"{entity}.geo.json") or {}
        animations = load(ASSETS / "geckolib" / "animations" / "entity" / f"{entity}.animation.json") or {}
        definition = (geo.get("minecraft:geometry") or [{}])[0]
        if not definition:
            err(f"{entity}: no GeckoLib model")
            continue
        clips = animations.get("animations", {})
        for clip in clips_named.get(body, set()):
            if clip not in clips:
                err(f"{entity}: the Java plays {clip}, which is not in {entity}.animation.json")
        for clip in cm.CLIPS[body]:
            if f"animation.{body}.{clip}" not in clips:
                err(f"{entity}.animation.json: missing animation.{body}.{clip}")
        width = definition.get("description", {}).get("texture_width", 0)
        height = definition.get("description", {}).get("texture_height", 0)
        bones = {bone["name"] for bone in definition.get("bones", [])}
        for clip in clips.values():
            for bone in clip.get("bones", {}):
                if bone not in bones:
                    err(f"{entity}.animation.json: animates unknown bone {bone}")
        regions = set()
        for bone in definition.get("bones", []):
            for cube in bone.get("cubes", []):
                w, h, d = cube["size"]
                u, v = cube["uv"]
                region = (u, v, u + 2 * (w + d), v + d + h)
                if region[2] > width or region[3] > height:
                    err(f"{entity}.geo.json: a cube in {bone['name']} maps outside the {width}x{height} sheet")
                regions.add(region)
        regions = sorted(regions)
        for i, a in enumerate(regions):
            for b in regions[i + 1:]:
                if a[0] < b[2] and b[0] < a[2] and a[1] < b[3] and b[1] < a[3]:
                    err(f"{entity}.geo.json: UV regions {a} and {b} overlap")
        # A body with a glow layer has a glowmask; one without has none (it would be a texture nothing draws).
        glows = entity in ct.GLOWING
        start = client.find(f"register(JugcraftTyrant.{entity.upper()},")
        end = client.find("EntityRendererRegistry.register(", start + 1)
        if start < 0 or ("AutoGlowingGeoLayer" in client[start:end if end >= 0 else len(client)]) != glows:
            err(f"CinderTyrantClient: the {entity}'s glow layer must match tools/cinder_tyrant.py GLOWING")
        if not glows and (ASSETS / "textures" / "entity" / f"{entity}_glowmask.png").is_file():
            err(f"textures/entity/{entity}_glowmask.png: the {entity} has no glow layer, so no glowmask")
        for name_ in (entity, f"{entity}_glowmask") if glows else (entity,):
            sheet = ASSETS / "textures" / "entity" / f"{name_}.png"
            if not sheet.is_file():
                err(f"{entity}: missing textures/entity/{name_}.png")
                continue
            with Image.open(sheet) as image:
                if image.size != (width * cm.TEXTURE_SCALE, height * cm.TEXTURE_SCALE):
                    err(f"textures/entity/{name_}.png must be {width * cm.TEXTURE_SCALE}x{height * cm.TEXTURE_SCALE}")
                if any(image.convert("RGBA").getchannel("A").histogram()[1:255]):
                    err(f"textures/entity/{name_}.png has half-transparent pixels (it is drawn cut out)")
    # His controllers own their bones: the body's clips never key his seams or his heart, nor the heat's a body bone.
    owned = {bone: controller for controller, bones in cm.CONTROLLERS.items() for bone in bones}
    for clip_name, clip in cm.ANIMATIONS["cinder_tyrant"]()["animations"].items():
        controller = "heat" if clip_name.split(".")[-1].startswith("heat_") else "body"
        for bone in clip.get("bones", {}):
            if owned.get(bone, "body") != controller:
                err(f"cinder_tyrant: {clip_name} ({controller}) animates {bone}, which the {owned.get(bone, 'body')} controller owns")
    for controller in cm.CONTROLLERS:
        if f'new AnimationController<CinderTyrantEntity>("{controller}"' not in boss:
            err(f"CinderTyrantEntity has no {controller} controller for tools/cinder_tyrant_models.py CONTROLLERS")
    # His loot, and his trophies: Arms VII's, as his bosses/cinder_tyrant table holds them.
    table = load(DATA / MOD / "loot_table" / "entities" / "cinder_tyrant.json") or {}
    if table.get("type") != "minecraft:gift":
        err("loot_table/entities/cinder_tyrant.json must be a gift table (rolled for each participant)")
    found_loot = {}
    for pool in table.get("pools", []):
        for entry in pool.get("entries", []):
            item = entry.get("name", "").split(":")[-1]
            chance = pool.get("condition", {}).get("chance")
            count = entry.get("modifier", {}).get("count", {})
            found_loot[item] = chance if chance is not None else (count.get("min"), count.get("max"))
    expected = {"tyrant_scale": ct.SCALES, **{item: ct.TROPHY_CHANCE for item in ct.TROPHIES}, **ct.CHANCES}
    if found_loot != expected:
        err(f"loot_table/entities/cinder_tyrant.json gives {found_loot}, not tools/cinder_tyrant.py's {expected}")
    if tuple(arms_variants.trophies("cinder_tyrant")) != tuple(ct.TROPHIES):
        err(f"tools/cinder_tyrant.py TROPHIES {ct.TROPHIES} are not Arms VII's: {arms_variants.trophies('cinder_tyrant')}")
    trophies = load(DATA / MOD / "loot_table" / "bosses" / "cinder_tyrant.json") or {}
    held = [entry.get("name") for pool in trophies.get("pools", []) for entry in pool.get("entries", [])]
    if held != [f"{MOD}:{item}" for item in ct.TROPHIES]:
        err(f"loot_table/bosses/cinder_tyrant.json holds {held}, not his trophies {ct.TROPHIES}")
    for table_id in ("entities/cinder_tyrant", "bosses/cinder_tyrant"):
        if f'Jugcraft.id("{table_id}")' not in loot_java:
            err(f"CinderTyrantLoot must roll loot_table/{table_id}.json")
    for trophy in ct.TROPHIES:
        if f'Jugcraft.id("{trophy}")' not in registry:
            err(f"JugcraftTyrant must find the {trophy} (Arms VII registers it)")
    for key in ct.RECIPES:
        if not (DATA / MOD / "recipe" / f"{key}.json").is_file():
            err(f"recipe/{key}.json is missing")
    costumes = json.dumps(load(DATA / MOD / "tags" / "item" / "trick_or_treat_costumes.json") or {})
    for item in ct.COSTUMES:
        if f"{MOD}:{item}" not in costumes:
            err(f"{item} must count as a costume (tags/item/trick_or_treat_costumes.json)")
    advancement = load(DATA / MOD / "advancement" / f"{ct.ADVANCEMENT['key']}.json") or {}
    if advancement.get("criteria", {}).get("done", {}).get("trigger") != "minecraft:impossible":
        err(f"advancement/{ct.ADVANCEMENT['key']}.json must be granted from code (an impossible \"done\")")
    for key in ct.MESSAGES:
        if f'"message.jugcraft.cinder_tyrant.{key}"' not in boss + loot_java:
            err(f"tools/cinder_tyrant.py MESSAGES has {key}, which the Java never sends")
    # The Salamander Charm wards off hot ground, decided on the server: the kiln's slag asks it, and so does the damage.
    charm = sources.get("SalamanderCharm", "")
    if "DamageTypes.HOT_FLOOR" not in charm or "ServerLivingEntityEvents.ALLOW_DAMAGE" not in charm:
        err("SalamanderCharm must turn aside hot-floor damage on the server (ServerLivingEntityEvents.ALLOW_DAMAGE)")
    slag = (JAVA_ROOT / "lair" / "MoltenSlagBlock.java").read_text(encoding="utf-8")
    if "SalamanderCharm.warded(living)" not in slag:
        err("MoltenSlagBlock must spare whoever holds a Salamander Charm")
    for cls, text in sources.items():
        for key in re.findall(r'"((?:message|tooltip|entity)\.jugcraft\.[a-z_.]+[a-z_])"', text):
            if key not in lang:
                err(f"{cls}.java sends {key}, which has no words in en_us.json")
        for banned, why in (("setChunkForced", "loads a chunk"), ("addRegionTicket", "loads a chunk"),
                            ("TicketType", "loads a chunk"), ("changeDimension", "crosses dimensions"),
                            ("TeleportTransition", "crosses dimensions")):
            if banned in text:
                err(f"{cls}.java {why} ({banned})")

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


def check_concordance(registered):
    """The Arcane Concordance (tools/concordance.py): Java mirrors its numbers; its spells avoid Spell Engine's
    null-default traps; its research graph resolves, has an entry with no prerequisites and no cycles; its codex,
    research bridge and opt-outs name things that exist."""
    co = concordance
    lang = load(ASSETS / "lang" / "en_us.json") or {}
    root = JAVA_ROOT / "concordance"
    constants = {
        "rules/FocusPool.java": {"MAX": co.FOCUS_MAX, "REGEN_TICKS": co.FOCUS_REGEN_TICKS},
        "Examination.java": {"DARK_LIGHT": co.DARK_LIGHT},
        "LumenMoteBlock.java": {"LIGHT": co.MOTE_LIGHT, "TRAIL_LIGHT": co.TRAIL_LIGHT, "STEP_TICKS": co.MOTE_STEP_TICKS,
                                "TRAIL_CHECK_TICKS": co.TRAIL_CHECK_TICKS},
        "KindledLanternItem.java": {"CAPACITY": co.LANTERN_CAPACITY, "BURN_TICKS": co.LANTERN_BURN_TICKS},
        "LampwrightBenchBlockEntity.java": {"STUDY_TICKS": co.STUDY_TICKS},
        "LumenSconceBlockEntity.java": {"BURN_TICKS": co.SCONCE_BURN_TICKS, "POUR": co.SCONCE_POUR,
                                        "RATE_LIMIT": co.SCONCE_RATE_LIMIT, "RATE_WINDOW": co.SCONCE_RATE_WINDOW},
        "ResearchNotesItem.java": {"COOLDOWN_TICKS": co.NOTES_COOLDOWN_TICKS},
        "ResearchNotes.java": {"MAX_ENTRIES": co.NOTES_MAX_ENTRIES},
    }
    for name, values in constants.items():
        path = root / name
        java = path.read_text(encoding="utf-8") if path.exists() else ""
        for const, value in values.items():
            if not re.search(rf"\bint {const} = {value};", java):
                err(f"concordance/{name}: {const} differs from tools/concordance.py ({value})")
    # Every Concordance item icon is its 16x16 map in tools/item_icons/, committed as drawn (docs/ITEM_ICONS.md).
    import item_icons
    for icon in ("initiate_wand", "adept_wand", "kindled_lantern", "kindled_lantern_lit", "research_notes",
                 "research_notes_written", "arcane_concordance"):
        texture = ASSETS / "textures" / "item" / f"{icon}.png"
        if not item_icons.has(icon) or not texture.is_file():
            err(f"concordance: {icon} needs its map tools/item_icons/{icon}.txt and its texture (tools/generate_textures.py)")
            continue
        with Image.open(texture) as img:
            if img.convert("RGBA").tobytes() != item_icons.draw(icon).tobytes() or img.size != (16, 16):
                err(f"concordance: textures/item/{icon}.png differs from its map: run tools/generate_textures.py")
    resource_java = (root / "resource" / "ResourceType.java").read_text(encoding="utf-8") \
        if (root / "resource" / "ResourceType.java").exists() else ""
    match = re.search(r"PRINCIPLES = Set\.of\(([^;]*)\);", resource_java)
    if not match or set(re.findall(r'"([a-z_]+)"', match.group(1))) != set(co.PRINCIPLES):
        err("concordance/resource/ResourceType.java: PRINCIPLES differs from tools/concordance.py")
    kinds_java = (root / "resource" / "ResourceKind.java").read_text(encoding="utf-8") \
        if (root / "resource" / "ResourceKind.java").exists() else ""
    if set(re.findall(r'^\t[A-Z_]+\("([a-z_]+)"', kinds_java, re.M)) != set(co.RESOURCES):
        err("concordance/resource/ResourceKind.java: resource ids differ from RESOURCES in tools/concordance.py")
    if not KINDLE_MOTE_STEPS_FIT(co):
        err("concordance: a Kindled mote's steps do not fit its age property (0..15)")

    # Spells: Spell Engine parses them with plain Gson, so a typo becomes null and crashes later.
    targets = {"NONE", "CASTER", "AIM", "BEAM", "AREA", "FROM_TRIGGER"}
    deliveries = {"DIRECT", "PROJECTILE", "METEOR", "CLOUD", "SHOOT_ARROW", "AFFECT_ARROW", "MELEE", "STASH_EFFECT",
                  "CUSTOM"}
    actions = {"DAMAGE", "HEAL", "STATUS_EFFECT", "FIRE", "SPAWN", "SUMMON", "TELEPORT", "COOLDOWN", "AGGRO", "DISRUPT",
               "IMMUNITY", "VELOCITY", "CUSTOM"}
    schools = {f"spell_power:{s}" for s in ("generic", "arcane", "fire", "frost", "healing", "lightning", "soul")}
    animations = set()
    for path in (ASSETS / "player_animations").glob("*.json"):
        clip = load(path) or {}
        if "animations" in clip:
            animations |= {f"{MOD}:{name}" for name in clip["animations"]}
        elif "name" in clip:
            animations.add(f"{MOD}:{clip['name'].lower().replace(' ', '_')}")
    sounds = load(ASSETS / "sounds.json") or {}
    concordance_java = (root / "JugcraftConcordance.java").read_text(encoding="utf-8") if (root / "JugcraftConcordance.java").exists() else ""
    registered_sounds = set(re.findall(r'sound\("([a-z_.]+)"\)', concordance_java))
    loop = re.search(r'for \(String release : List\.of\(([^)]*)\)\)', concordance_java)
    if loop:
        registered_sounds |= {f"concordance.{name}" for name in re.findall(r'"([a-z_]+)"', loop.group(1))}
    spells = set()
    for path in sorted((DATA / MOD / "spell").glob("*.json")):
        spell = load(path) or {}
        name = path.stem
        spells.add(name)
        if spell.get("school") not in schools:
            err(f"spell {name}: unknown school {spell.get('school')}")
        target = spell.get("target", {}).get("type", "CASTER")
        if target not in targets:
            err(f"spell {name}: unknown target {target}")
        if target == "AIM" and "aim" not in spell["target"] or target == "AREA" and "area" not in spell["target"]:
            err(f"spell {name}: target {target} needs its own object")
        if spell.get("deliver", {}).get("type", "DIRECT") not in deliveries:
            err(f"spell {name}: unknown delivery")
        for impact in spell.get("impacts", []):
            action = impact.get("action", {})
            if action.get("type") not in actions:
                err(f"spell {name}: unknown impact {action.get('type')}")
            if action.get("type") == "CUSTOM" and not action.get("custom", {}).get("handler"):
                err(f"spell {name}: a CUSTOM impact needs custom.handler")
        cast = spell.get("active", {}).get("cast", {})
        if cast.get("duration", 0) > 0 and not cast.get("animation", {}).get("id"):
            err(f"spell {name}: a timed cast needs an animation (Spell Engine's client reads it every tick)")
        for clip in (cast.get("animation", {}).get("id"), spell.get("release", {}).get("animation", {}).get("id")):
            if clip and clip.startswith(f"{MOD}:") and clip not in animations:
                err(f"spell {name}: unknown player animation {clip}")
        for sound in (cast.get("start_sound", {}).get("id"), spell.get("release", {}).get("sound", {}).get("id")):
            if sound and sound.startswith(f"{MOD}:") and sound.split(":", 1)[1] not in sounds:
                err(f"spell {name}: unknown sound {sound}")
            # Spell Engine plays a spell's sounds on the server by looking them up in the sound registry.
            if sound and sound.startswith(f"{MOD}:") and sound.split(":", 1)[1] not in registered_sounds:
                err(f"spell {name}: sound {sound} is not registered as a SoundEvent in JugcraftConcordance.java")
        if "learn" in spell:
            err(f"spell {name}: a Concordance spell must not be bindable at a Spell Binding Table")
        cost = spell.get("cost", {})
        if cost.get("durability", 1) != 0 or cost.get("cooldown", {}).get("hosting_item", True):
            err(f"spell {name}: set cost.durability 0 and cost.cooldown.hosting_item false")
        for key in ("name", "description"):
            text = lang.get(f"spell.{MOD}.{name}.{key}")
            if text is None:
                err(f"spell {name}: missing lang spell.{MOD}.{name}.{key}")
            elif re.search(r"%(?!%)", text.replace("%%", "")):
                err(f"spell {name}: Spell Engine formats its {key}; escape % as %%")
        if not (ASSETS / "textures" / "spell" / f"{name}.png").is_file():
            err(f"spell {name}: missing icon textures/spell/{name}.png")

    # Research, invocations and workings.
    research = {path.stem: load(path) or {} for path in (DATA / MOD / "concordance" / "research").glob("*.json")}
    conversions = {path.stem: load(path) or {} for path in (DATA / MOD / "concordance" / "conversion").glob("*.json")}
    resource_ids = {f"essence/{p}" for p in co.PRINCIPLES} | {r for r in co.RESOURCES if r not in ("essence", "bound_will")}
    ratios = {}
    for key, entry in conversions.items():
        source, result = entry.get("from", {}), entry.get("to", {})
        if entry.get("schema") != co.SCHEMA or source.get("resource") not in resource_ids \
                or result.get("resource") not in resource_ids or source.get("resource") == result.get("resource"):
            err(f"conversion {key}: needs schema {co.SCHEMA} and two different amount resources ({sorted(resource_ids)})")
            continue
        if not (isinstance(source.get("amount"), int) and isinstance(result.get("amount"), int)
                and source["amount"] >= 1 and result["amount"] >= 1):
            err(f"conversion {key}: amounts must be whole numbers of at least 1")
            continue
        edge = (source["resource"], result["resource"])
        ratios[edge] = max(ratios.get(edge, 0), result["amount"] / source["amount"])
    # No loop of conversions may come back with as much as it started with (Java: ConversionTable).
    nodes = sorted({n for edge in ratios for n in edge})
    best = {(a, b): ratios.get((a, b), 0.0) for a in nodes for b in nodes}
    for k in nodes:
        for a in nodes:
            for b in nodes:
                best[a, b] = max(best[a, b], best[a, k] * best[k, b])
    for n in nodes:
        if best[n, n] >= 1 - 1e-9:
            err(f"conversions: {n} converts round without loss")
    invocations = {path.stem: load(path) or {} for path in (DATA / MOD / "concordance" / "invocation").glob("*.json")}
    workings = {path.stem: load(path) or {} for path in (DATA / MOD / "concordance" / "working").glob("*.json")}
    def known_item(ref):
        ns, path = split(ref)
        return ns != MOD or path in registered
    def resolves(ref):
        return tag_exists("item", ref[1:]) if ref.startswith("#") else known_item(ref)
    for key, entry in research.items():
        if entry.get("schema") != co.SCHEMA:
            err(f"research {key}: schema {entry.get('schema')}")
        if entry.get("principle") not in co.PRINCIPLES or entry.get("tradition") not in co.TRADITIONS \
                or entry.get("stage") not in co.STAGES:
            err(f"research {key}: unknown principle, tradition or stage")
        states = list(entry.get("states", {}))
        if states != co.RESEARCH_STAGES[:len(states)] or not states:
            err(f"research {key}: states must run in order from encountered ({states})")
        for state, block in entry.get("states", {}).items():
            for rule in block.get("any", []):
                kind = rule.get("type")
                if kind in ("examine", "study"):
                    if not resolves(rule.get("specimens", "")):
                        err(f"research {key}/{state}: unknown specimens {rule.get('specimens')}")
                elif kind == "invoke":
                    if split(rule.get("invocation", ":"))[1] not in invocations:
                        err(f"research {key}/{state}: unknown invocation {rule.get('invocation')}")
                elif kind == "notes":
                    if state == "mastered":
                        err(f"research {key}: notes cannot stand for mastery")
                elif kind == "practice":
                    if not re.fullmatch(r"[a-z0-9_.-]+:[a-z0-9_./-]+", rule.get("activity", "")):
                        err(f"research {key}/{state}: practice needs an activity id")
                else:
                    err(f"research {key}/{state}: unknown evidence {kind}")
        for requirement in entry.get("requires", []):
            if split(requirement.get("research", ":"))[1] not in research:
                err(f"research {key}: requires unknown {requirement}")
        for state, unlocks in entry.get("unlocks", {}).items():
            for ref in unlocks.get("invocations", []):
                if split(ref)[1] not in invocations:
                    err(f"research {key}: unlocks unknown invocation {ref}")
            for ref in unlocks.get("workings", []):
                if split(ref)[1] not in workings:
                    err(f"research {key}: unlocks unknown working {ref}")
            for ref in unlocks.get("rituals", []):
                if not (DATA / MOD / "concordance" / "ritual" / f"{split(ref)[1]}.json").is_file():
                    err(f"research {key}: unlocks unknown ritual {ref}")
    if research and not any(not entry.get("requires") for entry in research.values()):
        err("research: no entry can be started without another (no entry path)")
    graph = {key: [split(r["research"])[1] for r in entry.get("requires", [])] for key, entry in research.items()}
    state = {}
    def visit(node):
        if state.get(node) == 1:
            err(f"research: prerequisite cycle through {node}")
            return
        if state.get(node) == 2:
            return
        state[node] = 1
        for nxt in graph.get(node, []):
            visit(nxt)
        state[node] = 2
    for node in graph:
        visit(node)
    for key, entry in invocations.items():
        if key not in spells:
            err(f"invocation {key}: no spell data/{MOD}/spell/{key}.json")
        if split(entry.get("research", ":"))[1] not in research or entry.get("stage") not in co.RESEARCH_STAGES:
            err(f"invocation {key}: unknown research or stage")
        if not 0 < entry.get("mastered_focus", 0) <= entry.get("focus", 0) <= co.FOCUS_MAX:
            err(f"invocation {key}: Focus costs must be 0 < mastered <= focus <= {co.FOCUS_MAX}")
    for key, entry in workings.items():
        if split(entry.get("research", ":"))[1] not in research:
            err(f"working {key}: unknown research")
        refs = [entry.get("work", "")] + list(entry.get("specimens", {})) + [entry.get(k) for k in ("specimen", "result") if entry.get(k)]
        for ref in refs:
            if not known_item(ref):
                err(f"working {key}: unknown item {ref}")
        if entry.get("type") == "channel":
            conversion = conversions.get(split(entry.get("conversion", ":"))[1], {})
            source, result = conversion.get("from", {}), conversion.get("to", {})
            if source.get("resource") != "focus" or result.get("resource") != "essence/radiance":
                err(f"working {key}: channels by {entry.get('conversion')}, which must turn focus into essence/radiance")
            elif result.get("amount", 0) >= source.get("amount", 0):
                err(f"working {key}: channelling must lose Focus (no free conversion)")
        if entry.get("type") not in ("craft", "infuse", "channel"):
            err(f"working {key}: unknown type {entry.get('type')}")

    # The codex and its research bridge (Modonomicon refuses the whole reload over one bad research id).
    research_dir = DATA / MOD / "modonomicon" / "research" / "concordance"
    facts = {f["id"] for f in load(research_dir / "facts.json") or []}
    nodes = {n["id"] for n in load(research_dir / "nodes.json") or []}
    advancement_names = {path.stem for path in (DATA / MOD / "advancement").glob("*.json")}
    for node in load(research_dir / "nodes.json") or []:
        for fact in node.get("required_facts", []):
            if fact not in facts:
                err(f"codex research: node {node['id']} needs unknown fact {fact}")
    for hook in load(research_dir / "hooks.json") or []:
        if hook.get("fact_id") not in facts:
            err(f"codex research: hook {hook.get('id')} grants unknown fact")
        if split(hook["trigger_target"])[1] not in advancement_names:
            err(f"codex research: hook {hook.get('id')} waits on unknown advancement {hook['trigger_target']}")
    book = DATA / MOD / "modonomicon" / "books" / co.BOOK
    for path in sorted(book.rglob("*.json")):
        text = path.read_text(encoding="utf-8")
        # An entry or category icon must be a real item: Modonomicon refuses to open a book with an empty icon.
        for icon in re.findall(r'"icon": "([^"]+)"', text):
            if split(icon)[0] == MOD and split(icon)[1] not in registered:
                err(f"codex {path.relative_to(book)}: icon {icon} is not a Jugcraft item")
        for node in re.findall(r'"node_id": "([^"]+)"', text):
            if node not in nodes:
                err(f"codex {path.relative_to(book)}: unknown research node {node}")
        for recipe in re.findall(r'"recipe_id_1": "([^"]+)"', text):
            if not (DATA / split(recipe)[0] / "recipe" / f"{split(recipe)[1]}.json").is_file():
                err(f"codex {path.relative_to(book)}: unknown recipe {recipe}")
        for key in re.findall(r'"(book\.[a-z_.0-9]+|research_node\.[a-z_.0-9]+)"', text):
            if key not in lang:
                err(f"codex {path.relative_to(book)}: missing lang {key}")
    # Opt-outs name real items; the Kindled mote has its block state and name; LambDynamicLights names a real item.
    for path in (DATA / MOD / "spell_assignments").glob("*.json"):
        if path.stem not in registered:
            err(f"spell_assignments/{path.name}: not a Jugcraft item")
    for block in co.itemless_blocks():
        if load(ASSETS / "blockstates" / f"{block}.json") is None or f"block.{MOD}.{block}" not in lang:
            err(f"concordance: {block} needs a block state and a name")
    lights = load(ASSETS / "dynamiclights" / "item" / "kindled_lantern.json") or {}
    if split(lights.get("match", {}).get("items", ":"))[1] not in registered:
        err("dynamiclights/item/kindled_lantern.json: unknown item")
    for item in co.INSTRUMENTS:
        resolver = load(DATA / MOD / "spell_assignments" / f"{item}.json") or {}
        if resolver.get("access") != "TAG" or resolver.get("access_param") != co.SPELL_TAG:
            err(f"spell_assignments/{item}.json: an instrument must resolve the {co.SPELL_TAG} spell tag")
    # Every whole translation key the Concordance's Java names exists (keys built from a prefix are checked above).
    sources = sorted(root.rglob("*.java")) + sorted(CLIENT_JAVA_ROOT.glob("Concordance*.java")) \
        + sorted(CLIENT_JAVA_ROOT.glob("LampwrightBench*.java"))
    for path in sources:
        for key in re.findall(r'"((?:message|screen|tooltip|container|key)\.jugcraft\.[a-z_.]+[a-z_])"', path.read_text(encoding="utf-8")):
            if key not in lang:
                err(f"{path.name}: missing lang {key}")
    sconce = (root / "LumenSconceBlock.java").read_text(encoding="utf-8") if (root / "LumenSconceBlock.java").exists() else ""
    body = sconce[sconce.find("private static Component message("):]
    body = body[:body.find("\n\t}\n")]
    for key in re.findall(r'"([a-z_]+)"', body):
        if f"message.{MOD}.concordance.sconce.{key}" not in lang:
            err(f"LumenSconceBlock.java: missing lang message.{MOD}.concordance.sconce.{key}")
    status = (root / "BenchStatus.java").read_text(encoding="utf-8") if (root / "BenchStatus.java").exists() else ""
    for key in re.findall(r'[A-Z_]+\("([a-z_]+)"\)', status):
        if f"message.{MOD}.concordance.bench.{key}" not in lang:
            err(f"BenchStatus.java: missing lang message.{MOD}.concordance.bench.{key}")
    components = (root / "JugcraftConcordance.java").read_text(encoding="utf-8") if (root / "JugcraftConcordance.java").exists() else ""
    for component in lights.get("match", {}).get("components", {}):
        if f'"{split(component)[1]}"' not in components:
            err(f"dynamiclights/item/kindled_lantern.json: component {component} is not registered in JugcraftConcordance")
    check_composition(co, root, lang, registered, research)
    check_invocations(co, root, lang, research)
    check_ember(co, root, lang, research)
    check_wayfaring(co, root, lang)
    check_baselines(root)
    check_rituals(co, root, lang, registered, research)
    check_alchemy(co, root, lang, registered, research)
    check_ecology(co, root, lang, registered, research)
    check_celestial(co, root, lang, registered, research)
    check_crimson(co, root, lang, registered, research)
    check_workers(co, root, lang, registered, research)
    check_logistics(co, root, lang, registered)
    check_artifice(co, root, lang, registered, research)
    check_relics(co, root, lang, registered, research)
    check_equivalence(co, root, lang, registered, research)
    check_sympathy(co, root, lang, registered, research)
    check_conclave(co, root, lang, registered, research)
    check_progression(co, root, lang, registered, research)
    check_spire(co, root, lang, registered, research)
    check_journal(co, root, lang)
    check_signs(co, root, lang)
    check_authority(root, lang)
    check_economy(root)
    check_persistence(root)
    check_journey()
    check_delivery()
    check_game_test_entrypoints()


def check_composition(co, root, lang, registered, research):
    """Roadmap steps 8 and 9: the grammar's limits and effect kinds match the Java; every component and instrument is
    within those limits and learnt from research that exists; the codex examples fit the instrument; every text the
    compiler can show exists; and Concordance spells deliver their effects only through Jugcraft's own CUSTOM impacts
    (the one effect boundary), never Spell Engine's damage or healing (which would apply effects twice)."""
    def java(name):
        path = root / name
        return path.read_text(encoding="utf-8") if path.exists() else ""
    for name, values in (("compose/Grammar.java", co.COMPOSE_LIMITS), ("effect/EffectSpec.java", co.EFFECT_LIMITS),
                         ("ConcordanceCommand.java", {"COMPOSE_RATE_TICKS": co.COMPOSE_RATE_TICKS})):
        text = java(name)
        for const, value in values.items():
            if not re.search(rf"\bint {const} = {value};", text):
                err(f"concordance/{name}: {const} differs from tools/concordance.py ({value})")
    kinds = {m[1]: (m[2].lower(), int(m[3])) for m in
             re.findall(r'^\t([A-Z_]+)\("([a-z_]+)", On\.([A-Z]+), (\d+)\)', java("effect/EffectKind.java"), re.M)}
    if kinds != {k: (v["on"], v["work"]) for k, v in co.EFFECT_KINDS.items()}:
        err("concordance/effect/EffectKind.java: kinds, targets or work differ from EFFECT_KINDS in tools/concordance.py")
    limits = co.COMPOSE_LIMITS
    states = set(co.RESEARCH_STAGES)
    for key, info in co.COMPONENTS.items():
        where = f"component {key}"
        data = load(DATA / MOD / "concordance" / "component" / f"{key}.json")
        if data != co.component_json(info):
            err(f"{where}: data file differs from tools/concordance.py (regenerate)")
        requires = info.get("requires", {})
        if split(requires.get("research", ":"))[1] not in research or requires.get("state") not in states:
            err(f"{where}: learnt from unknown research {requires}")
        if not (0 <= info["capacity"] <= limits["MAX_COMPONENT_COST"] and 0 <= info["focus"] <= limits["MAX_COMPONENT_COST"]):
            err(f"{where}: capacity and Focus must be 0..{limits['MAX_COMPONENT_COST']}")
        slot = info["slot"]
        body = info.get(slot)
        if slot not in ("delivery", "selection", "operation", "modifier", "termination") or not isinstance(body, dict):
            err(f"{where}: slot {slot} needs its own object")
            continue
        if slot == "delivery" and not (body["form"] in ("here", "touch", "ray")
                                       and (body["range"] == 0) == (body["form"] == "here") and body["range"] <= limits["MAX_RANGE"]):
            err(f"{where}: delivery form or range out of the grammar")
        if slot == "selection" and not (body["pick"] in ("struck", "creatures", "blocks", "allies")
                                        and ((body["radius"], body["targets"]) == (0, 1) if body["pick"] == "struck"
                                             else 1 <= body["radius"] <= limits["MAX_RADIUS"] and 1 <= body["targets"] <= limits["MAX_TARGETS"])):
            err(f"{where}: selection out of the grammar")
        if slot == "operation":
            effect = body.get("effect")
            if effect not in co.EFFECT_KINDS or body.get("intent") not in ("helpful", "harmful") or body.get("principle") not in co.PRINCIPLES:
                err(f"{where}: unknown effect, intent or Principle")
            if effect == "damage" and body.get("school") != co.PRINCIPLES[body["principle"]]["school"]:
                err(f"{where}: damage must use its Principle's Spell Power school")
            if (effect == "status") != ("status" in body):
                err(f"{where}: a status effect (and only a status effect) names a status")
            if body.get("magnitude", 0) > co.EFFECT_LIMITS["MAX_MAGNITUDE"] or body.get("duration", 0) > co.EFFECT_LIMITS["MAX_DURATION"]:
                err(f"{where}: beyond the effect limits")
            if effect == "illumination" and body.get("duration", 0) > co.MOTE_STEP_TICKS * 15:
                err(f"{where}: light cannot last longer than a mote's 15 steps")
            if "scaling" in body and not (effect == "damage" and body.get("school")
                                          and 0 <= body["scaling"] <= co.INVOCATION_SCALING_MAX):
                err(f"{where}: only damage naming its school scales with Spell Power, by 0..{co.INVOCATION_SCALING_MAX}")
        if info.get("authored") and slot == "modifier":
            err(f"{where}: a modifier cannot be authored (tunings are how players change invocations)")
        if slot == "modifier" and not (body["aspect"] in ("magnitude", "duration", "radius", "range")
                                       and 1 <= body["amount"] <= limits["MAX_MODIFIER_AMOUNT"]):
            err(f"{where}: modifier out of the grammar")
        if slot == "termination" and not (1 <= body["pulses"] <= limits["MAX_PULSES"] and (
                body["interval"] == 0 if body["pulses"] == 1 else limits["MIN_INTERVAL"] <= body["interval"] <= limits["MAX_INTERVAL"])):
            err(f"{where}: termination out of the grammar")
        for lang_key in (f"component.{MOD}.{key}", f"component.{MOD}.{key}.description"):
            if lang_key not in lang:
                err(f"{where}: missing lang {lang_key}")
    for path in sorted((DATA / MOD / "concordance" / "component").glob("*.json")):
        if path.stem not in co.COMPONENTS:
            err(f"concordance/component/{path.name}: not in tools/concordance.py (stale file)")
    for key, info in co.INSTRUMENT_LIMITS.items():
        if split(info["item"])[1] not in registered or split(info["item"])[1] not in co.INSTRUMENTS:
            err(f"instrument {key}: {info['item']} is not a Jugcraft instrument")
        for field, cap in (("capacity", "MAX_CAPACITY"), ("targets", "MAX_TARGETS"), ("work", "MAX_WORK"), ("branches", "MAX_BRANCHES")):
            if not 0 <= info[field] <= limits[cap]:
                err(f"instrument {key}: {field} beyond the grammar's {cap}")
        if not 0 <= info["duration"] <= co.EFFECT_LIMITS["MAX_DURATION"]:
            err(f"instrument {key}: duration beyond the effect limit")
    wand = co.INSTRUMENT_LIMITS["initiate_wand"]
    for text in co.COMPOSE_EXAMPLES:
        cost = co.composition_cost(text)
        if cost["capacity"] > wand["capacity"] or cost["targets"] > wand["targets"] or cost["work"] > wand["work"] \
                or cost["focus"] > co.FOCUS_MAX or text.count(" then ") > wand["branches"]:
            err(f"codex example '{text}' does not fit the Initiate's Wand: {cost}")
    # Every text the compiler can produce, and every name it refers to, exists.
    keys = set()
    for path in sorted((root / "compose").glob("*.java")):
        keys |= set(re.findall(r'Text\.of\("([a-z_.]+)"', path.read_text(encoding="utf-8")))
    keys |= {f"explain.operation.{kind}" for kind in co.EFFECT_KINDS}
    keys |= {f"slot.{slot}" for slot in ("delivery", "selection", "operation", "modifier", "termination")}
    keys |= {f"aspect.{aspect}" for aspect in ("magnitude", "duration", "radius", "range")} | {"on.creature", "on.block"}
    for key in sorted(keys):
        if f"compose.{MOD}.{key}" not in lang:
            err(f"concordance/compose: missing lang compose.{MOD}.{key}")
    sources = sorted(root.rglob("*.java")) + sorted(CLIENT_JAVA_ROOT.glob("Concordance*.java"))
    for path in sources:
        for key in re.findall(r'"(compose\.jugcraft\.[a-z_.]+[a-z_])"', path.read_text(encoding="utf-8")):
            if key not in lang:
                err(f"{path.name}: missing lang {key}")
    # One effect boundary: Concordance spells use only Jugcraft's registered CUSTOM impacts.
    handlers = set()
    for path in sorted(root.glob("*.java")):
        handlers |= set(re.findall(r'registerCustomImpact\(Jugcraft\.id\("([a-z_]+)"\)', path.read_text(encoding="utf-8")))
    tagged = (load(DATA / MOD / "tags" / "spell" / "concordance.json") or {}).get("values", [])
    for spell_id in tagged:
        spell = load(DATA / MOD / "spell" / f"{split(spell_id)[1]}.json")
        if spell is None:
            err(f"spell tag {co.SPELL_TAG}: {spell_id} has no spell file")
            continue
        for impact in spell.get("impacts", []):
            action = impact.get("action", {})
            handler = action.get("custom", {}).get("handler", "")
            if action.get("type") != "CUSTOM" or split(handler)[0] != MOD or split(handler)[1] not in handlers:
                err(f"spell {spell_id}: Concordance spells act only through Jugcraft's registered CUSTOM impacts "
                    f"(found {action.get('type')} {handler})")
        if not (ASSETS / "textures" / "spell" / f"{split(spell_id)[1]}.png").is_file():
            err(f"spell {spell_id}: no icon textures/spell/{split(spell_id)[1]}.png")


def invocation_signature(text):
    """What a composition is regardless of its numbers (Java: Plan.signature): per level, the delivery form, the pick
    and the sorted effect/intent pairs."""
    co = concordance
    levels = []
    for level in text.split(" then "):
        words = [w.split("+")[0] for w in level.split()]
        form = co.COMPONENTS[words[0]]["delivery"]["form"]
        pick = co.COMPONENTS[words[1]]["selection"]["pick"]
        kinds = sorted(f"{co.COMPONENTS[w]['operation']['effect']}/{co.COMPONENTS[w]['operation']['intent']}"
                       for w in words if co.COMPONENTS[w]["slot"] == "operation")
        levels.append(f"{form}/{pick}:{','.join(kinds)}")
    return " then ".join(levels)


def check_invocations(co, root, lang, research):
    """Roadmap step 10: every invocation is a composition in the shared grammar that fits the Initiate's Wand with
    each of its tunings; it costs at least what its composition would (mastery taking off at most a quarter) and its
    cooldown is at least the composition's; its declared work and persistence cover every form; no two do the same
    thing; the six roles are covered; authored words are used only by invocations and every one is used; and each has
    its spell, handler, gestures, sounds, particles, icon, codex entry and texts."""
    def java(name):
        path = root / name
        return path.read_text(encoding="utf-8") if path.exists() else ""
    wand = co.INSTRUMENT_LIMITS["initiate_wand"]
    role_enum = re.search(r"public enum Role \{(.*?)\n\t\t[a-z]", java("rules/Definitions.java"), re.S)
    roles_java = set(re.findall(r'^\t\t[A-Z_]+\("([a-z_]+)"\)', role_enum.group(1) if role_enum else "", re.M))
    if roles_java != set(co.INVOCATION_ROLES):
        err(f"concordance/rules/Definitions.java: Role ids {sorted(roles_java)} differ from INVOCATION_ROLES")
    for name, const, value in (("rules/RulesParser.java", "MAX_TUNINGS", co.INVOCATION_MAX_TUNINGS),
                               ("Tunings.java", "MAX", 16)):
        if not re.search(rf"\bint {const} = {value};", java(name)):
            err(f"concordance/{name}: {const} differs from {value}")
    if not re.search(rf"double MAX_SCALING = {co.INVOCATION_SCALING_MAX};", java("rules/RulesParser.java")):
        err("concordance/rules/RulesParser.java: MAX_SCALING differs from INVOCATION_SCALING_MAX")
    if len(co.INVOCATIONS) > 16:
        err("invocations: more than an instrument's Tunings.MAX can tune")
    if 'registerCustomImpact(Jugcraft.id("invocation")' not in java("Invocations.java"):
        err("concordance/Invocations.java: must register the CUSTOM impact jugcraft:invocation")
    if set(info["role"] for info in co.INVOCATIONS.values()) != set(co.INVOCATION_ROLES):
        err(f"invocations: roles {sorted(set(i['role'] for i in co.INVOCATIONS.values()))} do not cover all six")
    used = set()
    signatures = {}
    codex_entries = {entry for (category, entry) in co.codex() if category == "invocations"}
    for key, info in co.INVOCATIONS.items():
        where = f"invocation {key}"
        data = load(DATA / MOD / "concordance" / "invocation" / f"{key}.json")
        if data != co.invocation_json(key, info):
            err(f"{where}: data file differs from tools/concordance.py (regenerate)")
        words = [w.split("+")[0] for w in info["composition"].replace(" then ", " ").split()]
        if any(w not in co.COMPONENTS for w in words):
            err(f"{where}: unknown words in {info['composition']}")
            continue
        used |= set(words)
        if len(info["tunings"]) > co.INVOCATION_MAX_TUNINGS:
            err(f"{where}: at most {co.INVOCATION_MAX_TUNINGS} tunings")
        for tuning in info["tunings"]:
            component = co.COMPONENTS.get(tuning, {})
            if component.get("slot") != "modifier" or component.get("authored"):
                err(f"{where}: tuning {tuning} is not a players' modifier")
        forms = co.invocation_forms(key)
        base_text, base, base_numbers = forms[None]
        least = base["focus"]
        if not (info["focus"] >= least and info["mastered_focus"] >= least - (least + 3) // 4):
            err(f"{where}: costs less than its composition ({least} Focus; mastery may take off {(least + 3) // 4})")
        if round(info["cooldown"] * 20) < base["cooldown"]:
            err(f"{where}: cooldown {info['cooldown']} s is shorter than its composition's {base['cooldown']} ticks")
        for tuning, (text, cost, numbers) in forms.items():
            label = f"{where}{'' if tuning is None else ' + ' + tuning}"
            if text is None:
                err(f"{label}: the tuning changes nothing in it")
                continue
            if cost["capacity"] > wand["capacity"] or cost["targets"] > wand["targets"] or cost["work"] > wand["work"]:
                err(f"{label}: does not fit the Initiate's Wand: {cost}")
            if cost["work"] > info["work"] or co.persistence(numbers) > info["persists"]:
                err(f"{label}: spends {cost['work']} work and lasts {co.persistence(numbers)} ticks; it declares "
                    f"{info['work']} and {info['persists']}")
            if co.persistence(numbers) > wand["duration"]:
                err(f"{label}: lasts longer than the wand allows")
            if invocation_signature(text) != invocation_signature(base_text):
                err(f"{label}: a tuning changed what it does")
        signature = invocation_signature(base_text)
        if signature in signatures:
            err(f"{where}: does what {signatures[signature]} does ({signature})")
        signatures[signature] = key
        harmful = [w for w in words if co.COMPONENTS[w]["slot"] == "operation"
                   and co.COMPONENTS[w]["operation"]["intent"] == "harmful"]
        if harmful and any(co.COMPONENTS[w].get("selection", {}).get("pick") == "allies" for w in words):
            err(f"{where}: harms allies")
        spell = load(DATA / MOD / "spell" / f"{key}.json") or {}
        if spell != co.spell_json(key):
            err(f"{where}: spell file differs from tools/concordance.py (regenerate)")
        handlers = [i.get("action", {}).get("custom", {}).get("handler") for i in spell.get("impacts", [])]
        if handlers != [f"{MOD}:invocation"]:
            err(f"{where}: its spell must have the one CUSTOM impact {MOD}:invocation (found {handlers})")
        if spell.get("range") != float(co.invocation_range(info)):
            err(f"{where}: spell range differs from its delivery")
        if info["particle"] not in co.INVOCATION_PARTICLES:
            err(f"{where}: particle {info['particle']} is not in INVOCATION_PARTICLES")
        if not spell.get("release", {}).get("animation") or not spell.get("release", {}).get("sound"):
            err(f"{where}: needs a release gesture and sound")
        if split(info["research"])[1] not in research or info["stage"] not in ("understood", "mastered"):
            err(f"{where}: learnt from unknown research or stage")
        unlocks = (research.get(split(info["research"])[1], {}).get("unlocks", {}).get(info["stage"], {})
                   .get("invocations", []))
        if f"{MOD}:{key}" not in unlocks:
            err(f"{where}: {info['research']} {info['stage']} does not unlock it")
        if key not in codex_entries:
            err(f"{where}: no codex entry")
        for lang_key in (f"message.{MOD}.concordance.fizzle.{key}", f"role.{MOD}.{info['role']}"):
            if lang_key not in lang:
                err(f"{where}: missing lang {lang_key}")
    for key, info in co.COMPONENTS.items():
        if info.get("authored") and key not in used:
            err(f"component {key}: authored but no invocation uses it")
    # Kindle's numbers in the generator come from its data.
    kindle = co.form_numbers(co.INVOCATIONS["kindle"]["composition"])
    if kindle["range"] != co.KINDLE_RANGE or kindle["operations"][0]["duration"] != co.KINDLE_MOTE_STEPS * co.MOTE_STEP_TICKS:
        err("invocation kindle: KINDLE_RANGE or KINDLE_MOTE_STEPS differ from its composition")
    # Every message key the invocation code names exists.
    for name in ("Invocations.java", "ConcordanceCommand.java", "ConcordanceSpells.java", "ComposedSpells.java"):
        for key in re.findall(r'"(message\.jugcraft\.concordance\.[a-z_.]+[a-z_])"', java(name)):
            if key not in lang:
                err(f"concordance/{name}: missing lang {key}")
        for key in re.findall(r'"(tooltip\.jugcraft\.concordance\.[a-z_.]+[a-z_])"', java(name)):
            if key not in lang:
                err(f"concordance/{name}: missing lang {key}")


def check_game_test_entrypoints():
    """Every game test class is registered, or its tests silently never run (Fabric finds them only through the
    gametest source set's fabric.mod.json entrypoints)."""
    mod = load(ROOT / "src" / "gametest" / "resources" / "fabric.mod.json") or {}
    listed = {cls for classes in mod.get("entrypoints", {}).values() for cls in classes}
    for path in sorted((ROOT / "src" / "gametest" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "test").glob("*.java")):
        text = path.read_text(encoding="utf-8")
        if ("@GameTest" in text or "ClientGameTest" in text) and f"io.github.jimbozoomer.jugcraft.test.{path.stem}" not in listed:
            err(f"src/gametest/{path.name}: has game tests but is not a fabric-gametest or fabric-client-gametest entrypoint")


def check_baselines(root):
    """Roadmap step 11: the benchmark and the rules it runs are pure Java (the harness and the game test run the same
    code), and presentation options cannot change server outcomes: no shared class reads the client's display settings,
    and the settings shared code can see (reduced motion and, since step 27, the visual intensity, both held by
    concordance/sign/Presentation.java) are read only in animateTick, which runs on the client (check_signs checks the
    intensity's readers)."""
    for package in ("balance", "compose", "effect", "rules", "resource", "ritual", "alchemy", "ecology", "celestial", "crimson",
                    "worker", "logistics", "artifice", "relic", "equivalence", "hex", "dream", "conclave", "progression", "wonder"):
        for path in sorted((root / package).glob("*.java")):
            if re.search(r"^import net\.(minecraft|fabricmc|spell_engine|spell_power)", path.read_text(encoding="utf-8"), re.M):
                err(f"concordance/{package}/{path.name}: must stay pure Java (no Minecraft or mod imports)")
    for path in sorted(JAVA_ROOT.rglob("*.java")):
        text = path.read_text(encoding="utf-8")
        if "ConcordanceClientOptions" in text:
            err(f"{path.relative_to(JAVA_ROOT)}: shared code must not read the client's display settings (ConcordanceClientOptions)")
        if path == root / "sign" / "Presentation.java":
            continue  # where the settings are held, not read
        method = ""
        for line in text.splitlines():
            signature = re.match(r"^\t(?:public|protected|private|static)[^=;]*\(", line)
            if signature:
                method = line
            if "reducedMotion" in line and "boolean reducedMotion" not in line and "animateTick" not in method:
                err(f"{path.relative_to(JAVA_ROOT)}: reducedMotion is a display setting; read it only in animateTick")


def check_rituals(co, root, lang, registered, research):
    """Roadmap step 12: the Java lifecycle and limits mirror tools/concordance_rituals.py; every structure and ritual
    names things that exist, stays within its limits and is reachable (each offering can be crafted or found, each
    ritual is unlocked by the research that teaches it); every interruption, fault, role and phase has its words; and
    the anchor's GeckoLib assets, the participants' gesture and the Fusion built-in pack are all there."""
    ri = co.rituals
    def java(name, base=root):
        path = base / name
        return path.read_text(encoding="utf-8") if path.exists() else ""
    constants = {
        "ritual/RitualMachine.java": {"STEP_TICKS": ri.STEP_TICKS, "GATHER_TICKS": ri.GATHER_TICKS,
                                      "PARTICIPANT_MARGIN": ri.PARTICIPANT_MARGIN, "PARTICIPANT_HEIGHT": ri.PARTICIPANT_HEIGHT},
        "CircleAnchorBlockEntity.java": {"SLOTS": ri.ANCHOR_SLOTS, "CACHE_TICKS": ri.REPORT_CACHE_TICKS},
        "LeyPylonBlockEntity.java": {"CAPACITY": ri.PYLON_CAPACITY, "POUR": ri.PYLON_POUR, "JE_PER_LEY": ri.JE_PER_LEY,
                                     "JE_RATE": ri.PYLON_JE_RATE},
        "ritual/StructurePattern.java": ri.STRUCTURE_LIMITS,
        "ritual/RitualDefinition.java": {k: v for k, v in ri.RITUAL_LIMITS.items() if k != "MAX_OFFERINGS"},
    }
    for name, values in constants.items():
        text = java(name)
        for const, value in values.items():
            if not re.search(rf"\bint {const} = {value};", text):
                err(f"concordance/{name}: {const} differs from tools/concordance_rituals.py ({value})")
    if ri.RITUAL_LIMITS["MAX_OFFERINGS"] != ri.ANCHOR_SLOTS:
        err("concordance_rituals: a ritual offers at most one item per anchor slot")
    activity = re.search(r'String ACTIVITY = "([^"]+)";', java("CircleAnchorBlockEntity.java"))
    practised = {rule.get("activity") for entry in research.values() for block in entry.get("states", {}).values()
                 for rule in block.get("any", []) if rule.get("type") == "practice"}
    if not activity or activity.group(1) not in practised:
        err("CircleAnchorBlockEntity.ACTIVITY is not the practice any research learns from")
    # Words for every interruption, fault, role and phase the Java names.
    for enum, prefix in (("Interruption", "interrupted"), ("Problem", "fault"), ("Role", "role"), ("Phase", "phase")):
        source = java("ritual/RitualMachine.java") + java("ritual/StructureValidator.java") + java("ritual/StructurePattern.java")
        body = re.search(rf"public enum {enum} \{{(.*?)\n\t\}}", source, re.S)
        ids = re.findall(r'^\t\t[A-Z_]+\("([a-z_]+)"\)', body.group(1) if body else "", re.M)
        if not ids:
            err(f"concordance/ritual: no ids found for {enum}")
        for value in ids:
            if f"message.{MOD}.concordance.circle.{prefix}.{value}" not in lang:
                err(f"ritual: missing lang message.{MOD}.concordance.circle.{prefix}.{value}")
    # Structures.
    structures = {path.stem: load(path) or {} for path in (DATA / MOD / "concordance" / "structure").glob("*.json")}
    limits = ri.STRUCTURE_LIMITS
    for key, entry in structures.items():
        if split(entry.get("anchor", ":"))[1] not in registered:
            err(f"structure {key}: anchor {entry.get('anchor')} is not a Jugcraft block")
        seen, channels = set(), 0
        for group in entry.get("parts", []):
            role = group.get("role")
            if role not in ("channel", "boundary", "clearance"):
                err(f"structure {key}: unknown role {role}")
            block = group.get("block")
            if (role == "clearance") != (block is None):
                err(f"structure {key}: a clearance names no block, every other part one")
            if block and (not tag_exists("block", block[1:]) if block.startswith("#") else
                          split(block)[0] == MOD and split(block)[1] not in registered):
                err(f"structure {key}: unknown block {block}")
            for offset in group.get("at", []):
                reach = max(abs(v) for v in offset)
                if not 0 < reach <= limits["MAX_REACH"] or tuple(offset) in seen:
                    err(f"structure {key}: part at {offset} is outside 1..{limits['MAX_REACH']} or repeated")
                seen.add(tuple(offset))
                channels += role == "channel"
        if not 0 < len(seen) <= limits["MAX_PARTS"] or channels > limits["MAX_CHANNELS"]:
            err(f"structure {key}: {len(seen)} parts and {channels} channels exceed its limits")
        if f"structure.{MOD}.{key}" not in lang:
            err(f"structure {key}: missing lang structure.{MOD}.{key}")
    # Rituals: references, limits, reachability and the research that unlocks them.
    rituals = {path.stem: load(path) or {} for path in (DATA / MOD / "concordance" / "ritual").glob("*.json")}
    recipes = {path.stem for path in (DATA / MOD / "recipe").glob("*.json")}
    made = {split(r["result"]["transform"]["into"])[1] for r in rituals.values() if "transform" in r.get("result", {})}
    unlocked = {split(ref)[1] for entry in research.values() for block in entry.get("unlocks", {}).values()
                for ref in block.get("rituals", [])}
    lim = ri.RITUAL_LIMITS
    for key, entry in rituals.items():
        structure = structures.get(split(entry.get("structure", ":"))[1])
        if structure is None:
            err(f"ritual {key}: unknown structure {entry.get('structure')}")
        study = research.get(split(entry.get("research", ":"))[1])
        if study is None or entry.get("stage") not in study.get("states", {}):
            err(f"ritual {key}: unknown research or stage")
        elif key not in {split(r)[1] for r in study.get("unlocks", {}).get(entry["stage"], {}).get("rituals", [])}:
            err(f"ritual {key}: its research does not list it among what {entry.get('stage')} unlocks")
        if key not in unlocked:
            err(f"ritual {key}: no research unlocks it")
        for field, low, high in (("participants", 1, lim["MAX_PARTICIPANTS"]), ("focus", 0, lim["MAX_FOCUS"]),
                                 ("steps", 1, lim["MAX_STEPS"]), ("ley", 0, lim["MAX_LEY"]),
                                 ("backlash", 0, lim["MAX_BACKLASH"])):
            if not (isinstance(entry.get(field), int) and low <= entry[field] <= high):
                err(f"ritual {key}: {field} must be {low}..{high}")
        if entry.get("focus", 0) > co.FOCUS_MAX:
            err(f"ritual {key}: costs more Focus than a player can hold")
        if entry.get("ley", 0) > 0 and structure is not None \
                and not any(g.get("role") == "channel" for g in structure.get("parts", [])):
            err(f"ritual {key}: draws Ley Charge from a structure with no channels")
        offerings = entry.get("offerings", [])
        if not 0 < len(offerings) <= lim["MAX_OFFERINGS"]:
            err(f"ritual {key}: 1 to {lim['MAX_OFFERINGS']} offerings")
        for offering in offerings:
            ref = offering.get("item", "")
            if ref.startswith("#"):
                if not tag_exists("item", ref[1:]):
                    err(f"ritual {key}: unknown item tag {ref}")
            elif split(ref)[0] == MOD and (split(ref)[1] not in registered
                                           or split(ref)[1] not in recipes and split(ref)[1] not in made):
                err(f"ritual {key}: offering {ref} is not a Jugcraft item with a way to make it")
        result = entry.get("result", {})
        if "transform" in result:
            into = result["transform"].get("into", ":")
            if split(into)[0] == MOD and split(into)[1] not in registered:
                err(f"ritual {key}: makes unknown item {into}")
            if not any(o.get("item") == result["transform"].get("from") and o.get("count") == 1 for o in offerings):
                err(f"ritual {key}: transforms an item that is not offered once")
        elif not 0 < len(result.get("effects", [])) <= lim["MAX_GRANTS"]:
            err(f"ritual {key}: needs a transform or 1 to {lim['MAX_GRANTS']} effects")
        for grant in result.get("effects", []):
            operation = grant.get("operation", {})
            if "scaling" in operation:
                err(f"ritual {key}: a ritual's effects never scale with Spell Power")
            if co.EFFECT_KINDS.get(operation.get("effect"), {}).get("on") != "creature":
                err(f"ritual {key}: effect {operation.get('effect')} does not act on creatures")
        if f"ritual.{MOD}.{key}" not in lang:
            err(f"ritual {key}: missing lang ritual.{MOD}.{key}")
    # Ley Charge comes in only through a losing conversion, and nothing turns it back.
    conversion = load(DATA / MOD / "concordance" / "conversion" / "radiance_to_ley.json") or {}
    if conversion.get("from", {}).get("resource") != "essence/radiance" or conversion.get("to", {}).get("resource") != "ley_charge" \
            or conversion.get("to", {}).get("amount", 0) >= conversion.get("from", {}).get("amount", 0):
        err("conversion radiance_to_ley must turn Radiance into less Ley Charge")
    if '"jugcraft:radiance_to_ley"' not in java("LeyPylonBlockEntity.java"):
        err("LeyPylonBlockEntity.java: must pour through the conversion jugcraft:radiance_to_ley")
    if "EnergyStorage.SIDED.registerForBlockEntity" not in java("JugcraftConcordance.java") \
            or "LeyPylonBlock extends BaseEntityBlock implements EnergyConnectable" not in java("LeyPylonBlock.java"):
        err("ley_pylon: must take electricity through the shared energy interface and connect to cables")
    # The anchor's GeckoLib assets: model, animations named by the Java, UVs inside the sheet.
    geo = load(ASSETS / "geckolib" / "models" / "block" / "circle_anchor.geo.json") or {}
    animations = load(ASSETS / "geckolib" / "animations" / "block" / "circle_anchor.animation.json") or {}
    names = set(animations.get("animations", {}))
    for name in re.findall(r'thenLoop\("([^"]+)"\)', java("CircleAnchorBlockEntity.java")):
        if name not in names:
            err(f"CircleAnchorBlockEntity.java: animation {name} is not in circle_anchor.animation.json")
    definition = (geo.get("minecraft:geometry") or [{}])[0]
    width = definition.get("description", {}).get("texture_width", 0)
    height = definition.get("description", {}).get("texture_height", 0)
    bones = {bone["name"] for bone in definition.get("bones", [])}
    for clip in animations.get("animations", {}).values():
        for bone in clip.get("bones", {}):
            if bone not in bones:
                err(f"circle_anchor.animation.json: animates unknown bone {bone}")
    for bone in definition.get("bones", []):
        for cube in bone.get("cubes", []):
            w, h, d = cube["size"]
            u, v = cube["uv"]
            if u + 2 * (w + d) > width or v + d + h > height:
                err(f"circle_anchor.geo.json: a cube in {bone['name']} maps outside the {width}x{height} sheet")
    with Image.open(ASSETS / "textures" / "block" / "circle_anchor.png") as sheet:
        if sheet.size != (width, height):
            err("textures/block/circle_anchor.png must be the size the geo model declares")
    if "GeoBlockRenderer" not in java("CircleClient.java", CLIENT_JAVA_ROOT):
        err("CircleClient.java: the anchor must be drawn by GeckoLib")
    # The participants' gesture, and the Fusion pack (loaded only with Fusion).
    if not (ASSETS / "player_animations" / "circle_channel.json").is_file() \
            or 'Jugcraft.id("circle_channel")' not in java("CircleClient.java", CLIENT_JAVA_ROOT):
        err("rituals: the circle_channel gesture must exist and be the one CircleClient plays")
    pack = RES / "resourcepacks" / "fusion_textures"
    model = load(pack / "assets" / MOD / "models" / "block" / "warding_stone.json") or {}
    if load(pack / "pack.mcmeta") is None or model.get("loader") != "fusion:model":
        err("resourcepacks/fusion_textures: needs pack.mcmeta and the Warding Stone's Fusion model")
    connected = ASSETS / "textures" / "block" / "warding_stone_connected.png"
    if not connected.is_file() or (load(connected.with_name(connected.name + ".mcmeta")) or {}).get("fusion", {}).get("layout") != "simple":
        err("textures/block/warding_stone_connected.png needs its Fusion metadata")
    if 'isModLoaded("fusion")' not in java("CircleClient.java", CLIENT_JAVA_ROOT):
        err("CircleClient.java: register the Fusion pack only when Fusion is installed (it is optional)")


def check_ember(co, root, lang, research):
    """Ember, part 1 (tools/concordance_ember.py): the Java mirrors its numbers, ids and kinds of hearth; the kindling word
    is an alteration in Ember's school; Hearthbinding is mastered by the hearthkeeping practice, which the Overworld's
    hearths alone can complete; Ember's invocations are Hearthbinding's; and every word it adds has its lang."""
    em = co.ember
    def java(name):
        path = root / "ember" / name
        return path.read_text(encoding="utf-8") if path.exists() else ""
    ember, smoulder = java("Ember.java"), java("SmoulderEffect.java")
    if not re.search(rf"\bint FIRE_TICKS = {em.SMOULDER_FIRE_TICKS};", smoulder):
        err("concordance/ember/SmoulderEffect.java: FIRE_TICKS differs from tools/concordance_ember.py")
    if f"0x{em.SMOULDER_COLOUR:06X}" not in smoulder:
        err("concordance/ember/SmoulderEffect.java: its colour differs from SMOULDER_COLOUR")
    kindling = em.COMPONENTS["kindling"]["operation"]
    if kindling.get("effect") != "alteration" or kindling.get("school") != co.PRINCIPLES["ember"]["school"]:
        err("component kindling: it must be an alteration in Ember's school")
    if f'String SCHOOL = "{kindling.get("school")}";' not in ember:
        err("concordance/ember/Ember.java: SCHOOL differs from the kindling word's school")
    if f'String ACTIVITY = "{em.HEARTHKEEPING}";' not in ember or f'Jugcraft.id("{split(em.SMOULDER)[1]}")' not in ember:
        err("concordance/ember/Ember.java: ACTIVITY or the Smoulder id differs from tools/concordance_ember.py")
    body = ember[ember.find("public static @Nullable String hearth("):]
    body = body[:body.find("\n\t}\n")]
    kinds = set(re.findall(r'return "([a-z_]+)"', body)) | set(re.findall(r'\? "([a-z_]+)"', body))
    if kinds != set(em.HEARTHS):
        err(f"concordance/ember/Ember.java: hearth() names {sorted(kinds)}, not HEARTHS {sorted(em.HEARTHS)}")
    overworld = [kind for kind in em.HEARTHS if kind != "soul_campfire"]
    if not 1 <= em.HEARTH_MASTERY <= len(overworld):
        err("Hearthbinding's mastery asks for more kinds of hearth than the Overworld has")
    practice = [rule for block in research.get("hearthbinding", {}).get("states", {}).values()
                for rule in block.get("any", []) if rule.get("type") == "practice"]
    if not practice or practice[0].get("activity") != em.HEARTHKEEPING or practice[0].get("distinct") != em.HEARTH_MASTERY:
        err("Hearthbinding must be mastered by the hearthkeeping practice, HEARTH_MASTERY kinds of hearth")
    for key, info in em.INVOCATIONS.items():
        if info["principle"] != "ember" or info["research"] != f"{MOD}:hearthbinding":
            err(f"invocation {key}: Ember's invocations are Hearthbinding's")
    for key in (f"effect.{MOD}.smoulder", f"message.{MOD}.concordance.examine.heat", f"tag.item.{MOD}.ember_specimens"):
        if key not in lang:
            err(f"Ember: missing lang {key}")
    check_ember_regalia(co, root, lang)


def check_ember_regalia(co, root, lang):
    """Ember, part 2 (tools/concordance_ember.py, docs/features/arcane-concordance-ember-regalia.md): the Java mirrors the
    regalia's numbers and ids; the foci carry their Spell Power only through Trinkets; the bangle's blow goes through the
    effect boundary and Spell Power's attribute enchantments are refused on the set; the client wires GeckoLib and poses
    the bones before hiding the slim sleeves; every item is the owner's model and icon with the owner's name; every
    recipe is made of Overworld things; the owner's two slots are given to players; the owner's GeckoLib model has the
    bones and fits its sheet; and the owner's recordings, not a drawn cue, play Hearthflare."""
    em = co.ember
    def text(path):
        return path.read_text(encoding="utf-8") if path.exists() else ""
    gear, trinket, armor = (text(root / "ember" / name) for name in ("EmberGear.java", "EmberTrinketItem.java", "EmberArmorItem.java"))
    client, renderer = text(CLIENT_JAVA_ROOT / "EmberClient.java"), text(CLIENT_JAVA_ROOT / "ember" / "EmberArmorRenderer.java")
    numbers = {"LESSER_FOCUS_POWER": ("double", em.LESSER_FOCUS_POWER), "FOCUS_POWER": ("double", em.FOCUS_POWER),
               "ROBE_PIECE_POWER": ("double", em.ROBE_PIECE_POWER), "BANGLE_SMOULDER_TICKS": ("int", em.BANGLE_SMOULDER_TICKS),
               "BANGLE_REACH_MARGIN": ("double", em.BANGLE_REACH_MARGIN),
               "ARMOR_ENCHANTABILITY": ("int", em.ARMOR_ENCHANTABILITY),
               **{f"{name.upper()}_DURABILITY": ("int", armour["durability"]) for name, armour in em.ARMOR_SETS.items()}}
    for name, (kind, value) in numbers.items():
        if not re.search(rf"\b{kind} {name} = {value};", gear):
            err(f"concordance/ember/EmberGear.java: {name} differs from tools/concordance_ember.py ({value})")
    models = {armour["model"] for armour in em.ARMOR_SETS.values()}
    if len(models) != 1 or f'String ARMOR_MODEL = "{next(iter(models))}";' not in gear:
        err("concordance/ember/EmberGear.java: ARMOR_MODEL is not the model tools/concordance_ember.py wears the sets as")
    for name, armour in em.ARMOR_SETS.items():
        d = armour["defense"]
        made = f'material("{name}", {name.upper()}_DURABILITY, {d["helmet"]}, {d["chestplate"]}, {d["leggings"]}, {d["boots"]})'
        if made not in gear:
            err(f"concordance/ember/EmberGear.java: the {name} material differs from ARMOR_SETS (expected {made})")
        for item in armour["pieces"]:
            if not re.search(rf'\barmor\("{item}", "{name}", {name.upper()}_ARMOR, ', gear):
                err(f"concordance/ember/EmberGear.java: {item} is not registered as a {name} piece")
    if ('Jugcraft.id("repairs_" + set + "_gear")' not in gear or "ResourceKey.create(EquipmentAssets.ROOT_ID, Jugcraft.id(set))" not in gear
            or any(em.repair_tag(name) != f"{MOD}:repairs_{name}_gear" for name in em.ARMOR_SETS)):
        err("concordance/ember/EmberGear.java: each set's repair tag and equipment asset must be named after the set")
    geared = re.search(r"double GEARED_FIRE_POWER = ([0-9.]+);", text(root / "balance" / "Baselines.java"))
    if not geared or float(geared.group(1)) != em.FOCUS_POWER + 4 * em.ROBE_PIECE_POWER:
        err("concordance/balance/Baselines.java: GEARED_FIRE_POWER is not a Focus of Fire and a whole fire set")
    # The ids EmberGear registers are the generator's items, the trinkets as trinkets and the pieces as armour.
    trinkets = set(re.findall(r'\bitem\("([a-z_]+)"', gear))
    pieces = set(re.findall(r'\barmor\("([a-z_]+)"', gear))
    if trinkets != set(em.TRINKETS) or pieces != set(em.ARMOR):
        err(f"concordance/ember/EmberGear.java registers {sorted(trinkets)} and {sorted(pieces)}, not TRINKETS and ARMOR")
    for match in re.finditer(r'\bitem\("([a-z_]+)",[^;]*;', gear):
        if ".attributes(" in match.group(0) or "ATTRIBUTE_MODIFIERS" in match.group(0):
            err(f"concordance/ember/EmberGear.java: {match.group(1)} carries attribute modifiers on the item (they would count in "
                "the hand); a worn piece's Spell Power is Trinkets'")
    if ("implements TrinketCallback" not in trinket or "forEachTrinketModifier" not in trinket or "ATTRIBUTE_MODIFIERS" in trinket
            or "Ember.SCHOOL" not in trinket):
        err("concordance/ember/EmberTrinketItem.java: a focus's fire must be a Trinkets modifier in Ember's school, never the item's own")
    if "implements GeoItem" not in armor or "EmberHooks.armor.apply(this)" not in armor:
        err("concordance/ember/EmberArmorItem.java: the set must be a GeoItem drawn through EmberHooks")
    if "EmberHooks.armor = " not in client or "EmberClient.register();" not in text(CLIENT_JAVA_ROOT / "ConcordanceClient.java"):
        err("client: EmberClient must set EmberHooks.armor, and ConcordanceClient must call it (a worn piece fails without it)")
    posed = renderer.find("super.adjustModelBonesForRender(")
    if posed < 0 or any(f'"{bone}"' not in renderer for bone in em.SLIM_BONES) or "skipRender(true)" not in renderer[posed:]:
        err("client/ember/EmberArmorRenderer.java: adjustModelBonesForRender must call super first (it poses the bones to the "
            "wearer) and then hide the slim sleeves")
    if "player.isWithinAttackRange(player.getMainHandItem(), target.getBoundingBox(), BANGLE_REACH_MARGIN)" not in gear:
        err("concordance/ember/EmberGear.java: the bangle's blow must count only within reach of the weapon in hand "
            "(BANGLE_REACH_MARGIN), so a Shock arc's second foe or a far weapon skill is not set smouldering")
    if ("ConcordanceEffects.apply(" not in gear or "REQUIRES_MATCHING_ATTRIBUTE" not in gear or "EnchantmentEvents.ALLOW_ENCHANTING" not in gear
            or any(call in gear for call in ("igniteForSeconds", "igniteForTicks", "addEffect(", "setRemainingFireTicks", "sendParticles"))):
        err("concordance/ember/EmberGear.java: the bangle's blow must go through ConcordanceEffects.apply only, and Spell Power's "
            "attribute enchantments must be refused on the set")
    # Each item: the owner's model (texture renamed), an imported icon, the owner's name and Jugcraft's tooltip.
    owner_lang = em.owner_lang()
    for item, owner in em.OWNER_ITEMS.items():
        if load(ASSETS / "items" / f"{item}.json") != {"model": {"type": "minecraft:model", "model": f"{MOD}:item/{item}"}}:
            err(f"assets/{MOD}/items/{item}.json: not the item's own model")
        if load(ASSETS / "models" / "item" / f"{item}.json") != em.owner_model(item):
            err(f"assets/{MOD}/models/item/{item}.json: not the owner's {owner} model with its texture renamed")
        if f"textures/item/{item}.png" not in em.OWNER_FILES:
            err(f"{item}: its icon is not one of the owner's files (OWNER_FILES)")
        if lang.get(f"item.{MOD}.{item}") != owner_lang.get(f"item.ars_jymbaumental.{owner}") or f"tooltip.{MOD}.{item}" not in lang:
            err(f"{item}: its name must be the owner's ({owner_lang.get(f'item.ars_jymbaumental.{owner}')}) and it needs a tooltip")
    if set(em.OWNER_ITEMS) != set(em.ITEMS) or set(em.ITEMS) != set(em.TRINKETS) | set(em.ARMOR):
        err("tools/concordance_ember.py: ITEMS, OWNER_ITEMS, TRINKETS and ARMOR must name the same items")
    excluded = set(co.equivalence.EXCLUDED)
    for item in em.ITEMS:
        if f"{MOD}:{item}" not in excluded:
            err(f"{item}: the Concordance's magical things are never weighed (tools/concordance_equivalence.py EXCLUDED)")
    # Recipes: Ember's own items, or things the Overworld gives (the progression graph's sources with no dimension).
    progression = co.progression
    for item, recipe in em.RECIPES.items():
        data = load(DATA / MOD / "recipe" / f"{item}.json") or {}
        if data.get("fabric:load_conditions") != [{"condition": f"{MOD}:feature_enabled", "feature": "concordance"}]:
            err(f"recipe {item}: it must load with the Concordance")
        for ref in data.get("key", {}).values():
            ref = ref if isinstance(ref, str) else ""
            ok = (ref.startswith(f"{MOD}:") and split(ref)[1] in em.ITEMS
                  or ref in progression.VANILLA_SOURCES and not progression.VANILLA_SOURCES[ref][3]
                  or ref.startswith("#") and ref[1:] in progression.VANILLA_TAGS)
            if not ok:
                err(f"recipe {item}: {ref} is not Ember's own or had in the Overworld (tools/concordance_progression.py)")
    for item in em.ITEMS:
        if item not in em.RECIPES:
            err(f"{item} has no recipe")
    # The owner's two slots: defined, given to players, holding their items, named and drawn with the owner's icons.
    entities = load(DATA / "trinkets" / "entities" / f"{MOD}_ember.json") or {}
    if entities.get("entities") != ["player"] or set(entities.get("slots", [])) != set(em.TRINKET_SLOTS):
        err(f"data/trinkets/entities/{MOD}_ember.json must give players the owner's slots {sorted(em.TRINKET_SLOTS)}")
    for slot, info in em.TRINKET_SLOTS.items():
        group, name = slot.split("/")
        definition = load(DATA / "trinkets" / "slots" / group / f"{name}.json") or {}
        if definition.get("amount") != info["amount"] or definition.get("icon") != f"{MOD}:container/slots/{info['icon']}":
            err(f"data/trinkets/slots/{slot}.json: amount {info['amount']} and the owner's icon")
        if f"textures/gui/sprites/container/slots/{info['icon']}.png" not in em.OWNER_FILES:
            err(f"slot {slot}: its icon is not the owner's (OWNER_FILES)")
        held = set((load(DATA / "trinkets" / "tags" / "item" / group / f"{name}.json") or {}).get("values", []))
        if held != {f"{MOD}:{item}" for item, worn in em.TRINKETS.items() if worn == slot}:
            err(f"data/trinkets/tags/item/{slot}.json holds {sorted(held)}")
        if f"trinkets.slot.{group}.{name}" not in lang:
            err(f"slot {slot}: missing lang trinkets.slot.{group}.{name}")
    for item, piece in em.ARMOR.items():
        tag = {"helmet": "head_armor", "chestplate": "chest_armor", "leggings": "leg_armor", "boots": "foot_armor"}[piece]
        if f"{MOD}:{item}" not in (load(DATA / "minecraft" / "tags" / "item" / f"{tag}.json") or {}).get("values", []):
            err(f"{item} is not in #minecraft:{tag}")
    for name in em.ARMOR_SETS:
        if (load(DATA / MOD / "tags" / "item" / f"repairs_{name}_gear.json") or {}).get("values") != ["#minecraft:wool"]:
            err(f"data/{MOD}/tags/item/repairs_{name}_gear.json must hold wool")
        if f"textures/armor/{name}.png" not in em.OWNER_FILES:
            err(f"the {name} set's worn texture is not one of the owner's files (OWNER_FILES)")
    # The owner's GeckoLib model: format, the sheet size every set worn on it has, the bones the renderer poses and hides,
    # every face on the sheet.
    model_path = ASSETS / "geckolib" / "models" / "armor" / f"{next(iter(models))}.geo.json"
    geo = load(model_path) or {}
    definition = (geo.get("minecraft:geometry") or [{}])[0]
    width = definition.get("description", {}).get("texture_width", 0)
    height = definition.get("description", {}).get("texture_height", 0)
    for name in em.ARMOR_SETS:
        sheet = ASSETS / "textures" / "armor" / f"{name}.png"
        size = Image.open(sheet).size if sheet.exists() else (0, 0)
        if geo.get("format_version") != "1.12.0" or (width, height) != size:
            err(f"{model_path.name}: format 1.12.0 and {name}'s {size[0]}x{size[1]} sheet (it declares {width}x{height})")
    bones = {bone.get("name") for bone in definition.get("bones", [])}
    for bone in em.ARMOR_BONES + em.SLIM_BONES:
        if bone not in bones:
            err(f"{model_path.name}: no bone {bone}")
    for bone in definition.get("bones", []):
        for cube in bone.get("cubes", []):
            uv = cube.get("uv")
            if isinstance(uv, dict):  # per-face UVs
                rects = [(face, *f.get("uv", (0, 0)), *f.get("uv_size", (0, 0))) for face, f in uv.items()]
            else:  # box UV: the cube's net from its corner
                w, h, d = cube.get("size", (0, 0, 0))
                rects = [("box", *(uv or (0, 0)), 2 * (w + d), d + h)]
            for face, u, v, du, dv in rects:
                if not (0 <= min(u, u + du) and max(u, u + du) <= width and 0 <= min(v, v + dv) and max(v, v + dv) <= height):
                    err(f"{model_path.name}: {bone.get('name')}'s {face} face maps outside its {width}x{height} sheet")
    for name in em.ARMOR_SETS:
        if (ASSETS / "equipment" / f"{name}.json").exists():
            err(f"assets/{MOD}/equipment/{name}.json: GeckoLib draws the set; a flat layer would be drawn under it")
    # Sounds: the owner's recordings play their events, and no cue is drawn for them any more.
    played = load(ASSETS / "sounds.json") or {}
    cues = set(re.findall(r'^\s+"([a-z_]+)": \(', text(ROOT / "tools" / "concordance_sounds.py"), re.M))
    for event, names in em.SOUND_FILES.items():
        if [sound.get("name") for sound in played.get(event, {}).get("sounds", [])] != [f"{MOD}:{name}" for name in names]:
            err(f"sounds.json: {event} must play the owner's {names}")
        if event.split(".", 1)[1] in cues:
            err(f"tools/concordance_sounds.py still draws {event}, which plays the owner's recordings")
        for name in names:
            if not (ASSETS / "sounds" / f"{name}.ogg").exists():
                err(f"sounds/{name}.ogg is missing: run tools/owner_art.py")
    import concordance_ember_art
    drawn = {f"textures/{kind}/{name}.png" for kind, name in concordance_ember_art.textures()}
    if drawn & set(em.OWNER_FILES):
        err(f"tools/concordance_ember_art.py draws over imported owner files: {sorted(drawn & set(em.OWNER_FILES))}")



def check_wayfaring(co, root, lang):
    """The trinkets slice, part 1 (tools/concordance_trinkets.py, docs/features/arcane-concordance-trinkets.md): the Java
    mirrors its numbers, slots, modifiers and absorbed harm; the worn items give attributes only as Trinkets modifiers
    named by their kind (so two of a kind never add up) and only Relic Lore lets them be put on; the belt keeps its added
    charm; the death saves run before Dreaming's listener and decline the void, a held totem and a dream; the wave goes
    through the effect boundary only; every item is the owner's model and icon with the owner's name and an Overworld
    recipe; the slots are given to players with the owner's icons and no cosmetic copies; no two items share a name; and
    part 1b's belt and boot are drawn on the wearer as check_wayfaring_worn says."""
    tr = co.trinkets
    def text(path):
        return path.read_text(encoding="utf-8") if path.exists() else ""
    way, worn = text(root / "trinket" / "Wayfaring.java"), text(root / "trinket" / "WornTrinketItem.java")
    numbers = {"CHARM_SLOTS": ("int", tr.CHARM_SLOTS), "BELT_CHARM_SLOTS": ("int", tr.BELT_CHARM_SLOTS),
               "ABSORB_EXHAUSTION": ("float", f"{tr.ABSORB_EXHAUSTION}F"), "ABSORB_COOLDOWN_TICKS": ("int", tr.ABSORB_COOLDOWN_TICKS),
               "FEATHER_JUMP": ("double", tr.FEATHER_JUMP),
               "AMPHIBIAN_SWIM": ("double", tr.AMPHIBIAN_SWIM), "AMPHIBIAN_OXYGEN": ("double", tr.AMPHIBIAN_OXYGEN),
               "ICE_BREAKER_KNOCKBACK": ("double", tr.ICE_BREAKER_KNOCKBACK), "WAVE_RADIUS": ("double", tr.WAVE_RADIUS),
               "WAVE_RADIUS_PER_HARM": ("double", tr.WAVE_RADIUS_PER_HARM), "WAVE_MAX_RADIUS": ("double", tr.WAVE_MAX_RADIUS),
               "WAVE_TARGETS": ("int", tr.WAVE_TARGETS), "WAVE_DAMAGE": ("int", tr.WAVE_DAMAGE), "WAVE_PUSH": ("int", tr.WAVE_PUSH),
               "WAVE_SLOW_TICKS": ("int", tr.WAVE_SLOW_TICKS), "VIAL_HEALTH": ("float", f"{tr.VIAL_HEALTH}F"),
               "VIAL_REGENERATION_TICKS": ("int", tr.VIAL_REGENERATION_TICKS),
               "PHOENIX_REGENERATION_TICKS": ("int", tr.PHOENIX_REGENERATION_TICKS),
               "PHOENIX_FIRE_RESISTANCE_TICKS": ("int", tr.PHOENIX_FIRE_RESISTANCE_TICKS),
               "BELT_SLOT": ("String", f'"{tr.BELT_SLOT}"'), "CHARM_SLOT": ("String", f'"{tr.CHARM_SLOT}"'),
               "FEET_SLOT": ("String", f'"{tr.FEET_SLOT}"')}
    for name, (kind, value) in numbers.items():
        if not re.search(rf"\b{kind} {name} = {re.escape(str(value))};", way):
            err(f"concordance/trinket/Wayfaring.java: {name} differs from tools/concordance_trinkets.py ({value})")
    if 'String SLOT_COUNT = "trinkets:slot_count/" + CHARM_SLOT;' not in way or tr.SLOT_COUNT != "trinkets:slot_count/" + tr.CHARM_SLOT:
        err("concordance/trinket/Wayfaring.java: SLOT_COUNT must be Trinkets' count attribute for the Charm slot")
    for item, types in tr.ABSORBS.items():
        if f'"{item}", List.of({", ".join(chr(34) + t + chr(34) for t in types)})' not in way:
            err(f"concordance/trinket/Wayfaring.java: ABSORBS for {item} differs from tools/concordance_trinkets.py ({types})")
    if len(re.findall(r'^\t\t\t"[a-z_]+", List\.of\(', way, re.M)) != len(tr.ABSORBS):
        err("concordance/trinket/Wayfaring.java: ABSORBS names other charms than tools/concordance_trinkets.py")
    # Each registration: the item, the kind its modifiers are named after, and the modifiers (attribute -> constant).
    constants = {"SLOT_COUNT": tr.SLOT_COUNT, "BELT_CHARM_SLOTS": tr.BELT_CHARM_SLOTS, "FEATHER_JUMP": tr.FEATHER_JUMP,
                 "AMPHIBIAN_SWIM": tr.AMPHIBIAN_SWIM, "AMPHIBIAN_OXYGEN": tr.AMPHIBIAN_OXYGEN,
                 "ICE_BREAKER_KNOCKBACK": tr.ICE_BREAKER_KNOCKBACK}
    registered = {}
    for match in re.finditer(r'\bitem\("([a-z_]+)", "([a-z_]+)", Map\.of\((.*?)\), Rarity\.', way, re.S):
        args = [arg.strip() for arg in re.split(r",\s*", match.group(3).replace("\n", " ")) if arg.strip()]
        modifiers = {}
        for attribute, value in zip(args[0::2], args[1::2]):
            attribute = attribute.strip('"') if attribute.startswith('"') else constants.get(attribute, attribute)
            modifiers[attribute] = constants.get(value.replace("(double)", "").strip(), value)
        registered[match.group(1)] = (match.group(2), modifiers)
    if set(registered) != set(tr.TRINKETS):
        err(f"concordance/trinket/Wayfaring.java registers {sorted(registered)}, not TRINKETS")
    for item, (kind, modifiers) in registered.items():
        if kind != tr.KIND.get(item) or modifiers != tr.MODIFIERS.get(item, {}):
            err(f"concordance/trinket/Wayfaring.java: {item} is registered as {kind} {modifiers}, not {tr.KIND.get(item)} "
                f"{tr.MODIFIERS.get(item, {})} (tools/concordance_trinkets.py)")
    if tr.KIND["phoenix_down"] != "angelic_feather" or tr.MODIFIERS.get("phoenix_down") != tr.MODIFIERS.get("angelic_feather"):
        err("tools/concordance_trinkets.py: worn, the Phoenix Down is an Angelic Feather as well (its kind and modifiers)")
    if ("implements TrinketCallback" not in worn or "forEachTrinketModifier" not in worn or "ATTRIBUTE_MODIFIERS" in worn
            or 'Jugcraft.id("wayfaring/" + kind + "/" + attribute.getPath())' not in worn or "slotIdentifier.withSuffix" in worn):
        err("concordance/trinket/WornTrinketItem.java: a worn item's attributes must be Trinkets modifiers named by its kind "
            "(never by slot: two of a kind would add up), never the item's own")
    if "return entity instanceof Player player && Reliquary.knows(player);" not in worn:
        err("concordance/trinket/WornTrinketItem.java: only someone who understands Relic Lore may put one on (canEquip)")
    if ("holdsAddedCharm(entity)" not in worn or "slot.get() == stack" not in worn
            or "TrinketCallback.super.canUnequip(stack, slot, entity)" not in worn):
        err("concordance/trinket/WornTrinketItem.java: the worn belt (not a cosmetic one) keeps its added charm (canUnequip), "
            "and Curse of Binding still holds")
    if "slot.cosmetic() ? TrinketDropRule.KEEP" not in worn:
        err("concordance/trinket/WornTrinketItem.java: a cosmetic copy stays through death (Trinkets would drop it and keep it)")
    absorb = way[way.find("public static boolean absorb("):way.find("private static boolean allowDamage(")]
    if ("ABSORB_COOLDOWN_TICKS" not in absorb or "if (points > food.getFoodLevel())" not in absorb
            or "causeFoodExhaustion(exhaustion - points * 4.0F)" not in absorb or "TAKEN.put(" not in absorb):
        err("concordance/trinket/Wayfaring.java: a charm takes only a blow the food bar can pay for (whole points from the "
            "bar, the rest as exhaustion, which vanilla caps), and keeps vanilla's hurt cooldown, which its blows never start")
    if "return !(entity instanceof ServerPlayer player) || !absorb(player, source, amount);" not in way:
        err("concordance/trinket/Wayfaring.java: ALLOW_DAMAGE must refuse a blow only when absorb() took it")
    if ".attributes(" in way or "ATTRIBUTE_MODIFIERS" in way:
        err("concordance/trinket/Wayfaring.java: a worn item carries no attribute modifiers of its own (they would count in the hand)")
    if "SlotAttributes.createAttributeForSlot(CHARM_SLOT)" not in way:
        err("concordance/trinket/Wayfaring.java: the Charm slot's count attribute must be registered at start-up")
    if ("ALLOW_DEATH.addPhaseOrdering(SAVE_PHASE, Event.DEFAULT_PHASE)" not in way or "ALLOW_DEATH.register(SAVE_PHASE," not in way
            or re.search(r"ALLOW_DEATH\.register\((?!SAVE_PHASE)", way)):
        err("concordance/trinket/Wayfaring.java: the death saves must run in SAVE_PHASE, before Dreaming's default-phase listener")
    save = way[way.find("public static boolean save("):way.find("private static boolean holdsTotem(")]
    for guard in ("DamageTypeTags.BYPASSES_INVULNERABILITY", "Dreaming.dreaming(player)", "holdsTotem(player)", "Reliquary.knows(player)",
                  "Reliquary.enabled()"):
        if guard not in save:
            err(f"concordance/trinket/Wayfaring.java: a death save must decline unless {guard} allows it")
    if "DataComponents.DEATH_PROTECTION" not in way:
        err("concordance/trinket/Wayfaring.java: a held totem answers before a vial (DataComponents.DEATH_PROTECTION)")
    if ("ConcordanceEffects.apply(" not in way or "instanceof Enemy" not in way or "source.is(DamageTypes.FALL)" not in way
            or any(call in way for call in ("hurtServer(", "addEffect(", ".push(", "setDeltaMovement(", "igniteFor"))):
        err("concordance/trinket/Wayfaring.java: the wave (after a plain fall, at hostile creatures) and the saves' statuses "
            "must go through ConcordanceEffects.apply only")
    # Each item: the owner's model (texture renamed), an imported icon (and sidecar), the owner's name, a tooltip.
    for item in tr.OWNER_ITEMS:
        if load(ASSETS / "items" / f"{item}.json") != {"model": {"type": "minecraft:model", "model": f"{MOD}:item/{item}"}}:
            err(f"assets/{MOD}/items/{item}.json: not the item's own model")
        if load(ASSETS / "models" / "item" / f"{item}.json") != tr.owner_model(item):
            err(f"assets/{MOD}/models/item/{item}.json: not the owner's {tr.OWNER_ITEMS[item]} model with its texture renamed")
        if f"textures/item/{item}.png" not in tr.OWNER_FILES:
            err(f"{item}: its icon is not one of the owner's files (OWNER_FILES)")
        if (item in tr.ANIMATED) != (f"textures/item/{item}.png.mcmeta" in tr.OWNER_FILES):
            err(f"{item}: its animation sidecar must be imported exactly when the owner's icon is a strip (ANIMATED)")
        if lang.get(f"item.{MOD}.{item}") != tr.owner_name(item) or f"tooltip.{MOD}.{item}" not in lang:
            err(f"{item}: its name must be the owner's ({tr.owner_name(item)}) and it needs a tooltip")
    if not set(tr.OWNER_ITEMS) == set(tr.ITEMS) == set(tr.TRINKETS) or not set(tr.MODIFIERS) <= set(tr.ITEMS):
        err("tools/concordance_trinkets.py: ITEMS, OWNER_ITEMS and TRINKETS must name the same items")
    if not set(tr.ABSORBS) <= {item for item, slot in tr.TRINKETS.items() if slot == tr.CHARM_SLOT}:
        err("tools/concordance_trinkets.py: only charms absorb harm")
    excluded = set(co.equivalence.EXCLUDED)
    for item in tr.ITEMS:
        if f"{MOD}:{item}" not in excluded:
            err(f"{item}: the Concordance's magical things are never weighed (tools/concordance_equivalence.py EXCLUDED)")
    # Recipes: the slice's own items, or things the Overworld gives.
    progression = co.progression
    for item in tr.ITEMS:
        data = load(DATA / MOD / "recipe" / f"{item}.json") or {}
        if item not in tr.RECIPES and item not in tr.SHAPELESS:
            err(f"{item} has no recipe")
        if data.get("fabric:load_conditions") != [{"condition": f"{MOD}:feature_enabled", "feature": "concordance"}]:
            err(f"recipe {item}: it must load with the Concordance")
        for ref in list(data.get("key", {}).values()) + list(data.get("ingredients", [])):
            ref = ref if isinstance(ref, str) else ""
            ok = (ref.startswith(f"{MOD}:") and split(ref)[1] in tr.ITEMS and split(ref)[1] != item
                  or ref in progression.VANILLA_SOURCES and not progression.VANILLA_SOURCES[ref][3])
            if not ok:
                err(f"recipe {item}: {ref} is not the slice's own or had in the Overworld (tools/concordance_progression.py)")
    # The slots: the belt slot is Trinkets' own (left as it defines it); the charm and feet slots are Jugcraft's.
    entities = load(DATA / "trinkets" / "entities" / f"{MOD}_wayfaring.json") or {}
    if entities.get("entities") != ["player"] or set(entities.get("slots", [])) != set(tr.GRANTED_SLOTS):
        err(f"data/trinkets/entities/{MOD}_wayfaring.json must give players {sorted(tr.GRANTED_SLOTS)}")
    group, name = tr.BELT_SLOT.split("/")
    if (DATA / "trinkets" / "slots" / group / f"{name}.json").exists():
        err(f"data/trinkets/slots/{tr.BELT_SLOT}.json: the belt slot is Trinkets' own; redefining it would make its icon "
            "depend on data-pack order")
    owned = {**tr.OWNER_FILES, **tr.OWNER_BLOCKS_FILES}
    for slot, info in tr.TRINKET_SLOTS.items():
        group, name = slot.split("/")
        definition = load(DATA / "trinkets" / "slots" / group / f"{name}.json") or {}
        if (definition.get("amount") != info["amount"] or definition.get("icon") != f"{MOD}:container/slots/{info['icon']}"
                or definition.get("cosmetic_slots") is not False):
            err(f"data/trinkets/slots/{slot}.json: amount {info['amount']}, the owner's icon, and no cosmetic slots")
        if f"textures/gui/sprites/container/slots/{info['icon']}.png" not in owned:
            err(f"slot {slot}: its icon is not the owner's (OWNER_FILES, OWNER_BLOCKS_FILES)")
        if lang.get(f"trinkets.slot.{group}.{name}") != info["name"]:
            err(f"slot {slot}: lang trinkets.slot.{group}.{name} must be {info['name']}")
    for slot in tr.GRANTED_SLOTS:
        group, name = slot.split("/")
        held = set((load(DATA / "trinkets" / "tags" / "item" / group / f"{name}.json") or {}).get("values", []))
        if held != {f"{MOD}:{item}" for item, worn_in in tr.TRINKETS.items() if worn_in == slot}:
            err(f"data/trinkets/tags/item/{slot}.json holds {sorted(held)}")
    # Two items with one English name cannot be told apart (the kinetic belt is the Drive Belt for this reason).
    names = {}
    for key, value in lang.items():
        if key.startswith(f"item.{MOD}.") and key.count(".") == 2:
            names.setdefault(value, []).append(key)
    for value, keys in names.items():
        if len(keys) > 1:
            err(f"lang: {', '.join(sorted(keys))} share the name {value}")
    check_wayfaring_worn(co, lang)


def _worn_faces(model, anchor_y):
    """The faces of a worn block model (tools/concordance_trinkets.py worn_model) on its part, in the player model's pixels
    (x to the wearer's left, y down, z to their back), for a model anchored at (0, anchor_y, 0) on the part: (axis, the
    way it faces along it, its plane, its extent on the other two axes in order)."""
    out = []
    top = 8 + anchor_y
    for element in model["elements"]:
        (fx, fy, fz), (tx, ty, tz) = element["from"], element["to"]
        lo, hi = (fx - 8, top - ty, 8 - tz), (tx - 8, top - fy, 8 - fz)
        for face in element["faces"]:
            k, sign = {"west": (0, -1), "east": (0, 1), "up": (1, -1), "down": (1, 1), "south": (2, -1), "north": (2, 1)}[face]
            out.append((k, sign, hi[k] if sign > 0 else lo[k], [(lo[j], hi[j]) for j in range(3) if j != k]))
    return out


def _item_tag(tag, seen=None):
    """The items an item tag ("ns:path") holds in this repository's own data, nested tags followed; what vanilla or
    another mod adds to it is not seen."""
    seen = set() if seen is None else seen
    if tag in seen:
        return set()
    seen.add(tag)
    ns, _, path = tag.partition(":")
    file = RES / "data" / ns / "tags" / "item" / f"{path}.json"
    data = load(file) if file.is_file() else None
    items = set()
    for entry in data.get("values", []) if isinstance(data, dict) else []:
        value = entry.get("id") if isinstance(entry, dict) else entry
        if isinstance(value, str):
            items |= _item_tag(value[1:], seen) if value.startswith("#") else {value}
    return items


def check_wayfaring_worn(co, lang):
    """Wayfaring part 1b (tools/concordance_trinkets.py WORN, docs/features/arcane-concordance-trinkets.md): the Leather
    Belt and Amphibian Boot drawn on the wearer by Trinkets' data-driven renderer. Each worn sheet is the owner's, imported
    as supplied into the items atlas, one frame of the size WORN gives, and every worn sheet the owner drew for the slice's
    items is drawn; the boxes' nets lie on their sheet apart from each other, read no half-clear texel (cutout draws them
    solid) and between them read every opaque texel the owner drew; the models and render definitions are the generator's,
    anchored where Trinkets attaches them, and no two faces of a model facing the same way share a plane (model_writer
    would have moved one; a box set on another's face lies back to back with it, which the generator's comments answer
    for); every face over its part's box keeps off the skin, its outer layer and vanilla armour's shells as armor_models
    keeps 3D armour (the soles by model_writer's nudge only), and two models' faces stay that nudge apart where they
    overlap standing (the two boots between the legs); no other definition draws a Wayfaring item; and no Java renderer
    replaces a definition (Trinkets uses a registered renderer instead of the data for that item). Every definition wraps
    its models in Jugcraft's jugcraft:unless_covered element (registered by the client before resources load), which
    hides them under WORN_COVERED_BY's armour and on a wearer whose "Show my worn trinkets" setting is off: the element
    reads the wearer's armour and the wearer's own choice (WornDisplay, never this computer's setting); the client sends
    the setting on joining and whenever it is saved; the server takes it only for that player, rate-limited, and keeps it
    on them as an attachment sent to everyone who sees them, kept through death and not saved; the setting is saved and
    on the settings screen, on by default, and has its words.
    Jugcraft's own 3D armour (worn_models.json) is not compared: it is drawn toward the camera, over what it meets (the
    feature record's limits)."""
    import armor_models
    import model_writer
    tr = co.trinkets
    for item, (ns, owner) in tr.OWNER_ITEMS.items():
        if (tr.MAGIC / "assets" / ns / "textures" / "models" / "items" / f"{owner}.png").is_file() and item not in tr.WORN:
            err(f"{item}: the owner drew a worn sheet for it ({ns}/textures/models/items/{owner}.png), but WORN does not draw it")
    for item, info in tr.WORN.items():
        ns, owner = tr.OWNER_ITEMS[item]
        runtime = f"textures/item/{item}_worn.png"
        if tr.OWNER_FILES.get(runtime) != f"assets/{ns}/textures/models/items/{owner}.png":
            err(f"{item}: its worn sheet must be the owner's {ns}/textures/models/items/{owner}.png, imported as {runtime} "
                "(OWNER_FILES)")
            continue
        png = ASSETS / runtime
        if not png.is_file() or png.with_name(png.name + ".mcmeta").exists():
            err(f"{runtime} is missing (run tools/owner_art.py), or has an animation sidecar (a worn sheet is one frame)")
            continue
        with Image.open(png) as image:
            sheet = image.convert("RGBA")
        size = info["sheet"]
        if sheet.size != (size, size):
            err(f"{runtime} is {sheet.size[0]}x{sheet.size[1]}, not the {size}x{size} WORN gives")
            continue
        alpha = sheet.getchannel("A")
        read = {}
        for name, (u, v), dims, _corner in info["boxes"]:
            if not all(isinstance(c, int) and c >= -size for c in (u, v, *dims)) or min(dims) < 0:
                err(f"{item}: the {name}'s net corner and size are whole texels ({(u, v)}, {dims})")
                continue
            for face, (u0, v0, u1, v1) in tr.worn_net(u, v, *dims).items():
                x0, x1, y0, y1 = min(u0, u1), max(u0, u1), min(v0, v1), max(v0, v1)
                if x0 < 0 or y0 < 0 or x1 > size or y1 > size:
                    err(f"{item}: the {name}'s {face} face reads {[u0, v0, u1, v1]}, outside its {size}x{size} sheet")
                    continue
                if any(alpha.crop((x0, y0, x1, y1)).histogram()[1:255]):
                    err(f"{item}: the {name}'s {face} face reads half-clear texels, which cutout draws solid")
                for x in range(x0, x1):
                    for y in range(y0, y1):
                        if (x, y) in read:
                            err(f"{item}: the {name}'s {face} face and the {read[x, y]} read the same texel ({x}, {y})")
                        read[x, y] = f"{name}'s {face} face"
        unread = [(x, y) for x in range(size) for y in range(size) if alpha.getpixel((x, y)) == 255 and (x, y) not in read]
        if unread:
            err(f"{item}: {len(unread)} opaque texels of its sheet lie in no box's net, e.g. {unread[0]}: the boxes no longer "
                "fit the owner's sheet")
    # The models and render definitions, as the generator writes them.
    faces = {}
    for model, (item, part) in tr.worn_models().items():
        if part not in armor_models.BASE:
            err(f"{model}: {part} is not a part of the player model")
            continue
        # Where Trinkets anchors the model (ModelAttachementImpl: the part's own box's middle plus half its size times
        # the offset, up and front negated) must be the part's bottom middle, where worn_model and the soles take it to be.
        (blo, bhi) = armor_models.BASE[part]
        anchor = [(blo[k] + bhi[k]) / 2 + s * (bhi[k] - blo[k]) / 2 * tr.WORN_OFFSET[k] for k, s in enumerate((1, -1, -1))]
        if anchor != [0, bhi[1], 0] or tr.WORN_ANCHOR_Y != bhi[1]:
            err(f"{model}: Trinkets anchors it at {anchor} on the {part} (offset {tr.WORN_OFFSET}), not at the part's "
                f"bottom middle [0, {bhi[1]}, 0] where WORN_ANCHOR_Y ({tr.WORN_ANCHOR_Y}) places its boxes")
        expected = tr.worn_model(item, part)
        drawn = json.loads(json.dumps(expected))
        model_writer.finish_elements(drawn["elements"])
        if drawn != expected:
            err(f"{model}: two of its faces facing the same way share a plane (model_writer moves one when it is written); "
                "fit the boxes apart")
        if load(ASSETS / "models" / "item" / f"{model}.json") != drawn:
            err(f"assets/{MOD}/models/item/{model}.json differs from tools/concordance_trinkets.py (run the generator)")
        faces[model] = (item, part, _worn_faces(expected, tr.WORN_ANCHOR_Y))
        # The skin and vanilla armour under the part, as armor_models keeps 3D armour off them: a face over the part's box
        # (grown by the shell in question) stands armor_models.SKIN_GAP off the skin and its outer layer, and
        # model_writer's nudge off vanilla armour's shells; a boot's sole (a leg's downward face) only the nudge off
        # each, so as little of it is buried as can be.
        skin, armour = armor_models.SKIN_SHELLS[part], (0.5, 1.0)
        for k, sign, plane, rect in faces[model][2]:
            others = [j for j in range(3) if j != k]
            out = sign * (plane - (bhi[k] if sign > 0 else blo[k]))
            sole = part.endswith("_leg") and k == 1 and sign > 0
            for shell in skin + armour:
                margin = max(skin) if shell in skin else shell
                if not all(rect[i][0] < bhi[j] + margin and rect[i][1] > blo[j] - margin for i, j in enumerate(others)):
                    continue
                need = model_writer.COPLANAR_NUDGE if sole or shell in armour else armor_models.SKIN_GAP
                if abs(out - shell) < need - 1e-6:
                    err(f"{model}: a face {out:.2f} px out from the {part}'s box lies within {need} px of the "
                        f"{shell} px shell (the skin, its outer layer or vanilla armour) and would flicker")
    # Worn on parts that never move apart standing (the body and legs; armor_models.PIVOTS), two models' faces facing the
    # same way never share a plane where they overlap, on one part or two: the boots overlap between the legs.
    still = [(model, [(k, sign, plane + armor_models.PIVOTS[part][k],
                       [(lo + armor_models.PIVOTS[part][j], hi + armor_models.PIVOTS[part][j])
                        for j, (lo, hi) in zip([j for j in range(3) if j != k], rect)])
                      for k, sign, plane, rect in found])
             for model, (item, part, found) in faces.items() if part in ("body", "right_leg", "left_leg")]
    for i, (a, faces_a) in enumerate(still):
        for b, faces_b in still[i + 1:]:
            for k, sign, plane, rect in faces_a:
                for k2, sign2, plane2, rect2 in faces_b:
                    if (k, sign) == (k2, sign2) and abs(plane - plane2) < model_writer.COPLANAR_NUDGE - 1e-6 and all(
                            r[0] < r2[1] and r2[0] < r[1] for r, r2 in zip(rect, rect2)):
                        err(f"{a} and {b}: faces {abs(plane - plane2):.2f} px apart where they overlap standing (need "
                            f"{model_writer.COPLANAR_NUDGE}); they would flicker")
    # Each WORN item's render definition is the generator's, and no other definition, in any namespace or folder (Trinkets
    # reads every trinkets/ folder through), draws one of the slice's items, by its id or a tag holding it.
    for item in tr.WORN:
        if load(ASSETS / "trinkets" / f"{item}.json") != tr.worn_render(item):
            err(f"assets/{MOD}/trinkets/{item}.json differs from tools/concordance_trinkets.py worn_render (run the generator)")
    wayfaring = {f"{MOD}:{item}" for item in tr.TRINKETS}
    for path in sorted(RES.glob("assets/*/trinkets/**/*.json")):
        name = path.relative_to(ROOT)
        data = load(path)
        if not isinstance(data, dict):
            err(f"{name}: a Trinkets render definition is a JSON object")
            continue
        targets = data.get("target", [])
        drawn = set()
        for target in targets if isinstance(targets, list) else [targets]:
            if not isinstance(target, str):
                err(f"{name}: its target {target!r} is not an item id or a #tag")
            elif target.startswith("#"):
                drawn |= _item_tag(target[1:]) & wayfaring
            elif target in wayfaring:
                drawn.add(target)
        for target in sorted(drawn):
            item = target.split(":", 1)[1]
            if item not in tr.WORN:
                err(f"{name} draws {target}, which WORN does not (add it to WORN)")
            elif path != ASSETS / "trinkets" / f"{item}.json":
                err(f"{name}: a second render definition draws {target} (its own is assets/{MOD}/trinkets/{item}.json)")
    if tr.WORN_GROW < armor_models.SKIN_GAP or tr.WORN_LEFT_STEP < model_writer.COPLANAR_NUDGE:
        err(f"tools/concordance_trinkets.py: WORN_GROW must be at least armor_models.SKIN_GAP ({armor_models.SKIN_GAP}), so the "
            "belt and boots clear vanilla leggings and boots, and WORN_LEFT_STEP at least model_writer.COPLANAR_NUDGE "
            f"({model_writer.COPLANAR_NUDGE}), so the two boots never share a plane")
    # A Java renderer registered for an item replaces its render definition (TrinketRenderLayer.extract).
    for path in sorted(CLIENT_JAVA_ROOT.rglob("*.java")):
        text = path.read_text(encoding="utf-8")
        for call in re.findall(r"TrinketRendererRegistry\.registerRenderer\(([^,;]+)", text):
            if any(item.upper() in call for item in tr.WORN):
                err(f"{path.relative_to(ROOT)}: a Java renderer for {call.strip()} would replace its render definition")
    # Hidden under armour, and by the player's setting (the owner, 10 October 2026): every WORN item names the armour slots
    # that hide it, and the element its definition uses is the one the client registers, which does what it says.
    if set(tr.WORN_COVERED_BY) != set(tr.WORN):
        err(f"tools/concordance_trinkets.py: WORN_COVERED_BY covers {sorted(tr.WORN_COVERED_BY)}, not WORN's {sorted(tr.WORN)}")
    for item, slots in tr.WORN_COVERED_BY.items():
        if not slots or len(set(slots)) != len(slots) or not set(slots) <= {"head", "chest", "legs", "feet"}:
            err(f"tools/concordance_trinkets.py: WORN_COVERED_BY[{item!r}] must name armour slots (head, chest, legs, feet), "
                f"each once, not {slots}")
    # Each definition is one Jugcraft element, naming WORN_COVERED_BY's slots, round the item's model elements.
    for item in tr.WORN:
        data = load(ASSETS / "trinkets" / f"{item}.json") if (ASSETS / "trinkets" / f"{item}.json").is_file() else None
        render = data.get("render") if isinstance(data, dict) else None
        wrapper = render[0] if isinstance(render, list) and len(render) == 1 and isinstance(render[0], dict) else {}
        models = wrapper.get("then")
        if (wrapper.get("type") != tr.WORN_ELEMENT or wrapper.get("armour") != tr.WORN_COVERED_BY.get(item)
                or not isinstance(models, list) or not models
                or any(not isinstance(m, dict) or m.get("type") != "minecraft:model" for m in models)):
            err(f"assets/{MOD}/trinkets/{item}.json must be one {tr.WORN_ELEMENT} element, armour "
                f"{tr.WORN_COVERED_BY.get(item)}, round its model elements")

    def code(path):
        """A Java file's code with its comments taken out and its whitespace made single spaces ("" if missing)."""
        text = path.read_text(encoding="utf-8") if path.is_file() else ""
        return re.sub(r"\s+", " ", re.sub(r"/\*.*?\*/|//[^\n]*", "", text, flags=re.S))

    def bare(text):
        return re.sub(r"\s+", "", text)

    element = CLIENT_JAVA_ROOT / "trinket" / "UnlessCoveredTrinketElement.java"
    wayfaring_client = CLIENT_JAVA_ROOT / "WayfaringClient.java"
    element_code = code(element)
    if tr.WORN_ELEMENT != f"{MOD}:unless_covered" or 'TYPE = Jugcraft.id("unless_covered")' not in element_code:
        err(f"{element.relative_to(ROOT)}: the element must be {MOD}:unless_covered, the type WORN_ELEMENT names "
            f"({tr.WORN_ELEMENT})")
    for needle, why in (('fieldOf("armour")', "read the armour slots its definition names"),
                        ('fieldOf("then")', "read the elements it draws"),
                        ("WornDisplay.hidden(owner)", "read the wearer's own choice, which the server sends to everyone"),
                        ("getItemBySlot(", "read the wearer's own armour (the render state's is not filled yet)"),
                        ("equippable.slot() == slot", "count armour only in the slot it is worn in"),
                        ("equippable.assetId().isPresent()", "count only armour drawn on the body (an equippable with an asset)"),
                        ("!worn.has(DataComponents.GLIDER)", "not count a glider (an elytra) as covering"),
                        ("element.resolveDependencies(resolver)", "hand its held models on to be baked")):
        if needle not in element_code:
            err(f"{element.relative_to(ROOT)} must {why} ({needle})")
    if "ConcordanceClientOptions" in element_code:
        err(f"{element.relative_to(ROOT)} must not read this computer's setting: each wearer's choice comes from the server "
            "(WornDisplay), so everyone sees the same")
    registered = bare(code(wayfaring_client))
    if "TrinketRenderElements.ID_MAPPER.put(UnlessCoveredTrinketElement.TYPE,UnlessCoveredTrinketElement.CODEC)" not in registered:
        err(f"{wayfaring_client.relative_to(ROOT)} must register the element with Trinkets (TrinketRenderElements.ID_MAPPER)")
    for needle, why in (("ClientPlayConnectionEvents.JOIN.register(", "send the setting on joining a world"),
                        ("ClientPlayNetworking.canSend(WornDisplayPayload.TYPE)", "send only to a server that takes it"),
                        ("new WornDisplayPayload(ConcordanceClientOptions.wornTrinkets())", "send the player's setting")):
        if bare(needle) not in registered:
            err(f"{wayfaring_client.relative_to(ROOT)} must {why} ({needle})")
    if "WayfaringClient.register();" not in code(CLIENT_JAVA_ROOT / "ConcordanceClient.java"):
        err("ConcordanceClient.register() must call WayfaringClient.register(), or no render definition decodes")
    # The server side: the choice is that player's, rate-limited, sent to everyone who sees them, kept through death, not saved.
    display = JAVA_ROOT / "concordance" / "trinket" / "WornDisplay.java"
    display_code = code(display)
    hidden = re.search(r"HIDDEN = AttachmentRegistry\.<Unit>builder\(\)([^;]*);", display_code)
    if (not hidden or ".copyOnDeath()" not in hidden.group(1) or "AttachmentSyncPredicate.all()" not in hidden.group(1)
            or ".persistent(" in hidden.group(1)):
        err(f"{display.relative_to(ROOT)}: HIDDEN must be sent to everyone who sees the wearer (AttachmentSyncPredicate.all()), "
            "kept through death (copyOnDeath) and not saved (the client sends it again on joining)")
    for needle, why in (("registerGlobalReceiver(WornDisplayPayload.TYPE, (payload, context) -> choose(context.player(), payload.shown()))",
                         "take a choice only for the player who sent it"),
                        ("ServerPlayConnectionEvents.DISCONNECT", "forget a waiting choice when its player leaves")):
        if bare(needle) not in bare(display_code):
            err(f"{display.relative_to(ROOT)} must {why} ({needle})")
    if bare(display_code).count("RateGate.allow(player,ACTION,CHANGE_TICKS)") < 2:
        err(f"{display.relative_to(ROOT)} must limit how often a player's choice changes, both where a change is made at once "
            "and where a waiting one lands (RateGate.allow(player, ACTION, CHANGE_TICKS))")
    if "WornDisplay.register();" not in code(JAVA_ROOT / "concordance" / "trinket" / "Wayfaring.java"):
        err("Wayfaring.register() must call WornDisplay.register()")
    # The setting: on at first, saved, sent to the server whenever it is saved, and the screen's switch seeded from it.
    options = code(CLIENT_JAVA_ROOT / "ConcordanceClientOptions.java")
    screen = code(CLIENT_JAVA_ROOT / "ConcordanceSettingsScreen.java")
    if ('properties.getProperty("concordance.worn_trinkets", "true")' not in options
            or 'properties.setProperty("concordance.worn_trinkets", Boolean.toString(wornTrinkets))' not in options
            or "wornTrinkets = trinkets;" not in options):
        err("ConcordanceClientOptions must load concordance.worn_trinkets (on by default), take it from set() and save it")
    for method in ("set", "setWornTrinkets"):
        body = re.search(r"public static void " + method + r"\([^{]*\{([^}]*)\}", options)
        if not body or "WayfaringClient.sendChoice();" not in body.group(1):
            err(f"ConcordanceClientOptions.{method}() must send the setting to the server when it is saved "
                "(WayfaringClient.sendChoice())")
    seed = re.search(r"boolean\[\] values = \{(.*?)\};", screen)
    toggle = re.search(r'config\.worn_trinkets"\), values\[(\d)\]\)(.*?)\.build\(\)', screen)
    n = int(toggle.group(1)) if toggle else -1
    if (not seed or not toggle
            or [s.strip() for s in seed.group(1).split(",")][n:n + 1] != ["ConcordanceClientOptions.wornTrinkets()"]
            or ".setDefaultValue(true)" not in toggle.group(2)
            or f".setSaveConsumer(value -> values[{n}] = value)" not in toggle.group(2)
            or f"values[{n}], intensity[0])" not in screen):
        err("ConcordanceSettingsScreen must offer Show my worn trinkets (seeded from wornTrinkets(), on by default) and save it")
    for key in tr.CLIENT:
        if key not in lang:
            err(f"lang: {key} is missing (tools/concordance_trinkets.py CLIENT)")


def check_alchemy(co, root, lang, registered, research):
    """Roadmap step 13: the Java simulation mirrors tools/concordance_alchemy.py (axes, bands, heat, limits); every
    ingredient, preparation and property is valid and reachable (vanilla items, or Jugcraft items with recipes); every
    line the simulation can say has its words; and the crucible's GeckoLib model, animations and animated sheet agree."""
    al = co.alchemy
    def java(name, base=root):
        path = base / name
        return path.read_text(encoding="utf-8") if path.exists() else ""
    constants = {
        "alchemy/Mixture.java": {"MAX_PARTS": ("int", al.MAX_PARTS), "MAX_OPERATIONS": ("int", al.MAX_OPERATIONS),
                                 "SEARING_KEEP": ("long", al.SEARING_KEEP), "SEARING_CONTAMINANT": ("long", al.SEARING_CONTAMINANT)},
        "alchemy/Heat.java": {"AMBIENT": ("int", al.AMBIENT), "RATE": ("int", al.HEAT_RATE)},
        "alchemy/Assay.java": {"SPOON": ("int", al.SPOON), "GLASS": ("int", al.GLASS), "MASTERED": ("int", al.MASTERED)},
        "CrucibleBlockEntity.java": {"BUFFER_SLOTS": ("int", al.BUFFER_SLOTS), "STIR_TICKS": ("int", al.STIR_TICKS),
                                     "AUTOMATION_TICKS": ("int", al.AUTOMATION_TICKS),
                                     "WATER_PER_BUCKET": ("int", al.WATER_PER_BUCKET)},
    }
    for name, values in constants.items():
        text = java(name)
        for const, (kind, value) in values.items():
            if not re.search(rf"\b{kind} {const} = {value};", text):
                err(f"concordance/{name}: {const} differs from tools/concordance_alchemy.py ({value})")
    if not re.search(rf"double MAX_PROPERTY = {al.MAX_PROPERTY};", java("rules/RulesParser.java")):
        err("concordance/rules/RulesParser.java: MAX_PROPERTY differs from tools/concordance_alchemy.py")
    axes = re.findall(r'^\t[A-Z_]+\("([a-z_]+)"\)', java("alchemy/Axis.java"), re.M)
    if axes != al.AXES:
        err(f"concordance/alchemy/Axis.java: axes {axes} differ from AXES (same order)")
    bands = {m[0]: (None if m[1] == "Integer.MIN_VALUE" else int(m[1]), int(m[2]))
             for m in re.findall(r'^\t[A-Z_]+\("([a-z_]+)", ([A-Za-z_.0-9]+), (\d+)\)', java("alchemy/Band.java"), re.M)}
    if bands != al.BANDS:
        err(f"concordance/alchemy/Band.java: bands {bands} differ from BANDS")
    sources = dict(re.findall(r'"(minecraft:[a-z_]+)", (\d+)', java("alchemy/Heat.java")))
    if {k: int(v) for k, v in sources.items()} != al.HEAT_SOURCES:
        err("concordance/alchemy/Heat.java: SOURCES differ from HEAT_SOURCES")
    activity = re.search(r'String ACTIVITY = "([^"]+)";', java("Alchemy.java"))
    practised = {rule.get("activity") for block in research.get("alembic_arts", {}).get("states", {}).values()
                 for rule in block.get("any", []) if rule.get("type") == "practice"}
    if not activity or activity.group(1) not in practised:
        err("Alchemy.ACTIVITY is not the practice the Alembic Arts learn from")
    # Data.
    folder = DATA / MOD / "concordance"
    ingredients = {p.stem: load(p) or {} for p in (folder / "ingredient").glob("*.json")}
    preparations = {p.stem: load(p) or {} for p in (folder / "preparation").glob("*.json")}
    properties = {p.stem: load(p) or {} for p in (folder / "property").glob("*.json")}
    recipes = {p.stem for p in (DATA / MOD / "recipe").glob("*.json")}
    items = [entry.get("item", "") for entry in ingredients.values()]
    if len(items) != len(set(items)):
        err("ingredients: an item is more than one ingredient")
    for key, entry in ingredients.items():
        ref = entry.get("item", ":")
        if split(ref)[0] == MOD and (split(ref)[1] not in registered or split(ref)[1] not in recipes):
            err(f"ingredient {key}: {ref} is not a Jugcraft item with a recipe")
        values = entry.get("properties", {})
        if not values or set(values) - set(al.AXES) or not all(0 <= v <= al.MAX_PROPERTY for v in values.values()):
            err(f"ingredient {key}: properties must be axes with 0..{al.MAX_PROPERTY} units")
        if f"jei.{MOD}.alchemy.{key}" not in lang:
            err(f"ingredient {key}: missing lang jei.{MOD}.alchemy.{key}")
    plain = [k for k, e in preparations.items() if "tool" not in e]
    if len(plain) != 1:
        err(f"preparations: exactly one must have no tool ({plain})")
    for key, entry in preparations.items():
        tool = entry.get("tool")
        if tool and (split(tool)[1] not in registered or split(tool)[1] not in recipes):
            err(f"preparation {key}: tool {tool} is not a craftable Jugcraft item")
        if not (0 <= entry.get("scale", -1) <= 2 and 0 <= entry.get("ready", -1) <= 1):
            err(f"preparation {key}: scale 0..2, ready 0..1")
    if set(properties) != set(al.AXES) | {"contaminant"}:
        err(f"properties: one for each axis and contaminant ({sorted(properties)})")
    for key, entry in properties.items():
        if not (1 <= entry.get("max_level", 0) <= 5 and 1 <= entry.get("max_ticks", 0) <= 2400
                and 1 <= entry.get("ticks_per_unit", 0) <= 2400 and entry.get("threshold", 0) > 0 and entry.get("per_level", 0) > 0):
            err(f"property {key}: numbers outside the shared effect limits")
        if not re.fullmatch(r"minecraft:[a-z_]+", entry.get("status", "")):
            err(f"property {key}: status must be a vanilla effect id")
        if key == "contaminant" and entry.get("intent") != "harmful":
            err("property contaminant: must be harmful")
        # A brew never outlasts the vanilla potion of the same effect at the highest level it can reach (vanilla's
        # Potions: night vision, fire resistance and water breathing 3600 ticks; regeneration and poison 900, and at
        # level II 450 and 432). Effects with no vanilla potion (resistance, nausea) keep the shared limit above.
        vanilla = {("minecraft:night_vision", 1): 3600, ("minecraft:fire_resistance", 1): 3600,
                   ("minecraft:water_breathing", 1): 3600, ("minecraft:regeneration", 1): 900,
                   ("minecraft:regeneration", 2): 450, ("minecraft:poison", 1): 900, ("minecraft:poison", 2): 432}
        status, top = entry.get("status"), entry.get("max_level", 1)
        bound = vanilla.get((status, top)) if (status, 1) in vanilla else None
        if (status, 1) in vanilla and bound is None:
            err(f"property {key}: {status} has no vanilla potion at level {top}")
        elif bound is not None and entry.get("max_ticks", 0) > bound:
            err(f"property {key}: {entry.get('max_ticks')} ticks outlasts vanilla's level {top} {status} potion ({bound})")
    for ref in al.ALCHEMY_SPECIMENS:
        if ref not in items:
            err(f"alchemy specimen {ref} is not an ingredient")
    for item in ("crucible", "mortar", "stirring_rod", "sampling_spoon", "assay_glass", "formula"):
        if item not in recipes:
            err(f"alchemy: {item} has no recipe")
    # Every line the simulation can say has its words.
    for path in sorted((root / "alchemy").glob("*.java")):
        for key in re.findall(r'Text\.of\("([a-z_.]+)"', path.read_text(encoding="utf-8")):
            if f"compose.{MOD}.{key}" not in lang:
                err(f"concordance/alchemy/{path.name}: missing lang compose.{MOD}.{key}")
    for band in al.BANDS:
        if f"compose.{MOD}.band.{band}" not in lang:
            err(f"alchemy: missing lang compose.{MOD}.band.{band}")
    for axis in al.AXES:
        if f"principle.{MOD}.{axis}" not in lang:
            err(f"alchemy: missing lang principle.{MOD}.{axis}")
    # The crucible's GeckoLib assets.
    geo = load(ASSETS / "geckolib" / "models" / "block" / "crucible.geo.json") or {}
    animations = load(ASSETS / "geckolib" / "animations" / "block" / "crucible.animation.json") or {}
    names = set(animations.get("animations", {}))
    for name in re.findall(r'thenLoop\("([^"]+)"\)', java("CrucibleBlockEntity.java")):
        if name not in names:
            err(f"CrucibleBlockEntity.java: animation {name} is not in crucible.animation.json")
    definition = (geo.get("minecraft:geometry") or [{}])[0]
    width = definition.get("description", {}).get("texture_width", 0)
    height = definition.get("description", {}).get("texture_height", 0)
    bones = {bone["name"] for bone in definition.get("bones", [])}
    for clip in animations.get("animations", {}).values():
        for bone in clip.get("bones", {}):
            if bone not in bones:
                err(f"crucible.animation.json: animates unknown bone {bone}")
    regions = []
    for bone in definition.get("bones", []):
        for cube in bone.get("cubes", []):
            w, h, d = cube["size"]
            u, v = cube["uv"]
            region = (u, v, u + 2 * (w + d), v + d + h, tuple(cube["size"]))
            if region[2] > width or region[3] > height:
                err(f"crucible.geo.json: a cube in {bone['name']} maps outside the {width}x{height} frame")
            regions.append(region)
    for i, a in enumerate(regions):
        for b in regions[i + 1:]:
            if a[:4] != b[:4] and a[0] < b[2] and b[0] < a[2] and a[1] < b[3] and b[1] < a[3]:
                err(f"crucible.geo.json: UV regions {a[:4]} and {b[:4]} overlap")
    sheet = ASSETS / "textures" / "block" / "crucible.png"
    with Image.open(sheet) as image:
        if image.size != (width, height * al.CRUCIBLE_FRAMES):
            err(f"textures/block/crucible.png must be {al.CRUCIBLE_FRAMES} frames of {width}x{height}")
    if "animation" not in (load(sheet.with_name("crucible.png.mcmeta")) or {}):
        err("textures/block/crucible.png needs its animation metadata (GeckoLib animates the liquid's surface)")
    if "CRUCIBLE_ENTITY" not in java("CircleClient.java", CLIENT_JAVA_ROOT):
        err("the crucible must be drawn by GeckoLib on the client")


def check_geckolib(name, java_text, uvs, sizes, kind="block"):
    """A GeckoLib block's (or entity's) assets agree: every animation the Java names exists, every animated bone exists,
    and every cube's box-UV region lies inside the sheet without overlapping another's; the sheet is the size the model
    says."""
    geo = load(ASSETS / "geckolib" / "models" / kind / f"{name}.geo.json") or {}
    animations = load(ASSETS / "geckolib" / "animations" / kind / f"{name}.animation.json") or {}
    clips = set(animations.get("animations", {}))
    for clip in re.findall(r'thenLoop\("([^"]+)"\)', java_text):
        if clip not in clips:
            err(f"{name}: animation {clip} is not in {name}.animation.json")
    definition = (geo.get("minecraft:geometry") or [{}])[0]
    width = definition.get("description", {}).get("texture_width", 0)
    height = definition.get("description", {}).get("texture_height", 0)
    bones = {bone["name"] for bone in definition.get("bones", [])}
    for clip in animations.get("animations", {}).values():
        for bone in clip.get("bones", {}):
            if bone not in bones:
                err(f"{name}.animation.json: animates unknown bone {bone}")
    regions = []
    for bone in definition.get("bones", []):
        for cube in bone.get("cubes", []):
            w, h, d = cube["size"]
            u, v = cube["uv"]
            region = (u, v, u + 2 * (w + d), v + d + h)
            if region[2] > width or region[3] > height:
                err(f"{name}.geo.json: a cube in {bone['name']} maps outside the {width}x{height} sheet")
            regions.append(region)
    for i, a in enumerate(regions):
        for b in regions[i + 1:]:
            if a != b and a[0] < b[2] and b[0] < a[2] and a[1] < b[3] and b[1] < a[3]:
                err(f"{name}.geo.json: UV regions {a} and {b} overlap")
    sheet = ASSETS / "textures" / kind / f"{name}.png"
    if not sheet.is_file():
        err(f"{name}: missing its GeckoLib sheet textures/{kind}/{name}.png")
    else:
        with Image.open(sheet) as image:
            if image.size != (width, height):
                err(f"textures/{kind}/{name}.png must be {width}x{height}")


def check_ecology(co, root, lang, registered, research):
    """Roadmap step 14: the Java mirrors tools/concordance_ecology.py (factors, sampling bounds, devices); every organism
    and disturbance names things that exist and can be grown or made; composting never gives back what growing cost (no
    positive loop); every word the garden says has its text; its icons are their maps; its GeckoLib assets agree."""
    ec = co.ecology
    garden = root / "garden"
    registered = set(registered) | set(co.itemless_blocks())
    def java(name, base=root):
        path = base / name
        return path.read_text(encoding="utf-8") if path.exists() else ""
    constants = {
        "ecology/Habitat.java": {"MAX_NUTRIENTS": ec.MAX_NUTRIENTS},
        "ecology/Sampler.java": {"RADIUS": ec.SAMPLE_RADIUS, "HEIGHT": ec.SAMPLE_HEIGHT, "MAX_DIVERSITY": ec.MAX_DIVERSITY,
                                 "MAX_DISTURBANCE": ec.MAX_DISTURBANCE, "FRESH_TICKS": ec.FRESH_TICKS},
        "ecology/SampleBudget.java": {"PER_TICK": ec.SAMPLES_PER_TICK},
        "ecology/Mulch.java": {"QUARTERS": ec.MULCH_QUARTERS},
        "ecology/Organism.java": {"STAGES": ec.STAGES},
        "garden/Garden.java": {"BONE_MEAL_NUTRIENTS": ec.BONE_MEAL_NUTRIENTS, "AWAKEN_LIMIT": ec.AWAKEN_LIMIT,
                               "WATER_REACH": ec.WATER_REACH},
        "garden/VerdantHeartBlockEntity.java": {"BEAT_TICKS": ec.BEAT_TICKS, "CAPACITY": ec.HEART_CAPACITY},
        "garden/MulchMawBlockEntity.java": {"DIGEST_TICKS": ec.DIGEST_TICKS, "REACH": ec.MAW_REACH},
        "garden/HabitatGaugeBlockEntity.java": {"GAUGE_TICKS": ec.GAUGE_TICKS},
        "garden/GleanerBlockEntity.java": {"GLEAN_TICKS": ec.GLEAN_TICKS, "REACH": ec.GLEAN_REACH, "SLOTS": ec.GLEANER_SLOTS,
                                           "CAPACITY": ec.GLEANER_CAPACITY, "DRAW": ec.GLEANER_DRAW,
                                           "HEART_REACH": ec.HEART_REACH, "COST": ec.GLEAN_COST},
    }
    for name, values in constants.items():
        text = java(name)
        for const, value in values.items():
            if not re.search(rf"\bint {const} = {value};", text):
                err(f"concordance/{name}: {const} differs from tools/concordance_ecology.py ({value})")
    factors = {m[0]: m[1] for m in re.findall(r'^\t[A-Z_]+\("([a-z_]+)", ([A-Za-z_.0-9]+)\)', java("ecology/Factor.java"), re.M)}
    if list(factors) != list(ec.FACTORS):
        err(f"concordance/ecology/Factor.java: factors {list(factors)} differ from FACTORS (same order)")
    if not re.search(r'String ACTIVITY = "' + re.escape(ec.CULTIVATION) + '";', java("garden/Garden.java")):
        err("Garden.ACTIVITY differs from tools/concordance_ecology.py CULTIVATION")
    practised = {rule.get("activity") for block in research.get("verdant_husbandry", {}).get("states", {}).values()
                 for rule in block.get("any", []) if rule.get("type") == "practice"}
    if ec.CULTIVATION not in practised:
        err("Verdant Husbandry does not learn from the cultivation practice")
    # Organisms and disturbances.
    folder = DATA / MOD / "concordance"
    organisms = {p.stem: load(p) or {} for p in (folder / "organism").glob("*.json")}
    disturbances = {p.stem: load(p) or {} for p in (folder / "disturbance").glob("*.json")}
    recipes = {p.stem for p in (DATA / MOD / "recipe").glob("*.json")}
    if set(organisms) != set(ec.ORGANISMS):
        err(f"organisms: {sorted(organisms)} differ from ORGANISMS")
    mulch = {}
    for name, (quarters, values) in ec.MULCH.items():
        for value in values:
            mulch[value] = quarters
    for key, entry in organisms.items():
        block = split(entry.get("block", ":"))[1]
        if block not in registered:
            err(f"organism {key}: block {entry.get('block')} is not registered")
        niche = entry.get("niche", {})
        for factor, values in niche.items():
            top = ec.FACTORS.get(factor)
            if top is None or len(values) != 4 or values != sorted(values) or values[0] < 0 or values[3] > top:
                err(f"organism {key}: niche.{factor} must be [least, ideal from, ideal to, most] within 0..{top}")
        floor = niche.get("nutrients", [0])[0]
        if floor < entry.get("cost", 0):
            err(f"organism {key}: its nutrient floor ({floor}) is below its cost")
        if entry.get("role") != "crop":
            continue
        item = split(entry.get("item", ":"))[1]
        if item not in registered or item not in recipes:
            err(f"organism {key}: {entry.get('item')} is not a Jugcraft item with a recipe (its first seed)")
        for stage in range(ec.STAGES + 1):
            if not (ASSETS / "textures" / "block" / f"{key}_stage{stage}.png").is_file():
                err(f"organism {key}: missing textures/block/{key}_stage{stage}.png")
        if not (DATA / MOD / "loot_table" / "blocks" / f"{block}.json").is_file():
            err(f"organism {key}: missing its loot table")
        # Composting a harvest never gives back what regrowing it cost: no positive loop through the Maw.
        if entry.get("cost", 0) > 0:
            back = entry.get("produce", 0) * mulch.get(entry.get("item"), 0) + entry.get("chaff", 0) * mulch.get(f"{MOD}:verdant_chaff", 0)
            spent = (ec.STAGES - entry.get("replant", 0)) * entry.get("cost", 0) * ec.MULCH_QUARTERS
            if back >= spent:
                err(f"organism {key}: composting its harvest ({back} quarters) returns at least what regrowing cost ({spent})")
        if entry.get("item") not in mulch:
            err(f"organism {key}: its item is not food for a Mulch Maw (#jugcraft:mulch/*)")
    for key, entry in disturbances.items():
        if not (1 <= entry.get("value", 0) <= ec.MAX_DISTURBANCE) or not entry.get("blocks"):
            err(f"disturbance {key}: needs blocks and a value from 1 to {ec.MAX_DISTURBANCE}")
        for ref in entry.get("blocks", []):
            ns, path = split(ref.lstrip("#"))
            if ns == MOD and not ref.startswith("#") and path not in registered:
                err(f"disturbance {key}: {ref} is not a registered block")
    for device in ("verdant_bed", "verdant_heart", "mulch_maw", "habitat_gauge", "gleaner"):
        if device not in recipes:
            err(f"garden: {device} has no recipe")
    # Every word the garden says has its text.
    for path in sorted((root / "ecology").glob("*.java")) + sorted(garden.glob("*.java")):
        text = path.read_text(encoding="utf-8")
        for key in re.findall(r'Text\.of\("([a-z_.]+)"', text):
            if not key.endswith(".") and f"compose.{MOD}.{key}" not in lang:
                err(f"{path.name}: missing lang compose.{MOD}.{key}")
        for key in re.findall(r'(?:status|Garden\.status)\("([a-z_]+)"', text):
            if f"compose.{MOD}.ecology.status.{key}" not in lang:
                err(f"{path.name}: missing lang compose.{MOD}.ecology.status.{key}")
        for key in re.findall(r'"message\.jugcraft\.concordance\.(garden\.[a-z_]+)"', text) + re.findall(r'tell\([a-z]+, "(garden\.[a-z_]+)"', text):
            if f"message.{MOD}.concordance.{key}" not in lang:
                err(f"{path.name}: missing lang message.{MOD}.concordance.{key}")
    for growth in ("thriving", "tolerating", "stalled"):
        if f"compose.{MOD}.ecology.growth.{growth}" not in lang:
            err(f"garden: missing lang compose.{MOD}.ecology.growth.{growth}")
    for factor in ec.FACTORS:
        if f"compose.{MOD}.factor.{factor}" not in lang:
            err(f"garden: missing lang compose.{MOD}.factor.{factor}")
    # The icons are their maps.
    import item_icons
    for icon in ("sunpetal", "dewmoss", "gloamcap", "mendvetch", "verdant_chaff", "verdant_heart", "mulch_maw", "gleaner",
                 "habitat_gauge"):
        texture = ASSETS / "textures" / "item" / f"{icon}.png"
        if not item_icons.has(icon) or not texture.is_file():
            err(f"garden: {icon} needs its map tools/item_icons/{icon}.txt and its texture")
            continue
        with Image.open(texture) as img:
            if img.convert("RGBA").tobytes() != item_icons.draw(icon).tobytes() or img.size != (16, 16):
                err(f"garden: textures/item/{icon}.png differs from its map: run tools/generate_textures.py")
    for name in ("verdant_heart", "mulch_maw", "gleaner"):
        uvs, sizes = ec.SIZES[name]
        entity = {"verdant_heart": "VerdantHeartBlockEntity", "mulch_maw": "MulchMawBlockEntity", "gleaner": "GleanerBlockEntity"}[name]
        check_geckolib(name, java(f"{entity}.java", garden), uvs, sizes)
    if not re.search(r"HEART_ENTITY", java("CircleClient.java", CLIENT_JAVA_ROOT)):
        err("the garden's living devices must be drawn by GeckoLib on the client")


def check_celestial(co, root, lang, registered, research):
    """Roadmap step 15: the Java mirrors tools/concordance_celestial.py (the calendar, the limits, the observatory, the
    astrolabe and the pulse); every pattern keeps to its limits, gives an allowed effect, costs more to recall than to
    attune and has its name; Celestial Attunement can be mastered from patterns that keep to no season; the sky's
    classes read the world's clock and never a client's; every word the sky says has its text; the icons are their maps;
    the observatory's GeckoLib assets agree, and its sheet is painted only in the owner's telescope's colours."""
    ce = co.celestial
    sky = root / "sky"
    def java(name, base=root):
        path = base / name
        return path.read_text(encoding="utf-8") if path.exists() else ""
    constants = {
        "celestial/Calendar.java": {"DAY": ce.DAY, "RECALL_TICKS": ce.RECALL_TICKS, "MAX_FORECAST_DAYS": ce.MAX_FORECAST_DAYS,
                                    "MAX_ATTUNEMENT_TICKS": ce.MAX_ATTUNEMENT_TICKS},
        "celestial/CelestialParser.java": {"MAX_RESONANCE": ce.MAX_RESONANCE, "MAX_COST": ce.MAX_COST, "MAX_PERIOD": ce.MAX_PERIOD},
        "sky/ObservatoryBlockEntity.java": {"CAPACITY": ce.OBSERVATORY_CAPACITY, "GATHER_TICKS": ce.GATHER_TICKS},
        "sky/AstrolabeItem.java": {"CAPACITY": ce.ASTROLABE_CAPACITY},
        "sky/Sky.java": {"PULSE_TICKS": ce.PULSE_TICKS, "EFFECT_TICKS": ce.EFFECT_TICKS, "FORECAST_DAYS": ce.FORECAST_DAYS},
    }
    for name, values in constants.items():
        text = java(name)
        for const, value in values.items():
            if not re.search(rf"\bint {const} = {value};", text):
                err(f"concordance/{name}: {const} differs from tools/concordance_celestial.py ({value})")
    if not re.search(rf'"amplifier", 0, {ce.MAX_AMPLIFIER}\)', java("celestial/CelestialParser.java")):
        err(f"CelestialParser: the amplifier's limit differs from MAX_AMPLIFIER ({ce.MAX_AMPLIFIER})")
    seasons = re.search(r"SEASONS = Set\.of\(([^)]*)\)", java("celestial/Pattern.java"))
    if not seasons or sorted(re.findall(r'"([a-z_]+)"', seasons.group(1))) != sorted(ce.SEASONS):
        err("celestial/Pattern.java: SEASONS differs from tools/concordance_celestial.py SEASONS")
    harvest = (JAVA_ROOT / "agriculture" / "HarvestMoon.java").read_text(encoding="utf-8")
    if f"long DUSK = {ce.DUSK};" not in harvest or f"long DAWN = {ce.DAWN};" not in harvest:
        err("the night the patterns keep differs from the Harvest Moon's (agriculture/HarvestMoon.java DUSK and DAWN)")
    if not re.search(r'String ACTIVITY = "' + re.escape(ce.OBSERVATION) + '";', java("sky/Sky.java")):
        err("Sky.ACTIVITY differs from tools/concordance_celestial.py OBSERVATION")
    if "getOverworldClockTime()" not in java("sky/Sky.java"):
        err("Sky.time must read the overworld's clock (the one the sun and moon follow)")
    for path in sorted(sky.glob("*.java")) + sorted((root / "celestial").glob("*.java")):
        if re.search(r"^import net\.minecraft\.client", path.read_text(encoding="utf-8"), re.M):
            err(f"{path.name}: the sky is decided on the server; it must not read the client's (net.minecraft.client)")
    practice = [rule for block in research.get("celestial_attunement", {}).get("states", {}).values()
                for rule in block.get("any", []) if rule.get("type") == "practice"]
    if not practice or practice[0].get("activity") != ce.OBSERVATION:
        err("Celestial Attunement does not learn from the observation practice")
    elif sum(1 for info in ce.PATTERNS.values() if not info.get("season")) < practice[0].get("distinct", 1):
        err("Celestial Attunement cannot be mastered from patterns that keep to no season")
    # The patterns.
    folder = DATA / MOD / "concordance" / "pattern"
    patterns = {p.stem: load(p) or {} for p in folder.glob("*.json")}
    if set(patterns) != set(ce.PATTERNS):
        err(f"patterns: {sorted(patterns)} differ from PATTERNS")
    for key, entry in patterns.items():
        period, offset = entry.get("period", 0), entry.get("offset", -1)
        attune = entry.get("attunement", {})
        if not (1 <= period <= ce.MAX_PERIOD and 0 <= offset < period):
            err(f"pattern {key}: period must be 1 to {ce.MAX_PERIOD} and its offset within it")
        if not (0 <= entry.get("from", -1) < entry.get("to", 0) <= ce.DAY):
            err(f"pattern {key}: its hours must run forward within a day")
        if not (1 <= entry.get("resonance", 0) <= min(ce.MAX_RESONANCE, ce.OBSERVATORY_CAPACITY)):
            err(f"pattern {key}: resonance must be 1 to {ce.MAX_RESONANCE} (and fit an observatory)")
        if attune.get("effect") not in ce.ATTUNEMENT_EFFECTS or not (0 <= attune.get("amplifier", -1) <= ce.MAX_AMPLIFIER):
            err(f"pattern {key}: its effect must be one of ATTUNEMENT_EFFECTS, amplifier 0 to {ce.MAX_AMPLIFIER}")
        if not (1 <= attune.get("cost", 0) < attune.get("recall", 0) <= min(ce.MAX_COST, ce.ASTROLABE_CAPACITY)):
            err(f"pattern {key}: attuning costs at least 1, recalling more, and an astrolabe must hold the recall")
        if entry.get("season") is not None and entry.get("season") not in ce.SEASONS:
            err(f"pattern {key}: unknown season {entry.get('season')}")
        if f"pattern.{MOD}.{key}" not in lang:
            err(f"pattern {key}: missing lang pattern.{MOD}.{key}")
    recipes = {p.stem for p in (DATA / MOD / "recipe").glob("*.json")}
    for thing in ("observatory", "astrolabe"):
        if thing not in recipes or thing not in registered:
            err(f"sky: {thing} needs a recipe and a registration")
    # Every word the sky says has its text.
    entity = java("ObservatoryBlockEntity.java", sky)
    priority = re.search(r"PRIORITY = List\.of\(([^)]*)\)", entity)
    statuses = set(re.findall(r'"([a-z_]+)"', priority.group(1))) if priority else set()
    if not priority:
        err("ObservatoryBlockEntity: PRIORITY, the statuses in order, is missing")
    statuses |= set(re.findall(r'next = "([a-z_]+)"', entity)) | set(re.findall(r'return "([a-z_]+)"', java("Sky.java", sky)))
    statuses |= set(re.findall(r'\? "([a-z_]+)" : null', java("Sky.java", sky)))
    for status in sorted(statuses):
        if f"compose.{MOD}.sky.status.{status}" not in lang:
            err(f"sky: missing lang compose.{MOD}.sky.status.{status}")
    for path in sorted(sky.glob("*.java")):
        for key in re.findall(r'"message\.jugcraft\.concordance\.(sky\.[a-z_]+)"', path.read_text(encoding="utf-8")):
            if f"message.{MOD}.concordance.{key}" not in lang:
                err(f"{path.name}: missing lang message.{MOD}.concordance.{key}")
    for phase in range(8):
        if f"message.{MOD}.concordance.sky.moon.{phase}" not in lang:
            err(f"sky: missing lang for moon phase {phase}")
    for season in list(ce.SEASONS) + ["off"]:
        if f"message.{MOD}.concordance.sky.season.{season}" not in lang:
            err(f"sky: missing lang for season {season}")
    # The icons are their maps.
    import item_icons
    for icon in ("astrolabe", "observatory"):
        texture = ASSETS / "textures" / "item" / f"{icon}.png"
        if not item_icons.has(icon) or not texture.is_file():
            err(f"sky: {icon} needs its map tools/item_icons/{icon}.txt and its texture")
            continue
        with Image.open(texture) as img:
            if img.convert("RGBA").tobytes() != item_icons.draw(icon).tobytes() or img.size != (16, 16):
                err(f"sky: textures/item/{icon}.png differs from its map: run tools/generate_textures.py")
    uvs, sizes = ce.SIZES["observatory"]
    check_geckolib("observatory", entity, uvs, sizes)
    if not re.search(r"OBSERVATORY_ENTITY", java("CircleClient.java", CLIENT_JAVA_ROOT)):
        err("the observatory must be drawn by GeckoLib on the client")
    import concordance_celestial_art as art
    allowed = art.owner_colours() | art.EXTENDED
    sheet = ASSETS / "textures" / "block" / "observatory.png"
    if sheet.is_file():
        with Image.open(sheet) as img:
            used = {pixel[:3] for _count, pixel in img.convert("RGBA").getcolors(maxcolors=65536) if pixel[3] > 0}
        if used - allowed:
            err(f"textures/block/observatory.png: colours not from the owner's telescope: {sorted(used - allowed)[:4]}")
    # LambDynamicLights (optional): the astrolabe's glow keys on a component the sky registers.
    light = load(ASSETS / "dynamiclights" / "item" / "astrolabe.json") or {}
    for component in light.get("match", {}).get("components", {}):
        if f'"{split(component)[1]}"' not in java("Sky.java", sky):
            err(f"dynamiclights/item/astrolabe.json: component {component} is not registered in Sky")


def check_workers(co, root, lang, registered, research):
    """Roadmap step 17: the Java mirrors tools/concordance_workers.py (the bond, navigation, parser limits, the roster
    and the statuses, in order); every worker definition is its table's and has its entity, name and GeckoLib assets;
    every status a worker can give has its text, and the inactive reasons the brief names are there; mastery comes from
    each kind's service; the brains keep no saved state and no worker loads a chunk or uses a portal; every word has its
    text; the icons are their maps."""
    wk = co.workers
    spirits = root / "spirits"
    def java(name, base=root):
        path = base / name
        return path.read_text(encoding="utf-8") if path.exists() else ""
    constants = {
        "worker/Bond.java": {"MAX": wk.BOND_MAX, "GAIN": wk.BOND_GAIN, "DAILY_GAIN": wk.BOND_DAILY_GAIN,
                             "NEGLECT": wk.BOND_NEGLECT, "FIRST": wk.BOND_FIRST, "SECOND": wk.BOND_SECOND},
        "worker/Navigation.java": {"GIVE_UP": wk.GIVE_UP, "RETRY_TICKS": wk.RETRY_TICKS},
        "worker/WorkerParser.java": {"MAX_RADIUS": wk.MAX_RADIUS, "MAX_CARRY": wk.MAX_CARRY, "MAX_QUOTA": wk.MAX_QUOTA,
                                     "MAX_SUPPORT_TICKS": wk.MAX_SUPPORT_TICKS},
        "spirits/WorkerEntity.java": {"THINK_TICKS": wk.THINK_TICKS},
        "spirits/WorkerRoster.java": {"MAX_PER_PLAYER": wk.ROSTER_MAX},
    }
    for name, values in constants.items():
        text = java(name)
        for const, value in values.items():
            if not re.search(rf"\bint {const} = {value};", text):
                err(f"concordance/{name}: {const} differs from tools/concordance_workers.py ({value})")
    if f"long VISIT_TICKS = {wk.VISIT_TICKS}L;" not in java("HearthlingEntity.java", spirits):
        err("spirits/HearthlingEntity.java: VISIT_TICKS differs from tools/concordance_workers.py")
    work = re.search(r"WORK = List\.of\(([^)]*)\)", java("worker/WorkerParser.java"))
    if not work or re.findall(r'"([a-z_]+)"', work.group(1)) != wk.WORK:
        err("worker/WorkerParser.java: WORK differs from tools/concordance_workers.py")
    statuses = re.findall(r'^\t[A-Z_]+\("([a-z_]+)", (?:true|false)\)', java("worker/Status.java"), re.M)
    if statuses != list(wk.STATUSES):
        err(f"worker/Status.java: statuses {statuses} differ from STATUSES (same order)")
    for reason in ("waiting_for_resources", "blocked_by_access", "cannot_navigate", "outside_agreement", "finished"):
        if reason not in statuses:
            err(f"worker/Status.java: a worker must be able to say {reason}")
    for status in statuses:
        if f"compose.{MOD}.worker.status.{status}" not in lang:
            err(f"workers: missing lang compose.{MOD}.worker.status.{status}")
    if not re.search(r'String ACTIVITY = "' + re.escape(wk.SERVICE) + '";', java("Workers.java", spirits)):
        err("Workers.ACTIVITY differs from tools/concordance_workers.py SERVICE")
    practice = [rule for block in research.get("binding_arts", {}).get("states", {}).values()
                for rule in block.get("any", []) if rule.get("type") == "practice"]
    if not practice or practice[0].get("activity") != wk.SERVICE or practice[0].get("distinct", 0) != 3:
        err("the Binding Arts must be mastered by the service of all three kinds (practice, distinct 3)")
    # The definitions, their entities and their assets.
    folder = DATA / MOD / "concordance" / "worker"
    defined = {p.stem: load(p) or {} for p in folder.glob("*.json")}
    if set(defined) != set(wk.WORKERS):
        err(f"concordance/worker: {sorted(defined)} differ from WORKERS")
    kinds = {info["kind"] for info in wk.WORKERS.values()}
    if kinds != {"familiar", "spirit", "construct"}:
        err(f"workers: the three kinds need a definition each, found {sorted(kinds)}")
    registry = java("Workers.java", spirits)
    client = java("WorkerClient.java", CLIENT_JAVA_ROOT)
    classes = {"hearthling": "HearthlingEntity", "gathering_shade": "GatheringShadeEntity", "clockwork_porter": "ClockworkPorterEntity"}
    for key, info in wk.WORKERS.items():
        if defined.get(key, {}).get("kind") != info["kind"]:
            err(f"concordance/worker/{key}.json: differs from WORKERS (run tools/generate_material_data.py)")
        if f'entity("{key}"' not in registry:
            err(f"workers: {key} needs its entity type in Workers")
        if f"entity.{MOD}.{key}" not in lang:
            err(f"workers: missing lang entity.{MOD}.{key}")
        if f"Workers.{key.upper()}" not in client:
            err(f"workers: {key} must be drawn by GeckoLib on the client (WorkerClient)")
        source = java(f"{classes.get(key, key)}.java", spirits)
        if f'DEFINITION = "{MOD}:{key}"' not in source:
            err(f"spirits/{classes.get(key, key)}.java: must read its definition {MOD}:{key}")
        check_geckolib(key, source, None, None, kind="entity")
    # The brains keep no saved state; no worker loads a chunk, or follows anyone through a portal.
    for path in sorted(spirits.glob("*.java")):
        text = path.read_text(encoding="utf-8")
        for banned, why in (("setChunkForced", "loads a chunk"), ("addRegionTicket", "loads a chunk"),
                            ("TicketType", "loads a chunk"), ("changeDimension", "crosses dimensions"),
                            ("TeleportTransition", "crosses dimensions")):
            if banned in text:
                err(f"spirits/{path.name}: a worker never {why} ({banned})")
        # A familiar may hop beside its person in the same level: teleportTo(x, y, z), never the level-taking form.
        for call in re.findall(r"teleportTo\(([^;]*)\);", text):
            if call.count(",") != 2:
                err(f"spirits/{path.name}: a worker only hops within its own level (teleportTo(x, y, z))")
        if "MemoryModuleType" in text and path.name != "WorkerEntity.java":
            err(f"spirits/{path.name}: only WorkerEntity talks to the brain; a worker's model never lives in its memories")
    # Every word has its text.
    for path in sorted(spirits.glob("*.java")):
        for key in re.findall(r'"message\.jugcraft\.concordance\.(workers\.[a-z_]+)"', path.read_text(encoding="utf-8")):
            if not key.endswith(".") and f"message.{MOD}.concordance.{key}" not in lang:
                err(f"{path.name}: missing lang message.{MOD}.concordance.{key}")
    for kind in sorted(kinds):
        if f"message.{MOD}.concordance.workers.kind.{kind}" not in lang:
            err(f"workers: missing lang for the kind {kind}")
    jade = java("compat/jade/WorkerDataProvider.java", JAVA_ROOT) + java("compat/JugcraftJadeClient.java", CLIENT_JAVA_ROOT)
    for key in re.findall(r'"tooltip\.jugcraft\.concordance\.(jade\.[a-z_]+)"', jade):
        if f"tooltip.{MOD}.concordance.{key}" not in lang:
            err(f"Jade: missing lang tooltip.{MOD}.concordance.{key}")
    if "WorkerDataProvider" not in java("compat/jade/JugcraftJadePlugin.java", JAVA_ROOT):
        err("Jade: the worker provider must be registered in JugcraftJadePlugin")
    for thing in list(wk.ITEMS) + list(wk.BLOCKS):
        if thing not in registered or not (DATA / MOD / "recipe" / f"{thing}.json").is_file():
            err(f"workers: {thing} needs a registration and a recipe")
    import item_icons
    for icon in list(wk.ITEMS) + list(wk.BLOCKS):
        texture = ASSETS / "textures" / "item" / f"{icon}.png"
        if not item_icons.has(icon) or not texture.is_file():
            err(f"workers: {icon} needs its map tools/item_icons/{icon}.txt and its texture")
            continue
        with Image.open(texture) as img:
            if img.convert("RGBA").tobytes() != item_icons.draw(icon).tobytes() or img.size != (16, 16):
                err(f"workers: textures/item/{icon}.png differs from its map: run tools/generate_textures.py")


def check_logistics(co, root, lang, registered):
    """Roadmap step 18: the Java ledger and post numbers equal tools/concordance_logistics.py; the request states are
    the generator's, in order; every event kind, note and refusal the Java can name has its text; the post has its
    registration, recipe, assets and icon; and a courier's work moves items only through CourierLedger, whose moves are
    Transfer API transactions committed with their ledger step."""
    lg = co.logistics
    def java(name, base=root):
        path = base / name
        return path.read_text(encoding="utf-8") if path.exists() else ""
    ledger = java("logistics/Logistics.java")
    for const, value in (("MAX_WANTED", lg.MAX_WANTED), ("MAX_OPEN_PER_PLAYER", lg.MAX_OPEN_PER_PLAYER), ("MAX_OPEN", lg.MAX_OPEN),
                         ("HISTORY", lg.HISTORY)):
        if not re.search(rf"\bint {const} = {value};", ledger):
            err(f"concordance/logistics/Logistics.java: {const} differs from tools/concordance_logistics.py ({value})")
    for const, value in (("LEASE_TICKS", lg.LEASE_TICKS), ("RETRY_TICKS", lg.RETRY_TICKS)):
        if f"long {const} = {value}L;" not in ledger:
            err(f"concordance/logistics/Logistics.java: {const} differs from tools/concordance_logistics.py ({value})")
    couriers = java("courier/Couriers.java")
    for const, value in (("SOURCE_RADIUS", lg.SOURCE_RADIUS), ("SHOWN_HISTORY", lg.SHOWN_HISTORY)):
        if not re.search(rf"\bint {const} = {value};", couriers):
            err(f"concordance/courier/Couriers.java: {const} differs from tools/concordance_logistics.py ({value})")
    if not re.search(rf"\bint SLOTS = {lg.POST_SLOTS};", java("courier/CourierPostBlockEntity.java")):
        err("concordance/courier/CourierPostBlockEntity.java: SLOTS differs from tools/concordance_logistics.py POST_SLOTS")
    states = re.findall(r'^\t[A-Z_]+\("([a-z_]+)"\)', java("logistics/RequestState.java"), re.M)
    if states != list(lg.STATES):
        err(f"concordance/logistics/RequestState.java: states {states} differ from STATES (same order)")
    for state in states:
        if f"compose.{MOD}.courier.state.{state}" not in lang:
            err(f"logistics: missing lang for the state {state}")
    # Event kinds and notes: every word the ledger and the courier write into a request or its history.
    for word in set(re.findall(r'"([a-z_]+)"', ledger)):
        if word not in lg.EVENTS and word not in lg.NOTES:
            err(f"concordance/logistics/Logistics.java: '{word}' is neither an event in EVENTS nor a note in NOTES")
    porter = java("spirits/ClockworkPorterEntity.java")
    for word in set(re.findall(r'release\([^;]*?"([a-z_]+)"', porter + java("courier/CourierLedger.java"))):
        if word not in lg.NOTES:
            err(f"logistics: the release note '{word}' has no text in NOTES")
    for key in lg.EVENTS:
        if f"message.{MOD}.concordance.courier.event.{key}" not in lang:
            err(f"logistics: missing lang for the event {key}")
    for key in lg.NOTES:
        if f"compose.{MOD}.courier.note.{key}" not in lang:
            err(f"logistics: missing lang for the note {key}")
    outcomes = re.search(r"enum Outcome \{([^}]*)\}", ledger)
    for outcome in (re.findall(r"[A-Z_]+", outcomes.group(1)) if outcomes else []):
        if outcome != "DONE" and f"message.{MOD}.concordance.courier.refused.{outcome.lower()}" not in lang:
            err(f"logistics: missing lang for the refusal {outcome.lower()}")
    for path in sorted((root / "courier").glob("*.java")) + [root / "spirits" / "ClockworkPorterEntity.java", root / "spirits" / "PorterKeyItem.java"]:
        for key in re.findall(r'"message\.jugcraft\.concordance\.(courier\.[a-z_.]+)"', path.read_text(encoding="utf-8")):
            if not key.endswith(".") and f"message.{MOD}.concordance.{key}" not in lang:
                err(f"{path.name}: missing lang message.{MOD}.concordance.{key}")
    # A courier's work moves items only through CourierLedger; CourierLedger moves them only in committed transactions.
    work = porter.split("courier work (roadmap step 18)", 1)[-1].split("Draws Ley Charge", 1)[0]
    for move in (".insert(", ".extract(", "removeItem(", "setItem(", "insert(container"):
        if move in work:
            err(f"spirits/ClockworkPorterEntity.java: courier work moves items itself ({move}); use CourierLedger")
    if java("courier/CourierLedger.java").count("transaction.commit()") < 2:
        err("concordance/courier/CourierLedger.java: pickups and deliveries must commit their transactions with the ledger step")
    # The post.
    if "courier_post" not in registered or not (DATA / MOD / "recipe" / "courier_post.json").is_file():
        err("logistics: courier_post needs a registration and a recipe")
    for face in ("side", "top", "bottom"):
        if not (ASSETS / "textures" / "block" / f"courier_post_{face}.png").is_file():
            err(f"logistics: missing textures/block/courier_post_{face}.png")
    import item_icons
    texture = ASSETS / "textures" / "item" / "courier_post.png"
    if not item_icons.has("courier_post") or not texture.is_file():
        err("logistics: courier_post needs its map tools/item_icons/courier_post.txt and its texture")
    else:
        with Image.open(texture) as img:
            if img.convert("RGBA").tobytes() != item_icons.draw("courier_post").tobytes() or img.size != (16, 16):
                err("logistics: textures/item/courier_post.png differs from its map: run tools/generate_textures.py")


def check_artifice(co, root, lang, registered, research):
    """Roadmap step 19: the Java mirrors tools/concordance_artifice.py (qualities, limits, costs, tools); the definitions
    are the generator's; salvage always gives back less than forging costs; no item is two things at the bench; no ring
    can add more of an attribute than its cap, whatever is rolled, inscribed and set; every word has its text; rings are
    Trinkets rings that give their modifiers only through Trinkets (exactly once); the icons are their maps."""
    ar = co.artifice
    def java(name, base=root):
        path = base / name
        return path.read_text(encoding="utf-8") if path.exists() else ""
    if not re.search(rf"\bint BOND_CAPACITY = {ar.BOND_CAPACITY};", java("artifice/Forge.java")):
        err("concordance/artifice/Forge.java: BOND_CAPACITY differs from tools/concordance_artifice.py")
    qualities = {m[0]: (int(m[1]), int(m[2]), int(m[3])) for m in re.findall(r'^\t[A-Z]+\("([a-z]+)", (-?\d+), (\d+), (\d+)\)',
                                                                          java("artifice/Quality.java"), re.M)}
    if qualities != ar.QUALITIES or sum(weight for _b, _a, weight in ar.QUALITIES.values()) != 100:
        err(f"concordance/artifice/Quality.java: qualities {qualities} differ from QUALITIES (weights must total 100)")
    for const, value in (("MAX_COST", ar.MAX_COST), ("MAX_CAPACITY", ar.MAX_CAPACITY), ("MAX_SOCKETS", ar.MAX_SOCKETS),
                         ("MAX_RUNES", ar.MAX_RUNES), ("MAX_PART_COST", ar.MAX_PART_COST)):
        if not re.search(rf"\bint {const} = {value};", java("artifice/ArtificeParser.java")):
            err(f"concordance/artifice/ArtificeParser.java: {const} differs from tools/concordance_artifice.py ({value})")
    smithy = java("smithy/Artificery.java")
    for const, value in (("FORGE_FOCUS", ar.FORGE_FOCUS), ("REFORGE_FOCUS", ar.REFORGE_FOCUS), ("INSCRIBE_FOCUS", ar.INSCRIBE_FOCUS),
                         ("BOND_FOCUS", ar.BOND_FOCUS), ("CONFIRM_TICKS", ar.CONFIRM_TICKS), ("WEAR_TICKS", ar.WEAR_TICKS)):
        if not re.search(rf"\bint {const} = {value};", smithy):
            err(f"concordance/smithy/Artificery.java: {const} differs from tools/concordance_artifice.py ({value})")
    tools = {"REFORGE_CATALYST": ar.REFORGE_CATALYST, "UNSOCKET_TOOL": ar.UNSOCKET_TOOL, "SALVAGE_TOOL": ar.SALVAGE_TOOL,
             "BOND_TOOL": ar.BOND_TOOL}
    for const, item in tools.items():
        if f"Item {const} = Items.{split(item)[1].upper()};" not in smithy:
            err(f"concordance/smithy/Artificery.java: {const} differs from tools/concordance_artifice.py ({item})")
    if not re.search(r'String ACTIVITY = "' + re.escape(ar.ARTIFICE_PRACTICE) + '";', smithy):
        err("Artificery.ACTIVITY differs from tools/concordance_artifice.py ARTIFICE_PRACTICE")
    practice = [rule for block in research.get("runesmithing", {}).get("states", {}).values()
                for rule in block.get("any", []) if rule.get("type") == "practice"]
    if not practice or practice[0].get("activity") != ar.ARTIFICE_PRACTICE:
        err("Runesmithing must be mastered by the artifice practice")
    # The definitions are the generator's.
    folder = DATA / MOD / "concordance"
    for kind, table in (("substrate", ar.SUBSTRATES), ("gem", ar.GEMS), ("rune", ar.RUNES), ("affix", ar.AFFIXES)):
        found = {p.stem for p in (folder / kind).glob("*.json")}
        if found != set(table):
            err(f"concordance/{kind}: {sorted(found)} differ from the generator's {sorted(table)}")
    # No gain, and nothing is two things at the bench.
    bench_items = {}
    for key, info in ar.SUBSTRATES.items():
        if info["salvage"] >= info["cost"]:
            err(f"substrate {key}: salvage ({info['salvage']}) must give back less than forging costs ({info['cost']})")
        bench_items.setdefault(info["item"], []).append(f"substrate {key}")
    for key, info in ar.GEMS.items():
        bench_items.setdefault(info["item"], []).append(f"gem {key}")
    for key, info in ar.RUNES.items():
        bench_items.setdefault(info["item"], []).append(f"rune {key}")
    for item in tools.values():
        bench_items.setdefault(item, []).append("a bench tool")
    for item, uses in bench_items.items():
        if len(uses) > 1:
            err(f"artifice: {item} is {' and '.join(uses)} at the bench")
    # The most of each attribute a ring of each substrate could add.
    for key, info in ar.SUBSTRATES.items():
        most = {}
        best_affix = {}
        for affix in info["affixes"]:
            a = ar.AFFIXES[affix]
            best_affix[a["attribute"]] = max(best_affix.get(a["attribute"], 0.0), a["max"])
        for attribute, value in best_affix.items():
            most[attribute] = most.get(attribute, 0.0) + value
        runes = {}
        for rune in ar.RUNES.values():
            if not rune.get("substrates") or key in rune["substrates"]:
                runes.setdefault(rune["stat"]["attribute"], []).append(rune["stat"]["amount"])
        for attribute, amounts in runes.items():
            most[attribute] = most.get(attribute, 0.0) + sum(sorted(amounts, reverse=True)[:info["runes"]])
        for gem in ar.GEMS.values():
            attribute = gem["stat"]["attribute"]
            most[attribute] = most.get(attribute, 0.0) + gem["stat"]["amount"] * info["sockets"]
        for attribute, value in most.items():
            if attribute not in ar.ATTRIBUTE_CAPS:
                err(f"artifice: {attribute} has no cap in ATTRIBUTE_CAPS")
            elif value > ar.ATTRIBUTE_CAPS[attribute] + 1e-9:
                err(f"artifice: a {key} ring could add {value} {attribute}, above its cap {ar.ATTRIBUTE_CAPS[attribute]}")
    # Every word has its text.
    for word in set(re.findall(r'Result\.no\("([a-z_]+)"\)|Removal\([^;]*"([a-z_]+)"\)', java("artifice/Forge.java"))):
        for refusal in word:
            if refusal and f"compose.{MOD}.artifice.refusal.{refusal}" not in lang:
                err(f"artifice: missing lang for the refusal {refusal}")
    for refusal in re.findall(r'refuse\(player, "([a-z_]+)"\)', smithy):
        if f"compose.{MOD}.artifice.refusal.{refusal}" not in lang:
            err(f"artifice: missing lang for the refusal {refusal}")
    processes = re.findall(r'^\t\t[A-Z]+\("([a-z]+)", EnumSet', java("artifice/Forge.java"), re.M)
    if sorted(processes) != sorted(ar.PROCESSES):
        err(f"artifice: processes {processes} differ from PROCESSES")
    for key in list(ar.QUALITIES):
        if f"compose.{MOD}.artifice.quality.{key}" not in lang:
            err(f"artifice: missing lang for the quality {key}")
    for table, kind in ((ar.SUBSTRATES, "substrate"), (ar.AFFIXES, "affix"), (ar.GEMS, "gem"), (ar.RUNES, "rune")):
        for key in table:
            if f"compose.{MOD}.artifice.{kind}.{key}" not in lang:
                err(f"artifice: missing lang for the {kind} {key}")
    for attribute in {a["attribute"] for a in ar.AFFIXES.values()}:
        if f"compose.{MOD}.artifice.attribute.{attribute.replace(':', '.')}" not in lang:
            err(f"artifice: missing lang for the attribute {attribute}")
    for path in sorted((root / "smithy").glob("*.java")):
        for key in re.findall(r'"message\.jugcraft\.concordance\.(artifice\.[a-z_.]+)"', path.read_text(encoding="utf-8")):
            if not key.endswith(".") and f"message.{MOD}.concordance.{key}" not in lang:
                err(f"{path.name}: missing lang message.{MOD}.concordance.{key}")
        for key in re.findall(r'"tooltip\.jugcraft\.concordance\.(artifice\.[a-z_.]+)"', path.read_text(encoding="utf-8")):
            if f"tooltip.{MOD}.concordance.{key}" not in lang:
                err(f"{path.name}: missing lang tooltip.{MOD}.concordance.{key}")
    # Rings are Trinkets rings, and their modifiers come only through Trinkets (exactly once on equip and unequip).
    ring = java("smithy/ResonantRingItem.java")
    if "implements TrinketCallback" not in ring or "forEachTrinketModifier" not in ring:
        err("smithy/ResonantRingItem.java: a ring gives its modifiers through Trinkets' TrinketCallback")
    if "ATTRIBUTE_MODIFIERS" in smithy + ring:
        err("smithy: a ring must not carry vanilla attribute modifiers too (they would apply in the hand as well)")
    slot = load(ROOT / "src" / "main" / "resources" / "data" / "trinkets" / "tags" / "item" / "hand" / "ring.json") or {}
    if rid_value("resonant_ring") not in slot.get("values", []):
        err("data/trinkets/tags/item/hand/ring.json must list jugcraft:resonant_ring")
    entities = load(ROOT / "src" / "main" / "resources" / "data" / "trinkets" / "entities" / f"{MOD}.json") or {}
    if "hand/ring" not in entities.get("slots", []) or "player" not in entities.get("entities", []):
        err("data/trinkets/entities/jugcraft.json must give players the hand/ring slot")
    for thing in list(ar.ITEMS) + list(ar.BLOCKS):
        if thing not in registered:
            err(f"artifice: {thing} is not registered")
    if not (DATA / MOD / "recipe" / "artificer_bench.json").is_file():
        err("artifice: the Artificer's Bench needs a recipe")
    for face in ("side", "top", "bottom"):
        if not (ASSETS / "textures" / "block" / f"artificer_bench_{face}.png").is_file():
            err(f"artifice: missing textures/block/artificer_bench_{face}.png")
    import item_icons
    for icon in list(ar.ITEMS) + list(ar.BLOCKS):
        texture = ASSETS / "textures" / "item" / f"{icon}.png"
        if not item_icons.has(icon) or not texture.is_file():
            err(f"artifice: {icon} needs its map tools/item_icons/{icon}.txt and its texture")
            continue
        with Image.open(texture) as img:
            if img.convert("RGBA").tobytes() != item_icons.draw(icon).tobytes() or img.size != (16, 16):
                err(f"artifice: textures/item/{icon}.png differs from its map: run tools/generate_textures.py")


def check_relics(co, root, lang, registered, research):
    """Roadmap step 20: the Java mirrors tools/concordance_relics.py (the budgets, limits, contexts and the shrine's
    numbers); the definitions are the generator's, each relic works somewhere and never carried loose or worn for show,
    gives only an allowed effect within the limits, and can be made and recharged; nothing inside a container is ever
    looked at and relic charge never turns back into Ley Charge; every reason, context, mode and message has its text;
    the shrine's GeckoLib animations are the ones its block entity plays; Jade, Trinkets and the icons agree."""
    rl = co.relics
    def java(name, base=root):
        path = base / name
        return path.read_text(encoding="utf-8") if path.exists() else ""
    reliquary = java("reliquary/Reliquary.java")
    shrine = java("reliquary/ReliquaryShrineBlockEntity.java")
    constants = {
        "relic/Relics.java": {"BUDGET": rl.BUDGET, "SHRINE_BUDGET": rl.SHRINE_BUDGET, "BUDGET_TICKS": rl.BUDGET_TICKS,
                              "CALM_TICKS": rl.CALM_TICKS},
        "relic/RelicParser.java": {"MAX_CAPACITY": rl.MAX_CAPACITY, "MAX_RANGE": rl.MAX_RANGE, "MIN_INTERVAL": rl.MIN_INTERVAL,
                                   "MAX_INTERVAL": rl.MAX_INTERVAL, "MAX_DURATION": rl.MAX_DURATION, "MAX_AMPLIFIER": rl.MAX_AMPLIFIER},
        "reliquary/Reliquary.java": {"CHECK_TICKS": rl.CHECK_TICKS, "CHARGE_PER_LEY": rl.CHARGE_PER_LEY,
                                     "RECHARGE_PER_SECOND": rl.RECHARGE_PER_SECOND, "PYLON_REACH": rl.PYLON_REACH,
                                     "MAX_TARGETS": rl.MAX_TARGETS},
    }
    for name, values in constants.items():
        text = java(name)
        for const, value in values.items():
            if not re.search(rf"\bint {const} = {value};", text):
                err(f"concordance/{name}: {const} differs from tools/concordance_relics.py ({value})")
    contexts = re.findall(r'^\t[A-Z_]+\("([a-z_]+)"\)', java("relic/Context.java"), re.M)
    if contexts != list(rl.CONTEXTS):
        err(f"relic/Context.java: contexts {contexts} differ from CONTEXTS (same order)")
    ids = re.search(r"RELIC_IDS = List\.of\(([^)]*)\)", reliquary)
    if not ids or re.findall(r'"([a-z_]+)"', ids.group(1)) != rl.RELIC_IDS:
        err("reliquary/Reliquary.java: RELIC_IDS differs from tools/concordance_relics.py")
    if not re.search(r'String ACTIVITY = "' + re.escape(rl.RELIC_PRACTICE) + '";', reliquary):
        err("Reliquary.ACTIVITY differs from tools/concordance_relics.py RELIC_PRACTICE")
    if f'String RESEARCH = "{MOD}:relic_lore";' not in reliquary:
        err("Reliquary.RESEARCH must be jugcraft:relic_lore")
    practice = [rule for block in research.get("relic_lore", {}).get("states", {}).values()
                for rule in block.get("any", []) if rule.get("type") == "practice"]
    if not practice or practice[0].get("activity") != rl.RELIC_PRACTICE:
        err("Relic Lore must be mastered by the relic practice")
    elif len({c for info in rl.RELICS.values() for m in info["modes"] for c in m["contexts"]}) < practice[0].get("distinct", 1):
        err("Relic Lore's mastery asks for more different contexts than the relics have")
    # The definitions are the generator's, and each keeps to the rules.
    folder = DATA / MOD / "concordance" / "relic"
    found = {p.stem: load(p) or {} for p in folder.glob("*.json")}
    if set(found) != set(rl.RELICS):
        err(f"concordance/relic: {sorted(found)} differ from RELICS")
    recipes = {p.stem for p in (DATA / MOD / "recipe").glob("*.json")}
    for key, info in rl.RELICS.items():
        entry = found.get(key, {})
        if entry.get("modes") != info["modes"] or entry.get("capacity") != info["capacity"] or entry.get("owned") != info["owned"]:
            err(f"concordance/relic/{key}.json differs from the generator's: run tools/generate_material_data.py")
        if key not in rl.RELIC_IDS or key not in registered or key not in recipes:
            err(f"relic {key}: needs its item id in RELIC_IDS, a registration and a recipe")
        if not 1 <= info["capacity"] <= rl.MAX_CAPACITY:
            err(f"relic {key}: capacity must be 1 to {rl.MAX_CAPACITY}")
        seen = set()
        for m in info["modes"]:
            where = set(m["contexts"])
            if not where or where & {"inventory", "cosmetic"} or not where <= set(rl.CONTEXTS) or where & seen:
                err(f"relic {key} mode {m['id']}: contexts must be working ones, each in one mode only")
            seen |= where
            if m["status"] not in rl.RELIC_EFFECTS or not 0 <= m["amplifier"] <= rl.MAX_AMPLIFIER:
                err(f"relic {key} mode {m['id']}: its status must be one of RELIC_EFFECTS, amplifier 0 to {rl.MAX_AMPLIFIER}")
            if not (20 <= m["duration"] <= rl.MAX_DURATION and rl.MIN_INTERVAL <= m["interval"] <= rl.MAX_INTERVAL
                    and 1 <= m["cost"] <= info["capacity"] and 0 <= m["range"] <= rl.MAX_RANGE):
                err(f"relic {key} mode {m['id']}: its duration, interval, cost or range is out of bounds")
            if (m["target"] == "self") != (m["range"] == 0) or (m["target"] == "self" and "installed" in where):
                err(f"relic {key} mode {m['id']}: a self mode has no range and is never installed; the others need one")
            if m["interval"] % rl.CHECK_TICKS:
                err(f"relic {key} mode {m['id']}: its interval should be a whole number of checks ({rl.CHECK_TICKS} ticks)")
            if f"compose.{MOD}.relic.mode.{m['id']}" not in lang:
                err(f"relic {key}: missing lang for the mode {m['id']}")
        if f"tooltip.{MOD}.{key}" not in lang or f"item.{MOD}.{key}" not in lang:
            err(f"relic {key}: missing its name or tooltip")
    # Nothing inside a container is ever looked at, and charge never becomes Ley Charge again.
    for forbidden in ("CONTAINER", "BUNDLE_CONTENTS", "getEnderChestInventory"):
        if forbidden in reliquary:
            err(f"reliquary/Reliquary.java: relics are never looked for inside containers ({forbidden})")
    for path in sorted((root / "reliquary").glob("*.java")):
        if re.search(r"\.fill\(|\.setLey\(", path.read_text(encoding="utf-8")):
            err(f"{path.name}: relic charge must never be turned back into Ley Charge")
    if "Cause.Origin.SHRINE" not in shrine or "Reliquary.give(" not in shrine or "ConcordanceEffects.apply(" not in reliquary:
        err("reliquary: relics give their effects only through the shared effect boundary (shrines as SHRINE causes)")
    # Every word has its text.
    reasons = set(re.findall(r'new Verdict\((?:null|mode), (?:context == Context\.INSTALLED \? "[a-z_]+" : )?"([a-z_]+)"', java("relic/Relics.java")))
    reasons |= set(re.findall(r'"(cannot_install)"', java("relic/Relics.java")))
    reasons |= set(re.findall(r'new Outcome\([^;]*?"([a-z_]+)", 0\)', reliquary))
    reasons |= set(re.findall(r'return "([a-z_]+)";', shrine)) | set(re.findall(r'next = (?:definition == null \? )?"([a-z_]+)"', shrine))
    reasons -= {"working", "empty", ""}
    for reason in sorted(reasons | set(rl.REASONS)):
        if f"compose.{MOD}.relic.reason.{reason}" not in lang:
            err(f"relics: missing lang for the reason {reason}")
    if not reasons <= set(rl.REASONS):
        err(f"relics: reasons {sorted(reasons - set(rl.REASONS))} are not in REASONS")
    for context in rl.CONTEXTS:
        if f"compose.{MOD}.relic.context.{context}" not in lang:
            err(f"relics: missing lang for the context {context}")
    sources = sorted((root / "reliquary").glob("*.java")) + [JAVA_ROOT / "compat" / "jade" / "ShrineDataProvider.java"]
    client = ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client" / "compat" / "JugcraftJadeClient.java"
    for path in sources + [client]:
        text = path.read_text(encoding="utf-8") if path.exists() else ""
        for kind, key in re.findall(r'"(message|tooltip|compose)\.jugcraft\.((?:concordance\.)?(?:relic|jade\.shrine)[a-z_.]*)"', text):
            if not key.endswith(".") and f"{kind}.{MOD}.{key}" not in lang:
                err(f"{path.name}: missing lang {kind}.{MOD}.{key}")
    # The shrine: its GeckoLib animations are the ones it plays; Jade shows it; Trinkets gives the necklace slot.
    animations = load(ASSETS / "geckolib" / "animations" / "block" / "reliquary_shrine.animation.json") or {}
    for name in re.findall(r'thenLoop\("([a-z_.]+)"\)', shrine):
        if name not in animations.get("animations", {}):
            err(f"reliquary_shrine.animation.json: missing {name}")
    if not (ASSETS / "geckolib" / "models" / "block" / "reliquary_shrine.geo.json").is_file():
        err("relics: missing the shrine's GeckoLib model")
    plugin = (JAVA_ROOT / "compat" / "jade" / "JugcraftJadePlugin.java").read_text(encoding="utf-8")
    if "ShrineDataProvider.INSTANCE, ReliquaryShrineBlockEntity.class" not in plugin or "ReliquaryShrineBlock.class" not in (
            client.read_text(encoding="utf-8") if client.exists() else ""):
        err("Jade: the Reliquary Shrine needs its data provider and its tooltip")
    if f"config.jade.plugin_{MOD}.reliquary_shrine" not in lang:
        err("Jade: missing the shrine's config name")
    for relic, slot in rl.TRINKET_SLOTS.items():
        group, name = slot.split("/")
        tag = load(ROOT / "src" / "main" / "resources" / "data" / "trinkets" / "tags" / "item" / group / f"{name}.json") or {}
        if rid_value(relic) not in tag.get("values", []):
            err(f"data/trinkets/tags/item/{slot}.json must list {rid_value(relic)}")
    entities = load(ROOT / "src" / "main" / "resources" / "data" / "trinkets" / "entities" / f"{MOD}_relics.json") or {}
    if set(rl.TRINKET_SLOTS.values()) - set(entities.get("slots", [])) or "player" not in entities.get("entities", []):
        err(f"data/trinkets/entities/{MOD}_relics.json must give players {sorted(set(rl.TRINKET_SLOTS.values()))}")
    if "TrinketsApi.getAttachment(" not in reliquary or "Context.COSMETIC" not in reliquary:
        err("Reliquary.find must read the Trinkets slots, treating cosmetic ones as worn for show")
    if "reliquary_shrine" not in registered or "reliquary_shrine" not in recipes:
        err("relics: the Reliquary Shrine needs a registration and a recipe")
    equipment = load(ASSETS / "equipment" / "owlsight_circlet.json") or {}
    worn = ASSETS / "textures" / "entity" / "equipment" / "humanoid" / "owlsight_circlet.png"
    if not equipment.get("layers", {}).get("humanoid") or not worn.is_file():
        err("relics: the Owlsight Circlet needs its equipment asset and worn texture")
    sheet = ASSETS / "textures" / "block" / "reliquary_shrine.png"
    if not sheet.is_file():
        err("relics: missing textures/block/reliquary_shrine.png")
    else:
        with Image.open(sheet) as img:
            if img.size != (64, 64):
                err("relics: the shrine's sheet must be 64x64")
    import item_icons
    for icon in rl.RELIC_IDS + ["reliquary_shrine"]:
        texture = ASSETS / "textures" / "item" / f"{icon}.png"
        if not item_icons.has(icon) or not texture.is_file():
            err(f"relics: {icon} needs its map tools/item_icons/{icon}.txt and its texture")
            continue
        with Image.open(texture) as img:
            if img.convert("RGBA").tobytes() != item_icons.draw(icon).tobytes() or img.size != (16, 16):
                err(f"relics: textures/item/{icon}.png differs from its map: run tools/generate_textures.py")


def check_equivalence(co, root, lang, registered, research):
    """Roadmap step 21: the Java mirrors tools/concordance_equivalence.py (the markup, limits, cap, eligibility rules and
    the scale's numbers); the catalogue and graph on disk are the generator's; Python's own audit finds no unvalued
    item, no recipe that gains value and no profitable cycle (sources apart), and every round trip through the scale
    loses; nothing catalogued is excluded or scarce; every reason and message has its text; the icon is its map."""
    eq = co.equivalence
    def java(name, base=root):
        path = base / name
        return path.read_text(encoding="utf-8") if path.exists() else ""
    assay = java("equivalence/Assay.java")
    for const, value in (("FORM_NUMERATOR", eq.FORM_MARKUP.numerator), ("FORM_DENOMINATOR", eq.FORM_MARKUP.denominator)):
        if not re.search(rf"\blong {const} = {value};", assay):
            err(f"equivalence/Assay.java: {const} differs from tools/concordance_equivalence.py FORM_MARKUP")
    if f"long MAX_BALANCE = {eq.MAX_BALANCE:_}L;" not in assay or f"int MAX_BATCH = {eq.MAX_BATCH};" not in assay:
        err("equivalence/Assay.java: MAX_BALANCE or MAX_BATCH differs from tools/concordance_equivalence.py")
    if f'String PRIMA = "{eq.PRIMA}";' not in assay:
        err("equivalence/Assay.java: PRIMA differs from tools/concordance_equivalence.py")
    parser = java("equivalence/EquivalenceParser.java")
    for const, value in (("MAX_GRAINS", f"{eq.MAX_GRAINS:_}"), ("MAX_PER", eq.MAX_PER), ("MAX_COUNT", eq.MAX_COUNT)):
        if not re.search(rf"\bint {const} = {value};", parser):
            err(f"equivalence/EquivalenceParser.java: {const} differs from tools/concordance_equivalence.py")
    audit = java("equivalence/CycleAudit.java")
    if f"int MAX_LENGTH = {eq.MAX_LENGTH};" not in audit or f"int MAX_CYCLES = {eq.MAX_CYCLES:_};" not in audit:
        err("equivalence/CycleAudit.java: MAX_LENGTH or MAX_CYCLES differs from tools/concordance_equivalence.py")
    eligibility = java("equivalence/Eligibility.java")
    if f'String EXCLUDED_TAG = "{eq.EXCLUDED_TAG}";' not in eligibility:
        err("equivalence/Eligibility.java: EXCLUDED_TAG differs from tools/concordance_equivalence.py")
    reasons = set(re.findall(r'return "([a-z_]+)";', eligibility)) | {"uncatalogued"}
    scale = java("assay/Assaying.java")
    reasons |= set(re.findall(r'refuse\(player, (?:stack|pattern), "([a-z_]+)"\)', scale))
    for reason in sorted(reasons | set(eq.REASONS)):
        if f"compose.{MOD}.assay.reason.{reason}" not in lang:
            err(f"equivalence: missing lang for the reason {reason}")
    if not reasons <= set(eq.REASONS):
        err(f"equivalence: reasons {sorted(reasons - set(eq.REASONS))} are not in REASONS")
    for key in re.findall(r'"message\.jugcraft\.concordance\.(assay\.[a-z_]+)"', scale):
        if f"message.{MOD}.concordance.{key}" not in lang:
            err(f"assay/Assaying.java: missing lang message.{MOD}.concordance.{key}")
    if f"int CONFIRM_TICKS = {eq.CONFIRM_TICKS};" not in scale or f'String ACTIVITY = "{eq.ASSAY_PRACTICE}";' not in scale:
        err("assay/Assaying.java: CONFIRM_TICKS or ACTIVITY differs from tools/concordance_equivalence.py")
    if "Overflow.REJECT" not in scale or ".extract(PRIMA, cost, true)" not in scale:
        err("assay/Assaying.java: the ledger moves all or nothing through the shared Reservoir")
    practice = [rule for block in research.get("assay", {}).get("states", {}).values()
                for rule in block.get("any", []) if rule.get("type") == "practice"]
    if not practice or practice[0].get("activity") != eq.ASSAY_PRACTICE:
        err("Assay must be mastered by the assay practice")
    elif sum(1 for info in eq.MATERIALS.values() if info[2]) < practice[0].get("distinct", 1):
        err("Assay's mastery asks for more different materials than can be dissolved")
    # The catalogue and graph on disk are the generator's.
    folder = DATA / MOD / "concordance"
    materials = {p.stem: load(p) or {} for p in (folder / "material").glob("*.json")}
    expected = {item.split(":")[1]: item for item in eq.MATERIALS}
    if set(materials) != set(expected):
        err(f"concordance/material: {sorted(materials)} differ from MATERIALS")
    for stem, entry in materials.items():
        item = expected.get(stem)
        if item and (entry.get("item") != item or entry.get("value") != eq.value(*eq.MATERIALS[item][:2])
                     or entry.get("dissolve") != eq.MATERIALS[item][2] or entry.get("form") != eq.MATERIALS[item][3]):
            err(f"concordance/material/{stem}.json differs from the generator's: run tools/generate_material_data.py")
    transmutations = {p.stem: load(p) or {} for p in (folder / "transmutation").glob("*.json")}
    if set(transmutations) != set(eq.TRANSMUTATIONS):
        err(f"concordance/transmutation: {sorted(transmutations)} differ from TRANSMUTATIONS")
    # Python's own audit: no unvalued item, no gaining recipe, no profitable cycle; every round trip loses.
    for problem in eq.audit():
        err(f"equivalence: {problem}")
    for item, (grains, per, dissolve, form) in eq.MATERIALS.items():
        if not (1 <= grains <= eq.MAX_GRAINS and 1 <= per <= eq.MAX_PER):
            err(f"material {item}: its value is out of bounds")
        if dissolve and form:
            for n in (1, 7, 64):
                paid = (grains * n) // per
                cost = -(-(grains * n * eq.FORM_MARKUP.numerator) // (per * eq.FORM_MARKUP.denominator))
                if cost <= paid:
                    err(f"material {item}: forming {n} must cost more than dissolving {n} pays")
    excluded = set(eq.EXCLUDED)
    for item in eq.MATERIALS:
        if item in excluded or item in ("minecraft:diamond", "minecraft:emerald", "minecraft:netherite_ingot", "minecraft:ancient_debris"):
            err(f"material {item}: it may not be catalogued (excluded or scarce)")
    tag = load(DATA / MOD / "tags" / "item" / "equivalence" / "excluded.json") or {}
    if sorted(tag.get("values", [])) != sorted(eq.EXCLUDED):
        err("tags/item/equivalence/excluded.json differs from EXCLUDED")
    for thing in eq.EXCLUDED:
        if thing.startswith(f"{MOD}:") and split(thing)[1] not in registered:
            err(f"equivalence: the excluded {thing} is not registered")
    if "assayers_scale" not in registered or not (DATA / MOD / "recipe" / "assayers_scale.json").is_file():
        err("equivalence: the Assayer's Scale needs a registration and a recipe")
    model = load(ASSETS / "models" / "block" / "assayers_scale.json") or {}
    if not model.get("elements") or not (ASSETS / "textures" / "block" / "assayers_scale.png").is_file():
        err("equivalence: the Assayer's Scale needs its model and texture")
    import item_icons
    texture = ASSETS / "textures" / "item" / "assayers_scale.png"
    if not item_icons.has("assayers_scale") or not texture.is_file():
        err("equivalence: assayers_scale needs its map tools/item_icons/assayers_scale.txt and its texture")
    else:
        with Image.open(texture) as img:
            if img.convert("RGBA").tobytes() != item_icons.draw("assayers_scale").tobytes() or img.size != (16, 16):
                err("equivalence: textures/item/assayers_scale.png differs from its map: run tools/generate_textures.py")


def check_sympathy(co, root, lang, registered, research):
    """Roadmap step 22: the Java mirrors tools/concordance_hexes.py (link, curse, ward and dream numbers, the ward
    categories); the curses on disk are the generator's and each is bounded (a hindrance from CURSE_EFFECTS, level I or
    II, short pulses, minutes in all, castable through a fresh link, its remedy not its own reagent); hostile effects go
    through the shared boundary under its multiplayer rules; the dream's escrow is one attachment that never survives a
    death by copying; every reason, end and message has its text; ward sigils can be made for every category; the
    censer's and wisp's GeckoLib assets are the ones played and fit their sheets; the icons are their maps.
    (check_hexes is fall addition 21's cauldron hexes, a different feature.)"""
    hx = co.hexes
    def java(name, base=root):
        path = base / name
        return path.read_text(encoding="utf-8") if path.exists() else ""
    constants = {
        "hex/Hexes.java": {"MAX_RANGE": hx.MAX_RANGE, "MAX_CURSES": hx.MAX_CURSES, "INVESTIGATE_FOCUS": hx.INVESTIGATE_FOCUS},
        "hex/Link.java": {"FRESH": hx.LINK_FRESH, "DECAY_TICKS": hx.DECAY_TICKS},
        "hex/HexParser.java": {"MAX_AMPLIFIER": hx.MAX_AMPLIFIER, "MAX_PULSE_DURATION": hx.MAX_PULSE_DURATION,
                               "MIN_PULSE_TICKS": hx.MIN_PULSE_TICKS, "MAX_TOTAL_TICKS": f"{hx.MAX_TOTAL_TICKS:_}", "MAX_FOCUS": hx.MAX_FOCUS},
        "dream/DreamRules.java": {"MAX_TICKS": f"{hx.DREAM_TICKS:_}", "RADIUS": hx.DREAM_RADIUS, "WISPS": hx.DREAM_WISPS,
                                  "WISP_TICKS": hx.WISP_TICKS, "MAX_CAUGHT": hx.MAX_CAUGHT, "FOCUS": hx.DREAM_FOCUS},
    }
    for name, values in constants.items():
        text = java(name)
        for const, value in values.items():
            if not re.search(rf"\bint {const} = {value};", text):
                err(f"concordance/{name}: {const} differs from tools/concordance_hexes.py ({value})")
    if f"long TICKS = {hx.WARD_TICKS:_}L;" not in java("hex/Ward.java"):
        err("hex/Ward.java: TICKS differs from tools/concordance_hexes.py WARD_TICKS")
    categories = re.findall(r'^\t[A-Z_]+\("([a-z_]+)"\)', java("hex/WardCategory.java"), re.M)
    if categories != list(hx.WARDS):
        err(f"hex/WardCategory.java: categories {categories} differ from WARDS (same order)")
    sympathy, dreaming = java("sympathy/Sympathy.java"), java("dreaming/Dreaming.java")
    for text, name, research_id, practice in ((sympathy, "Sympathy", "sympathy", hx.SYMPATHY_PRACTICE),
                                              (dreaming, "Dreaming", "dreamwalking", hx.DREAM_PRACTICE)):
        if f'String RESEARCH = "{MOD}:{research_id}";' not in text or f'String ACTIVITY = "{practice}";' not in text:
            err(f"{name}.java: RESEARCH or ACTIVITY differs from tools/concordance_hexes.py")
        rules = [rule for block in research.get(research_id, {}).get("states", {}).values()
                 for rule in block.get("any", []) if rule.get("type") == "practice"]
        if not rules or rules[0].get("activity") != practice:
            err(f"{research_id} must be mastered by its practice {practice}")
    if len(hx.CURSES) < next((rule.get("distinct", 1) for block in research.get("sympathy", {}).get("states", {}).values()
                              for rule in block.get("any", []) if rule.get("type") == "practice"), 1):
        err("Sympathy's mastery asks for more different curses than there are")
    # The curses on disk are the generator's, and each is bounded.
    folder = DATA / MOD / "concordance" / "curse"
    found = {p.stem: load(p) or {} for p in folder.glob("*.json")}
    if set(found) != set(hx.CURSES):
        err(f"concordance/curse: {sorted(found)} differ from CURSES")
    reagents = {}
    for key, info in hx.CURSES.items():
        entry = found.get(key, {})
        if any(entry.get(field) != info[field] for field in ("reagent", "status", "amplifier", "pulse_duration", "pulse_ticks",
                                                               "total_ticks", "strength", "focus", "remedy")):
            err(f"concordance/curse/{key}.json differs from the generator's: run tools/generate_material_data.py")
        if info["status"] not in hx.CURSE_EFFECTS or not 0 <= info["amplifier"] <= hx.MAX_AMPLIFIER:
            err(f"curse {key}: its status must be a hindrance from CURSE_EFFECTS, level I or II")
        if not (20 <= info["pulse_duration"] <= hx.MAX_PULSE_DURATION and hx.MIN_PULSE_TICKS <= info["pulse_ticks"] <= info["total_ticks"]
                <= hx.MAX_TOTAL_TICKS):
            err(f"curse {key}: its pulse or total time is out of bounds")
        if not (1 <= info["strength"] <= hx.LINK_FRESH and 1 <= info["focus"] <= hx.MAX_FOCUS):
            err(f"curse {key}: it must be castable through a fresh link, for Focus a player can have")
        if info["remedy"] == info["reagent"] or info["reagent"] in reagents:
            err(f"curse {key}: one curse per reagent, and no curse lifted by its own reagent")
        reagents[info["reagent"]] = key
        if f"compose.{MOD}.hex.curse.{key}" not in lang:
            err(f"curse {key}: missing its name")
    # Hostile effects go through the shared boundary, under its multiplayer rules; links and curses are revalidated.
    if "ConcordanceEffects.mayHarm(" not in sympathy or "Intent.HARMFUL" not in sympathy or "ConcordanceEffects.apply(" not in sympathy:
        err("Sympathy: curses pulse only through the shared effect boundary, as harmful, under its multiplayer rules")
    if sympathy.count("Hexes.pulse(") != 1 or "Hexes.cast(" not in sympathy:
        err("Sympathy: every cast and every pulse is revalidated by the pure rules")
    if "ConcordanceEffects.guard(" not in sympathy or "WardCategory.MOVING" not in sympathy:
        err("Sympathy: the moving ward guards the shared boundary")
    # The dream's escrow: one attachment, never copied on death; every ending goes through end().
    expedition = re.search(r'EXPEDITION = AttachmentRegistry[^;]*;', dreaming)
    if not expedition or "copyOnDeath" in expedition.group(0) or ".persistent(Saved.versioned(\"dream_expedition\", DreamExpedition.CODEC))" not in expedition.group(0):
        err("Dreaming.EXPEDITION must be one persistent attachment that is not copied on death")
    for event in ("ServerPlayConnectionEvents.JOIN", "ServerPlayConnectionEvents.DISCONNECT", "ServerLivingEntityEvents.ALLOW_DEATH",
                  "ServerPlayerEvents.AFTER_RESPAWN"):
        if event not in dreaming:
            err(f"Dreaming: a dream must end (or be recovered) on {event}")
    if "DreamRules.check(now, expedition.until(), here, distance)" not in dreaming:
        err("Dreaming: the check must end a dream whose dreamer left the body's dimension")
    if "DreamRules.spent(" not in dreaming:
        err("Dreaming: a dream never gives back more experience than was entered with (DreamRules.spent)")
    # Every word has its text.
    reasons = set(re.findall(r'return "([a-z_]+)";', java("hex/Hexes.java")))
    for name in ("sympathy/TaglockItem.java", "sympathy/Sympathy.java"):
        reasons |= set(re.findall(r'(?:reason = |return )"([a-z_]+)";', java(name)))
    reasons |= set(re.findall(r'compose\.jugcraft\.hex\.reason\.([a-z_]+)"', java("sympathy/ScryingGlassItem.java")))
    reasons -= {""}
    for reason in sorted(reasons | set(hx.HEX_REASONS)):
        if f"compose.{MOD}.hex.reason.{reason}" not in lang:
            err(f"hexes: missing lang for the reason {reason}")
    if not reasons <= set(hx.HEX_REASONS):
        err(f"hexes: reasons {sorted(reasons - set(hx.HEX_REASONS))} are not in HEX_REASONS")
    dream_reasons = set(re.findall(r'return "([a-z_]+)";', dreaming)) - {""}
    for reason in sorted(dream_reasons | set(hx.DREAM_REASONS)):
        if f"compose.{MOD}.dream.reason.{reason}" not in lang:
            err(f"dreams: missing lang for the reason {reason}")
    if not dream_reasons <= set(hx.DREAM_REASONS):
        err(f"dreams: reasons {sorted(dream_reasons - set(hx.DREAM_REASONS))} are not in DREAM_REASONS")
    ends = re.findall(r'^\t\t[A-Z_]+\("([a-z_]+)"\)', java("dream/DreamRules.java"), re.M)
    if sorted(ends) != sorted(hx.DREAM_ENDS):
        err(f"dream/DreamRules.java: ends {ends} differ from DREAM_ENDS")
    for category in list(hx.WARDS) + ["none"]:
        if f"compose.{MOD}.hex.ward.{category}" not in lang:
            err(f"hexes: missing lang for the ward {category}")
    for path in sorted((root / "sympathy").glob("*.java")) + sorted((root / "dreaming").glob("*.java")):
        text = path.read_text(encoding="utf-8")
        for kind, key in re.findall(r'"(message|tooltip|compose)\.jugcraft\.((?:concordance\.)?(?:hex|dream)[a-z_.]*)"', text):
            if not key.endswith(".") and f"{kind}.{MOD}.{key}" not in lang:
                err(f"{path.name}: missing lang {kind}.{MOD}.{key}")
        for key in re.findall(r'"tooltip\.jugcraft\.([a-z_]+)"', text):
            if f"tooltip.{MOD}.{key}" not in lang:
                err(f"{path.name}: missing lang tooltip.{MOD}.{key}")
    if f"entity.{MOD}.dream_wisp" not in lang:
        err("dreams: missing the dream wisp's name")
    # What it takes to make and use: everything is registered, made from obtainable things, and every ward has a sigil.
    recipes = {p.stem: load(p) or {} for p in (DATA / MOD / "recipe").glob("*.json")}
    for thing in ("taglock", "scrying_glass", "ward_sigil", "dreamglass", "oneiric_censer"):
        if thing not in registered:
            err(f"hexes: {thing} is not registered")
    for thing in ("taglock", "scrying_glass", "oneiric_censer"):
        if thing not in recipes:
            err(f"hexes: {thing} needs a recipe")
    for category in hx.WARDS:
        result = recipes.get(f"ward_sigil_{category}", {}).get("result", {})
        if result.get("id") != rid_value("ward_sigil") or result.get("components", {}).get(rid_value("ward")) != category:
            err(f"hexes: the {category} ward sigil needs its recipe, its result carrying {rid_value('ward')}={category}")
        if rid_value("dreamglass") not in recipes.get(f"ward_sigil_{category}", {}).get("ingredients", []):
            err(f"hexes: ward sigils are made with dreamglass (the {category} one is not)")
    pickaxe = load(DATA / "minecraft" / "tags" / "block" / "mineable" / "pickaxe.json") or {}
    if rid_value("oneiric_censer") not in pickaxe.get("values", []):
        err("hexes: the Oneiric Censer is mined with a pickaxe")
    # GeckoLib: the animations played exist; every cube's UV fits its sheet.
    import concordance_hex_models as models
    for kind, name, source in (("block", "oneiric_censer", "dreaming/OneiricCenserBlockEntity.java"),
                               ("entity", "dream_wisp", "dreaming/DreamWispEntity.java")):
        geo = load(ASSETS / "geckolib" / "models" / kind / f"{name}.geo.json") or {}
        animations = load(ASSETS / "geckolib" / "animations" / kind / f"{name}.animation.json") or {}
        if geo != models.GEO[name]() or animations != models.ANIMATIONS[name]():
            err(f"hexes: the {name} GeckoLib model or animations differ from tools/concordance_hex_models.py")
        for clip in re.findall(r'thenLoop\("([a-z_.]+)"\)', java(source)):
            if clip not in animations.get("animations", {}):
                err(f"{name}.animation.json: missing {clip}")
        description = (geo.get("minecraft:geometry") or [{}])[0].get("description", {})
        width, height = description.get("texture_width", 0), description.get("texture_height", 0)
        uv, sizes = models.SIZES[name]
        for cube, (u, v) in uv.items():
            w, h, d = sizes[cube]
            if u + 2 * (d + w) > width or v + d + h > height:
                err(f"hexes: {name}'s {cube} box does not fit its {width}x{height} sheet")
        sheet = ASSETS / "textures" / kind / f"{name}.png"
        if not sheet.is_file():
            err(f"hexes: missing textures/{kind}/{name}.png")
        else:
            with Image.open(sheet) as img:
                if img.size != (width, height):
                    err(f"hexes: textures/{kind}/{name}.png must be {width}x{height}")
    import item_icons
    for icon in ("taglock", "scrying_glass", "ward_sigil", "dreamglass", "oneiric_censer"):
        texture = ASSETS / "textures" / "item" / f"{icon}.png"
        if not item_icons.has(icon) or not texture.is_file():
            err(f"hexes: {icon} needs its map tools/item_icons/{icon}.txt and its texture")
            continue
        with Image.open(texture) as img:
            if img.convert("RGBA").tobytes() != item_icons.draw(icon).tobytes() or img.size != (16, 16):
                err(f"hexes: textures/item/{icon}.png differs from its map: run tools/generate_textures.py")


def check_conclave(co, root, lang, registered, research):
    """Roadmap step 23: the Java mirrors tools/concordance_conclave.py (renown, ranks, diminishing returns, obligations and
    the parser's limits); the commissions and projects on disk are the generator's, each within bounds, asking for
    practices the Concordance records and traditions the research has; no commission pays in what it asks for; every
    research requirement names an entry and is met once; every cooperation rule has a solo alternative within a week; a
    solo player can reach every rank without teaching (the acceptance condition); the Conclave reuses Jugcraft's parties
    and UseMode and adds no currency; every reason and message has its text; the lectern's model, texture and icon
    agree."""
    cc = co.conclave
    def java(name, base=root):
        path = base / name
        return path.read_text(encoding="utf-8") if path.exists() else ""
    conclave = java("conclave/Conclave.java")
    if f"long WEEK = {cc.WEEK:_}L;" not in conclave or "long OBLIGATION_TICKS = WEEK;" not in conclave:
        err("conclave/Conclave.java: WEEK or OBLIGATION_TICKS differs from tools/concordance_conclave.py")
    for const in ("TEACHING_RENOWN", "TEACHING_PER_LEARNER", "TRADITION_RENOWN"):
        if not re.search(rf"\bint {const} = {getattr(cc, const)};", conclave):
            err(f"conclave/Conclave.java: {const} differs from tools/concordance_conclave.py")
    renown = re.search(r"RESEARCH_RENOWN = Map\.of\(([^)]*)\)", conclave)
    found = {key: int(value) for key, value in re.findall(r'"([a-z]+)", (\d+)', renown.group(1))} if renown else {}
    if found != cc.RESEARCH_RENOWN:
        err("conclave/Conclave.java: RESEARCH_RENOWN differs from tools/concordance_conclave.py")
    diminishing = re.search(r"DIMINISHING = List\.of\(([^)]*)\)", conclave)
    if not diminishing or [int(n) for n in re.findall(r"\d+", diminishing.group(1))] != cc.DIMINISHING:
        err("conclave/Conclave.java: DIMINISHING differs from tools/concordance_conclave.py")
    ranks = re.findall(r'^\t[A-Z]+\("([a-z]+)", (\d+), (\d+), (\d+), (\d+), (true|false)\)', java("conclave/Rank.java"), re.M)
    if [(r[0], int(r[1]), int(r[2]), int(r[3]), int(r[4]), r[5] == "true") for r in ranks] != cc.RANKS:
        err("conclave/Rank.java: the ranks differ from tools/concordance_conclave.py RANKS (same order)")
    parser = java("conclave/ConclaveParser.java")
    for const in ("MAX_TIER", "MAX_COUNT", "MAX_RENOWN", "MAX_REWARD", "MAX_STAGES", "MAX_REQUIREMENTS", "MAX_CONTRIBUTORS", "MAX_DAYS",
                  "MAX_PRACTICE", "MAX_PROJECT_RENOWN"):
        if not re.search(rf"\bint {const} = {getattr(cc, const)};", parser):
            err(f"conclave/ConclaveParser.java: {const} differs from tools/concordance_conclave.py")
    kinds = re.findall(r'^\t[A-Z]+\("([a-z]+)"\)', java("conclave/Kind.java"), re.M)
    if kinds != cc.KINDS:
        err(f"conclave/Kind.java: kinds {kinds} differ from KINDS")
    # The data on disk is the generator's, and each piece keeps to the rules.
    folder = DATA / MOD / "concordance"
    commissions = {p.stem: load(p) or {} for p in (folder / "commission").glob("*.json")}
    projects = {p.stem: load(p) or {} for p in (folder / "project").glob("*.json")}
    if set(commissions) != set(cc.COMMISSIONS) or set(projects) != set(cc.PROJECTS):
        err("concordance/commission or concordance/project differ from COMMISSIONS and PROJECTS")
    for key, info in cc.COMMISSIONS.items():
        if commissions.get(key) != cc.commission_json(info):
            err(f"concordance/commission/{key}.json differs from the generator's: run tools/generate_material_data.py")
    for key, info in cc.PROJECTS.items():
        if projects.get(key) != cc.project_json(info):
            err(f"concordance/project/{key}.json differs from the generator's: run tools/generate_material_data.py")
    traditions = {info.get("tradition") for info in research.values()} - {None}
    if set(cc.TRADITIONS) != traditions:
        err(f"conclave: TRADITIONS {sorted(cc.TRADITIONS)} differ from the research entries' traditions {sorted(traditions)}")
    activities = set()
    for path in sorted(root.rglob("*.java")):
        activities |= set(re.findall(r'String ACTIVITY = "([a-z_:]+)";', path.read_text(encoding="utf-8")))
    for activity in cc.ACTIVITIES:
        if activity not in activities:
            err(f"conclave: no Java ACTIVITY records the practice {activity}")
        if f"compose.{MOD}.conclave.activity.{split(activity)[1]}" not in lang:
            err(f"conclave: missing lang for the practice {activity}")
    tiers = set()
    for key, (name, tradition, tier, ask, renown, reward, count) in cc.COMMISSIONS.items():
        tiers.add(tier)
        if tradition not in cc.TRADITIONS or not 1 <= tier <= cc.MAX_TIER or not 1 <= renown <= cc.MAX_RENOWN \
                or not 1 <= count <= cc.MAX_REWARD:
            err(f"commission {key}: its tradition, tier, renown or reward is out of bounds")
        if ask[0] == "deliver" and (ask[1] == reward or not 1 <= ask[2] <= cc.MAX_COUNT):
            err(f"commission {key}: it may not pay in what it asks for, and asks 1 to {cc.MAX_COUNT}")
        if ask[0] == "practice" and ask[1] not in cc.ACTIVITIES:
            err(f"commission {key}: {ask[1]} is not a practice the Conclave knows")
        if f"compose.{MOD}.conclave.commission.{key}" not in lang:
            err(f"commission {key}: missing its name")
    if tiers != set(range(1, cc.MAX_TIER + 1)):
        err("conclave: every commission tier needs at least one commission")
    for key, info in cc.PROJECTS.items():
        if info["tradition"] not in cc.TRADITIONS or not 1 <= len(info["stages"]) <= cc.MAX_STAGES:
            err(f"project {key}: its tradition or stage count is out of bounds")
        for stage_id, _name, renown, contributors, days, requirements in info["stages"]:
            if not (1 <= contributors <= cc.MAX_CONTRIBUTORS and 1 <= days <= cc.MAX_DAYS and 1 <= len(requirements) <= cc.MAX_REQUIREMENTS):
                err(f"project {key} stage {stage_id}: its cooperation rule or requirements are out of bounds")
            for requirement_id, kind, target, count in requirements:
                if kind == "research" and (target not in {f"{MOD}:{r}" for r in research} or count != 1):
                    err(f"project {key} stage {stage_id}: the research requirement {target} must name an entry, once")
                if kind == "practice" and target not in cc.ACTIVITIES:
                    err(f"project {key} stage {stage_id}: {target} is not a practice the Conclave knows")
            if f"compose.{MOD}.conclave.stage.{key}.{stage_id}" not in lang:
                err(f"project {key}: missing the name of the stage {stage_id}")
    # The acceptance condition: a solo player, without teaching, reaches every rank.
    by_tradition, solo_kinds, reached = cc.solo_route(research)
    if reached != [rank[0] for rank in cc.RANKS]:
        err(f"conclave: a solo player reaches only {reached} (renown {sum(by_tradition.values())}, {solo_kinds} kinds)")
    # Reuse, not replacement: parties and UseMode are Jugcraft's own; renown is never spent and is no currency.
    starbound = java("starbound/Starbound.java")
    lectern = java("starbound/ConclaveLecternBlockEntity.java")
    if "JugcraftParties.partyId(" not in starbound or "JugcraftParties.isLeader(" not in starbound or "UseMode" not in lectern:
        err("conclave: projects and lecterns use Jugcraft's own parties and the shared UseMode switch")
    if re.search(r"Jugs\.|\.take\(|renown\(\) -|\bspend\w*\(", starbound + java("conclave/Standing.java")):
        err("conclave: renown is standing, never spent, and no coin")
    if "ConcordanceProgress.listen(" not in starbound:
        err("conclave: research, practice and teaching are heard from ConcordanceProgress")
    # Every word has its text.
    reasons = set()
    for name in ("conclave/Conclave.java", "conclave/Projects.java", "starbound/Starbound.java"):
        reasons |= set(re.findall(r'(?:return |Award\(0, |Contribution\(0, )"([a-z_]+)"', java(name)))
        reasons |= set(re.findall(r'refuse\(player, "([a-z_]+)"\)', java(name)))
    reasons -= {""}
    for reason in sorted(reasons | set(cc.REASONS)):
        if f"compose.{MOD}.conclave.reason.{reason}" not in lang:
            err(f"conclave: missing lang for the reason {reason}")
    if not reasons <= set(cc.REASONS):
        err(f"conclave: reasons {sorted(reasons - set(cc.REASONS))} are not in REASONS")
    for key in re.findall(r'say\((?:player|out), "([a-z_.]+)"', starbound):
        if not key.endswith(".") and f"message.{MOD}.concordance.conclave.{key}" not in lang:
            err(f"starbound/Starbound.java: missing lang message.{MOD}.concordance.conclave.{key}")
    for key in re.findall(r'"message\.jugcraft\.concordance\.(conclave\.[a-z_]+)"', starbound):
        if f"message.{MOD}.concordance.{key}" not in lang:
            err(f"starbound/Starbound.java: missing lang message.{MOD}.concordance.{key}")
    for mode in ("personal", "party"):
        if f"message.{MOD}.concordance.conclave.mode.{mode}" not in lang:
            err(f"conclave: missing lang for the lectern's {mode} mode")
    for rank in cc.RANKS:
        if f"compose.{MOD}.conclave.rank.{rank[0]}" not in lang:
            err(f"conclave: missing the name of the rank {rank[0]}")
    for key in cc.ADVANCEMENTS:
        if not (DATA / MOD / "advancement" / f"{key}.json").is_file():
            err(f"conclave: missing the advancement {key}")
    for key in re.findall(r'award\([^;]*?Jugcraft\.id\("(conclave_[a-z_]+)"', starbound):
        if not any(advancement.startswith(key) for advancement in cc.ADVANCEMENTS):
            err(f"starbound/Starbound.java: awards {key}, which has no advancement")
    # The lectern: registered, made, mined; its model reads only its regions; its texture and icon are drawn.
    if "conclave_lectern" not in registered or not (DATA / MOD / "recipe" / "conclave_lectern.json").is_file():
        err("conclave: the Conclave Lectern needs a registration and a recipe")
    axe = load(DATA / "minecraft" / "tags" / "block" / "mineable" / "axe.json") or {}
    if rid_value("conclave_lectern") not in axe.get("values", []):
        err("conclave: the Conclave Lectern is mined with an axe")
    import concordance_conclave_models as models
    import model_writer
    model = load(ASSETS / "models" / "block" / "conclave_lectern.json") or {}
    expected = models.lectern_model()
    model_writer.finish_elements(expected["elements"])  # as the generator writes it: no two faces share a plane
    if model != expected:
        err("conclave: the lectern's block model differs from tools/concordance_conclave_models.py")
    texture = ASSETS / "textures" / "block" / "conclave_lectern.png"
    if not texture.is_file():
        err("conclave: missing textures/block/conclave_lectern.png")
    else:
        with Image.open(texture) as img:
            if img.size != (16, 16):
                err("conclave: the lectern's texture must be 16x16")
    import item_icons
    icon = ASSETS / "textures" / "item" / "conclave_lectern.png"
    if not item_icons.has("conclave_lectern") or not icon.is_file():
        err("conclave: conclave_lectern needs its map tools/item_icons/conclave_lectern.txt and its texture")
    else:
        with Image.open(icon) as img:
            if img.convert("RGBA").tobytes() != item_icons.draw("conclave_lectern").tobytes() or img.size != (16, 16):
                err("conclave: textures/item/conclave_lectern.png differs from its map: run tools/generate_textures.py")


def check_progression(co, root, lang, registered, research):
    """Roadmap step 24: the Java mirrors tools/concordance_progression.py (the parser's limits, the graph's node names);
    the stages and practice gates on disk are the generator's; the progression graph, followed down to things at hand
    (specimens, stations, instruments, structures, offerings, devices, encounters and what the world gives), lets one
    player alone reach every research state, practice, commission, project and stage from a fresh world, without
    structure loot, without the Nether and without the End, with nothing circular and nothing needing what only a later
    stage makes, two routes through each middle stage and a recovery route for every finite world material; the
    record's table of mandatory steps is the graph's; and the graph is what the codex, the spell gates, the equipment
    gates and recipe visibility are checked against: every codex condition is a reachable research state, each entry
    opens before the research it introduces begins, every invocation's entry opens exactly when it is learnt, every
    component and Java gate names a research state the graph has, every recipe-made thing a step needs has a codex
    recipe page readable before it is needed, and no required step depends on an optional library."""
    pg = co.progression
    def java(name, base=root):
        path = base / name
        return path.read_text(encoding="utf-8") if path.exists() else ""
    parser = java("progression/ProgressionParser.java")
    for const in ("MAX_ROUTES", "MAX_COUNT"):
        if not re.search(rf"\bint {const} = {getattr(pg, const)};", parser):
            err(f"progression/ProgressionParser.java: {const} differs from tools/concordance_progression.py")
    graph_java = java("progression/ProgressionGraph.java")
    for const, value in (("NOTES", "social:notes"), ("PROJECT", "project:conclave"), ("OATH_RESEARCH", f"{MOD}:first_light")):
        if f'String {const} = "{value}";' not in graph_java:
            err(f"progression/ProgressionGraph.java: {const} must be {value} (tools/concordance_progression.py)")
    # The data on disk is the generator's.
    folder = DATA / MOD / "concordance"
    stages = {p.stem: load(p) or {} for p in (folder / "stage").glob("*.json")}
    practices = {p.stem: load(p) or {} for p in (folder / "practice").glob("*.json")}
    if set(stages) != set(pg.STAGES) or set(practices) != set(pg.PRACTICES) | set(pg.MILESTONES):
        err("concordance/stage or concordance/practice differ from STAGES, PRACTICES and MILESTONES")
    for key, info in pg.STAGES.items():
        if stages.get(key) != pg.stage_json(info):
            err(f"concordance/stage/{key}.json differs from the generator's: run tools/generate_material_data.py")
    for key in pg.PRACTICES:
        if practices.get(key) != pg.practice_json(key):
            err(f"concordance/practice/{key}.json differs from the generator's: run tools/generate_material_data.py")
    for key in pg.MILESTONES:
        if practices.get(key) != pg.milestone_json(key):
            err(f"concordance/practice/{key}.json differs from the generator's: run tools/generate_material_data.py")
    # The graph, down to things at hand.
    invocations = {p.stem: load(p) or {} for p in (folder / "invocation").glob("*.json")}
    rituals = {p.stem: load(p) or {} for p in (folder / "ritual").glob("*.json")}
    at_hand = pg.load_at_hand(DATA)
    for problem in pg.audit(research, invocations, rituals, at_hand):
        err(f"progression: {problem}")
    for dimension in ("nether", "end"):
        reached = pg.without_dimension(research, invocations, rituals, at_hand, dimension)
        if reached != sorted(pg.STAGES, key=pg._order):
            err(f"progression: without the {dimension.title()}, one player reaches only {reached}")
    # The record shows the graph's mandatory steps, not a copy that could drift.
    record = (ROOT / "docs" / "features" / "arcane-concordance-progression.md").read_text(encoding="utf-8") \
        if (ROOT / "docs" / "features" / "arcane-concordance-progression.md").exists() else ""
    table = re.search(r"<!-- mandatory: generated -->\n(.*?)\n<!-- mandatory: end -->", record, re.S)
    expected = "\n".join(pg.mandatory_table(pg.mandatory(research, invocations, rituals, at_hand)))
    if not table or table.group(1).strip() != expected:
        err("docs/features/arcane-concordance-progression.md: its table of mandatory steps differs from the graph's:\n" + expected)
    nodes = pg.graph(research, invocations, rituals, at_hand)
    alone = pg.reachable(nodes, research)
    def state_of(node_id):
        # A codex fact's id (jugcraft:concordance/<research>_<state>) as the graph's research node.
        path = node_id.split(":", 1)[1].removeprefix("concordance/")
        entry, _, state = path.rpartition("_")
        return pg.research_node(f"{MOD}:{entry}", state)
    # Codex navigation: every condition is a reachable research state; each research entry's page opens before it begins.
    entries = {}
    base = DATA / MOD / "modonomicon" / "books" / co.BOOK / "entries"
    for path in sorted(base.rglob("*.json")):
        entry = load(path) or {}
        entries[path.relative_to(base).with_suffix("").as_posix()] = entry
        for condition in [entry.get("condition", {})] + [page.get("condition", {}) for page in entry.get("pages", [])]:
            if condition.get("type") == "modonomicon:research_node_unlocked":
                node = state_of(condition.get("node_id", ""))
                if node not in nodes or node not in alone:
                    err(f"codex {path.relative_to(base)}: its condition {condition.get('node_id')} is no state one player can reach")
    def opens(entry, before):
        """Whether a codex entry is open before {before} is reached (its condition reachable without it)."""
        condition = entry.get("condition", {})
        if condition.get("type") != "modonomicon:research_node_unlocked":
            return True
        return state_of(condition["node_id"]) in pg.reachable(nodes, research, without={before})
    for key, info in research.items():
        first = next((state for state in pg.RESEARCH_STATES if state in info.get("states", {})), None)
        pages = [entry for name, entry in entries.items() if name.endswith(f"/{key}")]
        if not pages:
            err(f"research {key}: no codex entry introduces it")
        elif first and not any(opens(entry, pg.research_node(f"{MOD}:{key}", first)) for entry in pages):
            err(f"research {key}: its codex entry opens only after the research has begun")
    for key, info in invocations.items():
        entry = entries.get(f"invocations/{key}", {})
        condition = entry.get("condition", {}).get("node_id", "")
        if not entry or state_of(condition) != pg.research_node(info.get("research", ""), info.get("stage", "")):
            err(f"codex invocations/{key}: its entry must open when the invocation is learnt ({info.get('research')} {info.get('stage')})")
    # Spell gates and equipment gates name states the graph has.
    for path in sorted((folder / "component").glob("*.json")):
        requires = (load(path) or {}).get("requires")
        if requires and pg.research_node(requires.get("research", ""), requires.get("state", "")) not in alone:
            err(f"component {path.stem}: its gate {requires} is no research state one player can reach")
    gates = {}
    for path in sorted(root.rglob("*.java")):
        text = path.read_text(encoding="utf-8")
        activity = re.search(r'String ACTIVITY = "([a-z_:]+)";', text)
        gate = re.search(r'String RESEARCH = "([a-z_]+:[a-z_]+)";', text)
        if gate and split(gate.group(1))[1] not in research:
            err(f"{path.relative_to(JAVA_ROOT)}: its gate {gate.group(1)} names no research entry")
        if activity:
            gates[split(activity.group(1))[1]] = (path, gate.group(1) if gate else None)
    for key, (_tradition, gate, _devices, _encounters) in pg.PRACTICES.items():
        path, java_gate = gates.get(key, (None, None))
        if path is None:
            err(f"progression: no Java ACTIVITY records the practice {key}")
        elif key == "ritual":
            if any(info.get("research") != rid_value(gate) for info in rituals.values()):
                err(f"progression: every ritual is taught by {gate}, the ritual practice's gate")
        elif java_gate != rid_value(gate):
            err(f"progression: the practice {key} is gated by {java_gate} in {path.name}, not {gate} as PRACTICES says")
    # Recipe visibility: the codex (Modonomicon, required) shows how to make every recipe-made thing a step needs.
    shown = {}
    for name, entry in entries.items():
        for page in entry.get("pages", []):
            recipe = page.get("recipe_id_1")
            if recipe:
                made = load(DATA / MOD / "recipe" / f"{split(recipe)[1]}.json") or {}
                result = made.get("result", {})
                shown.setdefault(result.get("id") if isinstance(result, dict) else result, []).append((entry, page))
    for key in sorted(alone):
        item = key[len("item:"):] if key.startswith(f"item:{MOD}:") else None
        if item is None or item in pg.JUGCRAFT_SOURCES:
            continue
        if item not in at_hand["recipes"]:
            # Made by a ritual, a bench working or a practice: the codex has an entry for it.
            if not any(name.endswith("/" + split(item)[1]) for name in entries) and item not in pg.PRODUCERS:
                err(f"progression: {item} is made by no recipe and no codex entry says how")
            continue
        without = pg.reachable(nodes, research, without={key})
        def readable(entry, page):
            return all(state_of(c["node_id"]) in without for c in (entry.get("condition", {}), page.get("condition", {}))
                       if c.get("type") == "modonomicon:research_node_unlocked")
        if not any(readable(entry, page) for entry, page in shown.get(item, [])):
            err(f"progression: the codex shows no recipe for {item} that can be read before it is needed")
    # A station or device a step needs is never lost by breaking it: it drops itself.
    for key in sorted(alone):
        item = split(key[len("item:"):])[1] if key.startswith(f"item:{MOD}:") else None
        if item and (ASSETS / "blockstates" / f"{item}.json").is_file():
            loot = json.dumps(load(DATA / MOD / "loot_table" / "blocks" / f"{item}.json") or {})
            if f'"{MOD}:{item}"' not in loot:
                err(f"progression: {MOD}:{item} is needed by a step, so breaking it must drop it")
    # No required step depends on an optional library: nothing in an optional adapter records progress.
    for base_dir in (JAVA_ROOT / "compat", CLIENT_JAVA_ROOT / "compat"):
        for path in sorted(base_dir.rglob("*.java")) if base_dir.exists() else []:
            if re.search(r"ConcordanceProgress\.record\(|StageProgress\.milestone\(|Starbound\.(contribute|fulfil|swear)\(",
                         path.read_text(encoding="utf-8")):
                err(f"{path.relative_to(ROOT)}: an optional adapter must not be the only way a step is done")
    stages_java = java("stages/StageProgress.java")
    if '.then(Commands.literal("stage")' not in stages_java or "copyOnDeath()" not in stages_java:
        err("stages/StageProgress.java: the stage command (the fallback to the codex) and stages kept through death")
    if "held.order() >= reached.order()" not in stages_java:
        err("stages/StageProgress.java: a stage once reached is never lost")
    # Every word has its text.
    for key, info in pg.STAGES.items():
        if f"compose.{MOD}.stage.{key}" not in lang:
            err(f"progression: missing the name of the stage {key}")
        for route in info["routes"]:
            if f"compose.{MOD}.stage.{key}.{route['id']}" not in lang:
                err(f"progression: missing the text of the route {key}/{route['id']}")
        if not (DATA / MOD / "advancement" / f"concordance_stage_{key}.json").is_file():
            err(f"progression: missing the advancement concordance_stage_{key}")
    for key in re.findall(r'"message\.jugcraft\.concordance\.((?:stage|progression)\.[a-z_]+)"', stages_java):
        if f"message.{MOD}.concordance.{key}" not in lang:
            err(f"stages/StageProgress.java: missing lang message.{MOD}.concordance.{key}")


def check_spire(co, root, lang, registered, research):
    """Roadmap step 25: the Java mirrors tools/concordance_spire.py (the parser's limits, the day, the reach, the field's
    hold, the heart's store and its look, the field kinds); the wonder and its configurations on disk are the
    generator's; there are at least two configurations and their fields differ; each names research that exists, a
    practice the Concordance records, a crown built round the heart and a renewable upkeep item; the Kindling is a
    ritual whose offerings can be had; the heart is registered, made, mined and drops itself, and GeckoLib has its model
    and the three animations the Java plays; the spire never breaks or replaces a block; the rite and the practices are
    heard from the circles and the research engine; the Architect stage has the raised spire's route; every word has
    its text."""
    sp = co.spire
    def java(name, base=root):
        path = base / name
        return path.read_text(encoding="utf-8") if path.exists() else ""
    parser = java("wonder/WonderParser.java")
    for const in ("MAX_PHASES", "MAX_PRACTICES", "MAX_SUSTAIN", "MAX_ATTENDANCE", "MIN_PULSE", "MAX_PULSE", "MAX_RITE_RANGE",
                  "MAX_RADIUS", "MAX_COUNT", "MAX_UPKEEP", "MAX_LEY", "MAX_REQUIRES"):
        if not re.search(rf"\bint {const} = {getattr(sp, const)};", parser):
            err(f"wonder/WonderParser.java: {const} differs from tools/concordance_spire.py")
    if f"long DAY = {sp.DAY:_}L;" not in java("wonder/Spires.java"):
        err("wonder/Spires.java: DAY differs from tools/concordance_spire.py")
    spire_java = java("spire/ConcordSpire.java")
    heart_java = java("spire/SpireHeartBlockEntity.java")
    for const, value in (("REACH", sp.REACH), ("HOLD_TICKS", sp.HOLD_TICKS)):
        if not re.search(rf"\bint {const} = {value};", spire_java):
            err(f"spire/ConcordSpire.java: {const} differs from tools/concordance_spire.py")
    for const, value in (("SLOTS", sp.SLOTS), ("CHECK_TICKS", sp.CHECK_TICKS)):
        if not re.search(rf"\bint {const} = {value};", heart_java):
            err(f"spire/SpireHeartBlockEntity.java: {const} differs from tools/concordance_spire.py")
    if f'String WONDER = "{sp.WONDER_ID}";' not in spire_java or f'String MILESTONE = "{sp.MILESTONE}";' not in spire_java:
        err("spire/ConcordSpire.java: WONDER or MILESTONE differs from tools/concordance_spire.py")
    kinds = re.findall(r'^\t[A-Z]+\("([a-z]+)"\)', java("wonder/FieldKind.java"), re.M)
    if kinds != sp.FIELD_KINDS:
        err(f"wonder/FieldKind.java: kinds {kinds} differ from FIELD_KINDS")
    # The data on disk is the generator's.
    folder = DATA / MOD / "concordance"
    if (load(folder / "wonder" / "concord_spire.json") or {}) != sp.wonder_json():
        err("concordance/wonder/concord_spire.json differs from the generator's: run tools/generate_material_data.py")
    configurations = {p.stem: load(p) or {} for p in (folder / "wonder_configuration").glob("*.json")}
    if set(configurations) != set(sp.CONFIGURATIONS):
        err("concordance/wonder_configuration differs from CONFIGURATIONS")
    for key in sp.CONFIGURATIONS:
        if configurations.get(key) != sp.configuration_json(key):
            err(f"concordance/wonder_configuration/{key}.json differs from the generator's: run tools/generate_material_data.py")
    # Multiple configurations, each meaningful and reachable.
    if len(sp.CONFIGURATIONS) < 2 or len({info["field"]["kind"] for info in sp.CONFIGURATIONS.values()}) < 2:
        err("spire: a wonder offers at least two configurations whose fields differ")
    structures = {p.stem: load(p) or {} for p in (folder / "structure").glob("*.json")}
    traditions = {info.get("tradition") for info in research.values()}
    pg = co.progression
    for key, info in sp.CONFIGURATIONS.items():
        if info["tradition"] not in traditions:
            err(f"spire configuration {key}: no research belongs to {info['tradition']}")
        if info["practice"] not in co.conclave.ACTIVITIES:
            err(f"spire configuration {key}: {info['practice']} is no practice the Concordance records")
        for requirement in info["requires"]:
            entry = research.get(split(requirement["research"])[1], {})
            if requirement["state"] not in entry.get("states", {}):
                err(f"spire configuration {key}: needs {requirement['research']} {requirement['state']}, which does not exist")
        crown = structures.get(split(info["crown"])[1], {})
        if crown.get("anchor") != rid_value("spire_heart"):
            err(f"spire configuration {key}: its crown {info['crown']} must be built round the Spire Heart")
        kind = pg.VANILLA_SOURCES.get(info["upkeep"]["item"], ("",))[0]
        if kind not in ("craftable", "renewable"):
            err(f"spire configuration {key}: a kept wonder runs on what can be renewed, not {info['upkeep']['item']} ({kind or 'unlisted'})")
        for kind_key in ("radius", "count"):
            if not 1 <= info["field"][kind_key] <= getattr(sp, f"MAX_{kind_key.upper()}"):
                err(f"spire configuration {key}: field {kind_key} out of bounds")
    for structure in ("spire_foundation", "spire_shaft"):
        if structures.get(structure, {}).get("anchor") != rid_value("spire_heart"):
            err(f"spire: structure {structure} must be built round the Spire Heart")
    kindling = load(folder / "ritual" / "spire_kindling.json") or {}
    if kindling.get("research") != rid_value("circle_lore") or kindling.get("participants") != 1:
        err("spire: the Kindling is a ritual one player can complete, taught by Circle Lore")
    for offering in kindling.get("offerings", []):
        if pg.VANILLA_SOURCES.get(offering["item"], ("found",))[0] == "found":
            err(f"spire: the Kindling's offering {offering['item']} cannot be relied on")
    # The heart: registered, made, mined, dropping itself, drawn by GeckoLib with the animations the Java plays.
    if "spire_heart" not in registered or not (DATA / MOD / "recipe" / "spire_heart.json").is_file():
        err("spire: the Spire Heart needs a registration and a recipe")
    if f'"{rid_value("spire_heart")}"' not in json.dumps(load(DATA / MOD / "loot_table" / "blocks" / "spire_heart.json") or {}):
        err("spire: a broken Spire Heart drops itself")
    pickaxe = load(DATA / "minecraft" / "tags" / "block" / "mineable" / "pickaxe.json") or {}
    if rid_value("spire_heart") not in pickaxe.get("values", []):
        err("spire: the Spire Heart is mined with a pickaxe")
    animations = load(ASSETS / "geckolib" / "animations" / "block" / "spire_heart.animation.json") or {}
    for name in re.findall(r'thenLoop\("(animation\.spire_heart\.[a-z]+)"\)', heart_java):
        if name not in animations.get("animations", {}):
            err(f"spire: GeckoLib has no {name}")
    # The model, its animations and its sheet are the art modules' own; every box's UV fits the sheet.
    import concordance_spire_art as spire_art
    import concordance_spire_models as spire_models
    geo = load(ASSETS / "geckolib" / "models" / "block" / "spire_heart.geo.json") or {}
    if geo != spire_models.GEO["spire_heart"]() or animations != spire_models.ANIMATIONS["spire_heart"]():
        err("spire: the Spire Heart's GeckoLib model or animations differ from tools/concordance_spire_models.py")
    description = (geo.get("minecraft:geometry") or [{}])[0].get("description", {})
    width, height = description.get("texture_width", 0), description.get("texture_height", 0)
    uv, sizes = spire_models.SIZES["spire_heart"]
    for cube, (u, v) in uv.items():
        w, h, d = sizes[cube]
        if u + 2 * (d + w) > width or v + d + h > height:
            err(f"spire: the Spire Heart's {cube} box does not fit its {width}x{height} sheet")
    import item_icons
    for kind in ("block", "item"):
        texture = ASSETS / "textures" / kind / "spire_heart.png"
        if not texture.is_file():
            err(f"spire: missing textures/{kind}/spire_heart.png")
    sheet = ASSETS / "textures" / "block" / "spire_heart.png"
    if sheet.is_file():
        with Image.open(sheet) as img:
            if img.size != (width, height) or img.convert("RGBA").tobytes() != spire_art.heart_sheet().tobytes():
                err("spire: textures/block/spire_heart.png differs from tools/concordance_spire_art.py: run tools/generate_textures.py")
    icon = ASSETS / "textures" / "item" / "spire_heart.png"
    if not item_icons.has("spire_heart"):
        err("spire: spire_heart needs its map tools/item_icons/spire_heart.txt")
    elif icon.is_file():
        with Image.open(icon) as img:
            if img.convert("RGBA").tobytes() != item_icons.draw("spire_heart").tobytes():
                err("spire: textures/item/spire_heart.png differs from its map: run tools/generate_textures.py")
    # Nothing is broken or replaced; the rite and practices come from the circles and the research engine.
    if re.search(r"destroyBlock\(|removeBlock\(", spire_java + heart_java):
        err("spire: the spire never breaks or removes a block")
    if "Rituals.listen(" not in spire_java or "Rituals.completed(" not in java("CircleAnchorBlockEntity.java"):
        err("spire: the Kindling is heard from the circles (Rituals.listen, called when a ritual completes)")
    if "ConcordanceProgress.listen(" not in spire_java:
        err("spire: practices are heard from ConcordanceProgress")
    # The Architect stage has the raised spire's route, through its milestone's gate.
    routes = [route for route in pg.STAGES["architect"]["routes"] if sp.MILESTONE in route.get("milestones", [])]
    if not routes or split(sp.MILESTONE)[1] not in pg.MILESTONES:
        err("spire: the Architect stage needs a route through the raised spire's milestone and its gate")
    # Every word has its text.
    for key in re.findall(r'(?:say|tell)\((?:player|out), "([a-z_]+)"', spire_java) + re.findall(r'"message\.jugcraft\.concordance\.spire\.([a-z_]+)"', spire_java):
        if f"message.{MOD}.concordance.spire.{key}" not in lang:
            err(f"spire/ConcordSpire.java: missing lang message.{MOD}.concordance.spire.{key}")
    for key in re.findall(r'refuse\(player, "([a-z_]+)"\)', spire_java) + re.findall(r'return "([a-z_]+)";', spire_java + java("wonder/Spires.java")):
        if key not in ("active", "unfounded", "raising", "disabled", "unknown", "damaged", "unattended", "unsupplied", "structure",
                       "practices", "rite", "sustain", "raised", "stage", "research", "founded", "no_spire") and \
                f"compose.{MOD}.spire.reason.{key}" not in lang:
            err(f"spire: missing lang for the reason {key}")
    for key in sp.REASONS:
        if f"compose.{MOD}.spire.reason.{key}" not in lang:
            err(f"spire: missing lang for the reason {key}")
    for key in list(sp.MISSING) + ["nothing"]:
        if f"compose.{MOD}.spire.missing.{key}" not in lang:
            err(f"spire: missing lang compose.{MOD}.spire.missing.{key}")
    for key in sp.DORMANT:
        if f"compose.{MOD}.spire.dormant.{key}" not in lang:
            err(f"spire: missing lang compose.{MOD}.spire.dormant.{key}")
    for key in sp.FIELD_KINDS:
        if f"compose.{MOD}.spire.field.{key}" not in lang:
            err(f"spire: missing lang compose.{MOD}.spire.field.{key}")
    for key in sp.ADVANCEMENTS:
        if not (DATA / MOD / "advancement" / f"{key}.json").is_file():
            err(f"spire: missing the advancement {key}")


def rid_value(path):
    return f"{MOD}:{path}"


# The most Vitae an hour of offering can give, whatever heals the giver (docs/features/arcane-concordance-vitae.md).
HOURLY_VITAE_BOUND = 60


def check_crimson(co, root, lang, registered, research):
    """Roadmap step 16: the Java mirrors tools/concordance_crimson.py (exhaustion, efficiency, growth, the chalice,
    surge and blade numbers); every rite gives no more Vitae than the health it takes and can be made from full health;
    an hour of offering is bounded whatever heals the giver, and a surge never turns Vitae into more Focus; health is
    taken in exactly one place; mastery comes from the blade's growth; every word has its text; the icons are their maps;
    the offering gesture exists and skips players whose arms ArmsMotion poses."""
    cr = co.crimson
    def java(name, base=root):
        path = base / name
        return path.read_text(encoding="utf-8") if path.exists() else ""
    constants = {
        "crimson/Exhaustion.java": {"MAX": cr.EXHAUSTION_MAX, "RECOVERY_TICKS": cr.RECOVERY_TICKS},
        "crimson/CrimsonParser.java": {"MAX_HEALTH": cr.MAX_HEALTH, "MAX_COOLDOWN": cr.MAX_COOLDOWN},
        "crimson/Growth.java": {"MAX_SUBJECTS": cr.MAX_SUBJECTS, "DAILY_CAP": cr.DAILY_CAP, "MAX_VIGOR": cr.MAX_VIGOR,
                                "VIGOR_PER_VITAE": cr.VIGOR_PER_VITAE, "MAX_STAGE": len(cr.STAGES) - 1},
        "vigil/Vigil.java": {"CHALICE_CAPACITY": cr.CHALICE_CAPACITY, "SURGE_VITAE": cr.SURGE_VITAE, "SURGE_FOCUS": cr.SURGE_FOCUS,
                             "SURGE_COOLDOWN": cr.SURGE_COOLDOWN, "NOURISH_VITAE": cr.NOURISH_VITAE, "BLOW_VIGOR": cr.BLOW_VIGOR},
    }
    for name, values in constants.items():
        text = java(name)
        for const, value in values.items():
            if not re.search(rf"\bint {const} = {value};", text):
                err(f"concordance/{name}: {const} differs from tools/concordance_crimson.py ({value})")
    growth = java("crimson/Growth.java")
    if f"long WINDOW_TICKS = {cr.WINDOW_TICKS}L;" not in growth:
        err("crimson/Growth.java: WINDOW_TICKS differs from tools/concordance_crimson.py")
    diminishing = re.search(r"DIMINISHING = List\.of\(([^)]*)\)", growth)
    if not diminishing or [int(n) for n in re.findall(r"\d+", diminishing.group(1))] != cr.DIMINISHING:
        err("crimson/Growth.java: DIMINISHING differs from tools/concordance_crimson.py")
    for name, column in (("TOTAL", 0), ("EACH", 1), ("KINDS", 2)):
        found = re.search(rf"int\[\] {name} = \{{([^}}]*)\}}", growth)
        if not found or [int(n) for n in re.findall(r"\d+", found.group(1))] != [stage[column] for stage in cr.STAGES]:
            err(f"crimson/Growth.java: {name} differs from tools/concordance_crimson.py STAGES")
    offerings = java("crimson/Offerings.java")
    for top, percent in cr.EFFICIENCY[:2]:
        if not re.search(rf"exhaustion <= {top}\) \{{\s*return {percent};", offerings):
            err(f"crimson/Offerings.java: efficiency up to {top} points differs from EFFICIENCY ({percent}%)")
    if f"? {cr.EFFICIENCY[2][1]} : 0" not in offerings:
        err("crimson/Offerings.java: the last efficiency band differs from EFFICIENCY")
    # The rites, and the hour's bound.
    folder = DATA / MOD / "concordance" / "offering"
    rites = {p.stem: load(p) or {} for p in folder.glob("*.json")}
    if set(rites) != set(cr.RITES):
        err(f"offering rites: {sorted(rites)} differ from RITES")
    for key, rite in rites.items():
        health, vitae, tired, floor = rite.get("health", 0), rite.get("vitae", 0), rite.get("exhaustion", 0), rite.get("floor", 0)
        if not (1 <= vitae <= health <= cr.MAX_HEALTH) or not (1 <= tired <= cr.EXHAUSTION_MAX) or floor < 1:
            err(f"offering {key}: needs 1 <= vitae <= health <= {cr.MAX_HEALTH}, exhaustion 1 to {cr.EXHAUSTION_MAX} and a floor")
        if health + floor > 20:
            err(f"offering {key}: it could never be made, even from full health (health {health} + floor {floor} > 20)")
        offerings_an_hour = (cr.EXHAUSTION_MAX + 72000 // cr.RECOVERY_TICKS) // max(1, tired)
        if offerings_an_hour * vitae > HOURLY_VITAE_BOUND:
            err(f"offering {key}: an hour could give {offerings_an_hour * vitae} Vitae, above the bound of {HOURLY_VITAE_BOUND}")
    if cr.SURGE_FOCUS > cr.SURGE_VITAE:
        err("a Crimson Surge must not give more Focus than the Vitae it costs")
    vigil = root / "vigil"
    sets = sum(path.read_text(encoding="utf-8").count(".setHealth(") for path in sorted(vigil.glob("*.java")))
    if sets != 1:
        err(f"vigil: health must be taken in exactly one place (the offering), found {sets} setHealth calls")
    if not re.search(r'String ACTIVITY = "' + re.escape(cr.LIVING_GROWTH) + '";', java("Vigil.java", vigil)):
        err("Vigil.ACTIVITY differs from tools/concordance_crimson.py LIVING_GROWTH")
    practice = [rule for block in research.get("crimson_rites", {}).get("states", {}).values()
                for rule in block.get("any", []) if rule.get("type") == "practice"]
    if not practice or practice[0].get("activity") != cr.LIVING_GROWTH or practice[0].get("distinct", 0) > len(cr.STAGES) - 1:
        err("Crimson Rites must learn from the blade's growth, with no more stages than the blade has")
    for thing in ("crimson_chalice", "thornheart_blade"):
        if thing not in registered or not (DATA / MOD / "recipe" / f"{thing}.json").is_file():
            err(f"vigil: {thing} needs a registration and a recipe")
    # Every word has its text.
    for path in sorted(vigil.glob("*.java")):
        for key in re.findall(r'"message\.jugcraft\.concordance\.(vigil\.[a-z_]+)"', path.read_text(encoding="utf-8")):
            if not key.endswith(".") and f"message.{MOD}.concordance.{key}" not in lang:
                err(f"{path.name}: missing lang message.{MOD}.concordance.{key}")
    for result in re.findall(r'return "([a-z_]+)";', java("Vigil.java", vigil)):
        if f"message.{MOD}.concordance.vigil.{result}" not in lang:
            err(f"Vigil.surge: missing lang message.{MOD}.concordance.vigil.{result}")
    for stage in range(len(cr.STAGES)):
        if f"message.{MOD}.concordance.vigil.stage.{stage}" not in lang:
            err(f"vigil: missing lang for stage {stage}")
    client = java("VigilClient.java", CLIENT_JAVA_ROOT)
    for key in re.findall(r'"screen\.jugcraft\.concordance\.(vigil\.[a-z_]+)"', client):
        if f"screen.{MOD}.concordance.{key}" not in lang:
            err(f"VigilClient: missing lang screen.{MOD}.concordance.{key}")
    gesture = re.search(r'GESTURE = Jugcraft\.id\("([a-z_]+)"\)', client)
    if not gesture or not (ASSETS / "player_animations" / f"{gesture.group(1)}.json").is_file():
        err("VigilClient: the offering gesture needs its clip in assets/jugcraft/player_animations")
    if "instanceof ArmItem" not in client:
        err("VigilClient: the offering gesture must skip players whose arms ArmsMotion poses (ArmItem)")
    import item_icons
    for icon in ("crimson_chalice", "thornheart_blade"):
        texture = ASSETS / "textures" / "item" / f"{icon}.png"
        if not item_icons.has(icon) or not texture.is_file():
            err(f"vigil: {icon} needs its map tools/item_icons/{icon}.txt and its texture")
            continue
        with Image.open(texture) as img:
            if img.convert("RGBA").tobytes() != item_icons.draw(icon).tobytes() or img.size != (16, 16):
                err(f"vigil: textures/item/{icon}.png differs from its map: run tools/generate_textures.py")


def KINDLE_MOTE_STEPS_FIT(co):
    return 0 < co.KINDLE_MOTE_STEPS <= 15


def main():
    registered = (set(all_blocks()) | set(all_items()) | set(machine_blocks()) | set(machine_items())
                  | set(ag.all_blocks()) | set(ag.all_items()) | set(petro.petro_items()) | set(petro.petro_blocks())
                  | set(deposits.DEPOSITS) | set(guide_books.BOOKS) | set(tank_display.BLOCKS)
                  | set(arms.items()) | set(arms_variants.items())
                  | set(gear.items()) | set(plastic.blocks()) | set(exosuit.items()) | set(grapple.items())
                  | set(field_chemistry.items()) | set(construction.items()) | set(construction.blocks()) | set(gas_storage.items()) | set(control_electronics.blocks()) | set(control_electronics.items()) | set(rocketry.items()) | set(rocketry.blocks()) | set(dieselworks.blocks()) | set(kaiserworks.blocks()) | set(trenchworks.blocks()) | set(fortifications.blocks()) | set(bunkerworks.blocks()) | set(fire_control.blocks()) | set(fire_control.items()) | set(raiders.ITEMS) | set(armoured_walker.ITEMS) | set(zeppelin.ITEMS) | set(mech.ITEMS) | set(landship.ITEMS) | set(artillery.ITEMS) | set(tower_guns.items())
                  | set(ph.blocks()) | set(ph.items()) | set((town_assets.blocks() + styx.blocks()))
                  | set(guns.items()) | set(guns.blocks()) | set(concordance.items()) | set(concordance.blocks()) | set(industrial_forms.blocks()))
    check_assets(sorted(registered))
    check_model_textures()
    check_petro()
    check_loot(registered)
    check_recipes(registered)
    check_machine_recipe_files(registered)
    check_fluid_recipes(registered)
    check_industrial_forms()
    check_tags()
    check_worldgen()
    check_java()
    check_deposits()
    check_gear()
    check_thallite_gear()
    check_arms()
    check_item_icons()
    check_arms_variants()
    check_mesh_models()
    check_arms_motion()
    check_flail_heads()
    check_exosuit()
    check_worn_armor()
    check_grapple()
    check_guns()
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
    check_guide_books()
    check_handbook(registered)
    check_agriculture()
    check_kitchen()
    check_feasts()
    check_menu()
    check_rice()
    check_soil()
    check_orchard()
    check_garden()
    check_vegetables_herbs_spices()
    check_cakes()
    check_pies_and_tarts()
    check_milkshakes()
    check_recipe_categories()
    check_advancements(registered)
    check_model_uvs()
    check_lairs()
    check_vesperine()
    check_tatterlace()
    check_yeti()
    check_cinder_tyrant()
    check_material_sets()
    check_art()
    check_pixel_hollows()
    check_town()
    check_diagonal_connections()
    check_concordance(registered)
    for path in RES.rglob("*.json"):
        load(path)
    if errors:
        print("\n".join(errors), file=sys.stderr)
        sys.exit(1)
    print(f"PASS: {len(registered)} material IDs, data files and recipe audit. No Minecraft build or game test performed.")


def check_journal(co, root, lang):
    """Roadmap step 26: the Concordance Journal's sections are tools/concordance_journal.py's, in its order, each with a
    title; every word the journal, the stage report and the journal's screens use has its text (the states, the rule
    kinds, the milestones); the journal only reads (it records, grants and changes nothing); the GuiLib workspace is
    reached only when GuiLib is installed, and its stylesheet is shipped; the screens only send the request, which
    carries nothing."""
    jn = co.journal
    def text(path):
        return path.read_text(encoding="utf-8") if path.exists() else ""
    journal = text(root / "journal" / "Journal.java")
    stages = text(root / "stages" / "StageProgress.java")
    client = text(CLIENT_JAVA_ROOT / "JournalClient.java") + text(CLIENT_JAVA_ROOT / "JournalScreen.java")
    workspace_path = ROOT / "src" / "client" / "kotlin" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client" / "JournalWorkspace.kt"
    workspace = text(workspace_path)
    sections = re.findall(r'contribute\("([a-z_]+)"', journal)
    if sections != list(jn.SECTIONS):
        err(f"journal/Journal.java: sections {sections} differ from SECTIONS in tools/concordance_journal.py")
    for key in jn.SECTIONS:
        if f"journal.{MOD}.section.{key}" not in lang:
            err(f"journal: missing lang journal.{MOD}.section.{key}")
    for state in ("none", "encountered", "observed", "understood", "mastered"):
        if f"journal.{MOD}.state.{state}" not in lang:
            err(f"journal: missing lang journal.{MOD}.state.{state}")
    kinds = re.findall(r'^\t\t[A-Z]+\("([a-z]+)"\)', text(root / "rules" / "EvidenceRule.java"), re.M)
    if not kinds or set(kinds) != set(jn.RULES):
        err(f"journal: RULES in tools/concordance_journal.py must name every evidence kind {kinds}")
    for milestone in co.progression.MILESTONES:
        if f"compose.{MOD}.milestone.{milestone}" not in lang:
            err(f"journal: missing lang compose.{MOD}.milestone.{milestone}")
    for source, name in ((journal, "journal/Journal.java"), (stages, "stages/StageProgress.java"), (client, "the journal's screens"),
                         (workspace, "JournalWorkspace.kt")):
        for key in re.findall(r'"((?:journal|screen|key)\.jugcraft\.[a-z_.]+|message\.jugcraft\.concordance\.stage\.need\.[a-z_]+)"', source):
            if not key.endswith(".") and key not in lang:
                err(f"{name}: missing lang {key}")
    if re.search(r"ConcordanceProgress\.(record|grant|award|setFocus)\(|StageProgress\.milestone\(|\.put\(", journal):
        err("journal/Journal.java: the journal only reads; it records, grants and changes nothing")
    if "RateGate.allow(player, \"journal\"" not in journal:
        err("journal/Journal.java: the server answers a journal request at most every REQUEST_TICKS (RateGate)")
    if not re.search(r'isModLoaded\("guilib"\)\)\s*\{\s*JournalWorkspace\.open\(\)', client):
        err("JournalClient.java: the GuiLib workspace opens only when GuiLib is installed")
    for path in CLIENT_JAVA_ROOT.rglob("*.java"):
        if path.name != "JournalClient.java" and "JournalWorkspace" in text(path):
            err(f"{path.name}: only JournalClient reaches the GuiLib workspace (behind its isModLoaded check)")
    if '"jugcraft:ui/journal.css"' not in workspace or not (ASSETS / "ui" / "journal.css").is_file():
        err("journal: the GuiLib workspace's stylesheet assets/jugcraft/ui/journal.css must exist and be the one it opens")
    request = text(root / "journal" / "JournalRequestPayload.java")
    if "StreamCodec.unit(" not in request:
        err("journal/JournalRequestPayload.java: a request carries nothing (the server decides what a player sees)")


def check_signs(co, root, lang):
    """Roadmap step 27: the signs, the visual intensities and the server's limits are tools/concordance_signs.py's, in
    its order; the warnings' sounds are registered, drawn and subtitled; every server-side Concordance particle goes
    through a sign (no sendParticles), so each client draws it as its settings allow; the display settings are read only
    by client code and blocks' animateTick (the server never decides from them); a crucible tells a client only its
    heat, volume and the spoon's reading; and the setting's words exist."""
    sg = co.signs
    def text(path):
        return path.read_text(encoding="utf-8") if path.exists() else ""
    sign_java = text(root / "sign" / "Sign.java")
    presentation = text(root / "sign" / "Presentation.java")
    signs_java = text(root / "sign" / "Signs.java")
    found = re.findall(r'^\t([A-Z]+)\("([a-z]+)", Kind\.([A-Z]+), (\d+)\)', sign_java, re.M)
    expected = [(key, kind.upper(), str(count)) for key, (kind, count) in sg.SIGNS.items()]
    if [(sid, kind, count) for _, sid, kind, count in found] != expected:
        err(f"sign/Sign.java: signs {[(sid, kind, count) for _, sid, kind, count in found]} differ from SIGNS in tools/concordance_signs.py")
    kinds = re.search(r"enum Kind \{([^}]*?);", sign_java, re.S)
    if not kinds or [k.strip().lower() for k in kinds.group(1).split(",")] != list(sg.KINDS):
        err("sign/Sign.java: Sign.Kind must list KINDS of tools/concordance_signs.py, in order")
    intensities = re.findall(r'^\t\t([A-Z]+)\((\d+), (\d+), (\d+)\)', presentation, re.M)
    if [(name.lower(), (int(a), int(b), int(c))) for name, a, b, c in intensities] != list(sg.INTENSITY.items()):
        err(f"sign/Presentation.java: intensities {intensities} differ from INTENSITY in tools/concordance_signs.py")
    for name, value, source in (("FAR", sg.FAR, presentation), ("NEAR", sg.NEAR, presentation),
                                ("WARNING_FLOOR", sg.WARNING_FLOOR, presentation), ("AMBIENT_SHARE", sg.AMBIENT_SHARE, presentation),
                                ("PER_TICK", sg.PER_TICK, signs_java), ("WARNING_RESERVE", sg.WARNING_RESERVE, signs_java)):
        if not re.search(rf"\b{name} = {value}(\.0)?;", source):
            err(f"sign: {name} must be {value}, as in tools/concordance_signs.py")
    concordance = text(root / "JugcraftConcordance.java")
    for event, subtitle in sg.SOUND_EVENTS.items():
        if f'sound("{event}")' not in concordance:
            err(f"JugcraftConcordance.java: the warning sound {event} is not registered")
        if not (ASSETS / "sounds" / "concordance" / (event.split(".", 1)[1] + ".ogg")).is_file():
            err(f"sign: missing sounds/concordance/{event.split('.', 1)[1]}.ogg (tools/concordance_sounds.py draws it)")
        if lang.get(f"subtitles.{MOD}.{event}") != subtitle:
            err(f"sign: the warning sound {event} needs its subtitle")
    for path in sorted(root.rglob("*.java")):
        source = text(path)
        relative = path.relative_to(root).as_posix()
        if "sendParticles(" in source:
            err(f"concordance/{relative}: server particles go through a sign (Signs.show), so each client draws them as its settings allow")
        if "Presentation." in source and not relative.startswith("sign/") and "animateTick" not in source:
            err(f"concordance/{relative}: the display settings (Presentation) are read only by clients and blocks' animateTick")
    crucible = text(root / "CrucibleBlockEntity.java")
    update = re.search(r"getUpdateTag\(HolderLookup\.Provider registries\) \{(.*?)\n\t\}", crucible, re.S)
    sent = set(re.findall(r'tag\.put\w+\("([a-z_]+)"', update.group(1))) if update else set()
    if sent != {"temperature", "parts", "taste", "murky"}:
        err(f"CrucibleBlockEntity.java: a client is told only the heat, the volume and the spoon's reading, not {sorted(sent)}")
    settings = text(CLIENT_JAVA_ROOT / "ConcordanceSettingsScreen.java")
    if '"screen.jugcraft.concordance.config.intensity." + value.id()' not in settings:
        err("ConcordanceSettingsScreen.java: each intensity is named by screen.jugcraft.concordance.config.intensity.<id>")
    for key in list(sg.CLIENT) + [f"screen.{MOD}.concordance.config.intensity.{name}" for name in sg.INTENSITY]:
        if key not in lang:
            err(f"sign: missing lang {key}")


# Where Fabric's permission events are asked as questions rather than raised by vanilla: the Concordance's Authority,
# and the arms, guns and walkers that ask for the actual player's permission (relative to JAVA_ROOT).
PERMISSION_QUERIES = {
    "PlayerBlockBreakEvents.BEFORE": {"concordance/Authority.java", "walker/DieselWalker.java"},
    # GunShots.allowed asks as the shooter before either a bullet or bayonet deals damage.
    "AttackEntityCallback.EVENT": {"weapons/TwoHanded.java", "guns/GunShots.java"},
}
# What each indirect route asks Authority (relative to the concordance package): roadmap step 28's audit.
AUTHORITY_ROUTES = {
    "ConcordanceEffects.java": ["Authority.answering(level, behind(context))", "Authority.mayStrike(context.level(), context.actor(), behind(context), target)"],
    "Illumination.java": ["return Authority.mayChange(level, player, pos);"],
    "garden/GleanerBlockEntity.java": ["Authority.answering(level, keeper)", "Authority.mayChange(level, answering, pos)"],
    "spirits/ClockworkPorterEntity.java": ["Authority.answering(level, owner) == null", "Authority.mayChangeFor(level, owner, source)",
                                           "Authority.mayChangeFor(level, owner, target)",
                                           "Authority.mayChangeFor(level, request.ticket().requester(), pos)"],
    "spirits/GatheringShadeEntity.java": ["Authority.answering(level, agreement.holder()) == null"],
    "spirits/PorterKeyItem.java": ["Authority.mayChange(level, player, pos)"],
    "spire/ConcordSpire.java": ["Authority.answering(level, state.keeper())", "Authority.mayChange(level, keeper, at)"],
    "sympathy/Sympathy.java": ["Authority.mayStrike(caster, target)", 'return "not_yours";'],
    "sympathy/ScryingGlassItem.java": ["Sympathy.allowed(player, target)", "Hexes.MAX_RANGE"],
    "CircleAnchorBlockEntity.java": ["Authority.present(level, disturber)"],
    "Alchemy.java": ["harmful(dose) ? Intent.HARMFUL"],
}


def check_authority(root, lang):
    """Roadmap step 28: an indirect magical action never does what its player could not do by hand. Every route the
    audit found asks concordance/Authority at the moment of the change; nothing asks with nobody behind it; the stand-in
    that may answer for an absent owner (Fabric's FakePlayer) is made only in Authority; Fabric's permission events are
    asked as questions only where Jugcraft already asks them; absent owners' devices wait unless the server says
    otherwise (the option defaults to false); the Spire Heart gives nothing out through a face; and the new words
    exist."""
    def text(path):
        return path.read_text(encoding="utf-8") if path.exists() else ""
    authority = text(root / "Authority.java")
    if 'ABSENT_OPTION = "concordance.absent_owner_authority"' not in authority:
        err("concordance/Authority.java: the absent-owner option is concordance.absent_owner_authority")
    if "FakePlayer.get(level, new GameProfile(owner, STAND_IN))" not in authority or "!absentOwnersAct()" not in authority:
        err("concordance/Authority.java: a stand-in answers for an absent owner only when the server's option allows it")
    for path in sorted(JAVA_ROOT.rglob("*.java")):
        source = text(path)
        relative = path.relative_to(JAVA_ROOT).as_posix()
        if "FakePlayer" in source and relative != "concordance/Authority.java":
            err(f"{relative}: only concordance/Authority makes the stand-in that answers for an absent owner")
        for event, allowed in PERMISSION_QUERIES.items():
            if f"{event}.invoker()" in source and relative not in allowed:
                err(f"{relative}: {event} is asked as a question only in {sorted(allowed)} (add the site there, reviewed)")
    for path in sorted(root.rglob("*.java")):
        source = text(path)
        relative = path.relative_to(root).as_posix()
        if re.search(r"mayChange\(\s*\w+\s*,\s*null\s*,", source):
            err(f"concordance/{relative}: a change is asked as the person behind it, never as nobody (Authority)")
    for relative, needles in AUTHORITY_ROUTES.items():
        source = text(root / relative)
        for needle in needles:
            if needle not in source:
                err(f"concordance/{relative}: its route must ask Authority as audited ({needle})")
    config = text(JAVA_ROOT / "config" / "JugcraftConfig.java")
    if '"concordance.absent_owner_authority", false' not in config:
        err("JugcraftConfig.java: concordance.absent_owner_authority defaults to false (absent owners' devices wait)")
    heart = text(root / "spire" / "SpireHeartBlockEntity.java")
    if "implements WorldlyContainer" not in heart or not re.search(r"canTakeItemThroughFace\([^)]*\) \{\s*return false;", heart):
        err("spire/SpireHeartBlockEntity.java: the upkeep store takes in through its faces and gives out through none")
    for key in (f"compose.{MOD}.ecology.status.keeper_away", f"compose.{MOD}.hex.reason.not_yours",
                f"message.{MOD}.concordance.courier.too_fast", f"message.{MOD}.concordance.workers.key_refused"):
        if key not in lang:
            err(f"authority: missing lang {key}")


def check_economy(root):
    """Roadmap step 29: tools/concordance_economy.py models every Concordance conversion and every source that time alone
    drives. Its figures must be the game's (each anchor is in its Java or data file), the data conversions must all be
    in it, no cycle of conversions across the systems (every crop's regrow-and-compost loop included, grown by time and
    hastened by a spire) may come back with as much as it started with, and the record's table must be the model's."""
    sys.path.insert(0, str(ROOT / "tools"))
    import concordance_economy as ec
    def text(path):
        return path.read_text(encoding="utf-8") if path.exists() else ""
    for name, anchors in ec.ANCHORS.items():
        for where, needle in anchors:
            if where.startswith("data:"):
                if not (DATA / MOD / where[5:]).is_file():
                    err(f"economy {name}: {where[5:]} is missing")
            elif needle not in text(root / where):
                err(f"economy {name}: concordance/{where} no longer says {needle}; update tools/concordance_economy.py")
    conversions = {path.stem: load(path) or {} for path in (DATA / MOD / "concordance" / "conversion").glob("*.json")}
    modelled = {key: (amount, made) for key, _, amount, _, made in ec.CONVERSIONS}
    for key, entry in conversions.items():
        if modelled.get(key) != (entry.get("from", {}).get("amount"), entry.get("to", {}).get("amount")):
            err(f"economy: the conversion {key} is not modelled as it is in the data ({modelled.get(key)})")
    heart = load(DATA / MOD / "concordance" / "organism" / "verdant_heart.json") or {}
    if (heart.get("cost"), max(heart.get("thriving", 0), heart.get("tolerating", 0))) != (1, 2):
        err("economy: the Verdant Heart's beat is modelled as 1 nutrient for at most 2 Verdance")
    if f"MAX = {ec.FOCUS_MAX};" not in text(root / "rules" / "FocusPool.java"):
        err("economy: FOCUS_MAX is not FocusPool.MAX")
    if ec.VITAE_AN_HOUR != HOURLY_VITAE_BOUND:
        err("economy: VITAE_AN_HOUR is not check_crimson's HOURLY_VITAE_BOUND")
    gaining = ec.gaining_cycles(ec.edges())
    if gaining:
        err(f"economy: a cycle of conversions gives back at least what it took, from {gaining}")
    for name, back, spent in ec.crop_loops():
        if back >= spent:
            err(f"economy: the crop loop {name} composts back {back} quarters for {spent} spent")
    record = text(ROOT / "docs" / "features" / "arcane-concordance-economy.md")
    table = re.search(r"<!-- economy:start -->\n(.*?)\n<!-- economy:end -->", record, re.S)
    if not table or table.group(1).strip() != ec.table().strip():
        err("docs/features/arcane-concordance-economy.md: its table is not tools/concordance_economy.py's (run it and paste)")


# Concordance saves whose loads read nothing (never saved), and the bases whose stamp their subclasses inherit.
UNSAVED_ENTITIES = {"dreaming/DreamWispEntity.java"}
STAMPING_BASES = ("LivingDeviceBlockEntity", "WorkerEntity")
# The records whose entries are independent, read entry by entry (an unreadable one kept as written).
ENTRY_BY_ENTRY = {"spire/SpireRecord.java", "spirits/BoundWills.java", "spirits/WorkerRoster.java", "sky/AstralClaims.java",
                  "starbound/ConclaveProjects.java"}


def check_persistence(root):
    """Roadmap step 30: every format the Concordance saves in a world carries a version. Each SavedData's codec and each
    persistent attachment goes through Saved.versioned (which reads saves from before versions and brings older ones
    forward); each block entity's and creature's save stamps its version (Saved.stamp), directly or through its base;
    and the records whose entries are independent read entry by entry (Saved.keeping), keeping what they cannot read."""
    def text(path):
        return path.read_text(encoding="utf-8") if path.exists() else ""
    saved = text(root / "Saved.java")
    for needle in ("public static <A> Codec<A> versioned(", "public static <K, V> Codec<Kept<K, V>> keeping(",
                   "public static void stamp(ValueOutput output, int version)"):
        if needle not in saved:
            err(f"concordance/Saved.java: missing {needle}")
    for path in sorted(root.rglob("*.java")):
        source = text(path)
        relative = path.relative_to(root).as_posix()
        if re.search(r"extends SavedData\b", source) and not re.search(r"CODEC = Saved\.versioned\(", source):
            err(f"concordance/{relative}: a SavedData's CODEC goes through Saved.versioned (roadmap step 30)")
        for match in re.finditer(r"AttachmentRegistry\.<[^;]*?\.persistent\(([^;]*?)\)", source, re.S):
            if not match.group(1).startswith("Saved.versioned("):
                err(f"concordance/{relative}: a persistent attachment goes through Saved.versioned ({match.group(1)[:40]})")
        if re.search(r"void (saveAdditional|addAdditionalSaveData)\(ValueOutput output\)", source) and relative not in UNSAVED_ENTITIES:
            inherits = any(f"extends {base}" in source for base in STAMPING_BASES)
            if "Saved.stamp(output, " not in source and not inherits:
                err(f"concordance/{relative}: its save stamps its version (Saved.stamp) or inherits a base that does")
            if inherits and "super." not in source.split("ValueOutput output)", 1)[1][:200]:
                err(f"concordance/{relative}: its save calls its base's, which stamps the version")
    for relative in ENTRY_BY_ENTRY:
        if "Saved.keeping(" not in text(root / relative):
            err(f"concordance/{relative}: its entries are read one by one (Saved.keeping), so one it cannot read is kept")


def check_journey():
    """Roadmap step 31: tools/concordance_journey.py's three routes each reach the Architect stage for one player alone
    with only their own research, need their own Spire's practice and differ from one another; the first success is the
    data's; every game test the routes name exists; and the record's routes are the tool's."""
    sys.path.insert(0, str(ROOT / "tools"))
    import concordance_journey as jr
    for problem in jr.problems():
        err(problem)
    path = ROOT / "docs" / "features" / "arcane-concordance-journey.md"
    record = path.read_text(encoding="utf-8") if path.exists() else ""
    block = re.search(r"<!-- journey:start -->\n(.*?)\n<!-- journey:end -->", record, re.S)
    if not block or block.group(1).strip() != "\n".join(jr.table()).strip():
        err("docs/features/arcane-concordance-journey.md: its routes are not tools/concordance_journey.py's (run it and paste)")
    # The client test presses the journal's own key, as a player would.
    client = (ROOT / "src" / "client" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "client" / "JournalClient.java")
    if "public static KeyMapping key()" not in (client.read_text(encoding="utf-8") if client.exists() else ""):
        err("client/JournalClient.java: key() gives tests the journal's key")


def check_delivery():
    """Roadmap step 32: tools/concordance_delivery.py's library integration matrix has one row for every library in the
    lock and none other, agrees with the lock on whether Jugcraft needs each, names only game tests that exist and calls
    unused only what Jugcraft's code never refers to; the delivery record's matrix and the player guide's tables are the
    tool's."""
    sys.path.insert(0, str(ROOT / "tools"))
    import concordance_delivery as dl
    for problem in dl.problems():
        err(problem)
    for name, marker, lines in (("docs/features/arcane-concordance-delivery.md", "matrix", dl.table()),
                                ("docs/ARCANE_CONCORDANCE_GUIDE.md", "guide", dl.guide_tables())):
        path = ROOT / name
        text = path.read_text(encoding="utf-8") if path.exists() else ""
        block = re.search(rf"<!-- {marker}:start -->\n(.*?)\n<!-- {marker}:end -->", text, re.S)
        if not block or block.group(1).strip() != "\n".join(lines).strip():
            err(f"{name}: its {marker} tables are not tools/concordance_delivery.py's (run it and paste)")


if __name__ == "__main__":
    main()
