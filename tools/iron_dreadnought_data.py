"""JSON resources for the Iron Dreadnought (tools/iron_dreadnought.py, docs/features/iron-dreadnought.md): the GeckoLib
bodies of him, his Sparkers, his hot rivets and his mortar shell (tools/iron_dreadnought_models.py); the names; his loot
table, Salvaged Coil, the Grounding Strap and the Dreadnought Visor (worn as a costume); Salvaged Coil's recipes; the
costume tags; the damage type of his arcs (jugcraft:arc) and the tag of the electric harm the Grounding Strap halves
(jugcraft:electric); and Grounded. The Dynamo Halberd and the Piston Hammer are Arms VII trophies, written with the other
variants (tools/arms_variants.py), and so is his trophy loot table (loot_table/bosses/iron_dreadnought.json).

Called from tools/agriculture_data.py (assets, loot, recipes, tags, advancements).
"""
from agriculture import COSTUME_HAT_TAG, COSTUME_TAG
from decor_data import MOD, rid
from regatta_data import box, hat_display
import iron_dreadnought as dn
import iron_dreadnought_models as dm

SIDES = ("north", "south", "east", "west")


def visor_model():
    """The Dreadnought Visor as worn: a riveted plate of olive drab across the brow and down over the eyes (the head
    fills 1.6 to 14.4), its sides wrapping back round the temples, with his searchlight's housing standing out from its
    middle and the warm lens in it (light 10). Open at the back and under the chin."""
    plate, lamp = "#plate", "#lamp"
    elements = [
        # The brow plate across the front, its sides round the temples.
        box((1.0, 8.0, 0.6), (15.0, 15.0, 1.6), plate),
        box((0.6, 8.0, 1.6), (1.6, 15.0, 9.0), plate),
        box((14.4, 8.0, 1.6), (15.4, 15.0, 9.0), plate),
        # The searchlight's housing on the plate's middle, standing out from it.
        box((5.5, 9.5, -0.6), (10.5, 14.5, 0.6), lamp, faces=("north", "east", "west", "up", "down")),
    ]
    housing = elements[3]
    housing["faces"]["north"] = {"uv": [3.0, 3.0, 13.0, 13.0], "texture": lamp}
    # The lens: a plane just before the housing's face (a plane, so it leaves no open sides).
    lens = {"from": [6.5, 10.5, -0.65], "to": [9.5, 13.5, -0.65], "shade": False, "light_emission": 10,
            "faces": {"north": {"uv": [5.0, 5.0, 11.0, 11.0], "texture": lamp}}}
    elements.append(lens)
    return {"textures": {"particle": rid("item/dreadnought_visor_plate"), "plate": rid("item/dreadnought_visor_plate"),
                         "lamp": rid("item/dreadnought_visor_lamp")},
            "elements": elements, "display": hat_display(-8)}


# ---------------------------------------------------------------- assets

def assets(root, write, lang):
    models = root / "models"
    # His bodies: each entity its own model and clips (GeckoLib finds them by the entity's id).
    for entity, body in dn.GECKO.items():
        geo = dm.MODELS[body]().geo()
        geo["minecraft:geometry"][0]["description"] = {**geo["minecraft:geometry"][0]["description"],
                                                       "identifier": f"geometry.{entity}"}
        write(root / "geckolib" / "models" / "entity" / f"{entity}.geo.json", geo)
        write(root / "geckolib" / "animations" / "entity" / f"{entity}.animation.json", dm.ANIMATIONS[body]())
    for entity, display in dn.ENTITIES.items():
        lang[f"entity.{MOD}.{entity}"] = display

    # Salvaged Coil and the Grounding Strap: icons. The visor: its icon in slots, frames and on the ground; worn or held,
    # the visor.
    for item in dn.ITEMS:
        write(models / "item" / f"{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{item}")}})
        write(root / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{item}")}})
    for item in dn.COSTUMES:
        write(models / "item" / f"{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{item}")}})
        write(models / "item" / f"{item}_worn.json", visor_model())
        write(root / "items" / f"{item}.json", {"model": {
            "type": "minecraft:select", "property": "minecraft:display_context",
            "cases": [{"when": ["gui", "ground", "fixed", "on_shelf"], "model": {"type": "minecraft:model", "model": rid(f"item/{item}")}}],
            "fallback": {"type": "minecraft:model", "model": rid(f"item/{item}_worn")}}})
    for item, display in {**dn.ITEMS, **dn.COSTUMES}.items():
        lang[f"item.{MOD}.{item}"] = display
    lang[f"tooltip.{MOD}.salvaged_coil"] = "Copper from the Iron Dreadnought's coils: makes cheaper Sealed Orders, or redstone"
    lang[f"tooltip.{MOD}.grounding_strap"] = "In the offhand, lightning and electric arcs do you half harm"
    lang[f"tooltip.{MOD}.dreadnought_visor"] = "A costume: counts for trick-or-treating and the costume contest"

    for key, text in dn.MESSAGES.items():
        lang[f"message.{MOD}.iron_dreadnought.{key}"] = text
    lang[f"death.attack.{MOD}.{dn.ARC}"] = "%1$s was struck by an electric arc"
    lang[f"death.attack.{MOD}.{dn.ARC}.player"] = "%1$s was struck by an electric arc from %2$s"
    lang[f"death.attack.{MOD}.{dn.ARC}.item"] = "%1$s was struck by an electric arc from %2$s using %3$s"
    info = dn.ADVANCEMENT
    lang[f"advancements.{MOD}.{info['key']}.title"] = info["title"]
    lang[f"advancements.{MOD}.{info['key']}.description"] = info["description"]


# ---------------------------------------------------------------- data

def chance(items, probability):
    return {"condition": {"type": "minecraft:random_chance", "chance": probability},
            "entries": [{"type": "minecraft:item", "name": rid(item)} for item in items], "rolls": 1}


def loot(out, write):
    """His loot (rolled for each participant by IronDreadnoughtLoot, as a gift: nothing in it reads a killer): Salvaged
    Coil, a chance of one of his two trophies (as his bosses/iron_dreadnought table holds them), and the chance of each
    of the rest. `out` is the blocks' loot table folder."""
    low, high = dn.SALVAGED_COIL
    pools = [{"entries": [{"type": "minecraft:item", "name": rid("salvaged_coil"), "modifier": {
        "type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": low, "max": high}}}], "rolls": 1}]
    pools.append(chance(dn.TROPHIES, dn.TROPHY_CHANCE))
    pools += [chance((item,), p) for item, p in dn.CHANCES.items()]
    write(out.parent / "entities" / "iron_dreadnought.json", {"type": "minecraft:gift",
                                                              "random_sequence": rid("entities/iron_dreadnought"),
                                                              "pools": pools})


def recipes(out, write, conditions):
    for key, recipe in dn.RECIPES.items():
        result = recipe["result"]
        result = result if ":" in result else rid(result)
        body = {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": recipe["shapeless"]}
        write(out / f"{key}.json", {"fabric:load_conditions": conditions(), **body,
                                    "result": {"id": result, "count": recipe.get("count", 1)}})


def tags(tags):
    # The visor is a costume worn on the head.
    for item in dn.COSTUMES:
        tags.add("item", COSTUME_TAG, rid(item))
        tags.add("item", COSTUME_HAT_TAG, rid(item))
    # The electric harm the Grounding Strap halves.
    for damage_type in dn.ELECTRIC:
        tags.add("damage_type", f"{MOD}:electric", damage_type)


def damage_types(data, write):
    """His arcs' damage type: harm like a power line's, scaled by difficulty when he deals it."""
    write(data / MOD / "damage_type" / f"{dn.ARC}.json",
          {"message_id": f"{MOD}.{dn.ARC}", "exhaustion": 0.1, "scaling": "when_caused_by_living_non_player"})


def advancements(data, write):
    info = dn.ADVANCEMENT
    write(data / MOD / "advancement" / f"{info['key']}.json", {
        "parent": "minecraft:adventure/root",
        "display": {"icon": {"id": info["icon"]}, "title": {"translate": f"advancements.{MOD}.{info['key']}.title"},
                    "description": {"translate": f"advancements.{MOD}.{info['key']}.description"},
                    "frame": info["frame"], "show_toast": True, "announce_to_chat": True},
        "criteria": {"done": {"trigger": "minecraft:impossible"}}})
