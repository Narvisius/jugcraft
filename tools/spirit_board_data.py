"""JSON resources for the Spirit Board (fall additions 17), from tools/agriculture.py: the board's model (a thin birch board
with a dark edge; its lettered face and the planchette are drawn by SpiritBoardRenderer from entity textures, being
more letters than a block texture holds) and blockstate; the item; names, messages and what each wish is called; loot;
and tags (the candles a séance needs; the items that grant each wish). The recipe is in SHAPED; the advancements in
HALLOWEEN_ADVANCEMENTS.

Called from agriculture_data.py (assets, loot, tags). Formats follow vanilla Minecraft 26.3's own files.
"""
from agriculture import SPIRIT_BOARD
from decor_data import MOD, rid, box, block_model, self_drop, turned, flat_item

TEXT = {
    "message.jugcraft.spirit_board.too_far": "Sit closer to the board to touch the planchette",
    "message.jugcraft.spirit_board.already": "Your fingers are already on the planchette",
    "message.jugcraft.spirit_board.crowded": "There is no room for more fingers on the planchette",
    "message.jugcraft.spirit_board.joined": "You rest your fingers on the planchette beside the others",
    "message.jugcraft.spirit_board.resting": "The planchette is still. Wait a moment",
    "message.jugcraft.spirit_board.dark": "The planchette won't move without candlelight",
    "message.jugcraft.spirit_board.broken": "Every finger has left the planchette, and the spirit slips away",
    "message.jugcraft.spirit_board.spells": "The planchette spells: %s",
    "message.jugcraft.spirit_board.wish": "%s wishes for %s",
    "message.jugcraft.restless_spirit.not_that": "The spirit turns away: that isn't what it wishes for",
    "message.jugcraft.restless_spirit.at_rest": "%s is at rest",
}


def board():
    """The board lying flat, its long sides to the north and south (the blockstate turns it): a birch top and a dark edge."""
    return block_model({"top": "spirit_board_top", "edge": "spirit_board_edge"},
                       [box((0.5, 0, 2.5), (15.5, 1, 13.5), "#edge", textures={"up": "#top"})], "spirit_board_top")


def assets(root, write, lang):
    name = SPIRIT_BOARD["block"]
    write(root / "models" / "block" / f"{name}.json", board())
    write(root / "blockstates" / f"{name}.json", {"variants": {
        f"facing={f}": turned(rid(f"block/{name}"), f) for f in ("north", "south", "east", "west")}})
    flat_item(root, write, name)
    lang[f"block.{MOD}.{name}"] = SPIRIT_BOARD["display"]
    for wish, info in SPIRIT_BOARD["wishes"].items():
        lang[f"spirit_wish.{MOD}.{wish}"] = info["display"]
    lang.update(TEXT)


def loot(out, write):
    write(out / f"{SPIRIT_BOARD['block']}.json", self_drop(SPIRIT_BOARD["block"]))


def tags(tags):
    tags.add("block", "minecraft:mineable/axe", rid(SPIRIT_BOARD["block"]))
    for candle in SPIRIT_BOARD["candles"]:
        tags.add("block", SPIRIT_BOARD["candles_tag"], candle)
    for wish, info in SPIRIT_BOARD["wishes"].items():
        for item in info["items"]:
            tags.add("item", f"jugcraft:spirit_wishes/{wish}", item)
