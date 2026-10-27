"""JSON resources for the Pumpkin King (tools/pumpkin_king.py, docs/features/pumpkin-king.md): the GeckoLib bodies of him,
his Jack-o'-Straws, his rolling head and the pumpkin he tosses (tools/pumpkin_king_models.py); their names; his loot
table, the King's Ember, the Crowward Charm and the Pumpkin King's Crown (worn as a costume); the King's Lantern (a
block) and its recipe; the costume tags, the crow wards (agriculture/Scarecrows.java) and the creatures the Fright boon
spares (#jugcraft:frightless); and Uncrowned. The Hallowreap and the Crowbill are Arms VII trophies, written with
the other variants (tools/arms_variants.py), and so is his trophy loot table (loot_table/bosses/pumpkin_king.json). His
field, the Crowfield, is part 1's (tools/crowfield.py, tools/lair_data.py).

Called from tools/agriculture_data.py (assets, loot, recipes, tags, advancements).
"""
from agriculture import COSTUME_HAT_TAG, COSTUME_TAG
from decor_data import MOD, rid
from regatta_data import box, hat_display
import arms_variants
import pumpkin_king as pk
import pumpkin_king_models as pm

CROW_WARDS = f"{MOD}:crow_wards"
FRIGHTLESS = f"{MOD}:frightless"


def crown_model():
    """The Pumpkin King's Crown as worn: a band of dried vine round the head (which fills 1.6 to 14.4), five ears of corn
    standing from it, the front one the tallest, each with a husk leaf behind it. Where two parts meet, only one of the
    touching faces is kept, so no two faces flicker against each other."""
    vine, corn, leaf = "#vine", "#corn", "#husk"
    sides = ("north", "south", "east", "west", "up")
    elements = [box((1.0, 13.0, 1.0), (15.0, 15.6, 15.0), vine)]
    for (x0, z0), top in (((7.0, 1.2), 22.0), ((2.0, 2.0), 20.0), ((12.0, 2.0), 20.0), ((2.0, 12.0), 19.5),
                          ((12.0, 12.0), 19.5)):
        elements.append(box((x0, 15.6, z0), (x0 + 2.0, top, z0 + 2.0), corn, faces=sides))
        back = z0 + 2.0 if z0 < 8 else z0
        elements.append(box((x0 - 0.5, 15.6, back), (x0 + 2.5, top - 1.5, back + 0.1), leaf, faces=sides))
    return {"textures": {"particle": rid("item/pumpkin_kings_crown_vine"), "vine": rid("item/pumpkin_kings_crown_vine"),
                         "corn": rid("item/pumpkin_kings_crown_corn"), "husk": rid("item/pumpkin_kings_crown_husk")},
            "elements": elements, "display": hat_display(-9)}


# ---------------------------------------------------------------- assets

def assets(root, write, lang):
    models = root / "models"
    # His bodies: each entity its own model and clips (GeckoLib finds them by the entity's id).
    for entity, body in pk.GECKO.items():
        geo = pm.MODELS[body]().geo()
        geo["minecraft:geometry"][0]["description"] = {**geo["minecraft:geometry"][0]["description"],
                                                       "identifier": f"geometry.{entity}"}
        write(root / "geckolib" / "models" / "entity" / f"{entity}.geo.json", geo)
        write(root / "geckolib" / "animations" / "entity" / f"{entity}.animation.json", pm.ANIMATIONS[body]())
    for entity, display in pk.ENTITIES.items():
        lang[f"entity.{MOD}.{entity}"] = display

    # The ember and the charm: icons. The crown: its icon in slots, frames and on the ground; worn or held, the crown.
    for item in pk.ITEMS:
        write(models / "item" / f"{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{item}")}})
        write(root / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{item}")}})
    for item in pk.COSTUMES:
        write(models / "item" / f"{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{item}")}})
        write(models / "item" / f"{item}_worn.json", crown_model())
        write(root / "items" / f"{item}.json", {"model": {
            "type": "minecraft:select", "property": "minecraft:display_context",
            "cases": [{"when": ["gui", "ground", "fixed", "on_shelf"], "model": {"type": "minecraft:model", "model": rid(f"item/{item}")}}],
            "fallback": {"type": "minecraft:model", "model": rid(f"item/{item}_worn")}}})
    for item, display in {**pk.ITEMS, **pk.COSTUMES}.items():
        lang[f"item.{MOD}.{item}"] = display
    lang[f"tooltip.{MOD}.kings_ember"] = ("The coal of the Pumpkin King's fire: burns as long as a block of coal, and set in an "
                                          "heirloom pumpkin makes a King's Lantern")
    lang[f"tooltip.{MOD}.crowward_charm"] = "In the offhand, crows will not come down on crops within 12 blocks of you"
    lang[f"tooltip.{MOD}.pumpkin_kings_crown"] = "A costume: counts for trick-or-treating and the costume contest"

    # The King's Lantern: a carved pumpkin's block, its face toward whoever placed it.
    for block, display in pk.BLOCKS.items():
        write(models / "block" / f"{block}.json", {"parent": "minecraft:block/orientable", "textures": {
            "front": rid(f"block/{block}_front"), "side": rid(f"block/{block}_side"), "top": rid(f"block/{block}_top")}})
        write(root / "blockstates" / f"{block}.json", {"variants": {
            f"facing={facing}": ({"model": rid(f"block/{block}"), "y": y} if y else {"model": rid(f"block/{block}")})
            for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
        write(root / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{block}")}})
        lang[f"block.{MOD}.{block}"] = display

    for key, text in pk.MESSAGES.items():
        lang[f"message.{MOD}.pumpkin_king.{key}"] = text
    info = pk.ADVANCEMENT
    lang[f"advancements.{MOD}.{info['key']}.title"] = info["title"]
    lang[f"advancements.{MOD}.{info['key']}.description"] = info["description"]


# ---------------------------------------------------------------- data

def chance(items, probability):
    return {"condition": {"type": "minecraft:random_chance", "chance": probability},
            "entries": [{"type": "minecraft:item", "name": rid(item)} for item in items], "rolls": 1}


def loot(out, write):
    """His loot (rolled for each player paid by PumpkinKingLoot, as a gift: nothing in it reads a killer): the King's
    Ember, a chance of one of his two trophies (as his bosses/pumpkin_king table holds them), and the chance of each of the
    rest; and the King's Lantern, which breaks back into itself. `out` is the blocks' loot table folder."""
    pools = [{"entries": [{"type": "minecraft:item", "name": rid("kings_ember")}], "rolls": 1}]
    pools.append(chance(pk.TROPHIES, pk.TROPHY_CHANCE))
    pools += [chance((item,), p) for item, p in pk.CHANCES.items()]
    write(out.parent / "entities" / "pumpkin_king.json", {"type": "minecraft:gift", "random_sequence": rid("entities/pumpkin_king"),
                                                          "pools": pools})
    for block in pk.BLOCKS:
        write(out / f"{block}.json", {"type": "minecraft:block", "random_sequence": rid(f"blocks/{block}"), "pools": [{
            "rolls": 1, "condition": {"type": "minecraft:survives_explosion"},
            "entries": [{"type": "minecraft:item", "name": rid(block)}]}]})


def recipes(out, write, conditions):
    for key, recipe in pk.RECIPES.items():
        result = recipe["result"]
        result = result if ":" in result else rid(result)
        body = {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": recipe["shapeless"]}
        write(out / f"{key}.json", {"fabric:load_conditions": conditions(), **body,
                                    "result": {"id": result, "count": recipe.get("count", 1)}})


def tags(tags):
    # The crown is a costume worn on the head.
    for item in pk.COSTUMES:
        tags.add("item", COSTUME_TAG, rid(item))
        tags.add("item", COSTUME_HAT_TAG, rid(item))
    # The charm keeps crows off, held in the offhand, as a lit scarecrow does.
    tags.add("item", CROW_WARDS, rid("crowward_charm"))
    for block in pk.BLOCKS:
        tags.add("block", "minecraft:mineable/axe", rid(block))
    # The creatures the Fright boon never frightens: the bosses.
    for entity in arms_variants.FRIGHTLESS:
        tags.add("entity_type", FRIGHTLESS, entity)


def advancements(data, write):
    info = pk.ADVANCEMENT
    write(data / MOD / "advancement" / f"{info['key']}.json", {
        "parent": "minecraft:adventure/root",
        "display": {"icon": {"id": info["icon"]}, "title": {"translate": f"advancements.{MOD}.{info['key']}.title"},
                    "description": {"translate": f"advancements.{MOD}.{info['key']}.description"},
                    "frame": info["frame"], "show_toast": True, "announce_to_chat": True},
        "criteria": {"done": {"trigger": "minecraft:impossible"}}})
