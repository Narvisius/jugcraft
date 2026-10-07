"""Generated data for the biomes branch's wild plants (tools/plants.py): models, blockstates, items, names, loot, tags,
the registration list Java reads (/jugcraft/plants.json) and a placement feature per plant (worldgen/feature/<plant>)
that biomes scatter (tools/biomes.py EXTRAS).
"""
import flora_data
import plants as pl

MOD = "jugcraft"
FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}


def rid(path):
    return path if ":" in path else f"{MOD}:{path}"


def tex(name):
    return rid(f"block/{name}")


def assets(root, write, lang):
    models = root / "models" / "block"
    for plant, info in pl.PLANTS.items():
        kind = info["kind"]
        lang[f"block.{MOD}.{plant}"] = info["display"]
        if info.get("art") == "flora":
            flora_data.plant_assets(root, write, plant)
            if kind == "flower":
                lang[f"block.{MOD}.{pl.potted(plant)}"] = f"Potted {info['display']}"
            continue
        if kind == "flower":
            # Vanilla's flower and potted-flower shapes, with our textures.
            write(models / f"{plant}.json", {"parent": "minecraft:block/cross", "textures": {"cross": tex(plant)}})
            write(models / f"{pl.potted(plant)}.json", {"parent": "minecraft:block/flower_pot_cross", "textures": {"plant": tex(plant)}})
            write(root / "blockstates" / f"{plant}.json", {"variants": {"": {"model": rid(f"block/{plant}")}}})
            write(root / "blockstates" / f"{pl.potted(plant)}.json", {"variants": {"": {"model": rid(f"block/{pl.potted(plant)}")}}})
            icon = tex(plant)
            lang[f"block.{MOD}.{pl.potted(plant)}"] = f"Potted {info['display']}"
        elif kind in pl.TALL:
            for half in ("bottom", "top"):
                write(models / f"{plant}_{half}.json", {"parent": "minecraft:block/cross", "textures": {"cross": tex(f"{plant}_{half}")}})
            write(root / "blockstates" / f"{plant}.json", {"variants": {
                "half=lower": {"model": rid(f"block/{plant}_bottom")}, "half=upper": {"model": rid(f"block/{plant}_top")}}})
            icon = tex(f"{plant}_top")
        elif kind in ("water_plant", "floor_plant"):
            write(models / f"{plant}.json", {"parent": "minecraft:block/cross", "textures": {"cross": tex(plant)}})
            write(root / "blockstates" / f"{plant}.json", {"variants": {"": {"model": rid(f"block/{plant}")}}})
            icon = tex(plant)
        elif kind == "surface":
            # Vanilla's lily pad shape (its tint is unused: our texture carries its colours).
            write(models / f"{plant}.json", {"parent": "minecraft:block/lily_pad", "textures": {"particle": tex(plant), "texture": tex(plant)}})
            write(root / "blockstates" / f"{plant}.json", {"variants": {"": {"model": rid(f"block/{plant}")}}})
            icon = tex(plant)
        else:
            # Vanilla's flowerbed shapes (one to four clumps), turned by facing, like pink petals.
            for n in range(1, 5):
                write(models / f"{plant}_{n}.json", {"parent": f"minecraft:block/flowerbed_{n}",
                                                      "textures": {"flowerbed": tex(plant), "stem": tex(f"{plant}_stem")}})
            parts = []
            for n in range(1, 5):
                amounts = "|".join(str(a) for a in range(n, 5))
                for facing, y in FACINGS.items():
                    when = {"facing": facing} if n == 1 else {"facing": facing, "flower_amount": amounts}
                    apply = {"model": rid(f"block/{plant}_{n}")}
                    if y:
                        apply["y"] = y
                    parts.append({"apply": apply, "when": when})
            write(root / "blockstates" / f"{plant}.json", {"multipart": parts})
            icon = tex(plant)
        write(root / "models" / "item" / f"{plant}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": icon}})
        write(root / "items" / f"{plant}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{plant}")}})


def match_block(block, **state):
    return {"type": "minecraft:match_block", "blocks": rid(block), "state": {k: str(v) for k, v in state.items()}}


def loot(out, write):
    explosion = {"type": "minecraft:survives_explosion"}
    for plant, info in pl.PLANTS.items():
        kind = info["kind"]
        if kind == "flower":
            write(out / f"{plant}.json", {"type": "minecraft:block", "pools": [
                {"condition": explosion, "entries": [{"type": "minecraft:item", "name": rid(plant)}], "rolls": 1}],
                "random_sequence": rid(f"blocks/{plant}")})
            # Vanilla's potted flowers: the pot and the flower.
            write(out / f"{pl.potted(plant)}.json", {"type": "minecraft:block", "pools": [
                {"condition": explosion, "entries": [{"type": "minecraft:item", "name": "minecraft:flower_pot"}], "rolls": 1},
                {"condition": explosion, "entries": [{"type": "minecraft:item", "name": rid(plant)}], "rolls": 1}],
                "random_sequence": rid(f"blocks/{pl.potted(plant)}")})
        elif kind == "tall_grass":
            # Only shears take it, from the lower half, like tall grass.
            write(out / f"{plant}.json", {"type": "minecraft:block", "pools": [
                {"condition": {"type": "minecraft:all_of", "terms": [match_block(plant, half="lower"), "minecraft:tool/can_shear"]},
                 "entries": [{"type": "minecraft:item", "name": rid(plant)}], "rolls": 1}], "random_sequence": rid(f"blocks/{plant}")})
        elif kind in ("grass", "hanging"):
            # Only shears take it, like short grass and hanging moss.
            write(out / f"{plant}.json", {"type": "minecraft:block", "pools": [
                {"condition": "minecraft:tool/can_shear", "entries": [{"type": "minecraft:item", "name": rid(plant)}], "rolls": 1}],
                "random_sequence": rid(f"blocks/{plant}")})
        elif kind == "vine":
            # Shears take one for each face it covers, like glow lichen.
            write(out / f"{plant}.json", {"type": "minecraft:block", "pools": [
                {"condition": "minecraft:tool/can_shear", "entries": [{"type": "minecraft:item", "name": rid(plant), "modifier": [
                    *[{"type": "minecraft:set_count", "add": True, "count": 1, "condition": match_block(plant, **{face: "true"})}
                      for face in ("down", "up", "north", "south", "west", "east")],
                    {"type": "minecraft:set_count", "add": True, "count": -1}, {"type": "minecraft:explosion_decay"}]}], "rolls": 1}],
                "random_sequence": rid(f"blocks/{plant}")})
        elif kind in pl.TALL:
            # From the lower half only, like the lilac.
            write(out / f"{plant}.json", {"type": "minecraft:block", "pools": [
                {"condition": explosion, "entries": [{"type": "minecraft:item", "condition": match_block(plant, half="lower"),
                                                      "name": rid(plant)}], "rolls": 1}], "random_sequence": rid(f"blocks/{plant}")})
        elif kind == "water_plant":
            # Only shears take it, like seagrass.
            write(out / f"{plant}.json", {"type": "minecraft:block", "pools": [
                {"condition": "minecraft:tool/can_shear", "entries": [{"type": "minecraft:item", "name": rid(plant)}], "rolls": 1}],
                "random_sequence": rid(f"blocks/{plant}")})
        elif kind in ("surface", "floor_plant"):
            write(out / f"{plant}.json", {"type": "minecraft:block", "pools": [
                {"condition": explosion, "entries": [{"type": "minecraft:item", "name": rid(plant)}], "rolls": 1}],
                "random_sequence": rid(f"blocks/{plant}")})
        else:
            # One per clump, like pink petals.
            write(out / f"{plant}.json", {"type": "minecraft:block", "pools": [{"entries": [{"type": "minecraft:item", "modifier": [
                *[{"type": "minecraft:set_count", "condition": match_block(plant, flower_amount=n), "count": n} for n in range(1, 5)],
                {"type": "minecraft:explosion_decay"}], "name": rid(plant)}], "rolls": 1}], "random_sequence": rid(f"blocks/{plant}")})


def tags(tags):
    for plant, info in pl.PLANTS.items():
        kind = info["kind"]
        if kind in ("grass", "tall_grass", "hanging", "vine"):
            # Like vanilla's grasses, hanging moss and vines: cut quickly by a sword, and trees grow through them.
            tags.add("block", "minecraft:sword_efficient", rid(plant))
            tags.add("block", "minecraft:replaceable_by_trees", rid(plant))
            if kind in ("grass", "tall_grass"):
                tags.add("block", "minecraft:enchantment_power_transmitter", rid(plant))
            continue
        if kind in ("tall_plant", "dune_plant", "floor_plant", "water_plant", "surface"):
            if kind in ("tall_plant", "dune_plant"):
                tags.add("block", "minecraft:replaceable_by_trees", rid(plant))
                tags.add("block", "minecraft:sword_efficient", rid(plant))
            continue
        if kind == "flower":
            for registry in ("block", "item"):
                tags.add(registry, "minecraft:small_flowers", rid(plant))
            tags.add("block", "minecraft:flower_pots", rid(pl.potted(plant)))
        elif kind == "tall_flower":
            for registry in ("block", "item"):
                tags.add(registry, "minecraft:flowers", rid(plant))
        else:
            for registry in ("block", "item"):
                tags.add(registry, "minecraft:flowers", rid(plant))
            tags.add("block", "minecraft:inside_step_sound_blocks", rid(plant))
        tags.add("block", "minecraft:bee_attractive", rid(plant))
        tags.add("block", "minecraft:replaceable_by_trees", rid(plant))
        tags.add("block", "minecraft:enchantment_power_transmitter", rid(plant))


def placement_state(plant):
    """What a plant's feature places: one block, the lower half of a tall flower, or clumps of any size and facing."""
    kind = pl.PLANTS[plant]["kind"]
    if kind in ("flower", "grass"):
        return {"id": rid(plant)}
    if kind in pl.TALL:
        return {"id": rid(plant), "properties": {"half": "lower"}}
    if kind in ("water_plant", "surface", "floor_plant"):
        return {"id": rid(plant)}
    return {"type": "minecraft:weighted", "entries": [
        {"data": {"id": rid(plant), "properties": {"facing": facing, "flower_amount": str(n)}}, "weight": 1}
        for n in range(1, 5) for facing in FACINGS]}


def feature(plant):
    """A plant's own feature: one of it placed (simple_block), or for a hanging plant a strand hanging down from where
    it starts, or for a vine a spread of it over nearby faces (vanilla's glow lichen feature)."""
    kind = pl.PLANTS[plant]["kind"]
    if kind == "hanging":
        length = pl.HANGING["worldgen_length"]
        return {"type": "minecraft:block_column", "allowed_placement": {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"},
                "direction": "down", "prioritize_tip": True, "layers": [
                    {"height": {"type": "minecraft:uniform", "min_inclusive": 0, "max_inclusive": length - 1},
                     "provider": {"id": rid(plant), "properties": {"tip": "false"}}},
                    {"height": 1, "provider": {"id": rid(plant), "properties": {"tip": "true"}}}]}
    if kind == "vine":
        return {"type": "minecraft:multiface_growth", "block": rid(plant), "can_be_placed_on": VINE_HOSTS, "can_place_on_ceiling": False,
                "can_place_on_floor": False, "can_place_on_wall": True, "chance_of_spreading": 0.6, "search_range": 6}
    return {"type": "minecraft:simple_block", "to_place": placement_state(plant)}


# What creeping ivy grows over in the wild: logs, stone, cobblestone and mossy stone.
VINE_HOSTS = ["minecraft:oak_log", "minecraft:dark_oak_log", "minecraft:spruce_log", "minecraft:birch_log", "minecraft:pale_oak_log",
              "minecraft:stone", "minecraft:cobblestone", "minecraft:mossy_cobblestone", "minecraft:andesite", "minecraft:tuff",
              "jugcraft:dead_log"]
# Where a hanging plant may start: just under leaves or a log, with air below.
HANG_FROM = {"type": "minecraft:all_of", "predicates": [
    {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"},
    {"type": "minecraft:any_of", "predicates": [
        {"type": "minecraft:matching_block_tag", "tag": "minecraft:leaves", "offset": [0, 1, 0]},
        {"type": "minecraft:matching_block_tag", "tag": "minecraft:logs", "offset": [0, 1, 0]}]}]}


def hanging_placement(count):
    """Strands under the canopy: from the ground up to just below the leaves or a branch."""
    return [{"type": "minecraft:count", "count": count}, {"type": "minecraft:in_square"},
            {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING_NO_LEAVES"},
            {"type": "minecraft:environment_scan", "direction_of_search": "up", "max_steps": 14, "target_condition": HANG_FROM,
             "allowed_search_condition": {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"}},
            {"type": "minecraft:biome"}]


def vine_placement(count):
    return [{"type": "minecraft:count", "count": count}, {"type": "minecraft:in_square"},
            {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING_NO_LEAVES"},
            {"type": "minecraft:offset", "x": 0, "y": {"type": "minecraft:uniform", "min_inclusive": 0, "max_inclusive": 3}, "z": 0},
            {"type": "minecraft:biome"}]


def patch_placement(plant):
    """Where a plant's "patch" scatters it in vanilla's biomes: one patch in about `rarity` chunks."""
    info = pl.PLANTS[plant]["patch"]
    kind = pl.PLANTS[plant]["kind"]
    if kind == "hanging":
        return [{"type": "minecraft:rarity_filter", "chance": info["rarity"]}] + hanging_placement(info["tries"])
    if kind == "vine":
        return [{"type": "minecraft:rarity_filter", "chance": info["rarity"]}] + vine_placement(info["tries"])
    return [{"type": "minecraft:rarity_filter", "chance": info["rarity"]}, {"type": "minecraft:in_square"},
            {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"}, {"type": "minecraft:biome"},
            {"type": "minecraft:count", "count": info["tries"]},
            {"type": "minecraft:offset", "x": {"type": "minecraft:trapezoid", "max": 6, "min": -6, "plateau": 0},
             "y": {"type": "minecraft:trapezoid", "max": 2, "min": -2, "plateau": 0},
             "z": {"type": "minecraft:trapezoid", "max": 6, "min": -6, "plateau": 0}},
            {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"}}]


def worldgen(data, write):
    write(data.parent / MOD / "plants.json", pl.registration())
    for plant, info in pl.PLANTS.items():
        write(data / MOD / "worldgen" / "feature" / f"{plant}.json", feature(plant))
        if "patch" in info:
            write(data / MOD / "worldgen" / "placed_feature" / f"patch_{plant}.json", {"feature": rid(plant), "placement": patch_placement(plant)})
