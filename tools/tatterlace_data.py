"""JSON resources for Madame Tatterlace (tools/tatterlace.py, docs/features/tatterlace.md): the GeckoLib bodies of her,
her tossed thimble, her rolling spool, her egg sacs and her spiderlings (tools/tatterlace_models.py); the names; her loot
table, Gossamer Silk, the Golden Thimble and Tatterlace's Headdress (worn as a costume); the silk's recipes; the costume
tags; and Unravelled. The Needle Rapier is an Arms VII trophy, written with the other variants (tools/arms_variants.py),
and so is her trophy loot table (loot_table/bosses/tatterlace.json).

Called from tools/agriculture_data.py (assets, loot, recipes, tags, advancements).
"""
from agriculture import COSTUME_HAT_TAG, COSTUME_TAG
from decor_data import MOD, rid
from regatta_data import box, hat_display
import tatterlace as tt
import tatterlace_models as tm

SIDES = ("north", "south", "east", "west", "up")


def headdress_model():
    """Tatterlace's Headdress as worn: a gold band round the head (which fills 1.6 to 14.4) set with an amethyst at the
    brow, a crown of red velvet on it, and three spires along its front, the middle one tallest, each tipped in gold.
    Where two parts meet, only one of the touching faces is kept, so no two faces flicker against each other."""
    velvet, gold, gem = "#velvet", "#gold", "#gem"
    elements = [
        box((1.0, 13.0, 1.0), (15.0, 15.4, 15.0), gold),                                       # the band
        box((2.4, 15.4, 2.4), (13.6, 21.0, 13.6), velvet, faces=SIDES),                        # the crown
        box((7.0, 13.6, 0.6), (9.0, 14.8, 1.0), gem, faces=("north", "east", "west", "up", "down")),  # the gem
    ]
    for x0, top in ((3.5, 25.0), (7.0, 27.0), (10.5, 25.0)):
        # A spire of velvet, and its gold tip over it.
        elements.append(box((x0, 21.0, 3.4), (x0 + 2.0, top, 5.4), velvet, faces=("north", "south", "east", "west")))
        elements.append(box((x0 - 0.2, top, 3.2), (x0 + 2.2, top + 1.6, 5.6), gold))
    return {"textures": {"particle": rid("item/tatterlace_headdress_velvet"), "velvet": rid("item/tatterlace_headdress_velvet"),
                         "gold": rid("item/tatterlace_headdress_gold"), "gem": rid("item/tatterlace_headdress_gem")},
            "elements": elements, "display": hat_display(-9)}


# ---------------------------------------------------------------- assets

def assets(root, write, lang):
    models = root / "models"
    # Her bodies: each entity its own model and clips (GeckoLib finds them by the entity's id).
    for entity, body in tt.GECKO.items():
        geo = tm.MODELS[body]().geo()
        geo["minecraft:geometry"][0]["description"] = {**geo["minecraft:geometry"][0]["description"],
                                                       "identifier": f"geometry.{entity}"}
        write(root / "geckolib" / "models" / "entity" / f"{entity}.geo.json", geo)
        write(root / "geckolib" / "animations" / "entity" / f"{entity}.animation.json", tm.ANIMATIONS[body]())
    for entity, display in tt.ENTITIES.items():
        lang[f"entity.{MOD}.{entity}"] = display

    # Gossamer Silk and the Golden Thimble: icons. The headdress: its icon in slots, frames and on the ground; worn or
    # held, the headdress.
    for item in tt.ITEMS:
        write(models / "item" / f"{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{item}")}})
        write(root / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{item}")}})
    for item in tt.COSTUMES:
        write(models / "item" / f"{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{item}")}})
        write(models / "item" / f"{item}_worn.json", headdress_model())
        write(root / "items" / f"{item}.json", {"model": {
            "type": "minecraft:select", "property": "minecraft:display_context",
            "cases": [{"when": ["gui", "ground", "fixed", "on_shelf"], "model": {"type": "minecraft:model", "model": rid(f"item/{item}")}}],
            "fallback": {"type": "minecraft:model", "model": rid(f"item/{item}_worn")}}})
    for item, display in {**tt.ITEMS, **tt.COSTUMES}.items():
        lang[f"item.{MOD}.{item}"] = display
    lang[f"tooltip.{MOD}.gossamer_silk"] = "Madame Tatterlace's silk: unpicks into string, and spins a cheaper Cursed Spindle"
    lang[f"tooltip.{MOD}.golden_thimble"] = "In the offhand, turns aside the first projectile every 15 seconds"
    lang[f"tooltip.{MOD}.tatterlace_headdress"] = "A costume: counts for trick-or-treating and the costume contest"
    lang[f"message.{MOD}.golden_thimble.turned"] = "The Golden Thimble turns it aside"

    for key, text in tt.MESSAGES.items():
        lang[f"message.{MOD}.tatterlace.{key}"] = text
    info = tt.ADVANCEMENT
    lang[f"advancements.{MOD}.{info['key']}.title"] = info["title"]
    lang[f"advancements.{MOD}.{info['key']}.description"] = info["description"]


# ---------------------------------------------------------------- data

def chance(item, probability):
    return {"condition": {"type": "minecraft:random_chance", "chance": probability},
            "entries": [{"type": "minecraft:item", "name": rid(item)}], "rolls": 1}


def loot(out, write):
    """Her loot (rolled for each participant by TatterlaceLoot, as a gift: nothing in it reads a killer). `out` is the
    blocks' loot table folder."""
    low, high = tt.SILK
    pools = [{"entries": [{"type": "minecraft:item", "name": rid("gossamer_silk"), "modifier": {
        "type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": low, "max": high}}}], "rolls": 1}]
    pools += [chance(item, p) for item, p in tt.CHANCES.items()]
    write(out.parent / "entities" / "tatterlace.json", {"type": "minecraft:gift", "random_sequence": rid("entities/tatterlace"),
                                                         "pools": pools})


def recipes(out, write, conditions):
    for key, recipe in tt.RECIPES.items():
        result = recipe["result"]
        result = result if ":" in result else rid(result)
        body = {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": recipe["shapeless"]}
        write(out / f"{key}.json", {"fabric:load_conditions": conditions(), **body,
                                    "result": {"id": result, "count": recipe.get("count", 1)}})


def tags(tags):
    # The headdress is a costume worn on the head.
    for item in tt.COSTUMES:
        tags.add("item", COSTUME_TAG, rid(item))
        tags.add("item", COSTUME_HAT_TAG, rid(item))


def advancements(data, write):
    info = tt.ADVANCEMENT
    write(data / MOD / "advancement" / f"{info['key']}.json", {
        "parent": "minecraft:adventure/root",
        "display": {"icon": {"id": info["icon"]}, "title": {"translate": f"advancements.{MOD}.{info['key']}.title"},
                    "description": {"translate": f"advancements.{MOD}.{info['key']}.description"},
                    "frame": info["frame"], "show_toast": True, "announce_to_chat": True},
        "criteria": {"done": {"trigger": "minecraft:impossible"}}})
