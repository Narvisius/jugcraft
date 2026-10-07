"""Generated data for the biomes branch (tools/biomes.py): biome files, their tree selectors and extra features,
biome tags and names. Region placement is Java (biome/JugcraftRegions); trees are tools/trees_data.py.
"""
import copy

import biomes as bm
from biome_bases import BASES

MOD = bm.MOD
PATCH_SPREAD = {"x": {"type": "minecraft:trapezoid", "max": 7, "min": -7, "plateau": 0},
                "y": {"type": "minecraft:trapezoid", "max": 3, "min": -3, "plateau": 0},
                "z": {"type": "minecraft:trapezoid", "max": 7, "min": -7, "plateau": 0}}


def rid(path):
    return path if ":" in path else f"{MOD}:{path}"


def spawn(entry):
    kind, weight, low, high = entry
    count = low if low == high else {"type": "minecraft:uniform", "max_inclusive": high, "min_inclusive": low}
    return {"type": kind, "count": count, "weight": weight}


STEPS = 11  # vanilla's generation steps (raw generation to top layer modification)


def base_trees(base):
    """The base's tree feature (vanilla's trees_* placed feature in its vegetation step), if it has one."""
    steps = BASES[base]["steps"]
    for feature in steps[9] if len(steps) > 9 else []:
        if feature.startswith("minecraft:trees_"):
            return feature
    return None


def steps(name):
    """The biome's features per step: its base's, with the trees swapped for its own (or dropped, for a treeless
    biome), drops and swaps applied, and its extras appended in EXTRAS order."""
    info = bm.BIOMES[name]
    out = copy.deepcopy(BASES[info["base"]]["steps"])
    # Nether and End bases list only the steps they use; every biome lists all of them.
    out += [[] for _ in range(STEPS - len(out))]
    tree = info.get("base_trees") or base_trees(info["base"])
    drop = list(info.get("drop", []))
    if info["trees"] is None:
        drop.append(tree)
    elif tree is None:
        # A base without trees (a desert, for example): the biome's own trees join its vegetation step. The feature is
        # the biome's alone, so its place cannot clash with another biome's order.
        out[9].append(rid(f"trees_{name}"))
    for step in out:
        for i, feature in enumerate(step):
            if feature == tree and info["trees"] is not None:
                step[i] = rid(f"trees_{name}")
            elif feature in info.get("swap", {}):
                step[i] = info["swap"][feature]
        step[:] = [f for f in step if f not in drop]
    if "ground" in info:
        # Laid first (step 2, local modifications), so the base's and the biome's features stand on it.
        out[2].append(rid(f"ground_{name}"))
    for extra in bm.EXTRAS:
        if extra in info.get("extras", []):
            out[bm.EXTRAS[extra]["step"]].append(rid(extra))
    return out


def ground_feature(name):
    """A Nether or End biome's ground: vegetation patches of its blocks over the floor, optionally with a plant."""
    ground = bm.BIOMES[name]["ground"]
    blocks = {"type": "minecraft:weighted", "entries": [{"data": block, "weight": weight} for block, weight in ground["blocks"].items()]}
    plant = ground.get("plant")
    return {"type": "minecraft:vegetation_patch", "depth": {"type": "minecraft:uniform", "max_inclusive": 2, "min_inclusive": 1},
            "extra_bottom_block_chance": 0.0, "extra_edge_column_chance": 0.3, "ground_state": blocks,
            "replaceable": ground.get("replaceable", "#minecraft:base_stone_nether"), "surface": "floor",
            "vegetation_chance": ground.get("plant_chance", 0.0) if plant else 0.0,
            "vegetation_feature": {"feature": plant or "jugcraft:glowcap", "placement": []}, "vertical_range": 5,
            "xz_radius": {"type": "minecraft:uniform", "max_inclusive": 7, "min_inclusive": 4}}


def biome(name):
    info = bm.BIOMES[name]
    base = BASES[info["base"]]
    spawns = copy.deepcopy(base["spawns"])
    if "creatures" in info:
        spawns["creature"] = [spawn(entry) for entry in info["creatures"]]
    if "monsters" in info:
        spawns["monster"] = [spawn(entry) for entry in info["monsters"]]
    attributes = {"minecraft:gameplay/natural_mob_spawns": {"argument": {
        "spawn_costs": copy.deepcopy(base.get("spawn_costs", {})), "spawns_by_category": spawns}, "modifier": "overlay"}}
    if "sky_color" in base:
        attributes["minecraft:visual/sky_color"] = base["sky_color"]
    if "music" in base:
        attributes["minecraft:audio/background_music"] = {"default": {"max_delay": 24000, "min_delay": 12000, "sound": base["music"]}}
    attributes.update(copy.deepcopy(base.get("attributes", {})))
    attributes.update(copy.deepcopy(info.get("attributes", {})))
    effects = copy.deepcopy(base.get("effects", {}))
    if effects.get("water_color", "#3f76e4") in ("#3f76e4", "#3d57d6"):
        # Plain water: vanilla's cold blue below -0.2, its usual blue above.
        effects["water_color"] = "#3f76e4" if info["temperature"] > -0.2 else "#3d57d6"
    effects.update(info.get("effects", {}))
    return {"attributes": dict(sorted(attributes.items())), "carvers": base["carvers"], "downfall": info["downfall"],
            "effects": dict(sorted(effects.items())), "features": steps(name),
            "has_precipitation": info.get("precipitation", base.get("precipitation", True)), "temperature": info["temperature"]}


def extra_placement(extra):
    info = bm.EXTRAS[extra]
    if info.get("placement") in ("hanging", "vine"):
        # The graveyard flora's hanging strands and ivy (tools/plants_data.py).
        import plants_data
        placement = plants_data.hanging_placement if info["placement"] == "hanging" else plants_data.vine_placement
        return placement(info["count"])
    if "placement" in info:
        return info["placement"]
    placement = []
    if "patches" in info:
        placement.append({"type": "minecraft:count", "count": info["patches"]})
    if "rarity" in info:
        placement.append({"type": "minecraft:rarity_filter", "chance": info["rarity"]})
    placement += [{"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"},
                  {"type": "minecraft:biome"}, {"type": "minecraft:count", "count": info["count"]},
                  {"type": "minecraft:offset", **PATCH_SPREAD}]
    air = {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"}
    if "on" in info:
        predicate = {"type": "minecraft:all_of", "predicates": [
            air, {"type": "minecraft:matching_blocks", "blocks": info["on"], "offset": [0, -1, 0]}]}
    else:
        predicate = air
    if "survive" in info:
        predicate = {"type": "minecraft:all_of", "predicates": [predicate, {"type": "minecraft:would_survive", "state": info["survive"]}]}
    placement.append({"type": "minecraft:block_predicate_filter", "predicate": predicate})
    return placement


def worldgen(data, write):
    write(data.parent / MOD / "region_rules.json", bm.rules_file())
    write(data.parent / MOD / "dimension_biomes.json", bm.dimension_file())
    write(data / MOD / "worldgen" / "material_rule" / "overworld" / "surface.json", surface_rule())
    write(data / "minecraft" / "worldgen" / "material_rule" / "overworld.json", OVERWORLD_MATERIAL_RULE)
    for tree, (feature, survives) in bm.PLACED_TREES.items():
        write(data / MOD / "worldgen" / "placed_feature" / f"{tree}.json", {"feature": feature, "placement": [
            {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:would_survive", "state": survives}}]})
    folder = data / MOD / "worldgen"
    for name, info in bm.BIOMES.items():
        write(folder / "biome" / f"{name}.json", biome(name))
        if "ground" in info:
            write(folder / "feature" / f"ground_{name}.json", ground_feature(name))
            write(folder / "placed_feature" / f"ground_{name}.json", {"feature": rid(f"ground_{name}"), "placement": [
                {"type": "minecraft:count_on_every_layer", "count": info["ground"].get("count", bm.GROUND_COUNT)},
                {"type": "minecraft:biome"}]})
        trees = info["trees"]
        if trees is None:
            continue
        write(folder / "feature" / f"trees_{name}.json", {
            "type": "minecraft:random_selector", "default": trees["default"],
            "features": [{"chance": chance, "feature": feature} for feature, chance in trees["picks"]]})
        usual, sometimes = trees["count"]
        write(folder / "placed_feature" / f"trees_{name}.json", {"feature": rid(f"trees_{name}"), "placement": [
            {"type": "minecraft:count", "count": {"type": "minecraft:weighted_list", "distribution": [
                {"data": usual, "weight": 9}, {"data": sometimes, "weight": 1}]}},
            {"type": "minecraft:in_square"},
            {"type": "minecraft:surface_water_depth_filter", "max_water_depth": trees.get("water_depth", 0)},
            {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR"},
            {"type": "minecraft:biome"}]})
    for extra, info in bm.EXTRAS.items():
        feature = info.get("feature")
        if "block" in info:
            # A block placed alone: a feature of its own, named after the extra.
            feature = rid(extra)
            state = {"id": info["block"]}
            if "properties" in info:
                state["properties"] = info["properties"]
            write(folder / "feature" / f"{extra}.json", {"type": "minecraft:simple_block", "to_place": state})
        elif "configured" in info:
            feature = rid(extra)
            write(folder / "feature" / f"{extra}.json", info["configured"])
        write(folder / "placed_feature" / f"{extra}.json", {"feature": feature, "placement": extra_placement(extra)})


# Vanilla's top-level Overworld material rule (26.3, by reference: its named parts), with Jugcraft's surface rule run
# first wherever the surface is decided. Jugcraft's rule only acts in Jugcraft biomes that set a "surface"; everywhere
# else vanilla's surface follows unchanged.
OVERWORLD_MATERIAL_RULE = {"type": "minecraft:sequence", "sequence": [
    "minecraft:bedrock_floor", "minecraft:overworld/copper_ore_vein", "minecraft:overworld/iron_ore_vein",
    {"type": "minecraft:condition", "if_true": {"type": "minecraft:above_preliminary_surface"},
     "then_run": {"type": "minecraft:sequence", "sequence": [rid("overworld/surface"), "minecraft:overworld/surface"]}},
    "minecraft:overworld/underground"]}


def material(block):
    """A block result, a named vanilla rule ("rule:minecraft:overworld/sand_or_sandstone_if_ceiling"), or a rule written
    out ({"type": "minecraft:bandlands"}, vanilla's badlands bands)."""
    if isinstance(block, dict):
        return block
    if block.startswith("rule:"):
        return block.removeprefix("rule:")
    return {"type": "minecraft:block", "result_state": block}


def floor_rule(surface):
    rules = [{"type": "minecraft:condition", "if_true": {"type": "minecraft:noise_threshold", "noise": "minecraft:surface",
                                                         "min_threshold": low, "max_threshold": high}, "then_run": material(block)}
             for low, high, block in surface.get("patches", [])]
    rules.append(material(surface["floor"]))
    return rules[0] if len(rules) == 1 else {"type": "minecraft:sequence", "sequence": rules}


def surface_rule():
    """Jugcraft's surface: for each biome with a "surface", its floor (with noise patches) and what lies under it."""
    rules = []
    for name, info in bm.BIOMES.items():
        surface = info.get("surface")
        if not surface:
            continue
        parts = [{"type": "minecraft:condition", "if_true": "minecraft:on_floor", "then_run": floor_rule(surface)}]
        if "under" in surface:
            parts.append({"type": "minecraft:condition", "if_true": "minecraft:under_floor", "then_run": material(surface["under"])})
        rules.append({"type": "minecraft:condition", "if_true": {"type": "minecraft:biome", "biome_is": rid(name)},
                      "then_run": {"type": "minecraft:sequence", "sequence": parts}})
    return {"type": "minecraft:sequence", "sequence": rules}


def tags(tags):
    # Seasons tags come from seasons.BIOMES, which lists the four-season ones (info["seasons"]).
    for name in bm.BIOMES:
        for tag in bm.biome_tags(name):
            tags.add("worldgen/biome", tag, rid(name))
    # What an End biome's ground patches may replace.
    tags.add("block", rid("end_ground_replaceable"), "minecraft:end_stone")


def lang(lang):
    for name, info in bm.BIOMES.items():
        lang[f"biome.{MOD}.{name}"] = info["display"]
