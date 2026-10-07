"""JSON resources for the harvest feast (fall additions 7), from tools/agriculture.py: the Harvest Feast Table's models
(a length on its own, at the start or end of a table, or in its middle: the trestle legs only at a table's ends),
blockstates, item, loot, recipe, tag, and its messages.

Called from agriculture_data.py (assets, loot, recipes, tags). Formats follow vanilla Minecraft 26.3's own files. The
dishes on a table are drawn by the client's FeastTableRenderer.
"""
from agriculture import FEAST
from decor_data import MOD, rid, box, block_model, self_drop

PARTS = ("single", "start", "middle", "end")
TEXT = {
    "message.jugcraft.feast_table.dish_taken": "That dish holds something else",
    "message.jugcraft.feast_table.empty": "That dish is empty",
    "message.jugcraft.feast_table.tier.0": "A simple meal (%s dishes, %s at the table)",
    "message.jugcraft.feast_table.tier.1": "A good meal! (%s dishes, %s at the table)",
    "message.jugcraft.feast_table.tier.2": "A feast! (%s dishes, %s at the table)",
    "message.jugcraft.feast_table.tier.3": "A harvest feast! (%s dishes, %s at the table)",
    "message.jugcraft.feast_table.tier.4": "A grand feast! (%s dishes, %s at the table)",
}


def table_model(part):
    """A length of trestle table running east-west: a planked top with an orange runner, a stretcher beam along it, and
    A-frame legs at whichever ends of the table this length is."""
    w, t, r = "#wood", "#top", "#runner"
    legs_west = part in ("single", "start")
    legs_east = part in ("single", "end")
    elements = [box((0, 12, 0), (16, 13, 16), t, textures={"north": w, "south": w, "east": w, "west": w, "down": w}),
                box((0, 13, 5), (16, 13.25, 11), r, faces=["up", "north", "south"])]
    beam_from = 3 if legs_west else 0
    beam_to = 13 if legs_east else 16
    elements.append(box((beam_from, 3, 7), (beam_to, 4.5, 9), w))
    for x0, present in ((1, legs_west), (13, legs_east)):
        if present:
            elements += [box((x0, 0, 1), (x0 + 2, 1, 15), w), box((x0, 1, 2), (x0 + 2, 12, 4), w), box((x0, 1, 12), (x0 + 2, 12, 14), w),
                         box((x0, 10, 4), (x0 + 2, 12, 12), w)]
    return block_model({"wood": "feast_table_wood", "top": "feast_table_top", "runner": "feast_table_runner"}, elements, "feast_table_wood")


def assets(root, write, lang):
    block = FEAST["block"]
    for part in PARTS:
        write(root / "models" / "block" / f"{block}_{part}.json", table_model(part))
    variants = {}
    for part in PARTS:
        variants[f"axis=x,part={part}"] = {"model": rid(f"block/{block}_{part}")}
        variants[f"axis=z,part={part}"] = {"model": rid(f"block/{block}_{part}"), "y": 90}
    write(root / "blockstates" / f"{block}.json", {"variants": variants})
    write(root / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{block}_single")}})
    lang[f"block.{MOD}.{block}"] = FEAST["display"]
    lang.update(TEXT)


def loot(out, write):
    write(out / f"{FEAST['block']}.json", self_drop(FEAST["block"]))


def recipes(out, write, conditions):
    write(out / f"{FEAST['block']}.json", {
        "fabric:load_conditions": conditions(), "type": "minecraft:crafting_shaped", "category": "building",
        "pattern": ["SSS", "L L"], "key": {"S": "#minecraft:wooden_slabs", "L": "#minecraft:logs"},
        "result": {"id": rid(FEAST["block"]), "count": FEAST["per_craft"]}})


def tags(tags):
    tags.add("block", "minecraft:mineable/axe", rid(FEAST["block"]))
