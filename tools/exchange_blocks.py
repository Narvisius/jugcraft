"""The Exchange's blocks (finance/ExchangeBlocks): the Market Chest's blockstate, model, item model, drops, tool tag and
names. Its textures are drawn by tools/exchange_textures.py. There is no crafting recipe: a company's controller buys
the chest from the Exchange for Jugs (an admin setting), so it is a money sink like the land office."""
import pathlib

MOD = "jugcraft"
CHEST = "market_chest"
# Block ids for tools/check_mod_data.py, which checks their assets, drops and tags.
BLOCKS = [CHEST]


def write_assets(write, rid, assets, data, lang):
    assets = pathlib.Path(assets)
    data = pathlib.Path(data)
    textures = {"front": rid(f"block/{CHEST}_front"), "side": rid(f"block/{CHEST}_side"), "top": rid(f"block/{CHEST}_top"),
                "bottom": "minecraft:block/dark_oak_planks", "particle": rid(f"block/{CHEST}_side")}
    body = {"from": [1, 0, 1], "to": [15, 13, 15], "faces": {
        "north": {"uv": [1, 3, 15, 16], "texture": "#front"},
        "south": {"uv": [1, 3, 15, 16], "texture": "#side"},
        "east": {"uv": [1, 3, 15, 16], "texture": "#side"},
        "west": {"uv": [1, 3, 15, 16], "texture": "#side"},
        "down": {"uv": [1, 1, 15, 15], "texture": "#bottom", "cullface": "down"}}}
    desk = {"from": [0, 13, 0], "to": [16, 16, 16], "faces": {
        "north": {"uv": [0, 0, 16, 3], "texture": "#front"},
        "south": {"uv": [0, 0, 16, 3], "texture": "#front"},
        "east": {"uv": [0, 0, 16, 3], "texture": "#front"},
        "west": {"uv": [0, 0, 16, 3], "texture": "#front"},
        "up": {"uv": [0, 0, 16, 16], "texture": "#top", "cullface": "up"},
        "down": {"uv": [0, 0, 16, 16], "texture": "#bottom"}}}
    write(assets / "models" / "block" / f"{CHEST}.json",
          {"parent": "minecraft:block/block", "textures": textures, "elements": [body, desk]})
    variants = {}
    for facing, turn in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        variant = {"model": rid(f"block/{CHEST}")}
        if turn:
            variant["y"] = turn
        variants[f"facing={facing}"] = variant
    write(assets / "blockstates" / f"{CHEST}.json", {"variants": variants})
    write(assets / "items" / f"{CHEST}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{CHEST}")}})
    write(data / MOD / "loot_table" / "blocks" / f"{CHEST}.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": rid(CHEST)}],
                   "condition": {"type": "minecraft:survives_explosion"}}],
        "random_sequence": rid(f"blocks/{CHEST}")})
    lang[f"block.{MOD}.{CHEST}"] = "Market Chest"
    lang[f"container.{MOD}.{CHEST}"] = "Market Chest"
