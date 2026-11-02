"""JSON resources for the harvest feast (fall additions 7), from tools/agriculture.py: the Harvest Feast Table's models
(a length on its own, at the start or end of a table, or in its middle: the trestle legs only at a table's ends; each
again with the Halloween Party Table's Bat Table Runner laid over its orange runner, `runner`), blockstates, item,
loot, recipe, tag, and its messages.

Called from agriculture_data.py (assets, loot, recipes, tags). Formats follow vanilla Minecraft 26.3's own files. The
dishes on a table are drawn by the client's FeastTableRenderer.
"""
from agriculture import FEAST
from decor_data import MOD, rid, box, block_model, self_drop

PARTS = ("single", "start", "middle", "end")
# Cloth styles stay synchronized with FeastTableBlock.Runner; linen is applied with white carpet.
RUNNERS = ("harvest", "bats", "linen")
TEXT = {
    "message.jugcraft.feast_table.dish_taken": "That dish holds something else",
    "message.jugcraft.feast_table.empty": "That dish is empty",
    "message.jugcraft.feast_table.tier.0": "A simple meal (%s dishes, %s at the table)",
    "message.jugcraft.feast_table.tier.1": "A good meal! (%s dishes, %s at the table)",
    "message.jugcraft.feast_table.tier.2": "A feast! (%s dishes, %s at the table)",
    "message.jugcraft.feast_table.tier.3": "A harvest feast! (%s dishes, %s at the table)",
    "message.jugcraft.feast_table.tier.4": "A grand feast! (%s dishes, %s at the table)",
}


def table_model(part, runner="harvest"):
    """A length of trestle table running east-west: a planked top with an orange runner, a stretcher beam along it, and
    A-frame legs at whichever ends of the table this length is. With the `bats` runner, the Bat Table Runner lies in the
    orange one's place and hangs down over the table's ends (party_table_data.bat_runner)."""
    w, t, r = "#wood", "#top", "#runner"
    legs_west = part in ("single", "start")
    legs_east = part in ("single", "end")
    elements = [box((0, 12, 0), (16, 13, 16), t, textures={"north": w, "south": w, "east": w, "west": w, "down": w})]
    if runner == "linen":
        # Cloth tiles exactly at x=0/16; drape only over the outer table ends.
        # Fixed UVs repeat the weave without stretching it across the full table.
        elements.append(box((0,13,0),(16,13.22,16),"#linen",faces=["up"]))
        for z0,z1 in ((0,.18),(15.82,16)):
            elements.append(box((0,8,z0),(16,13,z1),"#linen"))
        if legs_west: elements.append(box((0,8,.18),(.18,13,15.82),"#linen"))
        if legs_east: elements.append(box((15.82,8,.18),(16,13,15.82),"#linen"))
    elif runner == "bats":
        import party_table_data
        elements += party_table_data.bat_runner(legs_west, legs_east)
    else:
        elements.append(box((0, 13, 5), (16, 13.25, 11), r, faces=["up", "north", "south"]))
    beam_from = 3 if legs_west else 0
    beam_to = 13 if legs_east else 16
    elements.append(box((beam_from, 3, 7), (beam_to, 4.5, 9), w))
    for x0, present in ((1, legs_west), (13, legs_east)):
        if present:
            elements += [box((x0, 0, 1), (x0 + 2, 1, 15), w), box((x0, 1, 2), (x0 + 2, 12, 4), w), box((x0, 1, 12), (x0 + 2, 12, 14), w),
                         box((x0, 10, 4), (x0 + 2, 12, 12), w)]
    textures = {"wood": "feast_table_wood", "top": "feast_table_top", "runner": "feast_table_runner"}
    if runner == "bats":
        textures = {"wood": "feast_table_wood", "top": "feast_table_top", "bats": "feast_table_bat_runner"}
    model = block_model(textures, elements, "feast_table_wood")
    if runner == "linen": model['textures']['linen'] = 'minecraft:block/white_wool'
    return model


def model_name(block, part, runner):
    """A length's model: `<block>_<part>`, with `_bats` for the Bat Table Runner."""
    return f"{block}_{part}" + ("" if runner == "harvest" else f"_{runner}")


def assets(root, write, lang):
    block = FEAST["block"]
    for part in PARTS:
        for runner in RUNNERS:
            write(root / "models" / "block" / f"{model_name(block, part, runner)}.json", table_model(part, runner))
    variants = {}
    for part in PARTS:
        for runner in RUNNERS:
            name = rid(f"block/{model_name(block, part, runner)}")
            variants[f"axis=x,part={part},runner={runner}"] = {"model": name}
            variants[f"axis=z,part={part},runner={runner}"] = {"model": name, "y": 90}
    write(root / "blockstates" / f"{block}.json", {"variants": variants})
    write(root / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{block}_single")}})
    lang[f"block.{MOD}.{block}"] = FEAST["display"]
    lang.update(TEXT)


def loot(out, write):
    """A length drops itself, and the Bat Table Runner laid on it."""
    table = self_drop(FEAST["block"])
    table["pools"].append({"condition": {"type": "minecraft:all_of", "terms": [
        {"type": "minecraft:survives_explosion"},
        {"type": "minecraft:match_block", "blocks": rid(FEAST["block"]), "state": {"runner": "bats"}}]},
        "entries": [{"type": "minecraft:item", "name": rid("bat_table_runner")}], "rolls": 1})
    import copy
    linen = copy.deepcopy(table['pools'][-1])
    linen['condition']['terms'][1]['state']['runner'] = 'linen'
    linen['entries'][0]['name'] = 'minecraft:white_carpet'
    table['pools'].append(linen)
    write(out / f"{FEAST['block']}.json", table)


def recipes(out, write, conditions):
    write(out / f"{FEAST['block']}.json", {
        "fabric:load_conditions": conditions(), "type": "minecraft:crafting_shaped", "category": "building",
        "pattern": ["SSS", "L L"], "key": {"S": "#minecraft:wooden_slabs", "L": "#minecraft:logs"},
        "result": {"id": rid(FEAST["block"]), "count": FEAST["per_craft"]}})


def tags(tags):
    tags.add("block", "minecraft:mineable/axe", rid(FEAST["block"]))
