"""JSON resources for the sky lantern festival (fall additions 6), from tools/agriculture.py: the Sky Lantern (its item
model, tinted by its dye, its recipe, its dyeing recipe in the form of vanilla's leather_helmet_dyed, and the tag that
lets a water cauldron wash the dye out), the
mooncakes' items (their Cooking Pot recipes come from POT_RECIPES), names and the festival's message.

Called from agriculture_data.py (assets, recipes, tags). Formats follow vanilla Minecraft 26.3's own files. A lantern
in flight is drawn by the client's SkyLanternRenderer.
"""
from agriculture import LANTERNS
from decor_data import MOD, rid, flat_item

TEXT = {
    "message.jugcraft.sky_lantern.festival": "The sky fills with lanterns! Make a wish",
}


def assets(root, write, lang):
    lantern = LANTERNS["item"]
    write(root / "models" / "item" / f"{lantern}.json", {"parent": "minecraft:item/generated", "textures": {
        "layer0": rid(f"item/{lantern}"), "layer1": rid(f"item/{lantern}_frame")}})
    write(root / "items" / f"{lantern}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{lantern}"),
                                                          "tints": [{"type": "minecraft:dye",
                                                                     "default": (0xFF000000 | LANTERNS["default_colour"]) - (1 << 32)}]}})
    lang[f"item.{MOD}.{lantern}"] = LANTERNS["display"]
    lang[f"entity.{MOD}.{LANTERNS['entity']}"] = LANTERNS["display"]
    for cake, info in LANTERNS["mooncakes"].items():
        flat_item(root, write, cake)
        lang[f"item.{MOD}.{cake}"] = info["display"]
    lang.update(TEXT)


def recipes(out, write, conditions):
    write(out / f"{LANTERNS['item']}.json", {
        "fabric:load_conditions": conditions(), "type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": ["PPP", "P P", "SCS"],
        "key": {"P": "minecraft:paper", "S": "minecraft:string", "C": "minecraft:candle"},
        "result": {"id": rid(LANTERNS["item"]), "count": LANTERNS["per_craft"]}})
    # Dyeing, as Minecraft 26.3 dyes leather armour: the lantern and any dye give it back in that colour.
    write(out / f"{LANTERNS['item']}_dyed.json", {
        "fabric:load_conditions": conditions(), "type": LANTERNS["dye_recipe"], "group": LANTERNS["dye_group"],
        "target": rid(LANTERNS["item"]), "dye": "#minecraft:dyes", "result": {"id": rid(LANTERNS["item"])}})


def tags(tags):
    tags.add("item", LANTERNS["wash_tag"], rid(LANTERNS["item"]))
    for cake in LANTERNS["mooncakes"]:
        tags.add("item", "c:foods", rid(cake))
