"""JSON resources for the Cinder Tyrant (tools/cinder_tyrant.py, docs/features/cinder-tyrant.md): the GeckoLib bodies of
him, his Cinderlings, his gobs of magma and his falling cinders (tools/cinder_tyrant_models.py); the names; his loot
table, the Tyrant Scale, the Salamander Charm and the Tyrant's Crest (worn as a costume); the scale's recipes; the
costume tags; and Tempered. The Cinderbrand and the Magmaw are Arms VII trophies, written with the other variants
(tools/arms_variants.py), and so is his trophy loot table (loot_table/bosses/cinder_tyrant.json).

Called from tools/agriculture_data.py (assets, loot, recipes, tags, advancements).
"""
from agriculture import COSTUME_HAT_TAG, COSTUME_TAG
from decor_data import MOD, rid
from regatta_data import box, hat_display
import cinder_tyrant as ct
import cinder_tyrant_models as tm

POINT = ("north", "south", "east", "west", "up")


def crest_model():
    """The Tyrant's Crest as worn: a ridge of obsidian along the crown of the head (which fills 1.6 to 14.4), three
    spikes rising from it, the tallest at the front, and magma glowing between them. The spikes and the magma stand on
    the ridge, so their undersides, which would flicker against its top, are left out."""
    obsidian, magma = "#obsidian", "#magma"
    elements = [box((6.0, 14.0, 1.5), (10.0, 16.0, 14.5), obsidian)]
    for z, height in ((2.0, 9.0), (6.5, 7.0), (11.0, 5.0)):
        elements.append(box((6.5, 16.0, z), (9.5, 16.0 + height, z + 2.5), obsidian, faces=POINT))
    for z in (4.5, 9.0):
        glowing = box((7.0, 16.0, z), (9.0, 17.0, z + 2.0), magma, faces=POINT)
        glowing["light_emission"] = 15
        elements.append(glowing)
    return {"textures": {"particle": rid("item/tyrant_crest_obsidian"), "obsidian": rid("item/tyrant_crest_obsidian"),
                         "magma": rid("item/tyrant_crest_magma")},
            "elements": elements, "display": hat_display(-9)}


# ---------------------------------------------------------------- assets

def assets(root, write, lang):
    models = root / "models"
    # His bodies: each entity its own model and clips (GeckoLib finds them by the entity's id).
    for entity, body in ct.GECKO.items():
        geo = tm.MODELS[body]().geo()
        geo["minecraft:geometry"][0]["description"] = {**geo["minecraft:geometry"][0]["description"],
                                                       "identifier": f"geometry.{entity}"}
        write(root / "geckolib" / "models" / "entity" / f"{entity}.geo.json", geo)
        write(root / "geckolib" / "animations" / "entity" / f"{entity}.animation.json", tm.ANIMATIONS[body]())
    for entity, display in ct.ENTITIES.items():
        lang[f"entity.{MOD}.{entity}"] = display

    # The Tyrant Scale and the Salamander Charm: icons. The crest: its icon in slots, frames and on the ground; worn or
    # held, the crest.
    for item in ct.ITEMS:
        write(models / "item" / f"{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{item}")}})
        write(root / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{item}")}})
    for item in ct.COSTUMES:
        write(models / "item" / f"{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{item}")}})
        write(models / "item" / f"{item}_worn.json", crest_model())
        write(root / "items" / f"{item}.json", {"model": {
            "type": "minecraft:select", "property": "minecraft:display_context",
            "cases": [{"when": ["gui", "ground", "fixed", "on_shelf"], "model": {"type": "minecraft:model", "model": rid(f"item/{item}")}}],
            "fallback": {"type": "minecraft:model", "model": rid(f"item/{item}_worn")}}})
    for item, display in {**ct.ITEMS, **ct.COSTUMES}.items():
        lang[f"item.{MOD}.{item}"] = display
    lang[f"tooltip.{MOD}.tyrant_scale"] = "A scale of the Cinder Tyrant's obsidian: crushes into magma cream, and makes a cheaper Kiln Seal"
    lang[f"tooltip.{MOD}.salamander_charm"] = "In the offhand, hot ground never burns you: magma blocks and molten slag"
    lang[f"tooltip.{MOD}.tyrant_crest"] = "A costume: counts for trick-or-treating and the costume contest"

    for key, text in ct.MESSAGES.items():
        lang[f"message.{MOD}.cinder_tyrant.{key}"] = text
    info = ct.ADVANCEMENT
    lang[f"advancements.{MOD}.{info['key']}.title"] = info["title"]
    lang[f"advancements.{MOD}.{info['key']}.description"] = info["description"]


# ---------------------------------------------------------------- data

def chance(items, probability):
    return {"condition": {"type": "minecraft:random_chance", "chance": probability},
            "entries": [{"type": "minecraft:item", "name": rid(item)} for item in items], "rolls": 1}


def loot(out, write):
    """His loot (rolled for each participant by CinderTyrantLoot, as a gift: nothing in it reads a killer): Tyrant Scales,
    a chance of one of his two trophies (as his bosses/cinder_tyrant table holds them), and the chance of each of the
    rest. `out` is the blocks' loot table folder."""
    low, high = ct.SCALES
    pools = [{"entries": [{"type": "minecraft:item", "name": rid("tyrant_scale"), "modifier": {
        "type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": low, "max": high}}}], "rolls": 1}]
    pools.append(chance(ct.TROPHIES, ct.TROPHY_CHANCE))
    pools += [chance((item,), p) for item, p in ct.CHANCES.items()]
    write(out.parent / "entities" / "cinder_tyrant.json", {"type": "minecraft:gift",
                                                            "random_sequence": rid("entities/cinder_tyrant"), "pools": pools})


def recipes(out, write, conditions):
    for key, recipe in ct.RECIPES.items():
        result = recipe["result"]
        result = result if ":" in result else rid(result)
        body = {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": recipe["shapeless"]}
        write(out / f"{key}.json", {"fabric:load_conditions": conditions(), **body,
                                    "result": {"id": result, "count": recipe.get("count", 1)}})


def tags(tags):
    # The crest is a costume worn on the head.
    for item in ct.COSTUMES:
        tags.add("item", COSTUME_TAG, rid(item))
        tags.add("item", COSTUME_HAT_TAG, rid(item))


def advancements(data, write):
    info = ct.ADVANCEMENT
    write(data / MOD / "advancement" / f"{info['key']}.json", {
        "parent": "minecraft:adventure/root",
        "display": {"icon": {"id": info["icon"]}, "title": {"translate": f"advancements.{MOD}.{info['key']}.title"},
                    "description": {"translate": f"advancements.{MOD}.{info['key']}.description"},
                    "frame": info["frame"], "show_toast": True, "announce_to_chat": True},
        "criteria": {"done": {"trigger": "minecraft:impossible"}}})
