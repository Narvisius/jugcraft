"""JSON resources for the Alpha Werewolf (tools/alpha_werewolf.py, docs/features/alpha-werewolf.md): his GeckoLib body
(tools/alpha_werewolf_models.py); the names of him and his Packmates (drawn as the werewolves are); his loot table, the
Alpha Pelt, the Packfinder Charm and the Alpha's Hood (worn as a costume); the pelt's leather; the costume tags; and
Hunter's Moon. Moonfang and the Howler are Arms VII trophies, written with the other variants (tools/arms_variants.py),
and so is his trophy loot table (loot_table/bosses/werewolf_alpha.json). The pelt counts as a werewolf's pelt for the
Howling Totem through tools/lairs.py WEREWOLF_PELTS. Raked turf, the trail he leaves, is one of the Moonlit Ring's
lair-only blocks (tools/lairs.py, tools/lair_data.py).

Called from tools/agriculture_data.py (assets, loot, recipes, tags, advancements).
"""
from agriculture import COSTUME_HAT_TAG, COSTUME_TAG
from decor_data import MOD, rid
from regatta_data import box, hat_display
import alpha_werewolf as aw
import alpha_werewolf_models as am


def hood_model():
    """The Alpha's Hood as worn: his silver-grey wolf's head pulled over the wearer's (the head fills 1.6 to 14.4), its
    sides down round the temples, his ears standing up from its crown (the right one torn short), and his long muzzle
    jutting forward over the brow. Open at the back of the neck and under the chin."""
    fur, muzzle = "#fur", "#muzzle"
    return {"textures": {"particle": rid("item/alphas_hood_fur"), "fur": rid("item/alphas_hood_fur"),
                         "muzzle": rid("item/alphas_hood_muzzle")},
            "elements": [
                # The crown and sides of the hood over the head.
                box((0.6, 9.0, 0.6), (15.4, 15.6, 15.4), fur),
                # His ears from its crown: the left pricked, the right torn short.
                box((10.5, 15.6, 3.0), (13.5, 19.5, 5.5), fur, faces=("north", "south", "east", "west", "up")),
                box((2.5, 15.6, 3.0), (5.5, 17.5, 5.5), fur, faces=("north", "south", "east", "west", "up")),
                # His muzzle jutting forward over the brow, his nose at its end.
                box((5.0, 9.5, -3.5), (11.0, 13.0, 0.6), muzzle, faces=("north", "east", "west", "up", "down")),
            ],
            "display": hat_display(-8)}


# ---------------------------------------------------------------- assets

def assets(root, write, lang):
    models = root / "models"
    for entity, body in aw.GECKO.items():
        geo = am.MODELS[body]().geo()
        geo["minecraft:geometry"][0]["description"] = {**geo["minecraft:geometry"][0]["description"],
                                                       "identifier": f"geometry.{entity}"}
        write(root / "geckolib" / "models" / "entity" / f"{entity}.geo.json", geo)
        write(root / "geckolib" / "animations" / "entity" / f"{entity}.animation.json", am.ANIMATIONS[body]())
    for entity, display in aw.ENTITIES.items():
        lang[f"entity.{MOD}.{entity}"] = display

    # The pelt and the charm: icons. The hood: its icon in slots, frames and on the ground; worn or held, the hood.
    for item in aw.ITEMS:
        write(models / "item" / f"{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{item}")}})
        write(root / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{item}")}})
    for item in aw.COSTUMES:
        write(models / "item" / f"{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{item}")}})
        write(models / "item" / f"{item}_worn.json", hood_model())
        write(root / "items" / f"{item}.json", {"model": {
            "type": "minecraft:select", "property": "minecraft:display_context",
            "cases": [{"when": ["gui", "ground", "fixed", "on_shelf"], "model": {"type": "minecraft:model", "model": rid(f"item/{item}")}}],
            "fallback": {"type": "minecraft:model", "model": rid(f"item/{item}_worn")}}})
    for item, display in {**aw.ITEMS, **aw.COSTUMES}.items():
        lang[f"item.{MOD}.{item}"] = display
    lang[f"tooltip.{MOD}.alpha_pelt"] = ("The Alpha Werewolf's silver-grey pelt: tans into four leather, or binds a Howling "
                                         "Totem as a werewolf's pelt does")
    lang[f"tooltip.{MOD}.packfinder_charm"] = "In the offhand, every werewolf within 32 blocks glows, for everyone to see"
    lang[f"tooltip.{MOD}.alphas_hood"] = "A costume: counts for trick-or-treating and the costume contest"

    for key, text in aw.MESSAGES.items():
        lang[f"message.{MOD}.alpha_werewolf.{key}"] = text
    info = aw.ADVANCEMENT
    lang[f"advancements.{MOD}.{info['key']}.title"] = info["title"]
    lang[f"advancements.{MOD}.{info['key']}.description"] = info["description"]


# ---------------------------------------------------------------- data

def chance(items, probability):
    return {"condition": {"type": "minecraft:random_chance", "chance": probability},
            "entries": [{"type": "minecraft:item", "name": rid(item)} for item in items], "rolls": 1}


def loot(out, write):
    """His loot (rolled for each participant by AlphaWerewolfLoot, as a gift: nothing in it reads a killer): the Alpha
    Pelt, a chance of one of his two trophies (as his bosses/werewolf_alpha table holds them), and the chance of each of
    the rest. `out` is the blocks' loot table folder."""
    pools = [{"entries": [{"type": "minecraft:item", "name": rid("alpha_pelt")}], "rolls": 1}]
    pools.append(chance(aw.TROPHIES, aw.TROPHY_CHANCE))
    pools += [chance((item,), p) for item, p in aw.CHANCES.items()]
    write(out.parent / "entities" / "alpha_werewolf.json", {"type": "minecraft:gift",
                                                            "random_sequence": rid("entities/alpha_werewolf"),
                                                            "pools": pools})


def recipes(out, write, conditions):
    for key, recipe in aw.RECIPES.items():
        result = recipe["result"]
        result = result if ":" in result else rid(result)
        body = {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": recipe["shapeless"]}
        write(out / f"{key}.json", {"fabric:load_conditions": conditions(), **body,
                                    "result": {"id": result, "count": recipe.get("count", 1)}})


def tags(tags):
    # The hood is a costume worn on the head.
    for item in aw.COSTUMES:
        tags.add("item", COSTUME_TAG, rid(item))
        tags.add("item", COSTUME_HAT_TAG, rid(item))


def advancements(data, write):
    info = aw.ADVANCEMENT
    write(data / MOD / "advancement" / f"{info['key']}.json", {
        "parent": "minecraft:adventure/root",
        "display": {"icon": {"id": info["icon"]}, "title": {"translate": f"advancements.{MOD}.{info['key']}.title"},
                    "description": {"translate": f"advancements.{MOD}.{info['key']}.description"},
                    "frame": info["frame"], "show_toast": True, "announce_to_chat": True},
        "criteria": {"done": {"trigger": "minecraft:impossible"}}})
