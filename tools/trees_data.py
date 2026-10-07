"""Generated data for Jugcraft's own trees (agriculture.TREES, trees.SHAPES): saplings and leaves (models,
blockstates, items, loot and tags) and the tree features biomes and saplings grow. Wood sets are generated with the
other woods (festival_data.wood_assets); wood and leaf textures are drawn by wood_style.py.
"""
from agriculture import TREES, SEASON_STATES, SAPLING_CHANCES, sapling, leaf_looks
from trees import SHAPES, FALLEN, DECORATOR

MOD = "jugcraft"
SHEARS_OR_SILK = {"type": "minecraft:any_of", "terms": ["minecraft:tool/can_shear", "minecraft:tool/can_silk_touch"]}


def rid(path):
    return path if ":" in path else f"{MOD}:{path}"


def variants(looks):
    """A blockstate variant for {texture: weight}: one model, or a weighted list picked by position."""
    if len(looks) == 1:
        return {"model": rid(f"block/{next(iter(looks))}")}
    return [{"model": rid(f"block/{texture}"), "weight": weight} for texture, weight in looks.items()]


def assets(root, write):
    models = root / "models" / "block"
    for tree, info in TREES.items():
        young = sapling(tree)
        write(models / f"{young}.json", {"parent": "minecraft:block/cross", "textures": {"cross": rid(f"block/{young}")}})
        write(root / "blockstates" / f"{young}.json", {"variants": {"": {"model": rid(f"block/{young}")}}})
        write(root / "models" / "item" / f"{young}.json", {"parent": "minecraft:item/generated",
                                                            "textures": {"layer0": rid(f"block/{young}")}})
        write(root / "items" / f"{young}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{young}")}})
        # Leaves are untinted: each look is drawn in its own colours (green, autumn colours, bare twigs).
        looks = leaf_looks(tree)
        for state_looks in looks.values():
            for texture in state_looks:
                write(models / f"{texture}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": rid(f"block/{texture}")}})
        leaves = info["leaves"]
        if info["season"] is None:
            state = {"variants": {"": variants(looks[None])}}
        else:
            state = {"variants": {f"season={season}": variants(looks[season]) for season in SEASON_STATES}}
        write(root / "blockstates" / f"{leaves}.json", state)
        write(root / "items" / f"{leaves}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{leaves}")}})


def loot(out, write):
    not_shears = {"type": "minecraft:inverted", "term": SHEARS_OR_SILK}
    for tree, info in TREES.items():
        leaves, young = info["leaves"], rid(sapling(tree))
        # Vanilla spruce leaves' drops, with this tree's sapling.
        write(out / f"{leaves}.json", {"type": "minecraft:block", "pools": [
            {"entries": [{"type": "minecraft:alternatives", "children": [
                {"type": "minecraft:item", "condition": SHEARS_OR_SILK, "name": rid(leaves)},
                {"type": "minecraft:item", "condition": {"type": "minecraft:all_of", "terms": [
                    {"type": "minecraft:survives_explosion"},
                    {"type": "minecraft:table_bonus", "chances": SAPLING_CHANCES, "enchantment": "minecraft:fortune"}]},
                 "name": young}]}], "rolls": 1},
            {"condition": not_shears, "entries": [{"type": "minecraft:item", "condition": {
                "type": "minecraft:table_bonus", "chances": [0.02, 0.022222223, 0.025, 0.033333335, 0.1], "enchantment": "minecraft:fortune"},
                "modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}},
                             {"type": "minecraft:explosion_decay"}], "name": "minecraft:stick"}], "rolls": 1}],
            "random_sequence": rid(f"blocks/{leaves}")})
        write(out / f"{sapling(tree)}.json", {"type": "minecraft:block", "pools": [{
            "condition": {"type": "minecraft:survives_explosion"}, "entries": [{"type": "minecraft:item", "name": young}], "rolls": 1}],
            "random_sequence": rid(f"blocks/{sapling(tree)}")})


def tags(tags):
    for tree, info in TREES.items():
        for registry in ("block", "item"):
            tags.add(registry, "minecraft:leaves", rid(info["leaves"]))
            tags.add(registry, "minecraft:saplings", rid(sapling(tree)))


def uniform(value):
    if isinstance(value, list):
        return {"type": "minecraft:uniform", "max_inclusive": value[1], "min_inclusive": value[0]}
    return value


def trunk_placer(trunk):
    out = {"type": f"minecraft:{trunk['type']}_trunk_placer", "base_height": trunk["base_height"],
           "height_rand_a": trunk["height_rand_a"], "height_rand_b": trunk.get("height_rand_b", 0)}
    # Any other field of vanilla's placer (the bending trunk's bend length, for example), ranges as uniform.
    out.update({key: uniform(value) for key, value in trunk.items() if key not in ("type", "base_height", "height_rand_a", "height_rand_b")})
    return out


def foliage_placer(foliage):
    if foliage["type"] == "spruce":
        return {"type": "minecraft:spruce_foliage_placer", "offset": uniform(foliage["offset"]),
                "radius": uniform(foliage["radius"]), "trunk_height": uniform(foliage["trunk_height"])}
    out = {"type": f"minecraft:{foliage['type']}_foliage_placer", "offset": foliage["offset"], "radius": foliage["radius"]}
    if "height" in foliage:
        out["height"] = foliage["height"]
    # Any other field of vanilla's placer (the cherry placer's hanging leaves, the mega pine's crown height, for
    # example), ranges as uniform.
    out.update({key: uniform(value) for key, value in foliage.items() if key not in ("type", "offset", "radius", "height")})
    return out


def tree_blocks(wood):
    """A shape's log and leaves: its own (agriculture.TREES) or a vanilla wood's ("minecraft:oak")."""
    if wood.startswith("minecraft:"):
        return f"{wood}_log", f"{wood}_leaves", None
    leaves = TREES[wood]["leaves"] if wood in TREES else None
    return rid(f"{wood}_log"), rid(leaves) if leaves else None, TREES[wood]["season"] if wood in TREES else None


def minimum_size(info):
    """The free space a tree needs, as vanilla sets it for the same placers (spruce, pine, fancy oak, bush, oak)."""
    foliage = (info["foliage"] or {}).get("type")
    if info.get("giant") or info["trunk"]["type"] in ("giant", "mega_jungle"):
        # Two blocks wide: vanilla's giant spruce and mega jungle tree.
        return {"type": "minecraft:two_layers_feature_size", "lower_size": 1, "upper_size": 2}
    if foliage in ("spruce", "pine"):
        # Vanilla's spruce and pine trees alike.
        return {"type": "minecraft:two_layers_feature_size", "limit": 2, "upper_size": 2}
    if info["trunk"]["type"] == "fancy":
        return {"type": "minecraft:two_layers_feature_size", "limit": 0, "min_clipped_height": 4, "upper_size": 0}
    if foliage == "bush":
        return {"type": "minecraft:two_layers_feature_size", "limit": 0, "upper_size": 0}
    return {"type": "minecraft:two_layers_feature_size"}


def worldgen(data, write):
    folder = data / MOD / "worldgen"
    for shape, info in SHAPES.items():
        wood = info["wood"]
        decorators = list(info.get("decorators", []))
        if info["foliage"] is None:
            # No leaves: a single air "leaf" at each branch end, which places nothing.
            foliage = {"type": "minecraft:blob_foliage_placer", "height": 1, "offset": 0, "radius": 0}
            provider = {"id": "minecraft:air"}
        else:
            foliage = foliage_placer(info["foliage"])
            properties = {"distance": "7", "persistent": "false", "waterlogged": "false"}
            log, leaves, season = tree_blocks(wood)
            if season is not None:
                properties["season"] = SEASON_STATES[0]
                decorators.append({"type": rid(DECORATOR)})
            provider = {"id": leaves, "properties": dict(sorted(properties.items()))}
        write(folder / "feature" / f"{shape}.json", {
            "type": "minecraft:tree", "below_trunk_provider": "minecraft:soil_beneath_tree", "decorators": decorators,
            "foliage_placer": foliage, "foliage_provider": provider, "ignore_vines": True,
            "minimum_size": minimum_size(info),
            "trunk_placer": trunk_placer(info["trunk"]),
            "trunk_provider": {"id": tree_blocks(wood)[0], "properties": {"axis": "y"}}})
        survives = info.get("survives_as") or (f"{wood}_sapling" if wood.startswith("minecraft:") else rid(sapling(wood)))
        write(folder / "placed_feature" / f"{shape}_checked.json", {"feature": rid(shape), "placement": [
            {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:would_survive", "state": survives}}]})
    for wood, length in FALLEN.items():
        write(folder / "feature" / f"fallen_{wood}_tree.json", {
            "type": "minecraft:fallen_tree",
            "log_decorators": [{"type": "minecraft:attached_to_logs", "block_provider": {"type": "minecraft:weighted", "entries": [
                {"data": "minecraft:red_mushroom", "weight": 2}, {"data": "minecraft:brown_mushroom", "weight": 1}]},
                "directions": ["up"], "probability": 0.1}],
            "log_length": uniform(length), "stump_decorators": [],
            "trunk_provider": {"id": rid(f"{wood}_log"), "properties": {"axis": "y"}}})
        write(folder / "placed_feature" / f"fallen_{wood}_tree.json", {"feature": rid(f"fallen_{wood}_tree"), "placement": [
            {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:would_survive", "state": "minecraft:oak_sapling"}}]})
