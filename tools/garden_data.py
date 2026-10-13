"""JSON resources for the garden crops in the owner's art (tools/garden.py, slice 7a): the brown and red mushroom colonies
(their cross models through the owner's four stages, blockstates, loot and names) and the Rotten Tomato's entity name. The
cabbage, onion, tomato and corn wear their stages through tools/agriculture.py's tables (tools/agriculture_data.py writes
them, the tomato's over-ripe vine with them); the wild plants and the Rotten Tomato's item are written with the others there.

Called from agriculture_data.py (assets, loot). Formats follow vanilla Minecraft 26.3's own files.
"""
from decor_data import MOD, rid
from garden import COLONY, COLONIES, TEXT, colony_texture


def match(block, **state):
    return {"type": "minecraft:match_block", "blocks": rid(block), "state": {k: str(v) for k, v in state.items()}}


def assets(root, write, lang):
    for colony, info in COLONIES.items():
        for stage in range(COLONY["stages"]):
            texture = colony_texture(colony, stage)
            write(root / "models" / "block" / f"{texture}.json", {"parent": "minecraft:block/cross",
                                                                 "textures": {"cross": rid(f"block/{texture}")}})
        write(root / "blockstates" / f"{colony}.json", {"variants": {
            f"age={stage}": {"model": rid(f"block/{colony_texture(colony, stage)}")} for stage in range(COLONY["stages"])}})
        lang[f"block.{MOD}.{colony}"] = info["display"]
    lang.update(TEXT)


def loot(out, write):
    """A colony gives back its mushroom, and grown, as many more as picking gives (2-3); an explosion may scatter them."""
    pick, grown = COLONY["pick"], COLONY["stages"] - 1
    for colony, info in COLONIES.items():
        mushroom = info["mushroom"]
        write(out / f"{colony}.json", {"type": "minecraft:block", "random_sequence": rid(f"blocks/{colony}"), "pools": [
            {"rolls": 1, "entries": [{"type": "minecraft:item", "name": mushroom}], "condition": {"type": "minecraft:survives_explosion"}},
            {"rolls": 1, "condition": match(colony, age=grown), "entries": [{"type": "minecraft:item", "name": mushroom, "modifier": [
                {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": pick["min"], "max": pick["max"]}, "add": False},
                {"type": "minecraft:explosion_decay"}]}]}]})
