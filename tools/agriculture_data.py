"""Writes the Agriculture branch's JSON resources from tools/agriculture.py.

Called by generate_material_data.py. Loot tables and worldgen use the Minecraft 26.x formats
(singular "condition", "modifier", minecraft:match_block, inline block states), copied from
vanilla 26.3's own crop and berry-bush files.
"""
import carving_data
import festival_data
import festivity_data
import halloween_data
import night_data
import decor_data
import decor2_data
import decor3_data
import decor4_data
import decor5_data
import decor6_data
import decor7_data
import decor8_data
import decor9_data
import decor10_data
import decor11_data
import decor12_data
import decor13_data
import decor14_data
import chandlery_data
import cider_data
import pantry_data
import crow_data
import firework_data
import lantern_data
import feast_data
import maze_data
import ghost_data
import face_paint_data
import candy_data
import foraging_data
import bat_data
import hay_golem_data
import knitting_data
import pie_data
import spirit_board_data
import turkey_data
import theremin_data
import broom_data
import werewolf_data
import squirrel_data
import pumpkling_data
import midway_data
import ferris_wheel_data
import pinata_data
import hot_air_balloon_data
import leaf_blower_data
import decor15_data
import decor16_data
import decor17_data
import decor18_data
import decor19_data
import decor20_data
import ofrenda_data
import graveyard_data
import regatta_data
import flora_data
import plants_data
import trees_data
from agriculture import (FEATURE, TALL_CROPS, TALL_SECTIONS, CROPS, WILD_CROPS, WILD_PATCH, ITEMS, SICKLES,
                         SICKLE_PATTERN, COOKING, COOK_TIMES, SHAPELESS, SHAPED, POT_RECIPES, EQUIPMENT,
                         HEAT_TAG, HEAT_SOURCES, LEGUME_TAG, STALKS, WILD_BONUS, crop_blocks)

MOD = "jugcraft"


def rid(path):
    return path if ":" in path else f"{MOD}:{path}"


def condition(switch):
    """One load condition for a feature switch, or for a list of switches any one of which loads the recipe (a wood's
    agriculture.WOOD_SWITCHES): the first as "feature", the rest as "or" (config/FeatureEnabledCondition)."""
    if isinstance(switch, str):
        return {"condition": f"{MOD}:feature_enabled", "feature": switch}
    data = {"condition": f"{MOD}:feature_enabled", "feature": switch[0]}
    if len(switch) > 1:
        data["or"] = list(switch[1:])
    return data


def conditions(*features, switch=FEATURE):
    """Load conditions: the recipe's switch (the agriculture feature unless it belongs to another) and any others."""
    return [condition(f) for f in (switch, *features)]


def stage_texture(crop, stage):
    return f"{CROPS[crop]['block'].removesuffix('_crop')}_stage{stage}"


# ---------------------------------------------------------------- equipment models

def plane(axis, at, texture, low=0, high=16, y0=0, y1=16):
    """A double-sided upright plane: across x at z=`at` ("z") or across z at x=`at` ("x")."""
    if axis == "z":
        return {"from": [low, y0, at], "to": [high, y1, at], "shade": False,
                "faces": {"north": {"uv": [0, 0, 16, 16], "texture": texture},
                          "south": {"uv": [0, 0, 16, 16], "texture": texture}}}
    return {"from": [at, y0, low], "to": [at, y1, high], "shade": False,
            "faces": {"west": {"uv": [0, 0, 16, 16], "texture": texture},
                      "east": {"uv": [0, 0, 16, 16], "texture": texture}}}


def box(lo, hi, texture, faces=("north", "south", "east", "west", "up", "down")):
    """A box whose faces take their UVs from its position, so textures line up across elements."""
    return {"from": list(lo), "to": list(hi), "faces": {face: {"texture": texture} for face in faces}}


def trellis_elements():
    """A square lattice cage: four lattice walls just inside the block edge and a post at each corner."""
    elements = [plane("z", 1.5, "#trellis"), plane("z", 14.5, "#trellis"), plane("x", 1.5, "#trellis"), plane("x", 14.5, "#trellis")]
    for x in (1, 14):
        for z in (1, 14):
            elements.append(box((x, 0, z), (x + 1, 16, z + 1), "#post", faces=("north", "south", "east", "west", "up")))
    return elements


def trellis_crop_model():
    """Template for a climbing crop: vanilla's four crop planes inside the trellis cage."""
    crop = [plane("x", 4, "#crop", y0=-1, y1=15), plane("x", 12, "#crop", y0=-1, y1=15),
            plane("z", 4, "#crop", y0=-1, y1=15), plane("z", 12, "#crop", y0=-1, y1=15)]
    return {"parent": "minecraft:block/block", "ambientocclusion": False,
            "textures": {"particle": "#crop"}, "elements": trellis_elements() + crop}


def cooking_pot_model(contents):
    """An iron pot: body, a raised rim around the contents, and a handle on each side."""
    elements = [
        box((3, 0, 3), (13, 7.5, 13), "#side", faces=("north", "south", "east", "west", "down")),
        box((3.5, 7, 3.5), (12.5, 7, 12.5), "#contents", faces=("up",)),
    ]
    for lo, hi in (((2.5, 7, 2.5), (13.5, 8.5, 3.5)), ((2.5, 7, 12.5), (13.5, 8.5, 13.5)),
                   ((2.5, 7, 3.5), (3.5, 8.5, 12.5)), ((12.5, 7, 3.5), (13.5, 8.5, 12.5))):
        elements.append(box(lo, hi, "#rim", faces=("north", "south", "east", "west", "up")))
    for lo, hi in (((1, 5, 7), (3, 6, 9)), ((13, 5, 7), (15, 6, 9))):
        elements.append(box(lo, hi, "#rim"))
    return {"parent": "minecraft:block/block",
            "textures": {"particle": rid("block/cooking_pot_side"), "side": rid("block/cooking_pot_side"),
                         "rim": rid("block/cooking_pot_rim"), "contents": rid(f"block/cooking_pot_{contents}")},
            "elements": elements}


# ---------------------------------------------------------------- assets

def assets(root, write, lang):
    """Blockstates, block and item models, item definitions and names."""
    def crop_model(texture, parent="minecraft:block/crop", key="crop"):
        write(root / "models" / "block" / f"{texture}.json", {"parent": parent, "textures": {key: rid(f"block/{texture}")}})

    write(root / "models" / "block" / "trellis_crop.json", trellis_crop_model())
    for info in TALL_CROPS.values():
        block = info["block"]
        variants = {}
        for age, textures in enumerate(info["textures"]):
            for texture in textures:
                if info.get("trellis"):
                    write(root / "models" / "block" / f"{texture}.json", {"parent": rid("block/trellis_crop"), "textures": {
                        "crop": rid(f"block/{texture}"), "trellis": rid("block/trellis"), "post": rid("block/trellis_post")}})
                else:
                    crop_model(texture)
            for section in range(TALL_SECTIONS):
                # Sections above the plant's height never exist; they reuse its top model.
                variants[f"age={age},section={section}"] = {"model": rid(f"block/{textures[min(section, len(textures) - 1)]}")}
        write(root / "blockstates" / f"{block}.json", {"variants": variants})
        lang[f"block.{MOD}.{block}"] = info["display"]

    for crop, info in CROPS.items():
        if info.get("sculpted"):
            # The mandrake: its crop, wild plant and root are sculpted (tools/flora_data.py).
            wild = next(w for w, winfo in WILD_CROPS.items() if winfo["crop"] == crop)
            flora_data.mandrake_assets(root, write, info["block"], wild, info["seed"], info["stages"])
            lang[f"block.{MOD}.{info['block']}"] = info["display"]
            continue
        for stage in sorted(set(info["stages"])):
            crop_model(stage_texture(crop, stage))
        write(root / "blockstates" / f"{info['block']}.json", {"variants": {
            f"age={age}": {"model": rid(f"block/{stage_texture(crop, stage)}")} for age, stage in enumerate(info["stages"])}})
        lang[f"block.{MOD}.{info['block']}"] = info["display"]

    for wild, info in WILD_CROPS.items():
        if info.get("sculpted"):
            lang[f"block.{MOD}.{wild}"] = info["display"]
            continue
        texture = rid(f"block/{info['texture']}")
        write(root / "models" / "block" / f"{wild}.json", {"parent": "minecraft:block/cross", "textures": {"cross": texture}})
        write(root / "blockstates" / f"{wild}.json", {"variants": {"": {"model": rid(f"block/{wild}")}}})
        write(root / "models" / "item" / f"{wild}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": texture}})
        write(root / "items" / f"{wild}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{wild}")}})
        lang[f"block.{MOD}.{wild}"] = info["display"]

    # Equipment: the trellis and the cooking pot are blocks whose items show the block model.
    write(root / "models" / "block" / "trellis.json", {
        "parent": "minecraft:block/block", "ambientocclusion": False,
        "textures": {"particle": rid("block/trellis_post"), "trellis": rid("block/trellis"), "post": rid("block/trellis_post")},
        "elements": trellis_elements()})
    write(root / "blockstates" / "trellis.json", {"variants": {"": {"model": rid("block/trellis")}}})
    write(root / "models" / "block" / "cooking_pot.json", cooking_pot_model("empty"))
    write(root / "models" / "block" / "cooking_pot_cooking.json", cooking_pot_model("soup"))
    write(root / "blockstates" / "cooking_pot.json", {"variants": {
        "cooking=false": {"model": rid("block/cooking_pot")}, "cooking=true": {"model": rid("block/cooking_pot_cooking")}}})
    for block, info in EQUIPMENT.items():
        write(root / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{block}")}})
        lang[f"block.{MOD}.{block}"] = info["display"]
    lang[f"container.{MOD}.cooking_pot"] = "Cooking Pot"
    lang[f"container.{MOD}.cooking_pot.cold"] = "Needs heat below"

    festival_data.assets(root, write, lang)
    trees_data.assets(root, write)
    plants_data.assets(root, write, lang)
    carving_data.assets(root, write, lang)
    halloween_data.assets(root, write, lang)
    regatta_data.assets(root, write, lang)
    festivity_data.assets(root, write, lang)
    night_data.assets(root, write, lang)
    decor_data.assets(root, write, lang)
    decor2_data.assets(root, write, lang)
    decor3_data.assets(root, write, lang)
    decor4_data.assets(root, write, lang)
    decor5_data.assets(root, write, lang)
    decor6_data.assets(root, write, lang)
    decor7_data.assets(root, write, lang)
    decor8_data.assets(root, write, lang)
    decor9_data.assets(root, write, lang)
    decor10_data.assets(root, write, lang)
    decor11_data.assets(root, write, lang)
    decor12_data.assets(root, write, lang)
    decor13_data.assets(root, write, lang)
    decor14_data.assets(root, write, lang)
    chandlery_data.assets(root, write, lang)
    cider_data.assets(root, write, lang)
    pantry_data.assets(root, write, lang)
    crow_data.assets(root, write, lang)
    firework_data.assets(root, write, lang)
    lantern_data.assets(root, write, lang)
    feast_data.assets(root, write, lang)
    maze_data.assets(root, write, lang)
    ghost_data.assets(root, write, lang)
    face_paint_data.assets(root, write, lang)

    sculpted_seeds = {info["seed"] for info in CROPS.values() if info.get("sculpted")}
    for item, info in list(ITEMS.items()) + list(SICKLES.items()):
        if item in sculpted_seeds:
            # Drawn as a sculpted model by flora_data.mandrake_assets above.
            lang[f"item.{MOD}.{item}"] = info["display"]
            continue
        parent = "minecraft:item/handheld" if item in SICKLES else "minecraft:item/generated"
        write(root / "models" / "item" / f"{item}.json", {"parent": parent, "textures": {"layer0": rid(f"item/{item}")}})
        write(root / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{item}")}})
        lang[f"item.{MOD}.{item}"] = info["display"]
    # After the plain food items: candy corn's tinted model replaces its plain one.
    candy_data.assets(root, write, lang)
    foraging_data.assets(root, write, lang)
    bat_data.assets(root, write, lang)
    hay_golem_data.assets(root, write, lang)
    knitting_data.assets(root, write, lang)
    pie_data.assets(root, write, lang)
    spirit_board_data.assets(root, write, lang)
    turkey_data.assets(root, write, lang)
    theremin_data.assets(root, write, lang)
    broom_data.assets(root, write, lang)
    werewolf_data.assets(root, write, lang)
    squirrel_data.assets(root, write, lang)
    pumpkling_data.assets(root, write, lang)
    midway_data.assets(root, write, lang)
    ferris_wheel_data.assets(root, write, lang)
    pinata_data.assets(root, write, lang)
    hot_air_balloon_data.assets(root, write, lang)
    leaf_blower_data.assets(root, write, lang)
    decor15_data.assets(root, write, lang)
    decor16_data.assets(root, write, lang)
    decor17_data.assets(root, write, lang)
    decor18_data.assets(root, write, lang)
    decor19_data.assets(root, write, lang)
    decor20_data.assets(root, write, lang)
    ofrenda_data.assets(root, write, lang)
    graveyard_data.assets(root, write, lang)


# ---------------------------------------------------------------- loot tables

def match_block(block, **state):
    return {"type": "minecraft:match_block", "blocks": rid(block), "state": {k: str(v) for k, v in state.items()}}


def entry(item, *modifiers, condition=None):
    out = {"type": "minecraft:item", "name": rid(item)}
    if condition:
        out["condition"] = condition
    if modifiers:
        out["modifier"] = list(modifiers)
    return out


def pool(*entries, condition=None):
    out = {"entries": list(entries), "rolls": 1}
    if condition:
        out["condition"] = condition
    return out


def table(block, *pools, decay=True):
    out = {"type": "minecraft:block"}
    if decay:
        out["modifier"] = {"type": "minecraft:explosion_decay"}
    out["pools"] = list(pools)
    out["random_sequence"] = rid(f"blocks/{block}")
    return out


def uniform(low, high):
    return {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": low, "max": high}}


FORTUNE_UNIFORM = {"type": "minecraft:apply_bonus", "enchantment": "minecraft:fortune",
                   "formula": "minecraft:uniform_bonus_count", "parameters": {"bonusMultiplier": 1}}
FORTUNE_BINOMIAL = {"type": "minecraft:apply_bonus", "enchantment": "minecraft:fortune",
                    "formula": "minecraft:binomial_with_bonus_count", "parameters": {"extra": 3, "probability": 0.5714286}}


def loot(data, write):
    out = data / MOD / "loot_table" / "blocks"
    for info in TALL_CROPS.values():
        block, pick = info["block"], info["pick"]
        # Only the bottom section has loot, so a broken plant drops once: its seed back, plus the
        # same harvest as picking if it was ripe.
        pools = [pool(entry(info["seed"]), condition=match_block(block, section=0)),
                 pool(entry(pick["item"], uniform(pick["min"], pick["max"]), FORTUNE_UNIFORM),
                      condition=match_block(block, section=0, age=7))]
        if info["seed"] in [TALL_CROPS[c]["seed"] for c in STALKS["crops"]]:
            # A plant three blocks tall (picked or not) gives dry stalks for corn shocks.
            pools.append(pool(entry(STALKS["item"], uniform(STALKS["min"], STALKS["max"])), condition={
                "type": "minecraft:any_of", "terms": [match_block(block, section=0, age=age) for age in range(STALKS["from_age"], 8)]}))
        if info.get("trellis"):
            # Every block of a climbing plant stands in a trellis, which drops again.
            pools.append(pool(entry("trellis"), condition={"type": "minecraft:survives_explosion"}))
        write(out / f"{block}.json", table(block, *pools))
    for crop, info in CROPS.items():
        block, seed, produce = info["block"], info["seed"], info["produce"]
        ripe = match_block(block, age=7)
        if info["loot"] == "root":
            # Vanilla carrots: the planted item always, plus a binomial bonus when ripe.
            write(out / f"{block}.json", table(block, pool(entry(seed)), pool(entry(produce, FORTUNE_BINOMIAL), condition=ripe)))
        else:
            # Vanilla wheat: the produce when ripe, else a seed; ripe plants add bonus seeds.
            write(out / f"{block}.json", table(
                block,
                pool({"type": "minecraft:alternatives", "children": [entry(produce, condition=ripe), entry(seed)]}),
                pool(entry(seed, FORTUNE_BINOMIAL), condition=ripe)))
    for wild, info in WILD_CROPS.items():
        seed = TALL_CROPS[info["crop"]]["seed"] if info["crop"] in TALL_CROPS else CROPS[info["crop"]]["seed"]
        pools = [pool({"type": "minecraft:alternatives", "children": [
            entry(wild, condition="minecraft:tool/can_shear"),
            entry(seed, uniform(1, 2), {"type": "minecraft:explosion_decay"}),
        ]})]
        if wild in WILD_BONUS:
            # Now and then a different seed, unless shears took the plant.
            bonus = WILD_BONUS[wild]
            pools.append(pool(entry(bonus["item"], {"type": "minecraft:explosion_decay"}), condition={"type": "minecraft:all_of", "terms": [
                {"type": "minecraft:inverted", "term": "minecraft:tool/can_shear"},
                {"type": "minecraft:random_chance", "chance": bonus["chance"]}]}))
        write(out / f"{wild}.json", table(wild, *pools, decay=False))
    for block in EQUIPMENT:
        # Like vanilla scaffolding and cauldrons: the block itself, unless an explosion destroys it.
        write(out / f"{block}.json", table(block, pool(entry(block), condition={"type": "minecraft:survives_explosion"}), decay=False))
    festival_data.loot(out, write)
    trees_data.loot(out, write)
    plants_data.loot(out, write)
    carving_data.loot(out, write)
    halloween_data.loot(out, write)
    regatta_data.loot(out, write)
    festivity_data.loot(out, write)
    festivity_data.trades(data, write)
    night_data.loot(out, write)
    decor_data.loot(out, write)
    decor2_data.loot(out, write)
    decor3_data.loot(out, write)
    decor4_data.loot(out, write)
    decor5_data.loot(out, write)
    decor6_data.loot(out, write)
    decor7_data.loot(out, write)
    decor8_data.loot(out, write)
    decor9_data.loot(out, write)
    decor10_data.loot(out, write)
    decor11_data.loot(out, write)
    decor12_data.loot(out, write)
    decor13_data.loot(out, write)
    decor14_data.loot(out, write)
    chandlery_data.loot(out, write)
    cider_data.loot(out, write)
    pantry_data.loot(out, write)
    candy_data.loot(out, write)
    foraging_data.loot(out, write)
    bat_data.loot(out, write)
    hay_golem_data.loot(out, write)
    knitting_data.loot(out, write)
    pie_data.loot(out, write)
    spirit_board_data.loot(out, write)
    turkey_data.loot(out, write)
    werewolf_data.loot(out, write)
    midway_data.loot(out, write)
    decor15_data.loot(out, write)
    decor16_data.loot(out, write)
    decor17_data.loot(out, write)
    decor18_data.loot(out, write)
    decor19_data.loot(out, write)
    decor20_data.loot(out, write)
    ferris_wheel_data.loot(out, write)
    hot_air_balloon_data.loot(out, write)
    theremin_data.loot(out, write)
    ofrenda_data.loot(out, write)
    graveyard_data.loot(out, write)
    crow_data.loot(out, write)
    firework_data.loot(out, write)
    feast_data.loot(out, write)
    maze_data.loot(out, write)


# ---------------------------------------------------------------- recipes

def recipes(out, write):
    for result, info in COOKING.items():
        for kind, time in COOK_TIMES.items():
            name = result if kind == "smelting" else f"{result}_from_{kind}"
            write(out / f"{name}.json", {"fabric:load_conditions": conditions(), "type": f"minecraft:{kind}",
                                         "category": info.get("category", "food"),
                                         "ingredient": rid(info["input"]), "result": {"id": rid(result)},
                                         "experience": info["xp"], "cookingtime": time})
    for recipe in SHAPELESS:
        data = {"fabric:load_conditions": conditions(*recipe.get("features", []), switch=recipe.get("switch", FEATURE)),
                "type": "minecraft:crafting_shapeless",
                "category": recipe.get("category", "misc")}
        if "group" in recipe:
            data["group"] = recipe["group"]
        data.update({"ingredients": recipe["inputs"], "result": {"id": rid(recipe["result"]), "count": recipe["count"]}})
        write(out / f"{recipe['id']}.json", data)
    for recipe in SHAPED:
        data = {"fabric:load_conditions": conditions(*recipe.get("features", []), switch=recipe.get("switch", FEATURE)),
                "type": "minecraft:crafting_shaped", "category": recipe["category"]}
        if "group" in recipe:
            data["group"] = recipe["group"]
        result = {"id": rid(recipe["result"]), "count": recipe["count"]}
        if "components" in recipe:
            result["components"] = recipe["components"]
        data.update({"pattern": recipe["pattern"], "key": recipe["key"], "result": result})
        write(out / f"{recipe['id']}.json", data)
    for result, info in POT_RECIPES.items():
        out_item = {"id": rid(result)}
        if info.get("count", 1) != 1:
            out_item["count"] = info["count"]
        ingredients = []
        for ref, count in info["inputs"].items():
            part = {"ingredient": ref}
            if count != 1:
                part["count"] = count
            ingredients.append(part)
        write(out / "pot_cooking" / f"{result}.json", {"fabric:load_conditions": conditions(), "type": f"{MOD}:pot_cooking",
                                                      "ingredients": ingredients, "result": out_item, "time": info["time"]})
    for sickle, info in SICKLES.items():
        write(out / f"{sickle}.json", {"fabric:load_conditions": conditions(*info["features"]), "type": "minecraft:crafting_shaped",
                                       "category": "equipment", "pattern": SICKLE_PATTERN,
                                       "key": {"M": info["material"], "S": "minecraft:stick"}, "result": {"id": rid(sickle), "count": 1}})
    carving_data.recipes(out, write, conditions)
    festivity_data.recipes(out, write, conditions)
    firework_data.recipes(out, write, conditions)
    lantern_data.recipes(out, write, conditions)
    feast_data.recipes(out, write, conditions)
    maze_data.recipes(out, write, conditions)
    knitting_data.recipes(out, write, conditions)
    ghost_data.recipes(out, write, conditions)
    face_paint_data.recipes(out, write, conditions)
    decor3_data.recipes(out, write, conditions)
    graveyard_data.recipes(out, write, conditions)
    decor18_data.recipes(out, write, conditions)


# ---------------------------------------------------------------- tags

def tags(tags):
    for item, info in ITEMS.items():
        for tag in info.get("tags", []):
            tags.add("item", tag, rid(item))
            namespace, _, path = tag.partition(":")
            if namespace == "c" and path.startswith(("seeds/", "crops/")):
                tags.add("item", f"c:{path.split('/')[0]}", f"#{tag}")
    for block in crop_blocks() + ["trellis"]:
        tags.add("block", "minecraft:maintains_farmland", rid(block))
    for source in HEAT_SOURCES:
        tags.add("block", HEAT_TAG, source)
    tags.add("block", "minecraft:mineable/axe", rid("trellis"))
    tags.add("block", "minecraft:mineable/pickaxe", rid("cooking_pot"))
    for crop, info in CROPS.items():
        tags.add("block", "minecraft:crops", rid(info["block"]))
        if info["legume"]:
            tags.add("block", LEGUME_TAG, rid(info["block"]))
    festival_data.tags(tags)
    trees_data.tags(tags)
    plants_data.tags(tags)
    carving_data.tags(tags)
    halloween_data.tags(tags)
    werewolf_data.tags(tags)
    squirrel_data.tags(tags)
    pumpkling_data.tags(tags)
    midway_data.tags(tags)
    decor15_data.tags(tags)
    decor16_data.tags(tags)
    decor17_data.tags(tags)
    decor18_data.tags(tags)
    decor19_data.tags(tags)
    decor20_data.tags(tags)
    ferris_wheel_data.tags(tags)
    hot_air_balloon_data.tags(tags)
    regatta_data.tags(tags)
    festivity_data.tags(tags)
    night_data.tags(tags)
    decor_data.tags(tags)
    decor2_data.tags(tags)
    decor3_data.tags(tags)
    decor4_data.tags(tags)
    decor5_data.tags(tags)
    decor6_data.tags(tags)
    decor7_data.tags(tags)
    decor8_data.tags(tags)
    decor9_data.tags(tags)
    decor10_data.tags(tags)
    decor11_data.tags(tags)
    decor12_data.tags(tags)
    decor13_data.tags(tags)
    decor14_data.tags(tags)
    chandlery_data.tags(tags)
    cider_data.tags(tags)
    pantry_data.tags(tags)
    firework_data.tags(tags)
    lantern_data.tags(tags)
    feast_data.tags(tags)
    maze_data.tags(tags)
    candy_data.tags(tags)
    foraging_data.tags(tags)
    bat_data.tags(tags)
    hay_golem_data.tags(tags)
    knitting_data.tags(tags)
    pie_data.tags(tags)
    spirit_board_data.tags(tags)
    turkey_data.tags(tags)
    theremin_data.tags(tags)
    ofrenda_data.tags(tags)
    graveyard_data.tags(tags)


# ---------------------------------------------------------------- worldgen

def advancements(data, write):
    regatta_data.advancements(data, write)


def worldgen(data, write):
    festival_data.worldgen(data, write)
    cider_data.worldgen(data, write)
    trees_data.worldgen(data, write)
    plants_data.worldgen(data, write)
    halloween_data.worldgen(data, write)
    foraging_data.worldgen(data, write)
    werewolf_data.worldgen(data, write)
    spread = WILD_PATCH["spread_xz"]
    for wild in WILD_CROPS:
        write(data / MOD / "worldgen" / "feature" / f"{wild}.json",
              {"type": "minecraft:simple_block", "to_place": {"id": rid(wild)}})
        write(data / MOD / "worldgen" / "placed_feature" / f"patch_{wild}.json", {
            "feature": rid(wild),
            "placement": [
                {"type": "minecraft:rarity_filter", "chance": WILD_PATCH["rarity"]},
                {"type": "minecraft:in_square"},
                {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"},
                {"type": "minecraft:biome"},
                {"type": "minecraft:count", "count": WILD_PATCH["tries"]},
                {"type": "minecraft:offset",
                 "x": {"type": "minecraft:trapezoid", "max": spread, "min": -spread, "plateau": 0},
                 "y": {"type": "minecraft:trapezoid", "max": WILD_PATCH["spread_y"], "min": -WILD_PATCH["spread_y"], "plateau": 0},
                 "z": {"type": "minecraft:trapezoid", "max": spread, "min": -spread, "plateau": 0}},
                {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
                    {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"},
                    {"type": "minecraft:matching_blocks", "blocks": "minecraft:grass_block", "offset": [0, -1, 0]},
                ]}},
            ],
        })
