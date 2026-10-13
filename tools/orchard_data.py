"""JSON resources for the orchards (tools/orchard.py): each fruit tree's sapling and its leaves by fruit stage (models,
blockstates, item models, names), their loot (the leaves as the apple's, the sapling its seed), their tags, and the trees
themselves (worldgen: each tree's feature, a wild patch for the vanilla biomes it grows in, and the checked placement
Jugcraft's biomes name). The fruit, seeds and juices' items, names and tags come from tools/agriculture.py ITEMS, the
juices' set-down models from tools/menu_data.py, the pies' from tools/pie_data.py and the preserves' from the pantry's.

Called from agriculture_data.py (assets, loot, tags, worldgen). Formats follow vanilla Minecraft 26.3's own files.
"""
import orchard
from decor_data import MOD, rid, self_drop
from festival_data import SHEARS_OR_SILK
from trees_data import foliage_placer


def assets(root, write, lang):
    models, states, items = root / "models" / "block", root / "blockstates", root / "items"
    for tree, info in orchard.TREES.items():
        sapling, leaves = orchard.sapling(tree), orchard.leaves(tree)
        write(models / f"{sapling}.json", {"parent": "minecraft:block/cross", "textures": {"cross": rid(f"block/{sapling}")}})
        write(states / f"{sapling}.json", {"variants": {"": {"model": rid(f"block/{sapling}")}}})
        # Leaves untinted, so the blossom and fruit keep their colours, one model for each fruit stage.
        for stage in orchard.LEAF_STAGES:
            write(models / f"{leaves}{stage}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": rid(f"block/{leaves}{stage}")}})
        write(states / f"{leaves}.json", {"variants": {
            f"fruit={fruit}": {"model": rid(f"block/{leaves}{stage}")} for fruit, stage in enumerate(orchard.LEAF_STAGES)}})
        write(items / f"{leaves}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{leaves}")}})
        lang[f"block.{MOD}.{sapling}"] = f"{info['display']} Sapling"
        lang[f"block.{MOD}.{leaves}"] = f"{info['display']} Leaves"
    # The banana's stem: a pillar, as a log is, its rings on its ends.
    stem = orchard.BANANA_STEM["block"]
    write(models / f"{stem}.json", {"parent": "minecraft:block/cube_column",
                                    "textures": {"end": rid(f"block/{stem}_top"), "side": rid(f"block/{stem}")}})
    write(models / f"{stem}_horizontal.json", {"parent": "minecraft:block/cube_column_horizontal",
                                               "textures": {"end": rid(f"block/{stem}_top"), "side": rid(f"block/{stem}")}})
    write(states / f"{stem}.json", {"variants": {
        "axis=x": {"model": rid(f"block/{stem}_horizontal"), "x": 90, "y": 90},
        "axis=y": {"model": rid(f"block/{stem}")},
        "axis=z": {"model": rid(f"block/{stem}_horizontal"), "x": 90}}})
    write(items / f"{stem}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{stem}")}})
    lang[f"block.{MOD}.{stem}"] = orchard.BANANA_STEM["display"]


def loot(out, write):
    """Leaves drop as the apple's do (shears or silk touch take them; otherwise now and then a seed, sticks, and the ripe
    fruit); a sapling gives its seed back."""
    not_shears = {"type": "minecraft:inverted", "term": SHEARS_OR_SILK}
    for tree, info in orchard.TREES.items():
        leaves, seed = orchard.leaves(tree), rid(info["seed"])
        low, high = info["pick"]
        write(out / f"{leaves}.json", {"type": "minecraft:block", "pools": [
            {"entries": [{"type": "minecraft:alternatives", "children": [
                {"type": "minecraft:item", "condition": SHEARS_OR_SILK, "name": rid(leaves)},
                {"type": "minecraft:item", "condition": {"type": "minecraft:all_of", "terms": [
                    {"type": "minecraft:survives_explosion"},
                    {"type": "minecraft:table_bonus", "chances": [0.05, 0.0625, 0.083333336, 0.1], "enchantment": "minecraft:fortune"}]},
                 "name": seed}]}], "rolls": 1},
            {"condition": not_shears, "entries": [{"type": "minecraft:item", "condition": {
                "type": "minecraft:table_bonus", "chances": [0.02, 0.022222223, 0.025, 0.033333335, 0.1], "enchantment": "minecraft:fortune"},
                "modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}},
                             {"type": "minecraft:explosion_decay"}], "name": "minecraft:stick"}], "rolls": 1},
            {"condition": {"type": "minecraft:match_block", "blocks": rid(leaves), "state": {"fruit": "2"}},
             "entries": [{"type": "minecraft:item", "name": rid(tree), "modifier": [
                 {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": low, "max": high}},
                 {"type": "minecraft:explosion_decay"}]}], "rolls": 1}],
            "random_sequence": rid(f"blocks/{leaves}")})
        sapling = self_drop(orchard.sapling(tree))
        sapling["pools"][0]["entries"][0]["name"] = seed
        write(out / f"{orchard.sapling(tree)}.json", sapling)
    write(out / f"{orchard.BANANA_STEM['block']}.json", self_drop(orchard.BANANA_STEM["block"]))


def tags(tags):
    for tree in orchard.TREES:
        for registry in ("block", "item"):
            tags.add(registry, "minecraft:leaves", rid(orchard.leaves(tree)))
        tags.add("block", "minecraft:saplings", rid(orchard.sapling(tree)))
    # The banana's stem is a log to its fronds (they stay while it stands) and to an axe, but no fuel.
    stem = rid(orchard.BANANA_STEM["block"])
    for registry in ("block", "item"):
        tags.add(registry, "minecraft:logs", stem)
    tags.add("block", "minecraft:mineable/axe", stem)


def placer(foliage):
    """A tree's foliage placer (a blob, unless it names another type), its fields in the order the files have always had."""
    out = foliage_placer({"type": "blob", "offset": 0, **foliage})
    return {"type": out.pop("type"), **dict(sorted(out.items()))}


def worldgen(data, write):
    """Each tree: a straight trunk (oak, or its own) with a crown of its leaves (a blob, or the foliage type it names); a
    wild patch for the vanilla biomes it grows in (one tree in `rarity` chunks, where its sapling could stand); the
    placement Jugcraft's biomes pick it by (tools/biomes.py PLACED_TREES writes `<tree>_checked`)."""
    folder = data / MOD / "worldgen"
    for tree, info in orchard.TREES.items():
        trunk, foliage = info["trunk"], info["foliage"]
        write(folder / "feature" / f"{orchard.feature(tree)}.json", {
            "type": "minecraft:tree", "below_trunk_provider": "minecraft:soil_beneath_tree", "decorators": [],
            "foliage_placer": placer(foliage),
            "foliage_provider": {"id": rid(orchard.leaves(tree)),
                                 "properties": {"distance": "7", "fruit": "0", "persistent": "false", "waterlogged": "false"}},
            "ignore_vines": True, "minimum_size": {"type": "minecraft:two_layers_feature_size"},
            "trunk_placer": {"type": "minecraft:straight_trunk_placer", "base_height": trunk["base_height"],
                             "height_rand_a": trunk["height_rand_a"], "height_rand_b": 0},
            "trunk_provider": {"id": orchard.log(tree), "properties": {"axis": "y"}}})
        write(folder / "placed_feature" / f"patch_{orchard.feature(tree)}.json", {"feature": rid(orchard.feature(tree)), "placement": [
            {"type": "minecraft:rarity_filter", "chance": info["rarity"]},
            {"type": "minecraft:in_square"},
            {"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0},
            {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR"},
            {"type": "minecraft:biome"},
            {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:would_survive",
                                                                       "state": rid(orchard.sapling(tree))}},
        ]})
