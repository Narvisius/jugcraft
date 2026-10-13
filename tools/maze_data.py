"""JSON resources for the corn maze (fall additions 8), from tools/agriculture.py: maze corn (its three sections reuse
ripe corn's models), the Corn Maze Gate and its finish post (two hay-wrapped posts and a crossbar, the gate with a green
pennant, the post with a chequered flag), blockstates, the gate's item, loot (maze corn gives back its kernel from the
bottom; the finish post gives nothing), the gate's recipe, tags and words.

Called from agriculture_data.py (assets, loot, recipes, tags). Formats follow vanilla Minecraft 26.3's own files.
"""
from agriculture import MAZE
from decor_data import MOD, rid, box, block_model, turned, self_drop
import garden

# Ripe corn's models, in the owner's art (tools/garden.py MAZE_SECTIONS).
SECTIONS = garden.MAZE_SECTIONS
TEXT = {
    "message.jugcraft.corn_maze.go": "Go! Find the way through",
    "message.jugcraft.corn_maze.void.flew": "Run void: no flying in the maze",
    "message.jugcraft.corn_maze.void.climbed": "Run void: you climbed out over the corn",
    "message.jugcraft.corn_maze.void.left": "Run void: you left the maze",
    "message.jugcraft.corn_maze.void.slow": "Run void: you took too long",
    "message.jugcraft.corn_maze.shortcut": "That was a shortcut! The run doesn't count",
    "message.jugcraft.corn_maze.finished": "Through the maze in %s!",
    "message.jugcraft.corn_maze.planted": "A %s corn maze, %s by %s. Best times:",
    "message.jugcraft.corn_maze.plan": "A %s corn maze, %s by %s, ready to plant: use the gate holding corn kernels (sneak to change the size)",
    "message.jugcraft.corn_maze.size.tiny": "tiny",
    "message.jugcraft.corn_maze.size.small": "small",
    "message.jugcraft.corn_maze.size.medium": "medium",
    "message.jugcraft.corn_maze.size.large": "large",
    "message.jugcraft.corn_maze.size_set": "The maze will be %s: %s by %s",
    "message.jugcraft.corn_maze.needs": "Planting this maze takes %s corn kernels",
    "message.jugcraft.corn_maze.blocked": "The far side of the maze isn't clear for the finish post",
    "message.jugcraft.corn_maze.already": "This maze is already planted",
    "message.jugcraft.corn_maze.planting": "Planting a %s corn maze with %s corn kernels",
}


def post_model(flag):
    """Two hay-wrapped posts and a crossbar across the opening (facing north), with a flag hanging from the bar."""
    p, h, f = "#post", "#hay", "#flag"
    elements = [box((0, 0, 6), (2, 16, 10), p), box((14, 0, 6), (16, 16, 10), p),
                box((-0.5, 9, 5.5), (2.5, 12, 10.5), h, uvs={side: [0, 0, 16, 16] for side in ("north", "south", "east", "west", "up", "down")}),
                box((13.5, 9, 5.5), (16.5, 12, 10.5), h, uvs={side: [0, 0, 16, 16] for side in ("north", "south", "east", "west", "up", "down")}),
                box((0, 13, 7), (16, 15, 9), p),
                box((5, 8, 7.75), (11, 13, 8.25), f, faces=["north", "south"], uvs={"north": [0, 0, 16, 16], "south": [0, 0, 16, 16]})]
    return block_model({"post": "corn_maze_post", "hay": "minecraft:block/hay_block_side", "flag": flag}, elements, "corn_maze_post")


def assets(root, write, lang):
    models, states = root / "models" / "block", root / "blockstates"
    write(states / f"{MAZE['corn']}.json", {"variants": {f"section={s}": {"model": rid(f"block/{model}")} for s, model in SECTIONS.items()}})
    lang[f"block.{MOD}.{MAZE['corn']}"] = MAZE["corn_display"]
    for block, flag, display in ((MAZE["gate"], "corn_maze_pennant", MAZE["gate_display"]), (MAZE["finish"], "corn_maze_chequered", MAZE["finish_display"])):
        write(models / f"{block}.json", post_model(flag))
        write(states / f"{block}.json", {"variants": {f"facing={f}": turned(rid(f"block/{block}"), f) for f in ("north", "south", "east", "west")}})
        lang[f"block.{MOD}.{block}"] = display
    write(root / "items" / f"{MAZE['gate']}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{MAZE['gate']}")}})
    lang.update(TEXT)


def loot(out, write):
    write(out / f"{MAZE['gate']}.json", self_drop(MAZE["gate"]))
    # Maze corn gives back the kernel it was planted from, from its bottom section only.
    write(out / f"{MAZE['corn']}.json", self_drop(MAZE["kernel"], {"type": "minecraft:match_block", "blocks": rid(MAZE["corn"]),
                                                                  "state": {"section": "0"}}) | {"random_sequence": rid(f"blocks/{MAZE['corn']}")})
    # The finish post gives nothing: the gate plants it.
    write(out / f"{MAZE['finish']}.json", {"type": "minecraft:block", "pools": [], "random_sequence": rid(f"blocks/{MAZE['finish']}")})


def recipes(out, write, conditions):
    write(out / f"{MAZE['gate']}.json", {
        "fabric:load_conditions": conditions(), "type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": ["SHS", "S S"], "key": {"S": "minecraft:stick", "H": "minecraft:hay_block"},
        "result": {"id": rid(MAZE["gate"]), "count": 1}})


def tags(tags):
    tags.add("block", "minecraft:mineable/axe", rid(MAZE["gate"]))
    tags.add("block", "minecraft:mineable/axe", rid(MAZE["finish"]))
