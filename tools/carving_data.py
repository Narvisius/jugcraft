"""Writes the JSON resources of pumpkin carving (the Carving Knife and the hand-carved pumpkin) from
tools/agriculture.py.

Called from agriculture_data.py. Formats follow vanilla Minecraft 26.3's own files, read from the game
jar: the pumpkin model, the bee nest's loot table (copy_components and copy_state) and the light block's
item definition (a select on a block state property).
"""
from agriculture import CARVING, FEATURE

MOD = "jugcraft"

# Carving screen and messages. Template names follow CarvingTemplates.java (checked by check_mod_data.py).
SCREEN_TEXT = {
    "screen.jugcraft.carving": "Carve a Pumpkin",
    "screen.jugcraft.carving.cut": "Cut",
    "screen.jugcraft.carving.shave": "Shave",
    "screen.jugcraft.carving.erase": "Erase",
    "screen.jugcraft.carving.brush": "Brush: %s",
    "screen.jugcraft.carving.mirror_on": "Mirror: On",
    "screen.jugcraft.carving.mirror_off": "Mirror: Off",
    "screen.jugcraft.carving.apply": "Apply",
    "screen.jugcraft.carving.candle_on": "Candle: On",
    "screen.jugcraft.carving.candle_off": "Candle: Off",
    "screen.jugcraft.carving.undo": "Undo",
    "screen.jugcraft.carving.reset": "Reset",
    "screen.jugcraft.carving.preview": "Actual size",
    "screen.jugcraft.carving.hint": "Left: carve   Right: erase   Ctrl+Z: undo",
    "screen.jugcraft.carving.templates_only": "This server allows the starter faces only.",
    "screen.jugcraft.carving.does_not_fit": "That face doesn't fit what is already carved.",
    "message.jugcraft.carving.no_session": "Use the Carving Knife on the pumpkin again.",
    "message.jugcraft.carving.no_knife": "You need a Carving Knife in hand to carve.",
    "message.jugcraft.carving.too_far": "Too far away to carve that pumpkin.",
    "message.jugcraft.carving.not_allowed": "You can't carve here.",
    "message.jugcraft.carving.not_a_pumpkin": "That is no longer a pumpkin.",
    "message.jugcraft.carving.invalid": "That carving could not be read.",
    "message.jugcraft.carving.uncarving": "A knife can't put skin back.",
    "message.jugcraft.carving.not_a_template": "This server allows the starter faces only.",
}
TEMPLATE_NAMES = {"classic": "Classic", "cat": "Cat", "ghost": "Ghost", "spooky": "Spooky"}


def rid(path):
    return path if ":" in path else f"{MOD}:{path}"


def assets(root, write, lang):
    block, knife = CARVING["block"], CARVING["knife"]
    # The block is a plain vanilla pumpkin (vanilla's model and textures, so resource packs apply); the
    # carving is drawn over it by the client. Every state looks the same.
    write(root / "blockstates" / f"{block}.json", {"variants": {"": {"model": "minecraft:block/pumpkin"}}})
    for icon in (block, f"{block}_lit", f"{block}_soul"):
        write(root / "models" / "item" / f"{icon}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{icon}")}})
    # Lit with a torch, or with a soul torch (blue).
    lit = {"type": "minecraft:select", "property": "minecraft:block_state", "block_state_property": "soul",
           "cases": [{"when": "true", "model": {"type": "minecraft:model", "model": rid(f"item/{block}_soul")}}],
           "fallback": {"type": "minecraft:model", "model": rid(f"item/{block}_lit")}}
    write(root / "items" / f"{block}.json", {"model": {
        "type": "minecraft:select", "property": "minecraft:block_state", "block_state_property": "lit",
        "cases": [{"when": "true", "model": lit}],
        "fallback": {"type": "minecraft:model", "model": rid(f"item/{block}")}}})
    write(root / "models" / "item" / f"{knife}.json", {"parent": "minecraft:item/handheld", "textures": {"layer0": rid(f"item/{knife}")}})
    write(root / "items" / f"{knife}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{knife}")}})
    lang[f"block.{MOD}.{block}"] = CARVING["display"]
    lang[f"item.{MOD}.{knife}"] = CARVING["knife_display"]
    lang.update(SCREEN_TEXT)
    for template in CARVING["templates"]:
        lang[f"carving.{MOD}.template.{template}"] = TEMPLATE_NAMES[template]


def loot(out, write):
    # Like vanilla's bee nest: the item keeps the design (block entity component) and whether a torch (or a soul
    # torch) is inside.
    block = CARVING["block"]
    write(out / f"{block}.json", {"type": "minecraft:block", "pools": [{
        "condition": {"type": "minecraft:survives_explosion"},
        "entries": [{"type": "minecraft:item", "name": rid(block), "modifier": [
            {"type": "minecraft:copy_components", "include": [rid("carving")], "source": "block_entity"},
            {"type": "minecraft:copy_state", "block": rid(block), "properties": ["lit", "soul"]}]}],
        "rolls": 1}], "random_sequence": rid(f"blocks/{block}")})


def recipes(out, write, conditions):
    knife = CARVING["knife"]
    write(out / f"{knife}.json", {"fabric:load_conditions": conditions(), "type": "minecraft:crafting_shaped", "category": "equipment",
                                  "pattern": CARVING["knife_pattern"], "key": CARVING["knife_key"], "result": {"id": rid(knife), "count": 1}})


def tags(tags):
    tags.add("block", "minecraft:mineable/axe", rid(CARVING["block"]))
