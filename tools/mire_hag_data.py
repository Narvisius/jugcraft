"""JSON resources for the Mire Hag (tools/mire_hag.py, docs/features/mire-hag.md): the GeckoLib bodies of her, her
Likenesses (her own model and clips under their own name), her Root Knot, her roots, her grasping hands and her brew
(tools/mire_hag_models.py); the names; her loot table, Hagmoss, the Hag's Eye and the Hag's Shawl (worn as a costume);
Hagmoss's recipes; the costume tags; and Seen Through. The Hagthorn and the Bogfang are Arms VII trophies, written with
the other variants (tools/arms_variants.py), and so is her trophy loot table (loot_table/bosses/mire_hag.json).

Called from tools/agriculture_data.py (assets, loot, recipes, tags, advancements).
"""
from agriculture import COSTUME_HAT_TAG, COSTUME_TAG
from decor_data import MOD, rid
from regatta_data import box, hat_display
import mire_hag as mh
import mire_hag_models as hm

POINT = ("north", "south", "east", "west", "up")


def shawl_model():
    """The Hag's Shawl as worn: a hood of grey moss over the head (which fills 1.6 to 14.4), its crown, its sides and
    back falling to the shoulders, a brim over the brow, a fringe behind; two blades of sedge, crossed, standing out of
    its crown."""
    moss = "#moss"
    elements = [
        box((1.0, 14.4, 1.0), (15.0, 16.0, 15.0), moss),
        box((0.8, 6.0, 1.0), (1.6, 14.4, 15.0), moss),
        box((14.4, 6.0, 1.0), (15.2, 14.4, 15.0), moss),
        box((1.6, 5.0, 14.4), (14.4, 14.4, 15.2), moss),
        box((1.6, 12.6, 0.2), (14.4, 14.4, 1.0), moss),
        box((2.0, 2.0, 14.8), (14.0, 5.0, 15.6), moss),
    ]
    for angle in (45, -45):
        elements.append({"from": [5.0, 16.0, 8.0], "to": [11.0, 22.0, 8.0], "shade": False,
                         "rotation": {"origin": [8.0, 16.0, 8.0], "axis": "y", "angle": angle},
                         "faces": {"north": {"uv": [5, 0, 11, 6], "texture": "#sedge"},
                                   "south": {"uv": [5, 0, 11, 6], "texture": "#sedge"}}})
    return {"textures": {"particle": rid("item/hags_shawl_moss"), "moss": rid("item/hags_shawl_moss"),
                         "sedge": rid("item/hags_shawl_sedge")},
            "elements": elements, "display": hat_display(-7)}


# ---------------------------------------------------------------- assets

def assets(root, write, lang):
    models = root / "models"
    # Her bodies: each entity its own model and clips (GeckoLib finds them by the entity's id). Her Likenesses are she:
    # her geometry under their name, and her clips, which keep her name.
    for entity, body in mh.GECKO.items():
        geo = hm.MODELS[body]().geo()
        geo["minecraft:geometry"][0]["description"] = {**geo["minecraft:geometry"][0]["description"],
                                                       "identifier": f"geometry.{entity}"}
        write(root / "geckolib" / "models" / "entity" / f"{entity}.geo.json", geo)
        write(root / "geckolib" / "animations" / "entity" / f"{entity}.animation.json", hm.ANIMATIONS[body]())
    for entity, display in mh.ENTITIES.items():
        lang[f"entity.{MOD}.{entity}"] = display

    # Hagmoss and the Hag's Eye: icons. The shawl: its icon in slots, frames and on the ground; worn or held, the hood.
    for item in mh.ITEMS:
        write(models / "item" / f"{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{item}")}})
        write(root / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{item}")}})
    for item in mh.COSTUMES:
        write(models / "item" / f"{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{item}")}})
        write(models / "item" / f"{item}_worn.json", shawl_model())
        write(root / "items" / f"{item}.json", {"model": {
            "type": "minecraft:select", "property": "minecraft:display_context",
            "cases": [{"when": ["gui", "ground", "fixed", "on_shelf"], "model": {"type": "minecraft:model", "model": rid(f"item/{item}")}}],
            "fallback": {"type": "minecraft:model", "model": rid(f"item/{item}_worn")}}})
    for item, display in {**mh.ITEMS, **mh.COSTUMES}.items():
        lang[f"item.{MOD}.{item}"] = display
    lang[f"tooltip.{MOD}.hagmoss"] = "Grey moss from the Mire Hag's shawl: makes a cheaper Hag Stone, light grey dye or compost"
    lang[f"tooltip.{MOD}.hags_eye"] = "In the offhand, invisible creatures near you glow"
    lang[f"tooltip.{MOD}.hags_shawl"] = "A costume: counts for trick-or-treating and the costume contest"

    for key, text in mh.MESSAGES.items():
        lang[f"message.{MOD}.mire_hag.{key}"] = text
    info = mh.ADVANCEMENT
    lang[f"advancements.{MOD}.{info['key']}.title"] = info["title"]
    lang[f"advancements.{MOD}.{info['key']}.description"] = info["description"]


# ---------------------------------------------------------------- data

def chance(items, probability):
    return {"condition": {"type": "minecraft:random_chance", "chance": probability},
            "entries": [{"type": "minecraft:item", "name": rid(item)} for item in items], "rolls": 1}


def loot(out, write):
    """Her loot (rolled for each participant by MireHagLoot, as a gift: nothing in it reads a killer): Hagmoss, a chance
    of one of her two trophies (as her bosses/mire_hag table holds them), and the chance of each of the rest. `out` is the
    blocks' loot table folder."""
    low, high = mh.HAGMOSS
    pools = [{"entries": [{"type": "minecraft:item", "name": rid("hagmoss"), "modifier": {
        "type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": low, "max": high}}}], "rolls": 1}]
    pools.append(chance(mh.TROPHIES, mh.TROPHY_CHANCE))
    pools += [chance((item,), p) for item, p in mh.CHANCES.items()]
    write(out.parent / "entities" / "mire_hag.json", {"type": "minecraft:gift", "random_sequence": rid("entities/mire_hag"),
                                                       "pools": pools})


def recipes(out, write, conditions):
    for key, recipe in mh.RECIPES.items():
        result = recipe["result"]
        result = result if ":" in result else rid(result)
        body = {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": recipe["shapeless"]}
        write(out / f"{key}.json", {"fabric:load_conditions": conditions(), **body,
                                    "result": {"id": result, "count": recipe.get("count", 1)}})


def tags(tags):
    # The shawl is a costume worn on the head.
    for item in mh.COSTUMES:
        tags.add("item", COSTUME_TAG, rid(item))
        tags.add("item", COSTUME_HAT_TAG, rid(item))


def advancements(data, write):
    info = mh.ADVANCEMENT
    write(data / MOD / "advancement" / f"{info['key']}.json", {
        "parent": "minecraft:adventure/root",
        "display": {"icon": {"id": info["icon"]}, "title": {"translate": f"advancements.{MOD}.{info['key']}.title"},
                    "description": {"translate": f"advancements.{MOD}.{info['key']}.description"},
                    "frame": info["frame"], "show_toast": True, "announce_to_chat": True},
        "criteria": {"done": {"trigger": "minecraft:impossible"}}})
