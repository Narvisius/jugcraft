"""Writes Alpine Spawn's data from tools/alpine.py: the biome (from vanilla 26.3's meadow layout), its trees, its
villages and its tags. Called by generate_material_data.py."""
import alpine as al

MOD = "jugcraft"


def rid(path):
    return path if ":" in path else f"{MOD}:{path}"


def uniform(low, high):
    return {"type": "minecraft:uniform", "max_inclusive": high, "min_inclusive": low}


# Vanilla 26.3 meadow's mobs, plus foxes (a cool mountain meadow).
MEADOW_SPAWNS = {
    "ambient": [{"type": "minecraft:bat", "count": 8, "weight": 10}],
    "creature": [
        {"type": "minecraft:donkey", "count": uniform(1, 2), "weight": 1},
        {"type": "minecraft:rabbit", "count": uniform(2, 6), "weight": 2},
        {"type": "minecraft:sheep", "count": uniform(2, 4), "weight": 2},
        {"type": "minecraft:fox", "count": uniform(2, 4), "weight": 2},
    ],
    "monster": [
        {"type": "minecraft:spider", "count": 4, "weight": 100},
        {"type": "minecraft:zombie", "count": 4, "weight": 95},
        {"type": "minecraft:zombie_villager", "count": 1, "weight": 5},
        {"type": "minecraft:skeleton", "count": 4, "weight": 100},
        {"type": "minecraft:creeper", "count": 4, "weight": 100},
        {"type": "minecraft:slime", "count": 4, "weight": 100},
        {"type": "minecraft:enderman", "count": uniform(1, 4), "weight": 10},
        {"type": "minecraft:witch", "count": 1, "weight": 5},
    ],
    "underground_water_creature": [{"type": "minecraft:glow_squid", "count": uniform(4, 6), "weight": 10}],
}

# Vanilla 26.3 meadow's feature steps, with its trees swapped for Alpine Spawn's (an own placed feature keeps the
# global feature order intact).
FEATURES = [
    [],
    ["minecraft:lake_lava_underground", "minecraft:lake_lava_surface"],
    ["minecraft:amethyst_geode"],
    ["minecraft:monster_room", "minecraft:monster_room_deep"],
    [],
    [],
    ["minecraft:ore_dirt", "minecraft:ore_gravel", "minecraft:ore_granite_upper", "minecraft:ore_granite_lower",
     "minecraft:ore_diorite_upper", "minecraft:ore_diorite_lower", "minecraft:ore_andesite_upper",
     "minecraft:ore_andesite_lower", "minecraft:ore_tuff", "minecraft:ore_coal_upper", "minecraft:ore_coal_lower",
     "minecraft:ore_iron_upper", "minecraft:ore_iron_middle", "minecraft:ore_iron_small", "minecraft:ore_gold",
     "minecraft:ore_gold_lower", "minecraft:ore_redstone", "minecraft:ore_redstone_lower", "minecraft:ore_diamond",
     "minecraft:ore_diamond_medium", "minecraft:ore_diamond_large", "minecraft:ore_diamond_buried",
     "minecraft:ore_lapis", "minecraft:ore_lapis_buried", "minecraft:ore_copper", "minecraft:underwater_magma",
     "minecraft:disk_sand", "minecraft:disk_clay", "minecraft:disk_gravel", "minecraft:ore_emerald"],
    ["minecraft:ore_infested"],
    ["minecraft:spring_water", "minecraft:spring_lava"],
    ["minecraft:glow_lichen", "minecraft:patch_tall_grass_2", "minecraft:patch_grass_meadow", "minecraft:flower_meadow",
     rid("trees_alpine_spawn"), "minecraft:wildflowers_meadow"],
    ["minecraft:freeze_top_layer"],
]


def biome():
    return {
        "attributes": {
            "minecraft:audio/background_music": {"default": {"max_delay": 24000, "min_delay": 12000,
                                                             "sound": "minecraft:music.overworld.meadow"}},
            "minecraft:gameplay/natural_mob_spawns": {"argument": {"spawn_costs": {}, "spawns_by_category": MEADOW_SPAWNS},
                                                       "modifier": "overlay"},
            "minecraft:visual/sky_color": "#7ca3ff",
        },
        "carvers": ["minecraft:cave", "minecraft:cave_extra_underground", "minecraft:canyon"],
        "downfall": al.DOWNFALL,
        "effects": {"water_color": al.WATER_COLOR, "dry_foliage_color": al.DRY_FOLIAGE_COLOR},
        "features": FEATURES,
        "has_precipitation": True,
        "temperature": al.TEMPERATURE,
    }


def worldgen(data, write):
    folder = data / MOD / "worldgen"
    write(folder / "biome" / f"{al.BIOME}.json", biome())
    trees = al.TREES
    write(folder / "feature" / f"{trees['feature']}.json", {
        "type": "minecraft:random_selector", "default": trees["spruce"],
        "features": [{"chance": trees["larch_chance"], "feature": trees["larch"]}]})
    write(folder / "placed_feature" / "trees_alpine_spawn.json", {"feature": rid(trees["feature"]), "placement": [
        {"type": "minecraft:count", "count": {"type": "minecraft:weighted_list", "distribution": [
            {"data": trees["count"] - 1, "weight": 1}, {"data": trees["count"], "weight": 1},
            {"data": trees["count"] + 1, "weight": 1}]}},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0},
        {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR"},
        {"type": "minecraft:biome"},
    ]})
    # Villages: vanilla's taiga village pieces, in Alpine Spawn only, on their own tight grid.
    write(folder / "structure" / f"{al.VILLAGE}.json", {
        "type": "minecraft:jigsaw", "biomes": f"#{al.VILLAGE_TAG}", "max_distance_from_center": 80,
        "project_start_to_heightmap": "WORLD_SURFACE_WG", "size": 6, "spawn_overrides": {},
        "start_height": {"absolute": 0}, "start_pool": "minecraft:village/taiga/town_centers",
        "step": "surface_structures", "terrain_adaptation": "beard_thin", "use_expansion_hack": True})
    write(folder / "structure_set" / f"{al.VILLAGE_SET}.json", {
        "placement": {"type": "minecraft:random_spread", "salt": al.VILLAGES["salt"],
                      "separation": al.VILLAGES["separation"], "spacing": al.VILLAGES["spacing"]},
        "structures": [{"structure": rid(al.VILLAGE), "weight": 1}]})


def tags(tags):
    biome = rid(al.BIOME)
    for tag in al.BIOME_TAGS + [al.VILLAGE_TAG]:
        tags.add("worldgen/biome", tag, biome)
    # Maps, /locate and villagers' village logic know it as a village.
    tags.add("worldgen/structure", "minecraft:village", rid(al.VILLAGE))
    # The start search looks for these.
    tags.add("worldgen/structure", al.VILLAGE_STRUCTURES, rid(al.VILLAGE))


def lang(lang):
    lang[f"biome.{MOD}.{al.BIOME}"] = al.DISPLAY
