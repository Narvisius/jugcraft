"""JSON resources for Vesperine, the Last Reaper (tools/vesperine.py, docs/features/vesperine.md): the GeckoLib bodies of
her, her skulls, her thrown scythe and her thralls (tools/vesperine_models.py); the names; her loot table, the Reaper's
Hood and Reaper's Shade, the Dirge and Requiem skull trophies (blocks that hover and glow, worn as costumes); the Shade
Wreath and hood recipes; the costume tags; and The Last Harvest. The Vesper Scythe is an Arms VII trophy, written with
the other variants (tools/arms_variants.py), and so is her trophy loot table (loot_table/bosses/vesperine.json).

Called from tools/agriculture_data.py (assets, loot, recipes, tags, advancements).
"""
from agriculture import COSTUME_HAT_TAG, COSTUME_TAG
from decor_data import MOD, HORIZONTAL, rid, self_drop
from regatta_data import box, hat_display
import vesperine as vs
import vesperine_models as vm


# ---------------------------------------------------------------- the trophies and the hood

def skull_block(kind):
    """A trophy skull, a block's worth of Dirge or Requiem: a cranium over a heavy brow, square sockets glowing in their
    colour (a lit plane over the face), the upper teeth and a jaw; it hovers a pixel off the ground."""
    tex = {"particle": rid(f"block/{kind}_skull_bone"), "bone": rid(f"block/{kind}_skull_bone"),
           "face": rid(f"block/{kind}_skull_face"), "teeth": rid(f"block/{kind}_skull_teeth"),
           "glow": rid(f"block/{kind}_skull_glow")}
    cranium = box((3, 5, 3), (13, 13, 13), "#bone", front="#face")
    brow = box((3, 9.5, 2.25), (13, 10.75, 3), "#bone")
    maxilla = box((3.75, 2.75, 2.5), (12.25, 5, 10), "#bone", front="#teeth")
    jaw = box((3.75, 1, 2.75), (12.25, 2.75, 9.5), "#bone", front="#teeth")
    glow = {"from": [3, 5, 2.95], "to": [13, 13, 2.95], "shade": False, "light_emission": 15,
            "faces": {"north": {"uv": [0, 0, 10, 8], "texture": "#glow"}}}
    display = {
        "gui": {"rotation": [30, 225, 0], "translation": [0, 1, 0], "scale": [0.8, 0.8, 0.8]},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, 1, 0], "scale": [0.8, 0.8, 0.8]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
        # Worn, it covers the head as a carved pumpkin does.
        "head": {"rotation": [0, 0, 0], "translation": [0, 0.5, 0], "scale": [1.3, 1.3, 1.3]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.45, 0.45, 0.45]},
        "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.45, 0.45, 0.45]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.45, 0.45, 0.45]},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.45, 0.45, 0.45]},
    }
    return {"parent": "minecraft:block/block", "textures": tex,
            "elements": [cranium, brow, maxilla, jaw, glow], "display": display}


def hood_model():
    """The Reaper's Hood as worn: a black cowl over the head (which fills 1.6 to 14.4), open at the face and lined in
    crimson round it, drooping to a point behind, and a short mantle over the shoulders."""
    cloth, lining = "#cloth", "#lining"
    # The pieces meet face to face, never overlapping in one plane, so no two faces flicker against each other.
    brim = box((0.4, 12.4, 0.4), (15.6, 15.6, 1.6), cloth)                               # the brim over the brow
    right = box((0.4, 1, 0.4), (2.8, 12.4, 1.6), cloth)                                  # framing the face
    left = box((13.2, 1, 0.4), (15.6, 12.4, 1.6), cloth)
    elements = [
        box((1.6, 14.4, 1.6), (14.4, 15.6, 15.6), cloth),                                # over the crown
        box((1.6, 1, 14.4), (14.4, 14.4, 15.6), cloth),                                  # down the back
        box((0.4, 1, 1.6), (1.6, 15.6, 15.6), cloth),                                    # her right side
        box((14.4, 1, 1.6), (15.6, 15.6, 15.6), cloth),                                  # her left side
        brim, right, left,
        box((6, 13, 15.6), (10, 16, 18.4), cloth),                                       # the point, drooping behind
        box((7, 11.5, 18.4), (9, 14.5, 20), cloth),
        box((-0.5, -3, -0.5), (16.5, 1, 16.5), cloth),                                  # the mantle
    ]
    # The crimson lining shows round the inside of the opening.
    for element in (brim, right, left):
        element["faces"]["south"]["texture"] = lining
    right["faces"]["east"]["texture"] = lining
    left["faces"]["west"]["texture"] = lining
    display = hat_display(-6)
    return {"textures": {"particle": rid("item/reaper_hood_cloth"), "cloth": rid("item/reaper_hood_cloth"),
                         "lining": rid("item/reaper_hood_lining")},
            "elements": elements, "display": display}


# ---------------------------------------------------------------- assets

def assets(root, write, lang):
    models = root / "models"
    # Her bodies: each entity its own model and clips (GeckoLib finds them by the entity's id); Dirge and Requiem
    # share the skull's.
    built, clips = {}, {}
    for entity, body in vs.GECKO.items():
        if body not in built:
            built[body] = vm.MODELS[body]().geo()
            clips[body] = vm.ANIMATIONS[body]()
        geo = {**built[body], "minecraft:geometry": [dict(built[body]["minecraft:geometry"][0])]}
        geo["minecraft:geometry"][0]["description"] = {**geo["minecraft:geometry"][0]["description"],
                                                       "identifier": f"geometry.{entity}"}
        write(root / "geckolib" / "models" / "entity" / f"{entity}.geo.json", geo)
        write(root / "geckolib" / "animations" / "entity" / f"{entity}.animation.json", clips[body])
    for entity, display in vs.ENTITIES.items():
        lang[f"entity.{MOD}.{entity}"] = display

    # Reaper's Shade: an icon. The Reaper's Hood: its icon in slots, frames and on the ground; worn or held, the hood.
    write(models / "item" / "reaper_shade.json", {"parent": "minecraft:item/generated",
                                                  "textures": {"layer0": rid("item/reaper_shade")}})
    write(root / "items" / "reaper_shade.json", {"model": {"type": "minecraft:model", "model": rid("item/reaper_shade")}})
    write(models / "item" / "reaper_hood.json", {"parent": "minecraft:item/generated",
                                                 "textures": {"layer0": rid("item/reaper_hood")}})
    write(models / "item" / "reaper_hood_worn.json", hood_model())
    write(root / "items" / "reaper_hood.json", {"model": {
        "type": "minecraft:select", "property": "minecraft:display_context",
        "cases": [{"when": ["gui", "ground", "fixed", "on_shelf"],
                   "model": {"type": "minecraft:model", "model": rid("item/reaper_hood")}}],
        "fallback": {"type": "minecraft:model", "model": rid("item/reaper_hood_worn")}}})
    for item, display in vs.ITEMS.items():
        lang[f"item.{MOD}.{item}"] = display
    lang[f"tooltip.{MOD}.reaper_shade"] = "Night-black cloth of the Last Reaper: for Shade Wreaths and her hood"
    lang[f"tooltip.{MOD}.reaper_hood"] = "A costume: counts for trick-or-treating and the costume contest"

    # The skull trophies, facing whoever placed them.
    for kind in ("dirge", "requiem"):
        name = f"{kind}_skull"
        write(models / "block" / f"{name}.json", skull_block(kind))
        write(root / "blockstates" / f"{name}.json", {"variants": {f"facing={f}": {"model": rid(f"block/{name}"), **(
            {"y": {"north": 0, "east": 90, "south": 180, "west": 270}[f]} if f != "north" else {})} for f in HORIZONTAL}})
        write(root / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{name}")}})
        lang[f"block.{MOD}.{name}"] = vs.TROPHIES[name]

    for key, text in vs.MESSAGES.items():
        lang[f"message.{MOD}.vesperine.{key}"] = text
    info = vs.ADVANCEMENT
    lang[f"advancements.{MOD}.{info['key']}.title"] = info["title"]
    lang[f"advancements.{MOD}.{info['key']}.description"] = info["description"]


# ---------------------------------------------------------------- data

def chance(item, probability):
    return {"condition": {"type": "minecraft:random_chance", "chance": probability},
            "entries": [{"type": "minecraft:item", "name": rid(item)}], "rolls": 1}


def loot(out, write):
    """Her loot (rolled for each participant by VesperineLoot, as a gift: nothing in it reads a killer), and each trophy
    block's own drop. `out` is the blocks' loot table folder."""
    low, high = vs.SHADE
    pools = [{"entries": [{"type": "minecraft:item", "name": rid("reaper_shade"), "modifier": {
        "type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": low, "max": high}}}], "rolls": 1}]
    pools += [chance(item, p) for item, p in vs.CHANCES.items()]
    write(out.parent / "entities" / "vesperine.json", {"type": "minecraft:gift", "random_sequence": rid("entities/vesperine"),
                                                        "pools": pools})
    for name in vs.TROPHIES:
        write(out / f"{name}.json", self_drop(name))


def recipes(out, write, conditions):
    for key, recipe in vs.RECIPES.items():
        result = {"id": rid(recipe["result"]), "count": 1}
        if "shapeless" in recipe:
            body = {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": recipe["shapeless"]}
        else:
            body = {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": recipe["pattern"],
                    "key": recipe["key"]}
        write(out / f"{key}.json", {"fabric:load_conditions": conditions(), **body, "result": result})


def tags(tags):
    # The hood and both skulls are costumes worn on the head.
    for item in ("reaper_hood", "dirge_skull", "requiem_skull"):
        tags.add("item", COSTUME_TAG, rid(item))
        tags.add("item", COSTUME_HAT_TAG, rid(item))
    for name in vs.TROPHIES:
        tags.add("block", "minecraft:mineable/pickaxe", rid(name))


def advancements(data, write):
    info = vs.ADVANCEMENT
    write(data / MOD / "advancement" / f"{info['key']}.json", {
        "parent": "minecraft:adventure/root",
        "display": {"icon": {"id": info["icon"]}, "title": {"translate": f"advancements.{MOD}.{info['key']}.title"},
                    "description": {"translate": f"advancements.{MOD}.{info['key']}.description"},
                    "frame": info["frame"], "show_toast": True, "announce_to_chat": True},
        "criteria": {"done": {"trigger": "minecraft:impossible"}}})
