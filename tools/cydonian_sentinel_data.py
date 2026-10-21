"""JSON resources for the Cydonian Sentinel (tools/cydonian_sentinel.py, docs/features/cydonian-sentinel.md): the GeckoLib
bodies of it, its Shard Sentries, its Rust Shards, its Quake Spikes and its meteors (tools/cydonian_sentinel_models.py);
the names; its loot table, the Cydonian Shard, the Lowfall Core and the Sentinel's Visor (worn as a costume); the
shards' recipes (a cheaper Altar Keystone, and rare earth oxide from the Crusher); the costume tags; Red Planet; the Red
Storm's damage type; and the Rust boon's Rusted effect's name. The Corelance and the Redstar Cleaver are Arms VII
trophies, written with the other variants (tools/arms_variants.py), and so is its trophy loot table
(loot_table/bosses/cydonian_sentinel.json).

Called from tools/agriculture_data.py (assets, loot, recipes, tags, advancements, worldgen).
"""
from agriculture import COSTUME_HAT_TAG, COSTUME_TAG
from decor_data import MOD, rid
from regatta_data import box, hat_display
import cydonian_sentinel as cs
import cydonian_sentinel_models as sm
import json

SIDES = ("north", "south", "east", "west")


def visor_model():
    """The Sentinel's Visor as worn (the head fills 1.6 to 14.4, its face to the north): cheek plates of red stone down
    either side of the face, a brow plate, a band of dark iron across the eyes with a slit of red light in it (light
    15), and a crest of red stone along the top of the head. The slit is a plane a hair before the band, so they never
    flicker."""
    stone, glow = "#stone", "#glow"
    band = box((1.0, 7.0, 0.8), (15.0, 10.0, 1.6), stone)
    band["faces"]["north"] = {"uv": [1.0, 6.0, 15.0, 9.0], "texture": stone}
    slit = {"from": [3.0, 8.2, 0.7], "to": [13.0, 8.9, 0.7], "shade": False, "light_emission": 15,
            "faces": {"north": {"uv": [3.0, 7.0, 13.0, 8.0], "texture": glow}}}
    return {"textures": {"particle": rid("item/sentinels_visor_stone"), "stone": rid("item/sentinels_visor_stone"),
                         "glow": rid("item/sentinels_visor_glow")},
            "elements": [
                band, slit,
                box((1.0, 10.0, 0.8), (15.0, 11.5, 1.6), stone),
                box((0.8, 3.0, 0.8), (1.6, 11.5, 12.0), stone),
                box((14.4, 3.0, 0.8), (15.2, 11.5, 12.0), stone),
                box((7.0, 14.4, 2.0), (9.0, 17.0, 14.0), stone),
            ],
            "display": hat_display(-6)}


# ---------------------------------------------------------------- assets

def assets(root, write, lang):
    models = root / "models"
    # Its bodies: each entity its own model and clips (GeckoLib finds them by the entity's id).
    for entity, body in cs.GECKO.items():
        geo = sm.MODELS[body]().geo()
        geo["minecraft:geometry"][0]["description"] = {**geo["minecraft:geometry"][0]["description"],
                                                       "identifier": f"geometry.{entity}"}
        write(root / "geckolib" / "models" / "entity" / f"{entity}.geo.json", geo)
        write(root / "geckolib" / "animations" / "entity" / f"{entity}.animation.json", sm.ANIMATIONS[body]())
    for entity, display in cs.ENTITIES.items():
        lang[f"entity.{MOD}.{entity}"] = display

    # The Cydonian Shard and the Lowfall Core: icons. The visor: its icon in slots, frames and on the ground; worn or
    # held, the visor.
    for item in cs.ITEMS:
        write(models / "item" / f"{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{item}")}})
        write(root / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{item}")}})
    for item in cs.COSTUMES:
        write(models / "item" / f"{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{item}")}})
        write(models / "item" / f"{item}_worn.json", visor_model())
        write(root / "items" / f"{item}.json", {"model": {
            "type": "minecraft:select", "property": "minecraft:display_context",
            "cases": [{"when": ["gui", "ground", "fixed", "on_shelf"], "model": {"type": "minecraft:model", "model": rid(f"item/{item}")}}],
            "fallback": {"type": "minecraft:model", "model": rid(f"item/{item}_worn")}}})
    for item, display in {**cs.ITEMS, **cs.COSTUMES}.items():
        lang[f"item.{MOD}.{item}"] = display
    lang[f"tooltip.{MOD}.cydonian_shard"] = ("Red crystal from the Cydonian Sentinel's shell: makes a cheaper Altar "
                                             "Keystone, or the Crusher grinds it to rare earth oxide")
    lang[f"tooltip.{MOD}.lowfall_core"] = "In the offhand, you fall as on Mars: slowly, and with no fall damage"
    lang[f"tooltip.{MOD}.sentinels_visor"] = "A costume: counts for trick-or-treating and the costume contest"

    for key, text in cs.MESSAGES.items():
        lang[f"message.{MOD}.cydonian_sentinel.{key}"] = text
    for wind, text in cs.WIND_NAMES.items():
        lang[f"message.{MOD}.cydonian_sentinel.wind.{wind}"] = text
    info = cs.ADVANCEMENT
    lang[f"advancements.{MOD}.{info['key']}.title"] = info["title"]
    lang[f"advancements.{MOD}.{info['key']}.description"] = info["description"]
    damage = cs.DAMAGE_TYPE
    lang[f"death.attack.{damage['message_id']}"] = damage["death"]
    lang[f"death.attack.{damage['message_id']}.player"] = damage["death_player"]
    lang[f"effect.{MOD}.rusted"] = "Rusted"


# ---------------------------------------------------------------- data

def chance(items, probability):
    return {"condition": {"type": "minecraft:random_chance", "chance": probability},
            "entries": [{"type": "minecraft:item", "name": rid(item)} for item in items], "rolls": 1}


def loot(out, write):
    """Its loot (rolled for each participant by SentinelLoot, as a gift: nothing in it reads a killer): Cydonian Shards,
    a chance of one of its two trophies (as its bosses/cydonian_sentinel table holds them), and the chance of each of the
    rest. `out` is the blocks' loot table folder."""
    low, high = cs.SHARDS
    pools = [{"entries": [{"type": "minecraft:item", "name": rid("cydonian_shard"), "modifier": {
        "type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": low, "max": high}}}], "rolls": 1}]
    pools.append(chance(cs.TROPHIES, cs.TROPHY_CHANCE))
    pools += [chance((item,), p) for item, p in cs.CHANCES.items()]
    write(out.parent / "entities" / "cydonian_sentinel.json", {"type": "minecraft:gift",
                                                               "random_sequence": rid("entities/cydonian_sentinel"),
                                                               "pools": pools})


def recipes(out, write, conditions):
    for key, recipe in cs.RECIPES.items():
        result = recipe["result"]
        result = result if ":" in result else rid(result)
        body = {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": recipe["shapeless"]}
        write(out / f"{key}.json", {"fabric:load_conditions": conditions(*recipe.get("features", ())), **body,
                                    "result": {"id": result, "count": recipe.get("count", 1)}})
    # The Crusher's (a shard ground to rare earth oxide, cs.CRUSHING) is written with the other machine recipes
    # (tools/machines.py).


def tags(tags):
    # The visor is a costume worn on the head.
    for item in cs.COSTUMES:
        tags.add("item", COSTUME_TAG, rid(item))
        tags.add("item", COSTUME_HAT_TAG, rid(item))


def advancements(data, write):
    info = cs.ADVANCEMENT
    write(data / MOD / "advancement" / f"{info['key']}.json", {
        "parent": "minecraft:adventure/root",
        "display": {"icon": {"id": info["icon"]}, "title": {"translate": f"advancements.{MOD}.{info['key']}.title"},
                    "description": {"translate": f"advancements.{MOD}.{info['key']}.description"},
                    "frame": info["frame"], "show_toast": True, "announce_to_chat": True},
        "criteria": {"done": {"trigger": "minecraft:impossible"}}})


def worldgen(data, write):
    """The Red Storm's damage type."""
    damage = cs.DAMAGE_TYPE
    write(data / MOD / "damage_type" / "red_storm.json", {"message_id": damage["message_id"],
                                                          "exhaustion": damage["exhaustion"],
                                                          "scaling": damage["scaling"]})
    # The storm applies its own directional wind impulse. Vanilla damage otherwise adds
    # random knockback first, which can reverse it. Preserve chemistry/gun tag entries
    # written earlier; the owner-data adapter merges its optional entry afterwards.
    path = data / "minecraft" / "tags" / "damage_type" / "no_knockback.json"
    tag = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"replace": False, "values": []}
    if rid("red_storm") not in tag["values"]:
        tag["values"].append(rid("red_storm"))
    write(path, tag)
