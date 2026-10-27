"""What kind of place each landscape biome is, so seasonal content treats the landscape collection (the owner's
18 October collections, tools/landscape_content.py) as it treats Jugcraft's own biomes (docs/features/seasonal-biome-audit.md).

Overworld landscape biomes replace part of each Jugcraft region (menagerie/LandscapeBiomes); their Nether and End
biomes join those dimensions (landscape-dimensions.json); their caves lie underground. They arrived without the
biome-kind tags (#minecraft:is_forest, #c:is_swamp and the like) that seasonal content reads, so each is given a
kind here, from its name, climate and terrain:

- forest, taiga, grassland, hills, swamp: the kinds that have four seasons, as in tools/seasons.py;
- dark_woods: a forest kept gloomy all year (its fixed palette is the point, as the Gloomweald's);
- magic: a biome of fixed, otherworldly or nostalgic colours (as the Elder Vale and the Glimmer Grove);
- skylands: the floating islands, one biome for each season, which keep their own;
- arid, tropical, volcanic, coast, beach, ocean, river: no four seasons (tools/seasons.py);
- cave, nether, end: below ground or in another dimension.

From the kinds and the climate (checked against each biome's file by tools/check_mod_data.py):
- SEASONS: the four-season kinds where it rains and is neither frozen nor tropical (temperature 0.15 up to 0.9,
  the range of the seasonal vanilla and Jugcraft biomes), so their grass and leaves turn with the year;
- MILD: of those, the ones with mild winters (Mediterranean, lavender, redwood, warm shrub and orchid swamp, as their
  Jugcraft namesakes), which keep their colours' turn but get no winter snow;
- SNOWY: the land where snow falls all year (raining, temperature below 0.15);
- the kind tags below, which the werewolves, squirrels, turkeys and wisps read beside the vanilla and common tags,
  as do the fall's wild patches, the graveyard flowers and the Hag Stone (BESIDE, FALL_PATCHES, tools/lairs.py).
"""

MOD = "jugcraft"

# Every landscape biome, by its resource path (the caves sit in a cave/ folder).
KINDS = {
    # Caves, and two surface biomes the landscape placement treats as caves (LandscapeBiomes.Entry.cave).
    **{f"cave/landscape_{name}": "cave" for name in (
        "andesite_caves", "deep_caves", "diorite_caves", "frostfire_caves", "fungal_caves", "granite_caves",
        "infested_caves", "mantle_caves", "thermal_caves", "tuff_caves", "underground_jungle")},
    "landscape_glowing_grotto": "cave", "landscape_spider_nest": "cave",
    # The Nether and the End (tools/biomes.py OWNER_NETHER_POINTS, OWNER_END_BIOMES).
    **{f"landscape_{name}": "nether" for name in (
        "erupting_inferno", "visceral_heap", "withered_abyss", "undergrowth", "crystalline_chasm")},
    **{f"landscape_{name}": "end" for name in ("end_wilds", "end_reef", "end_flats", "end_corruption")},
    # Woods.
    **{f"landscape_{name}": "forest" for name in (
        "alpine_grove", "cloud_forest", "dead_forest", "forested_field", "lavender_forest", "maple_woods",
        "mediterranean_forest", "old_growth_dead_forest", "old_growth_woodland", "orchard", "sakura_grove",
        "seasonal_forest", "snowblossom_grove", "snowy_cherry_grove", "snowy_maple_forest", "snowy_maple_woods",
        "wintry_forest", "woodland", "yosemite_lowlands")},
    **{f"landscape_{name}": "taiga" for name in (
        "birch_taiga", "coniferous_forest", "fir_clearing", "redwood_forest", "shield", "shield_clearing",
        "siberian_grove", "siberian_taiga", "snowy_coniferous_forest", "snowy_fir_clearing", "snowy_shield")},
    "landscape_ominous_woods": "dark_woods",
    # Open country.
    **{f"landscape_{name}": "grassland" for name in (
        "blooming_plateau", "blooming_valley", "cold_shrubland", "field", "hot_shrubland", "lavender_field",
        "lavender_valley", "lush_valley", "moor", "overgrown_greens", "pasture", "prairie", "pumpkin_patch",
        "rocky_shrubland", "sakura_valley", "shrubland", "steppe", "tundra", "valley_clearing", "wintry_lowlands")},
    **{f"landscape_{name}": "hills" for name in (
        "alpine_highlands", "caldera", "crag", "emerald_peaks", "forested_highlands", "glacial_chasm",
        "haze_mountain", "highland", "highlands", "hot_springs", "rocky_mountains", "scarlet_mountains",
        "stony_spires", "temperate_highlands", "windswept_spires", "yellowstone", "yosemite_cliffs")},
    **{f"landscape_{name}": "swamp" for name in (
        "bayou", "bog", "ice_marsh", "marsh", "muskeg", "orchid_swamp", "wetland")},
    # No four seasons.
    **{f"landscape_{name}": "magic" for name in (
        "alpha_islands", "alpha_islands_winter", "amethyst_canyon", "amethyst_rainforest", "auroral_garden",
        "mirage_isles", "moonlight_grove", "moonlight_valley", "mystic_grove", "origin_valley",
        "wintry_origin_valley")},
    **{f"landscape_skylands_{season}": "skylands" for season in ("spring", "summer", "autumn", "winter")},
    **{f"landscape_{name}": "arid" for name in (
        "ancient_sands", "arid_highlands", "ashen_savanna", "brushland", "bryce_canyon", "cold_desert",
        "desert_canyon", "desert_oasis", "desert_spires", "dryland", "fractured_savanna", "gravel_desert",
        "lush_desert", "lush_savanna", "painted_mountains", "red_oasis", "sandstone_valley", "savanna_badlands",
        "savanna_slopes", "scrubland", "snowy_badlands", "warped_mesa", "wasteland", "wasteland_steppe",
        "white_mesa")},
    **{f"landscape_{name}": "tropical" for name in (
        "floodplain", "fungal_jungle", "jade_cliffs", "jungle_mountains", "rainforest", "rocky_jungle",
        "rocky_rainforest", "subtropics", "tropical_jungle", "tropics")},
    **{f"landscape_{name}": "volcanic" for name in ("volcanic_crater", "volcanic_peaks", "volcanic_plains", "volcano")},
    **{f"landscape_{name}": "coast" for name in ("basalt_cliffs", "frozen_cliffs", "granite_cliffs", "white_cliffs")},
    **{f"landscape_{name}": "beach" for name in ("dune_beach", "gravel_beach")},
    "landscape_deep_warm_ocean": "ocean",
    "landscape_warm_river": "river",
}

# The kinds with four seasons (tools/seasons.py: temperate grassland, forest, taiga, hills and swamp).
SEASONAL_KINDS = ("forest", "taiga", "grassland", "hills", "swamp")
# Their climate: rain falls (snow below 0.15) and it is not tropical (jungles are 0.95).
SEASON_TEMPERATURE = (0.15, 0.9)
SNOW_BELOW = 0.15

# Grass and leaves turn with the year (#jugcraft:has_seasons).
SEASONS = [f"landscape_{name}" for name in (
    "alpine_highlands", "birch_taiga", "blooming_plateau", "blooming_valley", "bog", "caldera", "cloud_forest",
    "coniferous_forest", "crag", "dead_forest", "field", "fir_clearing", "forested_field", "forested_highlands",
    "haze_mountain", "highland", "highlands", "hot_shrubland", "hot_springs", "lavender_field", "lavender_forest",
    "lavender_valley", "lush_valley", "maple_woods", "marsh", "mediterranean_forest", "moor", "old_growth_dead_forest",
    "old_growth_woodland", "orchard", "orchid_swamp", "overgrown_greens", "pasture", "prairie", "pumpkin_patch",
    "redwood_forest", "rocky_shrubland", "sakura_grove", "sakura_valley", "seasonal_forest", "shield",
    "shield_clearing", "shrubland", "steppe", "stony_spires", "temperate_highlands", "tundra", "valley_clearing",
    "wetland", "windswept_spires", "woodland", "yellowstone", "yosemite_cliffs", "yosemite_lowlands")]
# Of those, mild winters: no winter snow (#jugcraft:has_winter_snow leaves them out).
MILD = [f"landscape_{name}" for name in (
    "hot_shrubland", "lavender_field", "lavender_forest", "lavender_valley", "mediterranean_forest", "orchid_swamp",
    "redwood_forest")]
# Snow falls all year: raining and colder than 0.15, on the surface.
SNOWY = [f"landscape_{name}" for name in (
    "alpha_islands_winter", "alpine_grove", "auroral_garden", "cold_shrubland", "emerald_peaks", "frozen_cliffs",
    "glacial_chasm", "gravel_desert", "ice_marsh", "muskeg", "scarlet_mountains", "siberian_grove", "snowblossom_grove",
    "snowy_badlands", "snowy_cherry_grove", "snowy_coniferous_forest", "snowy_fir_clearing", "snowy_maple_forest",
    "snowy_maple_woods", "snowy_shield", "wintry_forest", "wintry_lowlands", "wintry_origin_valley")]

# Kind tags (data/jugcraft/tags/worldgen/biome/landscape_kind/), for content that reads vanilla's and the common
# biome-kind tags: werewolves and squirrels the woods, turkeys the woods and open country, wisps the swamps,
# snow werewolves the snowy land (tools/agriculture.py).
KIND_TAGS = {
    f"{MOD}:landscape_kind/woods": [b for b, kind in KINDS.items() if kind in ("forest", "taiga", "dark_woods")],
    f"{MOD}:landscape_kind/grasslands": [b for b, kind in KINDS.items() if kind == "grassland" and b not in SNOWY],
    f"{MOD}:landscape_kind/swamps": [b for b, kind in KINDS.items() if kind == "swamp"],
    f"{MOD}:landscape_kind/snowy": list(SNOWY),
    f"{MOD}:landscape_kind/dark_woods": [b for b, kind in KINDS.items() if kind == "dark_woods"],
}
WOODS_TAG = "#" + f"{MOD}:landscape_kind/woods"
GRASSLANDS_TAG = "#" + f"{MOD}:landscape_kind/grasslands"
SWAMPS_TAG = "#" + f"{MOD}:landscape_kind/swamps"
SNOWY_TAG = "#" + f"{MOD}:landscape_kind/snowy"
DARK_WOODS_TAG = "#" + f"{MOD}:landscape_kind/dark_woods"

# The fall's wild patches and the graveyard flowers (agriculture/JugcraftAgriculture.java fallPatch and the plants'
# "patch", tools/plants.py) reach the landscape biomes too: beside each common kind tag they read stands the landscape
# kind tag here, and its biomes get the patch as well. The rest (savanna, jungle, hills, floral, windswept, river,
# badlands) have no landscape counterpart: the landscape kinds there (arid, tropical, hills) are broader than any one
# common kind, and would put a savanna's pumpkins in a desert.
BESIDE = {"IS_FOREST": WOODS_TAG, "IS_TAIGA": WOODS_TAG, "IS_BIRCH_FOREST": WOODS_TAG, "IS_PLAINS": GRASSLANDS_TAG,
          "IS_SWAMP": SWAMPS_TAG, "IS_SNOWY": SNOWY_TAG, "IS_SPOOKY": DARK_WOODS_TAG}
# Those wild patches: the fall and Halloween harvest, the autumn mushrooms and wolfsbane. Every other wild crop keeps to
# the common tags (wildPatch).
FALL_PATCHES = ("wild_mandrake", "butternut_squash", "acorn_squash", "warty_gourd", "cranberry_bush", "chestnut_tree",
                "white_pumpkin", "jarrahdale_pumpkin", "cinderella_pumpkin", "red_kuri_pumpkin", "kabocha_pumpkin",
                "bottle_gourd", "mums", "chanterelle", "porcini", "puffball", "fly_agaric", "jack_o_lantern_mushroom",
                "wolfsbane")


def seasonal(kind, temperature, precipitation):
    """Whether a biome of this kind and climate has four seasons (the rule SEASONS follows)."""
    low, high = SEASON_TEMPERATURE
    return kind in SEASONAL_KINDS and precipitation and low <= temperature <= high


def snowy(kind, temperature, precipitation):
    """Whether snow falls all year on a surface biome of this kind and climate (the rule SNOWY follows)."""
    return kind not in ("cave", "nether", "end", "ocean", "river", "beach") and precipitation and temperature < SNOW_BELOW


def tags(out):
    for tag, biomes in KIND_TAGS.items():
        for biome in biomes:
            out.add("worldgen/biome", tag, f"{MOD}:{biome}")
