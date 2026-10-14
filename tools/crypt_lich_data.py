"""JSON resources for the Crypt Lich (tools/crypt_lich.py, docs/features/crypt-lich.md): the GeckoLib bodies of him, his
Restless Dead, his Soul Bolt and his bones (tools/crypt_lich_models.py); the names; his loot table, Grave Dust, the
Emptied Phylactery and the Lich's Crown (worn as a costume); Grave Dust's recipes; the costume tags; and Nameless. The
Soulreaver and the Gravewarden are Arms VII trophies, written with the other variants (tools/arms_variants.py), and so is
his trophy loot table (loot_table/bosses/crypt_lich.json).

Called from tools/agriculture_data.py (assets, loot, recipes, tags, advancements).
"""
from agriculture import COSTUME_HAT_TAG, COSTUME_TAG
from decor_data import MOD, rid
from regatta_data import box, hat_display
import crypt_lich as cl
import crypt_lich_models as lm

POINT = ("north", "south", "east", "west", "up")
SIDES = ("north", "south", "east", "west")


def crown_model():
    """The Lich's Crown as worn: a band of black iron round the head (which fills 1.6 to 14.4), open over it, dark
    sapphires set round it; eight points standing from it, at its corners and the middle of each side, and a tongue of
    soul-fire on each point, burning (light 12). The points stand on the band and the fire on the points, so their
    undersides, which would flicker against what is under them, are left out."""
    iron, fire = "#iron", "#fire"
    band = (2.4 / 16) * 16  # the band's height in pixels
    elements = []
    for lo, hi, long_faces in (((1.0, 13.0, 1.0), (15.0, 13.0 + band, 2.0), ("north", "south")),
                               ((1.0, 13.0, 14.0), (15.0, 13.0 + band, 15.0), ("north", "south")),
                               ((1.0, 13.0, 2.0), (2.0, 13.0 + band, 14.0), ("east", "west")),
                               ((14.0, 13.0, 2.0), (15.0, 13.0 + band, 14.0), ("east", "west"))):
        wall = box(lo, hi, iron)
        for side in long_faces:
            # The long faces show the band's strip of the texture: its lit top row, the sapphires, its dark foot.
            wall["faces"][side] = {"uv": [1.0, 4.6, 15.0, 7.0], "texture": iron}
        elements.append(wall)
    top = 13.0 + band
    # The points stand square on the band (a block's sixteenth wide, as the band is thick), at its corners and the middle
    # of each side.
    for x in (1.0, 7.5, 14.0):
        for z in (1.0, 7.5, 14.0):
            if (x, z) == (7.5, 7.5):
                continue
            elements.append(box((x, top, z), (x + 1.0, top + 2.6, z + 1.0), iron, faces=POINT))
            flame = {"from": [x + 0.05, top + 2.6, z + 0.05], "to": [x + 0.95, top + 5.0, z + 0.95], "shade": False,
                     "light_emission": 12, "faces": {side: {"uv": [4, 3, 12, 16], "texture": fire} for side in SIDES}}
            elements.append(flame)
    return {"textures": {"particle": rid("item/lichs_crown_iron"), "iron": rid("item/lichs_crown_iron"),
                         "fire": rid("item/lichs_crown_fire")},
            "elements": elements, "display": hat_display(-8)}


# ---------------------------------------------------------------- assets

def assets(root, write, lang):
    models = root / "models"
    # His bodies: each entity its own model and clips (GeckoLib finds them by the entity's id).
    for entity, body in cl.GECKO.items():
        geo = lm.MODELS[body]().geo()
        geo["minecraft:geometry"][0]["description"] = {**geo["minecraft:geometry"][0]["description"],
                                                       "identifier": f"geometry.{entity}"}
        write(root / "geckolib" / "models" / "entity" / f"{entity}.geo.json", geo)
        write(root / "geckolib" / "animations" / "entity" / f"{entity}.animation.json", lm.ANIMATIONS[body]())
    for entity, display in cl.ENTITIES.items():
        lang[f"entity.{MOD}.{entity}"] = display

    # Grave Dust and the Emptied Phylactery: icons. The crown: its icon in slots, frames and on the ground; worn or held,
    # the crown.
    for item in cl.ITEMS:
        write(models / "item" / f"{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{item}")}})
        write(root / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{item}")}})
    for item in cl.COSTUMES:
        write(models / "item" / f"{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{item}")}})
        write(models / "item" / f"{item}_worn.json", crown_model())
        write(root / "items" / f"{item}.json", {"model": {
            "type": "minecraft:select", "property": "minecraft:display_context",
            "cases": [{"when": ["gui", "ground", "fixed", "on_shelf"], "model": {"type": "minecraft:model", "model": rid(f"item/{item}")}}],
            "fallback": {"type": "minecraft:model", "model": rid(f"item/{item}_worn")}}})
    for item, display in {**cl.ITEMS, **cl.COSTUMES}.items():
        lang[f"item.{MOD}.{item}"] = display
    lang[f"tooltip.{MOD}.grave_dust"] = "The Crypt Lich's bones, ground to dust: makes a cheaper Writ of Exhumation, or bone meal"
    lang[f"tooltip.{MOD}.emptied_phylactery"] = "In the offhand, Wither never takes hold of you"
    lang[f"tooltip.{MOD}.lichs_crown"] = "A costume: counts for trick-or-treating and the costume contest"

    for key, text in cl.MESSAGES.items():
        lang[f"message.{MOD}.crypt_lich.{key}"] = text
    info = cl.ADVANCEMENT
    lang[f"advancements.{MOD}.{info['key']}.title"] = info["title"]
    lang[f"advancements.{MOD}.{info['key']}.description"] = info["description"]


# ---------------------------------------------------------------- data

def chance(items, probability):
    return {"condition": {"type": "minecraft:random_chance", "chance": probability},
            "entries": [{"type": "minecraft:item", "name": rid(item)} for item in items], "rolls": 1}


def loot(out, write):
    """His loot (rolled for each participant by CryptLichLoot, as a gift: nothing in it reads a killer): Grave Dust, a
    chance of one of his two trophies (as his bosses/crypt_lich table holds them), and the chance of each of the rest.
    `out` is the blocks' loot table folder."""
    low, high = cl.GRAVE_DUST
    pools = [{"entries": [{"type": "minecraft:item", "name": rid("grave_dust"), "modifier": {
        "type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": low, "max": high}}}], "rolls": 1}]
    pools.append(chance(cl.TROPHIES, cl.TROPHY_CHANCE))
    pools += [chance((item,), p) for item, p in cl.CHANCES.items()]
    write(out.parent / "entities" / "crypt_lich.json", {"type": "minecraft:gift", "random_sequence": rid("entities/crypt_lich"),
                                                         "pools": pools})


def recipes(out, write, conditions):
    for key, recipe in cl.RECIPES.items():
        result = recipe["result"]
        result = result if ":" in result else rid(result)
        body = {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": recipe["shapeless"]}
        write(out / f"{key}.json", {"fabric:load_conditions": conditions(), **body,
                                    "result": {"id": result, "count": recipe.get("count", 1)}})


def tags(tags):
    # The crown is a costume worn on the head.
    for item in cl.COSTUMES:
        tags.add("item", COSTUME_TAG, rid(item))
        tags.add("item", COSTUME_HAT_TAG, rid(item))


def advancements(data, write):
    info = cl.ADVANCEMENT
    write(data / MOD / "advancement" / f"{info['key']}.json", {
        "parent": "minecraft:adventure/root",
        "display": {"icon": {"id": info["icon"]}, "title": {"translate": f"advancements.{MOD}.{info['key']}.title"},
                    "description": {"translate": f"advancements.{MOD}.{info['key']}.description"},
                    "frame": info["frame"], "show_toast": True, "announce_to_chat": True},
        "criteria": {"done": {"trigger": "minecraft:impossible"}}})
