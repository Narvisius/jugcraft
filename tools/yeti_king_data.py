"""JSON resources for the Yeti King (tools/yeti_king.py, docs/features/yeti-king.md): the GeckoLib bodies of him, his
whelps, his hurled blocks of ice, his falling icicles and his glacial spikes (tools/yeti_king_models.py); the names; his
loot table, Yeti Fur, the Yeti Mitten and the Yeti King's Crown (worn as a costume); the fur's recipes; the costume tags;
and Abominable. The Glacier Maul and the Rimeclaw are Arms VII trophies, written with the other variants
(tools/arms_variants.py), and so is his trophy loot table (loot_table/bosses/yeti_king.json).

Called from tools/agriculture_data.py (assets, loot, recipes, tags, advancements).
"""
from agriculture import COSTUME_HAT_TAG, COSTUME_TAG
from decor_data import MOD, rid
from regatta_data import box, hat_display
import yeti_king as yk
import yeti_king_models as ym

POINT = ("north", "south", "east", "west", "up")


def crown_model():
    """The Yeti King's Crown as worn: a band of blue ice round the head (which fills 1.6 to 14.4), its points rising
    from it, the tallest at the front, two lesser ones beside it, one over each ear and one behind. The points stand on
    the band, so their undersides, which would flicker against its top, are left out."""
    ice, band = "#ice", "#band"
    elements = [box((1.0, 13.0, 1.0), (15.0, 15.0, 15.0), band)]
    for lo, hi in (((7.0, 15.0, 1.0), (9.0, 23.0, 3.0)),      # the tallest, at the front
                   ((3.5, 15.0, 1.0), (5.5, 20.0, 3.0)), ((10.5, 15.0, 1.0), (12.5, 20.0, 3.0)),
                   ((1.0, 15.0, 6.5), (3.0, 19.0, 8.5)), ((13.0, 15.0, 6.5), (15.0, 19.0, 8.5)),
                   ((7.0, 15.0, 13.0), (9.0, 19.0, 15.0))):
        elements.append(box(lo, hi, ice, faces=POINT))
    return {"textures": {"particle": rid("item/yeti_king_crown_ice"), "ice": rid("item/yeti_king_crown_ice"),
                         "band": rid("item/yeti_king_crown_band")},
            "elements": elements, "display": hat_display(-9)}


# ---------------------------------------------------------------- assets

def assets(root, write, lang):
    models = root / "models"
    # His bodies: each entity its own model and clips (GeckoLib finds them by the entity's id).
    for entity, body in yk.GECKO.items():
        geo = ym.MODELS[body]().geo()
        geo["minecraft:geometry"][0]["description"] = {**geo["minecraft:geometry"][0]["description"],
                                                       "identifier": f"geometry.{entity}"}
        write(root / "geckolib" / "models" / "entity" / f"{entity}.geo.json", geo)
        write(root / "geckolib" / "animations" / "entity" / f"{entity}.animation.json", ym.ANIMATIONS[body]())
    for entity, display in yk.ENTITIES.items():
        lang[f"entity.{MOD}.{entity}"] = display

    # Yeti Fur and the Yeti Mitten: icons. The crown: its icon in slots, frames and on the ground; worn or held, the
    # crown.
    for item in yk.ITEMS:
        write(models / "item" / f"{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{item}")}})
        write(root / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{item}")}})
    for item in yk.COSTUMES:
        write(models / "item" / f"{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{item}")}})
        write(models / "item" / f"{item}_worn.json", crown_model())
        write(root / "items" / f"{item}.json", {"model": {
            "type": "minecraft:select", "property": "minecraft:display_context",
            "cases": [{"when": ["gui", "ground", "fixed", "on_shelf"], "model": {"type": "minecraft:model", "model": rid(f"item/{item}")}}],
            "fallback": {"type": "minecraft:model", "model": rid(f"item/{item}_worn")}}})
    for item, display in {**yk.ITEMS, **yk.COSTUMES}.items():
        lang[f"item.{MOD}.{item}"] = display
    lang[f"tooltip.{MOD}.yeti_fur"] = "The Yeti King's fur: shears into white wool, and makes a cheaper Frost Horn"
    lang[f"tooltip.{MOD}.yeti_mitten"] = "In the offhand, its wearer never freezes: powder snow, frost and blizzards leave them warm"
    lang[f"tooltip.{MOD}.yeti_king_crown"] = "A costume: counts for trick-or-treating and the costume contest"

    for key, text in yk.MESSAGES.items():
        lang[f"message.{MOD}.yeti_king.{key}"] = text
    info = yk.ADVANCEMENT
    lang[f"advancements.{MOD}.{info['key']}.title"] = info["title"]
    lang[f"advancements.{MOD}.{info['key']}.description"] = info["description"]


# ---------------------------------------------------------------- data

def chance(items, probability):
    return {"condition": {"type": "minecraft:random_chance", "chance": probability},
            "entries": [{"type": "minecraft:item", "name": rid(item)} for item in items], "rolls": 1}


def loot(out, write):
    """His loot (rolled for each participant by YetiKingLoot, as a gift: nothing in it reads a killer): Yeti Fur, a
    chance of one of his two trophies (as his bosses/yeti_king table holds them), and the chance of each of the rest.
    `out` is the blocks' loot table folder."""
    low, high = yk.FUR
    pools = [{"entries": [{"type": "minecraft:item", "name": rid("yeti_fur"), "modifier": {
        "type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": low, "max": high}}}], "rolls": 1}]
    pools.append(chance(yk.TROPHIES, yk.TROPHY_CHANCE))
    pools += [chance((item,), p) for item, p in yk.CHANCES.items()]
    write(out.parent / "entities" / "yeti_king.json", {"type": "minecraft:gift", "random_sequence": rid("entities/yeti_king"),
                                                        "pools": pools})


def recipes(out, write, conditions):
    for key, recipe in yk.RECIPES.items():
        result = recipe["result"]
        result = result if ":" in result else rid(result)
        body = {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": recipe["shapeless"]}
        write(out / f"{key}.json", {"fabric:load_conditions": conditions(), **body,
                                    "result": {"id": result, "count": recipe.get("count", 1)}})


def tags(tags):
    # The crown is a costume worn on the head.
    for item in yk.COSTUMES:
        tags.add("item", COSTUME_TAG, rid(item))
        tags.add("item", COSTUME_HAT_TAG, rid(item))


def advancements(data, write):
    info = yk.ADVANCEMENT
    write(data / MOD / "advancement" / f"{info['key']}.json", {
        "parent": "minecraft:adventure/root",
        "display": {"icon": {"id": info["icon"]}, "title": {"translate": f"advancements.{MOD}.{info['key']}.title"},
                    "description": {"translate": f"advancements.{MOD}.{info['key']}.description"},
                    "frame": info["frame"], "show_toast": True, "announce_to_chat": True},
        "criteria": {"done": {"trigger": "minecraft:impossible"}}})
