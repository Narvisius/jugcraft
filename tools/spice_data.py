"""JSON resources for the spices (tools/spices.py, garden crops, herbs and spices part b): the Cinnamon Tree (its sapling,
leaves and two logs, their loot and tags, the tree its sapling grows and its wild patch) and the Spice Rack (its model,
blockstate, item, loot and names). The spice crops, wild plants, spices and dishes are written from tools/agriculture.py's
tables by tools/agriculture_data.py.

Called from agriculture_data.py (assets, loot, tags, worldgen). Formats follow vanilla Minecraft 26.3's own files.
"""
from agriculture import SAPLING_CHANCES
from decor_data import MOD, HORIZONTAL, rid, turned, self_drop
from spices import CINNAMON, RACK

SHEARS_OR_SILK = {"type": "minecraft:any_of", "terms": ["minecraft:tool/can_shear", "minecraft:tool/can_silk_touch"]}
TREE = f"{CINNAMON['tree']}_tree"


def rack():
    """The rack facing north: a plank back in the south of the block, two shelves before it with a rail along each, and
    its sides and top, so it hangs against a wall (agriculture/SpiceRackBlock's shape; client/SpiceRackRenderer stands the
    spices on its shelves)."""
    boxes = [([0, 0, 14], [16, 16, 16]),  # back
             ([0, 0, 9], [16, 1, 14]), ([0, 8, 9], [16, 9, 14]),  # shelves
             ([0, 15, 9], [16, 16, 14]),  # top
             ([0, 1, 9], [1, 15, 14]), ([15, 1, 9], [16, 15, 14]),  # sides
             ([1, 3, 9], [15, 4, 10]), ([1, 11, 9], [15, 12, 10])]  # rails
    return {"parent": "minecraft:block/block",
            "textures": {"particle": rid(f"block/{RACK['block']}"), "wood": rid(f"block/{RACK['block']}")},
            "elements": [{"from": lo, "to": hi, "faces": {side: {"texture": "#wood"} for side in
                                                          ("north", "south", "east", "west", "up", "down")}}
                         for lo, hi in boxes]}


def assets(root, write, lang):
    models, states = root / "models" / "block", root / "blockstates"
    sapling, leaves = CINNAMON["sapling"], CINNAMON["leaves"]
    write(models / f"{sapling}.json", {"parent": "minecraft:block/cross", "textures": {"cross": rid(f"block/{sapling}")}})
    write(states / f"{sapling}.json", {"variants": {"": {"model": rid(f"block/{sapling}")}}})
    write(root / "models" / "item" / f"{sapling}.json", {"parent": "minecraft:item/generated",
                                                         "textures": {"layer0": rid(f"block/{sapling}")}})
    write(root / "items" / f"{sapling}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{sapling}")}})
    write(models / f"{leaves}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": rid(f"block/{leaves}")}})
    write(states / f"{leaves}.json", {"variants": {"": {"model": rid(f"block/{leaves}")}}})
    write(root / "items" / f"{leaves}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{leaves}")}})
    # The two logs, as a wood set's: a column, lying along x or z.
    for name in (CINNAMON["log"], CINNAMON["stripped"]):
        textures = {"end": rid(f"block/{name}_top"), "side": rid(f"block/{name}")}
        write(models / f"{name}.json", {"parent": "minecraft:block/cube_column", "textures": textures})
        write(models / f"{name}_horizontal.json", {"parent": "minecraft:block/cube_column_horizontal", "textures": textures})
        horizontal = rid(f"block/{name}_horizontal")
        write(states / f"{name}.json", {"variants": {"axis=x": {"model": horizontal, "x": 90, "y": 90},
                                                     "axis=y": {"model": rid(f"block/{name}")},
                                                     "axis=z": {"model": horizontal, "x": 90}}})
        write(root / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{name}")}})
    for name, display in CINNAMON["displays"].items():
        lang[f"block.{MOD}.{name}"] = display

    name = RACK["block"]
    write(models / f"{name}.json", rack())
    write(states / f"{name}.json", {"variants": {f"facing={f}": turned(rid(f"block/{name}"), f) for f in HORIZONTAL}})
    write(root / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{name}")}})
    lang[f"block.{MOD}.{name}"] = RACK["display"]
    lang[f"message.{MOD}.{name}.full"] = "The spice rack is full"
    lang[f"message.{MOD}.{name}.empty"] = "The spice rack is empty"


def loot(out, write):
    """The sapling, logs and rack give themselves; the leaves give themselves to shears or Silk Touch, else, as vanilla
    spruce leaves, now and then a sapling (SAPLING_CHANCES, with Fortune) or a stick or two."""
    for name in (CINNAMON["sapling"], CINNAMON["log"], CINNAMON["stripped"], RACK["block"]):
        write(out / f"{name}.json", self_drop(name))
    leaves = CINNAMON["leaves"]
    write(out / f"{leaves}.json", {"type": "minecraft:block", "pools": [
        {"entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "condition": SHEARS_OR_SILK, "name": rid(leaves)},
            {"type": "minecraft:item", "condition": {"type": "minecraft:all_of", "terms": [
                {"type": "minecraft:survives_explosion"},
                {"type": "minecraft:table_bonus", "chances": SAPLING_CHANCES, "enchantment": "minecraft:fortune"}]},
             "name": rid(CINNAMON["sapling"])}]}], "rolls": 1},
        {"condition": {"type": "minecraft:inverted", "term": SHEARS_OR_SILK}, "entries": [{"type": "minecraft:item", "condition": {
            "type": "minecraft:table_bonus", "chances": [0.02, 0.022222223, 0.025, 0.033333335, 0.1], "enchantment": "minecraft:fortune"},
            "modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}},
                         {"type": "minecraft:explosion_decay"}], "name": "minecraft:stick"}], "rolls": 1}],
        "random_sequence": rid(f"blocks/{leaves}")})


def tags(tags):
    # Logs that burn (charcoal, fuel) and hold leaves up; leaves and sapling in vanilla's tags, as every tree's.
    for registry in ("block", "item"):
        for name in (CINNAMON["log"], CINNAMON["stripped"]):
            tags.add(registry, "minecraft:logs_that_burn", rid(name))
        tags.add(registry, "minecraft:leaves", rid(CINNAMON["leaves"]))
        tags.add(registry, "minecraft:saplings", rid(CINNAMON["sapling"]))
    tags.add("block", "minecraft:overworld_natural_logs", rid(CINNAMON["log"]))
    tags.add("block", "minecraft:mineable/axe", rid(RACK["block"]))


def worldgen(data, write):
    """The tree its sapling grows (a straight trunk of cinnamon logs under a blob of its leaves) and its wild patch: one tree in
    `rarity` chunks of the biomes it grows in, where its sapling could stand."""
    folder = data / MOD / "worldgen"
    write(folder / "feature" / f"{TREE}.json", {
        "type": "minecraft:tree", "below_trunk_provider": "minecraft:soil_beneath_tree", "decorators": [],
        "foliage_placer": {"type": "minecraft:blob_foliage_placer", "height": CINNAMON["height"], "offset": 0,
                           "radius": CINNAMON["radius"]},
        "foliage_provider": {"id": rid(CINNAMON["leaves"]),
                             "properties": {"distance": "7", "persistent": "false", "waterlogged": "false"}},
        "ignore_vines": True, "minimum_size": {"type": "minecraft:two_layers_feature_size"},
        "trunk_placer": {"type": "minecraft:straight_trunk_placer", "base_height": CINNAMON["trunk"],
                         "height_rand_a": CINNAMON["trunk_extra"], "height_rand_b": 0},
        "trunk_provider": {"id": rid(CINNAMON["log"]), "properties": {"axis": "y"}}})
    write(folder / "placed_feature" / f"patch_{TREE}.json", {"feature": rid(TREE), "placement": [
        {"type": "minecraft:rarity_filter", "chance": CINNAMON["rarity"]},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0},
        {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR"},
        {"type": "minecraft:biome"},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:would_survive",
                                                                   "state": rid(CINNAMON["sapling"])}},
    ]})
